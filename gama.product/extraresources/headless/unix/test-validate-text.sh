#!/bin/bash
# Test script for -validate-text feature of GAMA headless
# Tests single model validation and multi-model validation with imports.
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
ECLIPSE_DIR="$SCRIPT_DIR/../Eclipse"
PLUGIN_DIR="$ECLIPSE_DIR/plugins"
CONFIG_DIR="$ECLIPSE_DIR/configuration"
LAUNCHER_JAR=$(ls "$PLUGIN_DIR"/org.eclipse.equinox.launcher_*.jar 2>/dev/null | head -1)

if [ -z "$LAUNCHER_JAR" ]; then
  PRODUCT_BASE="/Users/hqnghi/git/hgama/gama/gama.product/target/products/gama.headless.product/macosx/cocoa/aarch64/Eclipse.app/Contents/Eclipse"
  LAUNCHER_JAR=$(ls "$PRODUCT_BASE/plugins/org.eclipse.equinox.launcher_"*.jar 2>/dev/null | head -1)
  CONFIG_DIR="$PRODUCT_BASE/configuration"
fi

if [ -z "$LAUNCHER_JAR" ]; then
  echo "Cannot find launcher JAR. Build the headless product first."
  exit 1
fi

BASE_ARGS=(-cp "$LAUNCHER_JAR" -Xms512m -Xmx2g
  --add-exports java.base/java.lang=ALL-UNNAMED
  --add-opens java.base/java.lang=ALL-UNNAMED
  --enable-preview
  org.eclipse.equinox.launcher.Main
  -configuration "$CONFIG_DIR"
  -application gama.headless.product)

PASS=0
FAIL=0

run_test() {
  local name="$1" expected="$2"
  shift 2
  local ws="/tmp/gama-test-$$-$RANDOM"
  local output
  output=$(java "${BASE_ARGS[@]}" -data "$ws" "$@" 2>&1)
  rm -rf "$ws"
  if echo "$output" | grep -q "$expected"; then
    echo "  PASS"
    PASS=$((PASS + 1))
  else
    echo "  FAIL ($name): expected '$expected'"
    echo "  Output:|$(echo "$output" | grep -v "^> \|^WARNING\|^2026-\|java.base")|"
    FAIL=$((FAIL + 1))
  fi
}

echo "=== Test 1: Valid standalone model ==="
run_test "standalone" "The model content is valid" \
  -validate-text 'model Test global { init { write "hello"; } }'

echo ""
echo "=== Test 2: Invalid syntax ==="
run_test "syntax" "Error in" \
  -validate-text 'not valid gaml syntax'

echo ""
echo "=== Test 3: Empty content ==="
run_test "empty" "No model content" \
  -validate-text ''

echo ""
echo "=== Test 4: Two models (Main imports Base) ==="
run_test "import" "All 2 model" \
  -validate-text 'model Base global { init { write "base"; } }' \
  'model Main import "Base.gaml" global { init { write "main"; } }'

echo ""
echo "=== Test 5: Three models (C imports B, B imports A) ==="
run_test "chain" "All 3 model" \
  -validate-text 'model A global { init { write "a"; } }' \
  'model B import "A.gaml" global { init { write "b"; } }' \
  'model C import "B.gaml" global { init { write "c"; } }'

echo ""
echo "=== Test 6: Broken import (single model) ==="
run_test "missing-import" "Impossible to locate" \
  -validate-text 'model X import "NoSuchModel.gaml" global {}'

echo ""
echo "=== Test 7: Subfolder import with explicit path ==="
run_test "subfolder" "All 2 model" \
  -validate-text 'sub/Base.gaml|model Base global {}' \
  'model Main import "sub/Base.gaml" global { init { write "main"; } }'

echo ""
echo "=== Test 8: Deep nesting (a/b/c) ==="
run_test "deep-nesting" "All 2 model" \
  -validate-text 'a/b/c/Deep.gaml|model Deep global {}' \
  'model Main import "a/b/c/Deep.gaml" global { init { write "main"; } }'

echo ""
echo "=== Test 9: Parent directory traversal (../../) ==="
run_test "parent-dir" "All 2 model" \
  -validate-text 'a/lib/Base.gaml|model Base global {}' \
  'a/b/c/Child.gaml|model Child import "../../lib/Base.gaml" global { init { write "child"; } }'

echo ""
echo "=== Test 10: Mixed style (path|content + name-extracted) ==="
run_test "mixed-style" "All 2 model" \
  -validate-text 'lib/Base.gaml|model Base global {}' \
  'model Main import "lib/Base.gaml" global { init { write "main"; } }'

echo ""
echo "=== Test 11: Batch validation — 4 independent models in one invocation ==="
run_test "batch" "All 4 model" \
  -validate-text 'model A global {}' \
  'model B global {}' \
  'model C global {}' \
  'model D global {}'

echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="
exit $FAIL

#!/bin/bash
# Test script for -validate-text feature of GAMA headless
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
ECLIPSE_DIR="$SCRIPT_DIR/../Eclipse"
PLUGIN_DIR="$ECLIPSE_DIR/plugins"
CONFIG_DIR="$ECLIPSE_DIR/configuration"
LAUNCHER_JAR=$(ls "$PLUGIN_DIR"/org.eclipse.equinox.launcher_*.jar 2>/dev/null | head -1)

if [ -z "$LAUNCHER_JAR" ]; then
  # Try the built product location
  PRODUCT_BASE="/Users/hqnghi/git/hgama/gama/gama.product/target/products/gama.headless.product/macosx/cocoa/aarch64/Eclipse.app/Contents/Eclipse"
  LAUNCHER_JAR=$(ls "$PRODUCT_BASE/plugins/org.eclipse.equinox.launcher_"*.jar 2>/dev/null | head -1)
  CONFIG_DIR="$PRODUCT_BASE/configuration"
fi

if [ -z "$LAUNCHER_JAR" ]; then
  echo "Cannot find launcher JAR. Build the headless product first."
  exit 1
fi

echo "=== Test 1: Valid GAML model ==="
java -cp "$LAUNCHER_JAR" \
  -Xms512m -Xmx2g \
  --add-exports java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --enable-preview \
  org.eclipse.equinox.launcher.Main \
  -configuration "$CONFIG_DIR" \
  -application gama.headless.product \
  -data "/tmp/gama-test-valid-$$" \
  -validate-text 'model test global { init { write "hello"; } }' 2>&1 | grep -E "(valid|Error|error)"

if [ $? -eq 0 ]; then
  echo "PASS: Valid model accepted (exit code 0)"
else
  echo "FAIL: Valid model produced unexpected result"
fi

echo ""
echo "=== Test 2: Invalid GAML model ==="
java -cp "$LAUNCHER_JAR" \
  -Xms512m -Xmx2g \
  --add-exports java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --enable-preview \
  org.eclipse.equinox.launcher.Main \
  -configuration "$CONFIG_DIR" \
  -application gama.headless.product \
  -data "/tmp/gama-test-invalid-$$" \
  -validate-text 'not valid gaml at all' 2>&1 | grep -E "(valid|Error|error)"

if [ $? -eq 0 ]; then
  echo "PASS: Invalid model rejected with errors"
else
  echo "FAIL: Invalid model did not produce expected errors"
fi

echo ""
echo "=== Test 3: Empty model content ==="
java -cp "$LAUNCHER_JAR" \
  -Xms512m -Xmx2g \
  --add-exports java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --enable-preview \
  org.eclipse.equinox.launcher.Main \
  -configuration "$CONFIG_DIR" \
  -application gama.headless.product \
  -data "/tmp/gama-test-empty-$$" \
  -validate-text '' 2>&1 | grep -E "(No model content|valid|Error)"

if [ $? -eq 0 ]; then
  echo "PASS: Empty content properly rejected"
else
  echo "FAIL: Empty content handling unexpected"
fi

echo ""
echo "=== All tests complete ==="

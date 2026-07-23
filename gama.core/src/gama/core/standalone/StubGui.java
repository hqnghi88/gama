package gama.core.standalone;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;

import gama.api.ui.IGui;

/**
 * No-op IGui implementation for standalone (headless) mode.
 * Uses a dynamic proxy so that any method call returns a safe default.
 */
public class StubGui {

	public static IGui create() {
		return (IGui) Proxy.newProxyInstance(
				IGui.class.getClassLoader(),
				new Class<?>[] { IGui.class },
				new InvocationHandler() {
					@Override
					public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
						final String name = method.getName();
						// Return IGui constants for known sub-objects
						if ("getStatus".equals(name)) return IGui.NULL_STATUS_DISPLAYER;
						if ("getDialogFactory".equals(name)) return IGui.NULL_DIALOG_FACTORY;
						if ("getProgressIndicator".equals(name)) return IGui.NULL_PROGRESS_INDICATOR;
						if ("getSnapshotMaker".equals(name)) return IGui.NULL_SNAPSHOT_MAKER;
						if ("getConsole".equals(name)) return null;
						if ("openWizard".equals(name)) return null;
						final Class<?> rt = method.getReturnType();
						if (rt == boolean.class) return true;
						if (rt == int.class || rt == long.class || rt == float.class || rt == double.class) return 0;
						if (rt == Map.class) return Collections.emptyMap();
						return null;
					}
				});
	}
}

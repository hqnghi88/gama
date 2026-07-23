package gama.core.standalone;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.URI;

import gama.api.runtime.IWorkspaceManager;

/**
 * Minimal workspace manager for standalone (headless) mode.
 * Provides a no-op IWorkspace proxy that supports resource change listeners,
 * so that GamlResourceIndexer can initialize without a real Eclipse workspace.
 */
public class StubWorkspaceManager implements IWorkspaceManager {

	private final java.nio.file.Path workspaceDir;
	private final IWorkspace workspace;

	public StubWorkspaceManager() {
		try {
			workspaceDir = Files.createTempDirectory("gama-standalone-workspace");
			workspaceDir.resolve(".gama_application_workspace").toFile().createNewFile();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		// Create a dynamic proxy for IWorkspace that handles addResourceChangeListener
		// and returns defaults for everything else
		final List<IResourceChangeListener> listeners = new CopyOnWriteArrayList<>();
		workspace = (IWorkspace) Proxy.newProxyInstance(
				IWorkspace.class.getClassLoader(),
				new Class<?>[] { IWorkspace.class },
				new InvocationHandler() {
					@Override
					public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
						final String name = method.getName();
						if ("addResourceChangeListener".equals(name) && args != null && args.length >= 1) {
							listeners.add((IResourceChangeListener) args[0]);
							return null;
						}
						if ("removeResourceChangeListener".equals(name) && args != null && args.length >= 1) {
							listeners.remove(args[0]);
							return null;
						}
						if ("getRoot".equals(name)) return null;
						if ("isAutoBuilding".equals(name)) return false;
						if ("isTreeLocked".equals(name)) return false;
						if ("getMaxBuildThreads".equals(name)) return 1;
						if ("getBuildOrder".equals(name)) return new String[0];
						if ("getRootProjects".equals(name)) return new org.eclipse.core.resources.IProject[0];
						if ("getDescription".equals(name)) return null;
						if ("getDelta".equals(name)) return null;
						if (method.getReturnType() == boolean.class) return false;
						if (method.getReturnType() == int.class) return 0;
						if (method.getReturnType() == long.class) return 0L;
						return null;
					}
				});
	}

	@Override
	public IWorkspace getWorkspace() { return workspace; }

	@Override
	public org.eclipse.core.resources.IWorkspaceRoot getRoot() { return null; }

	@Override
	public URI getWorkspaceURI() {
		return URI.createFileURI(workspaceDir.toString() + "/");
	}

	@Override
	public String getWorkspaceLocation() { return workspaceDir.toString(); }

	@Override
	public IPath getWorkspacePath() {
		return new org.eclipse.core.runtime.Path(workspaceDir.toString());
	}

	@Override
	public String checkWorkspaceDirectory(final String str, final boolean b, final boolean c, final boolean cloning) {
		return null;
	}

	@Override
	public String getModelIdentifier() { return "gama.standalone"; }

	@Override
	public void setLastSetWorkspaceDirectory(final String str) {}

	@Override
	public String getLastSetWorkspaceDirectory() { return workspaceDir.toString(); }

	@Override
	public void isRememberWorkspace(final boolean selection) {}

	@Override
	public void setLastUsedWorkspaces(final String string) {}

	@Override
	public boolean isRememberWorkspace() { return false; }

	@Override
	public String getLastUsedWorkspaces() { return ""; }

	@Override
	public Object checkWorkspace() throws IOException { return workspaceDir; }

	@Override
	public void forceWorkspaceRebuild() {}

	@Override
	public void clearWorkspace(final boolean b) {}

	@Override
	public String getCurrentGamaStampString() { return "standalone"; }
}

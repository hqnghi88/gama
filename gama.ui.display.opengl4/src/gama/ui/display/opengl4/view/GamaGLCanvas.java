/*******************************************************************************************************
 *
 * GamaGLCanvas.java, in gama.ui.display.opengl4, is part of the source code of the GAMA modeling and simulation
 * platform (v.2025-03).
 *
 * (c) 2007-2026 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.display.opengl4.view;

import java.util.ArrayList;
import java.util.List;

import gama.dev.DEBUG;
import gama.ui.display.opengl4.camera.IMultiListener;
import gama.ui.display.opengl4.renderer.IOpenGLRenderer;

/**
 * Android-compatible stub for GamaGLCanvas.
 * On desktop, this was an SWT Composite wrapping a JOGL GLWindow.
 * On Android, the GLSurfaceView handles the native GL surface;
 * this class provides the minimal API that the renderer helpers expect.
 */
public class GamaGLCanvas {

	static { DEBUG.OFF(); }

	/** The renderer owning this canvas. */
	private final IOpenGLRenderer renderer;

	/** The name for debug purposes. */
	final String name;

	/** Visible state. */
	volatile boolean visible;

	/** Width/height set by Android renderer. */
	private int surfaceWidth, surfaceHeight;

	/** Camera listeners. */
	private final List<IMultiListener> pendingCameraListeners = new ArrayList<>();

	/**
	 * Constructor.
	 */
	public GamaGLCanvas(final Object parent, final IOpenGLRenderer renderer, final String name) {
		this.renderer = renderer;
		this.name = name;
		this.visible = true;
	}

	/**
	 * Sets the surface dimensions (called from Android GLSurfaceView).
	 */
	public void setSurfaceSize(final int width, final int height) {
		this.surfaceWidth = width;
		this.surfaceHeight = height;
	}

	public int getSurfaceWidth() { return surfaceWidth; }
	public int getSurfaceHeight() { return surfaceHeight; }

	public boolean getVisibleStatus() { return visible; }

	public void updateVisibleStatus(final boolean v) { visible = v; }

	public void setVisible(final boolean v) { visible = v; }

	public boolean isDisposed() { return false; }

	/** On Android, getClientArea returns the surface dimensions. */
	public ClientArea getClientArea() { return new ClientArea(surfaceWidth, surfaceHeight); }

	/** Simple holder for client area dimensions. */
	public static class ClientArea {
		public final int width, height;
		public ClientArea(final int w, final int h) { this.width = w; this.height = h; }
	}

	/** On Android, there is no SWT Monitor; returns null. */
	public Object getMonitor() { return null; }

	/** Add camera listeners (no-op on Android — input handled differently). */
	public void addCameraListeners(final IMultiListener camera) {
		if (!pendingCameraListeners.contains(camera)) { pendingCameraListeners.add(camera); }
	}

	/** Remove camera listeners. */
	public void removeCameraListeners(final IMultiListener camera) {
		pendingCameraListeners.remove(camera);
	}

	public void startAnimator() {}
	public void pauseAnimator() {}
	public void resumeAnimator() {}

	public boolean setFocus() { return true; }
}

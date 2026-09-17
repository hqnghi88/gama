/*******************************************************************************************************
 *
 * IOpenGLRenderer.java, in gama.ui.display.opengl4, is part of the source code of the GAMA modeling and simulation platform
 * .
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.display.opengl4.renderer;

import gama.api.types.geometry.IPoint;
import gama.api.ui.displays.IDisplayData;
import gama.api.ui.displays.IDisplaySurface;
import gama.api.ui.displays.IGraphics;
import gama.ui.display.opengl4.OpenGL;
import gama.ui.display.opengl4.renderer.helpers.CameraHelper;
import gama.ui.display.opengl4.renderer.helpers.KeystoneHelper;
import gama.ui.display.opengl4.renderer.helpers.LightHelper;
import gama.ui.display.opengl4.renderer.helpers.PickingHelper;
import gama.ui.display.opengl4.renderer.helpers.SceneHelper;

/**
 * The Interface IOpenGLRenderer.
 * GLES 2.0 adaptation: GLEventListener dependency removed.
 * The lifecycle callbacks (init, dispose, reshape, display) are called by the Android-specific renderer.
 */
public interface IOpenGLRenderer extends IGraphics.ThreeD {

	/**
	 * Sets the canvas.
	 */
	default void setCanvas(Object canvas) {}

	/**
	 * Gets the canvas.
	 */
	default Object getCanvas() { return null; }

	/**
	 * Inits the scene.
	 */
	void initScene();

	/**
	 * Gets the width.
	 */
	double getWidth();

	/**
	 * Gets the height.
	 */
	double getHeight();

	/**
	 * Gets the real world point from window point.
	 */
	IPoint  getRealWorldPointFromWindowPoint(final IPoint mouse);

	/**
	 * Gets the surface.
	 */
	@Override
	IDisplaySurface getSurface();

	CameraHelper getCameraHelper();
	KeystoneHelper getKeystoneHelper();
	PickingHelper getPickingHelper();
	OpenGL getOpenGLHelper();
	LightHelper getLightHelper();
	SceneHelper getSceneHelper();

	default IDisplayData getData() { return getSurface() != null ? getSurface().getData() : null; }

	int getLayerWidth();
	int getLayerHeight();

	default boolean useShader() { return false; }

	boolean isDisposed();
	boolean hasDrawnOnce();

	default void onGLInitialized() {}
	default void onDrawFrame() {}
}
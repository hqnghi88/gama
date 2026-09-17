/*******************************************************************************************************
 *
 * TextDrawer.java, in gama.ui.display.opengl4, is part of the source code of the GAMA modeling and simulation platform
 * (v.2025-03).
 *
 * (c) 2007-2026 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.display.opengl4.scene.text;

import java.nio.DoubleBuffer;

import gama.api.types.color.IColor;
import gama.api.types.geometry.IPoint;
import gama.api.ui.layers.IDrawingAttributes;
import gama.dev.DEBUG;
import gama.ui.display.opengl4.OpenGL;
import gama.ui.display.opengl4.scene.ObjectDrawer;

/**
 * TextDrawer — Android stub.
 *
 * <p>
 * All JOGL graph/curve/font rendering has been removed. Text drawing methods are no-ops for now.
 * On Android, text rendering should use Android's Canvas/Paint or a dedicated text rendering library.
 * </p>
 */
public class TextDrawer extends ObjectDrawer<StringObject> {

	/**
	 * Instantiates a new text drawer.
	 *
	 * @param gl
	 *            the {@link OpenGL} helper that owns this drawer
	 */
	public TextDrawer(final OpenGL gl) {
		super(gl);
	}

	@Override
	protected void _draw(final StringObject s) {
		// TODO: Implement Android text rendering
		DEBUG.OUT("TextDrawer._draw: stub — text rendering not yet implemented on Android");
	}

	/**
	 * Public entry point for drawing a flat string. Stub on Android.
	 *
	 * @param text
	 *            the string to render
	 * @param attributes
	 *            the drawing attributes
	 * @param overlay
	 *            {@code true} for screen-space (overlay) rendering
	 */
	public void drawWithGraphLibraryPublic(final String text, final IDrawingAttributes attributes,
			final boolean overlay) {
		// TODO: Implement Android text rendering
		DEBUG.OUT("TextDrawer.drawWithGraphLibraryPublic: stub — text rendering not yet implemented on Android");
	}

	/**
	 * Draws the lateral (side) faces of extruded text. Stub on Android.
	 */
	public void drawSide() {
		// TODO: Implement Android text rendering
	}

	/**
	 * Draws the outline (border) of each glyph contour. Stub on Android.
	 */
	public void drawBorder() {
		// TODO: Implement Android text rendering
	}

	/**
	 * Draws the tessellated front or back face. Stub on Android.
	 *
	 * @param up
	 *            {@code true} for upward-facing (front) normal, {@code false} for downward (back)
	 */
	public void drawFaceFallback(final boolean up) {
		// TODO: Implement Android text rendering
	}

	/**
	 * Draws the lateral quad-strip side faces. Stub on Android.
	 *
	 * @param openGL
	 *            the {@link OpenGL} helper to draw into
	 */
	public void drawSideFallback(final OpenGL openGL) {
		// TODO: Implement Android text rendering
	}

	/**
	 * Draws the outline of each glyph contour. Stub on Android.
	 */
	public void drawBorderFallback() {
		// TODO: Implement Android text rendering
	}

	@Override
	public void dispose() {
		// No JOGL resources to clean up
	}
}

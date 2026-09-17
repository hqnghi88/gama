/*******************************************************************************************************
 *
 * IMultiListener.java, in gama.ui.display.opengl4, is part of the source code of the GAMA modeling and simulation platform
 * (v.2.0.0).
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama2 for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.display.opengl4.camera;

/**
 * The listener interface for receiving keyboard and mouse events.
 * On Android, input events are forwarded from the Android framework with generic parameters.
 */
public interface IMultiListener {

	default void mouseEntered(final int x, final int y) {}

	default void mouseExited(final int x, final int y) {}

	default void mouseMoved(final int x, final int y) {}

	default void mouseWheelMoved(final int x, final int y, final int rotation) {}

	default void mousePressed(final int x, final int y, final int button) {}

	default void mouseReleased(final int x, final int y, final int button) {}

	default void mouseDragged(final int x, final int y) {}

	default void mouseClicked(final int x, final int y, final int clickCount) {}

	default void keyPressed(final int keyCode, final char keyChar) {}

	default void keyReleased(final int keyCode, final char keyChar) {}

}

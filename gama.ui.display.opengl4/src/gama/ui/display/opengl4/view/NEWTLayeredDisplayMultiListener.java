/*******************************************************************************************************
 *
 * NEWTLayeredDisplayMultiListener.java, in gama.ui.display.opengl4, is part of the source code of the GAMA modeling and
 * simulation platform (v.2025-03).
 *
 * (c) 2007-2026 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.display.opengl4.view;

import gama.api.ui.displays.IDisplaySurface;
import gama.api.utils.interfaces.IDisposable;
import gama.dev.DEBUG;
import gama.ui.experiment.views.displays.LayeredDisplayDecorator;
import gama.ui.experiment.views.displays.LayeredDisplayMultiListener;

/**
 * A listener for NEWT events — Android no-op stub.
 * On Android, input events are handled by the Android framework directly.
 */
public class NEWTLayeredDisplayMultiListener implements IDisposable {

	static {
		DEBUG.OFF();
	}

	/** The delegate. */
	final LayeredDisplayMultiListener delegate;

	/**
	 * Instantiates a new NEWT layered display multi listener.
	 *
	 * @param deco
	 *            the deco
	 * @param surface
	 *            the surface
	 */
	public NEWTLayeredDisplayMultiListener(final LayeredDisplayDecorator deco, final IDisplaySurface surface) {
		delegate = new LayeredDisplayMultiListener(surface, deco);
		DEBUG.OUT("NEWTLayeredDisplayMultiListener: initialized as Android stub");
	}

	/**
	 * Dispose.
	 */
	@Override
	public void dispose() {
		DEBUG.OUT("NEWTLayeredDisplayMultiListener: dispose (no-op on Android)");
	}

	// ---- Android input stubs: forward raw values to delegate ----

	public void keyPressed(final int keyCode, final char keyChar, final boolean isControlDown,
			final boolean isShiftDown, final boolean isMetaDown) {
		if (!isControlDown && !isShiftDown && !isMetaDown && keyChar != 0) {
			delegate.keyPressed(keyChar);
		}
	}

	public void keyReleased(final int keyCode, final char keyChar, final boolean isControlDown,
			final boolean isShiftDown, final boolean isMetaDown) {
		if (!isControlDown && !isShiftDown && !isMetaDown && keyChar != 0) {
			delegate.keyReleased(keyChar);
		}
	}

	public void mouseEntered(final int x, final int y) {
		delegate.mouseEnter(x, y, false, 0);
	}

	public void mouseExited(final int x, final int y) {
		delegate.mouseExit(x, y, false, 0);
	}

	public void mouseMoved(final int x, final int y) {
		delegate.mouseMove(x, y, false);
	}

	public void mousePressed(final int x, final int y, final int button) {
		if (button == 3) {
			delegate.menuDetected(x, y);
		} else {
			delegate.mouseDown(x, y, button, false);
		}
	}

	public void mouseReleased(final int x, final int y, final int button) {
		delegate.mouseUp(x, y, button, false);
	}

	public void mouseDragged(final int x, final int y) {
		delegate.dragDetected(x, y);
	}

	public void focusGained() {
		delegate.focusGained();
	}

	public void focusLost() {
		delegate.focusLost();
	}

}

/*******************************************************************************************************
 *
 * SWTOpenGLDisplaySurface.java, in gama.ui.display.opengl4, is part of the source code of the GAMA modeling and
 * simulation platform (v.2025-03).
 *
 * (c) 2007-2026 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.display.opengl4.view;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import gama.annotations.display;
import gama.annotations.doc;
import gama.api.kernel.agent.IAgent;
import gama.api.runtime.GeneralSynchronizer;
import gama.api.types.geometry.GamaPointFactory;
import gama.api.types.geometry.IPoint;
import gama.api.types.geometry.IShape;
import gama.api.ui.IOutput;
import gama.api.ui.displays.IDisplayData;
import gama.api.ui.displays.IDisplaySurface;
import gama.api.ui.displays.IGraphics;
import gama.api.ui.displays.IGraphicsScope;
import gama.api.ui.layers.IDrawingAttributes;
import gama.api.ui.layers.IEventLayerListener;
import gama.api.ui.layers.ILayer;
import gama.api.ui.layers.ILayerManager;
import gama.api.utils.geometry.IEnvelope;
import gama.api.utils.geometry.GamaEnvelopeFactory;
import gama.core.outputs.display.LayerManager;
import gama.dev.DEBUG;
import gama.ui.display.opengl4.renderer.JOGLRenderer;

/**
 * Android-compatible version of SWTOpenGLDisplaySurface.
 */
@display(value = { "opengl4" }, is3D = true)
@doc("Displays that uses the OpenGL technology to display their layers in 3D")
public class SWTOpenGLDisplaySurface implements IDisplaySurface.OpenGL {

	static { DEBUG.OFF(); }

	JOGLRenderer renderer;
	protected boolean zoomFit = true;
	Set<IEventLayerListener> listeners = new HashSet<>();
	protected final IOutput.Display output;
	protected final ILayerManager layerManager;
	protected IGraphicsScope scope;
	protected IPoint world_position = GamaPointFactory.create();
	protected volatile boolean visibleRegionsInvalidated;
	protected double zoomLevel = 1;
	protected volatile boolean visible = true;
	protected IDisplaySurface.OpenGL delegate;

	public SWTOpenGLDisplaySurface(final IOutput.Display output, final Object parent) {
		this.output = output;
		this.layerManager = new LayerManager(this, output);
		output.setSurface(this);
		if (output.getData() != null) {
			output.getData().addListener(this);
		}
	}

	public void setDelegate(final IDisplaySurface.OpenGL delegate) {
		this.delegate = delegate;
	}

	@Override
	public ILayerManager getManager() { return layerManager; }

	@Override
	public IDisplayData getData() { return output != null ? output.getData() : null; }

	@Override
	public IOutput.Display getOutput() { return output; }

	@Override
	public IGraphicsScope getScope() { return scope; }

	public void setScope(final IGraphicsScope scope) { this.scope = scope; }

	@Override
	public IGraphics getIGraphics() { return renderer; }

	@Override
	public double getZoomLevel() { return zoomLevel; }

	@Override
	public boolean isVisible() { return visible; }

	@Override
	public boolean isDisposed() { return false; }

	public void invalidateVisibleRegions() { visibleRegionsInvalidated = true; }

	@Override
	public void updateDisplay(final boolean force, final GeneralSynchronizer synchronizer) {
		if (delegate != null) { delegate.updateDisplay(force, synchronizer); return; }
	}

	@Override
	public void zoomFit() { zoomFit = true; }

	@Override
	public void zoomIn() { zoomLevel *= 1.2; }

	@Override
	public void zoomOut() { zoomLevel /= 1.2; }

	@Override
	public void toggleLock() {}

	@Override
	public Rectangle getBoundsForRobotSnapshot() { return new Rectangle(0, 0, 800, 600); }

	@Override
	public void setMousePosition(final int x, final int y) { world_position = GamaPointFactory.create(x, y, 0); }

	@Override
	public void draggedTo(final int x, final int y) { world_position = GamaPointFactory.create(x, y, 0); }

	@Override
	public Collection<IAgent> selectAgent(final int x, final int y) { return Collections.emptyList(); }

	@Override
	public void selectAgent(final IDrawingAttributes attr) {}

	@Override
	public void selectionIn(final IEnvelope env) {}

	@Override
	public IEnvelope getROIDimensions() { return GamaEnvelopeFactory.of(0, 0, 800, 600); }

	@Override
	public void setPaused(final boolean paused) {}

	@Override
	public boolean canTriggerContextualMenu() { return false; }

	@Override
	public boolean isArrowRedefined() { return false; }

	@Override
	public boolean isEscRedefined() { return false; }

	@Override
	public void changed(final IDisplayData.Changes changes, final Object source) {}

	@Override
	public void outputReloaded() { zoomFit = true; }

	@Override
	public void layersChanged() {}

	@Override
	public void addListener(final IEventLayerListener listener) { listeners.add(listener); }

	@Override
	public void removeListener(final IEventLayerListener listener) { listeners.remove(listener); }

	@Override
	public Collection<IEventLayerListener> getLayerListeners() { return listeners; }

	@Override
	public IEnvelope getVisibleRegionForLayer(final ILayer layer) { return null; }

	@Override
	public int getFPS() { return 0; }

	@Override
	public IPoint getModelCoordinates() { return world_position; }

	@Override
	public IPoint getWindowCoordinates() { return world_position; }

	@Override
	public IPoint getModelCoordinatesFrom(final int x, final int y, final java.awt.Point windowDimensions,
			final java.awt.Point envDimensions) {
		return GamaPointFactory.create(x, y, 0);
	}

	@Override
	public void setSize(final int w, final int h) {}

	@Override
	public int getWidth() { return 800; }

	@Override
	public int getHeight() { return 600; }

	@Override
	public double getEnvWidth() { return output != null && output.getData() != null ? output.getData().getEnvWidth() : 800; }

	@Override
	public double getEnvHeight() { return output != null && output.getData() != null ? output.getData().getEnvHeight() : 600; }

	@Override
	public double getDisplayWidth() { return getWidth(); }

	@Override
	public double getDisplayHeight() { return getHeight(); }

	@Override
	public void dispatchMouseEvent(final int x, final int y, final int button) {}

	@Override
	public void dispatchKeyEvent(final char c) {}

	@Override
	public void dispatchSpecialKeyEvent(final int keyCode) {}

	@Override
	public void selectAgentsAroundMouse() {}

	@Override
	public void dispose() {
		if (output != null && output.getData() != null) { output.getData().removeListener(this); }
	}

	public void outputReloaded(final boolean reload) { outputReloaded(); }

	@Override
	public void runAndUpdate(final Runnable r) {
		if (r != null) { r.run(); }
		updateDisplay(true, null);
	}

	@Override
	public void getModelCoordinatesInfo(final StringBuilder receiver) {}

	@Override
	public void setMenuManager(final Object menu) {}

	@Override
	public void focusOn(final IShape geometry) {}
}

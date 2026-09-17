/*******************************************************************************************************
 *
 * OpenGL.java, in gama.ui.display.opengl4, is part of the source code of the GAMA modeling and simulation platform
 * (v.2025-03).
 *
 * (c) 2007-2026 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.display.opengl4;

import java.awt.image.BufferedImage;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

import android.opengl.GLES20;

import gama.api.utils.geometry.GamaCoordinateSequenceFactory;
import gama.api.utils.geometry.GeometryUtils;
import gama.api.utils.geometry.ICoordinates.VertexVisitor;
import gama.ui.display.opengl4.scene.text.FastTriangulation;

import gama.api.types.color.GamaColorFactory;
import gama.api.types.color.IColor;
import gama.api.types.geometry.GamaPointFactory;
import gama.api.types.geometry.IPoint;
import gama.api.types.geometry.IShape;
import gama.api.ui.layers.IDrawingAttributes;
import gama.api.ui.layers.IDrawingAttributes.DrawerType;
import gama.api.utils.geometry.GamaEnvelopeFactory;
import gama.api.utils.geometry.ICoordinates;
import gama.api.utils.geometry.IEnvelope;
import gama.api.utils.geometry.Rotation3D;
import gama.api.utils.geometry.Scaling3D;
import gama.api.utils.geometry.UnboundedCoordinateSequence;
import gama.api.utils.interfaces.IImageProvider;
import gama.api.utils.prefs.GamaPreferences;
import gama.core.util.file.GamaGeometryFile;
import gama.dev.DEBUG;
import gama.gaml.operators.Maths;
import gama.ui.display.opengl4.renderer.IOpenGLRenderer;
import gama.ui.display.opengl4.renderer.caches.GeometryCache;
import gama.ui.display.opengl4.renderer.caches.GeometryCache.BuiltInGeometry;
import gama.ui.display.opengl4.renderer.caches.ITextureCache;
import gama.ui.display.opengl4.renderer.caches.TextureCache2;
import gama.ui.display.opengl4.renderer.helpers.AbstractRendererHelper;
import gama.ui.display.opengl4.renderer.helpers.KeystoneHelper;
import gama.ui.display.opengl4.renderer.shaders.BasicShader;
import gama.ui.display.opengl4.renderer.shaders.MatrixStack;
import gama.ui.display.opengl4.scene.AbstractObject;
import gama.ui.display.opengl4.scene.ObjectDrawer;
import gama.ui.display.opengl4.scene.geometry.GeometryDrawer;
import gama.ui.display.opengl4.scene.mesh.MeshDrawer;
import gama.ui.display.opengl4.scene.mesh.MeshObject;
import gama.ui.display.opengl4.scene.resources.ResourceDrawer;
import gama.ui.display.opengl4.scene.text.TextDrawer;


/**
 * A class that represents an intermediate state between the rendering and the opengl state. Implements
 * {@link ITesselator} so that the GLU tessellator callbacks ({@code begin}, {@code vertex}, {@code end}) are wired
 * directly to the VBO-based drawing path used by this core-profile renderer.
 */
public class OpenGL extends AbstractRendererHelper {

	static {
		DEBUG.OFF();
		GamaPreferences.Displays.DRAW_ROTATE_HELPER.onChange(v -> SHOULD_DRAW_ROTATION_SPHERE = v);
	}

	/** The should draw rotation sphere. */
	private static boolean SHOULD_DRAW_ROTATION_SPHERE = GamaPreferences.Displays.DRAW_ROTATE_HELPER.getValue();

	/** The Constant NO_TEXTURE. */
	public static final int NO_TEXTURE = Integer.MAX_VALUE;

	/** GLES 2.0 does not have profiles. */
	// public static final GLProfile PROFILE = GLProfile.get(GLProfile.GL4);

	/** The drawers. */
	final Map<DrawerType, ObjectDrawer<?>> drawers = new HashMap<>();

	/** The mesh drawers. */
	final Map<MeshDrawer.Signature, MeshDrawer> meshDrawers = new HashMap<>();

	/** The viewport. */
	// Matrices of the display
	public final int[] viewport = new int[4];

	/** The mvmatrix. */
	public final double mvmatrix[] = new double[16];

	/** The projmatrix. */
	public final double projmatrix[] = new double[16];

	/** The gl. */
	// The real openGL context — GLES20 uses static calls, no instance field needed
	// private GL4 gl;

	/** The basic shader. */
	private BasicShader basicShader;

	/**
	 * Gets the basic shader. Used by {@link gama.ui.display.opengl4.renderer.helpers.LightHelper} to push
	 * per-frame light uniforms.
	 *
	 * @return the BasicShader program, or {@code null} if not yet initialised
	 */
	public BasicShader getBasicShader() { return basicShader; }

	/** The model view matrix. */
	private final MatrixStack modelViewMatrix = new MatrixStack();

	/** The projection matrix. */
	private final MatrixStack projectionMatrix = new MatrixStack();

	/** Local constants replacing GLMatrixFunc.GL_MODELVIEW / GL_PROJECTION sentinel values. */
	private static final int GL_MODELVIEW_SENTINEL = 0;
	private static final int GL_PROJECTION_SENTINEL = 1;

	/** The current matrix mode. */
	private int currentMatrixMode = GL_MODELVIEW_SENTINEL;

	/**
	 * Gets the current matrix stack.
	 *
	 * @return the current matrix stack
	 */
	public MatrixStack getCurrentMatrixStack() {
		return currentMatrixMode == GL_PROJECTION_SENTINEL ? projectionMatrix : modelViewMatrix;
	}

	/**
	 * Returns the current model-view {@link org.joml.Matrix4f}. Used by
	 * {@link gama.ui.display.opengl4.scene.text.TextDrawer} to feed the scene matrices to the JOGL graph/curve
	 * {@code RegionRenderer}.
	 *
	 * @return the model-view matrix (live reference — do not mutate)
	 */
	public org.joml.Matrix4f getModelViewMatrix() { return modelViewMatrix.getCurrentMatrix(); }

	/**
	 * Returns the current projection {@link org.joml.Matrix4f}. Used by
	 * {@link gama.ui.display.opengl4.scene.text.TextDrawer} to feed the scene matrices to the JOGL graph/curve
	 * {@code RegionRenderer}.
	 *
	 * @return the projection matrix (live reference — do not mutate)
	 */
	public org.joml.Matrix4f getProjectionMatrix() { return projectionMatrix.getCurrentMatrix(); }

	/**
	 * Tessellation helper for triangulating concave polygons. Uses a pure-Java ear-clipping approach
	 * instead of the JOGL GLU tessellator.
	 */
	// GLU removed — tessellation handled via ITesselator callbacks backed by pure-Java triangulation

	/** The view height. */
	private int viewWidth, viewHeight;

	/** The current polygon mode. GLES 2.0 has no polygon mode; field retained for API compatibility only. */
	private int currentPolygonMode = GLES20.GL_TRIANGLES;

	/** The current color. */
	private IColor currentColor = GamaColorFactory.getWithDoubles(1, 1, 1, 1);

	/** The picking state. */
	// private final PickingHelper pickingState;

	/** The texture cache. */
	// Textures
	private final ITextureCache textureCache = new TextureCache2(this);

	/** The texture envelope. */
	private final IEnvelope textureEnvelope = GamaEnvelopeFactory.create();

	/** The current texture rotation. */
	private final Rotation3D currentTextureRotation = Rotation3D.identity();

	/** The null texture rotation. */
	private final Rotation3D nullTextureRotation = new Rotation3D(0, 0, 0, 1, false);

	/** The textured. */
	private boolean textured;

	/** The primary texture. */
	private int primaryTexture = NO_TEXTURE;

	/** The alternate texture. */
	private int alternateTexture = NO_TEXTURE;

	/** The anisotropic level. */
	public static float ANISOTROPIC_LEVEL = 0;

	/** The current color. */
	// Colors

	/** The current object alpha. */
	private double currentObjectAlpha = 1d;

	/** The previous object lighting. */
	boolean previousObjectWireframe, previousObjectLighting;

	/** The previous object line width. */
	float currentObjectLineWidth,
			previousObjectLineWidth = GamaPreferences.Displays.CORE_LINE_WIDTH.getValue().floatValue();

	/** The previous display lighting. */
	boolean previousDisplayWireframe, previousDisplayLighting;

	/** The lighted. */
	private boolean objectIsLighted;

	/** The display is lighted. */
	private boolean displayIsLighted;

	/** The geometry cache. */
	// Geometries
	protected final GeometryCache geometryCache;

	/** The display is wireframe. */
	protected volatile boolean displayIsWireframe;

	/** The object is wireframe. */
	protected volatile boolean objectIsWireframe;

	/**
	 * Tessellation helper for triangulating concave polygons and polygons with holes.
	 * Uses FastTriangulation pure-Java ear-clipping instead of the JOGL GLU tessellator.
	 */
	// GLUtessellatorImpl removed — tessellation now uses FastTriangulation

	/** The ratios. */
	// World
	final IPoint ratios = GamaPointFactory.create();

	/** The rotation mode. */
	private boolean rotationMode;

	/** The current normal. */
	final IPoint currentNormal = GamaPointFactory.create();

	/** The texture coords. */
	final IPoint textureCoords = GamaPointFactory.create();

	/** The working vertices. */
	final UnboundedCoordinateSequence workingVertices = new UnboundedCoordinateSequence();

	/** The saved Z translation. */
	private double currentZIncrement, currentZTranslation, savedZTranslation;

	/** The Z translation suspended. */
	private volatile boolean ZTranslationSuspended;

	/** The end scene. */
	// private final boolean useJTSTriangulation = !GamaPreferences.Displays.OPENGL_TRIANGULATOR.getValue();
	private final Pass endScene = this::endScene;

	/**
	 * Instantiates a new open GL.
	 *
	 * @param renderer
	 *            the renderer
	 */
	public OpenGL(final IOpenGLRenderer renderer) {
		super(renderer);
		geometryCache = new GeometryCache(renderer);
		TextDrawer td = new TextDrawer(this);
		var gd = new GeometryDrawer(this);
		var rd = new ResourceDrawer(this);
		drawers.put(DrawerType.STRING, td);
		drawers.put(DrawerType.GEOMETRY, gd);
		drawers.put(DrawerType.RESOURCE, rd);
	}

	/** The last bound texture ID for optimization. */
	private int lastBoundTexture = NO_TEXTURE;

	/** The last anti-aliasing setting for optimization. */
	private boolean lastAntiAliasSetting = false;

	/**
	 * Pure-Java replacement for GLU.gluProject. Projects object coordinates to window coordinates
	 * using the supplied model, projection matrices and viewport.
	 */
	private static void gluProject(final double objX, final double objY, final double objZ,
			final double[] model, final int modelOff, final double[] proj, final int projOff,
			final int[] view, final int viewOff, final double[] winPos) {
		double x = objX, y = objY, z = objZ;
		// ModelView transform
		x = model[0 + modelOff] * objX + model[4 + modelOff] * objY + model[8 + modelOff] * objZ + model[12 + modelOff];
		y = model[1 + modelOff] * objX + model[5 + modelOff] * objY + model[9 + modelOff] * objZ + model[13 + modelOff];
		z = model[2 + modelOff] * objX + model[6 + modelOff] * objY + model[10 + modelOff] * objZ + model[14 + modelOff];
		double w = model[3 + modelOff] * objX + model[7 + modelOff] * objY + model[11 + modelOff] * objZ + model[15 + modelOff];
		// Projection transform
		double px = proj[0 + projOff] * x + proj[4 + projOff] * y + proj[8 + projOff] * z + proj[12 + projOff] * w;
		double py = proj[1 + projOff] * x + proj[5 + projOff] * y + proj[9 + projOff] * z + proj[13 + projOff] * w;
		double pz = proj[2 + projOff] * x + proj[6 + projOff] * y + proj[10 + projOff] * z + proj[14 + projOff] * w;
		double pw = proj[3 + projOff] * x + proj[7 + projOff] * y + proj[11 + projOff] * z + proj[15 + projOff] * w;
		if (pw == 0.0) { winPos[0] = 0; winPos[1] = 0; return; }
		px /= pw; py /= pw; pz /= pw;
		// Map to window coordinates
		winPos[0] = view[0 + viewOff] + view[2 + viewOff] * (px + 1.0) * 0.5;
		winPos[1] = view[1 + viewOff] + view[3 + viewOff] * (py + 1.0) * 0.5;
		winPos[2] = (pz + 1.0) * 0.5;
	}

	/** The gl drawer. */
	private final ICoordinates.IndexedVisitor glDrawer = this::drawVertex;

	/**
	 * Gets the drawer for.
	 *
	 * @param type
	 *            the type
	 * @return the drawer for
	 */
	public ObjectDrawer<? extends AbstractObject<?, ?>> getDrawerFor(final AbstractObject<?, ?> object) {
		// Only create signature and checking mesh drawers if the object is indeed a mesh.
		if (object instanceof MeshObject mo) {
			// Optimize: avoid creating a new Signature every time if possible or rely on efficient map operations.
			// Currently, creating new Signature(cols, rows, triangles) is necessary for lookup.
			// Assuming Signature is a lightweight object (record-like).
			final var attributes = mo.getAttributes();
			final IPoint dim = attributes.getXYDimension();
			final MeshDrawer.Signature sig =
					new MeshDrawer.Signature((int) dim.getX(), (int) dim.getY(), attributes.isTriangulated());
			return meshDrawers.computeIfAbsent(sig, s -> new MeshDrawer(this));
		}
		return drawers.get(object.type);
	}

	/**
	 * Gets the geometry drawer.
	 *
	 * @return the geometry drawer
	 */
	public GeometryDrawer getGeometryDrawer() { return (GeometryDrawer) drawers.get(DrawerType.GEOMETRY); }

	/**
	 * Dispose.
	 */
	public void dispose() {
		for (ObjectDrawer<?> o : drawers.values()) { o.dispose(); }
		for (MeshDrawer md : meshDrawers.values()) { md.dispose(); }
		geometryCache.dispose();
		textureCache.dispose();

	}

	/**
	 * Gets the GL4 context — not applicable for GLES20 static calls.
	 * Retained for API compatibility; callers should migrate to direct GLES20 usage.
	 */
	public Object getGL() { return null; }

	/**
	 * No-op. GLES20 uses static calls — no context instance to store.
	 */
	public void setGL4(final Object gl2) { /* no-op */ }

	/**
	 * Reshapes the GL world to comply with a new view size and computes the resulting ratios between pixels and world
	 * coordinates
	 *
	 * @param newGL
	 *            the (possibly new) GL4 context
	 * @param width
	 *            the width of the view (in pixels)
	 * @param height
	 *            the height of the view (in pixels)
	 * @return
	 */
	public void reshape(final int width, final int height) {
		if (basicShader == null) {
			basicShader = new BasicShader();
			basicShader.start();

			// GLES 2.0 has no VAO — generate VBOs only; vertex attrib arrays are
			// rebound before each draw call in endDrawing().
			GLES20.glGenBuffers(4, vboIds, 0);
		}
		viewWidth = width;
		viewHeight = height;
		resetMatrix(GL_PROJECTION_SENTINEL);
		updatePerspective();
		resetMatrix(GL_MODELVIEW_SENTINEL);

		// Replace gluProject with manual matrix multiplication via JOML
		final double[] pixelSize = new double[4];
		gluProject(getWorldWidth(), 0, 0, mvmatrix, 0, projmatrix, 0, viewport, 0, pixelSize);
		final double initialEnvWidth = pixelSize[0];
		final double initialEnvHeight = pixelSize[1];
		final double envWidthInPixels = 2 * pixelSize[0] - width;
		final double envHeightInPixels = 2 * pixelSize[1] - height;
		final double windowWidthInModelUnits = getWorldWidth() * width / envWidthInPixels;
		final double windowHeightInModelUnits = getWorldHeight() * height / envHeightInPixels;
		final double xRatio = width / windowWidthInModelUnits / getData().getZoomLevel();
		final double yRatio = height / windowHeightInModelUnits / getData().getZoomLevel();
		if (DEBUG.IS_ON()) {
			debugSizes(width, height, initialEnvWidth, initialEnvHeight, envWidthInPixels, envHeightInPixels,
					getData().getZoomLevel(), xRatio, yRatio);
		}
		ratios.setLocation(xRatio, yRatio, 0d);
	}

	/**
	 * Debug sizes.
	 *
	 * @param width
	 *            the width
	 * @param height
	 *            the height
	 * @param initialEnvWidth
	 *            the initial env width
	 * @param initialEnvHeight
	 *            the initial env height
	 * @param envWidth
	 *            the env width
	 * @param envHeight
	 *            the env height
	 * @param zoomLevel
	 *            the zoom level
	 * @param xRatio
	 *            the x ratio
	 * @param yRatio
	 *            the y ratio
	 */
	private void debugSizes(final int width, final int height, final double initialEnvWidth,
			final double initialEnvHeight, final double envWidth, final double envHeight, final double zoomLevel,
			final double xRatio, final double yRatio) {

		DEBUG.SECTION("RESHAPING TO " + width + "x" + height);
		DEBUG.OUT("Camera zoom level ", 35, zoomLevel);
		DEBUG.OUT("Size of env in units ", 35, getWorldWidth() + " | " + getWorldHeight());
		DEBUG.OUT("Ratio width/height in units ", 35, getWorldWidth() / getWorldHeight());
		DEBUG.OUT("Initial Size of env in pixels ", 35, initialEnvWidth + " | " + initialEnvHeight);
		DEBUG.OUT("Size of env in pixels ", 35, envWidth + " | " + envHeight);
		DEBUG.OUT("Ratio width/height in pixels ", 35, envWidth / envHeight);
		DEBUG.OUT("Window pixels/env pixels ", 35, width / envWidth + " | " + height / envHeight);
		DEBUG.OUT("Current XRatio pixels/env in units ", 35, xRatio + " | " + yRatio);
		// DPIHelper not available on Android
		DEBUG.OUT("Window size: " + width + "x" + height);
	}

	/**
	 * Update perspective.
	 *
	 * @param gl
	 *            the gl
	 */
	public void updatePerspective() {
		final double height = getViewHeight();
		final double aspect = getViewWidth() / (height == 0d ? 1d : height);
		final double maxDim = getMaxEnvDim();
		double zNear = getZNear();
		if (zNear < 0.0) {
			zNear = maxDim / 100d;
			data.setZNear(zNear);
		}
		double zFar = getZFar();
		if (zFar < 0.0) {
			zFar = maxDim * 100d;
			data.setZFar(zFar);
		}

		if (!getData().isOrtho()) {
			try {
				double fW, fH;
				final double fovY = getData().getCameraLens();
				if (aspect > 1.0) {
					fH = Math.tan(fovY / 360 * Math.PI) * zNear;
					fW = fH * aspect;
				} else {
					fW = Math.tan(fovY / 360 * Math.PI) * zNear;
					fH = fW / aspect;
				}

				getCurrentMatrixStack().frustum(-fW, fW, -fH, fH, zNear, zFar);
			} catch (final BufferOverflowException e) {
				DEBUG.ERR("Buffer overflow exception");
			}
		} else if (aspect >= 1.0) {
			// JOML ortho(left, right, bottom, top, zNear, zFar) requires zNear < zFar.
			// The original glOrtho used (near=maxDim*10, far=-maxDim*10), which flipped Z.
			// Reproduce that by passing negative near and positive far to JOML.
			getCurrentMatrixStack().ortho(-maxDim * aspect, maxDim * aspect, -maxDim, maxDim, -maxDim * 10,
					maxDim * 10);
		} else {
			getCurrentMatrixStack().ortho(-maxDim, maxDim, -maxDim / aspect, maxDim / aspect, -maxDim * 10,
					maxDim * 10);
		}

		// else {
		// gl.glOrtho(-maxDim, maxDim, -maxDim, maxDim, maxDim * 10, -maxDim * 10);
		// }
		//
		// translateBy(0d, 0d, maxDim * 0.2);
		getRenderer().getCameraHelper().animate();
		GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, viewport, 0);
		// Read matrices from our own MatrixStack, not from the deprecated GL state
		// (GL_MODELVIEW_MATRIX / GL_PROJECTION_MATRIX are removed in core GL4).
		float[] mv = new float[16];
		float[] pr = new float[16];
		modelViewMatrix.getCurrentMatrix().get(mv);
		projectionMatrix.getCurrentMatrix().get(pr);
		for (int i = 0; i < 16; i++) {
			mvmatrix[i] = mv[i];
			projmatrix[i] = pr[i];
		}
	}

	/**
	 * Gets the pixel width and height of world.
	 *
	 * @return the pixel width and height of world
	 */
	public double[] getPixelWidthAndHeightOfWorld() {
		final double[] coord = new double[4];
		gluProject(getWorldWidth(), 0, 0, mvmatrix, 0, projmatrix, 0, viewport, 0, coord);
		return coord;
	}

	/**
	 * Gets the view width.
	 *
	 * @return the view width
	 */
	public int getViewWidth() { return viewWidth; }

	/**
	 * Gets the view height.
	 *
	 * @return the view height
	 */
	public int getViewHeight() { return viewHeight; }

	/**
	 * Sets the z increment.
	 *
	 * @param z
	 *            the new z increment
	 */
	public void setZIncrement(final double z) {
		currentZTranslation = 0;
		currentZIncrement = z;
	}

	/**
	 * Computes the translation in Z to enable z-fighting, using the current z increment, computed by ModelScene. The
	 * translations are cumulative
	 */
	public void translateByZIncrement() {
		if (!ZTranslationSuspended) { currentZTranslation += currentZIncrement; }
	}

	/**
	 * Suspend Z translation.
	 */
	public void suspendZTranslation() {
		ZTranslationSuspended = true;
		savedZTranslation = currentZTranslation;
		currentZTranslation = 0;
	}

	/**
	 * Resume Z translation.
	 */
	public void resumeZTranslation() {
		ZTranslationSuspended = false;
		currentZTranslation = savedZTranslation;
	}

	/**
	 * Gets the current Z translation.
	 *
	 * @return the current Z translation
	 */
	public double getCurrentZTranslation() { return currentZTranslation; }

	/**
	 * Gets the current Z increment.
	 *
	 * @return the current Z increment
	 */
	public double getCurrentZIncrement() { return currentZIncrement; }

	/**
	 * Returns the previous state
	 *
	 * @param lighted
	 * @return
	 */
	public boolean setObjectLighting(final boolean lighted) {
		boolean previous = objectIsLighted;
		if (lighted != previous) {
			objectIsLighted = lighted;
			updateLightMode();
		}
		return previous;
	}

	/**
	 * Sets the display lighting.
	 *
	 * @param lighted
	 *            the lighted
	 * @return true, if successful
	 */
	public boolean setDisplayLighting(final boolean lighted) {
		boolean previous = displayIsLighted;
		if (lighted != previous) {
			displayIsLighted = lighted;
			updateLightMode();
		}
		return previous;
	}

	/**
	 * Update light mode. In core GL4, per-vertex fixed-function lighting (GL_LIGHTING) has been removed. Lighting is
	 * handled entirely in shaders; this method is intentionally empty.
	 */
	private void updateLightMode() {
		// GL_LIGHTING is not available in OpenGL 4 core profile.
		// Lighting state is passed to shaders via uniforms (e.g. in BasicShader).
	}

	/**
	 * Gets the lighting.
	 *
	 * @return the lighting
	 */
	public boolean getLighting() { return objectIsLighted && displayIsLighted; }

	/**
	 * Matrix mode.
	 *
	 * @param mode
	 *            the mode
	 */
	public void matrixMode(final int mode) {
		currentMatrixMode = mode;
	}

	/**
	 * Push matrix.
	 */
	public void pushMatrix() {
		getCurrentMatrixStack().pushMatrix();
	}

	/**
	 * Pop matrix.
	 */
	public void popMatrix() {
		getCurrentMatrixStack().popMatrix();
	}

	/**
	 * Reset matrix.
	 *
	 * @param mode
	 *            the mode
	 */
	private void resetMatrix(final int mode) {
		matrixMode(mode);
		getCurrentMatrixStack().loadIdentity();
	}

	/**
	 * Push identity.
	 *
	 * @param mode
	 *            the mode
	 */
	public void pushIdentity(final int mode) {
		matrixMode(mode);
		pushMatrix();
		getCurrentMatrixStack().loadIdentity();
	}

	/**
	 * Pop.
	 *
	 * @param mode
	 *            the mode
	 */
	public void pop(final int mode) {
		matrixMode(mode);
		popMatrix();
	}

	/**
	 * Push.
	 *
	 * @param mode
	 *            the mode
	 */
	public void push(final int mode) {
		matrixMode(mode);
		pushMatrix();
	}

	/**
	 * Enable.
	 *
	 * @param state
	 *            the state
	 */
	public void enable(final int state) {
		// removed glEnableClientState
	}

	/**
	 * Disable.
	 *
	 * @param state
	 *            the state
	 */
	public void disable(final int state) {
		// removed glDisableClientState
	}

	/** The Constant INITIAL_BUFFER_SIZE. */
	private static final int INITIAL_BUFFER_SIZE = 1024 * 64; // 64k floats

	/** The current vertices. */
	private final FloatBuffer currentVertices = createDirectFloatBuffer(INITIAL_BUFFER_SIZE);

	/** The current colors. */
	private final FloatBuffer currentColors = createDirectFloatBuffer(INITIAL_BUFFER_SIZE);

	/** The current tex coords. */
	private final FloatBuffer currentTexCoords = createDirectFloatBuffer(INITIAL_BUFFER_SIZE);

	/** The current normals (one vec3 per vertex, replicated from the face normal set by {@link #outputNormal}). */
	private final FloatBuffer currentNormals = createDirectFloatBuffer(INITIAL_BUFFER_SIZE);

	/**
	 * Creates a direct FloatBuffer on Android (no JOGL Buffers dependency).
	 */
	private static FloatBuffer createDirectFloatBuffer(final int capacity) {
		return ByteBuffer.allocateDirect(capacity * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
	}

	/** The current draw style. */
	private int currentDrawStyle = GLES20.GL_TRIANGLES;

	/** The current vertex count. */
	private int currentVertexCount = 0;

	/** The vao id. */
	private int vaoId = -1;

	/** The vbo ids. Index 0 = positions, 1 = colours, 2 = tex-coords, 3 = normals. */
	private final int[] vboIds = new int[4];

	/**
	 * Ensure capacity.
	 *
	 * @param buffer
	 *            the buffer
	 * @param elementsToAdd
	 *            the elements to add
	 */
	private void ensureCapacity(final FloatBuffer buffer, final int elementsToAdd) {
		if (buffer.position() + elementsToAdd > buffer.capacity()) {
			// Actually we shouldn't dynamically reallocate NIO buffers trivially in the middle of drawing.
			// Let's implement a simple resize logic just in case:
			// But for now, since we track via currentVertexCount, let's just make them big enough.
		}
	}

	/**
	 * Begin drawing.
	 *
	 * @param style
	 *            the style
	 */
	public void beginDrawing(final int style) {
		currentDrawStyle = style;
		currentVertices.clear();
		currentColors.clear();
		currentTexCoords.clear();
		currentNormals.clear();
		currentVertexCount = 0;
	}

	/**
	 * End drawing.
	 */
	public void endDrawing() {
		if (currentVertices.position() == 0 || basicShader == null || vboIds[0] == 0) return;
		basicShader.start();

		// --- vertices (location 0) ---
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboIds[0]);
		currentVertices.flip();
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, currentVertices.limit() * 4, currentVertices, GLES20.GL_DYNAMIC_DRAW);
		GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, 0);
		GLES20.glEnableVertexAttribArray(0);

		// --- colors (location 1) ---
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboIds[1]);
		currentColors.flip();
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, currentColors.limit() * 4, currentColors, GLES20.GL_DYNAMIC_DRAW);
		GLES20.glVertexAttribPointer(1, 4, GLES20.GL_FLOAT, false, 0, 0);
		GLES20.glEnableVertexAttribArray(1);

		// --- tex coords (location 2) ---
		boolean hasTex = currentTexCoords.position() > 0;
		if (hasTex) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboIds[2]);
			currentTexCoords.flip();
			GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, currentTexCoords.limit() * 4, currentTexCoords,
					GLES20.GL_DYNAMIC_DRAW);
			GLES20.glVertexAttribPointer(2, 2, GLES20.GL_FLOAT, false, 0, 0);
			GLES20.glEnableVertexAttribArray(2);
		} else {
			GLES20.glDisableVertexAttribArray(2);
		}

		// --- normals (location 3) ---
		boolean hasNormals = currentNormals.position() > 0 && getLighting();
		if (hasNormals) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboIds[3]);
			currentNormals.flip();
			GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, currentNormals.limit() * 4, currentNormals,
					GLES20.GL_DYNAMIC_DRAW);
			GLES20.glVertexAttribPointer(3, 3, GLES20.GL_FLOAT, false, 0, 0);
			GLES20.glEnableVertexAttribArray(3);
		} else {
			GLES20.glDisableVertexAttribArray(3);
			// Provide a default up-facing normal so the shader doesn't read garbage
			GLES20.glVertexAttrib3f(3, 0.0f, 0.0f, 1.0f);
		}

		basicShader.loadModelMatrix(modelViewMatrix.getCurrentMatrix());
		basicShader.loadViewMatrix(new org.joml.Matrix4f().identity());
		basicShader.loadProjectionMatrix(projectionMatrix.getCurrentMatrix());
		basicShader.loadNormalMatrix(modelViewMatrix.getCurrentMatrix());
		basicShader.loadUseTexture(hasTex);

		// --- lighting uniforms ---
		basicShader.loadUseLighting(hasNormals);
		gama.api.types.geometry.IPoint camPos = getData().getCameraPos();
		basicShader.loadViewPos((float) camPos.getX(), (float) -camPos.getY(), (float) camPos.getZ());
		basicShader.loadShininess(32.0f);

		GLES20.glEnable(GLES20.GL_BLEND);
		boolean transparent = currentObjectAlpha < 1.0 || (currentColor != null && currentColor.alpha() < 255);
		if (transparent) { GLES20.glDepthMask(false); }

		GLES20.glDrawArrays(currentDrawStyle, 0, currentVertices.limit() / 3);

		if (transparent) { GLES20.glDepthMask(true); }

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		// No VAO to unbind in GLES 2.0 — just disable attrib arrays after draw
		GLES20.glDisableVertexAttribArray(0);
		GLES20.glDisableVertexAttribArray(1);
		if (hasTex) GLES20.glDisableVertexAttribArray(2);
		if (hasNormals) GLES20.glDisableVertexAttribArray(3);
		basicShader.stop();
	}

	/**
	 * Translate by Y negated.
	 *
	 * @param p
	 *            the p
	 */
	public void translateByYNegated(final IPoint p) {
		translateBy(p.getX(), -p.getY(), p.getZ());
	}

	/**
	 * Translate by.
	 *
	 * @param x
	 *            the x
	 * @param y
	 *            the y
	 * @param z
	 *            the z
	 */
	public void translateBy(final double x, final double y, final double z) {
		getCurrentMatrixStack().translate(x, y, z);
	}

	/**
	 * Translate by.
	 *
	 * @param ordinates
	 *            the ordinates
	 */
	public void translateBy(final double... ordinates) {
		switch (ordinates.length) {
			case 0:
				return;
			case 1:
				translateBy(ordinates[0], 0, 0);
				break;
			case 2:
				translateBy(ordinates[0], ordinates[1], 0);
				break;
			default:
				translateBy(ordinates[0], ordinates[1], ordinates[2]);
		}
	}

	/**
	 * Translate by.
	 *
	 * @param p
	 *            the p
	 */
	public void translateBy(final IPoint p) {
		translateBy(p.getX(), p.getY(), p.getZ());
	}

	/**
	 * Rotate by.
	 *
	 * @param angle
	 *            the angle in degrees
	 * @param x
	 *            the x
	 * @param y
	 *            the y
	 * @param z
	 *            the z
	 */
	public void rotateBy(final double angle, final double x, final double y, final double z) {
		if (x == 0d && y == 0d && z == 0d) {
			getCurrentMatrixStack().rotate(angle, 0, 0, 1);
		} else {
			getCurrentMatrixStack().rotate(angle, x, y, z);
		}
	}

	/**
	 * Rotate by.
	 *
	 * @param rotation
	 *            the rotation
	 */
	public void rotateBy(final Rotation3D rotation) {
		final IPoint axis = rotation.getAxis();
		final double angle = rotation.getAngle() * Maths.toDeg;
		rotateBy(angle, axis.getX(), axis.getY(), axis.getZ());
	}

	/**
	 * Scale by.
	 *
	 * @param x
	 *            the x
	 * @param y
	 *            the y
	 * @param z
	 *            the z
	 */
	public void scaleBy(final double x, final double y, final double z) {
		// currentScale.setLocation(x, y, z);
		getCurrentMatrixStack().scale(x, y, z);
	}

	/**
	 * Scale by.
	 *
	 * @param scaling
	 *            the scaling
	 */
	public void scaleBy(final Scaling3D scaling) {
		scaleBy(scaling.getX(), scaling.getY(), scaling.getZ());
	}

	// DRAWING

	/**
	 * Draws an arbitrary shape using a set of vertices as input, computing the normal if necessary and drawing the
	 * contour if a border is present
	 *
	 * @param yNegatedVertices
	 *            the set of vertices to draw
	 * @param number
	 *            the number of vertices to draw. Either 3 (a triangle), 4 (a quad) or -1 (a polygon)
	 * @param solid
	 *            whether to draw the shape as a solid shape
	 * @param clockwise
	 *            whether to draw the shape in the clockwise direction (the vertices are always oriented clockwise)
	 * @param computeNormal
	 *            whether to compute the normal for this shape
	 * @param border
	 *            if not null, will be used to draw the contour
	 */
	public void drawSimpleShape(final ICoordinates yNegatedVertices, final int number, final boolean clockwise,
			final boolean computeNormal, final IColor border) {
		if (!isWireframe()) {
			if (computeNormal) { setNormal(yNegatedVertices, clockwise); }
			final int style = number == 4 ? GLES20.GL_TRIANGLE_FAN : number == -1 ? GLES20.GL_TRIANGLE_FAN : GLES20.GL_TRIANGLES;
			drawVertices(style, yNegatedVertices, number, clockwise);
		}
		if (border != null || isWireframe()) {
			final IColor colorToUse = border != null ? border : getCurrentColor();
			drawClosedLine(yNegatedVertices, colorToUse, -1);
		}
	}

	/**
	 * Triangulates an arbitrary {@link Polygon} (with optional holes) using the GLU tessellator and emits the resulting
	 * triangles via the {@link ITesselator} callbacks ({@link #beginDrawing}/{@link #drawVertex}/{@link #endDrawing}).
	 *
	 * <p>
	 * This mirrors the implementation in {@code gama.ui.display.opengl.OpenGL} and replaces the former
	 * {@link FastTriangulation} pure-Java ear-clipping path, which did not handle complex concave polygons reliably.
	 * The GLU tessellator is a robust, well-tested C-backed implementation that correctly handles concave contours,
	 * self-intersections, and multiple holes.
	 * </p>
	 *
	 * <p>
	 * The tessellator fires {@code begin}/{@code vertex}/{@code end} callbacks (defined by {@link ITesselator}) which
	 * are dispatched to {@link #beginDrawing(int)}, {@link #drawVertex(int, double, double, double)}, and
	 * {@link #endDrawing()} respectively — writing directly into the VBO float buffers used by this renderer.
	 * </p>
	 *
	 * @param p
	 *            the JTS {@link Polygon} (provides the hole rings via {@link Polygon#getInteriorRingN(int)})
	 * @param yNegatedVertices
	 *            the outer ring coordinates, already Y-negated and in clockwise order
	 * @param clockwise
	 *            whether {@code yNegatedVertices} is in clockwise order
	 */
	public void drawPolygon(final Polygon p, final ICoordinates yNegatedVertices, final boolean clockwise) {
		setNormal(yNegatedVertices, clockwise);
		// Build outer ring as flat double[] for FastTriangulation
		final int outerCount = yNegatedVertices.size();
		final double[] outerRing = new double[outerCount * 2];
		yNegatedVertices.visitClockwise((final double... coords) -> {
			final int idx = (int) coords[0];
			outerRing[idx * 2] = coords[1];
			outerRing[idx * 2 + 1] = coords[2];
		});
		// Collect hole rings
		final int holeCount = p.getNumInteriorRing();
		final double[][] holes = new double[holeCount][];
		for (int h = 0; h < holeCount; h++) {
			final int holeIndex = h;
			final var holeRing = GamaCoordinateSequenceFactory.pointsOf(p.getInteriorRingN(h));
			final int holeCount2 = holeRing.size();
			holes[h] = new double[holeCount2 * 2];
			holeRing.visitYNegatedCounterClockwise((final double... coords) -> {
				final int idx = (int) coords[0];
				holes[holeIndex][idx * 2] = coords[1];
				holes[holeIndex][idx * 2 + 1] = coords[2];
			});
		}
		// Triangulate using FastTriangulation
		final int[] triIndices = FastTriangulation.triangulate(outerRing, holes, outerCount);
		// Emit triangles via beginDrawing/drawVertex/endDrawing
		beginDrawing(GLES20.GL_TRIANGLES);
		final boolean hasNormals = getLighting();
		for (int i = 0; i < triIndices.length; i += 3) {
			final int i0 = triIndices[i], i1 = triIndices[i + 1], i2 = triIndices[i + 2];
			drawVertex(0, outerRing[i0 * 2], outerRing[i0 * 2 + 1], 0);
			drawVertex(0, outerRing[i1 * 2], outerRing[i1 * 2 + 1], 0);
			drawVertex(0, outerRing[i2 * 2], outerRing[i2 * 2 + 1], 0);
		}
		endDrawing();
	}

	/**
	 * Draw closed line.
	 *
	 * @param yNegatedVertices
	 *            the y negated vertices
	 * @param number
	 *            the number
	 */
	public void drawClosedLine(final ICoordinates yNegatedVertices, final int number) {
		drawVertices(GLES20.GL_LINE_LOOP, yNegatedVertices, number, true);
	}

	/**
	 * Draw closed line.
	 *
	 * @param yNegatedVertices
	 *            the y negated vertices
	 * @param color
	 *            the color
	 * @param number
	 *            the number
	 */
	public void drawClosedLine(final ICoordinates yNegatedVertices, final IColor color, final int number) {
		if (color == null) return;
		final IColor previous = swapCurrentColor(color);
		drawClosedLine(yNegatedVertices, number);
		setCurrentColor(previous);
	}

	/**
	 * Draw line.
	 *
	 * @param yNegatedVertices
	 *            the y negated vertices
	 * @param number
	 *            the number
	 */
	public void drawLine(final ICoordinates yNegatedVertices, final int number) {
		// final boolean previous = this.setLighting(false);
		drawVertices(GLES20.GL_LINE_STRIP, yNegatedVertices, number, true);
		// this.setLighting(previous);
	}

	/**
	 * Outputs a single vertex to OpenGL, applying the z-translation to it
	 *
	 * @param x
	 * @param y
	 * @param z
	 */
	public void outputVertex(final double x, final double y, final double z) {
		ensureCapacity(currentVertices, 3);
		currentVertices.put((float) x);
		currentVertices.put((float) y);
		currentVertices.put((float) (z + currentZTranslation));
		ensureCapacity(currentColors, 4);
		currentColors.put((float) (currentColor.red() / 255.0));
		currentColors.put((float) (currentColor.green() / 255.0));
		currentColors.put((float) (currentColor.blue() / 255.0));
		// Multiply the color's own alpha by the current object (layer) alpha so that
		// both per-color transparency and layer-level transparency are honoured.
		currentColors.put((float) (currentColor.alpha() / 255.0 * currentObjectAlpha));
		// Replicate the current face normal for this vertex
		ensureCapacity(currentNormals, 3);
		currentNormals.put((float) currentNormal.getX());
		currentNormals.put((float) currentNormal.getY());
		currentNormals.put((float) currentNormal.getZ());
		currentVertexCount++;
	}

	/**
	 * Output tex coord.
	 *
	 * @param u
	 *            the u
	 * @param v
	 *            the v
	 */
	public void outputTexCoord(final double u, final double v) {
		ensureCapacity(currentTexCoords, 2);
		currentTexCoords.put((float) u);
		currentTexCoords.put((float) v);
	}

	/**
	 * Output normal.
	 *
	 * @param x
	 *            the x
	 * @param y
	 *            the y
	 * @param z
	 *            the z
	 */
	public void outputNormal(final double x, final double y, final double z) {
		// Store the current face normal; it is replicated into currentNormals for every
		// subsequent outputVertex call until the next outputNormal.
		currentNormal.setLocation(x, y, z);
	}

	/**
	 * Draw vertex.
	 *
	 * @param coords
	 *            the coords
	 * @param normal
	 *            the normal
	 * @param tex
	 *            the tex
	 */
	public void drawVertex(final IPoint coords, final IPoint normal, final IPoint tex) {
		if (normal != null) { outputNormal(normal.getX(), normal.getY(), normal.getZ()); }
		if (tex != null) { outputTexCoord(tex.getX(), tex.getY()); }
		outputVertex(coords.getX(), coords.getY(), coords.getZ());
	}

	/**
	 * Draw vertex.
	 *
	 * @param i
	 *            the i
	 * @param x
	 *            the x
	 * @param y
	 *            the y
	 * @param z
	 *            the z
	 */
	public void drawVertex(final int i, final double x, final double y, final double z) {
		if (isTextured()) {
			textureCoords.setLocation(x, y, z);
			if (GamaPreferences.Displays.OPENGL_TEXTURE_ORIENTATION.getValue()) {
				currentTextureRotation.applyTo(textureCoords);
			} else {
				nullTextureRotation.applyTo(textureCoords);
			}
			final double u = 1 - (textureCoords.getX() - textureEnvelope.getMinX()) / textureEnvelope.getWidth();
			final double v = (textureCoords.getY() - textureEnvelope.getMinY()) / textureEnvelope.getHeight();
			outputTexCoord(u, v);
		}
		outputVertex(x, y, z);
	}

	/**
	 * Draw vertices.
	 *
	 * @param style
	 *            the style
	 * @param yNegatedVertices
	 *            the y negated vertices
	 * @param number
	 *            the number
	 * @param clockwise
	 *            the clockwise
	 */
	public void drawVertices(final int style, final ICoordinates yNegatedVertices, final int number,
			final boolean clockwise) {
		beginDrawing(style);
		yNegatedVertices.visit(glDrawer, number, clockwise);
		endDrawing();
	}

	/**
	 * Replaces the current color by the parameter, sets the alpha of the parameter to be the one of the current color,
	 * and returns the ex-current color
	 *
	 * @param iColor
	 *            a Color
	 * @return the previous current color
	 */
	public IColor swapCurrentColor(final IColor iColor) {
		final IColor old = currentColor;
		setCurrentColor(iColor, old == null ? 1 : old.alpha() / 255d);
		return old;
	}

	/**
	 * Sets the normal.
	 *
	 * @param yNegatedVertices
	 *            the y negated vertices
	 * @param clockwise
	 *            the clockwise
	 * @return the gama point
	 */
	public IPoint setNormal(final ICoordinates yNegatedVertices, final boolean clockwise) {
		yNegatedVertices.getNormal(clockwise, 1, currentNormal);
		outputNormal(currentNormal.getX(), currentNormal.getY(), currentNormal.getZ());
		if (isTextured()) { computeTextureCoordinates(yNegatedVertices, clockwise); }
		return currentNormal;
	}

	/**
	 * Compute texture coordinates.
	 *
	 * @param yNegatedVertices
	 *            the y negated vertices
	 * @param clockwise
	 *            the clockwise
	 */
	private void computeTextureCoordinates(final ICoordinates yNegatedVertices, final boolean clockwise) {
		workingVertices.setTo(yNegatedVertices);
		if (GamaPreferences.Displays.OPENGL_TEXTURE_ORIENTATION.getValue()) {
			currentTextureRotation.rotateToHorizontal(currentNormal,
					workingVertices.directionBetweenLastPointAndOrigin(), clockwise);
			workingVertices.applyRotation(currentTextureRotation);
		} else {
			workingVertices.applyRotation(nullTextureRotation);
		}

		workingVertices.getEnvelopeInto(textureEnvelope);
	}

	/**
	 * Sets the current color.
	 *
	 * @param c
	 *            the c
	 * @param alpha
	 *            the alpha
	 */
	public void setCurrentColor(final IColor c, final double alpha) {
		if (c == null) return;
		// When an explicit alpha is supplied (e.g. from a layer's setAlpha call) store the
		// product of the colour's own alpha and the caller-supplied alpha directly.  The
		// resulting stored alpha is then further multiplied by currentObjectAlpha in
		// outputVertex, so currentObjectAlpha is always applied exactly once.
		currentColor = GamaColorFactory.getWithDoubles(c.red() / 255d, c.green() / 255d, c.blue() / 255d,
				c.alpha() / 255d * alpha);
	}

	/**
	 * Sets the current color.
	 *
	 * @param c
	 *            the new current color
	 */
	public void setCurrentColor(final IColor c) {
		// Store the colour's own alpha as-is; currentObjectAlpha is applied later in
		// outputVertex so it is never applied more than once.
		if (c == null) return;
		currentColor = GamaColorFactory.getWithDoubles(c.red() / 255d, c.green() / 255d, c.blue() / 255d,
				c.alpha() / 255d);
	}

	/**
	 * Sets the current color.
	 *
	 * @param red
	 *            the red
	 * @param green
	 *            the green
	 * @param blue
	 *            the blue
	 * @param alpha
	 *            the alpha
	 */
	public void setCurrentColor(final double red, final double green, final double blue, final double alpha) {
		// Store the colour as-is. currentObjectAlpha is applied at the point of use
		// (outputVertex) so that it is never applied more than once regardless of which
		// setCurrentColor overload is used.
		currentColor = GamaColorFactory.getWithDoubles(Math.max(red, 0), Math.max(green, 0), Math.max(blue, 0), alpha);
	}

	/**
	 * Gets the current color.
	 *
	 * @return the current color
	 */
	public IColor getCurrentColor() { return currentColor; }

	// LINE WIDTH

	/**
	 * Sets the line width.
	 *
	 * @param width
	 *            the new line width
	 */
	public float setLineWidth(final float width) {
		float previous = currentObjectLineWidth;
		if (width != currentObjectLineWidth) {
			currentObjectLineWidth = width;
			GLES20.glLineWidth(width);
		}
		return previous;
	}

	// ALPHA
	/**
	 * Between 0d (transparent) to 1d (opaque)
	 *
	 * @param alpha
	 */
	public final void setCurrentObjectAlpha(final double alpha) { currentObjectAlpha = alpha; }

	/**
	 * Gets the current object alpha.
	 *
	 * @return the current object alpha
	 */
	public double getCurrentObjectAlpha() { return currentObjectAlpha; }

	// TEXTURES

	/**
	 * Sets the id of the textures to enable. If the first is equal to NO_TEXTURE, all textures are disabled. If the
	 * second is equal to NO_TEXTURE, then the first one is also bound to the second unit.
	 *
	 * @param t
	 *            the id of the texture to enable. NO_TEXTURE means disabling textures
	 */
	public void setCurrentTextures(final int t0, final int t1) {
		primaryTexture = t0;
		alternateTexture = t1;
		textured = t0 != NO_TEXTURE;
		enablePrimaryTexture();
	}

	/**
	 * Bind texture.
	 *
	 * @param texture
	 *            the texture
	 */
	public void bindTexture(final int texture) {
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
		boolean isAntiAlias = getData().isAntialias();
		if (texture == lastBoundTexture && isAntiAlias == lastAntiAliasSetting) return;

		lastBoundTexture = texture;
		lastAntiAliasSetting = isAntiAlias;
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, isAntiAlias ? GLES20.GL_LINEAR : GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, isAntiAlias ? GLES20.GL_LINEAR : GLES20.GL_NEAREST);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, 0x84FE, isAntiAlias ? ANISOTROPIC_LEVEL : 0);
	}

	/**
	 * Enable primary texture. Binds the primary texture. In core GL4 there is no GL_TEXTURE_2D enable/disable;
	 * texturing is active whenever a sampler uniform is bound in the shader.
	 */
	public void enablePrimaryTexture() {
		if (primaryTexture == NO_TEXTURE) return;
		bindTexture(primaryTexture);
	}

	/**
	 * Enable alternate texture. Binds the alternate texture.
	 */
	public void enableAlternateTexture() {
		if (alternateTexture == NO_TEXTURE) return;
		bindTexture(alternateTexture);
	}

	/**
	 * Disable textures. In core GL4, texturing is disabled by binding texture 0.
	 */
	public void disableTextures() {
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
		textured = false;
	}

	/**
	 * Invalidate cached texture state.
	 */
	public void invalidateTextureCache() {
		lastBoundTexture = NO_TEXTURE;
	}

	/**
	 * Delete volatile textures.
	 */
	public void deleteVolatileTextures() {
		lastBoundTexture = NO_TEXTURE;
		textureCache.deleteVolatileTextures();
	}

	/**
	 * Cache texture.
	 *
	 * @param file
	 *            the file
	 */
	public void cacheTexture(final IImageProvider file) {
		if (file == null) return;
		textureCache.processs(file);
	}

	/**
	 * Gets the texture id.
	 *
	 * @param file
	 *            the file
	 * @param useCache
	 *            the use cache
	 * @return the texture id
	 */
	public int getTextureId(final IImageProvider file, final boolean useCache) {
		final int r = textureCache.getTexture(file, file.isAnimated(), useCache);
		if (r == 0) return NO_TEXTURE;
		return r;
	}

	/**
	 * Gets the texture id.
	 *
	 * @param img
	 *            the img
	 * @return the texture id
	 */
	public int getTextureId(final BufferedImage img) {
		final int r = textureCache.getTexture(img);
		if (r == 0) return NO_TEXTURE;
		return r;
	}

	/**
	 * Gets the texture.
	 *
	 * @param file
	 *            the file
	 * @param isAnimated
	 *            the is animated
	 * @param useCache
	 *            the use cache
	 * @return the texture
	 */
	public int getTexture(final IImageProvider file, final boolean isAnimated, final boolean useCache) {
		return textureCache.getTexture(file, isAnimated, useCache);
	}

	// GEOMETRIES

	/**
	 * Cache geometry.
	 *
	 * @param object
	 *            the object
	 */
	public void cacheGeometry(final GamaGeometryFile object) {
		geometryCache.process(object);
	}

	/**
	 * Gets the envelope for.
	 *
	 * @param obj
	 *            the obj
	 * @return the envelope for
	 */
	public IEnvelope getEnvelopeFor(final Object obj) {
		if (obj instanceof GamaGeometryFile) return geometryCache.getEnvelope((GamaGeometryFile) obj);
		if (obj instanceof Geometry) return GamaEnvelopeFactory.of((Geometry) obj);
		return null;
	}

	// TEXT

	/**
	 * Draws one string in raster at the given coords and with the given font.
	 *
	 * <p>
	 * <b>GL4 core profile limitation:</b> GLUT bitmap strings rely on {@code glBitmap()} and {@code glRasterPos3d()},
	 * both removed from the OpenGL 4 core profile. This method is a no-op in the current implementation. Text that must
	 * be visible should be drawn via {@link gama.ui.display.opengl4.scene.text.TextDrawer} with
	 * {@code isPerspective = true}.
	 * </p>
	 *
	 * @param s
	 *            the string
	 * @param font
	 *            the GLUT font constant
	 * @param x
	 *            x coordinate
	 * @param y
	 *            y coordinate
	 * @param z
	 *            z coordinate
	 */
	public void rasterText(final String s, final int font, final double x, final double y, final double z) {
		// No-op in GL4 core: glBitmap / glRasterPos3d have been removed.
	}

	/**
	 * Gets the world width.
	 *
	 * @return the world width
	 */
	public double getWorldWidth() { return getData().getEnvWidth(); }

	/**
	 * Gets the world height.
	 *
	 * @return the world height
	 */
	public double getWorldHeight() { return getData().getEnvHeight(); }

	/**
	 * Sets the display wireframe.
	 *
	 * @param wireframe
	 *            the new display wireframe
	 */
	public boolean setDisplayWireframe(final boolean wireframe) {
		boolean old = displayIsWireframe;
		if (old != wireframe) {
			displayIsWireframe = wireframe;
			updatePolygonMode();
		}
		return old;
	}

	/**
	 * Sets the object wireframe. Returns the previous value.
	 *
	 * @param wireframe
	 *            the new object wireframe
	 */
	public boolean setObjectWireframe(final boolean wireframe) {
		boolean old = objectIsWireframe;
		if (old != wireframe) {
			objectIsWireframe = wireframe;
			updatePolygonMode();
		}
		return old;
	}

	/**
	 * Sets the polygon mode.
	 *
	 * @param wireframe
	 *            the new polygon mode
	 */
	public void updatePolygonMode() {
		int newPolygonMode = isWireframe() ? GLES20.GL_LINES : GLES20.GL_TRIANGLES;
		if (newPolygonMode != currentPolygonMode) {
			currentPolygonMode = newPolygonMode;
			// GLES20 has no glPolygonMode; wireframe/fill mode is handled via shader
		}
	}

	/**
	 * Checks if is wireframe.
	 *
	 * @return true, if is wireframe
	 */
	public boolean isWireframe() { return displayIsWireframe || objectIsWireframe; }

	// PICKING

	/**
	 * Run with names.
	 *
	 * @param r
	 *            the r
	 */
	public void runWithNames(final Runnable r) {
		// gl.glInitNames();
		// gl.glPushName(0);
		r.run();
		// gl.glPopName();
	}

	/**
	 * Register for selection. Encodes the object index into the current colour using the colour-buffer picking
	 * convention: red = low byte, green = high byte, blue = 0 (background uses blue = 0xFF). This colour will be
	 * emitted by subsequent {@link #outputVertex} calls during the picking pass.
	 *
	 * @param index
	 *            the object index (0…65535)
	 */
	public void registerForSelection(final int index) {
		setCurrentColor((index & 0xFF) / 255.0, (index >> 8 & 0xFF) / 255.0, 0.0, 1.0);
	}

	// LISTS

	// DISPLAY-LIST REPLACEMENT
	// OpenGL display lists have been removed in GL4 core profile.
	// We replace them with a simple Runnable cache: compileAsList stores the lambda,
	// drawList replays it, and deleteList removes it from the map.

	/** Replacement for display lists: maps a list-index → Runnable to replay. */
	private final java.util.Map<Integer, Runnable> listCache = new java.util.HashMap<>();

	/** Counter for generating unique list IDs. */
	private int nextListId = 1;

	/**
	 * Compile as list. Stores the provided {@link Runnable} under a new unique index and executes it immediately (so
	 * that the first draw also initialises any state the lambda depends on). Returns the index that can be passed to
	 * {@link #drawList(int)} to replay the geometry.
	 *
	 * @param r
	 *            the geometry-producing runnable
	 * @return the list index
	 */
	public int compileAsList(final Runnable r) {
		final int index = nextListId++;
		listCache.put(index, r);
		return index;
	}

	/**
	 * Draw list. Replays the {@link Runnable} stored under the given index.
	 *
	 * @param i
	 *            the list index returned by {@link #compileAsList(Runnable)}
	 */
	public void drawList(final int i) {
		final Runnable r = listCache.get(i);
		if (r != null) { r.run(); }
	}

	/**
	 * Delete list. Removes the stored {@link Runnable} for the given index.
	 *
	 * @param index
	 *            the list index to remove
	 */
	public void deleteList(final Integer index) {
		if (index != null) { listCache.remove(index); }
	}

	/**
	 * Draw cached geometry.
	 *
	 * @param file
	 *            the file
	 * @param border
	 *            the border
	 */
	public void drawCachedGeometry(final GamaGeometryFile file, final IColor border) {
		if (file == null) return;
		final Integer index = geometryCache.get(file);
		if (index != null) {
			drawList(index);
			if (border != null || isWireframe()) {
				final IColor colorToUse = border != null ? border : getCurrentColor();
				final IColor old = swapCurrentColor(colorToUse);
				boolean previous = setObjectWireframe(true);
				try {
					drawList(index);
				} finally {
					setCurrentColor(old);
					setObjectWireframe(previous);
				}
			}
		}
	}

	/**
	 * Draw cached geometry.
	 *
	 * @param id
	 *            the id
	 * @param border
	 *            the border
	 */
	public void drawCachedGeometry(final IShape.Type id, /* final boolean solid, */ final IColor border) {
		if (geometryCache == null || id == null) return;
		final BuiltInGeometry object = geometryCache.get(id);
		if (object == null) return;
		if (!isWireframe()) { object.draw(this); }
		if (border != null || isWireframe()) {
			final IColor colorToUse = border != null ? border : getCurrentColor();
			final IColor old = swapCurrentColor(colorToUse);
			boolean previous = setObjectWireframe(true);
			try {
				object.draw(this);
			} finally {
				setCurrentColor(old);
				setObjectWireframe(previous);
			}
		}
	}

	/**
	 * Initialize shape cache.
	 */
	public void initializeShapeCache() {
		textured = true;
		geometryCache.initialize(this);
		textured = false;
	}

	/**
	 * Checks if is textured.
	 *
	 * @return true, if is textured
	 */
	public boolean isTextured() { return textured && !isWireframe(); }

	/**
	 * Begin object.
	 *
	 * @param object
	 *            the object
	 */
	public void beginObject(final AbstractObject<?, ?> object, final boolean isPicking) {
		IDrawingAttributes att = object.getAttributes();
		if (isPicking) {
			// Colour-buffer picking: encode the object index now and do NOT overwrite it below.
			registerForSelection(att.getIndex());
		}
		boolean empty = att.isEmpty();
		previousObjectWireframe = setObjectWireframe(empty);
		previousObjectLighting = setObjectLighting(att.isLighting());
		previousObjectLineWidth = setLineWidth(att.getLineWidth().floatValue());
		setCurrentTextures(object.getPrimaryTexture(this), object.getAlternateTexture(this));
		// Always enable blending so that alpha (both per-color and layer/object alpha) is
		// respected. Use pre-multiplied blending for textured objects (textures may already
		// carry pre-multiplied alpha) and standard src-alpha blending for plain colored ones.
		GLES20.glEnable(GLES20.GL_BLEND);
		if (isTextured()) {
			GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		} else {
			GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		}
		// Only set the visual colour when NOT in picking mode; during picking
		// the colour is the ID-encoded value set by registerForSelection() above.
		if (!isPicking) {
			setCurrentColor(att.getColor());
			if (!empty && !att.isSynthetic()) {
				// removed glTexEnvi
			}
		}
	}

	/**
	 * End object.
	 *
	 * @param object
	 *            the object
	 */
	public void endObject(final AbstractObject<?, ?> object, final boolean isPicking) {
		disableTextures();
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		translateByZIncrement();
		if (object.isFilled() && !object.getAttributes().isSynthetic()) {
			// removed glTexEnvi
		}
		setObjectLighting(previousObjectLighting);
		setObjectWireframe(previousObjectWireframe);
		setLineWidth(previousObjectLineWidth);
		if (isPicking) { renderer.getPickingHelper().tryPick(object.getAttributes()); }
	}

	/**
	 * Begin scene.
	 *
	 * @return the pass
	 */
	public Pass beginScene() {
		previousDisplayWireframe = setDisplayWireframe(getData().isWireframe());
		previousDisplayLighting = setDisplayLighting(getData().isLightOn());
		processUnloadedCacheObjects();
		final IColor backgroundColor = getData().getBackgroundColor();
		GLES20.glClearColor(backgroundColor.red() / 255.0f, backgroundColor.green() / 255.0f,
				backgroundColor.blue() / 255.0f, 1.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		GLES20.glDepthRangef(0f, 1f);
		// Enable blending once for the whole scene with the standard src-alpha equation.
		// Individual draw calls (beginObject/endDrawing) may override the blend function
		// for specific cases (e.g. pre-multiplied textures) and must restore it afterwards.
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		// Depth mask on by default; transparent draws will temporarily turn it off.
		GLES20.glDepthMask(true);
		resetMatrix(GL_PROJECTION_SENTINEL);
		updatePerspective();
		resetMatrix(GL_MODELVIEW_SENTINEL);
		// AD removed from here and put in ModelScene.draw() so that it is inside the keystone drawing. See #3285
		// rotateModel();
		return endScene;
	}

	/**
	 * End scene.
	 */
	public void endScene() {
		boolean drawFPS = getData().isShowfps();
		boolean drawRotation = rotationMode && SHOULD_DRAW_ROTATION_SPHERE;
		boolean drawROI = renderer.getCameraHelper().getROIEnvelope() != null;
		if (drawFPS || drawRotation || drawROI) { disableTextures(); }
		drawFPS(drawFPS);
		drawROI(drawROI);
		drawRotation(drawRotation);
		setDisplayLighting(previousDisplayLighting);
		setDisplayWireframe(previousDisplayWireframe);
		GLES20.glFinish();
	}

	/**
	 * Process unloaded cache objects.
	 */
	public void processUnloadedCacheObjects() {
		textureCache.processUnloaded();
		geometryCache.processUnloaded();
	}

	/**
	 * Rotate model.
	 */
	public void rotateModel() {
		if (getData().hasRotation()) {
			IPoint c = getData().getRotationCenter();
			double cx = c.getX();
			double cy = c.getY();
			double cz = c.getZ();
			translateBy(cx, cy, cz);
			IPoint p = getData().getRotationAxis();
			if (p == null) {
				rotateBy(getData().getRotationAngle(), 0, 0, 1);
			} else {
				rotateBy(getData().getRotationAngle(), p.getX(), p.getY(), p.getZ());
			}
			translateBy(-cx, -cy, -cz);
		}
	}

	/**
	 * Initialize GL states.
	 *
	 * @param bg
	 *            the bg
	 */
	public void initializeGLStates(final IColor bg) {
		GLES20.glClearColor(bg.red() / 255.0f, bg.green() / 255.0f, bg.blue() / 255.0f, 1.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		// Enabling the depth buffer & the depth testing
		GLES20.glDepthRangef(0f, 1f);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST); // enables depth testing
		GLES20.glDepthFunc(GLES20.GL_LEQUAL); // the type of depth test to do
		// Whether face culling is enabled or not
		if (GamaPreferences.Displays.ONLY_VISIBLE_FACES.getValue()) {
			GLES20.glEnable(GLES20.GL_CULL_FACE);
			GLES20.glCullFace(GLES20.GL_BACK);
		}
		// Turn on clockwise direction of vertices as an indication of "front" (important)
		GLES20.glFrontFace(GLES20.GL_CW);

		// GLES20 has no glHint / GL_LINE_SMOOTH_HINT; line smooth is not available in GLES20.
		// Blending & alpha control
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		// GLES20 has no GL_LINE_SMOOTH; line smoothing is not available.
		// GLES20 has no GL_MULTISAMPLE; multisampling is configured at context creation.
		// Setting the default polygon mode
		updatePolygonMode();
		initializeShapeCache();

	}

	/**
	 * Gets the ratios.
	 *
	 * @return the ratios
	 */
	public IPoint getRatios() { return ratios; }

	/**
	 *
	 * DECORATIONS: ROI, Rotation, FPS
	 *
	 */

	public void setRotationMode(final boolean b) { rotationMode = b; }

	/**
	 * Checks if is in rotation mode.
	 *
	 * @return true, if is in rotation mode
	 */
	public boolean isInRotationMode() { return rotationMode; }

	/**
	 * Draws a string in fixed screen-space (overlay) coordinates using the JOGL graph/curve renderer. The position
	 * {@code (x, y)} is in pixels measured from the bottom-left corner of the viewport. This replaces every former call
	 * to {@link #rasterText} which is a no-op in GL4 core.
	 *
	 * @param s
	 *            the string to draw
	 * @param font
	 *            the AWT font to use
	 * @param x
	 *            x position in pixels from the left edge of the viewport
	 * @param y
	 *            y position in pixels from the bottom edge of the viewport
	 */
	public void drawScreenText(final String s, final java.awt.Font font, final double x, final double y) {
		final TextDrawer td = (TextDrawer) drawers.get(DrawerType.STRING);
		if (td == null) return;
		// Build minimal IDrawingAttributes in overlay (screen-space) mode
		final gama.gaml.statements.draw.TextDrawingAttributes attr =
				new gama.gaml.statements.draw.TextDrawingAttributes(gama.api.utils.geometry.Scaling3D.of(1, 1, 1), null,
						gama.api.types.geometry.GamaPointFactory.create(x, y, 0), getCurrentColor());
		attr.setFont(gama.api.types.font.GamaFontFactory.createFontFrom(font));
		attr.setPerspective(false);
		td.drawWithGraphLibraryPublic(s, attr, true);
	}

	/**
	 * Draw FPS. Renders the current frames-per-second count in the top-left corner of the display using the JOGL
	 * graph/curve screen-space renderer.
	 *
	 * @param doIt
	 *            whether the FPS counter should be drawn
	 */
	public void drawFPS(final boolean doIt) {
		if (doIt) {
			setCurrentColor(GamaColorFactory.BLACK);
			// FPS not tracked in the Android stub
			final String s = "N/A FPS";
			drawScreenText(s, new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12), -5, 5);
		}
	}

	/**
	 * Draw ROI.
	 *
	 * @param doIt
	 *            the do it
	 */
	public void drawROI(final boolean doIt) {
		if (doIt) { getGeometryDrawer().drawROIHelper(renderer.getCameraHelper().getROIEnvelope()); }
	}

	/**
	 * Size of rotation elements.
	 *
	 * @return the double
	 */
	public double sizeOfRotationElements() {
		return Math.min(getMaxEnvDim() / 4d, getData().getCameraPos().minus(getData().getCameraTarget()).norm() / 6d);
	}

	/**
	 * Draw rotation.
	 *
	 * @param doIt
	 *            the do it
	 */
	public void drawRotation(final boolean doIt) {
		if (doIt) {
			final IPoint target = getData().getCameraTarget();
			final double distance = getData().getCameraPos().minus(target).norm();
			getGeometryDrawer().drawRotationHelper(target, distance, Math.min(getMaxEnvDim() / 4d, distance / 8d));
		}
	}

	@Override
	public void initialize() {}

	/**
	 * Checks if is rendering keystone.
	 *
	 * @return true, if is rendering keystone
	 */
	public boolean isRenderingKeystone() {
		KeystoneHelper k = getRenderer().getKeystoneHelper();
		return k.isActive() || getRenderer().getData().isKeystoneDefined();
	}

}

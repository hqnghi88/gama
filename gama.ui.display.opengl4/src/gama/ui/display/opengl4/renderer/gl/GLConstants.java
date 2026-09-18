package gama.ui.display.opengl4.renderer.gl;

/**
 * All GL constants used by opengl4, extracted from JOGL GL/GL4/GL2GL3 classes.
 * These values are identical to the OpenGL specification and GLES 2.0.
 */
public final class GLConstants {

	private GLConstants() {}

	// ── Primitive types ─────────────────────────────────────────────────

	public static final int GL_TRIANGLES = 0x0004;
	public static final int GL_TRIANGLE_STRIP = 0x0005;
	public static final int GL_TRIANGLE_FAN = 0x0006;
	public static final int GL_LINES = 0x0001;
	public static final int GL_LINE_LOOP = 0x0002;
	public static final int GL_LINE_STRIP = 0x0003;
	public static final int GL_POINTS = 0x0000;

	// ── Buffer targets ──────────────────────────────────────────────────

	public static final int GL_ARRAY_BUFFER = 0x8892;
	public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;

	// ── Buffer usage ────────────────────────────────────────────────────

	public static final int GL_STATIC_DRAW = 0x88E4;
	public static final int GL_DYNAMIC_DRAW = 0x88E8;

	// ── Data types ──────────────────────────────────────────────────────

	public static final int GL_FLOAT = 0x1406;
	public static final int GL_UNSIGNED_BYTE = 0x1401;
	public static final int GL_UNSIGNED_INT = 0x1405;
	public static final int GL_DOUBLE = 0x140A; // not in GLES2

	// ── Shader types ────────────────────────────────────────────────────

	public static final int GL_VERTEX_SHADER = 0x8B31;
	public static final int GL_FRAGMENT_SHADER = 0x8B30;

	// ── Shader/program query ────────────────────────────────────────────

	public static final int GL_COMPILE_STATUS = 0x8B81;
	public static final int GL_LINK_STATUS = 0x8B82;
	public static final int GL_INFO_LOG_LENGTH = 0x8B84;

	// ── Texture targets ─────────────────────────────────────────────────

	public static final int GL_TEXTURE_2D = 0x0DE1;

	// ── Texture parameters ──────────────────────────────────────────────

	public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
	public static final int GL_TEXTURE_MIN_FILTER = 0x2801;
	public static final int GL_TEXTURE_WRAP_S = 0x2802;
	public static final int GL_TEXTURE_WRAP_T = 0x2803;
	public static final int GL_NEAREST = 0x2600;
	public static final int GL_LINEAR = 0x2601;
	public static final int GL_REPEAT = 0x2901;
	public static final int GL_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FE; // JOGL extension

	// ── Pixel formats ───────────────────────────────────────────────────

	public static final int GL_RGBA = 0x1908;
	public static final int GL_RGB = 0x1907;
	public static final int GL_RED = 0x1903;

	// ── Framebuffer targets / attachments ────────────────────────────────

	public static final int GL_FRAMEBUFFER = 0x8D40;
	public static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
	public static final int GL_DEPTH_ATTACHMENT = 0x8D00;
	public static final int GL_RENDERBUFFER = 0x8D41;

	// ── Renderbuffer formats ────────────────────────────────────────────

	public static final int GL_DEPTH_COMPONENT = 0x1902;
	public static final int GL_DEPTH_COMPONENT16 = 0x81A5;
	public static final int GL_DEPTH_COMPONENT24 = 0x81A6;
	public static final int GL_DEPTH_COMPONENT32 = 0x81A7;

	// ── Enable caps ─────────────────────────────────────────────────────

	public static final int GL_BLEND = 0x0BE2;
	public static final int GL_DEPTH_TEST = 0x0B71;
	public static final int GL_CULL_FACE = 0x0B44;
	public static final int GL_MULTISAMPLE = 0x809D;
	public static final int GL_LINE_SMOOTH = 0x0B20;

	// ── Blend functions ─────────────────────────────────────────────────

	public static final int GL_SRC_ALPHA = 0x0302;
	public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
	public static final int GL_ONE = 1;
	public static final int GL_CONSTANT_ALPHA = 0x8003;
	public static final int GL_ONE_MINUS_CONSTANT_ALPHA = 0x8004;

	// ── Depth functions ─────────────────────────────────────────────────

	public static final int GL_LEQUAL = 0x0203;
	public static final int GL_LESS = 0x0201;
	public static final int GL_ALWAYS = 0x0207;

	// ── Face culling ────────────────────────────────────────────────────

	public static final int GL_BACK = 0x0405;
	public static final int GL_FRONT = 0x0404;
	public static final int GL_FRONT_AND_BACK = 0x0408;
	public static final int GL_CW = 0x0900;
	public static final int GL_CCW = 0x0901;

	// ── Polygon mode (not in GLES2) ─────────────────────────────────────

	public static final int GL_FILL = 0x1B02;
	public static final int GL_LINE = 0x1B01;
	public static final int GL_POINT = 0x1B00;

	// ── Clear buffer bits ───────────────────────────────────────────────

	public static final int GL_COLOR_BUFFER_BIT = 0x00004000;
	public static final int GL_DEPTH_BUFFER_BIT = 0x00000100;
	public static final int GL_STENCIL_BUFFER_BIT = 0x00000400;

	// ── Hint targets / modes ────────────────────────────────────────────

	public static final int GL_PERSPECTIVE_CORRECTION_HINT = 0x0C50;
	public static final int GL_LINE_SMOOTH_HINT = 0x0C52;
	public static final int GL_FOG_HINT = 0x0C54;
	public static final int GL_GENERATE_MIPMAP_HINT = 0x8191;
	public static final int GL_FASTEST = 0x1101;
	public static final int GL_NICEST = 0x1102;
	public static final int GL_DONT_CARE = 0x1100;

	// ── Query targets ───────────────────────────────────────────────────

	public static final int GL_VIEWPORT = 0x0BA2;
	public static final int GL_MAX_TEXTURE_SIZE = 0x0D33;
	public static final int GL_MAX_VERTEX_ATTRIBS = 0x8869;
	public static final int GL_RENDERER = 0x1F01;
	public static final int GL_VERSION = 0x1F02;
	public static final int GL_SHADING_LANGUAGE_VERSION = 0x8B8C;

	// ── GLU tessellation constants (not in GLES2) ───────────────────────

	public static final int GLU_TESS_BEGIN = 0x18600;
	public static final int GLU_TESS_END = 0x18601;
	public static final int GLU_TESS_VERTEX = 0x18602;
	public static final int GLU_TESS_TOLERANCE = 0x18603;
	public static final int GLU_TESS_WINDING_RULE = 0x18604;
	public static final int GLU_TESS_WINDING_ODD = 0x18606;

	// ── SMOOTH_LINE_WIDTH_GRANULARITY (JOGL extension) ─────────────────

	public static final int GL_SMOOTH_LINE_WIDTH_GRANULARITY = 0x0B23;
}

package gama.ui.display.opengl4.renderer.gl;

/**
 * Factory for creating the appropriate {@link GLWrapper} implementation.
 * Uses the classloader to detect whether Android GLES20 is available.
 */
public final class GLWrapperFactory {

	private GLWrapperFactory() {}

	/**
	 * Creates a GLWrapper for the given GL context object.
	 * If the object is a JOGL GL4, returns a {@link JoglGLWrapper}.
	 * Otherwise throws IllegalArgumentException.
	 */
	public static GLWrapper create(Object glContext) {
		if (glContext == null) throw new IllegalArgumentException("GL context cannot be null");

		// Try to detect JOGL GL4
		try {
			Class<?> gl4Class = Class.forName("com.jogamp.opengl.GL4");
			if (gl4Class.isInstance(glContext)) {
				return new JoglGLWrapper((com.jogamp.opengl.GL4) glContext);
			}
		} catch (ClassNotFoundException e) {
			// JOGL not on classpath, that's fine
		}

		throw new IllegalArgumentException("Unsupported GL context type: " + glContext.getClass().getName());
	}

	/**
	 * Creates a GLES2 GLWrapper (for Android).
	 * Uses reflection so this class compiles without android.jar on the classpath.
	 */
	public static GLWrapper createGLES2() {
		try {
			Class<?> gles2Class = Class.forName("gama.ui.display.opengl4.renderer.gl.Gles2GLWrapper");
			return (GLWrapper) gles2Class.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Gles2GLWrapper not available", e);
		}
	}
}

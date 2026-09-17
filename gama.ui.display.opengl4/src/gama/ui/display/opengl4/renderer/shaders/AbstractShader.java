/*******************************************************************************************************
 *
 * AbstractShader.java, in gama.ui.display.opengl4, is part of the source code of the GAMA modeling and simulation
 * platform (v.2025-03).
 *
 * (c) 2007-2026 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.display.opengl4.renderer.shaders;

import java.io.InputStream;
import java.util.Scanner;

import android.opengl.GLES20;

import gama.api.types.geometry.GamaPointFactory;
import gama.api.types.geometry.IPoint;
import gama.dev.DEBUG;

/**
 * The Class AbstractShader.
 */
public abstract class AbstractShader {

	/** The is overlay. */
	protected boolean isOverlay = false;

	/** The program ID. */
	private int programID;

	/** The vertex shader ID. */
	private final int vertexShaderID;

	/** The fragment shader ID. */
	private final int fragmentShaderID;

	/** The location layer alpha. */
	private int location_layerAlpha;

	/** The Constant POSITION_ATTRIBUTE_IDX. */
	public static final int POSITION_ATTRIBUTE_IDX = 0;

	/** The Constant UVMAPPING_ATTRIBUTE_IDX. */
	public static final int UVMAPPING_ATTRIBUTE_IDX = 3;

	/**
	 * Instantiates a new abstract shader.
	 */
	protected AbstractShader(final String vertexFile, final String fragmentFile) {
		InputStream vertexInputStream, fragmentInputStream;

		try {
			vertexInputStream = getClass().getResourceAsStream(vertexFile);
			if (vertexInputStream == null)
				System.err.println("Cannot locate vertex shader program " + vertexFile);
			fragmentInputStream = getClass().getResourceAsStream(fragmentFile);
			if (fragmentInputStream == null)
				System.err.println("Cannot locate fragment shader program " + fragmentFile);
		} catch (final Exception e) {
			DEBUG.ERR(e.getMessage());
			vertexShaderID = -1;
			fragmentShaderID = -1;
			return;
		}

		vertexShaderID = loadShader(vertexInputStream, GLES20.GL_VERTEX_SHADER);
		fragmentShaderID = loadShader(fragmentInputStream, GLES20.GL_FRAGMENT_SHADER);

		programID = GLES20.glCreateProgram();
		GLES20.glAttachShader(programID, vertexShaderID);
		GLES20.glAttachShader(programID, fragmentShaderID);

		bindAttributes();

		GLES20.glLinkProgram(programID);

		int[] linkStatus = new int[1];
		GLES20.glGetProgramiv(programID, GLES20.GL_LINK_STATUS, linkStatus, 0);
		if (linkStatus[0] == 0) {
			String log = GLES20.glGetProgramInfoLog(programID);
			DEBUG.ERR("Error linking shader program: " + log);
		}

		getAllUniformLocations();
	}

	/**
	 * Load shader.
	 */
	private int loadShader(final InputStream is, final int type) {
		String shaderString = null;

		try (Scanner s = new Scanner(is)) {
			s.useDelimiter("\\A");
			shaderString = s.hasNext() ? s.next() : "";

			final int shaderID = GLES20.glCreateShader(type);
			GLES20.glShaderSource(shaderID, shaderString);
			GLES20.glCompileShader(shaderID);

			int[] compiled = new int[1];
			GLES20.glGetShaderiv(shaderID, GLES20.GL_COMPILE_STATUS, compiled, 0);
			if (compiled[0] == 0) {
				String log = GLES20.glGetShaderInfoLog(shaderID);
				DEBUG.ERR("Error compiling shader (" + type + "): " + log);
			}

			return shaderID;
		}
	}

	/**
	 * Start.
	 */
	public void start() {
		GLES20.glUseProgram(programID);
	}

	/**
	 * Stop.
	 */
	public void stop() {
		GLES20.glUseProgram(0);
	}

	/**
	 * Gets the program ID.
	 */
	public int getProgramID() { return programID; }

	/**
	 * Sets the program ID.
	 */
	public void setProgramID(final int programID) { this.programID = programID; }

	/**
	 * Gets the uniform location.
	 */
	public int getUniformLocation(final String uniformName) {
		return GLES20.glGetUniformLocation(programID, uniformName);
	}

	/**
	 * Bind attributes.
	 */
	protected abstract void bindAttributes();

	/**
	 * Bind attribute.
	 */
	protected void bindAttribute(final int attribute, final String variableName) {
		GLES20.glBindAttribLocation(programID, attribute, variableName);
	}

	/**
	 * Gets the all uniform locations.
	 */
	protected void getAllUniformLocations() {
		location_layerAlpha = getUniformLocation("layerAlpha");
	}

	/**
	 * Load float.
	 */
	public void loadFloat(final int location, final float value) {
		GLES20.glUniform1f(location, value);
	}

	/**
	 * Sets the layer alpha.
	 */
	public void setLayerAlpha(final float layerAlpha) {
		loadFloat(location_layerAlpha, layerAlpha);
	}

	/**
	 * Gets the translation.
	 */
	public IPoint getTranslation() { return GamaPointFactory.create(); }

	/**
	 * Checks if is overlay.
	 */
	public boolean isOverlay() { return isOverlay; }

	/**
	 * Use normal.
	 */
	abstract public boolean useNormal();

	/**
	 * Use texture.
	 */
	abstract public boolean useTexture();

	/**
	 * Gets the texture ID.
	 */
	abstract public int getTextureID();
}

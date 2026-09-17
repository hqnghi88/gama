package gama.ui.display.opengl4.renderer.shaders;

import android.opengl.GLES20;
import org.joml.Matrix4f;
import org.joml.Matrix3f;

public class BasicShader extends AbstractShader {

	private int location_model;
	private int location_view;
	private int location_projection;
	private int location_normalMatrix;
	private int location_useTexture;
	private int location_useLighting;
	private int location_ambientColor;
	private int location_lightPosition;
	private int location_lightColor;
	private int location_viewPos;
	private int location_shininess;

	public BasicShader() {
		super("glsl/basic.vert", "glsl/basic.frag");
	}

	@Override
	protected void bindAttributes() {
		bindAttribute(0, "aPos");
		bindAttribute(1, "aColor");
		bindAttribute(2, "aTexCoord");
		bindAttribute(3, "aNormal");
	}

	@Override
	protected void getAllUniformLocations() {
		super.getAllUniformLocations();
		location_model         = getUniformLocation("model");
		location_view          = getUniformLocation("view");
		location_projection    = getUniformLocation("projection");
		location_normalMatrix  = getUniformLocation("normalMatrix");
		location_useTexture    = getUniformLocation("useTexture");
		location_useLighting   = getUniformLocation("useLighting");
		location_ambientColor  = getUniformLocation("ambientColor");
		location_lightPosition = getUniformLocation("lightPosition");
		location_lightColor    = getUniformLocation("lightColor");
		location_viewPos       = getUniformLocation("viewPos");
		location_shininess     = getUniformLocation("shininess");
	}

	public void loadModelMatrix(final Matrix4f matrix) {
		float[] matArray = new float[16];
		matrix.get(matArray);
		GLES20.glUniformMatrix4fv(location_model, 1, false, matArray, 0);
	}

	public void loadViewMatrix(final Matrix4f matrix) {
		float[] matArray = new float[16];
		matrix.get(matArray);
		GLES20.glUniformMatrix4fv(location_view, 1, false, matArray, 0);
	}

	public void loadProjectionMatrix(final Matrix4f matrix) {
		float[] matArray = new float[16];
		matrix.get(matArray);
		GLES20.glUniformMatrix4fv(location_projection, 1, false, matArray, 0);
	}

	public void loadNormalMatrix(final Matrix4f modelMatrix) {
		float[] m = new float[16];
		modelMatrix.get(m);
		// Extract upper-left 3x3 and compute its inverse-transpose (normal matrix)
		Matrix3f nm = new Matrix3f();
		nm.set(
			m[0], m[4], m[8],
			m[1], m[5], m[9],
			m[2], m[6], m[10]);
		nm.invert().transpose();
		float[] result = new float[9];
		nm.get(result);
		GLES20.glUniformMatrix3fv(location_normalMatrix, 1, false, result, 0);
	}

	public void loadUseTexture(final boolean useTexture) {
		GLES20.glUniform1i(location_useTexture, useTexture ? 1 : 0);
	}

	public void loadUseLighting(final boolean useLighting) {
		GLES20.glUniform1i(location_useLighting, useLighting ? 1 : 0);
	}

	public void loadAmbientColor(final float r, final float g, final float b) {
		GLES20.glUniform3f(location_ambientColor, r, g, b);
	}

	public void loadLightPosition(final float x, final float y, final float z) {
		GLES20.glUniform3f(location_lightPosition, x, y, z);
	}

	public void loadLightColor(final float r, final float g, final float b) {
		GLES20.glUniform3f(location_lightColor, r, g, b);
	}

	public void loadViewPos(final float x, final float y, final float z) {
		GLES20.glUniform3f(location_viewPos, x, y, z);
	}

	public void loadShininess(final float shininess) {
		GLES20.glUniform1f(location_shininess, shininess);
	}

	@Override
	public boolean useNormal() { return true; }

	@Override
	public boolean useTexture() { return true; }

	@Override
	public int getTextureID() { return 0; }
}

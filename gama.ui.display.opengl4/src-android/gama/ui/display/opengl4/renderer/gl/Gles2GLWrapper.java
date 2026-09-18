package gama.ui.display.opengl4.renderer.gl;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import android.opengl.GLES20;

/**
 * Android GLES 2.0 implementation of {@link GLWrapper}.
 * Delegates all calls to {@link GLES20} static methods.
 */
public class Gles2GLWrapper implements GLWrapper {

	// ── Program operations ──────────────────────────────────────────────

	@Override public int glCreateProgram() { return GLES20.glCreateProgram(); }
	@Override public void glAttachShader(int program, int shader) { GLES20.glAttachShader(program, shader); }
	@Override public void glLinkProgram(int program) { GLES20.glLinkProgram(program); }
	@Override public void glValidateProgram(int program) { GLES20.glValidateProgram(program); }
	@Override public void glUseProgram(int program) { GLES20.glUseProgram(program); }
	@Override public void glBindAttribLocation(int program, int index, String name) { GLES20.glBindAttribLocation(program, index, name); }
	@Override public int glGetUniformLocation(int program, String name) { return GLES20.glGetUniformLocation(program, name); }

	// ── Shader operations ───────────────────────────────────────────────

	@Override public int glCreateShader(int type) { return GLES20.glCreateShader(type); }
	@Override public void glShaderSource(int shader, int count, String[] strings, int[] lengths, int offset) {
		// GLES20.glShaderSource only takes (int shader, String string)
		GLES20.glShaderSource(shader, strings[offset]);
	}
	@Override public void glCompileShader(int shader) { GLES20.glCompileShader(shader); }
	@Override public void glGetShaderiv(int shader, int pname, int[] params, int offset) { GLES20.glGetShaderiv(shader, pname, params, offset); }
	@Override public void glGetShaderInfoLog(int shader, int maxLength, int[] lengthOffset, int offset1, byte[] log, int offset2) {
		String infoLog = GLES20.glGetShaderInfoLog(shader);
		if (infoLog != null) {
			byte[] bytes = infoLog.getBytes();
			int len = Math.min(bytes.length, maxLength);
			System.arraycopy(bytes, 0, log, offset2, len);
		}
	}

	// ── Uniform operations ──────────────────────────────────────────────

	@Override public void glUniform1f(int location, float value) { GLES20.glUniform1f(location, value); }
	@Override public void glUniform1i(int location, int value) { GLES20.glUniform1i(location, value); }
	@Override public void glUniform3f(int location, float x, float y, float z) { GLES20.glUniform3f(location, x, y, z); }
	@Override public void glUniformMatrix4fv(int location, int count, boolean transpose, float[] matrix, int offset) { GLES20.glUniformMatrix4fv(location, count, transpose, matrix, offset); }

	// ── Buffer operations ───────────────────────────────────────────────
	// GLES 2.0 does not have VAO support. These are no-ops.

	@Override public void glGenVertexArrays(int n, int[] arrays, int offset) { /* no-op: VAO not available */ }
	@Override public void glBindVertexArray(int array) { /* no-op: VAO not available */ }
	@Override public void glDeleteVertexArrays(int n, int[] arrays, int offset) { /* no-op: VAO not available */ }

	@Override public void glGenBuffers(int n, int[] buffers, int offset) { GLES20.glGenBuffers(n, buffers, offset); }
	@Override public void glBindBuffer(int target, int buffer) { GLES20.glBindBuffer(target, buffer); }
	@Override public void glBufferData(int target, long size, Buffer data, int usage) { GLES20.glBufferData(target, (int) size, data, usage); }
	@Override public void glBufferSubData(int target, long offset, long size, Buffer data) { GLES20.glBufferSubData(target, (int) offset, (int) size, data); }
	@Override public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long offset) { GLES20.glVertexAttribPointer(index, size, type, normalized, stride, (int) offset); }
	@Override public void glEnableVertexAttribArray(int index) { GLES20.glEnableVertexAttribArray(index); }
	@Override public void glDisableVertexAttribArray(int index) { GLES20.glDisableVertexAttribArray(index); }
	@Override public void glVertexAttrib3f(int index, float x, float y, float z) { GLES20.glVertexAttrib3f(index, x, y, z); }
	@Override public void glVertexAttrib4f(int index, float x, float y, float z, float w) {
		// GLES20 does not have glVertexAttrib4f; use glVertexAttribPointer with a temp buffer
		float[] tmp = { x, y, z, w };
		ByteBuffer bb = ByteBuffer.allocateDirect(16).order(java.nio.ByteOrder.nativeOrder());
		bb.asFloatBuffer().put(tmp).flip();
		GLES20.glVertexAttribPointer(index, 4, GLES20.GL_FLOAT, false, 0, bb);
	}
	@Override public void glDeleteBuffers(int n, int[] buffers, int offset) { GLES20.glDeleteBuffers(n, buffers, offset); }

	// ── Drawing operations ──────────────────────────────────────────────

	@Override public void glDrawArrays(int mode, int first, int count) { GLES20.glDrawArrays(mode, first, count); }
	@Override public void glDrawElements(int mode, int count, int type, long offset) { GLES20.glDrawElements(mode, count, type, (int) offset); }
	@Override public void glDrawBuffer(int mode) { /* no-op: GLES2 only renders to GL_COLOR_ATTACHMENT0 */ }

	// ── Texture operations ──────────────────────────────────────────────

	@Override public void glGenTextures(int n, int[] textures, int offset) { GLES20.glGenTextures(n, textures, offset); }
	@Override public void glBindTexture(int target, int texture) { GLES20.glBindTexture(target, texture); }
	@Override public void glTexParameteri(int target, int pname, int param) { GLES20.glTexParameteri(target, pname, param); }
	@Override public void glTexParameterf(int target, int pname, float param) { GLES20.glTexParameterf(target, pname, param); }
	@Override public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, Buffer data) { GLES20.glTexImage2D(target, level, internalFormat, width, height, border, format, type, data); }
	@Override public void glDeleteTextures(int n, int[] textures, int offset) { GLES20.glDeleteTextures(n, textures, offset); }

	// ── Framebuffer operations ──────────────────────────────────────────

	@Override public void glGenFramebuffers(int n, int[] framebuffers, int offset) { GLES20.glGenFramebuffers(n, framebuffers, offset); }
	@Override public void glBindFramebuffer(int target, int framebuffer) { GLES20.glBindFramebuffer(target, framebuffer); }
	@Override public void glDeleteFramebuffers(int n, int[] framebuffers, int offset) { GLES20.glDeleteFramebuffers(n, framebuffers, offset); }
	@Override public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) { GLES20.glFramebufferTexture2D(target, attachment, textarget, texture, level); }

	// ── Renderbuffer operations ─────────────────────────────────────────

	@Override public void glGenRenderbuffers(int n, int[] renderbuffers, int offset) { GLES20.glGenRenderbuffers(n, renderbuffers, offset); }
	@Override public void glBindRenderbuffer(int target, int renderbuffer) { GLES20.glBindRenderbuffer(target, renderbuffer); }
	@Override public void glRenderbufferStorage(int target, int internalformat, int width, int height) { GLES20.glRenderbufferStorage(target, internalformat, width, height); }
	@Override public void glDeleteRenderbuffers(int n, int[] renderbuffers, int offset) { GLES20.glDeleteRenderbuffers(n, renderbuffers, offset); }
	@Override public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) { GLES20.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer); }

	// ── State operations ────────────────────────────────────────────────

	@Override public void glEnable(int cap) { GLES20.glEnable(cap); }
	@Override public void glDisable(int cap) { GLES20.glDisable(cap); }
	@Override public void glBlendFunc(int sfactor, int dfactor) { GLES20.glBlendFunc(sfactor, dfactor); }
	@Override public void glBlendColor(float r, float g, float b, float a) { GLES20.glBlendColor(r, g, b, a); }
	@Override public void glDepthFunc(int func) { GLES20.glDepthFunc(func); }
	@Override public void glDepthMask(boolean flag) { GLES20.glDepthMask(flag); }
	@Override public void glCullFace(int mode) { GLES20.glCullFace(mode); }
	@Override public void glFrontFace(int mode) { GLES20.glFrontFace(mode); }
	@Override public void glPolygonMode(int face, int mode) { /* no-op: not available in GLES2 */ }
	@Override public void glLineWidth(float width) { GLES20.glLineWidth(width); }
	@Override public void glHint(int target, int mode) { GLES20.glHint(target, mode); }
	@Override public void glFinish() { GLES20.glFinish(); }

	// ── Clear operations ────────────────────────────────────────────────

	@Override public void glClearColor(float r, float g, float b, float a) { GLES20.glClearColor(r, g, b, a); }
	@Override public void glClear(int mask) { GLES20.glClear(mask); }
	@Override public void glClearDepth(double depth) { GLES20.glClearDepthf((float) depth); }

	// ── Pixel operations ────────────────────────────────────────────────

	@Override public void glReadBuffer(int mode) { /* no-op: GLES2 only reads from color attachment 0 */ }
	@Override public void glReadPixels(int x, int y, int width, int height, int format, int type, Buffer data) { GLES20.glReadPixels(x, y, width, height, format, type, data); }

	// ── Viewport ────────────────────────────────────────────────────────

	@Override public void glViewport(int x, int y, int width, int height) { GLES20.glViewport(x, y, width, height); }

	// ── Attribute queries ───────────────────────────────────────────────

	@Override public void glGetIntegerv(int pname, int[] params, int offset) { GLES20.glGetIntegerv(pname, params, offset); }
	@Override public void glGetFloatv(int pname, float[] params, int offset) { /* GLES20 does not have glGetFloatv */ }
	@Override public void glGetString(int pname) { GLES20.glGetString(pname); }
}

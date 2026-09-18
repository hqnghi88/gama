package gama.ui.display.opengl4.renderer.gl;

import java.nio.Buffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL4;

/**
 * JOGL implementation of {@link GLWrapper}. Delegates all calls to the
 * underlying {@link GL4} instance.
 */
public class JoglGLWrapper implements GLWrapper {

	private final GL4 gl;

	public JoglGLWrapper(final GL4 gl) {
		this.gl = gl;
	}

	/** Returns the underlying JOGL GL4 for callers that still need it. */
	public GL4 getGL4() { return gl; }

	// ── Program operations ──────────────────────────────────────────────

	@Override public int glCreateProgram() { return gl.glCreateProgram(); }
	@Override public void glAttachShader(int program, int shader) { gl.glAttachShader(program, shader); }
	@Override public void glLinkProgram(int program) { gl.glLinkProgram(program); }
	@Override public void glValidateProgram(int program) { gl.glValidateProgram(program); }
	@Override public void glUseProgram(int program) { gl.glUseProgram(program); }
	@Override public void glBindAttribLocation(int program, int index, String name) { gl.glBindAttribLocation(program, index, name); }
	@Override public int glGetUniformLocation(int program, String name) { return gl.glGetUniformLocation(program, name); }

	// ── Shader operations ───────────────────────────────────────────────

	@Override public int glCreateShader(int type) { return gl.glCreateShader(type); }
	@Override public void glShaderSource(int shader, int count, String[] strings, int[] lengths, int offset) { gl.glShaderSource(shader, count, strings, lengths, offset); }
	@Override public void glCompileShader(int shader) { gl.glCompileShader(shader); }
	@Override public void glGetShaderiv(int shader, int pname, int[] params, int offset) { gl.glGetShaderiv(shader, pname, params, offset); }
	@Override public void glGetShaderInfoLog(int shader, int maxLength, int[] lengthOffset, int offset1, byte[] log, int offset2) { gl.glGetShaderInfoLog(shader, maxLength, lengthOffset, offset1, log, offset2); }

	// ── Uniform operations ──────────────────────────────────────────────

	@Override public void glUniform1f(int location, float value) { gl.glUniform1f(location, value); }
	@Override public void glUniform1i(int location, int value) { gl.glUniform1i(location, value); }
	@Override public void glUniform3f(int location, float x, float y, float z) { gl.glUniform3f(location, x, y, z); }
	@Override public void glUniformMatrix4fv(int location, int count, boolean transpose, float[] matrix, int offset) { gl.glUniformMatrix4fv(location, count, transpose, matrix, offset); }

	// ── Buffer operations ───────────────────────────────────────────────

	@Override public void glGenVertexArrays(int n, int[] arrays, int offset) { gl.glGenVertexArrays(n, arrays, offset); }
	@Override public void glBindVertexArray(int array) { gl.glBindVertexArray(array); }
	@Override public void glDeleteVertexArrays(int n, int[] arrays, int offset) { gl.glDeleteVertexArrays(n, arrays, offset); }
	@Override public void glGenBuffers(int n, int[] buffers, int offset) { gl.glGenBuffers(n, buffers, offset); }
	@Override public void glBindBuffer(int target, int buffer) { gl.glBindBuffer(target, buffer); }
	@Override public void glBufferData(int target, long size, Buffer data, int usage) { gl.glBufferData(target, size, data, usage); }
	@Override public void glBufferSubData(int target, long offset, long size, Buffer data) { gl.glBufferSubData(target, offset, size, data); }
	@Override public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long offset) { gl.glVertexAttribPointer(index, size, type, normalized, stride, offset); }
	@Override public void glEnableVertexAttribArray(int index) { gl.glEnableVertexAttribArray(index); }
	@Override public void glDisableVertexAttribArray(int index) { gl.glDisableVertexAttribArray(index); }
	@Override public void glVertexAttrib3f(int index, float x, float y, float z) { gl.glVertexAttrib3f(index, x, y, z); }
	@Override public void glVertexAttrib4f(int index, float x, float y, float z, float w) { gl.glVertexAttrib4f(index, x, y, z, w); }
	@Override public void glDeleteBuffers(int n, int[] buffers, int offset) { gl.glDeleteBuffers(n, buffers, offset); }

	// ── Drawing operations ──────────────────────────────────────────────

	@Override public void glDrawArrays(int mode, int first, int count) { gl.glDrawArrays(mode, first, count); }
	@Override public void glDrawElements(int mode, int count, int type, long offset) { gl.glDrawElements(mode, count, type, offset); }
	@Override public void glDrawBuffer(int mode) { gl.glDrawBuffer(mode); }

	// ── Texture operations ──────────────────────────────────────────────

	@Override public void glGenTextures(int n, int[] textures, int offset) { gl.glGenTextures(n, textures, offset); }
	@Override public void glBindTexture(int target, int texture) { gl.glBindTexture(target, texture); }
	@Override public void glTexParameteri(int target, int pname, int param) { gl.glTexParameteri(target, pname, param); }
	@Override public void glTexParameterf(int target, int pname, float param) { gl.glTexParameterf(target, pname, param); }
	@Override public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, Buffer data) { gl.glTexImage2D(target, level, internalFormat, width, height, border, format, type, data); }
	@Override public void glDeleteTextures(int n, int[] textures, int offset) { gl.glDeleteTextures(n, textures, offset); }

	// ── Framebuffer operations ──────────────────────────────────────────

	@Override public void glGenFramebuffers(int n, int[] framebuffers, int offset) { gl.glGenFramebuffers(n, framebuffers, offset); }
	@Override public void glBindFramebuffer(int target, int framebuffer) { gl.glBindFramebuffer(target, framebuffer); }
	@Override public void glDeleteFramebuffers(int n, int[] framebuffers, int offset) { gl.glDeleteFramebuffers(n, framebuffers, offset); }
	@Override public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) { gl.glFramebufferTexture2D(target, attachment, textarget, texture, level); }

	// ── Renderbuffer operations ─────────────────────────────────────────

	@Override public void glGenRenderbuffers(int n, int[] renderbuffers, int offset) { gl.glGenRenderbuffers(n, renderbuffers, offset); }
	@Override public void glBindRenderbuffer(int target, int renderbuffer) { gl.glBindRenderbuffer(target, renderbuffer); }
	@Override public void glRenderbufferStorage(int target, int internalformat, int width, int height) { gl.glRenderbufferStorage(target, internalformat, width, height); }
	@Override public void glDeleteRenderbuffers(int n, int[] renderbuffers, int offset) { gl.glDeleteRenderbuffers(n, renderbuffers, offset); }
	@Override public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) { gl.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer); }

	// ── State operations ────────────────────────────────────────────────

	@Override public void glEnable(int cap) { gl.glEnable(cap); }
	@Override public void glDisable(int cap) { gl.glDisable(cap); }
	@Override public void glBlendFunc(int sfactor, int dfactor) { gl.glBlendFunc(sfactor, dfactor); }
	@Override public void glBlendColor(float r, float g, float b, float a) { gl.glBlendColor(r, g, b, a); }
	@Override public void glDepthFunc(int func) { gl.glDepthFunc(func); }
	@Override public void glDepthMask(boolean flag) { gl.glDepthMask(flag); }
	@Override public void glCullFace(int mode) { gl.glCullFace(mode); }
	@Override public void glFrontFace(int mode) { gl.glFrontFace(mode); }
	@Override public void glPolygonMode(int face, int mode) { gl.glPolygonMode(face, mode); }
	@Override public void glLineWidth(float width) { gl.glLineWidth(width); }
	@Override public void glHint(int target, int mode) { gl.glHint(target, mode); }
	@Override public void glFinish() { gl.glFinish(); }

	// ── Clear operations ────────────────────────────────────────────────

	@Override public void glClearColor(float r, float g, float b, float a) { gl.glClearColor(r, g, b, a); }
	@Override public void glClear(int mask) { gl.glClear(mask); }
	@Override public void glClearDepth(double depth) { gl.glClearDepth(depth); }

	// ── Pixel operations ────────────────────────────────────────────────

	@Override public void glReadBuffer(int mode) { gl.glReadBuffer(mode); }
	@Override public void glReadPixels(int x, int y, int width, int height, int format, int type, Buffer data) { gl.glReadPixels(x, y, width, height, format, type, data); }

	// ── Viewport ────────────────────────────────────────────────────────

	@Override public void glViewport(int x, int y, int width, int height) { gl.glViewport(x, y, width, height); }

	// ── Attribute queries ───────────────────────────────────────────────

	@Override public void glGetIntegerv(int pname, int[] params, int offset) { gl.glGetIntegerv(pname, params, offset); }
	@Override public void glGetFloatv(int pname, float[] params, int offset) { gl.glGetFloatv(pname, params, offset); }
	@Override public void glGetString(int pname) { gl.glGetString(pname); }

	// ── Context utility ─────────────────────────────────────────────────

	@Override public void setSwapInterval(int interval) { gl.setSwapInterval(interval); }
	@Override public Object getGLProfile() { return gl.getGLProfile(); }
	@Override public Object getContext() { return gl.getContext(); }
}

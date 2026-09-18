package gama.ui.display.opengl4.renderer.gl;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Abstract GL wrapper that decouples opengl4 from any concrete OpenGL binding.
 * Implementations exist for JOGL ({@link JoglGLWrapper}) and Android GLES 2.0
 * ({@link Gles2GLWrapper}).
 */
public interface GLWrapper {

	// ── Program operations ──────────────────────────────────────────────

	int glCreateProgram();

	void glAttachShader(int program, int shader);

	void glLinkProgram(int program);

	void glValidateProgram(int program);

	void glUseProgram(int program);

	void glBindAttribLocation(int program, int index, String name);

	int glGetUniformLocation(int program, String name);

	// ── Shader operations ───────────────────────────────────────────────

	int glCreateShader(int type);

	void glShaderSource(int shader, int count, String[] strings, int[] lengths, int offset);

	void glCompileShader(int shader);

	void glGetShaderiv(int shader, int pname, int[] params, int offset);

	void glGetShaderInfoLog(int shader, int maxLength, int[] lengthOffset, int offset1, byte[] log, int offset2);

	// ── Uniform operations ──────────────────────────────────────────────

	void glUniform1f(int location, float value);

	void glUniform1i(int location, int value);

	void glUniform3f(int location, float x, float y, float z);

	void glUniformMatrix4fv(int location, int count, boolean transpose, float[] matrix, int offset);

	// ── Buffer operations (VBO / IBO / VAO) ─────────────────────────────

	void glGenVertexArrays(int n, int[] arrays, int offset);

	void glBindVertexArray(int array);

	void glDeleteVertexArrays(int n, int[] arrays, int offset);

	void glGenBuffers(int n, int[] buffers, int offset);

	void glBindBuffer(int target, int buffer);

	void glBufferData(int target, long size, Buffer data, int usage);

	void glBufferSubData(int target, long offset, long size, Buffer data);

	void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long offset);

	void glEnableVertexAttribArray(int index);

	void glDisableVertexAttribArray(int index);

	void glVertexAttrib3f(int index, float x, float y, float z);

	void glVertexAttrib4f(int index, float x, float y, float z, float w);

	void glDeleteBuffers(int n, int[] buffers, int offset);

	// ── Drawing operations ──────────────────────────────────────────────

	void glDrawArrays(int mode, int first, int count);

	void glDrawElements(int mode, int count, int type, long offset);

	void glDrawBuffer(int mode);

	// ── Texture operations ──────────────────────────────────────────────

	void glGenTextures(int n, int[] textures, int offset);

	void glBindTexture(int target, int texture);

	void glTexParameteri(int target, int pname, int param);

	void glTexParameterf(int target, int pname, float param);

	void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format,
			int type, Buffer data);

	void glDeleteTextures(int n, int[] textures, int offset);

	// ── Framebuffer operations ──────────────────────────────────────────

	void glGenFramebuffers(int n, int[] framebuffers, int offset);

	void glBindFramebuffer(int target, int framebuffer);

	void glDeleteFramebuffers(int n, int[] framebuffers, int offset);

	void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level);

	// ── Renderbuffer operations ─────────────────────────────────────────

	void glGenRenderbuffers(int n, int[] renderbuffers, int offset);

	void glBindRenderbuffer(int target, int renderbuffer);

	void glRenderbufferStorage(int target, int internalformat, int width, int height);

	void glDeleteRenderbuffers(int n, int[] renderbuffers, int offset);

	void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer);

	// ── State operations ────────────────────────────────────────────────

	void glEnable(int cap);

	void glDisable(int cap);

	void glBlendFunc(int sfactor, int dfactor);

	void glBlendColor(float r, float g, float b, float a);

	void glDepthFunc(int func);

	void glDepthMask(boolean flag);

	void glCullFace(int mode);

	void glFrontFace(int mode);

	void glPolygonMode(int face, int mode);

	void glLineWidth(float width);

	void glHint(int target, int mode);

	void glFinish();

	// ── Clear operations ────────────────────────────────────────────────

	void glClearColor(float r, float g, float b, float a);

	void glClear(int mask);

	void glClearDepth(double depth);

	// ── Pixel operations ────────────────────────────────────────────────

	void glReadBuffer(int mode);

	void glReadPixels(int x, int y, int width, int height, int format, int type, Buffer data);

	// ── Viewport ────────────────────────────────────────────────────────

	void glViewport(int x, int y, int width, int height);

	// ── Attribute queries ───────────────────────────────────────────────

	void glGetIntegerv(int pname, int[] params, int offset);

	void glGetFloatv(int pname, float[] params, int offset);

	void glGetString(int pname);

	// ── Context utility (optional, JOGL-only if needed) ─────────────────

	default void setSwapInterval(int interval) { /* no-op by default */ }

	default Object getGLProfile() { return null; }

	default Object getContext() { return null; }
}

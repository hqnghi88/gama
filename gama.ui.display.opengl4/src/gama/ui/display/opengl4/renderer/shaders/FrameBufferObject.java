/*******************************************************************************************************
 *
 * FrameBufferObject.java, in gama.ui.display.opengl4, is part of the source code of the
 * GAMA modeling and simulation platform .
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.display.opengl4.renderer.shaders;

import java.nio.ByteBuffer;

import android.opengl.GLES20;

/**
 * The Class FrameBufferObject.
 */
public class FrameBufferObject {

	/** The width. */
	private int width;

	/** The height. */
	private int height;

	/** The frame buffer array. */
	private final int[] frameBufferArray = new int[] { -1 };

	/** The depth buffer array. */
	private final int[] depthBufferArray = new int[] { -1 };

	/** The depth buffer texture array. */
	private final int[] depthBufferTextureArray = new int[] { -1 };

	/** The texture array. */
	private final int[] textureArray = new int[] { -1 };

	/**
	 * Instantiates a new frame buffer object.
	 *
	 * @param width the width
	 * @param height the height
	 */
	public FrameBufferObject(final int width, final int height) {
		setDisplayDimensions(width, height);
		initialiseFrameBuffer();
	}

	/**
	 * Sets the display dimensions.
	 *
	 * @param width the width
	 * @param height the height
	 */
	public void setDisplayDimensions(final int width, final int height) {
		this.width = width;
		this.height = height;
		initialiseFrameBuffer();
	}

	/**
	 * Gets the frame buffer id.
	 *
	 * @return the frame buffer id
	 */
	public int getFrameBufferId() {
		return frameBufferArray[0];
	}

	/**
	 * Clean up.
	 */
	public void cleanUp() {// call when closing
		GLES20.glDeleteFramebuffers(1, frameBufferArray, 0);
		GLES20.glDeleteTextures(1, textureArray, 0);
		GLES20.glDeleteTextures(1, depthBufferTextureArray, 0);
		GLES20.glDeleteRenderbuffers(1, depthBufferArray, 0);
	}

	/**
	 * Bind frame buffer.
	 */
	public void bindFrameBuffer() {// call before rendering to this FBO
		bindFrameBuffer(frameBufferArray[0], width, height);
	}

	/**
	 * Unbind current frame buffer.
	 */
	public void unbindCurrentFrameBuffer() {// call to switch to default frame buffer
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, width, height);
	}

	/**
	 * Gets the FBO texture.
	 *
	 * @return the FBO texture
	 */
	public int getFBOTexture() {// get the resulting texture
		return textureArray[0];
	}

	/**
	 * Gets the depth texture.
	 *
	 * @return the depth texture
	 */
	public int getDepthTexture() {// get the resulting depth texture
		return depthBufferTextureArray[0];
	}

	/**
	 * Initialise frame buffer.
	 */
	private void initialiseFrameBuffer() {
		createFrameBuffer();
		createTextureAttachment(width, height);
		createDepthBufferAttachment(width, height);
		unbindCurrentFrameBuffer();
	}

	/**
	 * Bind frame buffer.
	 *
	 * @param frameBuffer the frame buffer
	 * @param width the width
	 * @param height the height
	 */
	private void bindFrameBuffer(final int frameBuffer, final int width, final int height) {
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);// To make sure the texture isn't bound
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer);
		GLES20.glViewport(0, 0, width, height);
	}

	/**
	 * Creates the frame buffer.
	 *
	 * @return the int
	 */
	private int createFrameBuffer() {
		// Only clean up a previously valid FBO; frameBufferArray[0] == -1 means not yet allocated.
		if (frameBufferArray[0] != -1) { cleanUp(); }
		GLES20.glGenFramebuffers(1, frameBufferArray, 0);
		// generate name for frame buffer
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferArray[0]);
		// create the framebuffer
		// glDrawBuffer is not available in GLES20; color attachment 0 is used by default
		return frameBufferArray[0];
	}

	/**
	 * Creates the texture attachment.
	 *
	 * @param width the width
	 * @param height the height
	 * @return the int
	 */
	private int createTextureAttachment(final int width, final int height) {
		GLES20.glGenTextures(1, textureArray, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureArray[0]);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		// Use GL_LINEAR (not a mipmap filter) for a render-target texture.
		// GL_LINEAR_MIPMAP_LINEAR would require glGenerateMipmap and makes the FBO incomplete without it.
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, width, height, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE,
				(ByteBuffer) null);
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textureArray[0], 0);
		return textureArray[0];
	}

	// private int createDepthTextureAttachment(final int width, final int height) {
	// gl.glGenTextures(1, depthBufferTextureArray, 0);
	// gl.glBindTexture(GL4.GL_TEXTURE_2D, depthBufferTextureArray[0]);
	// gl.glTexImage2D(GL4.GL_TEXTURE_2D, 0, GL4.GL_DEPTH_COMPONENT32, width, height, 0, GL4.GL_DEPTH_COMPONENT,
	// GL4.GL_FLOAT, (ByteBuffer) null);
	// gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR);
	// gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR);
	// gl.glFramebufferTextureEXT(GL4.GL_FRAMEBUFFER, GL4.GL_DEPTH_ATTACHMENT, depthBufferTextureArray[0], 0);
	// return depthBufferTextureArray[0];
	// }

	/**
	 * Creates the depth buffer attachment.
	 *
	 * @param width the width
	 * @param height the height
	 * @return the int
	 */
	private int createDepthBufferAttachment(final int width, final int height) {
		GLES20.glGenRenderbuffers(1, depthBufferArray, 0);
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthBufferArray[0]);
		GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT, width, height);
		GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER,
				depthBufferArray[0]);
		return depthBufferArray[0];
	}

}
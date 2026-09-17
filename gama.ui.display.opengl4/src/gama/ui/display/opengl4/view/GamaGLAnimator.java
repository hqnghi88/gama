/*******************************************************************************************************
 *
 * GamaGLAnimator.java, in gama.ui.display.opengl4, is part of the source code of the GAMA modeling and simulation
 * platform (v.2025-03).
 *
 * (c) 2007-2025 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.display.opengl4.view;

import java.util.concurrent.TimeUnit;

import gama.api.utils.prefs.GamaPreferences;
import gama.api.utils.prefs.IPreferenceChangeListener.IPreferenceAfterChangeListener;
import gama.dev.DEBUG;
import gama.dev.THREADS;
import gama.ui.shared.utils.WorkbenchHelper;

/**
 * Single Thread Animator (with target FPS) — Android stub.
 *
 * @author Alexis Drogoul, loosely adapted from (aqd@5star.com.tw)
 */
public class GamaGLAnimator implements Runnable {

	/** The fps changed. */
	IPreferenceAfterChangeListener<Integer> fpsChanged = newValue -> targetFPS = newValue;

	/** The cap FPS. */
	protected volatile boolean capFPS = GamaPreferences.Displays.OPENGL_CAP_FPS.getValue();

	/** The target FPS. */
	protected volatile int targetFPS = GamaPreferences.Displays.OPENGL_FPS.getValue();

	/** The animator thread. */
	protected final Thread animatorThread;

	/** The display runnable. */
	private final Runnable displayRunnable;
	/** The stop requested. */
	protected volatile boolean stopRequested = false;

	/** The fps update frames interval. */
	private int fpsUpdateFramesInterval = 50;
	/** The fps total duration. */
	private long fpsStartTime, fpsLastUpdateTime, fpsLastPeriod, fpsTotalDuration;

	/** The fps total frames. */
	private int fpsTotalFrames;
	/** The fps total. */
	private float fpsLast, fpsTotal;

	public void resetFPSCounter() {
		fpsStartTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
		fpsLastUpdateTime = fpsStartTime;
		fpsLastPeriod = 0;
		fpsTotalFrames = 0;
		fpsLast = 0f;
		fpsTotal = 0f;
		fpsLastPeriod = 0;
		fpsTotalDuration = 0;
	}

	public int getUpdateFPSFrames() { return fpsUpdateFramesInterval; }

	public long getFPSStartTime() { return fpsStartTime; }

	public long getLastFPSUpdateTime() { return fpsLastUpdateTime; }

	public long getLastFPSPeriod() { return fpsLastPeriod; }

	public float getLastFPS() { return fpsLast; }

	public int getTotalFPSFrames() { return fpsTotalFrames; }

	public long getTotalFPSDuration() { return fpsTotalDuration; }

	public float getTotalFPS() { return fpsTotal; }

	public void setUpdateFPSFrames(final int frames) {
		fpsUpdateFramesInterval = frames;
	}

	/**
	 * Instantiates a new single thread GL animator.
	 *
	 * @param displayRunnable
	 *            the runnable to execute on each frame
	 */
	public GamaGLAnimator(final Runnable displayRunnable) {
		this.displayRunnable = displayRunnable;
		this.animatorThread = new Thread(this, "Animator thread");
		GamaPreferences.Displays.OPENGL_FPS.onChange(fpsChanged);
		setUpdateFPSFrames(50);
	}

	public boolean isStarted() { return animatorThread.isAlive(); }

	public Thread getThread() { return animatorThread; }

	public boolean start() {
		this.stopRequested = false;
		this.animatorThread.start();
		fpsStartTime = System.currentTimeMillis();
		return true;
	}

	public boolean stop() {
		this.stopRequested = true;
		if (WorkbenchHelper.isDisplayThread()) return true;
		try {
			this.animatorThread.join();
		} catch (final InterruptedException e) {} finally {
			this.stopRequested = false;
			GamaPreferences.Displays.OPENGL_FPS.removeChangeListener(fpsChanged);
		}
		return true;
	}

	public boolean isAnimating() { return true; }

	public boolean isPaused() { return false; }

	public boolean pause() { return false; }

	public boolean resume() { return true; }

	@Override
	public void run() {
		while (!stopRequested) {
			try {
				WorkbenchHelper.run(displayRunnable);
				if (capFPS) {
					final long frameDuration = 1000 / targetFPS;
					final long timeSleep = frameDuration - fpsLastPeriod;
					if (timeSleep >= 0) { THREADS.WAIT(timeSleep); }
				}
			} catch (final RuntimeException ex) {
				uncaughtException(ex);
			}
			tickFPS();
		}
	}

	public void uncaughtException(final Throwable cause) {
		DEBUG.ERR("Uncaught exception in animator: " + cause.getMessage());
		cause.printStackTrace();
	}

	/**
	 * Increases total frame count and updates values if feature is enabled and update interval is reached.
	 */
	public final void tickFPS() {
		fpsTotalFrames++;
		if (fpsUpdateFramesInterval > 0 && fpsTotalFrames % fpsUpdateFramesInterval == 0) {
			final long now = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
			fpsLastPeriod = now - fpsLastUpdateTime;
			fpsLastPeriod = Math.max(fpsLastPeriod, 1);
			fpsLast = fpsUpdateFramesInterval * 1000f / fpsLastPeriod;
			fpsTotalDuration = now - fpsStartTime;
			fpsTotalDuration = Math.max(fpsTotalDuration, 1);
			fpsTotal = fpsTotalFrames * 1000f / fpsTotalDuration;
			fpsLastUpdateTime = now;
		}
	}

}

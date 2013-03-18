package me.itzgeoff.vidsync.common;

/**
 * Does absolutely nothing with the provided values.
 * @author Geoff
 *
 */
public class SuppressedProgressListener implements ProgressListener {

	private static ProgressListener instance;

	public SuppressedProgressListener() {
	}

	@Override
	public void expectedTotal(long value) {
	}

	@Override
	public void update(long value) {
	}

	public static ProgressListener getInstance() {
		if (instance == null) {
			instance = new SuppressedProgressListener();
		}
		return instance;
	}

}

package me.itzgeoff.vidsync.common;

public interface ProgressListener {
	void expectedTotal(long value);
	
	void update(long value);
}

package me.itzgeoff.vidsync.common;

public interface ResultConsumer<I,O> {

	void consumeResult(I in, O result);
	void failedToReachResult(I in, Exception e);
}

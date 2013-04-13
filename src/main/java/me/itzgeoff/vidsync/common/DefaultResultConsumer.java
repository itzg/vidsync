package me.itzgeoff.vidsync.common;

public class DefaultResultConsumer<I, O> implements ResultConsumer<I, O> {

    @Override
    public void consumeResult(I in, O result) {
    }

    @Override
    public void failedToReachResult(I in, Exception e) {
    }

}

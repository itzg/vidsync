package me.itzgeoff.vidsync.common;

public class PercentilePrinterProgressListener implements ProgressListener {
	private long total;
	private int previousPercentile = 0;
	private String prefix;

	public PercentilePrinterProgressListener(String prefix) {
		this.prefix = prefix;
	}

	public void update(long value) {
		int p = (int)(100*((double)value/total));
		if (p != previousPercentile) {
			System.out.printf("%s %d%%%n", prefix, p);
			previousPercentile = p;
		}
	}
	
	@Override
	public void expectedTotal(long value) {
		total = value;
	}

}

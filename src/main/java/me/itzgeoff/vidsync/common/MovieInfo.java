package me.itzgeoff.vidsync.common;

public class MovieInfo {
	private String title;
	
	private long duration;
	
	@Override
	public String toString() {
		return title + ": duration="+duration;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}

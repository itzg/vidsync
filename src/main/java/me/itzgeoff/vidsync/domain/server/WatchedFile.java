package me.itzgeoff.vidsync.domain.server;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class WatchedFile implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private long id;
	
	private String path;
	
	private String title;
	
	private long lastModified;
	
	private String contentSignature;
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public long getLastModified() {
		return lastModified;
	}

	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public String getContentSignature() {
		return contentSignature;
	}

	public void setContentSignature(String contentSignature) {
		this.contentSignature = contentSignature;
	}

	public long getId() {
		return id;
	}

	public WatchedFile() {
	}
}

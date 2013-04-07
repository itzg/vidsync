package me.itzgeoff.vidsync.domain.common;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.springframework.core.style.ToStringCreator;

@Entity
public class WatchedFile implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private long id;
	
	private String path;
	
	private String title;
	
	private long lastModified;
	
	private long fileSize;
	
	private String contentSignature;
	
	@Transient
	private transient File theFile;

	public WatchedFile() {
	}
	
	@Override
	public String toString() {
		return new ToStringCreator(this)
		.append("id", id)
		.append("path", path)
		.toString();
	}
	
	public WatchedFile(File fromFile) throws IOException {
		this.theFile = fromFile;
		this.path = fromFile.getCanonicalPath();
		this.lastModified = fromFile.lastModified();
		this.fileSize = fromFile.length();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj == this) return true;
		if (!(obj instanceof WatchedFile)) return false;
		
		WatchedFile rhs = (WatchedFile) obj;
		return rhs.id == this.id;
	}
	
	@Override
	public int hashCode() {
		return (int)(id>>>32 ^ id);
	}
	
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

	public File getTheFile() {
		return theFile;
	}

	public void setTheFile(File theFile) {
		this.theFile = theFile;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}
}

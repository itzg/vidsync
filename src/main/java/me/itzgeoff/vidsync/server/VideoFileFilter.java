package me.itzgeoff.vidsync.server;

import java.io.File;
import java.util.regex.Pattern;

import javax.swing.filechooser.FileFilter;

public class VideoFileFilter extends FileFilter implements java.io.FileFilter {
	static Pattern filenamePattern = Pattern.compile(".*\\.(mp4|m4v)", Pattern.CASE_INSENSITIVE);

	@Override
	public boolean accept(File f) {
		return !f.isHidden() && f.isFile() && filenamePattern.matcher(f.getName()).matches();
	}

	@Override
	public String getDescription() {
		return "MP4/M4V video files";
	}

}

package me.itzgeoff.vidsync.server;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PathWatcher implements PreferencesConsumer {
	
	private static final Logger logger = LoggerFactory
			.getLogger(PathWatcher.class);
	
	private static final VideoFileFilter videoFileFilter = new VideoFileFilter();
	
	@Autowired
	private WatchedFileRouter router;
	
	@Override
	public void preferencesChanged(String key, String value) {
		if (key.equals(ConfigFactory.PREF_PATHS)) {
			logger.debug("Saw change in preferences. Will extract {}", value);
			
			if (value == null) {
				removeAllWatches();
			}
			else {
				ObjectMapper objectMapper = new ObjectMapper();
				try {
					@SuppressWarnings("unchecked")
					ArrayList<String> savedFilePaths = objectMapper.readValue(value, ArrayList.class);
					
					processChangedPaths(savedFilePaths);
				} catch (IOException e) {
					logger.warn("Trying to parse paths preference value from {}", e, value);
				}

			}
		}
	}

	private void processChangedPaths(ArrayList<String> savedFilePaths) {
		for (String path : savedFilePaths) {
			processAddedPath(new File(path));
		}
	}

	public void processAddedPath(File path) {
		logger.debug("Processing added path {}", path);
		
		File[] videoFiles = path.listFiles(videoFileFilter);
		if (videoFiles != null) {
			for (File videoFile : videoFiles) {
				router.in(videoFile);
			}
		}
	}

	private void removeAllWatches() {
		// TODO Auto-generated method stub
		
	}

}

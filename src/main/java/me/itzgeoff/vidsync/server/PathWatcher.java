package me.itzgeoff.vidsync.server;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import me.itzgeoff.vidsync.domain.server.WatchedFile;
import me.itzgeoff.vidsync.domain.server.WatchedFilesRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

@ServerComponent
public class PathWatcher implements PreferencesConsumer {
	
	private static final Logger logger = LoggerFactory
			.getLogger(PathWatcher.class);
	
	private static final VideoFileFilter videoFileFilter = new VideoFileFilter();
	
	@Autowired
	private WatchedFilesRepository repository;
	
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

	private void processAddedPath(File path) {
		logger.debug("Processing added path {}", path);
		
		File[] videoFiles = path.listFiles(videoFileFilter);
		if (videoFiles != null) {
			for (File videoFile : videoFiles) {
				try {
					WatchedFile entry = repository.findByPath(videoFile.getCanonicalPath());
					if (entry == null) {
						logger.debug("{} looks new to us", videoFile);
					}
				} catch (IOException e) {
					logger.error("Trying to get canonical form of {}", e, videoFile);
				}
			}
		}
	}

	private void removeAllWatches() {
		// TODO Auto-generated method stub
		
	}

}

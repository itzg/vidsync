package me.itzgeoff.vidsync.server;


import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PathWatcher implements PreferencesConsumer {
	
	private static final Logger logger = LoggerFactory
			.getLogger(PathWatcher.class);
	
	private static final VideoFileFilter videoFileFilter = new VideoFileFilter();
	
	@Autowired
	private WatchedFileRouter router;
	
    @Autowired
    private TaskScheduler taskScheduler;

    private Map<File, WatchKey> watches = new HashMap<>();
    
    private ScheduledFuture<Void> watcherTask;

    private long pathWatcherDelay = 500;
	
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
	    // Rather than compute removals, just wipe and re-add watches
	    removeAllWatches();
		for (String path : savedFilePaths) {
			processAddedPathToWatch(new File(path));
		}
	}

	public void processAddedPathToWatch(File asFile) {
		logger.debug("Processing added path {}", asFile);
		
		File[] videoFiles = asFile.listFiles(videoFileFilter);
		if (videoFiles != null) {
			for (File videoFile : videoFiles) {
				handleChangedFile(videoFile);
			}
		}
		
		Path asPath = asFile.toPath();
		try {
            WatchKey watchKey = asPath.register(FileSystems.getDefault().newWatchService(), ENTRY_CREATE, ENTRY_MODIFY);
            synchronized (watches) {
                watches.put(asFile, watchKey);
                
                if (watcherTask == null) {
                    createWatcherTask();
                }
            }
        } catch (IOException e) {
            logger.error("Trying to register watcher on {}", e, asPath);
        }
	}

    protected void createWatcherTask() {
        taskScheduler.scheduleWithFixedDelay(new Runnable() {
            
            @Override
            public void run() {
                pollWatchKeys();
            }
        }, pathWatcherDelay);
    }

    private void handleChangedFile(File videoFile) {
        logger.debug("Handling changed file {}", videoFile);
        
        router.in(videoFile);
    }

	protected void pollWatchKeys() {
        ArrayList<WatchKey> keys;
        synchronized (watches) {
            keys = new ArrayList<>(watches.values());
        }
        
        for (WatchKey watchKey : keys) {
            final Path pathWatched = (Path) watchKey.watchable();

            for (WatchEvent<?> event : watchKey.pollEvents()) {
                if (event.kind().equals(ENTRY_CREATE) || event.kind().equals(ENTRY_MODIFY)) {
                    Path relPathToFile = (Path) event.context();
                    handleChangedFile(pathWatched.resolve(relPathToFile).toFile());
                }
                else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Got some other event kind {}", event.kind().name());
                    }
                }
            }
            
            if (!watchKey.reset()) {
                logger.error("watchKey for {} is no longer valid", pathWatched);
            }
        }
        
        logger.trace("Finished polling watch keys");
    }

    private void removeAllWatches() {
        synchronized (watches) {
            for (WatchKey watchKey : watches.values()) {
                watchKey.cancel();
            }
            watches.clear();
            
            if (watcherTask != null) {
                watcherTask.cancel(false);
                watcherTask = null;
            }
        }
	}
    
}

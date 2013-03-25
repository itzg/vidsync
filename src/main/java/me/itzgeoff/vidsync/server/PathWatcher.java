package me.itzgeoff.vidsync.server;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServerComponent
public class PathWatcher implements PreferencesConsumer {
	
	private static final Logger logger = LoggerFactory
			.getLogger(PathWatcher.class);
	
	@Override
	public void preferencesChanged(String key, String value) {
		if (key.equals(ConfigFactory.PREF_PATHS)) {
			logger.debug("Saw change in preferences. Will extract {}", value);
		}
	}

}

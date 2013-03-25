package me.itzgeoff.vidsync.server;

import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServerComponent
public class PathWatcher implements PreferencesConsumer {
	
	private static final Logger logger = LoggerFactory
			.getLogger(PathWatcher.class);
	
	@Override
	public void preferencesChanged(Preferences prefs) {
		logger.debug("Saw change in preferences. Will extract {}", prefs.get(ConfigFactory.PREF_PATHS,"[]"));
	}

}

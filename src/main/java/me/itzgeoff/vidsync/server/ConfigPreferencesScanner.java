package me.itzgeoff.vidsync.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class ConfigPreferencesScanner {
	
	private static final Logger logger = LoggerFactory
			.getLogger(ConfigPreferencesScanner.class);

	@Autowired
	private TaskScheduler taskScheduler;
	
	@Autowired
	private PreferencesConsumer[] consumers;
	
	@Value("${preferencesScanPeriod}")
	private long preferencesScanPeriod;
	
	private Map<String, String> previousPrefs;
	
	@PostConstruct
	public void init() {
		taskScheduler.scheduleAtFixedRate(new Runnable() {
			private boolean running;
			
			@Override
			public void run() {
				if (running) {
					return;
				}
				running = true;
				try {
					scanPreferences();
				} catch (Exception e) {
					logger.error("Trying to scan preferences", e);
				} finally {
					running = false;
				}
			}
		}, preferencesScanPeriod);
	}

	protected void scanPreferences() {
		Preferences serverPreferences = ConfigFactory.createServerPreferences();
		
		boolean changes = false;
		
		Map<String, String> latestData = new HashMap<>();
		try {
			String[] keys = serverPreferences.keys();
			for (String key : keys) {
				String currentValue = serverPreferences.get(key, "");
				if (previousPrefs == null || !currentValue.equals(previousPrefs.get(key))) {
					fireChange(key, currentValue);
					changes = true;
				}

				latestData.put(key, currentValue);
			}
		} catch (BackingStoreException e) {
			logger.error("Trying to enumerate preferences", e);
			return;
		}
		
		// look for removals
		if (previousPrefs != null) {
			Set<String> previousKeys = new HashSet<>(previousPrefs.keySet());
			previousKeys.removeAll(latestData.keySet());
			for (String key : previousKeys) {
				fireChange(key, null);
				changes = true;
			}
		}
		
		if (changes) {
			previousPrefs = latestData;
		}
	}

	private void fireChange(String key, String value) {
		for (PreferencesConsumer consumer : consumers) {
			consumer.preferencesChanged(key, value);
		}
	}
}

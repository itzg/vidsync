package me.itzgeoff.vidsync.server;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;

@ServerComponent
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
		
		Map<String, String> latestData = new HashMap<>();
		try {
			String[] keys = serverPreferences.keys();
			for (String key : keys) {
				latestData.put(key, serverPreferences.get(key, null));
			}
		} catch (BackingStoreException e) {
			logger.error("Trying to enumerate preferences", e);
			return;
		}
		
		if (previousPrefs == null || !previousPrefs.equals(latestData)) {
			fireChange(serverPreferences);
			previousPrefs = latestData;
		}
		
	}

	private void fireChange(Preferences serverPreferences) {
		for (PreferencesConsumer consumer : consumers) {
			consumer.preferencesChanged(serverPreferences);
		}
	}
}

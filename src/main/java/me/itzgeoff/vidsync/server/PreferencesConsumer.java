package me.itzgeoff.vidsync.server;

import java.util.prefs.Preferences;

public interface PreferencesConsumer {
	void preferencesChanged(Preferences prefs);
}

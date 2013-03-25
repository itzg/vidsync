package me.itzgeoff.vidsync.server;


public interface PreferencesConsumer {
	void preferencesChanged(String key, String value);
}

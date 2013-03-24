package me.itzgeoff.vidsync.client;

import me.itzgeoff.vidsync.services.VidSyncClientService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class VidSyncClientServiceImpl implements VidSyncClientService {
	
	private static final Logger logger = LoggerFactory
			.getLogger(VidSyncClientServiceImpl.class);
	
	@Override
	public String hello() {
		logger.debug("Somebody said hello");
		return "and hello to you";
	}

}

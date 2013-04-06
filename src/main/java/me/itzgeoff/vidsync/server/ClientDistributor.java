package me.itzgeoff.vidsync.server;

import me.itzgeoff.vidsync.domain.server.WatchedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ClientDistributor {

	
	private static final Logger logger = LoggerFactory
			.getLogger(ClientDistributor.class);
	
	@Async("worker")
	public void distributeNewFile(WatchedFile watchedFile) {
		logger.debug("Will distribute {}", watchedFile);
		
	}

}

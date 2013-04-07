package me.itzgeoff.vidsync.services;

import java.io.IOException;

import me.itzgeoff.vidsync.common.OfferResponse;
import me.itzgeoff.vidsync.common.OfferType;
import me.itzgeoff.vidsync.domain.common.WatchedFile;

public interface VidSyncClientService {
	public String hello();
	
	public OfferResponse offer(WatchedFile watchedFile, OfferType type);

	/**
	 * 
	 * @param file
	 * @return the port where the raw content should be sent
	 * @throws IOException 
	 */
    public int prepareForTransfer(WatchedFile file) throws IOException;
}

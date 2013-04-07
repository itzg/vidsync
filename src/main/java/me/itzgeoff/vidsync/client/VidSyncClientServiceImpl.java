package me.itzgeoff.vidsync.client;

import java.io.File;
import java.io.IOException;

import me.itzgeoff.vidsync.common.OfferResponse;
import me.itzgeoff.vidsync.common.OfferType;
import me.itzgeoff.vidsync.domain.common.WatchedFile;
import me.itzgeoff.vidsync.domain.common.WatchedFilesRepository;
import me.itzgeoff.vidsync.services.VidSyncClientService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public abstract class VidSyncClientServiceImpl implements VidSyncClientService {

    private static final Logger logger = LoggerFactory.getLogger(VidSyncClientServiceImpl.class);
    
    @Autowired
    private WatchedFilesRepository repository;

    // Injected lookup method
    protected abstract Receiver createReceiver();
    
    @Override
    public String hello() {
        logger.debug("Somebody said hello");
        return "and hello to you";
    }

    @Override
    public OfferResponse offer(WatchedFile offeredWatchedFile, OfferType type) {
        logger.debug("Got offered {} of type {}", offeredWatchedFile, type);
        
        WatchedFile knownWatchedFile = repository.findByContentSignature(offeredWatchedFile.getContentSignature());
        if (knownWatchedFile != null) {
            if (new File(knownWatchedFile.getPath()).exists()) {
                return OfferResponse.NO_ACTION;
            }
        }

        return OfferResponse.SEND_CONTENT;
    }

    @Override
    public int prepareForTransfer(WatchedFile file) throws IOException {
        Receiver receiver = createReceiver();

        int port = receiver.createSocket(file);
        receiver.receive();
        
        return port;
    }

}

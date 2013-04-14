package me.itzgeoff.vidsync.client;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static me.itzgeoff.vidsync.domain.client.LocalClientProperty.CLIENT_ID_KEY;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import me.itzgeoff.vidsync.common.OfferResponse;
import me.itzgeoff.vidsync.common.OfferType;
import me.itzgeoff.vidsync.domain.client.LocalClientProperty;
import me.itzgeoff.vidsync.domain.client.LocalClientPropertyRepository;
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
    
    @Autowired
    private LocalClientPropertyRepository propertyRepository;
    
    @Autowired
    private ReceiverSupport receiverSupport;

    // Injected lookup method
    protected abstract Receiver createReceiver();
    
    @Override
    public String hello() {
        LocalClientProperty clientId = propertyRepository.findByKey(CLIENT_ID_KEY);
        if (clientId == null) {
            clientId = propertyRepository.save(new LocalClientProperty(
                    CLIENT_ID_KEY, UUID.randomUUID().toString()));

            logger.debug("Created new client ID {}", clientId);
        }
        
        logger.trace("Responding with client ID {}", clientId);
        return clientId.getValue();
    }

    @Override
    public OfferResponse offer(WatchedFile offeredWatchedFile, OfferType type) {
        logger.debug("Got offered {} of type {}", offeredWatchedFile, type);
        
        WatchedFile knownWatchedFile = repository.findByContentSignature(offeredWatchedFile.getContentSignature());
        if (knownWatchedFile != null) {
            // Make sure locally known file currently exists
            File asFile = new File(knownWatchedFile.getPath());
            if (asFile.exists()) {
                knownWatchedFile.setTheFile(asFile);
                resolveWatchFiles(offeredWatchedFile, knownWatchedFile);
                return OfferResponse.NO_ACTION;
            }
            else {
                // missing on filesystem, so purge our knowledge of it and treat as new content
                repository.delete(knownWatchedFile);
                knownWatchedFile = null;
            }
        }

        return OfferResponse.SEND_CONTENT;
    }

    /**
     * This method gets called when the content signature is known, but we need to double check
     * that the metadata hasn't changed and needs to be updated in our copy.
     * @param offeredWatchedFile
     * @param knownWatchedFile with 'theFile' property filled with actual File
     */
    protected void resolveWatchFiles(WatchedFile offeredWatchedFile, WatchedFile knownWatchedFile) {
        if (!offeredWatchedFile.getTitle().equals(knownWatchedFile.getTitle())) {
            retitleWatchFile(knownWatchedFile, offeredWatchedFile.getTitle());
        }
    }

    /**
     * 
     * @param knownWatchedFile with 'theFile' property filled with actual File
     * @param newTitle
     */
    protected void retitleWatchFile(WatchedFile knownWatchedFile, String newTitle) {

        // Rename the file
        Path newFilePath = receiverSupport.resolveTitleToFilePath(newTitle);

        logger.debug("Re-titling {} to {}", knownWatchedFile.getTheFile(), newFilePath);
        
        try {
            Files.move(knownWatchedFile.getTheFile().toPath(), newFilePath);
            
            knownWatchedFile.setPath(newFilePath.toString());
            repository.save(knownWatchedFile);
        } catch (IOException e) {
            logger.error("Trying to rename changed file", e);
        }
        
        // ...and TODO update the movie info box
    }

    @Override
    public int prepareForTransfer(WatchedFile file) throws IOException {
        Receiver receiver = createReceiver();

        int port = receiver.createSocket(file);
        receiver.receive();
        
        return port;
    }

}

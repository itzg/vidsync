/**
 * Copyright 2013 Geoff Bourne
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.itzgeoff.vidsync.client;

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
        final String offeredTitle = offeredWatchedFile.getTitle();
        final String knownTitle = knownWatchedFile.getTitle();
        
        if (!offeredTitle.equals(knownTitle)) {
            logger.debug("Observed change in title from '{}' to '{}' of {}", knownTitle, offeredTitle, knownWatchedFile);
            retitleWatchFile(knownWatchedFile, offeredTitle);
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
            knownWatchedFile.setTitle(newTitle);
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

package me.itzgeoff.vidsync.server;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jmdns.ServiceInfo;

import me.itzgeoff.vidsync.common.OfferResponse;
import me.itzgeoff.vidsync.common.OfferType;
import me.itzgeoff.vidsync.domain.common.WatchedFile;
import me.itzgeoff.vidsync.domain.common.WatchedFilesRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Async("worker")
public class ClientDistributor {

    private static final Logger logger = LoggerFactory.getLogger(ClientDistributor.class);
    
    private Map<String, ServerViewOfClientInstance> clients = 
            Collections.synchronizedMap(new HashMap<String, ServerViewOfClientInstance>());
    
    @Autowired
    private WatchedFilesRepository repository;
    
    @Autowired
    private ClientSender sender;

    public void distributeNewFile(WatchedFile watchedFile) {
        distributeOffer(watchedFile, OfferType.NEW);
    }

    public void distributeExistingFile(WatchedFile watchedFile) {
        distributeOffer(watchedFile, OfferType.EXISTING);
    }

    public void distributeChangedFile(WatchedFile watchedFile) {
        distributeOffer(watchedFile, OfferType.CHANGED);
    }

    protected void distributeOffer(WatchedFile watchedFile, OfferType type) {
        logger.debug("Distribute {}", watchedFile);
        
        if (watchedFile.getTheFile() == null) {
            watchedFile.setTheFile(new File(watchedFile.getPath()));
        }
        
        ArrayList<ServerViewOfClientInstance> views = new ArrayList<>(clients.values());
        for (ServerViewOfClientInstance view : views) {
            logger.debug("Offering {} of type {} to {}", watchedFile, type, view);
            OfferResponse response = view.getProxy().offer(watchedFile, type);
            logger.debug("Responded with {}", response, view);
            
            switch (response) {
            case SEND_CONTENT:
                sender.send(watchedFile, view);
                break;
                
            case NO_ACTION:
                break;
            }
        }
    }

    public void addClient(ServerViewOfClientInstance viewOfClient) {
        
        clients.put(viewOfClient.getServiceInfo().getURLs()[0], viewOfClient);
        
        for (WatchedFile watchedFile : repository.findAll()) {
            File asFile = new File(watchedFile.getPath());
            if (asFile.exists()) {
                watchedFile.setTheFile(asFile);
                distributeExistingFile(watchedFile);
            }
        }
    }

    public void removeClient(ServiceInfo info) {
        clients.remove(info.getURLs()[0]);
    }

}

package me.itzgeoff.vidsync.server;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jmdns.ServiceInfo;

import me.itzgeoff.vidsync.common.OfferResponse;
import me.itzgeoff.vidsync.common.OfferType;
import me.itzgeoff.vidsync.common.ResultConsumer;
import me.itzgeoff.vidsync.domain.common.WatchedFile;
import me.itzgeoff.vidsync.domain.common.WatchedFilesRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ClientDistributor {

    private static final Logger logger = LoggerFactory.getLogger(ClientDistributor.class);
    
    private Map<String, ServerViewOfClientInstance> clients = 
            Collections.synchronizedMap(new HashMap<String, ServerViewOfClientInstance>());
    
    @Autowired
    private WatchedFilesRepository repository;
    
    @Autowired
    private ClientSender sender;

    private Set<DistributionKey> distributionsInFlight = Collections.synchronizedSet(new HashSet<DistributionKey>());

    @Async("worker")
    public void distributeNewFile(WatchedFile watchedFile, ResultConsumer<WatchedFile,Boolean> resultConsumer) {
        distributeOffer(watchedFile, OfferType.NEW, resultConsumer);
    }

    @Async("worker")
    public void distributeExistingFile(WatchedFile watchedFile, ResultConsumer<WatchedFile,Boolean> resultConsumer) {
        distributeOffer(watchedFile, OfferType.EXISTING, resultConsumer);
    }

    @Async("worker")
    public void distributeChangedFile(WatchedFile watchedFile, ResultConsumer<WatchedFile,Boolean> resultConsumer) {
        distributeOffer(watchedFile, OfferType.CHANGED, resultConsumer);
    }
    
    private static class DistributionKey {
        private WatchedFile watchedFile;
        private ServiceInfo serviceInfo;
        
        public DistributionKey(WatchedFile watchedFile, ServiceInfo serviceInfo) {
            this.watchedFile = watchedFile;
            this.serviceInfo = serviceInfo;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (obj == this) return true;

            DistributionKey rhs = (DistributionKey) obj;
            
            return this.watchedFile.equals(rhs.watchedFile) &&
                    this.serviceInfo.getKey().equals(rhs.serviceInfo.getKey());
        }
        
        @Override
        public int hashCode() {
            return watchedFile.hashCode() ^ serviceInfo.getKey().hashCode();
        }
    }
    
    protected void distributeOffer(WatchedFile watchedFile, OfferType type, 
            final ResultConsumer<WatchedFile, Boolean> resultConsumer) {
        logger.debug("Distribute {} of type {}", watchedFile, type);
        
        if (watchedFile.getTheFile() == null) {
            watchedFile.setTheFile(new File(watchedFile.getPath()));
        }
        
        ArrayList<ServerViewOfClientInstance> views = new ArrayList<>(clients.values());
        for (ServerViewOfClientInstance view : views) {
            final DistributionKey distributionKey = new DistributionKey(watchedFile, view.getServiceInfo());
            if (distributionsInFlight.add(distributionKey)) {
                logger.debug("Offering {} of type {} to {}", watchedFile, type, view);
                OfferResponse response;
                try {
                    response = view.getProxy().offer(watchedFile, type);
                } catch (Exception e) {
                    logger.error("Unexpected error while offering to "+view, e);
                    distributionsInFlight.remove(distributionKey);
                    continue;
                }
                logger.debug("Response for {} is {}", watchedFile, response);
                
                switch (response) {
                case SEND_CONTENT:
                    sender.send(watchedFile, view, new ResultConsumer<WatchedFile, Boolean>() {
                        @Override
                        public void consumeResult(WatchedFile in, Boolean result) {
                            distributionsInFlight.remove(distributionKey);
                            if (resultConsumer != null) {
                                resultConsumer.consumeResult(in, result);
                            }
                        }

                        @Override
                        public void failedToReachResult(WatchedFile in, Exception e) {
                            distributionsInFlight.remove(distributionKey);
                            if (resultConsumer != null) {
                                resultConsumer.failedToReachResult(in, e);
                            }
                        }
                    });
                    break;
                    
                case NO_ACTION:
                    distributionsInFlight.remove(distributionKey);
                    break;
                }
            }
            else {
                logger.debug("{} is already being distributed", watchedFile);
                if (resultConsumer != null) {
                    resultConsumer.consumeResult(watchedFile, false);
                }
            }
        }
    }

    public void addClient(ServerViewOfClientInstance viewOfClient) {
        clients.put(viewOfClient.getServiceInfo().getURLs()[0], viewOfClient);
        
        for (WatchedFile watchedFile : repository.findAll()) {
            File asFile = new File(watchedFile.getPath());
            if (asFile.exists()) {
                watchedFile.setTheFile(asFile);
                distributeExistingFile(watchedFile, null);
            }
        }

    }

    public void removeClient(ServiceInfo info) {
        clients.remove(info.getURLs()[0]);
    }

}

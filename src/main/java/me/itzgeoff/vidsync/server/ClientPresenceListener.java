package me.itzgeoff.vidsync.server;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import me.itzgeoff.vidsync.common.VidSyncConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import com.yammer.metrics.Counter;
import com.yammer.metrics.MetricRegistry;

@Component
public class ClientPresenceListener {
	
	private static final Logger logger = LoggerFactory.getLogger(ClientPresenceListener.class);

	@Autowired
	private JmDNS jmDNS;
	
	@Autowired
	private TaskExecutor taskExecutor;
	
	@Autowired
	private ServerViewOfClientManager clientManager;
	
	@Autowired
	private ClientDistributor distributor;
	
    @Autowired
    private MetricRegistry metrics;
	
	private ServiceListener serviceListener;

    private Counter counterClientsAdded;

    private Counter counterClientsConnected;

	@PostConstruct
	public void register() {
	    counterClientsAdded = metrics.counter(MetricRegistry.name(ClientPresenceListener.class, "clients-added-total"));
	    counterClientsConnected = metrics.counter(MetricRegistry.name(ClientPresenceListener.class, "clients-connected-current"));
	    
		serviceListener = new ServiceListener() {
			
			@Override
			public void serviceResolved(ServiceEvent evt) {
				if (isClientName(evt.getName())) {
					handleClientResolved(evt.getInfo());
				}
				else {
				    logger.trace("Saw another service resolved: {}", evt.getInfo());
				}
			}
			
			@Override
			public void serviceRemoved(ServiceEvent evt) {
				if (isClientName(evt.getName())) {
					handleClientRemoved(evt.getInfo());
				}
			}
			
			@Override
			public void serviceAdded(ServiceEvent arg0) {
				// this one isn't too interesting...we'll wait for service resolved
			}
		};
		

		taskExecutor.execute(new Runnable() {
			@Override
			public void run() {
				logger.debug("Added service listener, now looking for services already running");

				ServiceInfo[] runningAlready = jmDNS.list(VidSyncConstants.MDNS_SERVICE_TYPE);
				if (runningAlready != null) {
					for (ServiceInfo serviceInfo : runningAlready) {
						if (isClientName(serviceInfo.getName())) {
							handleClientResolved(serviceInfo);
						}
					}
				}
				
		        jmDNS.addServiceListener(VidSyncConstants.MDNS_SERVICE_TYPE, serviceListener);
			}
		});
	}

    private boolean isClientName(String mdnsName) {
        return mdnsName.startsWith(VidSyncConstants.MDNS_NAME_VIDSYNC_CLIENT);
    }

	protected void handleClientRemoved(ServiceInfo info) {
	    logger.debug("Saw removal of {}", info);
	    
	    distributor.removeClient(info);
	}

	protected void handleClientResolved(ServiceInfo info) {
		logger.debug("Resolved {}", info);
		counterClientsAdded.inc();
		counterClientsConnected.inc();
		
		ServerViewOfClientInstance viewOfClient = clientManager.createViewOfClient(info);
		
		distributor.addClient(viewOfClient);
	}

	@PreDestroy
	public void deregister() {
	    counterClientsConnected.dec();
		jmDNS.removeServiceListener(VidSyncConstants.MDNS_SERVICE_TYPE, serviceListener);
	}

}

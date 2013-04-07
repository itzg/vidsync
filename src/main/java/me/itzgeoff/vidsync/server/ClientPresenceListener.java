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
	
	private ServiceListener serviceListener;
	
	@PostConstruct
	public void register() {
		serviceListener = new ServiceListener() {
			
			@Override
			public void serviceResolved(ServiceEvent evt) {
				if (evt.getName().equals(VidSyncConstants.MDNS_NAME_VIDSYNC_CLIENT)) {
					handleClientResolved(evt.getInfo());
				}
			}
			
			@Override
			public void serviceRemoved(ServiceEvent evt) {
				if (evt.getName().equals(VidSyncConstants.MDNS_NAME_VIDSYNC_CLIENT)) {
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
						if (serviceInfo.getName().equals(VidSyncConstants.MDNS_NAME_VIDSYNC_CLIENT)) {
							handleClientResolved(serviceInfo);
						}
					}
				}
				
		        jmDNS.addServiceListener(VidSyncConstants.MDNS_SERVICE_TYPE, serviceListener);
			}
		});
	}
	
	protected void handleClientRemoved(ServiceInfo info) {
	    logger.debug("Saw removal of {}", info);
	    
	    distributor.removeClient(info);
	}

	protected void handleClientResolved(ServiceInfo info) {
		logger.debug("Resolved {}", info);
		
		ServerViewOfClientInstance viewOfClient = clientManager.createViewOfClient(info);
		
		distributor.addClient(viewOfClient);
	}

	@PreDestroy
	public void deregister() {
		jmDNS.removeServiceListener(VidSyncConstants.MDNS_SERVICE_TYPE, serviceListener);
	}
}

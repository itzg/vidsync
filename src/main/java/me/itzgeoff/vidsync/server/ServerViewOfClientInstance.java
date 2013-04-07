package me.itzgeoff.vidsync.server;

import javax.jmdns.ServiceInfo;

import me.itzgeoff.vidsync.services.VidSyncClientService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.style.ToStringCreator;

public class ServerViewOfClientInstance {
	private static final Logger logger = LoggerFactory.getLogger(ServerViewOfClientInstance.class);

	@Autowired
	private ServerViewOfClientManager clientManager;
	
	private ServiceInfo serviceInfo;

	private VidSyncClientService proxy;
	
	@Override
	public String toString() {
	    return new ToStringCreator(this)
	    .append("serviceInfo", serviceInfo)
	    .toString();
	}

	public ServiceInfo getServiceInfo() {
		return serviceInfo;
	}

	public void setServiceInfo(ServiceInfo serviceInfo) {
		this.serviceInfo = serviceInfo;
		logger.debug("Created proxy of remote service {}", proxy);
		proxy = clientManager.createClientServiceProxy(serviceInfo);
		
		String response = proxy.hello();
		logger.debug("Response from client was '{}'", response);
	}
	
	public VidSyncClientService getProxy() {
        return proxy;
    }
}

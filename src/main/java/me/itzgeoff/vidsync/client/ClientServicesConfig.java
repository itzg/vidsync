package me.itzgeoff.vidsync.client;

import java.io.IOException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import me.itzgeoff.vidsync.common.DynamicRmiServiceExporter;
import me.itzgeoff.vidsync.common.VidSyncConstants;
import me.itzgeoff.vidsync.services.VidSyncClientService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("client")
public class ClientServicesConfig {
	private static final Logger logger = LoggerFactory.getLogger(ClientServicesConfig.class);
	
	@Autowired
	private VidSyncClientService vidsyncService;
	
	@Bean
	public DynamicRmiServiceExporter vidsyncServiceExporter() {
		DynamicRmiServiceExporter exporter = new DynamicRmiServiceExporter();
		
		exporter.setServiceName(VidSyncConstants.RMI_SERVICE_CLIENT);
		exporter.setService(vidsyncService);
		exporter.setServiceInterface(VidSyncClientService.class);
		
		return exporter;
	}
	
	@Bean
	@Autowired
	public ServiceInfo vidsyncRmiMdnsServiceInfo(JmDNS jmDNS, DynamicRmiServiceExporter rmiExporter) throws IOException {
		ServiceInfo serviceInfo = ServiceInfo.create(VidSyncConstants.MDNS_SERVICE_TYPE, 
				VidSyncConstants.MDNS_NAME_VIDSYNC_CLIENT, 
				rmiExporter.getRmiRegistryPort(), 
				"VidSync Client RMI Service");
		
		jmDNS.registerService(serviceInfo);
		
		logger.info("Registered mDNS service {}", serviceInfo);
		
		return serviceInfo;
	}

}

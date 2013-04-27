package me.itzgeoff.vidsync.server;

import java.io.IOException;

import me.itzgeoff.vidsync.common.DynamicRmiServiceExporter;
import me.itzgeoff.vidsync.common.ServiceDiscovery;
import me.itzgeoff.vidsync.common.ServiceDiscovery.Service;
import me.itzgeoff.vidsync.common.VidSyncConstants;
import me.itzgeoff.vidsync.services.VidSyncServerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("server")
public class ServerServicesConfig {
	private static final Logger logger = LoggerFactory.getLogger(ServerServicesConfig.class);
	
	@Autowired
	private VidSyncServerService vidsyncService;
	
	@Bean
	public DynamicRmiServiceExporter vidsyncServiceExporter() {
		DynamicRmiServiceExporter exporter = new DynamicRmiServiceExporter();
		
		exporter.setServiceName("VidSyncServerService");
		exporter.setService(vidsyncService);
		exporter.setServiceInterface(VidSyncServerService.class);
		
		return exporter;
	}
	
	@Bean
	@Autowired
	public Service vidsyncRmiMdnsServiceInfo(ServiceDiscovery serviceDiscovery, 
	        DynamicRmiServiceExporter rmiExporter) throws IOException {

		return serviceDiscovery.registerService()
		.named(VidSyncConstants.MDNS_NAME_VIDSYNC_SERVER)
		.onPort(rmiExporter.getRmiRegistryPort())
		.done();
		
	}

}

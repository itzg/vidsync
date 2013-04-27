package me.itzgeoff.vidsync.client;

import java.io.IOException;

import me.itzgeoff.vidsync.common.DynamicRmiServiceExporter;
import me.itzgeoff.vidsync.common.ServiceDiscovery;
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
	private ClientAppConfig appConfig;
	
	@Autowired
	private VidSyncClientService vidsyncService;
	
	@Bean
	public VidSyncClientService vidSyncClientService() {
	    return new VidSyncClientServiceImpl() {
            
            @Override
            protected Receiver createReceiver() {
                return appConfig.receiver();
            }
        };
	}
	
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
	public ServiceDiscovery.Service vidsyncRmiMdnsServiceInfo(ServiceDiscovery serviceDiscovery, DynamicRmiServiceExporter rmiExporter) throws IOException {
		return serviceDiscovery.registerService()
		.named(VidSyncConstants.MDNS_NAME_VIDSYNC_CLIENT)
		.onPort(rmiExporter.getRmiRegistryPort())
		.done();
	}

}

package me.itzgeoff.vidsync.server;

import java.io.IOException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import me.itzgeoff.vidsync.common.DynamicRmiServiceExporter;
import me.itzgeoff.vidsync.common.JmDNSCloser;
import me.itzgeoff.vidsync.common.VidSyncConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerServicesConfig {
	private static final Logger logger = LoggerFactory.getLogger(ServerServicesConfig.class);
	
	@Autowired
	private VidsyncService vidsyncService;
	
	@Bean
	public DynamicRmiServiceExporter vidsyncServiceExporter() {
		DynamicRmiServiceExporter exporter = new DynamicRmiServiceExporter();
		
		exporter.setServiceName("VidsyncService");
		exporter.setService(vidsyncService);
		exporter.setServiceInterface(VidsyncService.class);
		
		return exporter;
	}
	
	@Bean
	@Autowired
	public JmDNS jmDNS(JmDNSCloser closer) throws IOException {
		JmDNS jmDNS = JmDNS.create();
		closer.setJmDNS(jmDNS);
		return jmDNS;
	}
	
	@Bean
	@Autowired
	public ServiceInfo vidsyncRmiMdnsServiceInfo(JmDNS jmDNS, DynamicRmiServiceExporter rmiExporter) throws IOException {
		ServiceInfo serviceInfo = ServiceInfo.create("_rmi._tcp.local.", 
				VidSyncConstants.MDNS_NAME_VIDSYNC_SERVER, 
				rmiExporter.getRmiRegistryPort(), 
				"Vidsync RMI Service");
		
		jmDNS.registerService(serviceInfo);
		
		logger.info("Registered mDNS service. Advertising {}", serviceInfo);
		
		return serviceInfo;
	}
}

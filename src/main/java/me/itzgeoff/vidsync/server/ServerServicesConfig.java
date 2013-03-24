package me.itzgeoff.vidsync.server;

import java.io.IOException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.remoting.rmi.RmiServiceExporter;

@Configuration
public class ServerServicesConfig {
	private static final Logger logger = LoggerFactory.getLogger(ServerServicesConfig.class);

	@Value("${rmiRegistryPort}")
	private int rmiRegistryPort;
	
	@Autowired
	private VidsyncService vidsyncService;
	
	@Bean
	public RmiServiceExporter vidsyncServiceExporter() {
		RmiServiceExporter exporter = new RmiServiceExporter();
		

		exporter.setServiceName("VidsyncService");
		exporter.setService(vidsyncService);
		exporter.setServiceInterface(VidsyncService.class);
		exporter.setRegistryPort(rmiRegistryPort);
		exporter.setAlwaysCreateRegistry(true);
		
		return exporter;
	}
	
	@Bean
	public JmDNS jmDNS() throws IOException {
		JmDNS jmDNS = JmDNS.create();
		return jmDNS;
	}
	
	@Bean
	@Autowired
	public ServiceInfo vidsyncRmiMdnsServiceInfo(JmDNS jmDNS) throws IOException {
		logger.info("Created mDNS service");
		ServiceInfo serviceInfo = ServiceInfo.create("_rmi._tcp.local.", "vidsync", rmiRegistryPort, "Vidsync RMI Service");
		
		jmDNS.registerService(serviceInfo);
		
		return serviceInfo;
	}
}

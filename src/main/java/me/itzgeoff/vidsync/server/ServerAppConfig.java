package me.itzgeoff.vidsync.server;

import java.net.Inet4Address;

import javax.jmdns.ServiceInfo;

import me.itzgeoff.vidsync.common.VidSyncConstants;
import me.itzgeoff.vidsync.services.VidSyncClientService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;

@Configuration
public class ServerAppConfig {
	
	private static final Logger logger = LoggerFactory.getLogger(ServerAppConfig.class);

	@Bean
	public PropertyPlaceholderConfigurer propertyPlaceholder() {
		PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
		Resource[] resources = new Resource[] {
			new ClassPathResource("defaults.properties"),
			new FileSystemResource("config/vidsync.properties")
		};
		configurer.setIgnoreResourceNotFound(true);
		configurer.setLocations(resources);
		
		return configurer;
	}

	
	public RmiProxyFactoryBean dynamicRmiProxyFactory(String hostAddress, int port) {
		// NOTE: This is a "fake" bean creator in order to allow the passing of prototype specific arguments.
		// If was an actual @Bean @Scope("prototype") then Spring will attempt to autowire the arguments
		// from the Spring context.
		RmiProxyFactoryBean factoryBean = new RmiProxyFactoryBean();
		
		factoryBean.setServiceInterface(VidSyncClientService.class);
		
		// This dynamically/discovered URL is what drove the need for this fake bean creator.
		factoryBean.setServiceUrl(String.format("rmi://%s:%d/%s",
				hostAddress, port, VidSyncConstants.RMI_SERVICE_CLIENT));
		
		// This is another part of the fake-ness...make the code think it was blessed as a Spring bean and
		// trigger the RMI proxy construction.
		factoryBean.afterPropertiesSet();

		return factoryBean;
	}
	
	@Bean
	public ServerViewOfClientManager serverViewOfClientManager() {
		return new ServerViewOfClientManager() {
			
			@Override
			public ServerViewOfClientInstance createViewOfClient(
					ServiceInfo info) {
				ServerViewOfClientInstance instance = serverViewOfClientInstance();
				instance.setServiceInfo(info);
				return instance;
			}

			@Override
			public VidSyncClientService createClientServiceProxy(
					ServiceInfo info) {
				
				Inet4Address[] serviceAddresses = info.getInet4Addresses();
				if (serviceAddresses != null && serviceAddresses.length > 0) {
					if (serviceAddresses.length > 1) {
						logger.warn("Multiple addresses provided by {}, but only using first one", info);
					}

					RmiProxyFactoryBean factory = dynamicRmiProxyFactory(serviceAddresses[0].getHostAddress(), info.getPort());

					return (VidSyncClientService) factory.getObject();
				}
				else {
					throw new IllegalArgumentException("Provided ServiceInfo didn't contain any addresses");
				}
			}
		};
	}

	@Bean
	@Scope("prototype")
	public ServerViewOfClientInstance serverViewOfClientInstance() {
		return new ServerViewOfClientInstance();
	}
}

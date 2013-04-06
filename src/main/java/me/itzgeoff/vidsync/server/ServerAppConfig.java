package me.itzgeoff.vidsync.server;

import java.net.Inet4Address;
import java.util.concurrent.Executor;

import javax.jmdns.ServiceInfo;

import me.itzgeoff.vidsync.common.VidSyncConstants;
import me.itzgeoff.vidsync.services.VidSyncClientService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class ServerAppConfig {
	
	private static final Logger logger = LoggerFactory.getLogger(ServerAppConfig.class);
	
	@Autowired
	private Environment env;

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
	
	@Bean
	@Qualifier("signature")
	public TaskExecutor signatureExecutor(@Value("${signature.executor.maxPoolSize:2}") int signatureMaxPoolSize) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		
		logger.debug("Creating signature executor with pool size of {}", signatureMaxPoolSize);
		executor.setCorePoolSize(signatureMaxPoolSize);
		executor.setMaxPoolSize(signatureMaxPoolSize);
        executor.setThreadNamePrefix("SignatureScan-");

		return executor;
	}
	
	@Bean
	@Qualifier("worker")
	public Executor workerExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(getCoreCount());
		executor.setMaxPoolSize(getCoreCount());
		executor.setThreadNamePrefix("Worker-");
		return executor;
	}

	private int getCoreCount() {
		return Runtime.getRuntime().availableProcessors();
	}}

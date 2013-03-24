package me.itzgeoff.vidsync.server;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

@Configuration
public class ServerAppConfig {

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
}

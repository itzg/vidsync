package me.itzgeoff.vidsync.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class ClientAppConfig {
    

    @Bean
    public static PropertyPlaceholderConfigurer propertyPlaceholder() {
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        Resource[] resources = new Resource[] {
            new ClassPathResource("defaults.properties"),
            new FileSystemResource("config/vidsync.properties")
        };
        configurer.setIgnoreResourceNotFound(true);
        configurer.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
        configurer.setLocations(resources);
        
        return configurer;
    }

    @Bean
    @Qualifier("receiver")
    public TaskExecutor receiverExecutor(@Value("${client.receiver.maxPoolSize}") int maxPoolSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(maxPoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setThreadNamePrefix("Receiver-");
        
        return executor;
    }
    
    @Bean
    @Scope("prototype")
    public Receiver receiver() {
        return new Receiver();
    }

}

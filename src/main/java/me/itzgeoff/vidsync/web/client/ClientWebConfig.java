package me.itzgeoff.vidsync.web.client;

import me.itzgeoff.vidsync.client.ClientAppConfig;
import me.itzgeoff.vidsync.client.Receiver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
public class ClientWebConfig {
    
    @Autowired
    private ClientAppConfig appConfig;
    
    @Bean
    public VidSyncClientController vidSyncClientController() {
        return new VidSyncClientController() {
            
            @Override
            protected Receiver createReceiver() {
                return appConfig.receiver();
            }
        };
    }

}

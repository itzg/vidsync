package me.itzgeoff.vidsync.common;

import java.io.IOException;

import javax.jmdns.JmDNS;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JmDNSConfig {
	
	@Bean
	@Autowired
	public JmDNS jmDNS(JmDNSCloser closer) throws IOException {
		JmDNS jmDNS = JmDNS.create();
		closer.setJmDNS(jmDNS);
		return jmDNS;
	}

}

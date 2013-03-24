package me.itzgeoff.vidsync.common;

import javax.annotation.PreDestroy;
import javax.jmdns.JmDNS;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class JmDNSCloser {

	private JmDNS jmDNS;
	
	public JmDNS getJmDNS() {
		return jmDNS;
	}

	public void setJmDNS(JmDNS jmDNS) {
		this.jmDNS = jmDNS;
	}

	@PreDestroy
	public void close() {
		if (jmDNS != null) {
			jmDNS.unregisterAllServices();
		}
	}
}

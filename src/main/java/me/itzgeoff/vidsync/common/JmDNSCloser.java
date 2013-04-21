/**
 * Copyright 2013 Geoff Bourne
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.itzgeoff.vidsync.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.PreDestroy;
import javax.jmdns.JmDNS;
import javax.jmdns.JmDNS.Delegate;
import javax.jmdns.ServiceInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JmDNSCloser implements Delegate {
    
    private static final Logger logger = LoggerFactory.getLogger(JmDNSCloser.class);

    private List<JmDNS> instances = 
            Collections.synchronizedList(new ArrayList<JmDNS>());
	
	public void addJmDNS(JmDNS jmDNS) {
		instances.add(jmDNS);
		jmDNS.setDelegate(this);
		logger.debug("Watching in order to close {}", jmDNS);
	}

	@PreDestroy
	public void close() {
		synchronized (instances) {
		    logger.debug("Closing instances {}", instances);
		    for (JmDNS jmDNS : instances) {
                jmDNS.unregisterAllServices();
            }
        }
	}

    /* (non-Javadoc)
     * @see javax.jmdns.JmDNS.Delegate#cannotRecoverFromIOError(javax.jmdns.JmDNS, java.util.Collection)
     */
    @Override
    public void cannotRecoverFromIOError(JmDNS deadJmDNS, Collection<ServiceInfo> infos) {
        logger.debug("Handling cannot-recover of {} with registered {}", deadJmDNS, infos);
        
        try {
            deadJmDNS.unregisterAllServices();
            deadJmDNS.close();
            instances.remove(deadJmDNS);
        } catch (Exception e) {
            logger.warn("Trying to clean up dead JmDNS", e);
        }
        
        try {
            JmDNS jmDNS = JmDNS.create();
            for (ServiceInfo serviceInfo : infos) {
                jmDNS.registerService(serviceInfo);
            }
            logger.debug("Created a new JmDNS {} with {}", jmDNS, infos);
            addJmDNS(jmDNS);
        } catch (IOException e) {
            logger.error("Failed to create new JmDNS", e);
        }
    }
}

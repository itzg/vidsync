package me.itzgeoff.vidsync.client;

import java.io.IOException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import me.itzgeoff.vidsync.common.VidSyncConstants;
import me.itzgeoff.vidsync.web.client.ClientWebConfig;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

@Configuration
public class ClientServicesConfig {
    private static final Logger logger = LoggerFactory.getLogger(ClientServicesConfig.class);

    @Bean
    @Autowired
    public ServiceInfo vidsyncClientServiceInfo(JmDNS jmDNS, @Qualifier("jettyPort") int jettyPort) throws IOException {
        ServiceInfo serviceInfo = ServiceInfo.create(VidSyncConstants.MDNS_SERVICE_TYPE,
                VidSyncConstants.MDNS_NAME_VIDSYNC_CLIENT, jettyPort, "VidSync Client HTTP Service");

        jmDNS.registerService(serviceInfo);

        logger.info("Registered mDNS service {}", serviceInfo);

        return serviceInfo;
    }

    @Bean
    public ServletContextHandler servletContextHandler() {
        ServletContextHandler servletContextHandler = new ServletContextHandler();
        servletContextHandler.setContextPath("/");
        
        return servletContextHandler;
    }
    
    @Bean
    public ServletHolder springMvcServletHolder() {
        ServletHolder servletHandler = new ServletHolder(DispatcherServlet.class);
        servletHandler.setInitParameter("contextClass",
                AnnotationConfigWebApplicationContext.class.getName());
        servletHandler.setInitParameter("contextConfigLocation", ClientWebConfig.class.getName());
        servletHandler.setInitOrder(20);

        return servletHandler;
    }

    @Bean
	public Server jettyServer() throws Exception {
	    Server server = new Server(0);
	    
        servletContextHandler().addServlet(springMvcServletHolder(), "/**");
        server.setHandler(servletContextHandler());
	    
	    server.start();
	    
	    logger.debug("HTTP services available at {}", server.getURI().getPort());
	    
	    return server;
	}

    @Bean
    @Qualifier("jettyPort")
    public int jettyServerPort() throws Exception {
        return jettyServer().getURI().getPort();
    }

}

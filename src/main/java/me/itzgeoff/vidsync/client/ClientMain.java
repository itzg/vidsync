package me.itzgeoff.vidsync.client;

import me.itzgeoff.vidsync.common.VidSyncConstants;
import me.itzgeoff.vidsync.web.client.ClientWebConfig;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.PropertySource;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class ClientMain {
    
    
    private static final Logger logger = LoggerFactory.getLogger(ClientMain.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		  @SuppressWarnings("resource")
		  AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		  ctx.getEnvironment().setActiveProfiles("client");
		  ctx.scan(
				  // our package
				  ClientMain.class.getPackage().getName(),
				  // common package
				  VidSyncConstants.class.getPackage().getName()
				  );
		  ctx.getEnvironment().getPropertySources().addFirst(new PropertySource<String>(null){

            @Override
            public Object getProperty(String name) {
                // TODO Auto-generated method stub
                return null;
            }
		      
		  });
		  
		  ctx.refresh();
		  
		  ctx.registerShutdownHook();
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

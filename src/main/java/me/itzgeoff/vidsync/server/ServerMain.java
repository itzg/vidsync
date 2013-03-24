package me.itzgeoff.vidsync.server;

import me.itzgeoff.vidsync.common.VidSyncConstants;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ServerMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		  @SuppressWarnings("resource")
		  AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		  ctx.scan(
				  // server package
				  ServerMain.class.getPackage().getName(),
				  // common package
				  VidSyncConstants.class.getPackage().getName()
				  );
		  ctx.refresh();
		  
		  ctx.registerShutdownHook();
	}

}

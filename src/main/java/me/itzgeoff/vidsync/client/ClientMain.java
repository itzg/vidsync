package me.itzgeoff.vidsync.client;

import me.itzgeoff.vidsync.common.VidSyncConstants;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ClientMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		  @SuppressWarnings("resource")
		  AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		  ctx.scan(
				  // our package
				  ClientMain.class.getPackage().getName(),
				  // common package
				  VidSyncConstants.class.getPackage().getName()
				  );
		  ctx.refresh();
		  
		  ctx.registerShutdownHook();
	}

}

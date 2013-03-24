package me.itzgeoff.vidsync.server;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ServerMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		  @SuppressWarnings("resource")
		  AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		  ctx.scan(ServerMain.class.getPackage().getName());
		  ctx.refresh();
		  
		  ctx.registerShutdownHook();
	}

}

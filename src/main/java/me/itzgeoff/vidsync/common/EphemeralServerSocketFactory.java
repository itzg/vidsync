package me.itzgeoff.vidsync.common;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
class EphemeralServerSocketFactory implements RMIServerSocketFactory {

	ServerSocket createdSocket;

	@Override
	public ServerSocket createServerSocket(int port) throws IOException {
		if (createdSocket != null) {
			return createdSocket;
		}
		
		createdSocket = new ServerSocket(0);
		return createdSocket;
	}
	
}
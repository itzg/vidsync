package me.itzgeoff.vidsync.common;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.rmi.RmiServiceExporter;

public class DynamicRmiServiceExporter extends RmiServiceExporter {
// NOTE: this extends a @Component, so don't declare again here or Spring init fails
	
	private Registry createdRegistry;
	
	@Autowired
	private EphemeralServerSocketFactory ourServerSocketFactory;

	/**
	 * This method cannot be used since the exporter will always create its own.
	 */
	@Override
	public final void setRegistry(Registry registry) {
		if (registry != null) {
			throw new IllegalArgumentException("This exporter only uses dynamically created registries");
		}
	}

	/**
	 * This method cannot be used since the exporter will use an ephemeral port.
	 */
	@Override
	public final void setRegistryPort(int registryPort) {
		if (registryPort != 0) {
			throw new IllegalArgumentException("This exporter only uses ephemeral ports");
		}
	}
	
	/**
	 * This method cannot be used since the exporter will always create its own.
	 */
	@Override
	public void setAlwaysCreateRegistry(boolean on) {
		if (on == false) {
			throw new IllegalArgumentException("This exporter always creates a repository");
		}
	}
	
	/**
	 * Always creates a {@link Registry} bound to an ephermeal port.
	 */
	@Override
	protected Registry getRegistry(int registryPort,
			RMIClientSocketFactory csf,
			RMIServerSocketFactory ssf) throws RemoteException {
		if (createdRegistry != null) {
			return createdRegistry;
		}
		
		if (csf != null) {
			createdRegistry = LocateRegistry.createRegistry(0, csf, ourServerSocketFactory);
		}
		else {
			createdRegistry = LocateRegistry.createRegistry(0, null, ourServerSocketFactory);
		}
		
		return createdRegistry;
	}

	/**
	 * Always creates a {@link Registry} bound to an ephermeal port.
	 */
	@Override
	protected Registry getRegistry(String registryHost, int registryPort,
			RMIClientSocketFactory csf,
			RMIServerSocketFactory ssf) throws RemoteException {
		return getRegistry(0, csf, null);
	}

	/**
	 * Always creates a {@link Registry} bound to an ephermeal port.
	 */
	@Override
	protected Registry getRegistry(int registryPort) throws RemoteException {
		return getRegistry(0, null, null);
	}
	
	/**
	 * @return the previously created registry
	 * @throws IllegalStateException if the registry had not been previously created
	 */
	public Registry getRegistry() {
		if (createdRegistry == null) {
			throw new IllegalStateException("This bean hasn't been properly initialized since the registry hasn't been created yet.");
		}
		return createdRegistry;
	}

	public int getRmiRegistryPort() {
		if (createdRegistry == null || ourServerSocketFactory.createdSocket == null) {
			throw new IllegalStateException("This bean hasn't been properly initialized since the registry hasn't been created yet.");
		}
		return ourServerSocketFactory.createdSocket.getLocalPort();
	}
}

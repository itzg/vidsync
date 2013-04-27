package me.itzgeoff.vidsync.server;

import me.itzgeoff.vidsync.common.ServiceDiscovery.ServiceInstance;
import me.itzgeoff.vidsync.services.VidSyncClientService;

public abstract class ServerViewOfClientManager {
	public abstract ServerViewOfClientInstance  createViewOfClient(ServiceInstance service);
	
	public abstract VidSyncClientService createClientServiceProxy(ServiceInstance service);
}

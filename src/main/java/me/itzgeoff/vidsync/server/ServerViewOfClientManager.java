package me.itzgeoff.vidsync.server;

import javax.jmdns.ServiceInfo;

import me.itzgeoff.vidsync.services.VidSyncClientService;

public abstract class ServerViewOfClientManager {
	public abstract ServerViewOfClientInstance  createViewOfClient(ServiceInfo info);
	
	public abstract VidSyncClientService createClientServiceProxy(ServiceInfo info);
}

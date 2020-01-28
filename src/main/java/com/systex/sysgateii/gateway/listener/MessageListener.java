package com.systex.sysgateii.gateway.listener;

public interface MessageListener<T> {
	public void messageReceived(String serverId, T msg);
}

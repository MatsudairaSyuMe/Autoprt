package com.systex.sysgateii.gateway.listener;
//20200901
//20201026 for cmdhis
public interface EventListener {
	public void onEvent(String id, EventType evt, String sno);
}

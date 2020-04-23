package com.systex.sysgateii.gateway.autoPrtSvr.Server;

/**
 * 
 * Created by MatsudairaSyume 2019/11/5
 */

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*
 * PrnSvr
 * Print Server controller
 *    
 * MatsudairaSyuMe
 * Ver 1.0
 *  20191126 
 */

import java.util.List;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.systex.sysgateii.gateway.autoPrtSvr.Client.PrtCli;
import com.systex.sysgateii.gateway.conf.DynamicProps;
import com.systex.sysgateii.gateway.listener.MessageListener;

public class PrnSvr implements MessageListener<byte[]>, Runnable  {
	private static Logger log = LoggerFactory.getLogger(PrnSvr.class);
//	private static Logger atlog = LoggerFactory.getLogger("atlog");
	static PrnSvr server;
	public static String logPath = "";
	static FASSvr fasDespacther;
	static ConcurrentHashMap<String, Object> cfgMap = null;
	static List<ConcurrentHashMap<String, Object>> list = null;

	public PrnSvr() {
		log.info("[0000]:=============[Start]=============");
		MDC.put("WSNO", "0000");
//		atlog.info("=============[Start]=============");
	}

	@Override
	public void messageReceived(String serverId, byte[] msg) {
		// TODO Auto-generated method stub
		log.debug("mrg received");
	}

	public void run() {
		RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
		String jvmName = bean.getName();
		String pid = jvmName.split("@")[0];
		MDC.put("WSNO", "0000");
		log.info("[0000]:------MainThreadId={}------", pid);
//		atlog.info("------MainThreadId={}------", pid);
		try {
			Thread thread;
			PrtCli conn;
			log.info("[0000]:------Call MaintainLog OK------");
//			atlog.info("------Call MaintainLog OK------");

			// Load Uniconv.dll
//			log.info("[0000]:AutoPrnCls : rateprtservice.xml is not well formed! PrnSrv");
//			atlog.info(":AutoPrnCls : rateprtservice.xml is not well formed! PrnSrv");

			if (list != null && list.size() > 0)
			{
				for (int i = 0; i < list.size(); i++) {
					cfgMap = list.get(i);
					conn = new PrtCli(cfgMap, fasDespacther, new Timer());
					thread = new Thread(conn);
					thread.start();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
	}

	public void stop()
	{
		log.debug("Enter stop");
	}
	
	public static void createServer(DynamicProps cfg) {
		log.debug("Enter createServer");
		cfgMap = null;
		list = cfg.getCfgPrtMapList();
		logPath = cfg.getConHashMap().get("system.logpath");
		log.debug("Enter createServer size={}", list.size());
		server = new PrnSvr();
	}

	public static void startServer(FASSvr setfassvr) {
		log.debug("Enter startServer");
		if (server != null) {
			fasDespacther = setfassvr;
			server.run();
		}
	}

	public static void stopServer() {
		log.debug("Enter stopServer");
		if (server != null) {
			server.stop();
		}
	}

	public static void sleep(int t) {
		try {
			TimeUnit.SECONDS.sleep(t);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

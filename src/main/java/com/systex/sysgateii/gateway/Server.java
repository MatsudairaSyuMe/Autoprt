package com.systex.sysgateii.gateway;

/**
 * 
 * Created by MatsudairaSyume 2019/11/5
 */


import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.systex.sysgateii.gateway.autoPrtSvr.Server.FASSvr;
import com.systex.sysgateii.gateway.autoPrtSvr.Server.PrnSvr;
import com.systex.sysgateii.gateway.conf.DynamicProps;

public class Server {
	private static Logger log = LoggerFactory.getLogger(Server.class);
	private static AtomicBoolean isShouldShutDown = new AtomicBoolean(false);
	private static final long TEST_TIME_SECONDS = 3;

	public static void main(String[] args) {
		try {
			log.info("sysgateii server start...");
			DynamicProps dcf = new DynamicProps("rateprtservice.xml");

			FASSvr.createServer(dcf.getConHashMap().get("svrsubport.svrip"));
			FASSvr.startServer();

			PrnSvr.createServer(dcf);
			PrnSvr.startServer(FASSvr.getFASSvr());
			log.info("sysgateii server start complete!");

			while (!isShouldShutDown.get()) {
				TimeUnit.SECONDS.sleep(TEST_TIME_SECONDS);
			}
			log.info("RateServer server stop!");
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}

	}

	public static void addDaemonShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
//				NodeController.stopController();
//				ClientController.stopController();
				PrnSvr.stopServer();
//            	WebServer.stopServer();
				setIsShouldShutDown(true);
			}
		});
	}

	public static void sleep(int t) {
		try {
			Thread.sleep(t * 3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static AtomicBoolean getIsShouldShutDown() {
		return isShouldShutDown;
	}

	public static void setIsShouldShutDown(boolean shutStatus) {
		Server.isShouldShutDown.set(shutStatus);
	}

}

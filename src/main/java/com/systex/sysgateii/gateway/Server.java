package com.systex.sysgateii.gateway;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;

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
import com.systex.sysgateii.gateway.comm.Constants;
import com.systex.sysgateii.gateway.conf.DynamicProps;
import com.systex.sysgateii.gateway.dao.GwDao;

import ch.qos.logback.classic.util.ContextInitializer;

public class Server {
	private static Logger log = null;
	private static AtomicBoolean isShouldShutDown = new AtomicBoolean(false);
	private static final long TEST_TIME_SECONDS = 3;
	//20200926
	private static GwDao jsel2ins = null;
	private static String svrstatustbname = "";
	private static String svrsstatustbmkey = "";
	private static String svrstatustbfields = "";
	private static String updValueptrn = "'%s','%s','%s','%s','%s','SYSTEM','','%s'";
	private static String auid = "";
	private static String svrip = "";
	//----

	public static void main(String[] args) {
		System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "." + File.separator + "logback.xml");
		log = LoggerFactory.getLogger(Server.class);
		try {
			log.info("sysgateii server start...");
			DynamicProps dcf = new DynamicProps("rateprtservice.xml");

			FASSvr.createServer(dcf.getConHashMap());
			FASSvr.startServer();

			//20200901
//			PrnSvr.createServer(dcf);
//			PrnSvr.startServer(FASSvr.getFASSvr());
			PrnSvr.createServer(dcf, FASSvr.getFASSvr());
			PrnSvr.startServer();
			//----
			//20200926
			if (dcf.isReadCfgFromCenter()) {
				svrstatustbname = dcf.getConHashMap().get("system.svrstatustb[@name]");
				svrsstatustbmkey = dcf.getConHashMap().get("system.svrstatustb[@mkey]");
				svrstatustbfields = dcf.getConHashMap().get("system.svrstatustb[@fields]");
				log.info("sysgateii server start complete! auid=[{}]",dcf.getAuid());
				if (jsel2ins == null)
					jsel2ins = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
				//AUID,BRNO,IP,CURSTUS,PID,CREATOR,MODIFIER,LASTUPDATE
				auid = dcf.getAuid();
				svrip = dcf.getSvrip();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
				String t = sdf.format(new java.util.Date());
				String updValue = String.format(updValueptrn,auid,PrnSvr.verbrno, svrip,
						Constants.STSUSEDACT, ManagementFactory.getRuntimeMXBean().getName().split("@")[0],t);
				int row = jsel2ins.UPSERT(svrstatustbname, svrstatustbfields, updValue, svrsstatustbmkey, PrnSvr.svrid);
				log.debug("total {} records update", row);
				jsel2ins.CloseConnect();
				jsel2ins = null;
				//----
			} else
				log.info("sysgateii server start complete!");
			//----
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
				//20200926
				try {
					if (jsel2ins == null)
						jsel2ins = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
					// AUID,BRNO,IP,CURSTUS,PID,CREATOR,MODIFIER,LASTUPDATE
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
					String t = sdf.format(new java.util.Date());
					String updValue = String.format(updValueptrn, auid, PrnSvr.verbrno, svrip, Constants.STSUSEDINACT,
							ManagementFactory.getRuntimeMXBean().getName().split("@")[0],t);
					int row = jsel2ins.UPSERT(svrstatustbname, svrstatustbfields, updValue, svrsstatustbmkey,
							PrnSvr.svrid);
					log.debug("total {} records update  status [{}]", row, Constants.STSUSEDINACT);
					jsel2ins.CloseConnect();
					jsel2ins = null;
				} catch (Exception e) {
					e.printStackTrace();
					log.error(e.getMessage());
				}
				//----
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

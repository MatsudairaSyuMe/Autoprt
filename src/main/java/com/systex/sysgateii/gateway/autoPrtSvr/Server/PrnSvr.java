package com.systex.sysgateii.gateway.autoPrtSvr.Server;

/**
 * 
 * Created by MatsudairaSyume 2019/11/5
 */

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
//20201006
import java.util.Collections;
//----
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;

/*
 * PrnSvr
 * Print Server controller
 *    
 * MatsudairaSyuMe
 * Ver 1.0
 *  20191126 
 */

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.systex.sysgateii.gateway.Server;
import com.systex.sysgateii.gateway.autoPrtSvr.Client.PrtCli;
import com.systex.sysgateii.gateway.comm.Constants;
import com.systex.sysgateii.gateway.conf.DynamicProps;
import com.systex.sysgateii.gateway.dao.GwDao;
import com.systex.sysgateii.gateway.listener.EventType;
import com.systex.sysgateii.gateway.listener.MessageListener;
import com.systex.sysgateii.gateway.util.Big5FontImg;
import com.systex.sysgateii.gateway.util.DateTimeUtil;
import com.systex.sysgateii.gateway.util.LogUtil;
import com.systex.sysgateii.gateway.util.ipAddrPars;

public class PrnSvr implements MessageListener<byte[]> {
	private static Logger log = LoggerFactory.getLogger(PrnSvr.class);
	//20201115
//	public static Logger amlog = null;
//	public static Logger atlog = null;
	//----
	public static Big5FontImg big5funt = null;
	public static AtomicBoolean p_fun_flag = new AtomicBoolean(false);
	public static String dburl = "";
	public static String dbuser = "";
	public static String dbpass = "";
	public static String statustbname = "";
	public static String statustbmkey = "";
	public static String statustbfields = "";
	//20200815
	public static String svrid = "";
	//20201115
	public static String bkno = "";
	//----
	public static String svrtbsdytbname = "";
	public static String svrtbsdytbmkey = "";
	public static String svrtbsdytbfields = "";
	//----
	//20201026 cmdhis and am error log
	public static String devcmdhistbname = "";
	public static String devcmdhistbsearkey = "";
	public static String devcmdhistbfields = "";
	//20201119 add EMPNO
	private String hisfldvalssptrn = "%s,%s,'%s','%s','%s','%s','%s'";
	private String hisfldvalssptrn2 = "'%s','%s','%s','%s','%s'";
	private String hisfldvalssptrn3 = "'%s','%s','%s','%s','%s','%s','%s','%s','%s'";
	//----
	//20201028
	//20201119 add EMPNO
	private String hisfldvalssptrn4 = "%s,%s,'%s','%s','%s','%s','%s','%s'";
	//----
	public static String devamtbname = "";
	public static String devamtbsearkey = "";
	public static String devamtbfields = "";

	//----
	static PrnSvr server;
	public static String logPath = "";
	static FASSvr fasDespacther;
	static ConcurrentHashMap<String, Object> cfgMap = null;
	static List<ConcurrentHashMap<String, Object>> list = null;
	//20200901
	private static PrnSvr me;
	//20201006
	Map<String, Thread> threadMap = Collections.synchronizedMap(new LinkedHashMap<String, Thread>());
	List<ConcurrentHashMap<String, Object>> lastcfglist = null;
	//----
	static List<Thread> threadList = Collections.synchronizedList(new ArrayList<Thread>());
	Map<String, PrtCli> nodeList = Collections.synchronizedMap(new LinkedHashMap<String, PrtCli>());
	Thread monitorThread;
	GwDao jdawcon = null;
	//20201226
	GwDao cmdhiscon = null;
	//----
	public static String cmdtbname = "";
	public static String cmdtbsearkey = "";
	public static String cmdtbfields = "";
	//20201106
	public static String dmtbname = "";
	public static String dmtbsearkey = "";
	public static String dmtbfields = "";
	//----
	//20201116 cancel verbno
	//public static String verbrno = "";
	//----
	public static int setResponseTimeout = 60 * 1000;// 毫秒
	//20201006
	static DynamicProps dcf = null;
	//----

	public PrnSvr() {
		log.info("[0000]:=============[Start]=============");
//		MDC.put("WSNO", "0000");
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
//		String byDate = sdf.format(new Date());

		//20201115 mark amlog,atlog
//		amlog = LogUtil.getDailyLogger(PrnSvr.logPath, verbrno + "_AM" + byDate, "info", "[%d{yyyy/MM/dd HH:mm:ss:SSS}]%msg%n");
//		atlog = LogUtil.getDailyLogger(PrnSvr.logPath, verbrno + "_AT" + byDate, "info", "[TID:%X{PID} %d{yyyy/MM/dd HH:mm:ss:SSS}]:[%X{WSNO}]:[%thread]:[%class{30} %M|%L]:%msg%n");
//		atlog.info("=============[Start]=============");
	}

	@Override
	public void messageReceived(String serverId, byte[] msg) {
		// TODO Auto-generated method stub
		log.debug("msg received");
	}
//20201006 mark
/*	public void run() {

		RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
		String jvmName = bean.getName();
		String pid = jvmName.split("@")[0];
		MDC.put("WSNO", "0000");
		log.info("[0000]:------MainThreadId={}------", pid);
		atlog.info("------MainThreadId={}------", pid);
		try {
			Thread thread;
			PrtCli conn;
			log.info("[0000]:------Call MaintainLog OK------");
			atlog.info("------Call MaintainLog OK------");

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
	}*/
	//----

	public void stop()
	{
		log.debug("Enter stop");
	}

	//20200901
	public static void createServer(DynamicProps cfg, FASSvr setfassvr) {
		createServer(cfg);
		fasDespacther = setfassvr;
		log.info("[0000]:------Call MaintainLog OK------");
		//20201115 mark atlog
//		atlog.info("------Call MaintainLog OK------");
		RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
		String jvmName = bean.getName();
		String pid = jvmName.split("@")[0];
		MDC.put("WSNO", "0000");
		log.info("[0000]:------MainThreadId={}------", pid);
		//20201115mark atlog
//		atlog.info("------MainThreadId={}------", pid);
		try {
			Thread thread;
			PrtCli conn;
			threadList.clear();
			getMe().nodeList.clear();
			if (list != null && list.size() > 0) {
				for (int i = 0; i < list.size(); i++) {
					//20201006
					/*
					cfgMap = list.get(i);
					conn = new PrtCli(cfgMap, fasDespacther, new Timer());
					thread = new Thread(conn);
					threadList.add(thread);
					*/
					synchronized (getMe()) {
						//20201006
						cfgMap = list.get(i);
						conn = new PrtCli(cfgMap, fasDespacther, new Timer());
						thread = new Thread(conn);
						getMe().threadMap.put(conn.getId(), thread);
						//----
						getMe().nodeList.put(conn.getId(), conn);
					}
				}
				//20201006
				//for Java8 approach
				threadList = getMe().threadMap.values().stream().collect(Collectors.toList());
				//----
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
	}

	public void start() {
		try {
			if (!threadList.isEmpty()) {
				for (Thread t : threadList) {
					t.start();
					log.info("thread [{}] start", t.getName());
				}
				monitorThread = new Thread(new Runnable() {
					@Override
					public void run() {
						while (true) {
							log.info("monitorThread");
							if (PrnSvr.dburl != null && PrnSvr.dburl.trim().length() > 0) {
								String selfld = "";
								String selkey = "";
								String[] sno = null;
								if (PrnSvr.cmdtbfields.indexOf(',') > -1) {
									selfld = PrnSvr.cmdtbfields.substring(PrnSvr.cmdtbfields.indexOf(',') + 1);
									selkey = PrnSvr.cmdtbfields.substring(0, PrnSvr.cmdtbfields.indexOf(','));
								} else {
									selfld = PrnSvr.cmdtbfields;
									selkey = PrnSvr.cmdtbsearkey;
								}
								try {
									jdawcon = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
									log.debug("current selfld=[{}] selkey=[{}] cmdtbsearkey=[{}]", selfld, selkey, PrnSvr.cmdtbsearkey);
									String[] cmd = jdawcon.SELMFLD(PrnSvr.cmdtbname, selfld, selkey, PrnSvr.svrid, false);
									if(cmd != null && cmd.length > 0)
										for (String s: cmd) {
											s = s.trim();
											log.debug("current row cmd [{}]", s);
											if (s.length() > 0 && s.indexOf(',') > -1) {
												String[] cmdary = s.split(",");
												//20201006
//												log.debug("cmd object node=[{}] curState=[{}] cmd getCurMode=[{}]", getMe().nodeList.get(cmdary[0]).getId(), getMe().nodeList.get(cmdary[0]).getCurState(), getMe().nodeList.get(cmdary[0]).getCurMode());
												//----
												if (cmdary.length > 1) {
//													if (!getMe().nodeList.containsKey(cmdary[0])) {
//														log.debug("!!! cmd object node=[{}] not found in nodeList !!!", cmdary[0]);
//														continue;
//													}
													//20201006
//													log.debug("cmd object node=[{}] curState=[{}] cmd getCurMode=[{}]",
//															getMe().nodeList.get(cmdary[0]).getId(), getMe().nodeList.get(cmdary[0]).getCurState(), getMe().nodeList.get(cmdary[0]).getCurMode());
													//----
													//20201026
													int idx = 0;
													String sts = "0";
													//20201028
													sno = null;
													boolean createNode = false;
													boolean restartAlreadyStop = false;
													if (DateTimeUtil.MinDurationToCurrentTime(3,cmdary[3])) {
														log.debug("brws=[{}] keep in cmd table longer then 3 minutes will be cleared",cmdary[0]);
														if (cmdary[1].trim().length() > 0) {
															log.debug("brws=[{}] cmd[{}] not execute will be marked fail in cmdhis",cmdary[0], cmdary[1]);
															if (cmdhiscon == null)
																cmdhiscon = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
															//20201119
															String[] chksno = cmdhiscon.SELMFLD(PrnSvr.devcmdhistbname, "SNO", "BRWS,CMD,CMDCREATETIME", "'" + cmdary[0] + "','"+ cmdary[1] + "','"+ cmdary[3]+ "'", false);
															SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
															String t = sdf.format(new java.util.Date());
															//20201119
															String failfldvals = String.format(hisfldvalssptrn4, PrnSvr.svrid, cmdary[2], cmdary[0],cmdary[1],cmdary[3],"FAIL",t,cmdary[4]);
															if (chksno == null || chksno.length == 0) {
																chksno = new String[1];
																chksno[0] = "-1";
															}
															sno = cmdhiscon.INSSELChoiceKey(PrnSvr.devcmdhistbname, "SVRID,AUID,BRWS,CMD,CMDCREATETIME,CMDRESULT,CMDRESULTTIME,EMPNO", failfldvals, PrnSvr.devcmdhistbsearkey, chksno[0], false, false);
															cmdhiscon.CloseConnect();
															cmdhiscon = null;
															sno = null;
														}
														jdawcon.DELETETB(PrnSvr.cmdtbname, "SVRID,BRWS",PrnSvr.svrid+",'" + cmdary[0] + "'");
														continue;
													}
													//----
													for (String ss: cmdary)
														log.debug("cmd[{}]=[{}]",idx++, ss);
													String curcmd = cmdary[1].trim().toUpperCase();
													//20201026 for cmdhis
													if (curcmd != null && curcmd.length() > 0) {
														cmdhiscon = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
														if (getMe().nodeList != null && getMe().nodeList.size() > 0) {
															if (getMe().nodeList.containsKey(cmdary[0])) {
																//20210204, 20210428 MatsudairaSyuMe Log Forging
																final String logStr = String.format("!!! cmd object node=[%s] already in nodeList please STOP this node before START !!!", cmdary[0]);
																if (Constants.FilterNewlinePattern.matcher(logStr).find())
																	log.error("!!! cmd object node already in nodeList please STOP this node before START !!! check dashboard");
																else
																	log.error(logStr);
																if (getMe().nodeList.get(cmdary[0]).getCurState() >= 0)
																	sts = "2";
																createNode = false;
															} else {
																log.debug("!!! cmd object node=[{}] not in nodeList will be created", cmdary[0]);
																createNode = true;
															}
														}
														String fldvals = String.format(hisfldvalssptrn, PrnSvr.svrid, cmdary[2], cmdary[0],cmdary[1],cmdary[3],sts,cmdary[4]);
														//20201028 check sno if command already insert to cmdhis
//														sno = cmdhiscon.INSSELChoiceKey(PrnSvr.devcmdhistb, "SVRID,AUID,BRWS,CMD,CMDCREATETIME,CURSTUS", "1,1,'9838901','START','2020-10-21 09:46:38.368000','0'", PrnSvr.evcmdhistbsearkey, "-1", false, true);
														String[] chksno = cmdhiscon.SELMFLD(PrnSvr.devcmdhistbname, "SNO", "BRWS,CMD,CMDCREATETIME", "'" + cmdary[0] + "','"+ cmdary[1] + "','"+ cmdary[3]+ "'", false);
//														log.debug("chksno=[{}]",chksno);
														if (chksno != null && chksno.length > 0 && Integer.parseInt(chksno[0].trim()) > -1) {
															for (String sss: chksno)
																log.debug("sno[{}] already exist",sss);
															//20210413 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent Comparison change equals to 
															if (curcmd.equalsIgnoreCase("RESTART")) { // current command is RESTART check cmdhis if already done STOP
																for (int i = 0; i < chksno.length; i++) {
																	String chkcmdresult = cmdhiscon.SELONEFLD(PrnSvr.devcmdhistbname, "CMDRESULT", "SNO", chksno[0], false);
																	log.debug("table sno=[{}] cmdhis cmd is RESTART and cmdresult=[{}]", chksno[i], chkcmdresult);
																	if (chkcmdresult != null && chkcmdresult.trim().length() > 0 && chkcmdresult.equals("STOP")) {
																		if (!restartAlreadyStop) {
																			sno = null; // prepared to start new node
																			restartAlreadyStop = true;
																		} else {
																			sno = new String[1];
																			sno[0] = chksno[i];																																				
																		}
																		log.debug("table son=[{}] chksno=[{}] cmdhis cmd is RESTART and cmdresult=[{}] restartAlreadyStop=[{}]", sno, chksno[i], chkcmdresult, restartAlreadyStop);
																	} else {
																	// current command is RESTART and waiting to STOP or already set ACTIVE waiting to finish
																		sno = new String[1];
																		sno[0] = chksno[i];																	
																	}
																}
															} else {
																// current command is not RESTART and waiting to finish
																sno = new String[1];
																sno[0] = chksno[0];
															}
														}
														if (sno == null) {// first time receive command insert new record to cmdhis
															//20201119 add EMPNO
															sno = cmdhiscon.INSSELChoiceKey(PrnSvr.devcmdhistbname, "SVRID,AUID,BRWS,CMD,CMDCREATETIME,CURSTUS,EMPNO", fldvals, PrnSvr.devcmdhistbsearkey, "-1", false, false);
															if (sno != null) {
																for (int i = 0; i < sno.length; i++)
																	log.debug("sno[{}]=[{}]",i,sno[i]);
																} else
																	log.error("sno null");
														}
														//----
													}
													//----
													log.debug("table sno=[{}] createNode=[{}] restartAlreadyStop=[{}]", (sno == null ? 0: sno[0]), createNode, restartAlreadyStop);
													//20210413 MatsudairaSyuMe prevent Null Dereference
													if (sno == null) {
														sno = new String[1];
														sno[0] = "";
													}
													//----
													//20210426 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent Comparison
													int selCmd = Constants.UNKNOWN;
													if (curcmd.toUpperCase(Locale.ENGLISH).equals("START"))
														selCmd = Constants.START;
													else if(curcmd.toUpperCase(Locale.ENGLISH).equals("STOP"))
														selCmd = Constants.STOP;
													else if(curcmd.toUpperCase(Locale.ENGLISH).equals("RESTART"))
														selCmd = Constants.RESTART;
													switch (selCmd) {//20210426 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent Comparison
													case Constants.START://20210426 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent Comparison
														//20201006, 20201026 cmdhis
														createNode(cmdary[0]);
														//----
														if (getMe().nodeList.get(cmdary[0]).getCurState() == -1) {
															//20201026 for cmdhis
															getMe().nodeList.get(cmdary[0]).onEvent(getMe().nodeList.get(cmdary[0]).getId(), EventType.ACTIVE, sno[0]);
															log.debug("cmd object node=[{}] enable session getCurMode=[{}]", getMe().nodeList.get(cmdary[0]).getId(), getMe().nodeList.get(cmdary[0]).getCurMode());
														} else {
															SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
															String t = sdf.format(new java.util.Date());
															int row = jdawcon.UPDT(PrnSvr.cmdtbname, "CMD, CMDRESULT,CMDRESULTTIME", "'','START','" + t + "'",
																	"SVRID,BRWS", PrnSvr.svrid + "," + cmdary[0]);
															//----
															log.debug("total {} records update", row);
															log.debug("cmd object node=[{}] already active!!!! getCurMode=[{}]", getMe().nodeList.get(cmdary[0]).getId(), getMe().nodeList.get(cmdary[0]).getCurMode());
															//20201026
//															String fldvals2 = String.format(hisfldvalssptrn2, "", cmdary[1], t, sts);
															PrtCli conn = getMe().nodeList.get(cmdary[0]);
															//20201119 add EMPNO
															//20201218 add original cmd to devcmdhis
															String fldvals3 = String.format(hisfldvalssptrn3, cmdary[1], cmdary[1], t, conn.getRemoteHostAddr(),//20210427 MatsudairaSyuMe Often Misused: Authentication
																	conn.getRmtaddr().getPort(),conn.getLocalHostAddr(), conn.getLocaladdr().getPort(),sts,cmdary[4]);
															//----
															//---
//															sno = cmdhiscon.INSSELChoiceKey(PrnSvr.devcmdhistbname, "SVRID,AUID,BRWS,CMD,CMDCREATETIME,CMDRESULT,CMDRESULTTIME,CURSTUS", "1,1,'9838901','','2020-10-21 09:46:38.368000','START','2020-10-21 09:46:38.368000','0','2'", "SNO", "31", false, true);
															sno = cmdhiscon.INSSELChoiceKey(PrnSvr.devcmdhistbname, "CMD,CMDRESULT,CMDRESULTTIME,DEVIP,DEVPORT,SVRIP,SVRPORT,RESULTSTUS,EMPNO", fldvals3, PrnSvr.devcmdhistbsearkey, sno[0], false, true);
															//----
															if (sno != null) {
																for (int i = 0; i < sno.length; i++)
																	log.debug("sno[{}]=[{}]",i,sno[i]);
															} else
																log.error("sno null");
															//----
														}
														break;
													case Constants.STOP://20210426 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent Comparison
														if (getMe().nodeList.get(cmdary[0]).getCurState() != -1) {
															//20201026 for cmdhis
															getMe().nodeList.get(cmdary[0]).onEvent(getMe().nodeList.get(cmdary[0]).getId(), EventType.SHUTDOWN, sno[0]);
															log.debug("cmd object node=[{}] stop session getCurMode=[{}]", getMe().nodeList.get(cmdary[0]).getId(), getMe().nodeList.get(cmdary[0]).getCurMode());
														} else {
															SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
															String t = sdf.format(new java.util.Date());
															int row = jdawcon.UPDT(PrnSvr.cmdtbname, "CMD, CMDRESULT,CMDRESULTTIME", "'','STOP','" + t + "'",
																	"SVRID,BRWS", PrnSvr.svrid + "," + cmdary[0]);
															log.debug("total {} records update", row);
															log.debug("cmd object node=[{}] already shutdown!!!! getCurMode=[{}]", getMe().nodeList.get(cmdary[0]).getId(), getMe().nodeList.get(cmdary[0]).getCurMode());
															//20201026
															//20201119 add EMPNO
															//20201218 add original cmd to devcmdhis
															String fldvals2 = String.format(hisfldvalssptrn2, cmdary[1], cmdary[1], t, sts,cmdary[4]);
															//----
															sno = cmdhiscon.INSSELChoiceKey(PrnSvr.devcmdhistbname, "CMD,CMDRESULT,CMDRESULTTIME,RESULTSTUS,EMPNO", fldvals2, PrnSvr.devcmdhistbsearkey, sno[0], false, true);
															//----
															if (sno != null) {
																for (int i = 0; i < sno.length; i++)
																	log.debug("sno[{}]=[{}]",i,sno[i]);
															} else
																log.error("sno null");
															//----

														}
														break;
													case Constants.RESTART://20210426 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent Comparison
														//20201221 1st time RESTART mode node already STOP
														if (!restartAlreadyStop && createNode)
															restartAlreadyStop = true;
														//----
														//20201028 add cmdhis
														if (!restartAlreadyStop && !createNode && getMe().nodeList.get(cmdary[0]).getCurState() != -1) {
															getMe().nodeList.get(cmdary[0]).onEvent(getMe().nodeList.get(cmdary[0]).getId(), EventType.RESTART, sno[0]);
															log.debug("cmd object node=[{}] stop session getCurMode=[{}]", getMe().nodeList.get(cmdary[0]).getId(), getMe().nodeList.get(cmdary[0]).getCurMode());
														} else {
															//start to create new node and start
															log.debug("start to create new node and start sno=[{}]", sno[0]);
															createNode(cmdary[0]);
															//----
															if (getMe().nodeList.get(cmdary[0]).getCurState() == -1) {
																getMe().nodeList.get(cmdary[0]).onEvent(getMe().nodeList.get(cmdary[0]).getId(), EventType.ACTIVE, sno[0]);
																log.debug("cmd object node=[{}] enable session getCurMode=[{}]", getMe().nodeList.get(cmdary[0]).getId(), getMe().nodeList.get(cmdary[0]).getCurMode());
															} else {
																SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
																String t = sdf.format(new java.util.Date());
																int row = jdawcon.UPDT(PrnSvr.cmdtbname, "CMD, CMDRESULT,CMDRESULTTIME", "'','RESTART','" + t + "'",
																		"SVRID,BRWS", PrnSvr.svrid + "," + cmdary[0]);
																log.debug("total {} records update", row);
																log.debug("cmd object node=[{}] already active!!!! getCurMode=[{}]", getMe().nodeList.get(cmdary[0]).getId(), getMe().nodeList.get(cmdary[0]).getCurMode());
																PrtCli conn = getMe().nodeList.get(cmdary[0]);
																//20201119 add EMPNO
																//20201218 add original cmd to devcmdhis
																String fldvals3 = String.format(hisfldvalssptrn3, cmdary[1], cmdary[1], t, conn.getRemoteHostAddr(),//20210427 MatsudairaSyuMe Often Misused: Authentication
																		conn.getRmtaddr().getPort(),conn.getLocalHostAddr(), conn.getLocaladdr().getPort(),sts,cmdary[4]);
																//----
																sno = cmdhiscon.INSSELChoiceKey(PrnSvr.devcmdhistbname, "CMD,CMDRESULT,CMDRESULTTIME,DEVIP,DEVPORT,SVRIP,SVRPORT,RESULTSTUS,EMPNO", fldvals3, PrnSvr.devcmdhistbsearkey, sno[0], false, true);
																//----
																if (sno != null) {
																	for (int i = 0; i < sno.length; i++)
																		log.debug("sno[{}]=[{}]",i,sno[i]);
																} else
																	log.error("sno null");
															}
														}
														break;
													default:
														log.debug("!!! cmd object node=[{}] cmd [{}] ignore", cmdary[0], cmdary[1]);
														break;
													}
												} else
													log.debug("!!! cmd object node=[{}] format error !!!", cmdary[0]);													
											} else {
												//20210204,20210428 MatsudairaSyuMe Log Forging
												final String logStr = String.format("!!!current row cmd error [%s]", s);
												if (Constants.FilterNewlinePattern.matcher(logStr).find())
													log.error("!!!current row cmd error [%s...] check dashboard");
												else
													log.error(logStr);
											}
										}
									jdawcon.CloseConnect();
									jdawcon = null;
									//20201026
									if (cmdhiscon != null)
										cmdhiscon.CloseConnect();
									cmdhiscon = null;
									//----
								} catch (Exception e) {
									e.printStackTrace();
									log.info("monitorThread read database error [{}]", e.toString());
								}
							}
							sleep(3);
						} // while
					}
				});// monitorThread
				monitorThread.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
	}
	//20201006
	public static int closeNode(String nid, boolean isRemove) {
		int rtn = 0;
		log.debug("start current threadMap size=[{}]", getMe().threadMap.size());
		log.debug("start current nodeList size=[{}]", getMe().nodeList.size());
		if (!getMe().nodeList.containsKey(nid))
			log.debug("!!! cmd object node=[{}] not found in nodeList !!!", nid);
		else {
			synchronized(getMe())
			{
				log.debug("!!! start to remove node=[{}] !!!", nid);
				Thread t = getMe().threadMap.get(nid);
				try {
					t.interrupt();
					t.join(1 * 1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log.error("!!! error for stop thread for [{}] !!!: [{}]", nid, e.toString());
				}
				getMe().threadMap.remove(nid);
				getMe().nodeList.remove(nid);
				rtn += 1;
			}
		}
		log.debug("stop current threadMap size=[{}]", getMe().threadMap.size());
		log.debug("stop current nodeList size=[{}]", getMe().nodeList.size());
		return rtn;
	};
	public int createNode(String nid) {
		int ret = 0;
		if (getMe().nodeList != null && getMe().nodeList.size() > 0) {
			if (getMe().nodeList.containsKey(nid)) {
				//20210204,20210428 MatsudairaSyuMe Log Forging
				final String logStr = String.format("!!! cmd object node=[%s] already in nodeList please STOP this node before START !!!", nid);
				if (Constants.FilterNewlinePattern.matcher(logStr).find())
					log.error("!!! cmd object node=[%s...] already in nodeList please STOP this node before START !!! check dashboard");
				else
					log.error(logStr);
				return ret;
			} else
				log.debug("!!! cmd object node=[{}] not in nodeList will be created", nid);
		}
		//20201021 mark
		log.debug("start current threadMap size=[{}] nodeList size=[{}]", getMe().threadMap.size(), getMe().nodeList.size());
//		log.debug("start current nodeList size=[{}]", getMe().nodeList.size());
		//----

		if (dcf != null)
			lastcfglist = dcf.getLastcfgPrtMapList();
		else
			log.info("dcf == null !!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		if (lastcfglist != null && lastcfglist.size() > 0) {
			log.info("lastcfglist.size() == [{}]", lastcfglist.size());
			for(int i=0; i < lastcfglist.size(); i++) {
				ConcurrentHashMap<String, Object> newcfgMap = lastcfglist.get(i);
				log.debug("check brws cmd [{}] lastcfglist [{}]", nid, newcfgMap.get("brws"));
				if (nid.trim().equals(newcfgMap.get("brws"))) {
					log.debug("prepare to create node brws [{}]", newcfgMap.get("brws"));
					PrtCli conn = new PrtCli(newcfgMap, fasDespacther, new Timer());
					Thread thread = new Thread(conn);
					getMe().threadMap.put(conn.getId(), thread);
					getMe().nodeList.put(conn.getId(), conn);
					ret += 1;
					thread.start();
					i = lastcfglist.size();
					break;
				}
			}
		} else
			log.debug("create node return 0");
		//20201021 mark
		log.debug("stop current threadMap size=[{}] size=[{}]", getMe().threadMap.size(), getMe().nodeList.size());
//		log.debug("stop current nodeList size=[{}]", getMe().nodeList.size());
		//----

		return ret;
	}
	//----
	public static void startServer() {
		log.debug("Enter startServer");
		/*20200901
		if (server != null) {
			server.start();
		}
		*/
		getMe().start();
		//----
	}

	public static PrnSvr getMe() {
		if (me == null) {
			me = new PrnSvr();
		}
		return me;
	}
	//----
	
	public static void createServer(DynamicProps cfg) {
		log.debug("Enter createServer");
		//20201006
		dcf = cfg;
		//----
		cfgMap = null;
		//20201116 cancel verbrno
		//verbrno = cfg.getConHashMap().get("svrsubport.verhbrno");
		//----
		list = cfg.getCfgPrtMapList();
		logPath = cfg.getConHashMap().get("system.logpath");
		String tout = cfg.getConHashMap().get("svrsubport.recvtimeout");
		if (tout != null && tout.trim().length() > 0) {
			setResponseTimeout = Integer.parseInt(tout);
		}
		log.debug("Enter createServer size={}", list.size());
		log.debug("receive timeout is ={} mili-seconds", setResponseTimeout);
		dburl = cfg.getConHashMap().get("system.db[@url]");
		dbuser = cfg.getConHashMap().get("system.db[@user]");
		dbpass = cfg.getConHashMap().get("system.db[@pass]");
		statustbname = cfg.getConHashMap().get("system.statustb[@name]");
		statustbmkey = cfg.getConHashMap().get("system.statustb[@mkey]");
		statustbfields = cfg.getConHashMap().get("system.statustb[@fields]");
		//20200815
		svrid = cfg.getConHashMap().get("system.svrid");
		bkno = cfg.getConHashMap().get("bkno").trim();
		//20201115
		//----
		svrtbsdytbname = cfg.getConHashMap().get("system.svrtbsdytb[@name]");
		svrtbsdytbmkey = cfg.getConHashMap().get("system.svrtbsdytb[@mkey]");
		svrtbsdytbfields = cfg.getConHashMap().get("system.svrtbsdytb[@fields]");
		//----
		//20200901
		cmdtbname = cfg.getConHashMap().get("system.devcmdtb[@name]");
		cmdtbsearkey = cfg.getConHashMap().get("system.devcmdtb[@mkey]");
		cmdtbfields = cfg.getConHashMap().get("system.devcmdtb[@fields]");
		//----
		//20201026 cmdhis and am error log
		devcmdhistbname = cfg.getConHashMap().get("system.devcmdhistb[@name]");
		devcmdhistbsearkey = cfg.getConHashMap().get("system.devcmdhistb[@mkey]");
		devcmdhistbfields = cfg.getConHashMap().get("system.devcmdhistb[@fields]");
		devamtbname = cfg.getConHashMap().get("system.devamtb[@name]");
		devamtbsearkey = cfg.getConHashMap().get("system.devamtb[@mkey]");
		devamtbfields = cfg.getConHashMap().get("system.devamtb[@fields]");
		//----

		//20201106
		dmtbname = cfg.getConHashMap().get("system.dmtb[@name]");
		dmtbsearkey = cfg.getConHashMap().get("system.dmtb[@mkey]");
		dmtbfields = cfg.getConHashMap().get("system.dmtb[@fields]");
		//----

		if (dburl != null && dburl.trim().length() > 0) {
			log.debug("will use db url:[{}] user name:[{}] update status table [{}] main key [{}] fields [{}]", dburl, dbuser, statustbname, statustbmkey, statustbfields);
			//20201115
//			log.debug("check tbsdy from table [{}] main key [{}]=[{}] fields [{}]", svrtbsdytbname, svrtbsdytbmkey, svrid, svrtbsdytbfields);
			log.debug("check tbsdy from table [{}] main key [{}]=[{}] fields [{}]", svrtbsdytbname, svrtbsdytbmkey, bkno, svrtbsdytbfields);
		}
		log.debug("receive timeout is ={} mili-seconds", setResponseTimeout);
		MDC.put("WSNO", "0000");
		MDC.put("PID", ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String byDate = sdf.format(new Date());
		//20201115
//		amlog = LogUtil.getDailyLogger(PrnSvr.logPath, verbrno + "AM" + byDate, "info", "[%d{yyyy/MM/dd HH:mm:ss:SSS}]%msg%n");
//		atlog = LogUtil.getDailyLogger(PrnSvr.logPath, verbrno + "AT" + byDate, "info", "[TID:%X{PID} %d{yyyy/MM/dd HH:mm:ss:SSS}]:[%X{WSNO}]:[%thread]:[%class{0} %M|%L]:%msg%n");
		//----
		try {
			p_fun_flag.set(false);
			big5funt = new Big5FontImg("FontTable_low.bin", "FontData_All.bin");
			p_fun_flag.set(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.debug("p_fun_flag={}", p_fun_flag);
//20200901
//		server = new PrnSvr();
		server = getMe();
		//----
	}

	//20201006 mark
/*	public static void startServer(FASSvr setfassvr) {
		log.debug("Enter startServer");
		if (server != null) {
			fasDespacther = setfassvr;
			server.run();
		}
	}*/
	//----

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

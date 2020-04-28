package com.systex.sysgateii.gateway.autoPrtSvr.Client;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
//import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.systex.sysgateii.gateway.autoPrtSvr.Server.FASSvr;
import com.systex.sysgateii.gateway.autoPrtSvr.Server.PrnSvr;
import com.systex.sysgateii.gateway.data.Constants;
import com.systex.sysgateii.gateway.comm.TXP;
import com.systex.sysgateii.gateway.conf.DscptMappingTable;
import com.systex.sysgateii.gateway.listener.ActorStatusListener;
import com.systex.sysgateii.gateway.prtCmd.Printer;
import com.systex.sysgateii.gateway.prtCmd.Impl.CS4625Impl;
import com.systex.sysgateii.gateway.prtCmd.Impl.CS5240Impl;
import com.systex.sysgateii.gateway.telegram.P0080TEXT;
import com.systex.sysgateii.gateway.telegram.P0880TEXT;
import com.systex.sysgateii.gateway.telegram.P1885TEXT;
import com.systex.sysgateii.gateway.telegram.P85TEXT;
import com.systex.sysgateii.gateway.telegram.Q0880TEXT;
import com.systex.sysgateii.gateway.telegram.Q98TEXT;
import com.systex.sysgateii.gateway.telegram.TITATel;
import com.systex.sysgateii.gateway.telegram.TOTATel;
import com.systex.sysgateii.gateway.util.LogUtil;
import com.systex.sysgateii.gateway.util.dataUtil;
import com.systex.sysgateii.gateway.util.ipAddrPars;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

@Sharable // 因為通道只有一組 handler instance 只有一個，所以可以 share
public class PrtCli extends ChannelDuplexHandler implements Runnable {
	private static Logger log = LoggerFactory.getLogger(PrtCli.class);

	private static Logger aslog = null;
	public Logger amlog = null;
//	private static Logger atlog = LoggerFactory.getLogger("atlog");
	public Logger atlog = null;
	public String pid = "";

	private static final ByteBuf HEARTBEAT_SEQUENCE = Unpooled
			.unreleasableBuffer(Unpooled.copiedBuffer("hb_request", CharsetUtil.UTF_8));

	private String clientId = "";       // brno from set up XML file
	private String byDate = "";
	// for ChannelDuplexHandler function
	ChannelHandlerContext currentContext;
	Channel clientChannel;
	private CountDownLatch readLatch;
	private String idleStateHandlerName = "idleStateHandler";
	public ByteBuf clientMessageBuf = Unpooled.buffer(4096);
	Object readMutex = new Object();
	// end for ChannelDuplexHandler function

	private Bootstrap bootstrap = new Bootstrap();
	private final static AtomicBoolean isConnected = new AtomicBoolean(false);

	private InetSocketAddress rmtaddr = null;
	private InetSocketAddress localaddr = null;
	private Channel channel_;
	private Timer timer_;
	private String brws = null;      // BRNO + WSNO from set up XML file
	private String type = null;      // printer type from set up XML file
	private String autoturnpage = null;
	private String getSeqStr = "";
	private int setSeqNo = 0;
	private File seqNoFile;

	private Printer prt = null;
	private int bufferSize = Integer.parseInt(System.getProperty("bufferSize", Constants.DEF_CHANNEL_BUFFER_SIZE + ""));
	private static final int MAXDELAY = 6;
	private static final int RECONNECT = 10;
	private int iRetry = 0;
	private boolean showStateMsg = false;

	// Signal Number
	private byte L1 = (byte) 0x01; // 1:0000001
	private byte L6 = (byte) 0x02; // 2:0000010
	private byte L5 = (byte) 0x04; // 4:0000100
	private byte L3 = (byte) 0x08; // 8:0001000
	private byte L0 = (byte) 0x10; // 16:0010000
	private byte L4 = (byte) 0x20; // 32:0100000
	private byte L2 = (byte) 0x40; // 64:1000000
	private byte IX = (byte) -1;
	private byte I0 = (byte) 0x00;
	private byte I1 = (byte) 0x01;
	private byte I2 = (byte) 0x02;
	private byte I3 = (byte) 0x04;
	private byte I4 = (byte) 0x08;
	private byte I5 = (byte) 0x10;
	private byte I6 = (byte) 0x20;
	private byte I7 = (byte) 0x40;
	private byte I8 = (byte) 0x80;

	// State Value
	public static final int SESSIONBREAK = -1; // 94補摺機斷線！.
	public static final int OPENPRINTER = 0;
//	public static final int CHECKPRINTER = 1;
//	public static final int CHECKPRINTERWAIT = 2;

	public static final int ENTERPASSBOOKSIG = 1; // 00請插入存摺... Capture Passbook
	public static final int CAPTUREPASSBOOK = 2;
	public static final int GETPASSBOOKSHOWSIG = 3; // Show Signal
	public static final int SETCPI = 4; // Set CPI
	public static final int SETLPI = 5; // Set LPI
	public static final int SETPRINTAREA = 6; // Set print area
	public static final int READMSR = 7; // Read MSR
	public static final int READMSRERR = 8; // 11磁條讀取失敗！Show Signal
	public static final int CHKACTNO = 9;// Check ACTNO(BOT or not) , PAGE, maybe line...
	public static final int CHKBARCODE = 10; // Get Passbook's Page Type=2
	public static final int SETSIGAFTERCHKBARCODE = 11; // Show Signal after get Passbook's Page Type=2
	public static final int EJECTAFTERPAGEERROR = 12; // Show Signal after get Passbook's Page error
	public static final int EJECTAFTERPAGEERRORWAIT = 13; // Show Signal after get Passbook's Page error
	public static final int SNDANDRCVTLM = 14; // compose TITA and send tita & Receive TOTA and check error
	public static final int SETREQSIG = 15; // Show Signal before send telegram to Host
	public static final int WAITSETREQSIG = 16; // wait Show Signal before send telegram to Host finished
	public static final int SENDTLM = 17; // start send TOTA and check error
	public static final int RECVTLM = 18; // Receive TOTA and check error
	public static final int STARTPROCTLM = 19; // start to send tita & Receive TOTA and check error
	
	public static final int PBDATAFORMAT = 20; //// Format 列印台幣存摺資料格式  print data
	public static final int FCDATAFORMAT = 21; //// Format 列印外匯存摺資料格式  print data
	public static final int GLDATAFORMAT = 22; //// Format 列印黃金存摺資料格式  print data
	public static final int FORMATPRTDATAERROR = 23; // 61存摺資料補登失敗！Show Signal
	public static final int WRITEMSR = 24; //// Write MSR
	public static final int WRITEMSRWAITCONFIRM = 25;  //// Write MSR ware confirm
	public static final int WRITEMSRERR = 26; //// Write MSR ERROR 71存摺磁條寫入有問題！
	public static final int READMSRERRAFTERWRITEMSRERR = 27; // 11磁條讀取失敗(1)！
	public static final int READMSRSUCAFTERWRITEMSRERR = 28; // 12存摺磁條讀取成功(1)！
	public static final int COMPMSRSUCAFTERWRITEMSRERR = 29; // 12存摺磁條比對正確(1)！
	public static final int COMPMSRERRAFTERWRITEMSRERR = 30; // 12存摺磁條比對失敗(1)！
	public static final int EJECTAFTERPAGEERRORWAITSTATUS = 31;
	public static final int WRITEMSRERRSHOWSIG = 32; // 71存摺磁條寫入失敗！ Show Signal
	public static final int SNDANDRCVDELTLM = 33; // 72存摺資料補登成功！
	public static final int SNDANDRCVDELTLMCHKEND = 34;        // 72存摺資料補登成功, 檢查翻頁及燈號開始！
	public static final int SNDANDRCVDELTLMCHKENDSETSIG = 35; // 72存摺資料補登成功, 檢查翻頁及燈號完成退摺開始！
	public static final int SNDANDRCVDELTLMCHKENDEJECTPRT = 36; // 72存摺資料補登成功, 檢查翻頁及燈號退摺！
	public static final int DELPASSBOOKREGCOMPERR = 37; // 73存摺資料補登刪除失敗！Show Signal
//	public static final int NOTFINISH = 38; // iEnd != 0 continue printing
//	public static final int NOTFINISHATP = 39; // iEnd != 0 continue printing, Auto turn page
//	public static final int NOTFINISHHTP = 40; // iEnd != 0 continue printing, Handy turn page, Show Reentry signal.
	public static final int FINISH = 38; // iEnd == 0 printing finished,
											// === 2 超過存摺頁次, 仍然顯示補登完成燈號
											// go to capture
	private int curState = SESSIONBREAK;
	private int iFirst = 0; // 0: start to print
							// 1: print after turn page
	private int iEnd = 0; // !< 繼續記號 0:開始 1:請翻下頁 2:頁次超過最大頁數
	private String actfiller = ""; // !< 帳號保留 MSR for PB/FC len 4
	private String msrbal = ""; // !< 磁條餘額 MSR for PB/FC len 14, GL len 12
	private String cline = ""; // !< 行次 MSR for PB/FC/GL len 2
	private String cpage = ""; // !< 頁次 MSR for PB/FC/GL len 2
	private String bkseq = ""; // !< 領用序號 MSR for PB len 1, FC len 2
	private String no = ""; // !< 存摺號碼 MSR for GL len 9
	private String pbver = ""; //!< MSR for FC 領用序號
	private int nline = 0;
	private int npage = 0;
	private int rpage = 0;
	private int iFig = 0; // type of passbook, AP type -- 1:PB / 2:FC / 3:GL
	private String total_con = ""; // total NB count
	private String org_mbal = ""; // original MSR's balance
	private int iCount = 0;
	private int iCon = 0;
	private String dCount = "";
	private int iLine = 0;
	private int pbavCnt = 999;
	byte[] fepdd = new byte[2];
	TITATel tital = null;
	TOTATel total = null;
//	private String cbkseq = "";
	private static final boolean firstOpenConn = true;
	private String account = "";
	private String catagory = "";   // working passbook workstation no
	private byte[] cusid = null;
	private FASSvr dispatcher;
	private boolean alreadySendTelegram = false;
	private byte[] resultmsg = null;
	private byte[] rtelem = null;
	private String msgid = "";
	ConcurrentHashMap<String, String> tx_area = new ConcurrentHashMap<String, String>();

	private List<byte[]> pb_arr = new ArrayList<byte[]>();
	private List<byte[]> fc_arr = new ArrayList<byte[]>();
	private List<byte[]> gl_arr = new ArrayList<byte[]>();
	private P0080TEXT p0080DataFormat = null;
	private Q0880TEXT q0880DataFormat = null;
	private P0880TEXT p0880DataFormat = null;
	private String pasname = "        ";

	private DscptMappingTable descm = null;
	private boolean Send_Recv_DATAInq = true;

	List<ActorStatusListener> actorStatusListeners = new ArrayList<ActorStatusListener>();

	public List<ActorStatusListener> getActorStatusListeners() {
		return actorStatusListeners;
	}

	public void setActorStatusListeners(List<ActorStatusListener> actorStatusListeners) {
		this.actorStatusListeners = actorStatusListeners;
	}

	public PrtCli(ConcurrentHashMap<String, Object> map, FASSvr dispatcher, Timer timer) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		this.setByDate(sdf.format(new Date()));

		this.brws = (String) map.get("brws");
		this.type = (String) map.get("type");
		this.autoturnpage = (String) map.get("autoturnpage");
		this.autoturnpage = this.autoturnpage.toLowerCase();
		this.timer_ = timer;
		this.clientId = this.brws.substring(0, 3);
		this.iRetry = 1;
		this.curState = SESSIONBREAK;
		this.iFirst = 0;
		this.dispatcher = dispatcher;
		this.descm = new DscptMappingTable();
		pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		MDC.put("WSNO", this.brws.substring(3));
		MDC.put("PID", pid);

		amlog = LogUtil.getDailyLogger(PrnSvr.logPath, this.clientId + "_AM" + byDate, "info", "[%d{yyyy/MM/dd HH:mm:ss:SSS}]%msg%n");
		aslog = LogUtil.getDailyLogger(PrnSvr.logPath, this.clientId + "_AS" + this.brws.substring(3) + byDate, "info", "TIME     [0000]:%d{yyyy.MM.dd HH:mm:ss:SSS} %msg%n");
		atlog = LogUtil.getDailyLogger(PrnSvr.logPath, this.clientId + "_AT" + byDate, "info", "[TID:%X{PID} %d{yyyy/MM/dd HH:mm:ss:SSS}]:[%X{WSNO}]:[%thread]:[%class{30} %M|%L]:%msg%n");
		atlog.info("=============[Start]=============");
		atlog.info("------MainThreadId={}------", pid);
		atlog.info("------Call MaintainLog OK------");

		if (this.type.equals("AUTO28")) {
			atlog.info("load Auto Printer type AUTO28");
		} else if (this.type.equals("AUTO20")) {
			atlog.info("load Auto Printer type AUTO20");
		} else if (this.type.equals("AUTO46")) {
			this.prt = new CS4625Impl(this, this.brws, this.type, this.autoturnpage);
			atlog.info("load Auto Printer type AUTO46");
		} else if (this.type.equals("AUTO52")) {
			this.prt = new CS5240Impl(this, this.brws, this.type, this.autoturnpage);
			atlog.info("load Auto Printer type AUTO52");
		} else {
			atlog.info("Auto Printer type define error!");
			return;
		}
		log.info("=================={} {}",this.brws.substring(0, 5), this.brws.substring(3));

		ipAddrPars nodePars = new ipAddrPars();
		nodePars.init();
		try {
			nodePars.CheckAddrT((String) map.get("ip"), "=", false);
			nodePars.list();
			if (nodePars.getCurrentParseResult()) {
				this.rmtaddr = nodePars.getCurrentRemoteNodeAddress();
				if (nodePars.getCurrentNodeType()) {
					Iterator<InetSocketAddress> iterator = new ArrayList<InetSocketAddress>(
							nodePars.getCurrentLocalNodeAddressMap().values()).iterator();
					if (iterator.hasNext()) {
						this.localaddr = iterator.next();
					}
				}
			}
		} catch (Exception e) {
			log.error("Address format error!!! {}", e.getMessage());
		}
	}
	
	public void sendBytes(byte[] msg) throws IOException {
		if (channel_ != null && channel_.isActive()) {
			aslog.info(String.format("SEND %s[%04d]:%s", this.brws.substring(3), msg.length, new String(msg)));
			ByteBuf buf = channel_.alloc().buffer().writeBytes(msg);
			channel_.writeAndFlush(buf);
		} else {
			throw new IOException("Can't send message to inactive connection");
		}
	}

	public void close() {
		try {
			channel_.close().sync();
			aslog.info(String.format("DIS  %s[%04d]:", this.brws.substring(3), 0));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public boolean connectStatus() {
		if (channel_ != null && channel_.isActive())
			return channel_.isActive();
		else
			return false;
	}

	private void doConnect(int _wait) {
		try {
			ChannelFuture f = bootstrap.connect(rmtaddr, localaddr);
			f.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (!future.isSuccess()) {// if is not successful, reconnect
						future.channel().close();
						if (iRetry > MAXDELAY)
							iRetry = MAXDELAY;
						final int _newwait = iRetry * RECONNECT * 100;
						if (curState == SESSIONBREAK && !showStateMsg) {
							amlog.info("[{}][{}][{}]:99補摺機斷線，請檢查線路！", brws, "        ", "            ");
							showStateMsg = true;
						}
						MDC.put("WSNO", brws.substring(3));
						MDC.put("PID", pid);
						atlog.info("Error , please check ... [{}:{}:{}]", rmtaddr.getAddress().toString(), rmtaddr.getPort(), localaddr.getPort());
						clientMessageBuf.clear();
						if (!future.channel().isActive()) {
							prtcliFSM(firstOpenConn);
						}
						Sleep(_newwait);
						iRetry += 1;
						bootstrap.connect(rmtaddr, localaddr).addListener(this);
					} else {// good, the connection is ok
						showStateMsg = false;
						channel_ = future.channel();
						// add a listener to detect the connection lost
						addCloseDetectListener(channel_);
						connectionEstablished();
					}
				}

				private void addCloseDetectListener(Channel channel) {
					// if the channel connection is lost, the
					// ChannelFutureListener.operationComplete() will be called
					channel.closeFuture().addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							connectionLost();
							log.debug("addCloseDetectListener {}", iRetry);
							scheduleConnect(_wait);
						}
					});

				}
			});
		} catch (Exception ex) {
			scheduleConnect(_wait / 3);

		}
	}

	private void scheduleConnect(int millis) {
		timer_.schedule(new TimerTask() {
			@Override
			public void run() {
				doConnect(millis);
			}
		}, millis);
	}

	public void handleMessage(String msg) {
		log.debug("msg={}", msg);

	}

	public void connectionLost() {
		log.debug("connectionLost()");
	}

	public void connectionEstablished() {
		log.debug("connectionEstablished()");
		this.clientMessageBuf.clear();
	}

	@Override
	public void run() {
		bootstrap.group(new NioEventLoopGroup());
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.option(ChannelOption.SO_REUSEADDR, true);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		bootstrap.option(ChannelOption.SO_LINGER, 0);
		bootstrap.option(ChannelOption.SO_REUSEADDR, true);
		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.option(ChannelOption.ALLOW_HALF_CLOSURE, false);
		bootstrap.option(ChannelOption.SO_RCVBUF, bufferSize);
		bootstrap.option(ChannelOption.SO_SNDBUF, bufferSize);
		bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(32768, 32768, 32768));
		prtcliFSM(firstOpenConn);

		bootstrap.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast("log", new LoggingHandler(PrtCli.class, LogLevel.INFO));
				ch.pipeline().addLast(new IdleStateHandler(500, 0, 0, TimeUnit.MILLISECONDS));
				ch.pipeline().addLast(getHandler("PrtCli"));
			}
		});
		scheduleConnect(3000);

	} // run

	public ChannelHandler getHandler(String _id) {
		clientId = _id;
		return this;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.debug(clientId + "channel active");
		this.iRetry = 1;
		this.currentContext = ctx;
		this.clientChannel = this.currentContext.channel();
		publishActiveEvent();
		super.channelActive(ctx);
		MDC.put("WSNO", this.brws.substring(3));
		MDC.put("PID", pid);
		showStateMsg = false;
		aslog.info(String.format("CON  %s[%04d]:", this.brws.substring(3), 0));
		prtcliFSM(!firstOpenConn);
		prt.getIsShouldShutDown().set(false);
		this.seqNoFile = new File("SEQNO", "SEQNO_" + this.brws);
		log.debug("seqNoFile local=" + this.seqNoFile.getAbsolutePath());
		if (seqNoFile.exists() == false) {
			File parent = seqNoFile.getParentFile();
			if (parent.exists() == false) {
				parent.mkdirs();
			}
			try {
				this.seqNoFile.createNewFile();
				FileUtils.writeStringToFile(this.seqNoFile, "0", Charset.defaultCharset());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.warn("WARN!!! create or open seqno file {} error {}", "SEQNO_" + this.brws, e.getMessage());
			}
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.debug(clientId + " channelInactive");
		publishInactiveEvent();
		this.clientChannel = null;
		super.channelInactive(ctx);
		prt.getIsShouldShutDown().set(true);
		prt.ClosePrinter();
		aslog.info(String.format("DIS  %s[%04d]:", this.brws.substring(3), 0));
		this.clientMessageBuf.clear();
		prtcliFSM(firstOpenConn);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		log.debug(clientId + " channelRead");
		try {
			if (msg instanceof ByteBuf) {
				ByteBuf buf = (ByteBuf) msg;
				if (buf.isReadable() && !buf.hasArray()) {
					// it is long raw telegram
					log.debug("readableBytes={} barray={}", buf.readableBytes(), buf.hasArray());
					byte[] asary = new byte[buf.readableBytes()];
					ByteBuf dup = buf.duplicate();
					dup.readBytes(asary);
					aslog.info(String.format("RECV %s[%04d]:%s", this.brws.substring(3), buf.readableBytes(), new String(asary)));
					if (clientMessageBuf.readerIndex() > (clientMessageBuf.capacity() / 2)) {
						clientMessageBuf.discardReadBytes();
						log.debug("adjustment clientMessageBuf readerindex ={}" + clientMessageBuf.readableBytes());
					}
					synchronized (this.readMutex) {
						clientMessageBuf.writeBytes(buf);
						prtcliFSM(!firstOpenConn);
					}
					log.debug("readableBytes={} barray={}", buf.readableBytes(), buf.hasArray());
				}
			} else // if
				log.warn("not ByteBuf");
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.toString());
		} finally {
			ReferenceCountUtil.release(msg);
			// 若是有配置等待鎖，則解鎖
			if (readLatch != null) {
				readLatch.countDown();
			}
		}
	}

	/**
	 * it's depends also on ChannelOption.MAX_MESSAGES_PER_READ which is 16 by
	 * default 當每一部份的訊息被讀取後會被呼叫 例如 buffer 中有 32 bytes，此功能會被呼叫 2 次
	 * .option(ChannelOption.MAX_MESSAGES_PER_READ, Integer.MAX_VALUE)
	 * 
	 * @throws Exception
	 */
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		log.debug(clientId + " channelReadComplete");
		super.channelReadComplete(ctx);
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		log.debug(clientId + " channelRegister");
//		prt.getIsShouldShutDown().set(false);
		super.channelRegistered(ctx);
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		log.debug(clientId + " channelUnregistered");
		ctx.close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		log.debug(clientId + " exceptionCaught=" + cause.getMessage());
		if (cause instanceof ConnectException) {
			publishInactiveEvent();
			ctx.close();
			prt.getIsShouldShutDown().set(true);
			prt.ClosePrinter();
			this.clientMessageBuf.clear();
			aslog.info(String.format("ERR  %s[%04d]:", this.brws.substring(3), 0));
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		log.debug(clientId + " userEventTriggered=" + evt.toString());
		if (evt instanceof IdleStateEvent) {
			if (clientChannel.pipeline().get(idleStateHandlerName) != null) {
				log.debug("unload idle state handler");
				clientChannel.pipeline().remove(idleStateHandlerName);
			}

			IdleStateEvent e = (IdleStateEvent) evt;
			if (e.state() == IdleState.READER_IDLE) {
				log.debug(clientId + " READER_IDLE");
				prtcliFSM(!firstOpenConn);

			} else if (e.state() == IdleState.WRITER_IDLE) {
				log.debug(clientId + " WRITER_IDLE");
			} else if (e.state() == IdleState.ALL_IDLE) {
				log.debug(clientId + " ALL_IDLE");
			}
		}
	}

	public synchronized void addActorStatusListener(ActorStatusListener listener) {
		log.debug(clientId + " actor status listener add");
		actorStatusListeners.add(listener);
	}

	public synchronized void removeActorStatusListener(ActorStatusListener listener) {
		log.debug(clientId + " actor status listener remove");
		actorStatusListeners.remove(listener);
	}

	public void publishShutdownEvent() {
		log.debug(clientId + " publish shutdown event to listener");
		log.debug("-publish end-");
	}

	public void publishActiveEvent() {
		log.debug(clientId + " publish active event to listener");
		this.isConnected.set(true);
		log.debug("-publish end-");
	}

	public void publishInactiveEvent() {
		log.debug(clientId + " publish Inactive event to listener");
		this.isConnected.set(false);
		log.debug("-publish end-");
	}

	public void publishactorSendmessage(String actorId, Object eventObj) {
		log.debug(actorId + " publish message to listener");

		log.debug("-publish end-");
	}

	// end for ChannelDuplexHandler function
    //sleep function using milliseconds unit as parameter 
	private void Sleep(int s) {
		try {
			TimeUnit.MILLISECONDS.sleep(s);
		} catch (InterruptedException e1) { // TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private boolean SetSignal(boolean firstSet, boolean sendReqFirst, String lightstr, String blinkstr) {
		byte l1 = (byte) 0x0, l2 = (byte) 0x0, b1 = (byte) 0x0, b2 = (byte) 0x0;
		char[] light = lightstr.toCharArray();
		char[] blink = blinkstr.toCharArray();
		boolean rtn = false;
		if (this.type.equals("AUTO28") || this.type.equals("AUTO20")) {
			// 顯示燈號
			if (light[1] == '1')
				l1 |= L0;
			if (light[2] == '1')
				b1 |= L0;
			if (light[3] == '1')
				l1 |= L1;
			if (light[4] == '1')
				l1 |= L2;
			if (light[5] == '1') {
				b1 |= L0;
				b1 |= L2;
			}
			if (light[6] == '1')
				l1 |= L3;
			if (light[7] == '1')
				l1 |= L5;
			if (light[8] == '1')
				l1 |= L6;
			if (light[9] == '1')
				l1 |= L4;
			// 閃爍燈號
			if (blink[1] == '1')
				b1 |= L0;
			if (blink[2] == '1')
				b1 |= L0;
			if (blink[3] == '1')
				b1 |= L1;
			if (blink[4] == '1')
				b1 |= L2;
			if (blink[5] == '1') {
				b1 |= L0;
				b1 |= L2;
			}
			if (blink[6] == '1')
				b1 |= L3;
			if (blink[7] == '1')
				b1 |= L5;
			if (blink[8] == '1')
				b1 |= L6;
			if (blink[9] == '1')
				b1 |= L4;
			rtn = prt.SetSignal(firstSet, sendReqFirst, l1, (byte) 0x0, b1, (byte) 0x0);
		} else if (this.type.equals("AUTO46") || this.type.equals("AUTO52")) {
			// 顯示燈號
			if (light[0] == '1')
				l1 |= I1;
			if (light[1] == '1')
				l2 |= I1;
			if (light[2] == '1')
				l2 |= I2;
			if (light[3] == '1')
				l2 |= I3;
			if (light[4] == '1')
				l2 |= I4;
			if (light[5] == '1')
				l2 |= I5;
			if (light[6] == '1')
				l2 |= I6;
			if (light[7] == '1')
				l2 |= I7;
			if (light[8] == '1')
				l2 |= I8;
			if (light[9] == '1')
				l2 |= I6;
			// 閃爍燈號
			if (blink[0] == '1')
				b1 |= I1;
			if (blink[1] == '1')
				b2 |= I1;
			if (blink[2] == '1')
				b2 |= I2;
			if (blink[3] == '1')
				b2 |= I3;
			if (blink[4] == '1')
				b2 |= I4;
			if (blink[5] == '1')
				b2 |= I5;
			if (blink[6] == '1')
				b2 |= I6;
			if (blink[7] == '1')
				b2 |= I7;
			if (blink[8] == '1')
				b2 |= I8;
			if (blink[9] == '1')
				b2 |= I6;
			rtn = prt.SetSignal(firstSet, sendReqFirst, l1, l2, b1, b2);
		}
		return rtn;
	}

	private boolean chk_Account(byte[] cussrc) {
		boolean rtn = false;
		short[] pair = { 6, 5, 4, 3, 2, 7, 6, 5, 4, 3, 2, -1 };
		short sum = 0, chkdg;
		for (int i = 0; i < (TXP.ACTNO_LEN - 1); i++)
			sum += (((short) cussrc[i] - 48) * pair[i]);
		chkdg = (short) (9 - (sum % 9));
		if (chkdg == (short) cussrc[TXP.ACTNO_LEN - 1] - 48)
			rtn = true;
		else {
			atlog.info("actno[{}] chkdg[{}] error!", new String(cussrc), chkdg);
		}
		return rtn;
	}

	private byte[] DataINQ(int iVal, int ifig, String dCount, String con) {
		return DataINQ(iVal, ifig, dCount, con, null);
	}

	private void setpasname(byte[] cussrc) {
		String chkcatagory = new String(cussrc, 3, 3);

		switch (chkcatagory) {
		// 台幣存摺
			case "001":
			case "002":
			case "003":
			case "004":
			case "005":
			case "006":
			case "008":
				pasname = "台幣存摺";
				break;
				// 外幣存摺
			case "007":
			case "021":
			case "701":
			case "702":
			case "703":
				pasname = "外幣存摺";
				break;
				// 黃金存摺
			case "071":
			case "072":
				pasname = "黃金存摺";
				break;
			default:
				pasname = "        ";
				break;
		}
		return;
	}
	private boolean MS_Check(byte[] cussrc) {
		boolean rtn = true;
		this.account = new String(cussrc, 0, TXP.ACTNO_LEN);
		if (!chk_Account(cussrc)) {
			rtn = false;
			amlog.info("[{}][{}][{}]:13存摺帳號錯誤！", brws, "        ", this.account);
			
			SetSignal(firstOpenConn, !firstOpenConn, "0000000000", "0000000001");
			return rtn;
		}
		log.debug("[{}]", new String(cussrc));
		/*************** check MSR's apno ***************/
		if (iEnd == 1) {
			//
			amlog.info("[{}][{}][{}]:13存摺帳號錯誤！", brws, pasname, this.account);
			rtn = false;
			return rtn;
		}
		/*************** check MSR's apno ***************/
		this.catagory = account.substring(3, 6);
		this.cpage = "";
		this.cline = "";
		switch (catagory) {
		// 台幣存摺
		case "001":
		case "002":
		case "003":
		case "004":
		case "005":
		case "006":
		case "008":
			this.actfiller = new String(cussrc, TXP.ACTNO_LEN, TXP.ACFILLER_LEN); // !< 帳號保留 MSR for PB/FC len 4
			this.msrbal = new String(cussrc, TXP.ACTNO_LEN + TXP.ACFILLER_LEN, TXP.MSRBAL_LEN); // !< 磁條餘額 MSR for PB/FC len 14, GL len 12
			this.cline = new String(cussrc, TXP.ACTNO_LEN + TXP.ACFILLER_LEN + TXP.MSRBAL_LEN, TXP.LINE_LEN); // !< 行次 MSR for PB/FC/GL len 2
			this.cpage = new String(cussrc, TXP.ACTNO_LEN + TXP.ACFILLER_LEN + TXP.MSRBAL_LEN + TXP.LINE_LEN, TXP.PAGE_LEN); // !< 頁次 MSR for PB/FC/GL len 2
			this.bkseq = new String(cussrc,
					TXP.ACTNO_LEN + TXP.ACFILLER_LEN + TXP.MSRBAL_LEN + TXP.LINE_LEN + TXP.PAGE_LEN, TXP.BKSEQ_LEN); // !< 領用序號 MSR for PB len 1, FC len 2
			atlog.info("台幣存摺 PB_MSR [{}]/[{}]/[{}]/[{}]/[{}]/[{}]", account, actfiller, msrbal, cline, cpage, bkseq);
			iFig = TXP.PBTYPE;
			break;
		// 外幣存摺
		case "007":
		case "021":
		case "701":
		case "702":
		case "703":
			this.actfiller = new String(cussrc, TXP.ACTNO_LEN, TXP.ACFILLER_LEN); // !< 帳號保留 MSR for PB/FC len 4
			this.msrbal = new String(cussrc, TXP.ACTNO_LEN + TXP.ACFILLER_LEN, TXP.MSRBAL_LEN); // !< 磁條餘額 MSR for PB/FC len 14, GL len 12
			this.cline = new String(cussrc, TXP.ACTNO_LEN + TXP.ACFILLER_LEN + TXP.MSRBAL_LEN, TXP.LINE_LEN); // !< 行次 MSR for PB/FC/GL len 2
			this.cpage = new String(cussrc, TXP.ACTNO_LEN + TXP.ACFILLER_LEN + TXP.MSRBAL_LEN + TXP.LINE_LEN,
					TXP.PAGE_LEN); // !< 頁次 MSR for PB/FC/GL len 2
			this.pbver = new String(cussrc,
					TXP.ACTNO_LEN + TXP.ACFILLER_LEN + TXP.MSRBAL_LEN + TXP.LINE_LEN + TXP.PAGE_LEN, TXP.PBVER_LEN); // !< 領用序號 MSR for PB len 1, FC len 2
			atlog.info("外幣存摺 FC_MSR [{}]/[{}]/[{}]/[{}]/[{}]/[{}]", account, actfiller, msrbal, cline, cpage, pbver);
			iFig = TXP.FCTYPE;
			break;
		// 黃金存摺
		case "071":
		case "072":
			this.msrbal = new String(cussrc, TXP.ACTNO_LEN, TXP.MSRBALGL_LEN); // !< 磁條餘額 MSR for PB/FC len 14, GL len 12
			this.cline = new String(cussrc, TXP.ACTNO_LEN + TXP.MSRBALGL_LEN, TXP.LINE_LEN); // !< 行次 MSR for PB/FC/GL len 2
			this.cpage = new String(cussrc, TXP.ACTNO_LEN + TXP.MSRBALGL_LEN + TXP.LINE_LEN, TXP.PAGE_LEN); // !< 頁次 MSR for PB/FC/GL len 2
			this.no = new String(cussrc, TXP.ACTNO_LEN + TXP.MSRBALGL_LEN + TXP.LINE_LEN + TXP.PAGE_LEN, TXP.NO_LEN); // !< 存摺號碼 MSR for GL len 9
			atlog.info("黃金存摺 GL_MSR [{}]/[{}]/[{}]/[{}]/[{}]", account, msrbal, cline, cpage, no);
			iFig = TXP.GLTYPE;
			break;
		default:
			amlog.info("[{}][{}][{}]:13存摺帳號錯誤！[{}](非台幣/外幣/黃金存摺)", brws, pasname, this.account);
			atlog.info("ERROR!! PB_MSR [{}]/[{}]/[{}]/[{}]/[{}]/[{}]", account, "", "", cline, cpage, bkseq);
			iFig = 0;
			rtn = false;
			break;
		}
		if (iFig == 0)
			return rtn;
		this.nline = Integer.parseInt(this.cline);
		this.npage = Integer.parseInt(this.cpage);

		return rtn;
	}

	/*********************************************************
	*  PbDataFormat() : Format TOTA Text to print            *
	*  function       : 列印台幣存摺資料格式                 *
	*  parameter 1    : tx_area data                         *
	*  parameter 2    : total NB count                       *
	*  return_code    : BOOL - TRUE                          *
	*                          FALSE               2008.01.24*
	*********************************************************/

	private boolean PbDataFormat() {
		boolean rtn = true;
		int tl,total;
		tl = this.iLine;
		total = this.iCon;
		String pbpr_date = String.format("%9s", " ");    //日期 9
		String pbpr_wsno = String.format("%7s", " ");    //櫃檯機編號 7
		String pbpr_crdblog = String.format("%36s", " ");   //摘要+支出收入金額 36
		String pbpr_crdb = String.format("%36s", " ");   //摘要+支出收入金額 36
		String pbpr_crdbT = String.format("%36s", " ");   //摘要+支出收入金額 36
		String pbpr_dscpt = String.format("%16s", " ");  //摘要 16 byte big
		String pbpr_balance = String.format("%18s", " ");//結存 18
		String pr_datalog = ""; //  80
		String pr_data = ""; //  80

		if (this.curState == STARTPROCTLM) {
			p0080DataFormat = new P0080TEXT();
			this.curState = PBDATAFORMAT;
		}
		log.debug("1--->p0080text=>{} {}", this.curState);
		try {
			//PB 日期(1+8)/空格(1)/櫃檯機編號(7)/摘要(16)/支出收入金額(20)/結存(18)/
			
			for (int i = 0; i < pb_arr.size(); i++) {
				//處理日期格式
				pr_data = "";
				pbpr_date = new String (p0080DataFormat.getTotaTextValueSrc("date", pb_arr.get(i))).trim();
				if (Integer.parseInt(pbpr_date) > 1000000)
					pbpr_date = String.format("%9s", pbpr_date);
				else {
					pbpr_date  = " " + pbpr_date.substring(0, 2) + "." + pbpr_date.substring(2, 4) + "." + pbpr_date.substring(4, 6);
				}
				pr_data = pr_data + pbpr_date;
				//處理櫃檯機編號
				pbpr_wsno = String.format("%5s%2s",new String (p0080DataFormat.getTotaTextValueSrc("trmno", pb_arr.get(i))).trim()
				,new String (p0080DataFormat.getTotaTextValueSrc("tlrno", pb_arr.get(i))).trim());
				pr_data = pr_data + " " + pbpr_wsno;
				//處理摘要
				byte dtype[] = p0080DataFormat.getTotaTextValueSrc("dsptype", pb_arr.get(i));
				byte[] dsptb = null;
				byte[] dsptbsnd = null;
				if (dtype[0] == (byte)'9') {
					dsptb = p0080DataFormat.getTotaTextValueSrc("dsptext", pb_arr.get(i));
					dsptb = FilterBig5(dsptb);
				} else {
					String desc = new String(p0080DataFormat.getTotaTextValueSrc("dscpt", pb_arr.get(i))).trim();
					if (DscptMappingTable.m_Dscpt.containsKey(desc))
////						dsptb = DscptMappingTable.m_Dscpt.get(desc).getBytes();
						dsptb = DscptMappingTable.m_Dscpt2.get(desc);
					else
						dsptb = desc.getBytes();
				}
				dsptbsnd = dsptb;
//				pbpr_dscpt = new String(FilterChi(pbpr_dscpt.getBytes()));
				//20100503 by Han 支出摘要第12位或若為中文碼時，轉為空白
				//20100503 by Han 存入摘要第17位或若為中文碼時，轉為空白
				byte[] tmpb1 = null;
////				log.debug("crdb=0 crdb=1 pbpr_dscpt[11]=[{}] dspt[16]=[{}] {} len={}",dsptb[11],dsptb[16], dsptb, dsptb.length);
				byte[] crdb = p0080DataFormat.getTotaTextValueSrc("crdb", pb_arr.get(i));
				if (crdb[0] == (byte)'0') {
					tmpb1 = new byte[24];
					Arrays.fill(tmpb1, (byte)' ');
					System.arraycopy(dsptb, 0, tmpb1, 0, dsptb.length);
					dsptb = tmpb1;
////					dsptb = FilterChi(dsptb, 12);
				}
				if (crdb[0] == (byte)'1') {
					tmpb1 = new byte[34];
////					dsptb = FilterChi(dsptb, 17);
					Arrays.fill(tmpb1, (byte)' ');
					System.arraycopy(dsptb, 0, tmpb1, 0, dsptb.length);
					dsptb = tmpb1;
				}
//				for (int ii = 0; ii < dsptb.length; ii++)
//					System.out.print(String.format("%x", dsptb[ii]));
//				System.out.println();
				log.debug("crdb=0 crdb=1 pbpr_dscpt[11]=[{}] dspt[16]=[{}] {} len={}",dsptb[11],dsptb[16], dsptb, dsptb.length);

				if (crdb[0] == (byte)'0') {
					pbpr_crdb = String.format("%12s", new String(dsptb, "BIG5"));
//					pbpr_crdblog = String.format("%12s", new String(dsptb));
					pbpr_crdblog = String.format("%12s", new String(dsptb, "BIG5"));
				} else {
					pbpr_crdb = String.format("%17s", new String(dsptb, "BIG5"));
//					pbpr_crdblog = String.format("%17s", new String(dsptb));
					pbpr_crdblog = String.format("%17s", new String(dsptb, "BIG5"));
				}
				//處理支出收入金額
				double dTxamt = 0.0;
				if (crdb[0] == (byte)'0') {
					//支出
					String samtbuf = "";
					samtbuf = new String(p0080DataFormat.getTotaTextValueSrc("stxamt", pb_arr.get(i)));
					dTxamt = Double.parseDouble(new String(p0080DataFormat.getTotaTextValueSrc("txamt", pb_arr.get(i))).trim()) / 100.0;
					if (samtbuf.equals("-"))
						dTxamt *= -1.0;
//					NumberFormat format =  new DecimalFormat("#####,###,##0.00        ");
//					pbpr_crdblog = pbpr_crdblog + String.format("%25s", format.format(dTxamt));
					pbpr_crdb = pbpr_crdb + String.format("%18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT) + "       ");
					
					pbpr_crdbT = String.format("%18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT) + "       ");
					
					pbpr_crdblog = pbpr_crdblog + String.format("%18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT) + "       ");
				} else {
					//收入
					String samtbuf = "";
					samtbuf = new String(p0080DataFormat.getTotaTextValueSrc("stxamt", pb_arr.get(i)));
					dTxamt = Double.parseDouble(new String(p0080DataFormat.getTotaTextValueSrc("txamt", pb_arr.get(i))).trim()) / 100.0;
					if (samtbuf.equals("-"))
						dTxamt *= -1.0;
//					NumberFormat format =  new DecimalFormat("#####,###,##0.00   ");
//					pbpr_crdb = pbpr_crdb + String.format("%19s", format.format(dTxamt));
//					pbpr_crdblog = pbpr_crdblog + String.format("%19s", format.format(dTxamt));
					pbpr_crdb = pbpr_crdb + String.format("%18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT) + "  ");

					pbpr_crdbT = String.format("%18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT) + "  ");

					pbpr_crdblog = pbpr_crdblog + String.format("%18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT) + "  ");
				}
				pr_datalog = pr_data;
				pbpr_crdb = String.format("%35s", pbpr_crdb);

				pbpr_crdbT = String.format("%35s", pbpr_crdbT);

				log.debug("pbpr_crdb len={} pbpr_crdbT [{}] len={}", pbpr_crdb.length(), pbpr_crdbT, pbpr_crdbT.length());

				String pr_dataprev = pr_data;

				pr_data = pr_data + pbpr_crdb;
				
				pr_datalog = pr_datalog + String.format("%35s", pbpr_crdblog);
				//處理結存
				String sbalbuff = "";
				sbalbuff = new String(p0080DataFormat.getTotaTextValueSrc("spbbal", pb_arr.get(i)));
				dTxamt = Double.parseDouble(new String(p0080DataFormat.getTotaTextValueSrc("pbbal", pb_arr.get(i))).trim()) / 100.0;
				if (sbalbuff.equals("-"))
					dTxamt *= -1.0;
//				NumberFormat format =  new DecimalFormat("*####,###,##0.00");
//				pbpr_balance = String.format("%19s", format.format(dTxamt));
				pbpr_balance = dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT2);
				if (this.type.equals("AUTO20")) {
					
				}
				byte[] nl = new byte[2];
				nl[0] = (byte)0x0d;
				nl[1] = (byte)0x0a;
				pr_data = pr_data + pbpr_balance + new String(nl);
				
				pbpr_crdbT = pbpr_crdbT + pbpr_balance + new String(nl);
				
				pr_datalog = pr_datalog + pbpr_balance;
				log.debug("pbpr_date=[{}] pbpr_wsno=[{}] pbpr_dscpt=[{}] pbpr_crdb=[{}] pbpr_balance=[{}] pr_data=[{}] pbpr_crdbT=[{}]", pbpr_date, pbpr_wsno, pbpr_dscpt, pbpr_crdb, pbpr_balance, pr_data, pbpr_crdbT);
				log.debug("pr_datalog=[{}]", pr_datalog);
				//Print Data
				if ( i == 0 )
				{
					for (int k=1; k <= (tl-1); k++)
					{
						if ( k == 12 && tl >= 13)
						{
							// tl 起始行數 > 12
//							prt.Parsing(firstOpenConn, "SKIP=3".getBytes());
							prt.SkipnLine(3);
						}
						else
//							prt.Parsing(firstOpenConn, "SKIP=1".getBytes());
							prt.SkipnLine(1);
					}
				}
				else
				{
					if ( (tl+i) == 13 )
					{
						// tl 起始行數 < 13
//						prt.Parsing(firstOpenConn, "SKIP=2".getBytes());
						prt.SkipnLine(2);
					}
					
				}
				log.debug("after skip line------------");
				
				byte[] sndbary = new byte[pr_dataprev.getBytes().length + pbpr_crdbT.getBytes().length];
				System.arraycopy(pr_dataprev.getBytes(), 0, sndbary, 0, pr_dataprev.getBytes().length);
				System.arraycopy(pbpr_crdbT.getBytes(), 0, sndbary, pr_dataprev.getBytes().length, pbpr_crdbT.getBytes().length);
				System.arraycopy(dsptbsnd, 0, sndbary, pr_dataprev.getBytes().length+1, dsptbsnd.length);
				
//				prt.Prt_Text(pr_data.getBytes());
				prt.Prt_Text(sndbary);
				//若印滿 24 筆且尚有補登資料，加印「請翻下頁繼續補登」
				if ( (tl+i) == 24 && (total > (i+1)) )
				{
					// 因為存摺會補到滿, PB 只有8頁, 如果是第8頁則不進行換頁流程
					// 20180518 , add
					if (this.npage >= TXP.PB_MAX_PAGE) {
						this.iEnd = 2;
						return true;
					}
					pr_data = "                                                     請翻下頁繼續補登\n";
					this.iEnd = 1;
					amlog.info("[{}][{}][{}]:62請翻下頁繼續補登..", brws, pasname, this.account);
					if (prt.Prt_Text(pr_data.getBytes()) == false)
						return false;
				}
				else
					this.iEnd = 0;
			}
		} catch (Exception e) {
			log.debug("error--->p0080text convert error", e.getMessage());
			rtn = false;
			this.curState = FORMATPRTDATAERROR;
		}
		return rtn;
	}

	/*********************************************************
	*  FcDataFormat() : Format TOTA Text to print            *
	*  function       : 列印外匯存摺資料格式                 *
	*  parameter 1    : tx_area data                         *
	*  parameter 2    : total NB count                       *
	*  return_code    : BOOL - TRUE                          *
	*                          FALSE               2008.08.25*
	*********************************************************/
	private boolean FcDataFormat() {
		boolean rtn = true;
		int tl, total;
		tl = this.iLine;
		total = this.iCon;
		String pbpr_date = String.format("%9s", " "); // 日期 9
		String pbpr_wsno = String.format("%5s", " "); // 櫃檯機編號 5
		String pbpr_crdblog = String.format("%36s", " "); // 摘要+支出收入金額 36
		String pbpr_crdb = String.format("%36s", " "); // 摘要+支出收入金額 36
		String pbpr_crdbT = String.format("%16s", " "); // 摘要+支出收入金額 36
		String pbpr_dscpt = String.format("%16s", " "); // 摘要 16 byte big
		String pbpr_balance = String.format("%18s", " ");// 結存 18
		String pr_datalog = ""; // 80
		String pr_data = ""; // 80

		if (this.curState == STARTPROCTLM) {
			q0880DataFormat = new Q0880TEXT();
			this.curState = PBDATAFORMAT;
		}
		log.debug("1--->q0880text=>{} {}", this.curState);
		try {
			// PB 日期(1+8)/空格(1)/櫃檯機編號(7)/摘要(16)/支出收入金額(20)/結存(18)/
			// FC 日期(1+8)/空格(1)/櫃檯機編號(5)/摘要(16)/幣別(3)/支出收入金額(21)/結存(18)

			for (int i = 0; i < fc_arr.size(); i++) {
				//處理日期格式
				pr_data = "";
				pbpr_date = String.format("%8s", (Integer.parseInt(new String (q0880DataFormat.getTotaTextValueSrc("date", fc_arr.get(i))).trim()) - 19110000));
				pr_data = pbpr_date;
				//處理櫃檯機編號
				pr_data = pr_data + " " + new String(q0880DataFormat.getTotaTextValueSrc("kinbr", fc_arr.get(i)))
					+ new String(q0880DataFormat.getTotaTextValueSrc("trmseq", fc_arr.get(i)));
				byte[] dsptb = q0880DataFormat.getTotaTextValueSrc("dscptx", fc_arr.get(i));
				String pr_dataprev = pr_data;
				dsptb = FilterBig5(dsptb);  //摘要
				pbpr_crdbT = String.format("%16s"," "); // 摘要 template(16 bytes)
				pr_data = pr_data + String.format("%-16s", new String(dsptb, "BIG5"));
				//處理幣別
				pbpr_crdbT = pbpr_crdbT + new String(q0880DataFormat.getTotaTextValueSrc("curcd", fc_arr.get(i)));
				pr_data = pr_data + new String(q0880DataFormat.getTotaTextValueSrc("curcd", fc_arr.get(i)));

				//處理支出收入金額
				double dTxamt = 0.0;
				byte[] crdb = q0880DataFormat.getTotaTextValueSrc("crdb", fc_arr.get(i));
				if (crdb[0] == (byte)'1') {
					//支出
					String samtbuf = "";
					samtbuf = new String(q0880DataFormat.getTotaTextValueSrc("hcode", fc_arr.get(i)));
					dTxamt = Double.parseDouble(new String(q0880DataFormat.getTotaTextValueSrc("txamt", fc_arr.get(i))).trim()) / 100.0;
					if (samtbuf.equals("1"))
						dTxamt *= -1.0;
//					pr_data = pr_data + String.format("%19s   ", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT));
					pr_data = pr_data + dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT1) + "  ";
					
//					pbpr_crdbT = pbpr_crdbT + String.format("  %19s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT));
					pbpr_crdbT = pbpr_crdbT + dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT1) + "  ";
					
					pbpr_crdblog = pbpr_crdblog + dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT1) + "  ";
				} else {
					//收入
					String samtbuf = "";
					samtbuf = new String(q0880DataFormat.getTotaTextValueSrc("hcode", fc_arr.get(i)));
					dTxamt = Double.parseDouble(new String(q0880DataFormat.getTotaTextValueSrc("txamt", fc_arr.get(i))).trim()) / 100.0;
					if (samtbuf.equals("1"))
						dTxamt *= -1.0;
					pr_data = pr_data + "  " + dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT1);

					pbpr_crdbT = pbpr_crdbT + "  " + dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT1);

					pbpr_crdblog = pbpr_crdblog + "   " + dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT1);
				}
				pr_datalog = pr_data;
				//處理結存
				log.debug("--->q0880text pbbal src [{}]", new String(q0880DataFormat.getTotaTextValueSrc("pbbal", fc_arr.get(i))).trim());
				dTxamt = Double.parseDouble(new String(q0880DataFormat.getTotaTextValueSrc("pbbal", fc_arr.get(i))).trim()) / 100.0;
				log.debug("--->q0880text pbbal float [{}]", dTxamt);
				pbpr_balance = dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT2);
				log.debug("--->q0880text pbbal convert=[{}]", pbpr_balance);
				byte[] nl = new byte[2];
				nl[0] = (byte)0x0d;
				nl[1] = (byte)0x0a;
				pr_data = pr_data + pbpr_balance + new String(nl);
				
				pbpr_crdbT = pbpr_crdbT + pbpr_balance + new String(nl);
			
				pr_datalog = pr_datalog + pbpr_balance;
				log.debug("pbpr_date=[{}] pbpr_wsno=[{}] pbpr_dscpt=[{}] pbpr_crdb=[{}] pbpr_balance=[{}] pr_data=[{}] pbpr_crdbT=[{}]", pbpr_date, pbpr_wsno, pbpr_dscpt, pbpr_crdb, pbpr_balance, pr_data, pbpr_crdbT);
				log.debug("pr_datalog=[{}]", pr_datalog);
				//Print Data
				if ( i == 0 )
				{
					for (int k=1; k <= (tl-1); k++)
					{
						if ( k == 12 && tl >= 13)
						{
							// tl 起始行數 > 12
//							prt.Parsing(firstOpenConn, "SKIP=3".getBytes());
							prt.SkipnLine(3);
						}
						else
//							prt.Parsing(firstOpenConn, "SKIP=1".getBytes());
							prt.SkipnLine(1);
					}
				}
				else
				{
					if ( (tl+i) == 13 )
					{
						// tl 起始行數 < 13
//						prt.Parsing(firstOpenConn, "SKIP=2".getBytes());
						prt.SkipnLine(2);
					}
					
				}
				log.debug("after skip line------------");
				byte[] sndbary = new byte[pr_dataprev.getBytes().length + pbpr_crdbT.getBytes().length];
				System.arraycopy(pr_dataprev.getBytes(), 0, sndbary, 0, pr_dataprev.getBytes().length);
				System.arraycopy(pbpr_crdbT.getBytes(), 0, sndbary, pr_dataprev.getBytes().length, pbpr_crdbT.getBytes().length);
				System.arraycopy(dsptb, 0, sndbary, pr_dataprev.getBytes().length, dsptb.length);
				prt.Prt_Text(sndbary);
				//若印滿 24 筆且尚有補登資料，加印「請翻下頁繼續補登」
				if ( (tl+i) == 24 && (total > (i+1)) )
				{
					// 因為存摺會補到滿, FC 只有5頁, 如果是第5頁則不進行換頁流程
					// 20180518 , add
					if (this.npage >= TXP.FC_MAX_PAGE) {
						this.iEnd = 2;
						return true;
					}
					pr_data = "                                                     請翻下頁繼續補登\n";
					this.iEnd = 1;
					amlog.info("[{}][{}][{}]:62請翻下頁繼續補登..", brws, pasname, this.account);
					if (prt.Prt_Text(pr_data.getBytes()) == false)
						return false;
				}
				else
					this.iEnd = 0;
			}

		} catch (Exception e) {
			log.debug("error--->q0880text convert error", e.getMessage());
			rtn = false;
			this.curState = FORMATPRTDATAERROR;
		}
		return rtn;
	}
	
	/*********************************************************
	*  GlDataFormat() : Format TOTA Text to print            *
	*  function       : 列印黃金存摺資料格式                 *
	*  parameter 1    : tx_area data                         *
	*  parameter 2    : total NB count                       *
	*  return_code    : BOOL - TRUE                          *
	*                          FALSE               2008.06.01*
	*********************************************************/
	private boolean GlDataFormat() {
		boolean rtn = true;
		int tl, total;
		tl = this.iLine;
		total = this.iCon;
		String pbpr_date = String.format("%8s", " "); // 日期 8
		String pbpr_wsno = String.format("%5s", " "); // 櫃檯機編號 5
		String pbpr_crdblog = String.format("%36s", " "); // 摘要+支出收入金額 36
		String pbpr_crdb = String.format("%36s", " "); // 摘要+支出收入金額 36
		String pbpr_crdbT = String.format("%10s", " "); // 摘要
		String pbpr_dscpt = String.format("%10s", " "); // 摘要 10 byte big
		String pbpr_balance = String.format("%12s", " ");// 結存 12
		String pr_datalog = ""; // 80
		String pr_data = ""; // 列印資料 80


		if (this.curState == STARTPROCTLM) {
			p0880DataFormat = new P0880TEXT();
			this.curState = PBDATAFORMAT;
		}
		log.debug("1--->p0880text=>{}", this.curState);
		try {
			// PB 日期(1+8)/空格(1)/櫃檯機編號(7)/摘要(16)/支出收入金額(20)/結存(18)/
			// FC 日期(1+8)/空格(1)/櫃檯機編號(5)/摘要(16)/幣別(3)/支出收入金額(21)/結存(18)
			// GL 空格(1)/日期(8)/空格(1)/櫃檯機編號(7)/空格(1)/摘要(8)/幣別(2)/單價(4.2)/空格(1)/支出(S5.2)/空格(1)/收入(S5.2)/空格(1)/結存(7.2)/空格(1)/更正記號(1)

			for (int i = 0; i < gl_arr.size(); i++) {
				//空格(1)+日期
				pr_data = "";
				pbpr_date = new String(p0880DataFormat.getTotaTextValueSrc("txday", gl_arr.get(i)));
				pr_data = pbpr_date;

				//空格(2)+櫃台機編號
				pr_data = pr_data + " " + new String(p0880DataFormat.getTotaTextValueSrc("kinbr", gl_arr.get(i)))
					+ new String(p0880DataFormat.getTotaTextValueSrc("trmseq", gl_arr.get(i)));
				byte[] dsptb = p0880DataFormat.getTotaTextValueSrc("dscptx", gl_arr.get(i));
				String pr_dataprev = pr_data;
				dsptb = FilterBig5(dsptb);

				//空格(1)+摘要
				pbpr_crdbT = String.format("%11s", " "); // " " + 摘要 template(11 bytes)
				pr_data = pr_data + " " + String.format("%-10s", new String(dsptb, "BIG5"));

				//空格(1)+幣別(NT/US)(2)+單價(7)
				//單價(4.2)
				pbpr_crdbT = pbpr_crdbT + " " + new String(p0880DataFormat.getTotaTextValueSrc("curcd", gl_arr.get(i)));
				pr_data = pr_data + " " + new String(p0880DataFormat.getTotaTextValueSrc("curcd", gl_arr.get(i)));

				double price = Double.parseDouble(new String(p0880DataFormat.getTotaTextValueSrc("price", gl_arr.get(i))).trim()) / 100.0;
				pbpr_crdbT = pbpr_crdbT + dataUtil.rfmtdbl(price, "ZZZ9.99");
				pr_data = pr_data + dataUtil.rfmtdbl(price, "ZZZ9.99");

				//處理支出(回售/提領)黃金數
				String wamtbuff = new String(p0880DataFormat.getTotaTextValueSrc("withsign", gl_arr.get(i)))
					+ new String(p0880DataFormat.getTotaTextValueSrc("withdraw", gl_arr.get(i)));

				//處理存入黃金數
				String damtbuff = new String(p0880DataFormat.getTotaTextValueSrc("deposign", gl_arr.get(i)))
						+ new String(p0880DataFormat.getTotaTextValueSrc("deposit", gl_arr.get(i)));

				//處理支出收入金額
				double dTxamt = 0.0;
				if (Double.parseDouble(wamtbuff) != 0) {
					//支出
					dTxamt = Double.parseDouble(wamtbuff) / 100.0;
					pr_data = pr_data +  " " + dataUtil.rfmtdbl(dTxamt, TXP.GRAM1) + "           ";
					
					pbpr_crdbT = pbpr_crdbT + " " + dataUtil.rfmtdbl(dTxamt, TXP.GRAM1) + "           ";
					
					pbpr_crdblog = pbpr_crdblog + " " + dataUtil.rfmtdbl(dTxamt, TXP.GRAM1) + "           ";
				} else {
					//收入
					dTxamt = Double.parseDouble(damtbuff) / 100.0;
					pr_data = pr_data +  "            " + dataUtil.rfmtdbl(dTxamt, TXP.GRAM1);
					
					pbpr_crdbT = pbpr_crdbT + "            " + dataUtil.rfmtdbl(dTxamt, TXP.GRAM1);
					
					pbpr_crdblog = pbpr_crdblog + "            " + dataUtil.rfmtdbl(dTxamt, TXP.GRAM1);
				}
				pr_datalog = pr_data;
				//處理結存
				log.debug("--->p0880text avebal src [{}]", new String(p0880DataFormat.getTotaTextValueSrc("avebal", gl_arr.get(i))).trim());
				dTxamt = Double.parseDouble(new String(p0880DataFormat.getTotaTextValueSrc("avebal", gl_arr.get(i))).trim()) / 100.0;
				log.debug("--->p0880text avebal float [{}]", dTxamt);
				pbpr_balance = dataUtil.rfmtdbl(dTxamt, TXP.GRAM2);
				log.debug("--->p0880text avebal convert=[{}]", pbpr_balance);
				tx_area.put("avebal", new String(p0880DataFormat.getTotaTextValueSrc("avebal", gl_arr.get(i))));
				
				tx_area.put("nbday", new String(p0880DataFormat.getTotaTextValueSrc("txday", gl_arr.get(i))));
				tx_area.put("nbseq", new String(p0880DataFormat.getTotaTextValueSrc("nbseq", gl_arr.get(i))));
				tx_area.put("kinbr", new String(p0880DataFormat.getTotaTextValueSrc("kinbr", gl_arr.get(i))));
				
				byte[] nl = new byte[2];
				nl[0] = (byte)0x0d;
				nl[1] = (byte)0x0a;
 			   pr_data = pr_data + " " + pbpr_balance + new String(nl);
				
				pbpr_crdbT = pbpr_crdbT + " " +  pbpr_balance + new String(nl);
				
				pr_datalog = pr_datalog + " " + pbpr_balance;
				log.debug("pbpr_date=[{}] pbpr_wsno=[{}] pbpr_dscpt=[{}] pbpr_crdb=[{}] pbpr_balance=[{}] pr_data=[{}] pbpr_crdbT=[{}]", pbpr_date, pbpr_wsno, pbpr_dscpt, pbpr_crdb, pbpr_balance, pr_data, pbpr_crdbT);
				log.debug("pr_datalog=[{}]", pr_datalog);
				//Print Data
				if ( i == 0 )
				{
					for (int k=1; k <= (tl-1); k++)
					{
						if ( k == 12 && tl >= 13)
						{
							// tl 起始行數 > 12
//							prt.Parsing(firstOpenConn, "SKIP=3".getBytes());
							prt.SkipnLine(3);
						}
						else
//							prt.Parsing(firstOpenConn, "SKIP=1".getBytes());
							prt.SkipnLine(1);
					}
				}
				else
				{
					if ( (tl+i) == 13 )
					{
						// tl 起始行數 < 13
//						prt.Parsing(firstOpenConn, "SKIP=2".getBytes());
						prt.SkipnLine(2);
					}
					
				}
				log.debug("after skip line------------");
				byte[] sndbary = new byte[pr_dataprev.getBytes().length + pbpr_crdbT.getBytes().length];
				System.arraycopy(pr_dataprev.getBytes(), 0, sndbary, 0, pr_dataprev.getBytes().length);
				System.arraycopy(pbpr_crdbT.getBytes(), 0, sndbary, pr_dataprev.getBytes().length, pbpr_crdbT.getBytes().length);
				System.arraycopy(dsptb, 0, sndbary, pr_dataprev.getBytes().length + 1, dsptb.length);
				prt.Prt_Text(sndbary);
				//若印滿 24 筆且尚有補登資料，加印「請翻下頁繼續補登」
				if ( (tl+i) == 24 && (total > (i+1)) )
				{
					// 因為存摺會補到滿, FC 只有5頁, 如果是第5頁則不進行換頁流程
					// 20180518 , add
					if (this.npage >= TXP.FC_MAX_PAGE) {
						this.iEnd = 2;
						return true;
					}
					pr_data = "                                                     請翻下頁繼續補登\n";
					this.iEnd = 1;
					amlog.info("[{}][{}][{}]:62請翻下頁繼續補登..", brws, pasname, this.account);
					if (prt.Prt_Text(pr_data.getBytes()) == false)
						return false;
				}
				else
					this.iEnd = 0;
			}

		} catch (Exception e) {
			log.debug("error--->p0880text convert error", e.getMessage());
			rtn = false;
			this.curState = FORMATPRTDATAERROR;
		}
		return rtn;
	}

	/*********************************************************
	*  WMSRFormat() : Format the new MSR                     *
	*  paramater 1  : tx area data                           *
	*  paramater 2  : AP flag                                *
	*  return_code  : BOOL - TRUE                            *
	*                        FALSE                 2008.01.30*
	*********************************************************/
	private boolean WMSRFormat(boolean start)
	{
		boolean rtn = false;
		int l = 0, p = 0, iCnt = 0;
		byte wline[] = new byte[2];
		byte wpage[] = new byte[2];
		Arrays.fill(wline, (byte) 0x0);
		Arrays.fill(wpage, (byte) 0x0);
		byte c_Msr[] = tx_area.get("c_Msr").getBytes();
		log.debug("{} {} {} WMSRFormat before to write flag={} PBTYPE {} MSR [{}]", brws, catagory, account, start, this.iFig, new String(c_Msr));
		if (start) {
			if (this.iFig == TXP.PBTYPE) {
				if (p0080DataFormat == null)
					p0080DataFormat = new P0080TEXT();
				System.arraycopy(c_Msr, 30, wline, 0, 2);
				System.arraycopy(c_Msr, 32, wpage, 0, 2);
				iCnt = pb_arr.size();
			}
			if (this.iFig == TXP.FCTYPE) {
				if (q0880DataFormat == null)
					q0880DataFormat = new Q0880TEXT();
				System.arraycopy(c_Msr, 30, wline, 0, 2);
				System.arraycopy(c_Msr, 32, wpage, 0, 2);
				iCnt = fc_arr.size();
			}
			if (this.iFig == TXP.GLTYPE) {
				if (p0880DataFormat == null)
					p0880DataFormat = new P0880TEXT();
				System.arraycopy(c_Msr, 24, wline, 0, 2);
				System.arraycopy(c_Msr, 26, wpage, 0, 2);
				iCnt = gl_arr.size();
			}
			l = Integer.parseInt(new String(wline));
			p = Integer.parseInt(new String(wpage));
			if ((l - 1) + iCnt == 24) {
				l = 1;
				p = p + 1;
			} else
				l = l + iCnt;
		}
		try {
			switch (this.iFig) {
			case TXP.PBTYPE:
				if (start) {
					byte[] spbbal = p0080DataFormat.getTotaTextValueSrc("spbbal", pb_arr.get(iCnt - 1));
					if (new String(spbbal).equals("-"))
						System.arraycopy(spbbal, 0, c_Msr, 16, 1);
					else
						System.arraycopy("0".getBytes(), 0, c_Msr, 16, 1);
					System.arraycopy(p0080DataFormat.getTotaTextValueSrc("pbbal", pb_arr.get(iCnt - 1)), 0, c_Msr, 17,
							13);
					System.arraycopy(String.format("%02d", l).getBytes(), 0, c_Msr, 30, 2);
					System.arraycopy(String.format("%02d", p).getBytes(), 0, c_Msr, 32, 2);
					tx_area.put("c_Msr", new String(c_Msr));
				}
				rtn = prt.MS_Write(start, brws, account, c_Msr);
				log.debug("{} {} {} WMSRFormat after to write new PBTYPE line={} page={} MSR {}", brws, catagory, account, l, p, tx_area.get("c_Msr"));
				break;
			case TXP.FCTYPE:
				if (start) {
					System.arraycopy("0".getBytes(), 0, c_Msr, 16, 1);
					System.arraycopy(q0880DataFormat.getTotaTextValueSrc("pbbal", fc_arr.get(iCnt - 1)), 0, c_Msr, 17,
							13);
					System.arraycopy(String.format("%02d", l).getBytes(), 0, c_Msr, 30, 2);
					System.arraycopy(String.format("%02d", p).getBytes(), 0, c_Msr, 32, 2);
					tx_area.put("c_Msr", new String(c_Msr));
				}
				rtn = prt.MS_Write(start, brws, account, c_Msr);
				log.debug("{} {} {} WMSRFormat after to write new FCTYPE line={} page={} MSR {}", brws, catagory, account, l, p, tx_area.get("c_Msr"));
				break;
			case TXP.GLTYPE:
				if (start ) {
					System.arraycopy("000".getBytes(), 0, c_Msr, 16, 3);
					System.arraycopy(p0880DataFormat.getTotaTextValueSrc("avebal", gl_arr.get(iCnt - 1)), 0, c_Msr, 15,
							9);
					System.arraycopy(String.format("%02d", l).getBytes(), 0, c_Msr, 24, 2);
					System.arraycopy(String.format("%02d", p).getBytes(), 0, c_Msr, 26, 2);
					tx_area.put("c_Msr", new String(c_Msr));
				}
				rtn = prt.MS_Write(start, brws, account, c_Msr);
				log.debug("{} {} {} WMSRFormat after to write new GLTYPE line={} page={} MSR {}", brws, catagory, account, l, p, tx_area.get("c_Msr"));
				break;
			default:
				rtn = false;
				break;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.debug("{} {} {} WMSRFormat exception {}", brws, catagory, account, e.getMessage());
		}

		return rtn;
	}

	private boolean WMSRFormat(boolean start, int setPage)
	{
		boolean rtn = false;
		int l = 0, p = 0;
		byte wline[] = new byte[2];
		byte wpage[] = new byte[2];
		Arrays.fill(wline, (byte) 0x0);
		Arrays.fill(wpage, (byte) 0x0);
		byte c_Msr[] = tx_area.get("c_Msr").getBytes();
		log.debug("{} {} {} WMSRFormat before to write flag={} PBTYPE {} MSR [{}] change page [{}]", brws, catagory, account, start, this.iFig, new String(c_Msr), p);
		p = setPage;
		try {
			switch (this.iFig) {
			case TXP.PBTYPE:
				System.arraycopy(String.format("%02d", p).getBytes(), 0, c_Msr, 32, 2);
				rtn = prt.MS_Write(start, brws, account, c_Msr);
				log.debug("{} {} {} WMSRFormat after to write new PBTYPE line={} page={} MSR {}", brws, catagory, account, l, p, tx_area.get("c_Msr"));
				break;
			case TXP.FCTYPE:
				System.arraycopy(String.format("%02d", p).getBytes(), 0, c_Msr, 32, 2);
				rtn = prt.MS_Write(start, brws, account, c_Msr);
				log.debug("{} {} {} WMSRFormat after to write new FCTYPE line={} page={} MSR {}", brws, catagory, account, l, p, tx_area.get("c_Msr"));
				break;
			case TXP.GLTYPE:
				System.arraycopy(String.format("%02d", p).getBytes(), 0, c_Msr, 26, 2);
				rtn = prt.MS_Write(start, brws, account, c_Msr);
				log.debug("{} {} {} WMSRFormat after to write new GLTYPE line={} page={} MSR {}", brws, catagory, account, l, p, tx_area.get("c_Msr"));
				break;
			default:
				rtn = false;
				break;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.debug("{} {} {} WMSRFormat exception {}", brws, catagory, account, e.getMessage());
		}

		return rtn;
	}
	/*********************************************************
	*	FilterBig5() : filter the chinese control code       *
	*   function     : 去除中文控制碼                        *
	*   parameter 1  : dsptext included the control code     *
	*   parameter 2  : dsptext length                        *
	*   return_code  : dsptext filter the control code       *
	*********************************************************/
	
	private byte[] FilterBig5(byte dsptext[]) {
		int iChin = 0, j = 0;
		for (int i = 0; i < dsptext.length; i++) {
			if (dsptext[i] == 0x04) {
				iChin = 1;
				continue;
			}
			if (dsptext[i] == 0x07) {
				if (iChin==1) {
					//dsptext[j++] = ' ';
					//dsptext[j++] = ' ';
				}
				iChin = 0;
				continue;
			}
			dsptext[j++] = dsptext[i];
		}
		for (int i = j; i < dsptext.length; i++)
			dsptext[j++] = ' ';
		// 20081104 , fix for chinese char cut half.
		if (dsptext[dsptext.length - 1] >= 0x80)
			dsptext[dsptext.length - 1] = 0x20;

		return dsptext;
	}

	/*********************************************************
	*	  FilterChi()  : filter the chinese control code       *
	*   function     : 去除文字全半形參雜(尾碼避免中文一半)  *
	*   parameter 1  : dsptext                               *
	*   parameter 2  : dsptext length                        *
	*   return_code  : dsptext                               *
	*********************************************************/

	private byte[] FilterChi(byte[] dsptext, int dsplen) {
		int pt = 0;

		for (int i = 0; i < dsplen; i++) {
			if ((int)dsptext[i] >= (int)(0x80 & 0xff)) {
				pt = pt + 2;
				i++;
			} else {
				pt = pt + 1;
			}
		}

		if (pt > dsplen)
			dsptext[dsplen - 1] = 0x20;
		return dsptext;
	}
	private byte[] DataDEL(int iVal, int ifig, String mbal) {
		byte[] rtn = null;
		P85TEXT p85text = null;
		Q98TEXT q98text = null;
		P1885TEXT p1885text = null;
		log.debug("1--->ifig={} mbal={}", ifig, mbal);
		try {
			if (iVal == TXP.SENDTHOST) { // send to host
				//***** Compose P85 TITA *****//
				if (ifig == TXP.PBTYPE)
				{
					p85text = new P85TEXT();
					boolean p85titatextrtn = p85text.initP85TitaTEXT((byte) '0');
					log.debug("p85titatextrtn.initP85TitaTEXT p0080titatextrtn={}", p85titatextrtn);
					tital.setValue("aptype", "P");
					tital.setValue("apcode", "85");
					tital.setValue("stxno", "00");
					tital.setValue("ptype", "0");
					tital.setValue("dscpt", "     ");
					tital.setValueLtoRfill("actno", tx_area.get("account"), (byte) ' ');
					if (tital.ChkCrdb(mbal) > 0)
						tital.setValue("crdb", "1");
					else
						tital.setValue("crdb", "0");
					String sm = this.msrbal.substring(1);
					atlog.info("pArr[0]=[{}]",sm);
					sm = tital.FilterMsr(sm, '-', '0');
					tital.setValue("txamt", sm);
					atlog.info("TITA_BASIC.txamt=[{}]",sm);
					tital.setValue("ver", "02");
					p85text.setValue("bkseq", this.bkseq);
					if (tital.ChkCrdb(this.msrbal) > 0)    ///check 20200224
						p85text.setValue("snpbbal", "+");
					else
						p85text.setValue("snpbbal", "-");
					p85text.setValue("npbbal", this.msrbal.substring(1));
//					String scnt = String.format("%04d", pb_arr.size());
					p85text.setValue("delcnt", String.format("%04d", pb_arr.size()));
//					p85text.setValue("fnbdtl", pb_arr.get(pb_arr.size() - 1));
					log.debug("pb_arr size() - 1={} [{}]", pb_arr.size() - 1, new String(pb_arr.get(pb_arr.size() - 1)));
					p85text.appendTitaText("date", pb_arr.get(pb_arr.size() - 1));
					rtn = tital.mkTITAmsg(tital.getTitalabel(), p85text.getP85Titatext());
					log.debug("P85 tita {}", new String(rtn));
				}
				//***** Compose Q98 TITA *****//
				else if (ifig == TXP.FCTYPE)
				{
					q98text = new Q98TEXT();
					boolean q98titatextrtn = q98text.initQ98TitaTEXT((byte) ' ');
					log.debug("q98titatextrtn.initQ98TitaTEXT q98titatextrtn={}", q98titatextrtn);
					tital.setValue("aptype", "Q");
					tital.setValue("apcode", "98");
					tital.setValue("stxno", "00");
					tital.setValue("ptype", "0");
					tital.setValue("dscpt", "     ");
					tital.setValueLtoRfill("actno", tx_area.get("account"), (byte) ' ');
					tital.setValue("crdb", "0");
					tital.setValue("nbcd", "3");
					log.debug("fc_arr size() - 1={} [{}]", fc_arr.size() - 1, new String(fc_arr.get(fc_arr.size() - 1)));
					String sm = this.msrbal.substring(1);
					atlog.info("fArr[0]=[{}]",sm);
					tital.setValue("txamt", sm);
					atlog.info("TITA_BASIC.txamt=[{}]",sm);
					q98text.setValue("newseq", tx_area.get("txseq"));
					q98text.setValue("oldseq", tx_area.get("txseq"));
					q98text.setValue("oldwsno", "00000");
					q98text.setValue("retur", "0");
					q98text.setValue("rbrno", tital.getValue("brno"));
					q98text.setValue("acbrno", tital.getValue("brno"));
					q98text.setValue("aptype", "14");
					q98text.setValue("corpno", "00");
					q98text.setValue("actfg", "1");
					q98text.setValue("nbcnt", String.format("%03d", fc_arr.size()));
					q98text.setValue("txday", tx_area.get("txday"));
					q98text.setValue("txseq", tx_area.get("txseq"));
					q98text.setValue("pbbal", tx_area.get("pbbal"));
					q98text.setValue("pbcol", tx_area.get("pbcol"));
					q98text.setValue("pbpage", tx_area.get("pbpage"));

					rtn = tital.mkTITAmsg(tital.getTitalabel(), q98text.getQ98Titatext());
					log.debug("Q98 tita [{}]", new String(rtn));
				}
				//***** Compose Pxx TITA *****//
				else if (ifig == TXP.GLTYPE)
				{
					p1885text = new P1885TEXT();
					boolean p1885titatextrtn = p1885text.initP1885TitaTEXT((byte) ' ');
					log.debug("p1885titatextrtn.initP1885TitaTEXT p1885titatextrtn={}", p1885titatextrtn);
					tital.setValue("aptype", "P");
					tital.setValue("apcode", "18");
					tital.setValue("stxno", "85");
					tital.setValue("ptype", "0");
					tital.setValue("dscpt", "     ");
					tital.setValueLtoRfill("actno", tx_area.get("account"), (byte) ' ');
					if (tital.ChkCrdb(mbal) > 0)
						tital.setValue("crdb", "1");
					else
						tital.setValue("crdb", "0");
					tital.setValue("nbcd", "8");
					log.debug("fc_arr size() - 1={} [{}]", gl_arr.size() - 1, new String(gl_arr.get(gl_arr.size() - 1)));
					String sm = "0" + this.msrbal;
					atlog.info("gArr[0]=[{}]",sm);
					tital.setValue("txamt", sm);
					log.debug("txamt[{}]", sm);
					atlog.info("TITA_BASIC.txamt=[{}]",sm);
					p1885text.setValueLtoRfill("glcomm", "00", (byte)' ');
					if (tital.ChkCrdb(tx_area.get("avebal")) > 0)
						p1885text.setValue("snpbbal", "+");
					else
						p1885text.setValue("snpbbal", "-");
					sm = tx_area.get("avebal");

					sm = "000" + tital.FilterMsr(sm, '-', '0');
					p1885text.setValue("npbbal", sm);
					p1885text.setValue("delcnt", String.format("%04d", gl_arr.size()));
					p1885text.setValue("nbday", tx_area.get("nbday"));
					p1885text.setValue("nbseq", tx_area.get("nbseq"));
					p1885text.setValue("kinbr", tx_area.get("kinbr"));
					p1885text.setValue("nbno", tx_area.get("nbno"));
					p1885text.setValue("lineno", tx_area.get("lineno"));
					p1885text.setValue("pageno", this.no);
					p1885text.setValue("end", "$");

					rtn = tital.mkTITAmsg(tital.getTitalabel(), p1885text.getP1885Titatext());
					log.debug("P1885 tita [{}]", new String(rtn));
				}
			} else { //iVal == RECVFHOST
				//***** Receive P001 TOTA and check error *****//
				if (ifig == TXP.PBTYPE)
				{
				}
				//***** Receive Q980 TOTA and check error *****//
				else if (ifig == TXP.FCTYPE)
				{
				}
				//***** Receive P885 TOTA and check error *****//
				else if (ifig == TXP.GLTYPE)
				{
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("telegram  DataDEL error on tita label: {}", e.getMessage());
		}
		return rtn;
	}

	private byte[] DataINQ(int iVal, int ifig, String dCount, String con, byte[] opttotatext) {
		// optotatext only used while iVal == TXP.RECVFHOST mode
		byte[] rtn = null;
		P0080TEXT p0080text = null;
		Q0880TEXT q0880text = null;
		P0880TEXT p0880text = null;
		int begin = Integer.parseInt(dCount);
		int totCnt = Integer.parseInt(con);
		int inqiLine = Integer.parseInt(tx_area.get("cline").trim());
		log.debug("1--->begin=>{} totCnt={} inqiLine={}", begin, totCnt, inqiLine);
		if (opttotatext != null && opttotatext.length > 0)
			log.debug("1.1--->opttotatext.length=>{}", opttotatext.length);
		try {
			if (iVal == TXP.SENDTHOST) { // send to host
				if (ifig == TXP.PBTYPE) {
					p0080text = new P0080TEXT();
					boolean p0080titatextrtn = p0080text.initP0080TitaTEXT((byte) '0');
					log.debug("p0080titatextrtn.initP0080TitaTEXT p0080titatextrtn={}", p0080titatextrtn);
					tital.setValue("aptype", "P");
					tital.setValue("apcode", "00");
					tital.setValue("stxno", "80");
					tital.setValue("dscpt", "S80  ");
					tital.setValueLtoRfill("actno", tx_area.get("account"), (byte) ' ');
					if (tital.ChkCrdb(this.msrbal) > 0)
						tital.setValue("crdb", "1");
					else
						tital.setValue("crdb", "0");
					String sm = this.msrbal.substring(1);
					sm = tital.FilterMsr(sm, '-', '0');
					tital.setValue("txamt", sm);
					tital.setValue("ver", "02");
					this.pbavCnt = 999;
//					this.pbavCnt = 1;
					p0080text.setValueRtoLfill("pbcnt", String.format("%d", this.pbavCnt), (byte) '0');
					p0080text.setValue("bkseq", this.bkseq);
					// 要求筆數(若該頁剩餘筆數 < 6，則為"剩餘筆數")
					if ((inqiLine - 1 + begin) + 6 > 24) {
						int reqcnt = 24 - (inqiLine - 1 + begin);
						atlog.info("reqcnt = [{}]",reqcnt);
						p0080text.setValueRtoLfill("reqcnt", Integer.toString(reqcnt), (byte) '0');
					} else {
						// 若剩餘要求之未登摺筆數 < 6，則為"剩餘之未登摺筆數"，否則為6
						if (totCnt > 0 && begin + 6 > totCnt) {
							log.debug("TxFlow : () -- reqcnt 2 ={}", Integer.toString(totCnt - begin));
							p0080text.setValueRtoLfill("reqcnt", Integer.toString(totCnt - begin), (byte) '0');
						} else {
							log.debug("TxFlow : () -- reqcnt 3 =6");
							p0080text.setValueRtoLfill("reqcnt", Integer.toString(6), (byte) '0');
						}
					}
					//未登摺之第幾筆
					log.debug("--->begin=>{}", begin);
					if (begin == 0)
						p0080text.setValueRtoLfill("begin", Integer.toString(1), (byte) '0');
					else
						p0080text.setValueRtoLfill("begin", Integer.toString(begin + 1), (byte) '0');
					rtn = tital.mkTITAmsg(tital.getTitalabel(), p0080text.getP0080Titatext());
				} else if (iFig == TXP.FCTYPE) {
					q0880text = new Q0880TEXT();
					boolean q0880titatextrtn = q0880text.initQ0880TitaTEXT((byte) '0');
					log.debug("q0880titatextrtn.initQ0880TitaTEXT q0880titatextrtn={}", q0880titatextrtn);
					tital.setValue("aptype", "Q");
					tital.setValue("apcode", "08");
					tital.setValue("stxno", "80");
					tital.setValue("dscpt", "S80  ");
					tital.setValueLtoRfill("actno", tx_area.get("account"), (byte) ' ');
					tital.setValue("crdb", "0");
					tital.setValue("nbcd", "3");
					tital.setValue("txamt", this.msrbal.substring(1));
					q0880text.appendTitaText("newseq", "                              ".getBytes());
					q0880text.setValue("retur", "0");
					q0880text.setValue("rbrno", this.brws.substring(0, 3));
					q0880text.setValue("acbrno", this.brws.substring(0, 3));
					q0880text.setValue("aptype", "14");
					q0880text.setValue("corpno", "00");
					q0880text.setValue("actfg", "1");
					this.pbavCnt = 999;
					q0880text.setValueRtoLfill("pbcnt", String.format("%d", this.pbavCnt), (byte) '0');
					q0880text.setValue("pbver", this.pbver);
					q0880text.setValue("txnos", String.format("%04d",6));
					if (begin == 0)
						q0880text.setValue("begin",String.format("%04d",1));
					else
						q0880text.setValue("begin",String.format("%04d", begin + 1));

					if (begin == 0) {
						q0880text.setValue("txday","00000000");
						q0880text.setValue("txseq","000000");
					}
					else {
						q0880text.setValue("txday",tx_area.get("txday"));
						q0880text.setValue("txseq",tx_area.get("txseq"));
					}
					q0880text.setValue("pbcol",this.cline);
					q0880text.setValue("pbpage",this.cpage);
					tx_area.put("pbcol", this.cline);
					tx_area.put("pbpage", this.cpage);
					rtn = tital.mkTITAmsg(tital.getTitalabel(), q0880text.getQ0880Titatext());
				} else if (iFig ==TXP.GLTYPE) {
					p0880text = new P0880TEXT();
					boolean p0880titatextrtn = p0880text.initP0880TitaTEXT((byte) '0');
					log.debug("p0880titatextrtn.initP0880TitaTEXT p0880titatextrtn={}", p0880titatextrtn);
					tital.setValue("aptype", "P");
					tital.setValue("apcode", "08");
					tital.setValue("stxno", "80");
					tital.setValue("dscpt", "S80  ");
					tital.setValueLtoRfill("actno", tx_area.get("account"), (byte) ' ');

					if (tital.ChkCrdb(this.msrbal) > 0)
						tital.setValue("crdb", "1");
					else
						tital.setValue("crdb", "0");

					tital.setValue("nbcd", "8");
					// 20080905 , prepare txamt
					String sm = "0" + this.msrbal;

					sm = tital.FilterMsr(sm, '-', '0');
					tital.setValue("txamt", sm);
					this.pbavCnt = 999;
					p0880text.setValueRtoLfill("pbcnt", String.format("%d", this.pbavCnt), (byte) '0');
					//GL-COMM共用(前兩位為0, 後48位為空白)
					p0880text.setValueLtoRfill("glcomm", "00".getBytes(), (byte) ' ');
					//要求筆數(若該頁剩餘筆數 < 6，則為"剩餘筆數")
					if ((inqiLine - 1 + begin) + 6 > 24) {
						int reqcnt = 24 - (inqiLine - 1 + begin);
						log.debug("TxFlow : DataINQ() -- reqcnt 1 ={}", Integer.toString(reqcnt));
						p0880text.setValueRtoLfill("reqcnt", Integer.toString(reqcnt), (byte) '0');
					} else {
						// 若剩餘要求之未登摺筆數 < 6，則為"剩餘之未登摺筆數"，否則為6
						if (totCnt > 0 && begin + 6 > totCnt) {
							log.debug("TxFlow : () -- reqcnt 2 ={}", Integer.toString(totCnt - begin));
							p0880text.setValueRtoLfill("reqcnt", Integer.toString(totCnt - begin), (byte) '0');
						} else {
							log.debug("TxFlow : () -- reqcnt 3 =6");
							p0880text.setValueRtoLfill("reqcnt", Integer.toString(6), (byte) '0');
						}
					}
					//未登摺之第幾筆
					log.debug("--->begin=>{}", begin);
					if (begin == 0)
						p0880text.setValueRtoLfill("begin", Integer.toString(1), (byte) '0');
					else
						p0880text.setValueRtoLfill("begin", Integer.toString(begin + 1), (byte) '0');
					p0880text.setValue("nbno",this.no);
					p0880text.setValue("pageno",this.cline);
					p0880text.setValue("pageno",this.cpage);
					tx_area.put("nbno", this.no);
					tx_area.put("lineno", this.cline);
					tx_area.put("pageno", this.cpage);
					rtn = tital.mkTITAmsg(tital.getTitalabel(), p0880text.getP0880Titatext());
					log.debug("TxFlow : DataINQ() -- rtn={}", new String(rtn));
				}
			} else { //iVal == RECVFHOST
				if (iFig == TXP.PBTYPE) {
					rtn = new byte[0];
					p0080text = new P0080TEXT();
					byte[] texthead = Arrays.copyOfRange(opttotatext, 0, p0080text.getP0080TotaheadtextLen());
					p0080text.copyTotaHead(texthead);
					con = new String(p0080text.getHeadValue("nbcnt"));
					log.debug("P0080totahead rtn={} tota.nbcnt={}",
							new String(texthead), con,
							new String(p0080text.getHeadValue("nbdelcnt")));
					if (Integer.parseInt(con) > this.pbavCnt) {
						//if (全部未登摺之資料筆數 > 存摺總剩餘可列印之資料筆數) Eject!
						SetSignal(firstOpenConn, !firstOpenConn, "0000000000","0000000001");
						Sleep(1000);
						amlog.info("[{}][{}][{}]:54全部未登摺之資料筆數[{}] > 存摺總剩餘可列印之資料筆數[{}]！", brws, pasname, this.account, con, this.pbavCnt);
						rtn = new byte[0];
					} else {
						int nCnt = Integer.parseInt(new String(p0080text.getHeadValue("nbdelcnt")));
						int iCnt = Integer.parseInt(dCount);
						atlog.info("iCnt=[{}] nCnt=[{}]", iCnt, nCnt);
						if (opttotatext.length > texthead.length) {
							int j = 0;
							byte[] text = Arrays.copyOfRange(opttotatext, p0080text.getP0080TotaheadtextLen(), opttotatext.length);
							log.debug("{} {} {} :TxFlow : () -- iCnt=[{}] nCnt=[{}] text.length={}", brws, catagory, account, iCnt, nCnt, text.length);
							if (text.length % p0080text.getP0080TotatextLen() == 0)
								j = text.length / p0080text.getP0080TotatextLen();
							log.debug("{} {} {} :TxFlow : () -- iCnt=[{}] nCnt=[{}] text.length={} j={}", brws, catagory, account, iCnt, nCnt, text.length, j);
							if (j == nCnt) {
								p0080text.copyTotaText(text, j);
								byte[] plus = {'+'};
								for (int i = 0; i < j; i++) {
									double dTxamt = Double.parseDouble(new String(p0080text.getTotaTextValue("txamt", i))) / 100.0;
									if (dTxamt == 0)
										p0080text.setTotaTextValue("stxamt", plus, i);
									atlog.info("i={} txamt={} dTxamt={} stxamt={} text=[{}]", i, new String(p0080text.getTotaTextValue("txamt", i)), dTxamt, new String(p0080text.getTotaTextValue("stxamt", i)), new String(p0080text.getTotaTexOc(i)));
								}
								this.pb_arr.addAll(p0080text.getTotaTextLists());
								rtn = text;
								log.debug("{} {} {} :TxFlow : () -- pb_arr.size={}", brws, catagory, account, pb_arr.size());
								iCnt = iCnt + nCnt;
								this.dCount = String.format("%03d", iCnt);
								log.debug("{} {} {} :TxFlow : after () -- dCount=[{}]", brws, catagory, account, this.dCount);
								//Print Data
							} else
								rtn = new byte[0];
						} else
							rtn = new byte[0];
					}
				} else if (iFig == TXP.FCTYPE) {
					rtn = new byte[0];
					q0880text = new Q0880TEXT();
					byte[] texthead = Arrays.copyOfRange(opttotatext, 0, q0880text.getQ0880TotaheadtextLen());
					q0880text.copyTotaHead(texthead);
					con = new String(q0880text.getHeadValue("nbcnt"));
					log.debug("Q0880totahead rtn={} tota.nbcnt={}",
							new String(texthead), con);
					if (Integer.parseInt(con) > this.pbavCnt) {
						//if (全部未登摺之資料筆數 > 存摺總剩餘可列印之資料筆數) Eject!
						SetSignal(firstOpenConn, !firstOpenConn, "0000000000","0000000001");
						Sleep(1000);
						amlog.info("[{}][{}][{}]:54全部未登摺之資料筆數[{}] > 存摺總剩餘可列印之資料筆數[{}]！", brws, pasname, this.account, con, this.pbavCnt);
						rtn = new byte[0];
					} else {
						totCnt = Integer.parseInt(con);
						atlog.info("begin=[{}] totCnt=[{}]", begin,	totCnt);
						if (opttotatext.length > texthead.length) {
							int j = 0;
							byte[] text = Arrays.copyOfRange(opttotatext, q0880text.getQ0880TotaheadtextLen(),
									opttotatext.length);
							log.debug("{} {} {} :TxFlow : () -- totCnt=[{}] text.length={} [{}]", brws, catagory, account,
									totCnt, text.length, new String(text));
							if (text.length % q0880text.getQ0880TotatextLen() == 0)
								j = text.length / q0880text.getQ0880TotatextLen();
							log.debug("{} {} {} :TxFlow : () -- totCnt=[{}] text.length={} j={}", brws, catagory,
									account, totCnt, text.length, j);
							if (j == totCnt) {
								q0880text.copyTotaText(text, j);
								if (begin < totCnt) {
									int dataCnt = 0;
									if ((totCnt - begin) >= 6)
										dataCnt = 6;
									else
										dataCnt = totCnt - begin;
									// 20080828 , 滿24筆時 dataCnt <= 6
									int iCur, iLeft;
									iCur = iLine + begin;
									iLeft = 25 - iCur;
									dataCnt = (dataCnt < iLeft) ? dataCnt : iLeft;
									atlog.info("dataCnt=[{}]",dataCnt);
									int i = 0;
									for (i = 0; i < dataCnt; i++) {
										// 20080923 , txday[0] == '0'
										if (q0880text.getTotaTextValue("totatxday", i) == null
												|| new String(q0880text.getTotaTextValue("totatxday", i)).trim()
														.length() == 0
												|| Integer.parseInt(
														new String(q0880text.getTotaTextValue("totatxday", i))) == 0)
											break;
										this.fc_arr.add(q0880text.getTotaTexOc(i));
										atlog.info("m_fArr[{}]=[{}]", begin + i, new String(q0880text.getTotaTexOc(i)));

									}
									if (i == 0) {
										atlog.info("m_fArr data null");
									} else {
										tx_area.put("txday", new String(q0880text.getTotaTextValue("totatxday", i - 1)));
										tx_area.put("txseq", new String(q0880text.getTotaTextValue("totatxseq", i - 1)));
										tx_area.put("pbbal", new String(q0880text.getTotaTextValue("pbbal", i - 1)));
										atlog.info("tx_area->txday=[{}] tx_area->txseq=[{}]",tx_area.get("txday"), tx_area.get("txseq"));
										rtn = text;
										log.debug("{} {} {} :TxFlow : () -- fc_arr.size={}", brws, catagory, account,
												fc_arr.size());
										this.dCount = String.format("%03d", begin + i);
										log.debug("{} {} {} :TxFlow : () -- this.dCount={}", brws, catagory, account,this.dCount);
									}
								} else {
									rtn = new byte[0];
								}
							}
						} else {
							rtn = new byte[0];
						}
					}
				} else if (iFig == TXP.GLTYPE) {
					rtn = new byte[0];
					p0880text = new P0880TEXT();
					byte[] texthead = Arrays.copyOfRange(opttotatext, 0, p0880text.getP0880TotaheadtextLen());
					p0880text.copyTotaHead(texthead);
					con = new String(p0880text.getHeadValue("nbcnt"));
					log.debug("P0880totahead rtn={} tota.nbcnt={}",
							new String(texthead), con);
					if (Integer.parseInt(con) > this.pbavCnt) {
						//if (全部未登摺之資料筆數 > 存摺總剩餘可列印之資料筆數) Eject!
						SetSignal(firstOpenConn, !firstOpenConn, "0000000000","0000000001");
						Sleep(1000);
						amlog.info("[{}][{}][{}]:54全部未登摺之資料筆數[{}] > 存摺總剩餘可列印之資料筆數[{}]！", brws, pasname, this.account, con, this.pbavCnt);
						rtn = new byte[0];
					} else {
						totCnt = Integer.parseInt(con);
						atlog.info("begin=[{}] totCnt=[{}]",begin,totCnt);
						if (opttotatext.length > texthead.length) {
							int j = 0;
							byte[] text = Arrays.copyOfRange(opttotatext, p0880text.getP0880TotaheadtextLen(),
									opttotatext.length);
							log.debug("{} {} {} :TxFlow : () -- totCnt=[{}] text.length={} [{}]", brws, catagory, account,
									totCnt, text.length, new String(text));
							if (text.length % p0880text.getP0880TotatextLen() == 0)
								j = text.length / p0880text.getP0880TotatextLen();
							log.debug("{} {} {} :TxFlow : () -- totCnt=[{}] text.length={} j={}", brws, catagory,
									account, totCnt, text.length, j);
							if (j == totCnt) {
								p0880text.copyTotaText(text, j);
								if (begin < totCnt) {
									int dataCnt = 0;
									if ((totCnt - begin) >= 6)
										dataCnt = 6;
									else
										dataCnt = totCnt - begin;
									// 20080828 , 滿24筆時 dataCnt <= 6
									int iCur, iLeft;
									iCur = iLine + begin;
									iLeft = 25 - iCur;
									dataCnt = (dataCnt < iLeft) ? dataCnt : iLeft;
									atlog.info("dataCnt=[{}]",dataCnt);
									int i = 0;
									for (i = 0; i < dataCnt; i++) {
										// 20080923 , txday[0] == '0'
										if (p0880text.getTotaTextValue("txday", i) == null
												|| new String(p0880text.getTotaTextValue("txday", i)).trim()
														.length() == 0
												|| Integer.parseInt(
														new String(p0880text.getTotaTextValue("txday", i))) == 0)
											break;
										this.gl_arr.add(p0880text.getTotaTexOc(i));
										atlog.info("m_fArr[{}]=[{}]",begin + i, new String(p0880text.getTotaTexOc(i)));
									}
									if (i == 0) {
										atlog.info("m_fArr data null");
									} else {
										tx_area.put("txday", new String(p0880text.getTotaTextValue("txday", i - 1)));
										tx_area.put("nbseq", new String(p0880text.getTotaTextValue("nbseq", i - 1)));
										tx_area.put("avebal", new String(p0880text.getTotaTextValue("avebal", i - 1)));
										log.debug("[{} {} {} : DataINQ() -- tx_area->txday=[{}] tx_area->nbseq=[{}] tx_area->avebal=[{}]",
												brws, catagory, account, tx_area.get("txday"), tx_area.get("nbseq"), tx_area.get("avebal"));
										rtn = text;
										log.debug("{} {} {} :TxFlow : () -- gl_arr.size={}", brws, catagory, account,
												gl_arr.size());
										this.dCount = String.format("%03d", begin + i);
										log.debug("{} {} {} :TxFlow : () -- this.dCount={}", brws, catagory, account,this.dCount);
									}
								} else {
									rtn = new byte[0];
								}
							}
						} else {
							rtn = new byte[0];
						}
					}
				}
			}
			if (iFig == TXP.PBTYPE)
				log.debug("4.1--->pb_arr.size=[{}]", pb_arr.size());
			else if (iFig == TXP.FCTYPE)
				log.debug("4.2--->fc_arr.size=[{}]", fc_arr.size());
			else if (iFig == TXP.GLTYPE)
				log.debug("4.3--->gl_arr.size=[{}]", gl_arr.size());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("telegram   error on tita label: {}", e.getMessage());
		}
		return rtn;
	}

	/*******************************************************************
	*	Send_Recv() : Send TX to Host /                *
	*                Receive Data from Host           *
	*   function    : 傳送INQ/UPD交易上中心/接收中心資料    *
	*   parameter 1 : AP type -- 1:PB / 2:FC / 3:GL   *
	*   parameter 2 : data function -- 1:INQ / 2:DEL  *
	*   parameter 3 : total NB count                  *
	*   parameter 4 : original MSR's balance          *
	*   return_code : = 0 - NORMAL                    *
	*                 < 0 - ERROR                     *
	********************************************************************/
	private int Send_Recv(int iflg, int ifun, String con, String mbal) {
		int rtn = 0;
		do {
			if (this.curState == SNDANDRCVTLM || this.curState == SNDANDRCVDELTLM) {
				this.iLine = Integer.parseInt(tx_area.get("cline").trim());
				con = "000";
				this.iCon = Integer.parseInt(con.trim());
				if (ifun == TXP.INQ) {
					pb_arr.clear();
					fc_arr.clear();
					gl_arr.clear();
				}
				tital = new TITATel();
				boolean titalrtn = tital.initTitaLabel((byte) '0');
				log.debug("tital.initTitaLabel rtn={}", titalrtn);

				try {
					tital.setValue("brno", "983");
					tital.setValue("wsno", "0403");
					try {
						this.setSeqNo = Integer
								.parseInt(FileUtils.readFileToString(this.seqNoFile, Charset.defaultCharset())) + 1;
						if (this.setSeqNo > 99999)
							this.setSeqNo = 0;
						FileUtils.writeStringToFile(this.seqNoFile, Integer.toString(this.setSeqNo),
								Charset.defaultCharset());
					} catch (Exception e) {
						log.warn("WORNING!!! update new seq number string {} error {}", this.setSeqNo, e.getMessage());
					}
					tital.setValueRtoLfill("txseq", String.format("%d", this.setSeqNo), (byte) '0');
					tital.setValue("trancd", "CB");
					tital.setValue("wstype", "0");
					tital.setValue("titalrno", "00");
					tital.setValueLtoRfill("txtype", " ", (byte) ' ');
					tital.setValue("spcd", "0");
					tital.setValue("nbcd", "0");
					tital.setValue("hcode", "0");
					tital.setValue("trnmod", "0");
					tital.setValue("sbtmod", "0");
					tital.setValue("curcd", "00");
					tital.setValue("pseudo", "1");
					if (!new String(this.fepdd).equals("  "))
						tital.setValue("fepdd", this.fepdd);
					atlog.info("fepdd=[{}]",this.fepdd);
					if (ifun == TXP.INQ) {
						if (this.iCount == 0) {
							amlog.info("[{}][{}][{}]:03中心存摺補登資料讀取中...", brws, pasname, this.account);
						}

						// Send Inquiry Request
						this.resultmsg = null;
						resultmsg = DataINQ(TXP.SENDTHOST, iflg, this.dCount, con);
						if (resultmsg == null || resultmsg.length == 0) {
							atlog.info("iMsgLen = 0");
							amlog.info("[{}][{}][{}]:31傳送之訊息長度為０！", brws, pasname, this.account);							
							rtn = -1;
						}
					} else {
						amlog.info("[{}][{}][{}]:04中心存摺已補登資料刪除中..", brws, pasname, this.account);
						this.resultmsg = null;
						resultmsg = DataDEL(TXP.SENDTHOST, iflg, mbal);
						if (resultmsg == null || resultmsg.length == 0) {
							atlog.info("iMsgLen = 0");
							amlog.info("[{}][{}][{}]:31傳送之訊息長度為０！", brws, pasname, this.account);							
							rtn = -1;
						}
					}
					this.curState = SETREQSIG;
					//20200403
					SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0010000000");
					//----
					atlog.info("TITA_TEXT=[{}]",new String(resultmsg));
					if (SetSignal(firstOpenConn, !firstOpenConn, "0000000000", "0010000000")) {
						this.curState = RECVTLM;
						log.debug("{} {} {} AutoPrnCls : --change start process telegram", brws, catagory, account);
					} else {
						this.curState = WAITSETREQSIG;
						log.debug("{} {} {} AutoPrnCls : --change wait Set Signal for request data", brws, catagory,
								account);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log.error("telegram compose error on tita label: {}", e.getMessage());
				}
			} else if (this.curState == WAITSETREQSIG) {
				if (SetSignal(!firstOpenConn, !firstOpenConn, "0000000000", "0010000000")) {
					this.curState = SENDTLM;
					log.debug("{} {} {} AutoPrnCls : --change start process telegram dispatcher.isTITA_TOTA_START()={} alreadySendTelegram ={} ", brws, catagory, account, dispatcher.isTITA_TOTA_START(), this.alreadySendTelegram);
				} else {
					log.debug("{} {} {} AutoPrnCls : --change wait Set Signal for request data", brws, catagory,
							account);
				}
			} else if (this.curState == SENDTLM || this.curState == RECVTLM) {
				if (this.curState == SENDTLM  && !dispatcher.isTITA_TOTA_START() && !alreadySendTelegram) {
					//not yet send telegram send firstly
					alreadySendTelegram = dispatcher.sendTelegram(resultmsg);
					if (ifun == 1 && iCount == 0) {
						amlog.info("[{}][{}][{}]:05中心存摺補登資料接收中...", brws, pasname, this.account);
					}
					this.curState = RECVTLM;
				} else if (dispatcher.isTITA_TOTA_START() && alreadySendTelegram) {
					this.rtelem = dispatcher.getResultTelegram();
					if (this.rtelem != null) {
						log.debug(
								"{} {} {} :AutoPrnCls : process telegram isTITA_TOTA_START={} alreadySendTelegram={} get {} [{}]",
								brws, catagory, account, dispatcher.isTITA_TOTA_START(), alreadySendTelegram,
								rtelem.length, new String(this.rtelem));
						total = new TOTATel();
						boolean totalrtn = total.copyTotaLabel(Arrays.copyOfRange(rtelem, 0, total.getTotalLabelLen()));
						log.debug("total.initTotaLabel rtn={} getTotalLabelLen={} {}", totalrtn,
								total.getTotalLabelLen(), this.rtelem.length);

						byte[] totatext = Arrays.copyOfRange(rtelem, total.getTotalLabelLen(), this.rtelem.length);
						log.debug("totatext len={}", totatext.length);
						try {
							String mt = new String(total.getValue("mtype"));
							String cMsg = "";
							if (mt.equals("E") || mt.equals("A") || mt.equals("X")) {
								if (total.getValue("mtype").equals("A"))
									msgid = "E" + new String(total.getValue("msgno"));
								for (int i = 0; i < totatext.length; i++)
									if (totatext[i] == 0x7 || totatext[i] == 0x4 || totatext[i] == 0x3)
										totatext[i] = 0x20;
								cMsg = "-" + new String(totatext).trim();
								log.debug("cMsg=[{}]", cMsg);
								int mno = Integer.parseInt(new String(total.getValue("msgno")));
								// 20100913 , E622:本次日不符 send C0099
								if (mno == 622) {
//									return 622;
									rtn = 622;
									break;
								}
								//20200428 add for receive TOTA ERROR message
								this.curState = EJECTAFTERPAGEERROR;
								// "A665" & "X665" 無補登摺資料、"A104" 該戶無未登摺資料
								if (mno == 665 || mno == 104) {
									SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000100");
									if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0000000100")) {
										amlog.info("[{}][{}][{}]:52[{}]{}{}!", brws, pasname, this.account,mt,mno, cMsg);
									} else {
										log.debug("{} {} {} {} {} {} AutoPrnCls : --change ", brws, catagory, account,
												mt, mno, cMsg);
									}
								}
								// E194 , 補登資料超過可印行數, 應至服務台換摺
								else if (mno == 194) {
									SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000001000");
									if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0000001000")) {
										amlog.info("[{}][{}][{}]:52[{}]{}{}!", brws, pasname, this.account,mt,mno, cMsg);
									} else {
										log.debug("{} {} {} {} {} {} AutoPrnCls : --change ", brws, catagory, account,
												mt, mno, cMsg);
									}
								} else {
									SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000001");
									if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0000000001")) {
										amlog.info("[{}][{}][{}]:52[{}]{}{}!", brws, pasname, this.account,mt,mno, cMsg);
									} else {
										log.debug("{} {} {} {} {} {} AutoPrnCls : --change ", brws, catagory, account,
												mt, mno, cMsg);
									}
								}
								if (ifun == 1)
									log.debug("[{}]:TxFlow : Send_Recv() -- INQ Data Failed ! msgid={}{}", brws, mt,
											mno);
								else
									log.debug("[{}]:TxFlow : Send_Recv() -- DEL Data Failed ! msgid={}{}", brws, mt,
											mno);
//								return (-2);
								rtn = -2;
								break;
							}
							if (ifun == TXP.INQ) {
								// Receive Inquiry Data
								// 20080923 , Check return value
								resultmsg = DataINQ(TXP.RECVFHOST, iflg, this.dCount, con, totatext);
								if (resultmsg == null || resultmsg.length == 0) {
									if (SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000001")) {
										amlog.info("[{}][{}][{}]:34接收資料錯誤！", brws, pasname, this.account);
									} else {
										log.debug("{} {} {} AutoPrnCls : --change ", brws, catagory, account);
									}
									atlog.info("Failed ! iRtncd=[-1]");
									rtn = -1;
									break;
								}
								iCount = Integer.parseInt(this.dCount);
								iCon = Integer.parseInt(con);
								log.debug("iCon={} iCon={} iLine={} (iLine - 1 + iCount)={}", iCount, iCon, iLine,
										(iLine - 1 + iCount));
								if ((iLine - 1 + iCount) >= 24) {
									atlog.info("[{}] TOTA_TEXT=[{}]", resultmsg.length, new String(resultmsg));
									amlog.info("[{}][{}][{}]:55存摺補登資料接收成功！", brws, pasname, this.account);
									this.curState = STARTPROCTLM;
									break;
								}
							} else {
								// Receive Delete Result
								DataDEL(TXP.RECVFHOST, iflg, "");
								amlog.info("[{}][{}][{}]:56存摺已補登資料刪除成功！", brws, pasname, this.account);
								this.curState = SNDANDRCVDELTLMCHKEND;
								break;
							}
							this.curState = STARTPROCTLM;
						} catch (Exception e) {
							e.getStackTrace();
							log.error("ERROR while get total label mtype {}" + e.getMessage());
						}
					} else {
						amlog.info("[{}][{}][{}]:21存摺頁次錯誤！[{}]", brws, pasname, this.account, rpage);
						if (SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000001")) {
							log.debug(
									"{} {} {} AutoPrnCls : --ckeep cheak barcode after Set Signal after check barcode",
									brws, catagory, account);
						} else {
							log.debug("{} {} {} AutoPrnCls : --keep cheak barcode after Set Signal after check barcode",
									brws, catagory, account);
						}
						rtn = -1;
					}
//					this.alreadySendTelegram = false;
				}
			}
		} while (this.iCount < iCon);

		//20200428 add for receive error TOTA  ERROR message set to this.curState == EJECTAFTERPAGEERROR
		if ((this.curState == STARTPROCTLM || this.curState == SNDANDRCVDELTLMCHKEND || this.curState == EJECTAFTERPAGEERROR)
				&& !dispatcher.isTITA_TOTA_START() && alreadySendTelegram)
			//relese channel
			this.alreadySendTelegram = false;

		if (ifun == TXP.DEL) {
			pb_arr.clear();
			fc_arr.clear();
			gl_arr.clear();
		}
		log.debug("{} {} {} this.curState={}", brws, catagory, account, this.curState);
		return rtn;
	}

	private void resetPassBook() {
		this.alreadySendTelegram = false;
		this.dispatcher.setTITA_TOTA_START(false);
		this.iFirst = 0;
		this.iEnd = 0;
		this.dCount = "000";
		this.iCount = Integer.parseInt(this.dCount);
		this.catagory = "";
		this.account = "";
		this.iFirst = 0;
		this.iEnd = 0;
		this.pb_arr.clear();
		this.fc_arr.clear();
		this.gl_arr.clear();
		this.pasname = "        ";
		this.curState = ENTERPASSBOOKSIG;
		SetSignal(firstOpenConn, firstOpenConn, "1100000000", "0000000000");
		log.debug("{}=====resetPassBook prtcliFSM", this.curState);
		return;
	}

	private void prtcliFSM(boolean isInit) {
		if (isInit) {
			this.curState = SESSIONBREAK;
			this.alreadySendTelegram = false;
			this.dispatcher.setTITA_TOTA_START(false);
			this.iFirst = 0;
			this.iEnd = 0;
			this.dCount = "000";
			this.iCount = Integer.parseInt(this.dCount);
			this.catagory = "";
			this.account = "";
			this.pasname = "        ";
			log.debug("=======================check prtcliFSM init");
			return;
		}

		log.debug("before {}=======================check prtcliFSM", this.curState);
		int before = this.curState;
		switch (this.curState) {
		case SESSIONBREAK:
			prt.OpenPrinter(firstOpenConn);
			this.curState = OPENPRINTER;
			log.debug("after {}=>{}===check prtcliFSM", before, this.curState);
			break;

		case OPENPRINTER:
			this.alreadySendTelegram = false;
			this.dispatcher.setTITA_TOTA_START(false);
			this.iFirst = 0;
			this.iEnd = 0;
			this.dCount = "000";
			this.iCount = Integer.parseInt(this.dCount);
			this.catagory = "";
			this.account = "";
			this.pasname = "        ";
			if ((this.iFirst == 0) && prt.OpenPrinter(!firstOpenConn)) {
				this.curState = ENTERPASSBOOKSIG;
				SetSignal(firstOpenConn, firstOpenConn, "1100000000", "0000000000");
				this.iFirst = 0;
				this.iEnd = 0;
				this.catagory = "";
				this.account = "";
				this.pb_arr.clear();
				this.fc_arr.clear();
				this.gl_arr.clear();
				log.debug("{}=====SetSignal prtcliFSM", this.curState);
			}
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case ENTERPASSBOOKSIG:
			if (SetSignal(!firstOpenConn, firstOpenConn, "1100000000", "0000000000")) {
				this.curState = CAPTUREPASSBOOK;
				amlog.info("[{}][{}][{}]****************************", brws, "        ", "            ");
				amlog.info("[{}][{}][{}]:00請插入存摺...", brws, pasname, "            ");
				prt.DetectPaper(firstOpenConn, 0);
			}
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case CAPTUREPASSBOOK:
			if (this.iFirst == 0 || this.autoturnpage.equals("false")) {
				if (this.iFirst == 1) {
					SetSignal(firstOpenConn, firstOpenConn, "1100000000", "0000010000");
					amlog.info("[{}][{}][{}]:62等待請翻下頁繼續補登...", brws, pasname, account);
				} else {
					if (prt.DetectPaper(!firstOpenConn, 0)) {
						this.curState = GETPASSBOOKSHOWSIG;
						log.debug("{} {} {} AutoPrnCls : --start Show Signal", brws, catagory, account);
						SetSignal(firstOpenConn, !firstOpenConn, "0000000000", "0010000000");
					} else
						log.debug("{} {} {} AutoPrnCls : Parsing() -- Detect Error!", brws, catagory, account);
				}
			}
			break;

		case GETPASSBOOKSHOWSIG:
			log.debug("{} {} {} :AutoPrnCls : Show Signal", brws, catagory, account);
			if (SetSignal(!firstOpenConn, !firstOpenConn, "0000000000", "0010000000")) {
				this.curState = SETCPI;
				prt.SetCPI(firstOpenConn, 6);
				log.debug("{} {} {} AutoPrnCls : --start Set CPI", brws, catagory, account);
			}
			break;

		case SETCPI:
			log.debug("{} {} {} :AutoPrnCls : Set CPI", brws, catagory, account);
			if (prt.SetCPI(!firstOpenConn, 6)) {
				this.curState = SETLPI;
				prt.SetLPI(firstOpenConn, 5);
				log.debug("{} {} {} AutoPrnCls : --start Set LPI", brws, catagory, account);
			}
			break;

		case SETLPI:
			log.debug("{} {} {} :AutoPrnCls : Set LPI", brws, catagory, account);
			if (prt.SetLPI(!firstOpenConn, 5)) {
				this.curState = SETPRINTAREA;
				prt.Parsing(firstOpenConn, "AREA".getBytes());
				log.debug("{} {} {} AutoPrnCls : --start Set LPI", brws, catagory, account);
			}
			break;
		case SETPRINTAREA:
			log.debug("{} {} {} :AutoPrnCls : Set PRINT Area", brws, catagory, account);
			if (prt.Parsing(firstOpenConn, "AREA".getBytes())) {
				this.curState = READMSR;
				cusid = null;
				if (null != (cusid = prt.MS_Read(firstOpenConn, brws))) {
					this.curState = CHKACTNO;
					for (int i = 0; i < cusid.length; i++)
						cusid[i] = cusid[i] == (byte) '<' ? (byte) '-' : cusid[i];
					setpasname(cusid);
					log.debug("{} {} {} 12存摺磁條讀取成功！", brws, catagory, new String(cusid, 0, TXP.ACTNO_LEN));
					amlog.info("[{}][{}][{}]:12存摺磁條讀取成功！", brws, pasname, new String(cusid, 0, TXP.ACTNO_LEN));
				}
				log.debug("{} {} {} AutoPrnCls : --start Read MSR", brws, catagory, account);
			}
			break;

		case READMSR:
			log.debug("{} {} {} :AutoPrnCls : Read MSR", brws, catagory, account);
			cusid = null;
			if (null != (cusid = prt.MS_Read(!firstOpenConn, brws))) {
				this.curState = CHKACTNO;
				for (int i = 0; i < cusid.length; i++)
					cusid[i] = cusid[i] == (byte) '<' ? (byte) '-' : cusid[i];
				setpasname(cusid);
				amlog.info("[{}][{}][{}]:12存摺磁條讀取成功！", brws, pasname, new String(cusid, 0, TXP.ACTNO_LEN));
				log.debug("{} {} {} AutoPrnCls : --start check Account", brws, catagory, account);
			}
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case CHKACTNO:
			log.debug("{} {} {} :========<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>AutoPrnCls : check Account", brws, catagory,
					account);
			if (MS_Check(cusid)) {
				tx_area.clear();
				tx_area.put("brws", brws);
				tx_area.put("account", account);
				tx_area.put("c_Msr", new String(cusid));
				tx_area.put("cpage", this.cpage);
				tx_area.put("cline", this.cline);
				tx_area.put("mbal", this.msrbal);
				tx_area.put("txday", "");
				tx_area.put("txseq", "");
				tx_area.put("keepacc", "");
				Arrays.fill(fepdd, (byte) ' ');
				this.iEnd = 0;
				this.dCount = "000";
				this.iCount = Integer.parseInt(this.dCount);
				tx_area.put("iEnd", Integer.toString(this.iEnd));
				this.curState = CHKBARCODE;
				log.debug("{} {} {} tx_area {} iFig={} AutoPrnCls : --start check barcode", brws, catagory, account,
						tx_area, iFig);
				amlog.info("[{}][{}][{}]:02檢查存摺頁次...", brws, pasname, account);
				if ((this.rpage = prt.ReadBarcode(firstOpenConn, (short) 2)) > 0) {
					log.debug("{} {} {} AutoPrnCls : --start telegram get rpage={} npage={}", brws, catagory, account,
							this.rpage, this.npage);
					if (npage == rpage) {
						amlog.info("[{}][{}][{}]:02檢查存摺頁次正確...正確頁次={} 插入頁次={} 行次={}", brws, pasname, account, npage, rpage, nline);

						if (SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0010000000")) {
							this.curState = SNDANDRCVTLM;
							log.debug("{} {} {} AutoPrnCls : --change process telegram", brws, catagory, account);
						} else {
							this.curState = SETSIGAFTERCHKBARCODE;
							log.debug("{} {} {} AutoPrnCls : --change Set Signal after check barcode", brws, catagory,
									account);
						}
					} else {
						amlog.info("[{}][{}][{}]:21存摺頁次錯誤！[{}]", brws, pasname, account, rpage);
						if (SetSignal(firstOpenConn, firstOpenConn, "0000000000","0000100000")) {
							this.curState = SETSIGAFTERCHKBARCODE;
							log.debug(
									"{} {} {} AutoPrnCls : --eject passbook set signal after check barcode page error!!",
									brws, catagory, account);
						} else {
							this.curState = SETSIGAFTERCHKBARCODE;
							log.debug("{} {} {} AutoPrnCls : --keep cheak barcode after Set Signal after check barcode",
									brws, catagory, account);
						}
					}
				}
			} else {
				this.curState = SESSIONBREAK;
				log.debug("{} {} {} AutoPrnCls : --check Account error", brws, catagory, account);
			}
			log.debug("{} {} {} AutoPrnCls : --Read MSR error", brws, catagory, account);

			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case CHKBARCODE:
			log.debug("{} {} {} :AutoPrnCls : process check barcode", brws, catagory, account);
			if ((this.rpage = prt.ReadBarcode(!firstOpenConn, (short) 2)) > 0) {
				log.debug("{} {} {} AutoPrnCls : --start telegram get rpage={} npage={}", brws, catagory, account,
						this.rpage, this.npage);
				if (npage == rpage) {
					amlog.info("[{}][{}][{}]:02檢查存摺頁次正確...正確頁次={} 插入頁次={} 行次={}", brws, pasname, account, npage, rpage, nline);
					if (SetSignal(firstOpenConn, !firstOpenConn, "0000000000", "0010000000")) {
						this.curState = SNDANDRCVTLM;
						log.debug("{} {} {} AutoPrnCls : --change process telegram", brws, catagory, account);
					} else {
						this.curState = SETSIGAFTERCHKBARCODE;
						log.debug("{} {} {} AutoPrnCls : --change Set Signal after check barcode", brws, catagory,
								account);
					}
				} else {
					amlog.info("[{}][{}][{}]:21存摺頁次錯誤！[{}]", brws, pasname, account, rpage);
//					WMSRFormat(true, rpage);
//					WMSRFormat(true, rpage);
					if (SetSignal(firstOpenConn, firstOpenConn, "0000000000","0000100000")) {
						this.curState = SETSIGAFTERCHKBARCODE;
						log.debug(
								"{} {} {} AutoPrnCls : --eject passbook set signal after check barcode page error!!",
								brws, catagory, account);
					} else {
						this.curState = SETSIGAFTERCHKBARCODE;
						log.debug("{} {} {} AutoPrnCls : --keep cheak barcode after Set Signal after check barcode",
								brws, catagory, account);
					}
				}
			}
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case SETSIGAFTERCHKBARCODE:
			log.debug("{} {} {} :AutoPrnCls : process setsignal after checkbcode", brws, catagory, account);
			if (npage == rpage) {
				if (SetSignal(!firstOpenConn, !firstOpenConn, "0000000000", "0010000000")) {
					this.curState = SNDANDRCVTLM;
					log.debug("{} {} {} AutoPrnCls : --change process telegram", brws, catagory, account);
				}
			} else {
//				amlog.info("[{}][{}][{}]:22存摺頁次不符...正確頁次={} 插入頁次={}", brws, pasname, account, npage, rpage);
				if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000","0000100000")) {
					amlog.info("[{}][{}][{}]:22存摺頁次不符...正確頁次={} 插入頁次={}", brws, pasname, account, npage, rpage);
					this.curState = EJECTAFTERPAGEERROR;
					log.debug(
							"{} {} {} AutoPrnCls : --eject passbook after check barcode page error!!",
							brws, catagory, account);
				}
			}
			if (this.rpage < 0) {
				SetSignal(!firstOpenConn, firstOpenConn, "0000000000","0000100000");
				this.curState = SESSIONBREAK;
				amlog.info("[{}][{}][{}]:21存摺頁次錯誤！[{}]", brws, pasname, account, rpage);
				close();
			}
			
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case EJECTAFTERPAGEERROR:
			log.debug("{} {} {} :AutoPrnCls : process EJECTAFTERPAGEERROR", brws,catagory, account);
			if (prt.Eject(firstOpenConn))
				resetPassBook();
			else
				this.curState = EJECTAFTERPAGEERRORWAIT;
			log.debug("after {}=>{} =====check prtcliFSM", before, this.curState);
			break;

		case EJECTAFTERPAGEERRORWAIT:
			log.debug("{} {} {} :AutoPrnCls : process EJECTAFTERPAGEERRORWAIT", brws,catagory, account);
			if (prt.Eject(!firstOpenConn))
				resetPassBook();
			log.debug("after {}=>{} =====check prtcliFSM", before, this.curState);
			break;

		case SNDANDRCVTLM:
			log.debug("{} {} {} :AutoPrnCls : process telegram isTITA_TOTA_START={} alreadySendTelegram={}", brws,
					catagory, account, dispatcher.isTITA_TOTA_START(), alreadySendTelegram);
			int r = 0;
			this.Send_Recv_DATAInq = true;
			if ((r = Send_Recv(this.iFig, TXP.INQ, "0", "0")) != 0) {
				//20200428 modify for receive TOTA ERROR message
				if (r < 0 && r != -2) {
					this.curState = SESSIONBREAK;
					amlog.info("[{}][{}][{}]:61存摺資料補登失敗！", brws, pasname, account);
				}
			}
			switch (this.iFig) {
				case TXP.PBTYPE:
					log.debug("SNDANDRCVTLM r = {} pb_arr.size()=>{}=====check prtcliFSM", r, pb_arr.size());
					break;
				case TXP.FCTYPE:
					log.debug("SNDANDRCVTLM r = {} fc_arr.size()=>{}=====check prtcliFSM", r, fc_arr.size());
					break;
				case TXP.GLTYPE:
					log.debug("SNDANDRCVTLM r = {} gl_arr.size()=>{}=====check prtcliFSM", r, gl_arr.size());
					break;
				default:
					log.debug("SNDANDRCVTLM r = {}  unknow passbook type=[{}]=====check prtcliFSM", r, this.iFig);
					break;
			}
			log.debug("after {}=>{} r={} =====check prtcliFSM", before, this.curState, r);
			break;

		case SETREQSIG:
		case WAITSETREQSIG:
		case SENDTLM:
		case RECVTLM:
			log.debug(
					"{} {} {} :AutoPrnCls : process set req signal before send telegram isTITA_TOTA_START={} alreadySendTelegram={}",
					brws, catagory, account, dispatcher.isTITA_TOTA_START(), alreadySendTelegram);
			r = 0;
			if (this.Send_Recv_DATAInq) {
				if ((r = Send_Recv(this.iFig, TXP.INQ, "0", "0")) != 0) {
					if (r < 0) {
						this.curState = SESSIONBREAK;
						amlog.info("[{}][{}][{}]:61存摺資料補登失敗！", brws, pasname, account);
					}
				}
			} else {
				if (Send_Recv(this.iFig, TXP.DEL, "", tx_area.get("mbal")) != 0) {
					if (r < 0) {
						this.curState = SESSIONBREAK;
						amlog.info("[{}][{}][{}]:61存摺資料補登失敗！", brws, pasname, account);
					}
				}
			}
			switch (this.iFig) {
				case TXP.PBTYPE:
					log.debug(
						"SENDTLM/RECVTLM r = {} pb_arr.size=>{} isTITA_TOTA_START={} alreadySendTelegram={}=====check prtcliFSM",
						r, pb_arr.size(), dispatcher.isTITA_TOTA_START(), this.alreadySendTelegram);
					break;
				case TXP.FCTYPE:
					log.debug(
						"SENDTLM/RECVTLM r = {} fc_arr.size=>{} isTITA_TOTA_START={} alreadySendTelegram={}=====check prtcliFSM",
						r, fc_arr.size(), dispatcher.isTITA_TOTA_START(), this.alreadySendTelegram);
					break;
				case TXP.GLTYPE:
					log.debug(
						"SENDTLM/RECVTLM r = {} gl_arr.size=>{} isTITA_TOTA_START={} alreadySendTelegram={}=====check prtcliFSM",
						r, gl_arr.size(), dispatcher.isTITA_TOTA_START(), this.alreadySendTelegram);
					break;
				default:
					log.debug(
						"SENDTLM/RECVTLM r = {} unonow passbook type=[{}] isTITA_TOTA_START={} alreadySendTelegram={}=====check prtcliFSM",
						r, this.iFig, dispatcher.isTITA_TOTA_START(), this.alreadySendTelegram);
					break;
			}
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case STARTPROCTLM:
			amlog.info("[{}][{}][{}]:06存摺資料補登中..", brws, pasname, account);
			switch (this.iFig) {
				case TXP.PBTYPE:
					log.debug("STARTPROCTLM pb_arr.size=>{}=====check prtcliFSM", pb_arr.size());
					if (PbDataFormat()) {
						this.curState = WRITEMSR;
					}
					break;
				case TXP.FCTYPE:
					log.debug("STARTPROCTLM fc_arr.size=>{}=====check prtcliFSM", fc_arr.size());
					if (FcDataFormat()) {
						this.curState = WRITEMSR;
					}
					break;
				case TXP.GLTYPE:
					log.debug("STARTPROCTLM gl_arr.size=>{}=====check prtcliFSM", gl_arr.size());
					if (GlDataFormat()) {
						this.curState = WRITEMSR;
					}
					break;
			}
			log.debug("{} {} {} AutoPrnCls : 補登... {}", brws, catagory, account, this.iFig == TXP.PBTYPE ? "台幣存摺" : (this.iFig == TXP.FCTYPE ? "外幣存摺" : "黃金存摺"));
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case PBDATAFORMAT:
			switch (this.iFig) {
				case TXP.PBTYPE:
					log.debug("PBDATAFORMAT pb_arr.size=>{}=====check prtcliFSM", pb_arr.size());
					if (PbDataFormat()) {
						this.curState = WRITEMSR;
					}
					break;
				case TXP.FCTYPE:
					log.debug("PBDATAFORMAT fc_arr.size=>{}=====check prtcliFSM", fc_arr.size());
					if (FcDataFormat()) {
						this.curState = WRITEMSR;
					}
					break;
				case TXP.GLTYPE:
					log.debug("PBDATAFORMAT gl_arr.size=>{}=====check prtcliFSM", gl_arr.size());
					if (GlDataFormat()) {
						this.curState = WRITEMSR;
					}
					break;
			}
			log.debug("{} {} {} AutoPrnCls : 補登... {}", brws, catagory, account, this.iFig == TXP.PBTYPE ? "台幣存摺" : (this.iFig == TXP.FCTYPE ? "外幣存摺" : "黃金存摺"));
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);

		case FORMATPRTDATAERROR:
			log.debug("{} {} {} ORMATPRTDATAERROR :AutoPrnCls : XXDataFormat() -- Print Data Error!", brws, catagory, account);
			amlog.info("[{}][{}][{}]:61存摺資料補登失敗！", brws, pasname, account);
			SetSignal(firstOpenConn, firstOpenConn, "0000000000","0000000001");
			prt.Eject(firstOpenConn);
			Sleep(2 * 1000);
			this.iEnd = 0;
			this.iFirst = 0;
			this.curState = OPENPRINTER;
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case WRITEMSR:
			log.debug("{} {} {} :AutoPrnCls : process WRITEMSR", brws, catagory, account);
			if (WMSRFormat(firstOpenConn)) {
				this.curState = SNDANDRCVDELTLM;
			} else
				this.curState = WRITEMSRWAITCONFIRM;
			log.debug("{} {} {} :AutoPrnCls : WMSRFormat() -- c_Msr=[{}]",brws, catagory, account, this.tx_area.get("c_Msr"));
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case WRITEMSRWAITCONFIRM:
			log.debug("{} {} {} :AutoPrnCls : process WRITEMSRWAITCONFIRM", brws, catagory, account);
			if (WMSRFormat(!firstOpenConn)) {
				amlog.info("[{}][{}][{}]:72存摺資料補登成功！", brws, pasname, account);				
				this.curState = SNDANDRCVDELTLM;
			}
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case SNDANDRCVDELTLM:
			log.debug("{} {} {} :AutoPrnCls : process SNDANDRCVDELTLM isTITA_TOTA_START={} alreadySendTelegram={}", brws,
					catagory, account, dispatcher.isTITA_TOTA_START(), alreadySendTelegram);
			r = 0;
			this.Send_Recv_DATAInq = false;
			if ((r = Send_Recv(this.iFig, TXP.DEL, "", tx_area.get("mbal"))) != 0) {
				if (r < 0) {
					SetSignal(firstOpenConn, firstOpenConn, "0000000000","0000000001");
					prt.Eject(firstOpenConn);
					Sleep(2 * 1000);
					this.iEnd = 0;
					this.iFirst = 0;

					this.curState = SESSIONBREAK;
					amlog.info("[{}][{}][{}]:73存摺資料補登刪除失敗！", brws, pasname, account);				
				}
			}
			log.debug("SNDANDRCVDELTLM r = {} pb_arr.size()=>{}=====check prtcliFSM", r, pb_arr.size());
			log.debug("after {}=>{} r={} =====check prtcliFSM", before, this.curState, r);
			break;

		case SNDANDRCVDELTLMCHKEND:
			log.debug("{} {} {} :AutoPrnCls : process SNDANDRCVDELTLMCHKEND", brws, catagory, account);
			if (iEnd == 1) {
				if (!this.autoturnpage.equals("false")){
					
				} else {
/*//20200403					if (SetSignal(firstOpenConn, firstOpenConn, "0000000000","0101010000")) {
						this.curState = SNDANDRCVDELTLMCHKENDEJECTPRT;
					}
				*/
					SetSignal(firstOpenConn, firstOpenConn, "0000000000","0101010000");
					this.curState = SNDANDRCVDELTLMCHKENDEJECTPRT;
				}
			} else {
				// Show Signal
/*//20200403				if (SetSignal(firstOpenConn, firstOpenConn, "0000000000","0001000000")) {
					this.curState = SNDANDRCVDELTLMCHKENDSETSIG;
				}*/
				SetSignal(firstOpenConn, firstOpenConn, "0000000000","0001000000");
				this.curState = SNDANDRCVDELTLMCHKENDSETSIG;
			}
			log.debug("after {}=>{} iEnd={} =====check prtcliFSM", before, this.curState, iEnd);
			break;

		case SNDANDRCVDELTLMCHKENDSETSIG:
			log.debug("{} {} {} :AutoPrnCls : process SNDANDRCVDELTLMCHKENDSETSIG", brws, catagory, account);
			if (iEnd == 1) {
				if (!this.autoturnpage.equals("false")){
					
				} else {
					if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000","0101010000")) {
						this.curState = SNDANDRCVDELTLMCHKENDEJECTPRT;
						if (prt.Eject(firstOpenConn)) {
							this.curState = CAPTUREPASSBOOK;
							iFirst = 1;
//20200401							Sleep(2 * 1000);
							log.debug("{} {} {}AutoPrnCls : 翻頁...", brws, catagory, account);
							
						}
					}
				}
			} else {
				// Show Signal
				if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0001000000")) {
					this.curState = SNDANDRCVDELTLMCHKENDEJECTPRT;
					if (prt.Eject(firstOpenConn)) {
//20200401						Sleep(2 * 1000);
						this.curState = FINISH;
					}
				}
			}
			log.debug("after {}=>{} iEnd={} =====check prtcliFSM", before, this.curState, iEnd);
			break;

		case SNDANDRCVDELTLMCHKENDEJECTPRT:
			log.debug("{} {} {} :AutoPrnCls : process SNDANDRCVDELTLMCHKENDEJECTPRT", brws, catagory, account);
			if (iEnd == 1) {
				if (!this.autoturnpage.equals("false")){
					
				} else {
					if (prt.Eject(!firstOpenConn)) {
						this.curState = CAPTUREPASSBOOK;
//20200401						Sleep(2 * 1000);
						log.debug("{} {} {}AutoPrnCls : 翻頁...", brws, catagory, account);
						iFirst = 1;
					}
				}
			} else {
				// Eject Priner
				if (prt.Eject(!firstOpenConn)) {
					this.curState = FINISH;
//20200401					Sleep(2 * 1000);
				}
			}
			log.debug("after {}=>{} iEnd={} =====check prtcliFSM", before, this.curState, iEnd);
			break;
			
		case FINISH:
			log.debug("{} {} {} :AutoPrnCls : process FINISH", brws, catagory, account);
			iFirst = 0;
			if (iEnd == 2)
				iEnd = 0;
			resetPassBook();
			log.debug("{} {} {}:AutoPrnCls : 完成!!.", brws, catagory, account);
			log.debug("after {}=>{} iEnd={} =====check prtcliFSM", before, this.curState, iEnd);
			break;

		default:
			log.debug("unknow status after {}=>{} iEnd={} =====check prtcliFSM", before, this.curState, iEnd);
			break;
		}
		return;
	}
	public String getClientId() {
		return this.clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getByDate() {
		return byDate;
	}

	public void setByDate(String byDate) {
		this.byDate = byDate;
	}
}

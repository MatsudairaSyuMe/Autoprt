package com.systex.sysgateii.gateway.autoPrtSvr.Server;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.systex.sysgateii.comm.pool.fas.FASSocketChannel;
import com.systex.sysgateii.gateway.comm.TXP;
import com.systex.sysgateii.gateway.listener.MessageListener;
import com.systex.sysgateii.gateway.util.dataUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

/*
 * FAS socket connect servr
 * socket controller
 *    
 * MatsudairaSyuMe
 * Ver 1.0
 *  20200115
 */
public class FASSvr implements MessageListener<byte[]>, Runnable {
	private static Logger log = LoggerFactory.getLogger(FASSvr.class);
	static FASSvr server;

	static String[] NODES = { "" };
	private FASSocketChannel ec2 = null;
	private static final int FAIL_EVERY_CONN_ATTEMPT = 1;
	private static final long TEST_TIME_SECONDS = 1;
	private Channel currConn;
	private boolean TITA_TOTA_START = false;
	private ConcurrentHashMap<Channel, File> currSeqMap = null;
	private File currSeqF = null;
	private String header1 = "\u000f\u000f\u000f";
	private String header2 = "";
	private int setSeqNo = 0;
	private String getSeqStr = "";


	public FASSvr() {
		log.info("FASSvr start");
	}

	@Override
	public void messageReceived(String serverId, byte[] msg) {
		// TODO Auto-generated method stub
		log.debug(" ms");
	}

	public void run() {
		RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
		String jvmName = bean.getName();
		long pid = Long.valueOf(jvmName.split("@")[0]);
		log.info("FASSvr MainThreadId={}", pid);
		try {
			this.ec2 = new FASSocketChannel(NODES);
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
	}

	public void stop() {
		log.debug("Enter stop");
	}

	public static void createServer(String cfgNodes) {
		log.debug("Enter createServer");
		NODES = cfgNodes.split(",");
		for (int i = 0; i < NODES.length; i++) {
			NODES[i] = NODES[i].trim();
			log.debug("Enter createServer {}", NODES[i]);
		}
		log.debug("Enter createServer size={}", NODES.length);
		server = new FASSvr();
	}

	public static void startServer() {
		log.debug("Enter startServer");
		if (server != null) {
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

	public static FASSvr getFASSvr() {
		return server;
	}

	public boolean sendTelegram(byte[] telmsg) {
		boolean rtn = false;
		if (telmsg == null) {
			log.debug("sendTelegram error send telegam null");
			return rtn;
		}
		try {
			int attempt = 0;
			this.currConn = null;
			while (null == (this.currConn = this.ec2.getConnPool().lease())) {
				if (++attempt < FAIL_EVERY_CONN_ATTEMPT)
					Thread.sleep(TEST_TIME_SECONDS);
				else
					break;
			}
			if (this.currConn != null) {
				try {
					InetSocketAddress sock = (InetSocketAddress) this.currConn.localAddress();
					int sendlen = TXP.CONTROL_BUFFER_SIZE + telmsg.length;
//					String p = String.format("%s:%d", new String(telmsg), sock.getPort());
					this.currSeqMap = this.ec2.getseqfMap();
					this.currSeqF = this.currSeqMap.get(this.currConn);
					try {
						this.setSeqNo = Integer.parseInt(FileUtils.readFileToString(this.currSeqF, Charset.defaultCharset())) + 1;
						if (this.setSeqNo > 999)
							this.setSeqNo = 0;
						FileUtils.writeStringToFile(this.currSeqF, Integer.toString(this.setSeqNo), Charset.defaultCharset());
						header2 = String.format("\u0001%03d\u000f\u000f",setSeqNo);
					} catch (Exception e) {
						log.warn("WORNING!!! update new seq number string {} error {}",this.setSeqNo, e.getMessage());
					}

					ByteBuf req = Unpooled.buffer();
					req.clear();
					req.writeBytes(header1.getBytes());
					req.writeBytes(dataUtil.to3ByteArray(sendlen));
					req.writeBytes(header2.getBytes());
					req.writeBytes(telmsg);
					this.currConn.writeAndFlush(req.retain()).sync();
					this.setTITA_TOTA_START(true);
					rtn = true;
//				} catch (UnsupportedEncodingException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log.debug("sendTelegram error {}", e.getMessage());
				}
			}
		} catch (final InterruptedException e) {
			log.debug("get connect from pool error {}", e.getMessage());
		} catch (final Throwable cause) {
			log.debug("get connect from pool error {}", cause.getMessage());
		}
		return rtn;
	}

	public byte[] getResultTelegram() {
		byte[] rtn = null;
		byte[] lenbary = new byte[3];
		byte [] telmbyteary = null;
		int size = 0;
		if (isTITA_TOTA_START()) {
			if (this.ec2.getrcvBuf().hasArray() && this.ec2.getrcvBuf().readableBytes() > 0) {
//				while (this.ec2.getrcvBuf().readableBytes() >= 12) {
				if (this.ec2.getrcvBuf().readableBytes() >= 12) {
					this.ec2.getrcvBuf().getBytes(this.ec2.getrcvBuf().readerIndex() + 3, lenbary);
					size = dataUtil.fromByteArray(lenbary);
					log.debug("clientMessageBuf.readableBytes={} size={}",this.ec2.getrcvBuf().readableBytes(), size);
					if (size > 0 && size <= this.ec2.getrcvBuf().readableBytes()) {
						telmbyteary = new byte[size];
						this.ec2.getrcvBuf().readBytes(telmbyteary);
						log.debug("read {} byte(s) from clientMessageBuf after {}", size, this.ec2.getrcvBuf().readableBytes());
						this.getSeqStr = new String(telmbyteary, 7, 3);
						this.currSeqMap = this.ec2.getseqfMap();
						this.currSeqF = this.currSeqMap.get(this.currConn);
						try {
							FileUtils.writeStringToFile(this.currSeqF, this.getSeqStr, Charset.defaultCharset());
						} catch (Exception e) {
							log.warn("WORNING!!! update new seq number string {} error {}",this.getSeqStr, e.getMessage());
						}
						rtn = new byte[telmbyteary.length - TXP.CONTROL_BUFFER_SIZE];
						System.arraycopy(telmbyteary, TXP.CONTROL_BUFFER_SIZE, rtn, 0, telmbyteary.length - TXP.CONTROL_BUFFER_SIZE);
						rtn = remove03(rtn);
						log.debug("get rtn len= {}", rtn.length);
						this.setTITA_TOTA_START(false);
//						break;
					}// else
//						break;
				}
//				rtn = new byte[this.ec2.getrcvBuf().readableBytes()];
//				log.debug("get rtn len= {}", rtn.length);
//				this.ec2.getrcvBuf().readBytes(rtn);
//				this.setTITA_TOTA_START(false);
				try {
					if (rtn != null && rtn.length > 0)
						this.ec2.getrcvBuf().clear();
					this.ec2.getConnPool().release(this.currConn);
				} catch (final Throwable cause) {
					log.debug("free connect from pool error {}", cause.getMessage());
				}
				log.debug("return connect to pool");
			}
		}
		return rtn;
	}
	
	private byte[] remove03(byte[] source) {
		if (source[source.length - 1] == 0x03) {
			source = ArrayUtils.subarray(source, 0, source.length - 1);
			log.debug("remove03");
		}
		return source;
	}

	public boolean isTITA_TOTA_START() {
		return TITA_TOTA_START;
	}

	public void setTITA_TOTA_START(boolean tITA_TOTA_START) {
		TITA_TOTA_START = tITA_TOTA_START;
	}
}

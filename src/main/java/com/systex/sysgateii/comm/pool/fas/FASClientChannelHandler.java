package com.systex.sysgateii.comm.pool.fas;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;

/**
 * Created by MatsudairaSyume
 *  2020/01/15
 */

public class FASClientChannelHandler extends ChannelInboundHandlerAdapter {
	private static Logger log = LoggerFactory.getLogger(FASClientChannelHandler.class);
	private static final ByteBuf HEARTBEAT_SEQUENCE = Unpooled
			.unreleasableBuffer(Unpooled.copiedBuffer("hb_request", CharsetUtil.UTF_8));

	static AtomicInteger count = new AtomicInteger(1);
//	private ByteBuf clientMessageBuf = Unpooled.buffer(16384);
	private ByteBuf clientMessageBuf = null;
	private ConcurrentHashMap<Channel, File> seqf_map = null;
	private File seqNoFile;
	private String getSeqStr = "";

	public FASClientChannelHandler(ByteBuf rcvBuf) {
		this.clientMessageBuf = rcvBuf;
	}

	public FASClientChannelHandler(ByteBuf rcvBuf, ConcurrentHashMap<Channel, File> seqfmap) {
		this.clientMessageBuf = rcvBuf;
		this.seqf_map = seqfmap;
	}


	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.debug("channelActive============");
		clientMessageBuf.clear();
		InetSocketAddress sock = (InetSocketAddress) ctx.channel().localAddress();
		seqNoFile = new File("SEQNO", "SEQNO_" + sock.getPort());
		log.debug("seqNoFile local=" + seqNoFile.getAbsolutePath());
		if (seqNoFile.exists() == false) {
			File parent = seqNoFile.getParentFile();
			if (parent.exists() == false) {
				parent.mkdirs();
			}
			try {
				seqNoFile.createNewFile();
				FileUtils.writeStringToFile(seqNoFile, "0", Charset.defaultCharset());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.seqf_map.put(ctx.channel(), seqNoFile);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.debug("channelInactive==========");
		clientMessageBuf.clear();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		log.debug("ChannelRead==============");
		byte [] telmbyteary = null;
		try {
			if (msg instanceof ByteBuf) {
				ByteBuf buf = (ByteBuf) msg;
				log.debug("capacity=" + buf.capacity() + " readableBytes=" + buf.readableBytes() + " barray="
						+ buf.hasArray() + " nio=  " + buf.nioBufferCount());
				log.debug("readableBytes={} barray={}", buf.readableBytes(), buf.hasArray());
				if (clientMessageBuf.readerIndex() > (clientMessageBuf.capacity() / 2)) {
					clientMessageBuf.discardReadBytes();
					log.debug("adjustment clientMessageBuf readerindex ={}" + clientMessageBuf.readableBytes());
				}
				if (buf.isReadable()) {
					log.debug("readable");
					int size = buf.readableBytes();
					log.debug("readableBytes={} barray={}", size, buf.hasArray());
					if (buf.isReadable() && !buf.hasArray()) {
						// it is long raw telegram
						//20200105
						log.debug("readableBytes={} barray={}", buf.readableBytes(), buf.hasArray());
						if (clientMessageBuf.readerIndex() > (clientMessageBuf.capacity() / 2)) {
							clientMessageBuf.discardReadBytes();
							log.debug("adjustment clientMessageBuf readerindex ={}" + clientMessageBuf.readableBytes());
						}
						clientMessageBuf.writeBytes(buf);
						log.debug("clientMessageBuf.readableBytes={}",clientMessageBuf.readableBytes());
/*						while (clientMessageBuf.readableBytes() >= 12) {
							byte[] lenbary = new byte[3];
							clientMessageBuf.getBytes(clientMessageBuf.readerIndex() + 3, lenbary);
							log.debug("clientMessageBuf.readableBytes={} size={}",clientMessageBuf.readableBytes(), fromByteArray(lenbary));
							if ((size = fromByteArray(lenbary)) > 0 && size <= clientMessageBuf.readableBytes()) {
								telmbyteary = new byte[size];
								clientMessageBuf.readBytes(telmbyteary);
								log.debug("read {} byte(s) from clientMessageBuf after {}", size, clientMessageBuf.readableBytes());
								getSeqStr = new String(telmbyteary, 7, 3);
								FileUtils.writeStringToFile(seqNoFile, getSeqStr, Charset.defaultCharset());
								List<String> rlist = cnvS004toR0061(telmbyteary);
								if (rlist != null && rlist.size() > 0) {
									for (String l : rlist) {
										telmbyteary = l.getBytes();
										buf = channel_.alloc().buffer().writeBytes(telmbyteary);
										publishactorSendmessage(clientId, buf);
									}
									try {
										int seqno = Integer.parseInt(
												FileUtils.readFileToString(seqNoFile, Charset.defaultCharset())) + 1;
										if (seqno > 999) {
											seqno = 0;
										}
										HostS004SndHost(seqno, verhbrno, verhwsno, curMrkttm);
										FileUtils.writeStringToFile(seqNoFile, Integer.toString(seqno), Charset.defaultCharset());
									} catch (Exception e) {
										log.warn(e.getMessage());
									}
								}

							} else
								break;
		
						}*/
					}
				} else // if
					log.warn("not ByteBuf");
			} else
				log.error("not ByteBuf message");
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object obj) throws Exception {

		if (obj instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) obj;
//			if (IdleState.WRITER_IDLE.equals(event.state())) { // 如果寫通道處於空閒狀態就發送心跳命令
			if (IdleState.READER_IDLE.equals(event.state())) { // 如果讀通道處於空閒狀態就發送心跳命令
				ctx.channel().writeAndFlush(HEARTBEAT_SEQUENCE.duplicate());
			}
		}
	}

	private int fromByteArray(byte[] bytes) {
	    int r = 0;
	    for (byte b: bytes)
	        r =  (r * 100) + ((((b >> 4)& 0xf) * 10 + (b & 0xf)));
	    return r;
	}
	private String date() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(new Date());
	}

}

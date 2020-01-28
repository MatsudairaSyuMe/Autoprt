package com.systex.sysgateii.comm.pool.fas;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * Created by MatsudairaSyume
 *  2020/01/15
 */

public class FASChannelPoolHandler implements ChannelPoolHandler {
	private static Logger log = LoggerFactory.getLogger(FASChannelPoolHandler.class);
	private ByteBuf rcvBuf = null;
	private ConcurrentHashMap<Channel, File> seqf_map = null;
	public FASChannelPoolHandler(ByteBuf rcvBuf) {
		this.rcvBuf = rcvBuf;
	}
	public FASChannelPoolHandler(ByteBuf rcvBuf, ConcurrentHashMap<Channel, File> seqfmap) {
		this.rcvBuf = rcvBuf;
		this.seqf_map = seqfmap;
	}
	public ByteBuf getRcvBuf() throws Exception {
		log.debug("getRcvBuf");
		return rcvBuf;
	}


	@Override
	public void channelAcquired(final Channel ch) throws Exception {
		// TODO Auto-generated method stub
		log.debug("channelAcquired");
	}

	@Override
	public void channelCreated(final Channel ch) throws Exception {
		// TODO Auto-generated method stub
		log.debug("channelCreated");
		SocketChannel channel = (SocketChannel) ch;
		channel.config().setKeepAlive(true);
		channel.config().setTcpNoDelay(true);
		channel.config().setReuseAddress(true);
//		channel.pipeline().addLast("log", new LoggingHandler(FASClientChannelHandler.class, LogLevel.INFO))
//				.addLast(new IdleStateHandler(4, 0, 0, TimeUnit.SECONDS)).addLast(new FASClientChannelHandler());
		channel.pipeline().addLast("log", new LoggingHandler(FASClientChannelHandler.class, LogLevel.INFO))
		.addLast(new FASClientChannelHandler(rcvBuf, seqf_map));
	}

	@Override
	public void channelReleased(final Channel ch) throws Exception {
		// TODO Auto-generated method stub
		log.debug("channelReleased");
	}

}

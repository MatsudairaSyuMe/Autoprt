package com.systex.sysgateii.gateway.util;

import java.net.Socket;
import java.net.SocketImpl;
import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class SocketTool {
	public static int getUnixFileDescriptor(Socket ss) {
		int rtn = -1;
		try {
			Field $impl = ss.getClass().getDeclaredField("impl");
			$impl.setAccessible(true);
			SocketImpl socketImpl = (SocketImpl) $impl.get(ss);
			Method $getFileDescriptor = SocketImpl.class.getDeclaredMethod("getFileDescriptor");
			$getFileDescriptor.setAccessible(true);
			FileDescriptor fd = (FileDescriptor) $getFileDescriptor.invoke(socketImpl);
			Field $fd = fd.getClass().getDeclaredField("fd");
			$fd.setAccessible(true);
			return (Integer) $fd.get(fd);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rtn;
	}
}

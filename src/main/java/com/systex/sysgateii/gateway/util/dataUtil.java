package com.systex.sysgateii.gateway.util;

public class dataUtil {
	public int ArraySearchIndexOf(final byte[] outerArray, final byte[] smallerArray) {
		for (int i = 0; i < outerArray.length - smallerArray.length + 1; ++i) {
			boolean found = true;
			for (int j = 0; j < smallerArray.length; ++j) {
				if (outerArray[i + j] != smallerArray[j]) {
					found = false;
					break;
				}
			}
			if (found)
				return i;
		}
		return -1;
	}

	public static int fromByteArray(byte[] bytes) {
		int r = 0;
		for (byte b : bytes)
			r = (r * 100) + ((((b >> 4) & 0xf) * 10 + (b & 0xf)));
		return r;
	}

	public static byte[] to3ByteArray(int l) {
		byte[] rtn = new byte[3];
		int tl = l;
		byte b1 = (byte) 0x0;
		byte b2 = (byte) 0x0;
		for (int i = rtn.length - 1; i >= 0; --i) {
			b1 = (byte) (tl % 10);
			tl = tl / 10;
			b2 = (byte) (tl % 10);
			tl = tl / 10;
			rtn[i] = (byte) (((b2 << 4) & 0xf0) | (b1 & 0xf));
		}
		return rtn;
	}

}

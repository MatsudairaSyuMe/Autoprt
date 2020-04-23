package com.systex.sysgateii.gateway.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import static java.lang.System.out;

/*
 * Convert tool between UTF8 and BIG5
 * MatsudairaSyuMe
 * 
 * Demonstrate default Charset-related details.
 * 20190902
 * 
 */
public class CharsetCnv {
	private static final Charset BIG5 = Charset.forName("BIG5");
	private static final Charset UTF8 = Charset.forName("UTF-8");

	/**
	 * Supplies the default encoding without using Charset.defaultCharset() and
	 * without accessing System.getProperty("file.encoding").
	 *
	 * @return Default encoding (default charset).
	 */
	public static String getEncoding() {
		final byte[] bytes = { 'D' };
		final InputStream inputStream = new ByteArrayInputStream(bytes);
		final InputStreamReader reader = new InputStreamReader(inputStream);
		final String encoding = reader.getEncoding();
		return encoding;
	}

	public String BIG5UTF8str(String big5input) throws Exception {
		if (big5input == null)
			throw new Exception("input data buffer null");
		byte[] utf8Encoded = null;
		byte[] big5Encoded = big5input.getBytes(BIG5);
		String decoded = new String(big5Encoded, BIG5);
		utf8Encoded = decoded.getBytes(UTF8);
		return new String(utf8Encoded, UTF8);
	}

	public String BIG5bytesUTF8str(byte[] big5input) throws Exception {
		if (big5input == null || big5input.length == 0)
			throw new Exception("input data buffer null");
		byte[] utf8Encoded = null;
		byte[] big5Encoded = big5input;
		String decoded = new String(big5Encoded, BIG5);
		utf8Encoded = decoded.getBytes(UTF8);
		return new String(utf8Encoded, UTF8);
	}

	public byte[] BIG5UTF8bytes(String big5input) throws Exception {
		if (big5input == null)
			throw new Exception("input data buffer null");
		byte[] utf8Encoded = null;
		byte[] big5Encoded = big5input.getBytes(BIG5);
		String decoded = new String(big5Encoded, BIG5);
		utf8Encoded = decoded.getBytes(UTF8);
		return utf8Encoded;
	}

	public String UTF8BIG5str(String utfinput) throws Exception {
		if (utfinput == null)
			throw new Exception("input data buffer null");
		byte[] big5Encoded = null;
//        byte[] utf8Encoded = utfinput.getBytes(UTF8); 
		String decoded = utfinput;
		big5Encoded = decoded.getBytes(BIG5);
		return new String(big5Encoded);
	}

	public String UTF8bytesBIG5str(byte[] utfinput) throws Exception {
		if (utfinput == null || utfinput.length == 0)
			throw new Exception("input data buffer null");
		byte[] big5Encoded = null;
		String decoded = new String(utfinput, UTF8);
		big5Encoded = decoded.getBytes(BIG5);
		return new String(big5Encoded, BIG5);
	}

	public byte[] UTF8BIG5bytes(String utfinput) throws Exception {
		if (utfinput == null)
			throw new Exception("input data buffer null");
		byte[] big5Encoded = null;
		byte[] utf8Encoded = utfinput.getBytes(UTF8);
		String decoded = new String(utf8Encoded, UTF8);
		big5Encoded = decoded.getBytes(BIG5);
		return big5Encoded;
	}

	public static void main(final String[] arguments) {
		CharsetCnv cc = new CharsetCnv();
		CharsetCnv.getEncoding();
		out.println("Default Encoding: " + CharsetCnv.getEncoding());
		try {
			String src = "AB 摩根太證息 12AB";
			out.println("UTF8 " + src);
			byte[] srcb = src.getBytes();
			for (int i = 0; i < srcb.length; i++)
				out.println(String.format("%02x ", srcb[i] & 0xff));
			/*
			 * String r = cc.UTF8BIG5str(src); out.println("摩根太證息 Encoding UTF8BIG5str: " +
			 * r);
			 */
			byte[] b = cc.UTF8BIG5bytes(src);
			out.println("UTF8 摩根太證息 Encoding BIG5UTF8bytes: ");
			for (int i = 0; i < b.length; i++)
				out.println(String.format("%02x ", b[i] & 0xff));
			/*
			 * out.println("big5 摩根太證息 Encoding BIG5UTF8str: " + cc.BIG5UTF8str(r));
			 */
			out.println("摩根太證息 Encoding bytes: " + new String(b));
			out.println("big5 摩根太證息 Encoding BIG5bytesUTF8str: " + cc.BIG5bytesUTF8str(b));
		} catch (Exception e) {
			System.err.println(e);
		}
	}
}

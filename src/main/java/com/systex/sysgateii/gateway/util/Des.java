package com.systex.sysgateii.gateway.util;

import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.util.Base64;

/**
 * DES 小工具
 * 20210122 MatsudairaSyuMe
 */

public class Des {
	public static final String ALGORITHM_DES = "DES/CBC/PKCS5Padding";
	public static final Base64.Decoder decoder = Base64.getDecoder();
	public static final Base64.Encoder encoder = Base64.getEncoder();

	/**
	 * DES演算法，加密
	 *
	 * @param data 待加密字串
	 * @param key  加密私鑰，長度不能夠小於8位
	 * @return 加密後的位元組陣列，一般結合Base64編碼使用
	 * @throws CryptException 異常
	 */
	public static String encode(String key, String data) throws Exception {
		return encode(key, data.getBytes());
	}

	/**
	 * DES演算法，加密
	 *
	 * @param data 待加密字串
	 * @param key  加密私鑰，長度不能夠小於8位
	 * @return 加密後的位元組陣列，一般結合Base64編碼使用
	 * @throws CryptException 異常
	 */
	public static String encode(String key, byte[] data) throws Exception {
		try {
			DESKeySpec dks = new DESKeySpec(key.getBytes());

			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			// key的長度不能夠小於8位位元組
			Key secretKey = keyFactory.generateSecret(dks);
			Cipher cipher = Cipher.getInstance(ALGORITHM_DES);
			IvParameterSpec iv = new IvParameterSpec("********".getBytes());
			AlgorithmParameterSpec paramSpec = iv;
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec);

			byte[] bytes = cipher.doFinal(data);
			return encoder.encodeToString(bytes);
		} catch (Exception e) {
			throw new Exception(e);
		}
	}

	/**
	 * DES演算法，解密
	 *
	 * @param data 待解密字串
	 * @param key  解密私鑰，長度不能夠小於8位
	 * @return 解密後的位元組陣列
	 * @throws Exception 異常
	 */
	public static byte[] decode(String key, byte[] data) throws Exception {
		try {
			SecureRandom sr = new SecureRandom();
			DESKeySpec dks = new DESKeySpec(key.getBytes());
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			// key的長度不能夠小於8位位元組
			Key secretKey = keyFactory.generateSecret(dks);
			Cipher cipher = Cipher.getInstance(ALGORITHM_DES);
			IvParameterSpec iv = new IvParameterSpec("********".getBytes());
			AlgorithmParameterSpec paramSpec = iv;
			cipher.init(Cipher.DECRYPT_MODE, secretKey, paramSpec);
			return cipher.doFinal(data);
		} catch (Exception e) {
//         e.printStackTrace();
			throw new Exception(e);
		}
	}

	/**
	 * 獲取編碼後的值
	 * 
	 * @param key
	 * @param data
	 * @return
	 * @throws Exception
	 * @throws Exception
	 */
	public static String decodeValue(String key, String data) throws Exception {
		byte[] datas;
		String value = null;

		datas = decode(key, decoder.decode(data));

		value = new String(datas);
		if (value.equals("")) {
			throw new Exception();
		}
		return value;
	}

	public static void main(String[] args) throws Exception {
		// 待加密內容
		String str = "加密內容";
		// 密碼,長度要是8的倍數
		String password = "9588028820109132570743325311898426347857298773549468758875018579537757772163084478873699447306034466200616411960574122434059469100235892702736860872901247123456";

		String result = Des.encode(password, str);
		System.out.println("加密後:" + result);

		// 直接將如上內容解密
		try {
			String decryResult = Des.decodeValue(password, result);
			System.out.println("解密後:" + decryResult);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
}

package com.systex.sysgateii.gateway.util;

import java.security.spec.KeySpec;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;
import java.util.Base64;


/**
 * DES 小工具
 * 20210122 MatsudairaSyuMe
 * change to AES256 Algorithm
 */

public class Des {
//	public static final String ALGORITHM_DES = "DES/CBC/PKCS5Padding";
	private static String salt = "ssshhhhhhhhhhh!!!!";
	public static final String ALGORITHM_AES256 = "PBKDF2WithHmacSHA256";
	public static final String ALGORITHM_AES = "AES";
	public static final String ALGORITHM_AESCBCPAD = "AES/CTR/PKCS5PADDING";

	/**
	 * DES演算法，加密
	 *
	 * @param strToEncrypt 待加密字串
	 * @param secret  加密私鑰
	 * @return 加密後的位元組陣列，一般結合Base64編碼使用
	 * @throws CryptException 異常
	 */
	public static String encode(String secret, String strToEncrypt) throws Exception {
		try {
			byte[] iv = { 0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x7, 0x6, 0x5, 0x4, 0x3, 0x2, 0x1, 0 };
			IvParameterSpec ivspec = new IvParameterSpec(iv);

			SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM_AES256);
			KeySpec spec = new PBEKeySpec(secret.toCharArray(), salt.getBytes(), 65536, 256);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKeySpec secretKeySpec = new SecretKeySpec(tmp.getEncoded(), ALGORITHM_AES);

			Cipher cipher = Cipher.getInstance(ALGORITHM_AESCBCPAD);
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivspec);
			return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
		} catch (Exception e) {
			System.out.println("Error while encrypting: " + e.toString());
		}
		return null;
	}


	/**
	 * 獲取編碼後的值
	 * 
	 * @param secret
	 * @param strToDecrypt
	 * @return
	 * @throws Exception
	 * @throws Exception
	 */
	public static String decodeValue(String secret, String strToDecrypt) throws Exception {
		try {
			byte[] iv = { 0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x7, 0x6, 0x5, 0x4, 0x3, 0x2, 0x1, 0 };
			IvParameterSpec ivspec = new IvParameterSpec(iv);

			SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM_AES256);
			KeySpec spec = new PBEKeySpec(secret.toCharArray(), salt.getBytes(), 65536, 256);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKeySpec secretKeySpec = new SecretKeySpec(tmp.getEncoded(), ALGORITHM_AES);

			Cipher cipher = Cipher.getInstance(ALGORITHM_AESCBCPAD);
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivspec);
			return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
		} catch (Exception e) {
			System.out.println("Error while decrypting: " + e.toString());
		}
		return null;
	}
	//20210202 MatsudairaSyuMe
	//cut out main
	//----
}

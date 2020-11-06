package com.systex.sysgateii.gateway.conf;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.systex.sysgateii.gateway.autoPrtSvr.Server.PrnSvr;

public class DscptMappingTable {
	private static Logger log = LoggerFactory.getLogger(DscptMappingTable.class);
	private String defname = "DMTABLE.INI";
	public static ConcurrentHashMap<String, String> m_Dscpt = new ConcurrentHashMap<String, String>();
	public static ConcurrentHashMap<String, byte[]> m_Dscpt2 = new ConcurrentHashMap<String, byte[]>();

	public DscptMappingTable () {
//20200916		new DscptMappingTable(this.defname);
		String JDBC_DRIVER = "com.ibm.db2.jcc.DB2Driver";
		String DB_URL = PrnSvr.dburl;
		String USER = PrnSvr.dbuser;
		String PASS = PrnSvr.dbpass;
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		m_Dscpt.clear();
		int total = 0;
		String id = "";
		byte[] flddm = null;
		byte[] dm = null;
		try {
			conn = DriverManager.getConnection(DB_URL, USER, PASS);
			log.debug("Connected database successfully...");
			// STEP 2: Register JDBC driver
			Class.forName(JDBC_DRIVER);
			// STEP 3: Open a connection

			log.debug("Connecting to a selected database...");
			// STEP 4: Execute a Select
			log.debug("select records from the table...");
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE,
					ResultSet.CLOSE_CURSORS_AT_COMMIT);
//			rs = ((Statement) stmt).executeQuery("SELECT ID, NAME FROM BISAP.TB_AUDMPRM");
			rs = ((Statement) stmt).executeQuery("SELECT ID, NAME FROM " + PrnSvr.dmtbname);
			while (rs.next()) {
				id = rs.getString("ID");
				flddm = rs.getBytes("NAME");
				if (flddm != null && flddm.length > 0) {
					int sz = 0;
					for (int i = flddm.length - 1; i >= 0; i--)
						if ((byte) flddm[i] == (byte) 0x0)
							sz += 1;
					if (sz > 0) {
						dm = new byte[flddm.length - sz];
						System.arraycopy(flddm, 0, dm, 0, (flddm.length - sz));
					} else
						dm = flddm;
				} else
					dm = flddm;
				m_Dscpt2.put(id.trim(), dm);
				total++;
			}
			if (conn != null)
				conn.close();
			log.debug("total " + total + " records");
			log.debug("total m_Dscpt2 " + m_Dscpt2.size() + " records");
		} catch (Exception e) {
			e.getStackTrace();
			log.error("ERROR!! " + e.getMessage());
			try {
				if (conn != null)
					conn.close();
			} catch (Exception e2) {
				e.getStackTrace();
				log.error("connect close ERROR!! " + e2.getMessage());
			}
		}
	}
	public DscptMappingTable (String filename) {
		BufferedReader reader;
		int total = 0;
		m_Dscpt.clear();
		try{
			File file = new File(filename);
			byte[] bytesArray = new byte[(int) file.length()];
			FileInputStream fis = new FileInputStream(file);
			fis.read(bytesArray); //read file into bytes[]
			fis.close();
			boolean pstart = false;
			int catchlen = 0;
			byte[] tmph = new byte[80];
			Arrays.fill(tmph, (byte)0x0);
			for (int i = 0; i < bytesArray.length; i++) {
//				System.out.print(String.format("%c %x ", (char)(bytesArray[i] & 0xff), bytesArray[i]));
				if (bytesArray[i] == (byte)'#')
					pstart = true;
				if (bytesArray[i] == (byte)0x0d && (i < (bytesArray.length - 1) && (bytesArray[i + 1] == (byte)0x0a))) {
					if (!pstart) {
						for (int j = 0; j < catchlen; j++) {
							if (tmph[j] == (byte)'=') {
								byte[] tmpb = new byte[catchlen - j - 1];
								System.arraycopy(tmph, j + 1, tmpb, 0, catchlen - j - 1);
								int start = -1;    //fill header space bytes
								for (int k = 0; k < tmpb.length; k++)
									if ((int)(tmpb[k] & 0xff) > (int)' ') {
										start = k;
										break;
									}
								int mark = -1;   //fill tail space bytes
								if ((start + 1) < tmpb.length) {
									for (int k = start + 1; k < tmpb.length; k++) {
										if ((int)(tmpb[k] & 0xff) > (int)0x7f && ((k + 1) < tmpb.length)) {
											k += 1;
											mark = -1;
										} else if ((int)(tmpb[k] & 0xff) == (int)' ') {
											if ((mark == -1) || ((mark + 1) != k))
												mark = k;
										} else
											mark = -1;
									}
								}
								byte[] tmpb2 = null;
								if (mark > -1)
									tmpb2 = new byte[mark - start + 1];
								else
									tmpb2 = new byte[tmpb.length - start];
								System.arraycopy(tmpb, start, tmpb2, 0, tmpb2.length);
								m_Dscpt2.put(new String(tmph, 0, j).trim(), tmpb2);
//								System.out.println("len=" + tmpb2.length + " :" + new String(tmpb2));
								break;
							}
						}
						catchlen = 0;
					}
					pstart = false;
					Arrays.fill(tmph, (byte)0x0);
					i += 1;
				} else {
					if (!pstart) {
						tmph[catchlen] = bytesArray[i];
						catchlen += 1;
					}
				}
			}
			log.debug("total m_Dscpt2 {} records", m_Dscpt2.size());
			InputStreamReader isr = new InputStreamReader(new FileInputStream(filename), "Big5");
			reader = new BufferedReader(isr);
			String line = reader.readLine();
			while (line != null) {
				line = line.trim();
				if (line.length() > 0 && !line.substring(0, 1).equals("#")) {
					String[] sar = line.split("=");
//					log.debug("item=[" + sar[0].trim() + "] desc=[" + sar[1].trim());
					m_Dscpt.put(sar[0].trim(), sar[1].trim());
					total += 1;
				}
				line = "";
				line = reader.readLine();
			// read next line
			}
			reader.close();
		} catch (Exception e) {
			e.getStackTrace();
			log.error("ERROR!! {}",e.getMessage());
		}
      log.debug("total {} records", total);
	}
	public static void main(String[] args) {
		//pstmt.setObject(1, bserviceID, java.sql.Types.BINARY); 
		DscptMappingTable d = new DscptMappingTable ("DMTABLE.INI");
		System.out.println(d.m_Dscpt.size());
		System.out.println(d.m_Dscpt.get("000E6"));
		System.out.println(new String(d.m_Dscpt2.get("000E6")));
		System.out.println(Arrays.toString(d.m_Dscpt2.get("M66")) + ": len=" + d.m_Dscpt2.get("M66").length + " :" + new String(d.m_Dscpt2.get("M66")));
	}
}

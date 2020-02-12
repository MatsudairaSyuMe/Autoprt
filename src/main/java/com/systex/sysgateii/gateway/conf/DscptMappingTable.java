package com.systex.sysgateii.gateway.conf;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DscptMappingTable {
	private static Logger log = LoggerFactory.getLogger(DscptMappingTable.class);
	private String defname = "DMTABLE.INI";
	public static ConcurrentHashMap<String, String> m_Dscpt = new ConcurrentHashMap<String, String>();

	public DscptMappingTable () {
		new DscptMappingTable(this.defname);
	}
	public DscptMappingTable (String filename) {
		BufferedReader reader;
		int total = 0;
		m_Dscpt.clear();
		try{
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
		DscptMappingTable d = new DscptMappingTable ("DMTABLE.INI");
		System.out.println(d.m_Dscpt.size());
		System.out.println(d.m_Dscpt.get("000E6"));
	}
}

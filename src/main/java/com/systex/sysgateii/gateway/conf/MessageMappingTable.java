package com.systex.sysgateii.gateway.conf;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageMappingTable {
	private static Logger log = LoggerFactory.getLogger(MessageMappingTable.class);
	private String defname = "MESSAGE.INI";
	public static ConcurrentHashMap<String, String> m_Message = new ConcurrentHashMap<String, String>();

	public MessageMappingTable() {
		new MessageMappingTable(this.defname);
	}

	public MessageMappingTable(String filename) {
		BufferedReader reader;
		int total = 0;
		m_Message.clear();
		try {
			InputStreamReader isr = new InputStreamReader(new FileInputStream(filename));
			reader = new BufferedReader(isr);
			String line = reader.readLine();
			while (line != null) {
				line = line.trim();
				if (line.length() > 0 && !line.substring(0, 1).equals("#") && line.contains("=")) {
					if (line.lastIndexOf('=') != (line.length() - 1)) {
						String[] sar = line.split("=");
//						log.debug("item=[" + sar[0].trim() + "] message=[" + sar[1].trim());
						m_Message.put(sar[0].trim(), sar[1].trim());
					} else {
						m_Message.put(line.substring(0, line.lastIndexOf('=')), "");
					}
					total += 1;
				}
				line = "";
				line = reader.readLine();
				// read next line
			}
			reader.close();
		} catch (Exception e) {
			e.getStackTrace();
			log.error("ERROR!! {}", e.getMessage());
		}
		log.debug("total {} records", total);
	}

	public static void main(String[] args) {
		MessageMappingTable d = new MessageMappingTable("MESSAGE.INI");
		System.out.println(d.m_Message.size());
		System.out.println(d.m_Message.get("E104"));
		System.out.println(d.m_Message.get("A104"));
	}

}

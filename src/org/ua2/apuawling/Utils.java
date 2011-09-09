package org.ua2.apuawling;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

public class Utils {
	
	private static final Logger logger = Logger.getLogger(Utils.class);
	
	public static String toString(String[] list) {
		StringBuilder sb = new StringBuilder("[");
		for(String item : list) {
			sb.append(" ").append(item);
		}
		sb.append(" ]");
		return sb.toString();
	}


	public static byte[] getAddressBytes(String addrStr) {
		logger.info("Creating proxy address for " + addrStr);
		byte[] addr = new byte[4];
		if(Character.isDefined(addrStr.charAt(0))) {
			String[] octets = addrStr.split("\\.");
			for(int digit = 0; digit <= 3; digit++) {
				int octetVal = Integer.parseInt(octets[digit]);
				addr[digit] = (byte)octetVal;
			}
		}
		return addr;
	}
	
	public static void appendFile(String filename, boolean pre, StringBuilder sb) {
		try {
			InputStream fileStream = Utils.class.getClassLoader().getResourceAsStream(filename);
			if(fileStream == null) {
				return;
			}

			BufferedReader fileReader = new BufferedReader(new InputStreamReader(fileStream));

			sb.append("<hr>\n");

			if(pre) {
				sb.append("<pre>\n");
			}

			String line = null;
			while((line = fileReader.readLine()) != null) {
				sb.append(line);
				if(pre) {
					sb.append("\n");
				} else {
					sb.append("<br>\n");
				}
			}

			if(pre) {
				sb.append("</pre>\n");
			} else {
				sb.append("<br>\n");
			}

			fileStream.close();
		} catch(Exception e) {
			logger.error("Cannot load " + filename, e);
		}
	}
}

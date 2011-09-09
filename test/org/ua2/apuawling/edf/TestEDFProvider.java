package org.ua2.apuawling.edf;

import org.ua2.edf.EDFData;

public class TestEDFProvider {
	public static void main(String[] args) {
		try {
			EDFProvider provider = new EDFProvider("ua2.org", 2020, "crackpot", "mad", null, null);
			EDFData request = new EDFData("request", "system_list");
			String banner = provider.sendAndRead(request).getChild("banner").getString();
			System.out.println("Banner:\n" + banner);
			provider.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}

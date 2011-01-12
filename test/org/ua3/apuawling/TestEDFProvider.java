package org.ua3.apuawling;

import org.ua3.apuawling.edf.EDFProvider;

public class TestEDFProvider {
	public static void main(String[] args) {
		try {
			EDFProvider provider = new EDFProvider("ua2.org", 2020, "crackpot", "mad", null, null);
			String banner = provider.getSystem().getString("banner");
			System.out.println("Banner:\n" + banner);
			provider.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}

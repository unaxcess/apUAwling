package org.ua2.apuawling;

import org.ua2.clientlib.UA;
import org.ua2.clientlib.UASession;

public class TestEDFClient {
	public static void main(String[] args) {
		try {
			UA ua = new UA();
			UASession session = new UASession(ua);

			session.connect("ua2.org", 2020);
			if(session.login("crackpot", "mad")) {
				String banner = session.loginBanner();
				System.out.println("UA banner:\n" + banner);
			} else {
				System.out.println("Login failed");
			}

			session.logout();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}

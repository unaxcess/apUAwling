package org.ua2.apuawling;

import org.json.JSONObject;

public class TestJSON {
	public static void main(String[] args) {
		try {
			JSONObject response = new JSONObject();

			response.put("id", "105");
			response.put("subject", "foo");
			response.put("body", "bar");

			response.append("inReplyTo", "101");
			response.append("inReplyTo", "102");
			response.append("inReplyTo", "103");
			response.append("inReplyTo", "104");

			System.out.println("Message response:\n" + response);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}

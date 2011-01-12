package org.json;

import junit.framework.TestCase;

public class TestParsing extends TestCase {
	
	private static final String OBJECT_STR = "{ \"first\":\"one\", \"second\":2, \"third\":true }";
	private static final String ARRAY_STR = "[ { \"name\":\"one\" }, { \"name\":\"two\" }, { \"name\":\"three\" } ]";
	
	public void testObjectAsObject() throws JSONException {
		JSONObject obj = new JSONObject(OBJECT_STR);
		
		assertEquals(obj.getString("first"), "one");
		assertEquals(obj.getInt("second"), 2);
		assertEquals(obj.getBoolean("third"), true);
	}
	
	public void testArrayAsArray() throws JSONException {
		JSONArray array = new JSONArray(ARRAY_STR);
		
		assertEquals(array.getJSONObject(0).getString("name"), "one");
		assertEquals(array.getJSONObject(1).getString("name"), "two");
		assertEquals(array.getJSONObject(2).getString("name"), "three");
	}

	public void testObjectAsArray() {
		try {
			new JSONObject(ARRAY_STR);
			fail("Should not reach this point");
		} catch(JSONException e) {
		}
	}
	
	public void testArrayAsObject() {
		try {
			new JSONArray(OBJECT_STR);
			fail("Should not reach this point");
		} catch(JSONException e) {
		}
	}

	public void testSomeOtherStuff() throws JSONException {
		JSONObject obj = new JSONObject("{ \"response\": [{\"folder\":\"badger\"},{\"folder\":\"minge\"}]}");

		assertEquals(obj.getJSONArray("response").getJSONObject(0).get("folder"), "badger");
		assertEquals(obj.getJSONArray("response").getJSONObject(1).get("folder"), "minge");
	}
}

package org.ua2.apuawling;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ua2.apuawling.JSONWrapper;

public class TestJSONWrapper extends TestCase {
	
	public void testParse() throws JSONException {
		JSONWrapper obj1 = JSONWrapper.parse("[{\"folder\":\"badger\"},{\"folder\":\"minge\"}]");
		assertEquals("badger", ((JSONObject)obj1.getArray().get(0)).getString("folder"));
		assertEquals("minge", ((JSONObject)obj1.getArray().get(1)).getString("folder"));
		
		JSONWrapper obj2 = JSONWrapper.parse("{ \"response\": [{\"folder\":\"badger\"},{\"folder\":\"minge\"}]}");
		JSONArray array = (JSONArray)obj2.getObject().get("response");
		assertEquals("badger", ((JSONObject)array.get(0)).getString("folder"));
		assertEquals("minge", ((JSONObject)array.get(1)).getString("folder"));
	}
}

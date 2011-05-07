package org.ua2.apuawling;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ua2.json.JSONWrapper;

public class TestJSONWrapper extends TestCase {
	
	public void testParse() throws JSONException {
		JSONWrapper obj1 = JSONWrapper.parse("[{\"folder\":\"badger\"},{\"folder\":\"minge\"}]");
		assertEquals("badger", ((JSONObject)obj1.getArray().get(0)).getString("folder"));
		assertEquals("minge", ((JSONObject)obj1.getArray().get(1)).getString("folder"));
		
		JSONWrapper obj2 = JSONWrapper.parse("{ \"response\": [{\"folder\":\"badger\"},{\"folder\":\"minge\"}]}");
		JSONArray array2 = (JSONArray)obj2.getObject().get("response");
		assertEquals("badger", ((JSONObject)array2.get(0)).getString("folder"));
		assertEquals("minge", ((JSONObject)array2.get(1)).getString("folder"));
		
		JSONWrapper obj3 = JSONWrapper.parse("[1,2,3]   ");
		JSONArray array3 = (JSONArray)obj3.getArray();
		assertEquals(1, array3.getInt(0));
		assertEquals(2, array3.getInt(1));
		assertEquals(3, array3.getInt(2));
	}
}

package org.ua3.apuawling;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONWrapper {
	private JSONObject object;
	private JSONArray array;
	
	public static JSONWrapper parse(String str) throws JSONException {
		if(str == null) {
			return null;
		}
		
		for(int pos = 0; pos < str.length(); pos++) {
			if(str.charAt(pos) == '{') {
				return new JSONWrapper(new JSONObject(str));
			} else if(str.charAt(pos) == '[') {
				return new JSONWrapper(new JSONArray(str));
			}
		}
		
		return null;
	}
	
	private JSONWrapper(JSONObject object) {
		this.object = object;
	}
	
	private JSONWrapper(JSONArray array) {
		this.array = array;
	}
	
	public boolean isObject() {
		return object != null;
	}
	
	public boolean isArray() {
		return array != null;
	}
	
	public JSONObject getObject() {
		if(object == null) {
			throw new ClassCastException("Wrapped data is not an object");
		}
		
		return object;
	}
	
	public JSONArray getArray() {
		if(array == null) {
			throw new ClassCastException("Wrapped data is not an array");
		}
		
		return array;
	}
	
	public String toString() {
		try {
			if(array != null) {
				return array.toString(2);
			} else if(object != null) {
				return object.toString(2);
			}
			
			return null;
		} catch(JSONException e) {
			throw new RuntimeException("Cannot convert to string", e);
		}
	}
}

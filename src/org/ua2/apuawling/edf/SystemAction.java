package org.ua2.apuawling.edf;

import java.util.List;

import org.json.JSONObject;
import org.ua2.apuawling.ActionException;
import org.ua2.apuawling.ProviderException;
import org.ua2.edf.EDFData;
import org.ua2.json.JSONWrapper;

public class SystemAction extends EDFAction<JSONObject> {

	public SystemAction(EDFProvider provider) {
		super(provider, Method.GET, "system");
	}

	@Override
	public JSONObject perform(List<String> elements, JSONWrapper data) throws ActionException, ProviderException {
		JSONObject response = null;
		
		try {
			EDFData request = new EDFData("request", "system_list");

			EDFData reply = sendAndRead(request);

			response = new JSONObject();
			
			response.put("banner", reply.getChild("banner").getString());
		} catch(Exception e) {
			handleException("Cannot get system", e);
		}
		
		return response;
	}
}

package org.ua2.apuawling.edf;

import java.util.List;

import org.json.JSONObject;
import org.ua2.apuawling.ActionException;
import org.ua2.apuawling.ProviderException;
import org.ua2.edf.EDFData;
import org.ua2.json.JSONWrapper;

public class GetUsersAction extends EDFAction<JSONObject> {

	public GetUsersAction(EDFProvider provider) {
		super(provider, Method.GET, "users(/(online))?");
	}

	@Override
	public JSONObject perform(List<String> elements, JSONWrapper data) throws ActionException, ProviderException {
		JSONObject response = null;
		
		try {
			EDFData request = new EDFData("request", "user_list");
			
			request.add("searchtype", 4);

			EDFData reply = sendAndRead(request);

			EDFData child = reply.getChild("user");

			response = new JSONObject();
		
			copyChild(child, "name", response);
		} catch(Exception e) {
			handleException("Cannot get user", e);
		}
		
		return response;
	}
}

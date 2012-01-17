package org.ua2.apuawling.edf;

import java.util.List;

import org.json.JSONArray;
import org.ua2.apuawling.ActionException;
import org.ua2.apuawling.ProviderException;
import org.ua2.edf.EDFData;
import org.ua2.json.JSONWrapper;

public class GetMessagesAction extends EDFAction<JSONArray> {
	
	private enum Filter {
		FULL,
		SAVED,
		;
		
		public String toString() {
			return name().toLowerCase();
		}
	}
	
	public GetMessagesAction(EDFProvider provider) {
		super(provider, Method.GET, "messages(/(" + Filter.FULL + "|" + Filter.SAVED + ")){1,2}");
	}

	@Override
	public EDFActionWrapper<JSONArray> perform(List<String> parameters, JSONWrapper data) throws ActionException, ProviderException {
		boolean full = false;
		boolean saved = false;
		
		for(int parameterNum = 1; parameterNum < parameters.size(); parameterNum++) {
			if(Filter.FULL == Filter.valueOf(parameters.get(parameterNum).toUpperCase())) {
				full = true;
			} else if(Filter.SAVED == Filter.valueOf(parameters.get(parameterNum).toUpperCase())) {
				saved = true;
			}
		}

		JSONArray response = null;
		EDFData request = null;
		EDFData reply = null;

		try {
			request = new EDFData("request", "message_list");

			if(saved) {
				request.add("saved", 1);
			}
			request.add("searchtype", 1);

			reply = sendAndRead(request);

			response = addMessagesToList(reply, null, false, full);
		} catch(Exception e) {
			handleException("Cannot get messages", e);
		}

		return new EDFActionWrapper<JSONArray>(response, request, reply);
	}
}

package org.ua2.apuawling.edf;

import java.util.List;

import org.json.JSONArray;
import org.ua2.apuawling.ActionException;
import org.ua2.apuawling.ProviderException;
import org.ua2.edf.EDFData;
import org.ua2.json.JSONWrapper;

public class GetThreadAction extends EDFAction<JSONArray> {
	
	private enum Filter {
		FULL,
		UNREAD,
		;
		
		public String toString() {
			return name().toLowerCase();
		}
	}
	
	public GetThreadAction(EDFProvider provider) {
		super(provider, Method.GET, "thread/" + ID_PATTERN + "(/(" + Filter.FULL + "|" + Filter.UNREAD + ")){0,2}");
	}

	@Override
	public EDFActionWrapper<JSONArray> perform(List<String> parameters, JSONWrapper data) throws ActionException, ProviderException {
		int id = 0;
		try {
			id = Integer.parseInt(parameters.get(1));
		} catch(NumberFormatException e) {
			throw new ActionException("Thread ID " + parameters.get(1) + " must be numeric");
		}

		boolean full = false;
		boolean unreadOnly = false;
		
		for(int parameterNum = 2; parameterNum < parameters.size(); parameterNum++) {
			if(Filter.FULL == Filter.valueOf(parameters.get(parameterNum).toUpperCase())) {
				full = true;
			} else if(Filter.UNREAD == Filter.valueOf(parameters.get(parameterNum).toUpperCase())) {
				unreadOnly = true;
			}
		}

		JSONArray response = null;
		EDFData request = null;
		EDFData reply = null;

		try {
			request = new EDFData("request", "message_list");

			request.add("threadid", id);
			request.add("searchtype", 1);
			
			reply = sendAndRead(request);

			response = addMessagesToList(reply, null, unreadOnly, full);
		} catch(Exception e) {
			handleException("Cannot get messages in thread " + id, e);
		}

		return new EDFActionWrapper<JSONArray>(response, request, reply);
	}
}

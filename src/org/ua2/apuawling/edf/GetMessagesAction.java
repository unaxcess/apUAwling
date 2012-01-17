package org.ua2.apuawling.edf;

import java.util.List;

import org.json.JSONArray;
import org.ua2.apuawling.ActionException;
import org.ua2.apuawling.ObjectNotFoundException;
import org.ua2.apuawling.ProviderException;
import org.ua2.edf.EDFData;
import org.ua2.json.JSONWrapper;

public class GetMessagesAction extends EDFAction<JSONArray> {
	
	private enum Filter {
		FULL,
		UNREAD,
		;
		
		public String toString() {
			return name().toLowerCase();
		}
	}
	
	public GetMessagesAction(EDFProvider provider) {
		super(provider, Method.GET, "folder/" + NAME_PATTERN + "(/(" + Filter.FULL + "|" + Filter.UNREAD + ")){0,2}");
	}

	@Override
	public EDFActionWrapper<JSONArray> perform(List<String> parameters, JSONWrapper data) throws ActionException, ProviderException {
		String name = parameters.get(1);
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
			int id = getFolderId(name);
			if(id == -1) {
				throw new ObjectNotFoundException("Folder " + name + " does not exist");
			}
			
			request = new EDFData("request", "message_list");

			request.add("folderid", id);
			request.add("searchtype", 1);
			
			if(name.equalsIgnoreCase("private")) {
				EDFData search = new EDFData("or");
				search.add("fromid", getUserId());
				search.add("toid", getUserId());
				request.add(search);
			}

			reply = sendAndRead(request);
			
			name = getChildStr(reply, "foldername");

			response = addMessagesToList(reply, name, unreadOnly, full);
		} catch(Exception e) {
			handleException("Cannot get messages in folder " + name, e);
		}

		return new EDFActionWrapper<JSONArray>(response, request, reply);
	}
}

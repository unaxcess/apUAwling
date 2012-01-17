package org.ua2.apuawling.edf;

import java.util.List;

import org.json.JSONObject;
import org.ua2.apuawling.ActionException;
import org.ua2.apuawling.ObjectNotFoundException;
import org.ua2.apuawling.ProviderException;
import org.ua2.edf.EDFData;
import org.ua2.json.JSONWrapper;

public class CreateThreadAction extends EDFAction<JSONObject> {
	
	public CreateThreadAction(EDFProvider provider) {
		super(provider, Method.POST, "folder/" + NAME_PATTERN);
	}

	@Override
	public EDFActionWrapper<JSONObject> perform(List<String> elements, JSONWrapper data) throws ActionException, ProviderException {
		String folder = elements.get(1);

		JSONObject message = data.getObject();
		JSONObject response = null;
		EDFData request = null;
		EDFData reply = null;
		
		try {
			request = new EDFData("request", "message_add");

			int id = getFolderId(folder);
			if(id == -1) {
				throw new ObjectNotFoundException("Folder " + folder + " does not exist");
			}
			request.add("folderid", id);
			
			String to = message.optString("to", null);
			if(to != null) {
				int toId = getUserId(to);
				if(toId != -1) {
					request.add("toid", toId);
				} else if(to != null) {
					request.add("toname", to);
				}
			}

			copyChild(message, "subject", request);
			copyChild(message, "body", request, "text");

			reply = sendAndRead(request);
			
			response = createMessage(reply);
		} catch(Exception e) {
			handleException("Cannot add message to " + folder, e);
		}
		
		return new EDFActionWrapper<JSONObject>(response, request, reply);
	}

}

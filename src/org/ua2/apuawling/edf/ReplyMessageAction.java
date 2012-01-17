package org.ua2.apuawling.edf;

import java.util.List;

import org.json.JSONObject;
import org.ua2.apuawling.ActionException;
import org.ua2.apuawling.ObjectNotFoundException;
import org.ua2.apuawling.ProviderException;
import org.ua2.edf.EDFData;
import org.ua2.json.JSONWrapper;

public class ReplyMessageAction extends EDFAction<JSONObject> {

	public ReplyMessageAction(EDFProvider provider) {
		super(provider, Method.POST, "message/" + ID_PATTERN);
	}
	
	@Override
	public EDFActionWrapper<JSONObject> perform(List<String> elements, JSONWrapper data) throws ActionException, ProviderException {
		int messageId = 0;
		try {
			messageId = Integer.parseInt(elements.get(1));
		} catch(NumberFormatException e) {
			throw new ActionException("Message ID " + elements.get(1) + " must be numeric");
		}

		JSONObject message = data.getObject();
		JSONObject response = null;
		EDFData request = null;
		EDFData reply = null;

		try {
			request = new EDFData("request", "message_add");

			EDFActionWrapper<JSONObject> parent = getMessage(messageId);
			if(parent == null) {
				throw new ObjectNotFoundException("Message " + messageId + " does not exist");
			}
			
			request.add("replyid", messageId);
			
			String folder = message.optString("folder", null);
			if(folder != null) {
				int id = getFolderId(folder);
				if(id == -1) {
					throw new ObjectNotFoundException("Folder " + folder + " does not exist");
				}
				request.add("folderid", id);
			}
		
			String to = message.optString("to", null);
			if(to != null) {
				int toId = getUserId(to);
				if(toId != -1) {
					request.add("toid", toId);
				} else if(to != null) {
					request.add("toname", to);
				}
			}

			if(!copyChild(message, "subject", request)) {
				copyChild(parent.getJSON(), "subject", request);
			}
			copyChild(message, "body", request, "text");

			reply = sendAndRead(request);

			response = createMessage(reply);
		} catch(Exception e) {
			handleException("Cannot reply to message " + messageId, e);
		}
		
		return new EDFActionWrapper<JSONObject>(response, request, reply);
	}

}

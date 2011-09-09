package org.ua2.apuawling.edf;

import java.util.List;

import org.json.JSONObject;
import org.ua2.apuawling.ActionException;
import org.ua2.apuawling.ObjectNotFoundException;
import org.ua2.apuawling.ProviderException;
import org.ua2.json.JSONWrapper;

public class GetMessageAction extends EDFAction<JSONObject> {

	public GetMessageAction(EDFProvider provider) {
		super(provider, Method.GET, "message/" + ID_PATTERN);
	}
	
	@Override
	public JSONObject perform(List<String> elements, JSONWrapper data) throws ActionException, ProviderException {
		int id = 0;
		try {
			id = Integer.parseInt(elements.get(1));
		} catch(NumberFormatException e) {
			throw new ActionException("Message ID " + elements.get(1) + " must be numeric");
		}

		JSONObject response = getMessage(id);
		if(response == null) {
			throw new ObjectNotFoundException("Message " + id + " does not exist");
		}
		
		return response;
	}

}

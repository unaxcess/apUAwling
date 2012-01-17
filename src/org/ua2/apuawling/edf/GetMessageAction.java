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
	public EDFActionWrapper<JSONObject> perform(List<String> parameters, JSONWrapper data) throws ActionException, ProviderException {
		int id = 0;
		try {
			id = Integer.parseInt(parameters.get(1));
		} catch(NumberFormatException e) {
			throw new ActionException("Message ID " + parameters.get(1) + " must be numeric");
		}

		EDFActionWrapper<JSONObject> wrapper = getMessage(id);
		if(wrapper == null) {
			throw new ObjectNotFoundException("Message " + id + " does not exist");
		}
		
		return wrapper;
	}

}

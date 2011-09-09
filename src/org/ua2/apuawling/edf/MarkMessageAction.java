package org.ua2.apuawling.edf;

import java.util.List;

import org.json.JSONObject;
import org.ua2.apuawling.ActionException;
import org.ua2.apuawling.InvalidActionException;
import org.ua2.apuawling.ProviderException;
import org.ua2.json.JSONWrapper;

public class MarkMessageAction extends EDFAction<JSONObject> {

	public MarkMessageAction(EDFProvider provider) {
		super(provider, Method.POST, "message/(read|unread|save|unsave)");
	}

	@Override
	public JSONObject perform(List<String> parameters, JSONWrapper data) throws ActionException, ProviderException {
		boolean type = !parameters.get(1).startsWith("un");
		if(parameters.get(1).endsWith("read")) {
			return readMessages(data.getArray(), type, false, false);
		} else if(parameters.get(1).endsWith("save")) {
			return readMessages(data.getArray(), type, false, false);
		}
		
		throw new InvalidActionException("");
	}

}

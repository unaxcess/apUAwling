package org.ua2.apuawling.edf;

import java.util.List;

import org.json.JSONObject;
import org.ua2.apuawling.ActionException;
import org.ua2.apuawling.ProviderException;
import org.ua2.json.JSONWrapper;

public class SubscribeAction extends EDFAction<JSONObject> {

	public SubscribeAction(EDFProvider provider) {
		super(provider, Method.POST, "folder/" + NAME_PATTERN + "/(subscribe|unsubscribe)");
	}

	@Override
	public EDFActionWrapper<JSONObject> perform(List<String> parameters, JSONWrapper data) throws ActionException, ProviderException {
		return subscribeFolder(parameters.get(1), "subscribe".equals(parameters.get(2)));
	}
}

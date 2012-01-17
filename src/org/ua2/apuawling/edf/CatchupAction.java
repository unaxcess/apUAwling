package org.ua2.apuawling.edf;

import java.util.List;

import org.json.JSONObject;
import org.ua2.apuawling.ActionException;
import org.ua2.apuawling.ProviderException;
import org.ua2.json.JSONWrapper;

public class CatchupAction extends EDFAction<JSONObject> {

	public CatchupAction(EDFProvider provider) {
		super(provider, Method.POST, "catchup/(message|folder)(/sticky)?");
	}

	@Override
	public EDFActionWrapper<JSONObject> perform(List<String> parameters, JSONWrapper data) throws ActionException, ProviderException {
		boolean sticky = parameters.size() == 3 && "sticky".equals(parameters.get(2));
		return readMessages(data.getArray(), true, true, sticky);
	}

}

package org.ua2.apuawling.edf;

import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.ua2.apuawling.ActionException;
import org.ua2.apuawling.ProviderException;
import org.ua2.json.JSONWrapper;

public class GetFoldersAction extends EDFAction<JSONArray> {
	
	private enum Filter {
		ALL,
		SUBSCRIBED,
		UNREAD,
		;
		
		public String toString() {
			return name().toLowerCase();
		}
	}
	
	private static final Logger logger = Logger.getLogger(GetFoldersAction.class);
	
	public GetFoldersAction(EDFProvider provider) {
		super(provider, Method.GET, "folders(/(" + Filter.ALL + "|" + Filter.SUBSCRIBED + "|" + Filter.UNREAD + "))?");
	}

	@Override
	public EDFActionWrapper<JSONArray> perform(List<String> parameters, JSONWrapper data) throws ActionException, ProviderException {
		Filter filter = parameters.size() == 2 ? Filter.valueOf(parameters.get(1).toUpperCase()) : Filter.ALL;

		EDFActionWrapper<JSONArray> response = null;
		logger.trace("Getting folders with filter=" + filter);
		
		try {
			response = getFolders(Filter.ALL != filter, Filter.UNREAD == filter, false);
		} catch(Exception e) {
			handleException("Cannot get folders", e);
		}
		
		return response;
	}
}

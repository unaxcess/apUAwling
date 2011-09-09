package org.ua2.apuawling.edf;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ua2.apuawling.Action;
import org.ua2.apuawling.ActionException;
import org.ua2.apuawling.IProvider;
import org.ua2.apuawling.ObjectNotFoundException;
import org.ua2.apuawling.ProviderException;
import org.ua2.apuawling.edf.EDFAction.ICopyProcessor;
import org.ua2.clientlib.UASession;
import org.ua2.edf.EDFData;

public class EDFProvider extends EDFClient implements IProvider {

	private long lastRequest = 0;

	private List<Action<?>> actions = new ArrayList<Action<?>>();

	private Map<String, Integer> folderLookup = null;
	private static Map<String, Integer> userLookup = null;
	private static Map<Integer, String> bodyLookup = new HashMap<Integer, String>();

	private static final Logger logger = Logger.getLogger(EDFProvider.class);

	public EDFProvider(String host, int port, String username, String password, InetAddress address, String client) {
		super(host, port, username, password, address, client);
		
		lastRequest = System.currentTimeMillis();

		add(new GetFoldersAction(this));
		add(new SubscribeAction(this));

		add(new GetMessagesAction(this));

		add(new GetMessageAction(this));
		add(new MarkMessageAction(this));
		add(new CatchupAction(this));

		add(new CreateThreadAction(this));
		add(new ReplyMessageAction(this));

		add(new GetUserAction(this));
		add(new GetUsersAction(this));

		add(new SystemAction(this));
	}
	
	private void add(Action<?> action) {
		actions.add(action);
	}

	@Override
	public Action<?> getAction(String method, String path) {
		for(Action<?> action : actions) {
			if(action.isMatch(method, path)) {
				return action;
			}
		}
		
		return null;
	}

	public long getLastRequest() {
		return lastRequest;
	}
	
	protected UASession getSession() throws ProviderException {
		lastRequest = System.currentTimeMillis();
		
		return super.getSession();
	}
}

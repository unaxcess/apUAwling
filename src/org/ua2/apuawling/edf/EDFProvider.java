package org.ua2.apuawling.edf;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.ua2.apuawling.Action;
import org.ua2.apuawling.IProvider;
import org.ua2.apuawling.ProviderException;
import org.ua2.clientlib.UASession;

public class EDFProvider extends EDFClient implements IProvider {

	private long lastRequest = 0;

	private List<Action<?>> actions = new ArrayList<Action<?>>();

	public EDFProvider(String host, int port, String username, String password, InetAddress address, String client) {
		super(host, port, username, password, address, client);
		
		lastRequest = System.currentTimeMillis();

		add(new GetFoldersAction(this));
		add(new SubscribeAction(this));

		add(new GetMessagesAction(this));
		add(new GetThreadAction(this));
		add(new GetFolderAction(this));

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

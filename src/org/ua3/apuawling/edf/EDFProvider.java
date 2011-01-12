package org.ua3.apuawling.edf;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ua2.clientlib.UA;
import org.ua2.clientlib.UASession;
import org.ua2.clientlib.exception.NoConnectionError;
import org.ua2.edf.EDFData;
import org.ua2.edf.EDFTypeException;
import org.ua3.apuawling.IProvider;
import org.ua3.apuawling.InvalidCommandException;
import org.ua3.apuawling.InvalidLoginException;
import org.ua3.apuawling.JSONWrapper;
import org.ua3.apuawling.ObjectNotFoundException;
import org.ua3.apuawling.ProviderException;
import org.ua3.apuawling.Server;
import org.ua3.apuawling.Session;

public class EDFProvider implements IProvider {

	private final String host;
	private final int port;
	private final String username;
	private final String password;
	private final InetAddress address;
	private final String client;
	
	private UASession session = null;
	private long lastRequest = 0;

	private Map<String, Integer> folderLookup = null;
	private static Map<String, Integer> userLookup = null;
	private static Map<Integer, String> bodyLookup = new HashMap<Integer, String>();

	private static final Command[] COMMANDS = new Command[] { Command.FOLDER, Command.FOLDERS, Command.MESSAGE, Command.SYSTEM, Command.USER, Command.USERS, };

	private static final Logger logger = Logger.getLogger(EDFProvider.class);
	
	public EDFProvider(String host, int port) {
		this(host, port, null, null, null, null);
	}

	public EDFProvider(String host, int port, String username, String password, InetAddress address, String client) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.address = address;
		this.client = client;
		
		lastRequest = System.currentTimeMillis();
	}

	public String getUsername() {
		return username;
	}

	public InetAddress getAddress() {
		return address;
	}
	
	public long getLastRequest() {
		return lastRequest;
	}
	
	public void disconnect() {
		try {
			if(session != null) {
				session.logout();
			}
		} catch(NoConnectionError e) {
			logger.error("Logout failed", e);
		}
	}

	private static void handleException(String logMsg, Exception e) throws ProviderException {
		handleException(logMsg, e, logMsg);
	}
	
	private static void handleException(String logMsg, Exception e, String msg) throws ProviderException {
		if(logMsg != null) {
			logger.error(logMsg, e);
		}
		if(e != null && e instanceof ProviderException) {
			throw (ProviderException)e;
		}
		throw new ProviderException(msg);
	}

	private UASession getSession() throws ProviderException {
		lastRequest = System.currentTimeMillis();
		
		if(session != null) {
			return session;
		}

		try {
			logger.debug("Creating UA session");
			UA ua = new UA();
			session = new UASession(ua);

			logger.debug("Connecting to " + host + " on " + port);
			session.connect(host, port);

			if(username != null) {
				if(client != null) {
					session.setClientName(client);
				} else {
					session.setClientName("apUAwling v" + Server.VERSION);
				}
				session.setClientProtocol("3.0-pre");
				
				if(!session.login(username, password, address, true)) {
					session = null;
					handleException("Login failed", new InvalidLoginException("Invalid credentials"));
				}
			}
		} catch(Exception e) {
			session = null;
			handleException("Login failed", e);
		}

		return session;
	}

	private EDFData sendAndRead(EDFData request) throws ProviderException {
		for(int attempt = 1; attempt <= 3; attempt++) {
			try {
				return getSession().sendAndRead(request);
			} catch(NoConnectionError e) {
				session = null;
				logger.error("No connection to UA", e);
			}
		}
		
		throw new ProviderException("Cannot connect to server");
	}

	private String getChildStr(EDFData data, String name) {
		EDFData child = data.getChild(name);
		if(child == null) {
			return null;
		}
		return child.getString();
	}

	private int getChildInt(EDFData data, String name) {
		EDFData child = data.getChild(name);
		if(child == null) {
			return 0;
		}
		return child.getInteger();
	}

	private boolean isChildBool(EDFData data, String name) {
		EDFData child = data.getChild(name);
		if(child == null) {
			return false;
		}
		try {
			Integer value = child.getInteger();
			return value == 1;
		} catch(EDFTypeException e) {
			logger.error("Cannot get " + name + " value", e);
			return false;
		}
	}

	private void copyChildStr(EDFData src, String name, JSONObject dest) throws JSONException {
		copyChildStr(src, name, dest, name);
	}
	
	private void copyChildStr(EDFData src, String srcName, JSONObject dest, String destName) throws JSONException {
		EDFData child = src.getChild(srcName);
		if(child != null) {
			dest.put(destName, child.getString());
		}
	}
	
	private boolean copyChildStr(JSONObject src, String name, EDFData dest) {
		return copyChildStr(src, name, dest, name);
	}

	private boolean copyChildStr(JSONObject src, String srcName, EDFData dest, String destName) {
		try {
			String value = src.getString(srcName);
			if(value != null) {
				dest.add(destName, value);
				return true;
			}
		} catch(JSONException e) {
		}
		
		return false;
	}
	
	private boolean copyChildInt(EDFData src, String name, JSONObject dest) throws JSONException {
		return copyChildInt(src, name, dest, name);
	}

	private boolean copyChildInt(EDFData src, String srcName, JSONObject dest, String destName) throws JSONException {
		EDFData child = src.getChild(srcName);
		if(child == null) {
			return false;
		}
		
		dest.put(destName, child.getInteger());
		return true;
	}

	private void addFoldersToLookup(EDFData data) throws JSONException {
		List<EDFData> children = data.getChildren("folder");
		for(EDFData child : children) {
			String name = child.getChild("name").getString();
			int id = child.getInteger();
			logger.debug("Adding " + name + "-> " + id + " to folder lookup");
			folderLookup.put(name.toLowerCase(), id);
			addFoldersToLookup(child);
		}
	}
	
	private void initFolderLookup() throws ProviderException {
		if(folderLookup == null) {
			folderLookup = new HashMap<String, Integer>();

			try {
				EDFData request = new EDFData("request", "folder_list");
				request.add("searchtype", 2);

				EDFData reply = sendAndRead(request);

				addFoldersToLookup(reply);
			} catch(Exception e) {
				folderLookup = null;
				handleException("Cannot get folder lookup", e);
			}
		}
	}

	private int getFolderId(String name) throws ProviderException {
		initFolderLookup();
		
		Integer id = folderLookup.get(name.toLowerCase());
		if(id == null) {
			return -1;
		}
		
		return id;
	}

	private int getUserId(String name) throws ProviderException {
		if(userLookup == null) {
			userLookup = new HashMap<String, Integer>();

			try {
				EDFData request = new EDFData("request", "user_list");

				EDFData reply = sendAndRead(request);

				List<EDFData> children = reply.getChildren("user");
				for(EDFData child : children) {
					String userName = child.getChild("name").getString();
					int id = child.getInteger();
					logger.debug("Adding " + name + "-> " + id + " to user lookup");
					userLookup.put(userName.toLowerCase(), id);
				}
			} catch(Exception e) {
				userLookup = null;
				handleException("Cannot get user " + name, e);
			}
		}

		Integer id = userLookup.get(name.toLowerCase());
		if(id == null) {
			return -1;
		}
		
		return id;
	}
	
	private void addFoldersToList(EDFData data, JSONArray list, boolean subscribedOnly, boolean unreadOnly) throws JSONException {
		List<EDFData> children = data.getChildren("folder");
		for(EDFData child : children) {
			int subtype = getChildInt(child, "subtype");
			int count = getChildInt(child, "nummsgs");
			int unread = getChildInt(child, "unread");
			if((!subscribedOnly || subtype > 0) && (!unreadOnly || unread > 0)) {
				JSONObject folder = new JSONObject();
				copyChildStr(child, "name", folder, "folder");
				folder.put("count", count);
				folder.put("unread", unread);
				list.put(folder);
			}

			addFoldersToList(child, list, subscribedOnly, unreadOnly);
		}
	}
	
	private JSONObject createMessage(EDFData src, String folderName, int parentId) throws JSONException {
		JSONObject dest = createMessage(src);
		
		if(folderName != null) {
			dest.put("folder", folderName);
		}
		
		if(parentId > 0) {
			dest.put("inReplyTo", parentId);
		}
		
		return dest;
	}
	
	private JSONObject createMessage(EDFData src) throws JSONException {
		JSONObject dest = new JSONObject();

		Object value = src.getValue();
		if(value instanceof Integer) {
			dest.put("id", src.getInteger());
		} else {
			copyChildInt(src, "messageid", dest, "id");
		}

		copyChildInt(src, "date", dest, "epoch");
		copyChildStr(src, "fromname", dest, "from");
		copyChildStr(src, "toname", dest, "to");
		copyChildStr(src, "subject", dest);

		dest.put("read", isChildBool(src, "read"));
		copyChildInt(src, "replyto", dest, "inReplyTo");
		
		copyChildStr(src, "text", dest, "body");
		
		if(dest.has("body")) {
			bodyLookup.put(dest.getInt("id"), dest.getString("body"));
		}

		return dest;
	}

	private void addMessagesToList(EDFData reply, String folderName, boolean unreadOnly, boolean full, JSONArray list) throws JSONException {
		int parentId = 0;
		Object value = reply.getValue();
		if(value instanceof Integer) {
			parentId = reply.getInteger();
		}
		List<EDFData> children = reply.getChildren("message");
		for(EDFData child : children) {
			if(!unreadOnly || !isChildBool(child, "read")) {
				JSONObject message = createMessage(child, folderName, parentId);
				if(full) {
					message.put("body", getMessageBody(message.getInt("id")));
				}
				list.put(message);
			}
			addMessagesToList(child, folderName, unreadOnly, full, list);
		}
	}

	private String getMessageBody(int id) {
		String body = bodyLookup.get(id);
		if(body == null) {
			try {
				getMessage(id);
				body = bodyLookup.get(id);
			} catch(Exception e) {
				logger.error("Cannot get body of message " + id, e);
			}
		}
		return body;
	}

	public Command[] getCommands() throws ProviderException {
		if(this == Session.getDefaultProvider()) {
			// What can you do without logging in?
			return new Command[] { Command.SYSTEM };
		}
		
		return COMMANDS;
	}
	
	private String getParameters(String[] parameters) {
		if(parameters == null) {
			return null;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for(String parameter : parameters) {
			sb.append(" ");
			sb.append(parameter);
		}
		sb.append(" ]");
		return sb.toString();
	}

	public Object provide(String method, Command command, String[] parameters, JSONWrapper data) throws ProviderException {
		if(logger.isDebugEnabled()) logger.debug("Providing " + method + " on " + command + " using " + getParameters(parameters) + ( data != null ? " and " + data.toString() : ""));
		
		if(Command.FOLDERS == command) {
			boolean subscribedOnly = false;
			boolean unreadOnly = false;
			for(String parameter : parameters) {
				if(parameter.equals("subscribed")) {
					subscribedOnly = true;
				} else if(parameter.equals("unread")) {
					subscribedOnly = true;
					unreadOnly = true;
				} else if(parameter.equals("all")) {
					subscribedOnly = false;
					unreadOnly = false;
				}
			}
			return getFolders(subscribedOnly, unreadOnly);

		} else if(Command.FOLDER == command) {
			if(parameters.length >= 1) {
				if("POST".equals(method)) {
					String name = parameters[0];
					return addMessage(name, data.getObject());
					
				} else {
					String name = parameters[0];
					boolean unreadOnly = false;
					boolean full = false;
					for(String parameter : parameters) {
						if(name == null) {
							name = parameter;
						} else if(parameter.equals("unread")) {
							unreadOnly = true;
						} else if(parameter.equals("full")) {
							full = true;
						}
					}
					return getFolder(name, unreadOnly, full);
					
				}
			}
			throw new InvalidCommandException("Must supply folder name");

		} else if(Command.MESSAGE == command) {
			if(parameters.length >= 1) {
				if("read".equals(parameters[0])) {
					if("POST".equals(method)) {
						return markMessages(true, data.getArray());
						
					} else {
						throw new ProviderException("Method " + method + " is not supported by " + command);
					}
				} else if("unread".equals(parameters[0])) {
					if("POST".equals(method)) {
						return markMessages(false, data.getArray());
						
					} else {
						throw new ProviderException("Method " + method + " is not supported by " + command);
					}
				} else {
					int id = 0;
					try {
						id = Integer.parseInt(parameters[0]);
					} catch(NumberFormatException e) {
						throw new ProviderException("Message ID " + parameters[0] + " must be numeric");
					}
						
					if("POST".equals(method)) {
						return replyToMessage(id, data.getObject());
						
					} else {
						return getMessage(id);
						
					}
				}
			}
	
			throw new InvalidCommandException("Must supply message ID");
		} else if(Command.SYSTEM == command) {
			return getSystem();

		} else if(Command.USER == command) {
			return getUser();

		} else if(Command.USERS == command) {
			boolean onlineOnly = false;
			for(String parameter : parameters) {
				if(parameter.equals("online")) {
					onlineOnly = true;
				}
			}
			return getUsers(onlineOnly);
		}

		throw new InvalidCommandException(command + " not supported");
	}

	public void close() throws ProviderException {
		try {
			getSession().logout();
		} catch(NoConnectionError e) {
			logger.error("Cannot close session", e);
		} finally {
			session = null;
		}
	}

	public JSONArray getFolders(boolean subscribedOnly, boolean unreadOnly) throws ProviderException {
		JSONArray response = null;
		logger.trace("Getting folders subscribedOnly=" + subscribedOnly + " unreadOnly=" + unreadOnly);
		
		try {
			EDFData request = new EDFData("request", "folder_list");
			request.add("searchtype", 3);

			EDFData reply = sendAndRead(request);

			response = new JSONArray();
			addFoldersToList(reply, response, subscribedOnly, unreadOnly);
		} catch(Exception e) {
			handleException("Cannot get folders", e);
		}
		
		return response;
	}
	
	public JSONArray getFolder(String name, boolean unreadOnly, boolean full) throws ProviderException {
		JSONArray response = null;
		
		try {
			int id = getFolderId(name);
			if(id == -1) {
				throw new ObjectNotFoundException("Folder " + name + " does not exist");
			}
			
			EDFData request = new EDFData("request", "message_list");

			request.add("folderid", id);
			request.add("searchtype", 1);
			
			if(name.equalsIgnoreCase("private")) {
				EDFData search = new EDFData("or");
				search.add("fromid", getSession().getUserId());
				search.add("toid", getSession().getUserId());
				request.add(search);
			}

			EDFData reply = sendAndRead(request);
			
			name = getChildStr(reply, "foldername");

			response = new JSONArray();
			addMessagesToList(reply, name, unreadOnly, full, response);
		} catch(Exception e) {
			handleException("Cannot get messages in folder " + name, e);
		}

		return response;
	}

	public JSONObject getMessage(int id) throws ProviderException {
		JSONObject response = null;
		
		try {
			EDFData request = new EDFData("request", "message_list");

			request.add("messageid", id);
			request.add("markread", 0);

			EDFData reply = sendAndRead(request);

			EDFData message = reply.getChild("message");
			if(message != null) {
				response = createMessage(message);
				copyChildStr(reply, "foldername", response, "folder");
			} else {
				handleException("No message in reply:\n" + reply.format(true), new ObjectNotFoundException("Cannot get message " + id));
			}
		} catch(Exception e) {
			handleException("Cannot get message " + id, e);
		}

		return response;
	}
	
	public JSONObject addMessage(String folder, JSONObject message) throws ProviderException {
		JSONObject response = null;
		
		try {
			EDFData request = new EDFData("request", "message_add");

			int id = getFolderId(folder);
			if(id == -1) {
				throw new ObjectNotFoundException("Folder " + folder + " does not exist");
			}
			request.add("folderid", id);
			
			try {
				String to = message.getString("to");
				int toId = getUserId(to);
				if(toId != -1) {
					request.add("toid", toId);
				} else if(to != null) {
					request.add("toname", to);
				}
			} catch(JSONException e) {
			}

			copyChildStr(message, "subject", request);
			copyChildStr(message, "body", request, "text");

			EDFData reply = sendAndRead(request);
			
			response = createMessage(reply);
		} catch(Exception e) {
			handleException("Cannot add message to " + folder, e);
		}
		
		return response;
	}

	public JSONObject replyToMessage(int id, JSONObject message) throws ProviderException {
		JSONObject response = null;
		
		try {
			EDFData request = new EDFData("request", "message_add");

			JSONObject parent = getMessage(id);
			if(parent == null) {
				throw new ObjectNotFoundException("Message " + id + " does not exist");
			}
			request.add("replyid", id);
			
			try {
				String to = message.getString("to");
				int toId = getUserId(to);
				if(toId != -1) {
					request.add("toid", toId);
				} else if(to != null) {
					request.add("toname", to);
				}
			} catch(JSONException e) {
			}

			if(!copyChildStr(message, "subject", request)) {
				copyChildStr(parent, "subject", request);
			}
			copyChildStr(message, "body", request, "text");

			EDFData reply = sendAndRead(request);

			response = createMessage(reply);
		} catch(Exception e) {
			handleException("Cannot reply to message " + id, e);
		}
		
		return response;
	}

	private JSONObject markMessages(boolean mark, JSONArray array) throws ProviderException {
		JSONObject response = new JSONObject();
		
		try {
			int count = 0;
			for(int messageNum = 0; messageNum < array.length(); messageNum++) {
				int messageId = array.getInt(messageNum);
				
				EDFData request = new EDFData("request", mark ? "message_mark_read" : "message_mark_unread");
				
				request.add("messageid", messageId);
				
				EDFData reply = sendAndRead(request);
				
				int numMarked = getChildInt(reply, "nummarked");
				
				count += numMarked;
			}
		
			response.put("count", count);
		} catch(Exception e) {
			handleException("Cannot mark messages " + array, e);
		}

		return response;
	}

	public JSONObject getSystem() throws ProviderException {
		JSONObject response = null;
		
		try {
			EDFData request = new EDFData("request", "system_list");

			EDFData reply = sendAndRead(request);

			response = new JSONObject();
			
			response.put("banner", reply.getChild("banner").getString());
		} catch(Exception e) {
			handleException("Cannot get system", e);
		}
		
		return response;
	}

	public JSONObject getUser() throws ProviderException {
		JSONObject response = null;
		
		try {
			EDFData request = new EDFData("request", "user_list");
			
			request.add("searchtype", 4);

			EDFData reply = sendAndRead(request);

			EDFData child = reply.getChild("user");

			response = new JSONObject();
		
			copyChildStr(child, "name", response);
		} catch(Exception e) {
			handleException("Cannot get user", e);
		}
		
		return response;
	}

	public JSONArray getUsers(boolean onlineOnly) throws ProviderException {
		JSONArray response = null;
		
		try {
			EDFData request = new EDFData("request", "user_list");
			if(onlineOnly) {
				request.add("searchtype", 1);
			}

			EDFData reply = sendAndRead(request);

			response = new JSONArray();
			List<EDFData> children = reply.getChildren("user");
			for(EDFData child : children) {
				JSONObject user = new JSONObject();
				copyChildStr(child, "name", user);
				EDFData login = child.getChild("login");
				if(login != null) {
					copyChildInt(login, "timeon", user);
				}
				response.put(user);
			}
		} catch(Exception e) {
			handleException("Cannot get users", e);
		}
		
		return response;
	}
}

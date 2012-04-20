package org.ua2.apuawling.edf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ua2.apuawling.Action;
import org.ua2.apuawling.ActionException;
import org.ua2.apuawling.ObjectNotFoundException;
import org.ua2.apuawling.ProviderException;
import org.ua2.edf.EDFData;
import org.ua2.edf.EDFTypeException;

public abstract class EDFAction<T> extends Action<EDFActionWrapper<T>> {

	protected interface ICopyProcessor {
		String getSrcName();
		String getDestName();
		JSONObject process(EDFData child) throws JSONException;
	}

	private EDFProvider provider;

	private Map<String, Integer> folderLookup = null;
	private static Map<String, Integer> userLookup = null;
	private static Map<Integer, String> bodyLookup = new HashMap<Integer, String>();

	private static final Logger logger = Logger.getLogger(EDFAction.class);
	
	protected EDFAction(EDFProvider provider, Method method, String path) {
		super(method, path);
		
		this.provider = provider;
	}
	
	protected EDFData sendAndRead(EDFData request) throws ProviderException {
		return provider.sendAndRead(request);
	}

	protected void handleException(String logMsg, Exception e) throws ActionException, ProviderException {
		handleException(logMsg, e, logMsg);
	}
	
	protected void handleException(String logMsg, Exception e, String msg) throws ActionException, ProviderException {
		if(logMsg != null) {
			logger.error(logMsg, e);
		}
		if(e != null && e instanceof ActionException) {
			throw (ActionException)e;
		}
		if(e != null && e instanceof ProviderException) {
			throw (ProviderException)e;
		}
		throw new ActionException(msg, e);
	}

	protected String getChildStr(EDFData data, String name) {
		EDFData child = data.getChild(name);
		if(child == null) {
			return null;
		}
		return child.getString();
	}

	protected int getChildInt(EDFData data, String name) {
		EDFData child = data.getChild(name);
		if(child == null) {
			return 0;
		}
		return child.getInteger();
	}

	protected boolean isChildBool(EDFData data, String name) {
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

	protected void copyChild(EDFData src, String name, JSONObject dest) throws JSONException {
		copyChild(src, name, dest, name);
	}
	
	protected void copyChild(EDFData src, String srcName, JSONObject dest, String destName) throws JSONException {
		EDFData child = src.getChild(srcName);
		if(child != null) {
			dest.put(destName, child.getValue());
		}
	}
	
	protected boolean copyChild(JSONObject src, String name, EDFData dest) {
		return copyChild(src, name, dest, name);
	}

	protected boolean copyChild(JSONObject src, String srcName, EDFData dest, String destName) {
		try {
			Object value = src.get(srcName);
			if(value != null) {
				if(value instanceof Integer) {
					dest.add(destName, (Integer)value);
				} else if(value instanceof String) {
					dest.add(destName, (String)value);
				}
				return true;
			}
		} catch(JSONException e) {
		}
		
		return false;
	}
	
	private void copyChildren(EDFData src, JSONObject dest, ICopyProcessor processor) throws JSONException {
		List<EDFData> inReplyTos = src.getChildren(processor.getSrcName());
		if(inReplyTos != null && inReplyTos.size() > 0) {
			JSONArray items = new JSONArray();
			for(EDFData inReplyTo : inReplyTos) {
				JSONObject item = processor.process(inReplyTo);
				if(item != null) {
					items.put(item);
				}
			}
			if(items.length() > 0) {
				dest.put(processor.getDestName(), items);
			}
		}
	}
	
	protected JSONArray addMessagesToList(EDFData reply, String folderName, boolean unreadOnly, boolean full) throws JSONException {
		JSONArray response = new JSONArray();
		addMessagesToList(reply, folderName, unreadOnly, full, 0, response, new HashMap<Integer, Integer>());
		return response;
	}

	private int addMessagesToList(EDFData reply, String folderName, boolean unreadOnly, boolean full, int pos, JSONArray list, Map<Integer, Integer> threads) throws JSONException {
		int parentId = 0;
		Object value = reply.getValue();
		if(value instanceof Integer) {
			parentId = reply.getInteger();
		}
		List<EDFData> children = reply.getChildren("message");
		for(EDFData child : children) {

			int id = 0;
			value = child.getValue();
			if(value instanceof Integer) {
				id = child.getInteger();
			} else {
				id = getChildInt(child, "messageid");
			}
			Integer threadId = getChildInt(child, "threadid");
			if(threadId == 0) {
				threadId = id;
				if(parentId > 0) {
					threadId = threads.get(parentId);
				}
			}
			threads.put(id, threadId);
			
			if(!unreadOnly || !isChildBool(child, "read")) {
				pos++;
				
				JSONObject message = createMessage(child);
				if(folderName != null) {
					message.put("folder", folderName);
				}
				if(parentId > 0) {
					message.put("inReplyTo", parentId);
				}
				message.put("position", pos);
				message.put("thread", threadId);
				if(full && !message.has("body")) {
					message.put("body", getMessageBody(message.getInt("id")));
				}
				
				list.put(message);
			}
			pos = addMessagesToList(child, folderName, unreadOnly, full, pos, list, threads);
		}
		
		return pos;
	}

	protected String getMessageBody(int id) {
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

	protected EDFActionWrapper<JSONObject> getMessage(int id) throws ProviderException, ActionException {
		JSONObject response = null;
		EDFData request = null;
		EDFData reply = null;

		try {
			request = new EDFData("request", "message_list");

			request.add("messageid", id);
			request.add("markread", 0);

			reply = sendAndRead(request);

			EDFData message = reply.getChild("message");
			if(message != null) {
				response = createMessage(message);
				copyChild(reply, "foldername", response, "folder");
			} else {
				handleException("No message in reply:\n" + reply.format(true), new ObjectNotFoundException("Cannot get message " + id));
			}
		} catch(Exception e) {
			handleException("Cannot get message " + id, e);
		}

		return new EDFActionWrapper<JSONObject>(response, request, reply);
	}
	
	protected JSONObject createMessage(EDFData src) throws JSONException {
		JSONObject dest = new JSONObject();

		int id = 0;
		Object value = src.getValue();
		if(value instanceof Integer) {
			id = src.getInteger();
		} else {
			id = getChildInt(src, "messageid");
		}
		dest.put("id", id);

		copyChild(src, "foldername", dest, "folder");
		copyChild(src, "date", dest, "epoch");
		copyChild(src, "fromname", dest, "from");
		copyChild(src, "toname", dest, "to");
		copyChild(src, "subject", dest);
		copyChild(src, "msgpos", dest, "position");
		copyChild(src, "threadid", dest, "thread");

		dest.put("read", isChildBool(src, "read"));
		copyChild(src, "replyto", dest, "inReplyTo");
		
		copyChildren(src, dest, new ICopyProcessor() {
			@Override
			public String getSrcName() {
				return "attachment";
			}

			@Override
			public String getDestName() {
				return "annotations";
			}

			@Override
			public JSONObject process(EDFData child) throws JSONException {
				if(!"text/x-ua-annotation".equals(getChildStr(child, "content-type"))) {
					return null;
				}
				
				JSONObject item = new JSONObject();
				copyChild(child, "date", item, "epoch");
				copyChild(child, "fromname", item, "from");
				copyChild(child, "text", item, "body");
				return item;
			}
			
		});
		
		copyChildren(src, dest, new ICopyProcessor() {
			@Override
			public String getSrcName() {
				return "replyto";
			}

			@Override
			public String getDestName() {
				return "inReplyToHierarchy";
			}

			@Override
			public JSONObject process(EDFData child) throws JSONException {
				JSONObject item = new JSONObject();
				int replyId = child.getInteger();
				item.put("id", replyId);
				copyChild(child, "fromname", item, "from");
				copyChild(child, "foldername", item, "folder");
				return item;
			}
			
		});
		
		copyChildren(src, dest, new ICopyProcessor() {
			@Override
			public String getSrcName() {
				return "replyby";
			}

			@Override
			public String getDestName() {
				return "replyToBy";
			}

			@Override
			public JSONObject process(EDFData child) throws JSONException {
				JSONObject item = new JSONObject();
				int replyId = child.getInteger();
				item.put("id", replyId);
				copyChild(child, "fromname", item, "from");
				copyChild(child, "foldername", item, "folder");
				return item;
			}
			
		});
		
		EDFData srcVotes = src.getChild("votes");
		if(srcVotes != null) {
			JSONObject destVote = new JSONObject();
			dest.append("votes", destVote);
			
			copyChild(srcVotes, "votetype", destVote);
			copyChild(srcVotes, "numvotes", destVote);
			
			copyChildren(srcVotes, destVote, new ICopyProcessor() {
				@Override
				public String getSrcName() {
					return "vote";
				}

				@Override
				public String getDestName() {
					return "vote";
				}

				@Override
				public JSONObject process(EDFData child) throws JSONException {
					JSONObject item = new JSONObject();
					int id = child.getInteger();
					item.put("id", id);
					copyChild(child, "text", item);
					copyChild(child, "numvotes", item);
					return item;
				}
				
			});
		}
		
		copyChild(src, "text", dest, "body");
		
		if(dest.has("body")) {
			bodyLookup.put(dest.getInt("id"), dest.getString("body"));
		}

		return dest;
	}

	protected int getFolderId(String name) throws ProviderException, ActionException {
		initFolderLookup();
		
		Integer id = folderLookup.get(name.toLowerCase());
		if(id == null) {
			return -1;
		}
		
		return id;
	}
	
	private void initFolderLookup() throws ProviderException, ActionException {
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

	private void addFoldersToLookup(EDFData data) throws JSONException {
		List<EDFData> children = data.getChildren("folder");
		for(EDFData child : children) {
			String name = child.getChild("name").getString();
			int id = child.getInteger();
			logger.debug("Adding " + name + " -> " + id + " to folder lookup");
			folderLookup.put(name.toLowerCase(), id);
			addFoldersToLookup(child);
		}
	}

	protected int getUserId(String name) throws ProviderException, ActionException {
		if(userLookup == null) {
			userLookup = new HashMap<String, Integer>();

			try {
				EDFData request = new EDFData("request", "user_list");

				EDFData reply = sendAndRead(request);

				List<EDFData> children = reply.getChildren("user");
				for(EDFData child : children) {
					String userName = child.getChild("name").getString();
					int id = child.getInteger();
					logger.debug("Adding " + userName + "-> " + id + " to user lookup");
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

	protected EDFActionWrapper<JSONArray> getFolders(boolean subscribedOnly, boolean unreadOnly, boolean summary) throws ProviderException, ActionException {
		JSONArray response = null;
		EDFData request = null;
		EDFData reply = null;
		logger.trace("Getting folders subscribedOnly=" + subscribedOnly + " unreadOnly=" + unreadOnly + " summary=" + summary);
		
		try {
			request = new EDFData("request", "folder_list");
			request.add("searchtype", 3);

			reply = sendAndRead(request);

			response = new JSONArray();
			addFoldersToList(reply, response, subscribedOnly, unreadOnly);
		} catch(Exception e) {
			handleException("Cannot get folders", e);
		}
		
		return new EDFActionWrapper<JSONArray>(response, request, reply);
	}

	private void addFoldersToList(EDFData data, JSONArray list, boolean subscribedOnly, boolean unreadOnly) throws JSONException {
		List<EDFData> children = data.getChildren("folder");
		for(EDFData child : children) {
			int subtype = getChildInt(child, "subtype");
			int count = getChildInt(child, "nummsgs");
			int unread = getChildInt(child, "unread");
			if((!subscribedOnly || subtype > 0) && (!unreadOnly || unread > 0)) {
				JSONObject folder = new JSONObject();
				copyChild(child, "name", folder, "folder");
				folder.put("count", count);
				folder.put("unread", unread);
				folder.put("subscribed", subtype > 0);
				list.put(folder);
			}

			addFoldersToList(child, list, subscribedOnly, unreadOnly);
		}
	}
	
	protected int getUserId() throws ProviderException {
		return provider.getSession().getUserId();
	}

	protected EDFActionWrapper<JSONObject> readMessages(JSONArray array, boolean type, boolean catchup, boolean stayCaughtUp) throws ProviderException, ActionException {
		JSONObject response = new JSONObject();
		EDFData request = null;
		EDFData reply = null;
		
		try {
			int count = 0;
			for(int messageNum = 0; messageNum < array.length(); messageNum++) {
				int messageId = array.getInt(messageNum);
				
				request = new EDFData("request", type ? "message_mark_read" : "message_mark_unread");
				
				request.add("messageid", messageId);
				if(catchup) {
					request.add("marktype", 1);
					if(stayCaughtUp) {
						request.add("markkeep", 1);
					}
				}
				
				reply = sendAndRead(request);
				
				int numMarked = getChildInt(reply, "nummarked");
				
				count += numMarked;
			}
		
			response.put("count", count);
		} catch(Exception e) {
			handleException("Cannot mark messages " + array, e);
		}

		return new EDFActionWrapper<JSONObject>(response, request, reply);
	}

	protected EDFActionWrapper<JSONObject> saveMessages(JSONArray array, boolean type) throws ProviderException, ActionException {
		JSONObject response = new JSONObject();
		EDFData request = null;
		EDFData reply = null;
		
		try {
			int count = 0;
			for(int messageNum = 0; messageNum < array.length(); messageNum++) {
				int messageId = array.getInt(messageNum);
				
				request = new EDFData("request", type ? "message_mark_save" : "message_mark_unsave");
				
				request.add("messageid", messageId);
				
				reply = sendAndRead(request);
				
				int numMarked = getChildInt(reply, "nummarked");
				
				count += numMarked;
			}
		
			response.put("count", count);
		} catch(Exception e) {
			handleException("Cannot mark messages " + array, e);
		}

		return new EDFActionWrapper<JSONObject>(response, request, reply);
	}

	protected EDFActionWrapper<JSONObject> subscribeFolder(String folder, boolean subscribe) throws ProviderException, ActionException {
		JSONObject response = null;
		EDFData request = null;
		EDFData reply = null;

		try {
			request = new EDFData("request", subscribe ? "folder_subscribe" : "folder_unsubscribe");

			int id = getFolderId(folder);
			if(id == -1) {
				throw new ObjectNotFoundException("Folder " + folder + " does not exist");
			}
			request.add("folderid", id);
			
			reply = sendAndRead(request);
			
			response = new JSONObject();
			
			copyChild(reply, "foldername", response, "folder");

		} catch(Exception e) {
			handleException("Cannot add message to " + folder, e);
		}
		
		return new EDFActionWrapper<JSONObject>(response, request, reply);
	}
}

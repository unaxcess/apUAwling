package org.ua2.apuawling.edf;

import java.net.InetAddress;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.ua2.apuawling.InvalidLoginException;
import org.ua2.apuawling.ProviderException;
import org.ua2.apuawling.Server;
import org.ua2.clientlib.UA;
import org.ua2.clientlib.UASession;
import org.ua2.clientlib.exception.NoConnectionError;
import org.ua2.edf.EDFData;
import org.ua2.edf.EDFTypeException;

public class EDFClient {

	private final String host;
	private final int port;
	private final String username;
	private final String password;
	private final InetAddress address;
	private final String client;
	
	private UASession session = null;

	private static final Logger logger = Logger.getLogger(EDFClient.class);
	
	public EDFClient(String host, int port, String username, String password, InetAddress address, String client) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.address = address;
		this.client = client;
	}

	public String getUsername() {
		return username;
	}

	public InetAddress getAddress() {
		return address;
	}
	
	public String getPassword() {
		return password;
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

	protected void handleException(String logMsg, Exception e) throws ProviderException {
		handleException(logMsg, e, logMsg);
	}
	
	protected void handleException(String logMsg, Exception e, String msg) throws ProviderException {
		if(logMsg != null) {
			logger.error(logMsg, e);
		}
		if(e != null && e instanceof ProviderException) {
			throw (ProviderException)e;
		}
		throw new ProviderException(msg, e);
	}

	protected synchronized UASession getSession() throws ProviderException {
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
				session.setClientProtocol("JSON " + Server.VERSION);
				
				if(!session.login(username, password, address, true)) {
					handleException("Login failed", new InvalidLoginException("Invalid credentials"));
				}
			}
		} catch(Exception e) {
		  if(session != null) {
			try { session.logout(); } catch(Exception e2) {};
			session = null;
			handleException("Login failed", e);
		  }
		}

		return session;
	}

	protected EDFData sendAndRead(EDFData request) throws ProviderException {
		for(int attempt = 1; attempt <= 3; attempt++) {
			try {
				if(getSession() != null) {
					return getSession().sendAndRead(request);
				}
			} catch(InvalidLoginException e) {
				session = null;
				throw e;
			} catch(NoConnectionError e) {
				session = null;
				logger.error("No connection to UA", e);
			}
		}
		
		throw new ProviderException("Cannot connect to server");
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
	
	public void close() throws ProviderException {
		try {
			getSession().logout();
		} catch(NoConnectionError e) {
			logger.error("Cannot close session", e);
		} finally {
			session = null;
		}
	}
}

package org.ua2.apuawling;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.ua2.apuawling.edf.EDFClient;
import org.ua2.apuawling.edf.EDFProvider;
import org.ua2.clientlib.exception.NoConnectionError;

public class Session {
	private enum Mode { EDF, REDIS };

	private Mode mode = Mode.EDF;

	private static String edfHost;
	private static int edfPort;
	private static String edfUsername;
	private static String edfPassword;

	private final Map<String, IProvider> authMap = new ConcurrentHashMap<String, IProvider>();
	private static EDFClient edfCachingClient = null;

	private final int TIMEOUT_MINUTES = 10;
	
	private static final Logger logger = Logger.getLogger(Session.class);
	
	public static final Session INSTANCE = new Session();
	
	private Session() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						long timestamp = System.currentTimeMillis() - TIMEOUT_MINUTES * 60 * 1000;
						checkMap(authMap, timestamp);
						
						// Sleep for half the timeout
						Thread.sleep(30 * 1000 * TIMEOUT_MINUTES);
					} catch(Exception e) {
						logger.error("Cannot check sessions", e);
					}
				}
			}
			
		}).start();
	}
	
	private void checkMap(Map<String, IProvider> providers, long timestamp) {
		Set<String> keys = new HashSet<String>();
		
		for(Entry<String, IProvider> entry : providers.entrySet()) {
			if(entry.getValue().getLastRequest() < timestamp) {
				logger.info("Disconnecting session " + entry.getKey());
				try {
					entry.getValue().disconnect();
				} catch(Exception e) {
					if(e instanceof NoConnectionError) {
						// To be expected
					} else {
						logger.error("Problem during disconnect", e);
					}
				}
				keys.add(entry.getKey());
			}
		}
		
		for(String key : keys) {
			providers.remove(key);
		}
	}
	
	public void setEDFMode(String host, int port, String username, String password) {
		mode = Mode.EDF;

		edfHost = host;
		edfPort = port;
		edfUsername = username;
		edfPassword = password;
		
		if(edfUsername != null) {
			logger.debug("Creating default EDF provider");
			edfCachingClient = new EDFClient(edfHost, edfPort, edfUsername, edfPassword, null, Server.CLIENT + " v" + Server.VERSION) {
			
			};
		}
	}
	
	public String getMapKey(String username, InetAddress address, String client) {
		return username + "|" + address.getHostAddress() + "|" + client;
	}

	public synchronized IProvider getProvider(String username, String password, InetAddress address, String client) throws ProviderException {
		IProvider provider = null;
		
		provider = authMap.get(getMapKey(username, address, client));
		if(logger.isTraceEnabled()) logger.trace("Provider for auth " + getMapKey(username, address, client) + " is " + provider);
		
		if(provider == null) {
			if(username == null || password == null) {
				logger.debug("Cannot create a provider if username or password is null");
				return null;
			}
			
			if(Mode.EDF == mode) {
				logger.debug("Creating EDF provider for " + username + " from " + address + " using " + client);
				provider = new EDFProvider(edfHost, edfPort, username, password, address, client);
			}

			authMap.put(getMapKey(username, address, client), provider);
		}

		logger.debug("Provider for " + getMapKey(username, address, client) + " is " + provider);
		return provider;
	}
	
	public void removeProvider(IProvider provider) {
		String key = null;
		for(Entry<String, IProvider> entry : authMap.entrySet()) {
			if(entry.getValue() == provider) {
				key = entry.getKey();
				break;
			}
		}
		
		if(key != null) {
			logger.debug("Removing auth " + key + " as provider " + provider);
			authMap.remove(key);
		}
	}
}

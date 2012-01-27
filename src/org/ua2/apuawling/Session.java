package org.ua2.apuawling;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;
import org.ua2.apuawling.edf.EDFClient;
import org.ua2.apuawling.edf.EDFProvider;
import org.ua2.clientlib.exception.NoConnectionError;

public class Session {
	private enum Mode { EDF, REDIS };

	private Mode mode = Mode.EDF;

	private String edfHost;
	private int edfPort;
	private String edfUsername;
	private String edfPassword;

	private final Map<String, IProvider> authMap = new ConcurrentHashMap<String, IProvider>();
	private EDFClient edfCachingClient = null;

	private final int TIMEOUT_MINUTES = 30;

	private Housekeeper housekeeper = null;
	
	private static Session INSTANCE = null;
	
	private static final Logger logger = Logger.getLogger(Session.class);
	
	private class Housekeeper implements Runnable {
		private boolean loop = true;
		
		@Override
		public void run() {
			logger.info("Starting housekeeping");
			
			while(loop) {
				try {
					long timestamp = System.currentTimeMillis() - TIMEOUT_MINUTES * 60 * 1000;
					checkMap(authMap, timestamp);
					
					// Sleep for half the timeout
					try {
						Thread.sleep(30 * 1000 * TIMEOUT_MINUTES);
					} catch(InterruptedException e) {}
				} catch(Exception e) {
					logger.error("Cannot check sessions", e);
				}
			}
			
			logger.info("Stopping housekeeping");
		}
		
		public void shutdown() {
			loop = false;
		}
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
	
	private Session(String host, int port, String username, String password) {
		mode = Mode.EDF;
		edfHost = host;
		edfPort = port;
		edfUsername = username;
		edfPassword = password;
		
		if(edfUsername != null) {
			logger.info("Creating default EDF provider");
			edfCachingClient = new EDFClient(edfHost, edfPort, edfUsername, edfPassword, null, Server.CLIENT + " v" + Server.VERSION) {
			
			};
		}
		
		logger.info("Creating housekeeper");
		housekeeper = new Housekeeper();
	}
	
	public static Session getInstance() {
		return INSTANCE;
	}
	
	public static Session startEDF(String host, int port, String username, String password, ExecutorService executor) {
		logger.info("Starting in EDF mode");

		INSTANCE = new Session(host, port, username, password);
		
		if(executor != null) {
			logger.info("Submitting housekeeper to executor");
			executor.submit(INSTANCE.housekeeper);
		} else {
			Thread thread = new Thread(INSTANCE.housekeeper, "Housekeeper");
			thread.start();
		}
		
		return INSTANCE;
	}
	
	public String getMapKey(String username, InetAddress address, String client) {
		return username + "|" + client;
	}

	public synchronized IProvider getProvider(String username, String password, InetAddress address, String client) throws ActionException {
		IProvider provider = null;
		
		provider = authMap.get(getMapKey(username, address, client));
		if(logger.isTraceEnabled()) logger.trace("Provider for auth " + getMapKey(username, address, client) + " is " + provider);
		
		if(provider != null && !provider.getPassword().equals(password)) {
			removeProvider(provider);
			provider = null;
		}
		
		if(provider == null) {
			if(username == null || password == null) {
				logger.debug("Cannot create a provider if username or password is null");
				return null;
			}
			
			if(Mode.EDF == mode) {
				logger.info("Creating EDF provider for " + username + " from " + address + " using " + client);
				provider = new EDFProvider(edfHost, edfPort, username, password, address, client);
			}

			authMap.put(getMapKey(username, address, client), provider);
		}

		logger.debug("Provider for " + getMapKey(username, address, client) + " is " + provider);
		return provider;
	}
	
	public void removeProvider(IProvider provider) {
		provider.disconnect();
		
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
	
	public void shutdown() {
		for(IProvider provider : authMap.values()) {
			logger.info("Disconnecting " + provider);
			provider.disconnect();
		}
		
		if(edfCachingClient != null) {
			logger.info("Disconnecting EDF caching client " + edfCachingClient);
			edfCachingClient.disconnect();
		}
		
		if(housekeeper != null) {
			logger.info("Shutting down session thread");
			housekeeper.shutdown();
		}
	}
}

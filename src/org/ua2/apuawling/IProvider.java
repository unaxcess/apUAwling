package org.ua2.apuawling;

import java.net.InetAddress;

import org.ua2.json.JSONWrapper;

public interface IProvider {

	String getUsername();
	InetAddress getAddress();
	String getPassword();
	long getLastRequest();
	void disconnect();

	Object provide(String method, String path, JSONWrapper data) throws ProviderException;
}

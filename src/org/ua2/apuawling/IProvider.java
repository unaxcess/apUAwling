package org.ua2.apuawling;

import java.net.InetAddress;

public interface IProvider {

	String getUsername();
	InetAddress getAddress();
	long getLastRequest();
	void disconnect();

	Object provide(String method, String path, JSONWrapper data) throws ProviderException;
}

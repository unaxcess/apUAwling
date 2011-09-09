package org.ua2.apuawling;

import java.net.InetAddress;

public interface IProvider {

	String getUsername();
	InetAddress getAddress();
	String getPassword();
	long getLastRequest();
	void disconnect();
	
	Action<?> getAction(String method, String path);
}

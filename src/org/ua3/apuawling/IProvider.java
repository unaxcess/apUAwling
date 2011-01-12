package org.ua3.apuawling;

import java.net.InetAddress;

public interface IProvider {

	enum Command {
		FOLDER, FOLDERS, MESSAGE, SYSTEM, USER, USERS
	};
	
	String getUsername();
	InetAddress getAddress();
	long getLastRequest();
	void disconnect();

	Command[] getCommands() throws ProviderException;
	Object provide(String method, Command command, String[] parameters, JSONWrapper data) throws ProviderException;
}

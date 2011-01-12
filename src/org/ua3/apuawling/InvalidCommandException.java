package org.ua3.apuawling;

@SuppressWarnings("serial")
public class InvalidCommandException extends ProviderException {
	public InvalidCommandException(String msg) {
		super(msg);
	}
}

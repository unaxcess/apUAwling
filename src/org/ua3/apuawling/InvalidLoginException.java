package org.ua3.apuawling;

@SuppressWarnings("serial")
public class InvalidLoginException extends ProviderException {
	public InvalidLoginException(String msg) {
		super(msg);
	}
}

package org.ua2.apuawling;

@SuppressWarnings("serial")
public class ProviderException extends Exception {
	public ProviderException(String msg) {
		super(msg);
	}

	public ProviderException(String msg, Throwable cause) {
		super(msg, cause);
	}
}

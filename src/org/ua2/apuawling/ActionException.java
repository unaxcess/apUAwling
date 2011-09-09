package org.ua2.apuawling;

@SuppressWarnings("serial")
public class ActionException extends Exception {
	public ActionException(String msg) {
		super(msg);
	}

	public ActionException(String msg, Throwable cause) {
		super(msg, cause);
	}
}

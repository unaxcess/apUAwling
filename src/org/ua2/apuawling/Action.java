package org.ua2.apuawling;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.ua2.json.JSONWrapper;

public abstract class Action<T> {
	
	private final String method;
	private final Pattern pattern;

	protected enum Method {
		GET,
		POST,
	};

	protected static final String NAME_PATTERN = "[a-zA-Z]([\\w\\.,'@\\-! ]{0,18}[\\w\\.,'@\\-!])?";
	protected static final String ID_PATTERN = "[0-9]+";
	
	private static final Logger logger = Logger.getLogger(Action.class);
	
	public Action(Method method, String path) {
		this.method = method.name();
		
		path = "^/" + path + "$";
		this.pattern = Pattern.compile(path);
	}
	
	public String getMethod() {
		return method;
	}
	
	public String getPath() {
		return pattern.pattern();
	}
	
	public boolean isMatch(String method, String path) {
		boolean match = this.method.equals(method) && this.pattern.matcher(path).find();
		logger.trace(this.method + " -vs- " + method + " " + path + " -vs- " + pattern.pattern() + " -> " + match);
		return match;
	}
	
	public abstract T perform(List<String> elements, JSONWrapper data) throws ActionException, ProviderException;
}

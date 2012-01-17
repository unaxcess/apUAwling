package org.ua2.apuawling.edf;

import org.apache.log4j.Logger;
import org.ua2.edf.EDFData;

public class EDFActionWrapper<T> {
	private T json;
	private EDFData request;
	private EDFData reply;
	
	public EDFActionWrapper(T json, EDFData request, EDFData reply) {
		Logger.getLogger(getClass()).debug("Creating EDF action wrapper json=" + json.toString() + " request=" + request.format(false) + " reply=" + reply.format(false));
		
		this.json = json;
		this.request = request;
		this.reply = reply;
	}
	
	public T getJSON() {
		return json;
	}
	
	public void setJSON(T json) {
		this.json = json;
	}
	
	public EDFData getRequest() {
		return request;
	}
	
	public void setRequest(EDFData request) {
		this.request = request;
	}
	
	public EDFData getReply() {
		return reply;
	}
	
	public void setReply(EDFData reply) {
		this.reply = reply;
	}
}

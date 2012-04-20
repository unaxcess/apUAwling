package org.ua2.apuawling;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class WorkerResponse implements HttpServletResponse {

	private OutputStream stream;
	private StringWriter content;
	private PrintWriter writer;

	private int status;
	private String message;
	private Map<String, String> headers = new TreeMap<String, String>();

	private static final String CRLF = "\r\n";

	private static final Logger logger = Logger.getLogger(WorkerResponse.class);

	public WorkerResponse(OutputStream stream) {
		this.stream = stream;
		this.content = new StringWriter();
		this.writer = new PrintWriter(content);
	}

	@Override
	public void flushBuffer() throws IOException {
		PrintWriter flusher = new PrintWriter(stream);
		
		logger.trace("Sending status " + status + " " + message);
		flush(flusher, "HTTP/1.0 " + status + (message != null ? " " + message : ""), true);

		for(Entry<String, String> entry : headers.entrySet()) {
			logger.trace("Setting header " + entry.getKey() + " to " + entry.getValue());
			flush(flusher, entry.getKey() + ": " + entry.getValue(), true);
		}
		
		flush(flusher, null, true);
		
		flush(flusher, content.toString(), false);

		flusher.close();
	}
	
	private void flush(PrintWriter flusher, String msg, boolean appendCRLF) {
		if(msg != null) {
			logger.trace("Sending " + msg);
			flusher.print(msg);
		}
		
		if(appendCRLF) {
			flusher.print(CRLF);
		}
	}

	@Override
	public int getBufferSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getCharacterEncoding() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContentType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Locale getLocale() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return writer;
	}

	@Override
	public boolean isCommitted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	@Override
	public void resetBuffer() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setBufferSize(int arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setCharacterEncoding(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setContentLength(int length) {
		setHeader("Content-Lenth", Integer.toString(length));
	}

	@Override
	public void setContentType(String type) {
		setHeader("Content-Type", type);
	}

	@Override
	public void setLocale(Locale arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addCookie(Cookie arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addDateHeader(String arg0, long arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addHeader(String arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addIntHeader(String arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean containsHeader(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String encodeRedirectURL(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String encodeRedirectUrl(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String encodeURL(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String encodeUrl(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getHeader(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> getHeaderNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> getHeaders(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getStatus() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void sendError(int status) throws IOException {
		sendError(status, null);
	}

	@Override
	public void sendError(int status, String message) throws IOException {
		if(message == null) {
			if(status == 200) {
				message = "OK";
			} else if(status == 400) {
				message = "Bad Request";
			} else if(status == 401) {
				message = "Unauthorized";
			} else if(status == 500) {
				message = "Internal server error";
			}
		}

		logger.trace("Setting error " + status + " " + message);
		this.status = status;
		this.message = message;

		//flushBuffer(); 
	}

	@Override
	public void sendRedirect(String arg0) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDateHeader(String arg0, long arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setHeader(String name, String value) {
		headers.put(name, value);
	}

	@Override
	public void setIntHeader(String arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStatus(int status) {
		this.status = status;
	}

	@Override
	public void setStatus(int arg0, String arg1) {
		// TODO Auto-generated method stub

	}
}

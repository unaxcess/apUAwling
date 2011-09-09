/**
 * HTTP request service class
 * 
 * Based on http://patriot.net/~tvalesky/TeenyWeb.java
 * 
 * @author techno
 *
 */
package org.ua2.apuawling;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;

import org.apache.log4j.Logger;

public class Worker {
	private String prefix;
	private BufferedReader reader;
	
	private WorkerRequest req;
	private WorkerResponse resp;

	private static final Logger logger = Logger.getLogger(Server.class);
	
	public Worker(String prefix, InetAddress address, InputStream input, OutputStream output) {
		this.prefix = prefix;
		this.reader = new BufferedReader(new InputStreamReader(input));

		req = new WorkerRequest();
		resp = new WorkerResponse(output);
	}

	public void work() {
		long start = System.currentTimeMillis();
		logger.trace("Starting work");
		
		int contentLength = 0;

		try {
			logger.trace("Reading headers..");
			String line = null;
			do {
				line = reader.readLine();
				if(line != null) {
					logger.trace("Read line " + line);
					if(req.getPathInfo() == null) {
						String[] fields = line.split(" ");
						req.setMethod(fields[0]);
						String pathInfo = fields[1];
						if(pathInfo.startsWith(prefix)) {
							pathInfo = pathInfo.substring(prefix.length());
						}
						req.setPathInfo(pathInfo);
					} else if(line != null) {
						int colon = line.indexOf(": ");
						if(colon > 0) {
							String name = line.substring(0, colon);
							String value = line.substring(colon + 2);
	
							if(name.equals("Content-Length")) {
								contentLength = Integer.parseInt(value);
							} else {
								req.setHeader(name, value);
							}
						}
					}
				}
			} while(line != null && line.length() > 0);
			
			if(contentLength > 0) {
				logger.trace("Reading " + contentLength + " bytes of content...");
				char[] chars = new char[100];
				while(req.getContentLength() < contentLength) {
					int read = reader.read(chars, 0, chars.length);
					if(read == chars.length) {
						req.append(chars);
					} else if(read > 0) {
						char[] temp = new char[read];
						System.arraycopy(chars, 0, temp, 0, read);
						temp = chars;
						req.append(temp);
					}
				}
			}

			JSONServlet servlet = new JSONServlet();
			
			if(req.getContentLength() == 0) {
				logger.debug("Getting " + req.getServletPath());
				servlet.doGet(req, resp);
			} else {
				logger.debug("Posting " + req.getServletPath());
				servlet.doPost(req, resp);
			}
			
			resp.flushBuffer();
			
		} catch(Exception e) {
			String msg = "Error while servicing request " + req.getServletPath();
			logger.error(msg, e);
		}
		
		long diff = System.currentTimeMillis() - start;
		logger.trace("Ending work after " + diff + "ms");
	}
}

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
import java.io.PrintStream;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Worker {
	private InetAddress address;
	private BufferedReader reader;
	private PrintStream stream;

	private static final String TEXT_TYPE = "text/html";
	private static final String COOKIE_NAME = "ua3session";
	private static final String CRLF = "\r\n";

	private static final DateFormat NO_CACHE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

	private static final Logger logger = Logger.getLogger(Server.class);

	public Worker(InetAddress address, InputStream input, OutputStream output) {
		this.address = address;
		this.reader = new BufferedReader(new InputStreamReader(input));
		this.stream = new PrintStream(output);
	}

	private void appendFile(String filename, boolean pre, StringBuilder sb) {
		try {
			InputStream fileStream = getClass().getClassLoader().getResourceAsStream(filename);
			if(fileStream == null) {
				return;
			}
			
			BufferedReader fileReader = new BufferedReader(new InputStreamReader(fileStream));
			
			sb.append("<hr>\n");
			
			if(pre) {
				sb.append("<pre>\n");
			}

			String line = null;
			while((line = fileReader.readLine()) != null) {
				sb.append(line);
				if(pre) {
					sb.append("\n");
				} else {
					sb.append("<br>\n");
				}
			}
			
			if(pre) {
				sb.append("</pre>\n");
			} else {
				sb.append("<br>\n");
			}

			fileStream.close();
		} catch(Exception e) {
			logger.error("Cannot load " + filename, e);
		}
	}

	private void sendHelp(String sessionId, IProvider provider) {
		Properties buildProps = new Properties();
		try {
			buildProps.load(getClass().getClassLoader().getResourceAsStream("build.properties"));
		} catch(Exception e) {
			logger.error("Cannot load build.properties", e);
		}
		
		// Prepare some HTML with version name, connection details etc
		StringBuilder sb = new StringBuilder();

		sb.append("<html>\n");

		sb.append("<head>\n");
		sb.append("<title>apUAwling</title>\n");
		sb.append("</head>\n");

		sb.append("<h1>A Prototype UA Web LINkage Gateway</h1>\n");
		sb.append("<h2>Version " + Server.VERSION + " (build " + buildProps.getProperty("buildNum") + " at " + buildProps.getProperty("buildTime") + ")</h2>\n");

		sb.append("URLs available:<br>\n");
		sb.append("<ul>\n");
		/* try {
			for(Command command : provider.getCommands()) {
				sb.append("<li>/" + command.name().toLowerCase() + "/</li>\n");
			}
		} catch(ProviderException e) {
			logger.error("Could not get commands", e);
		} */
		sb.append("</ul>\n");

		appendFile("README.txt", false, sb);
		appendFile("CHANGELOG.txt", true, sb);

		sb.append("</body>\n");
		sb.append("\n</html>");

		sendContent(sessionId, sb.toString());
	}
	
	private void sendAuth() {
		sendError(null, 401, null);
		sendHeader("WWW-Authenticate", "Basic realm=\"ua3\"");
	}

	private void processRequest(String sessionId, Map<String, String> headers, String request, JSONWrapper data, String auth) throws ProviderException, JSONException {
		logger.debug("Request " + request + " in session " + sessionId);

		if(logger.isTraceEnabled()) {
			logger.trace("Headers:");
			for(Entry<String, String> header : headers.entrySet()) {
				logger.trace("  " + header.getKey() + "," + header.getValue());
			}
			if(data != null) {
				logger.trace("Request data:\n" + data);
			}
		}

		String username = null;
		String password = null;
		if(auth != null) {
			// Authorisation is username:password in BASE64
			auth = new String(Base64.decodeBase64(auth.getBytes()));

			int colon = auth.indexOf(":");
			if(colon > 0) {
				username = auth.substring(0, colon);
				password = auth.substring(colon + 1);
			}
		}
		
		String userAgent = headers.get("User-Agent");
		
		IProvider provider = null;
		synchronized(Session.INSTANCE) {
			sessionId = Session.INSTANCE.getSessionId(sessionId, username, password, address, userAgent);

			provider = Session.INSTANCE.getProvider(sessionId);
		}

		boolean isBrowser = false;
		if(userAgent != null && userAgent.startsWith("Mozilla")) {
			isBrowser = true;
		}

		String[] fields = request.split(" ");
		if(fields.length == 3) {
			String method = fields[0];
			String path = fields[1];

			if(path.equals("/")) {
				sendHelp(sessionId, provider);
			} else if(provider != null) {
				// Strip trailing slashes
				while(path.endsWith("/")) {
					path = path.substring(0, path.length() - 1);
				}

				try {
					logger.debug("Asking provider to provide " + method + " on " + path);
					sendContent(sessionId, provider.provide(method, path, data), isBrowser);
				} catch(InvalidLoginException e) {
					Session.INSTANCE.remove(sessionId);
					sendAuth();
				} catch(ObjectNotFoundException e) {
					sendError(sessionId, 404, e.getMessage());
				}
			} else {
				sendAuth();
			}
		} else {
			String msg = "Malformed request " + request;
			logger.error(msg);
			sendError(sessionId, 400, msg);
		}
	}

	public void work() {
		long start = System.currentTimeMillis();
		logger.trace("Starting work");
		
		String request = null;
		String sessionId = null;

		try {
			Map<String, String> headers = new HashMap<String, String>();
			String auth = null;
			int contentLength = 0;
			StringBuilder content = new StringBuilder();

			logger.trace("Reading headers..");
			String line = null;
			do {
				line = reader.readLine();
				if(request == null) {
					request = line;
				} else if(line != null) {
					int colon = line.indexOf(": ");
					if(colon > 0) {
						String name = line.substring(0, colon);
						String value = line.substring(colon + 2);

						if(name.equals("Cookie")) {
							int equals = value.indexOf("=");
							if(equals > 0) {
								String cookieName = value.substring(0, equals);
								String cookieValue = value.substring(equals + 1);

								if(COOKIE_NAME.equals(cookieName)) {
									logger.debug("Found UA3 session cookie header " + line);
									sessionId = cookieValue;
								} else {
									headers.put(name, value);
								}
							} else {
								logger.warn("Invalid cookie header " + line);
							}
						} else if(name.equals("Authorization")) {
							logger.debug("Found auth " + value);
							int space = value.indexOf(" ");
							if(space > 0) {
								auth = value.substring(space + 1);
							}
						} else if(name.equals("Content-Length")) {
							contentLength = Integer.parseInt(value);
						} else {
							headers.put(name, value);
						}
					}
				}
			} while(line != null && line.length() > 0);
			
			logger.trace("Reading " + contentLength + " bytes of content...");
			while(content.length() < contentLength) {
				char[] chars = new char[100];
				int read = reader.read(chars, 0, chars.length);
				if(read > 0) {
					content.append(chars);
				}
			}

			boolean process = true;
			JSONWrapper data = null;
			if(content.length() > 0) {
				try {
					data = JSONWrapper.parse(content.toString());
				} catch(JSONException e) {
					logger.error("Cannot parse content: " + content.toString(), e);
					sendError(sessionId, 500, "Cannot parse content as JSON");
					process = false;
				}
			}
			
			if(process) {
				processRequest(sessionId, headers, request, data, auth);
			}
		} catch(Exception e) {
			String msg = "Error while servicing request " + request;
			logger.error(msg, e);
			msg += ". " + e.getMessage();
			sendError(sessionId, 500, msg);
		}
		
		long diff = System.currentTimeMillis() - start;
		logger.trace("Ending work after " + diff + "ms");
	}

	private void print(String line) {
		stream.print(line);
		stream.print(CRLF);
	}
	
	private void sendHeader(String name, String value) {
		logger.trace("Setting header " + name + " to " + value);
		print(name + ": " + value);
	}
	
	private void sendResponse(String sessionId, int code, String type, String content) {
		logger.trace("Sending response " + code + " to " + sessionId);

		String message = "Unknown";
		if(code == 200) {
			message = "OK";
		} else if(code == 400) {
			message = "Bad Request";
		} else if(code == 401) {
			message = "Unauthorized";
		} else if(code == 500) {
			message = "Internal server error";
		}

		print("HTTP/1.0 " + code + " " + message);

		if(sessionId != null) {
			sendHeader("Set-Cookie", COOKIE_NAME + "=" + sessionId);
		}

		if(content != null) {
			logger.trace("Sending content as " + type + ":\n" + content);
			
			sendHeader("Pragma", "no-cache");
			sendHeader("Cache-Control", "no-cache");
			Date now = new Date();
			sendHeader("Expires", NO_CACHE_FORMAT.format(now));

			sendHeader("Content-Type", type);

			byte[] bytes = content.getBytes();
			sendHeader("Content-Length", Integer.toString(bytes.length));
			
			print("");

			print(content);
		}
	}
	
	private void sendError(String sessionId, int code, String content) {
		sendResponse(sessionId, code, TEXT_TYPE, content);
	}

	private void sendContent(String sessionId, String content) {
		sendResponse(sessionId, 200, TEXT_TYPE, content);
	}

	private void sendContent(String sessionId, String content, boolean isBrowser) {
		String type = TEXT_TYPE;
		if(!isBrowser) {
			type = "application/json";
		}

		sendResponse(sessionId, 200, type, content);
	}
	
	private void sendContent(String sessionId, Object obj, boolean isBrowser) throws JSONException {
		String str = "";

		if(obj != null) {
			if(isBrowser) {
				if(obj instanceof JSONObject) {
					JSONObject json = (JSONObject)obj;
					str = json.toString(2);
				} else if(obj instanceof JSONArray) {
					JSONArray json = (JSONArray)obj;
					str = json.toString(2);
				} else {
					str = obj.toString();
				}
				str = "<html>\n<body>\n<pre>\n" + str + "\n</pre>\n</body>\n</html>";
			} else {
				str = obj.toString();
			}
		}
		
		sendContent(sessionId, str, isBrowser);
	}
}

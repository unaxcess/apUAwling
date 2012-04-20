package org.ua2.apuawling;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ua2.apuawling.edf.EDFActionWrapper;
import org.ua2.edf.EDFData;
import org.ua2.json.JSONWrapper;

@SuppressWarnings("serial")
public class JSONServlet extends HttpServlet {

	private static final String TEXT_TYPE = "text/html";
	private static final String UTF8 = "UTF-8";

	private static final String START_TAG = "\n<pre>\n";
	private static final String END_TAG  = "\n</pre>\n\n";

	private static final Logger logger = Logger.getLogger(JSONServlet.class);

	private void sendHelp(HttpServletResponse resp) throws IOException {
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
		/*
		 * try { for(Command command : provider.getCommands()) {
		 * sb.append("<li>/" + command.name().toLowerCase() + "/</li>\n"); } }
		 * catch(ProviderException e) { logger.error("Could not get commands",
		 * e); }
		 */
		sb.append("</ul>\n");

		Utils.appendFile("README.txt", false, sb);
		Utils.appendFile("CHANGELOG.txt", true, sb);

		sb.append("</body>\n");
		sb.append("\n</html>");

		sendContent(resp, sb.toString());
	}

	private void sendAuthRequired(HttpServletResponse resp) throws IOException {
		resp.setHeader("WWW-Authenticate", "Basic realm=\"uaJSON\"");
		resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
	}

	private void processRequest(HttpServletRequest req, HttpServletResponse resp, JSONWrapper data, String auth) throws ServletException, IOException, ActionException, JSONException {
		logger.info("Request " + req.getMethod() + " " + req.getPathInfo() + " from " + req.getRemoteHost() + " " + req.getRemoteAddr());

		String method = req.getMethod();
		String path = req.getPathInfo();

		if(logger.isTraceEnabled()) {
			logger.trace("Headers:");
			Enumeration<String> names = req.getHeaderNames();
			while(names.hasMoreElements()) {
				String key = names.nextElement();
				logger.trace("  " + key + "," + req.getHeader(key));
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

		if(username != null) {
			NDC.push(username);
		}

		try {

			String userAgent = req.getHeader("User-Agent");
			InetAddress proxyAddress = InetAddress.getByAddress(req.getRemoteHost(), new byte[4]);
			if(req.getHeader("X-Forwarded-For") != null) {
				try {
					String forwardedFor = req.getHeader("X-Forwarded-For");
					if(forwardedFor.indexOf(",") > 0) {
						String[] fields = forwardedFor.split(",");
						forwardedFor = fields[fields.length - 1].trim();
					}
					if(forwardedFor.length() > 0 && Character.isDigit(forwardedFor.charAt(0))) {
						proxyAddress = InetAddress.getByAddress(forwardedFor, Utils.getAddressBytes(forwardedFor));
						logger.debug("Created proxy address " + proxyAddress);
					}
				} catch(Exception e) {
					logger.error("Problem with proxy address", e);
				}
			}

			boolean debug = false;
			
			List<String> parameters = new ArrayList<String>();
			StringBuilder sb = new StringBuilder();
			String[] fieldArray = path.split("/");
			for(int fieldNum = 0; fieldNum < fieldArray.length; fieldNum++) {
				String field = fieldArray[fieldNum];
				if(fieldNum == 1 && (field.equals("browse") || field.equals("debug"))) {
					debug = true;
				} else if(field.trim().length() > 0) {
					parameters.add(field);
					sb.append("/");
					sb.append(field);
				}
			}
			
			path = sb.toString();

			IProvider provider = Session.getInstance().getProvider(username, password, proxyAddress, userAgent);

			if(parameters.size() == 0) {
				sendHelp(resp);
			} else if(provider != null) {
				try {
					Action<?> action = provider.getAction(method, path);
					if(action != null) {
						logger.debug("Asking provider to provide " + method + " on " + path);
						sendContent(resp, action.perform(parameters, data), debug);
					} else {
						sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Cannot find action for " + path);
					}
				} catch(InvalidLoginException e) {
					Session.getInstance().removeProvider(provider);
					sendAuthRequired(resp);
				} catch(ObjectNotFoundException e) {
					sendError(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
				} catch(InvalidActionException e) {
					sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
				} catch(Exception e) {
					logger.error("Cannot perform action", e);
					sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
				}
			} else {
				sendAuthRequired(resp);
			}
		} finally {
			if(username != null) {
				NDC.pop();
			}
		}
	}

	private void sendResponse(HttpServletResponse resp, int code, String message, String type, String content) throws IOException {
		logger.trace("Sending response " + code);

		if(code == HttpServletResponse.SC_OK) {
			resp.setStatus(code);
		} else { 
			if(message == null) {
				message = "Unknown";
				if(code == HttpServletResponse.SC_BAD_REQUEST) {
					message = "Bad Request";
				} else if(code == HttpServletResponse.SC_UNAUTHORIZED) {
					message = "Unauthorized";
				} else if(code == HttpServletResponse.SC_NOT_FOUND) {
					message = "Not found";
				} else if(code == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
					message = "Internal server error";
				}
			}
			
			resp.sendError(code, message);
		}

		resp.setHeader("Pragma", "no-cache");
		resp.setHeader("Cache-Control", "no-cache");
		resp.setDateHeader("Expires", new Date().getTime());

		if(content != null) {
			logger.trace("Sending content as " + type + ":\n" + content);

			resp.setContentType(type + "; charset=" + UTF8); 
			resp.setCharacterEncoding(UTF8); 

			byte[] bytes = content.getBytes();
			resp.setContentLength(bytes.length);

			resp.getWriter().print(content);
		}
	}

	private void sendError(HttpServletResponse resp, int code, String message) throws IOException {
		sendResponse(resp, code, message, TEXT_TYPE, "An error has occurred. Check the status message for details");
	}

	private void sendContent(HttpServletResponse resp, String content) throws IOException {
		sendResponse(resp, HttpServletResponse.SC_OK, null, TEXT_TYPE, content);
	}
	
	private void append(String str, boolean debug, StringBuilder sb) {
		if(debug) sb.append(START_TAG);
		sb.append(str);
		if(debug) sb.append(END_TAG);
	}

	private void convertObject(Object obj, boolean debug, StringBuilder sb) throws JSONException {
		if(obj instanceof JSONObject) {
			JSONObject json = (JSONObject)obj;
			append(json.toString(2), debug, sb);
			
		} else if(obj instanceof JSONArray) {
			JSONArray json = (JSONArray)obj;
			append(json.toString(2), debug, sb);
			
		} else {
			append(obj.toString(), debug, sb);
		}
	}
	
	private String escapeEDF(EDFData data) {
		String dataStr = data.format(true);
		dataStr = dataStr.replaceAll("<", "&lt;");
		dataStr = dataStr.replaceAll(">", "&gt;");
		return dataStr;
	}
	
	private void sendContent(HttpServletResponse resp, Object obj, boolean debug) throws JSONException, IOException {
		String type = TEXT_TYPE;
		if(!debug) {
			type = "application/json";
		}

		String content = "";

		if(obj != null) {
			StringBuilder sb = new StringBuilder();
			if(debug) {
				sb.append("<html>\n");
				sb.append("<body>\n");
				
				if(obj instanceof EDFActionWrapper) {
					EDFActionWrapper wrapper = (EDFActionWrapper)obj;
					
					sb.append("JSON:<br>\n");
					convertObject(wrapper.getJSON(), debug, sb);
					sb.append("<hr>\n");
					
					sb.append("EDF request:<br>\n");
					append(escapeEDF(wrapper.getRequest()), debug, sb);

					sb.append("EDF reply:<br>\n");
					append(escapeEDF(wrapper.getReply()), debug, sb);

				} else {
					append(obj.toString(), debug, sb);
				}
				
				sb.append("</body>\n");
				sb.append("</html>\n");
				
				content = sb.toString();
				
			} else {
				if(obj instanceof EDFActionWrapper) {
					EDFActionWrapper wrapper = (EDFActionWrapper)obj;
					content = wrapper.getJSON().toString();
					
				} else {
					content = obj.toString();
					
				}
			}
		}

		sendResponse(resp, HttpServletResponse.SC_OK, null, type, content);
	}

	private void processRequest(HttpServletRequest req, HttpServletResponse resp, JSONWrapper data) throws ServletException, IOException {
		try {
			// Get Authorization header
			String auth = req.getHeader("Authorization");

			if(auth != null && auth.toUpperCase().startsWith("BASIC ")) {
				processRequest(req, resp, data, auth.substring(6));
			} else {
				sendAuthRequired(resp);
			}
		} catch(ServletException e) {
			throw e;
		} catch(IOException e) {
			throw e;
		} catch(Exception e) {
			throw new ServletException("Error processing request", e);
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		processRequest(req, resp, null);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		StringBuilder content = new StringBuilder();
		int contentLength = req.getContentLength();
		BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
		logger.trace("Reading " + contentLength + " bytes of content...");
		char[] chars = new char[1000];
		while(content.length() < contentLength) {
			int read = reader.read(chars, 0, chars.length);
			if(read == chars.length) {
				content.append(chars);
			} else if(read > 0) {
				char[] temp = new char[read];
				System.arraycopy(chars, 0, temp, 0, read);
				temp = chars;
				content.append(temp);
			}
		}

		boolean process = true;
		JSONWrapper data = null;
		if(content.length() > 0) {
			try {
				data = JSONWrapper.parse(content.toString());
			} catch(JSONException e) {
				logger.error("Cannot parse content: " + content.toString(), e);
				resp.sendError(500, "Cannot parse content as JSON");
				process = false;
			}
		}

		if(process) {
			processRequest(req, resp, data);
		}
	}
}

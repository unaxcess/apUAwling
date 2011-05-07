package org.ua2.apuawling;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.Date;
import java.util.Enumeration;
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
import org.ua2.json.JSONWrapper;

@SuppressWarnings("serial")
public class JSONServlet extends HttpServlet {

	private static final String TEXT_TYPE = "text/html";

	private static final Logger logger = Logger.getLogger(JSONServlet.class);

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

		appendFile("README.txt", false, sb);
		appendFile("CHANGELOG.txt", true, sb);

		sb.append("</body>\n");
		sb.append("\n</html>");

		sendContent(resp, sb.toString());
	}

	private void sendAuthRequired(HttpServletResponse resp) throws IOException {
		resp.setHeader("WWW-Authenticate", "Basic realm=\"uaJSON\"");
		resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
	}

	private void processRequest(HttpServletRequest req, HttpServletResponse resp, JSONWrapper data, String auth) throws ServletException, IOException, ProviderException, JSONException {
		logger.info("Request " + req.getMethod() + " " + req.getPathInfo());

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

			logger.info("Creating inet address from remote data " + req.getRemoteHost() + " " + req.getRemoteAddr());
			//InetAddress proxyAddress = InetAddress.getByAddress(req.getRemoteHost(), Worker.getAddressBytes(req.getRemoteAddr()));
			InetAddress proxyAddress = InetAddress.getByAddress(req.getRemoteHost(), new byte[4]);
			if(req.getHeader("X-Forwarded-For") != null) {
				try {
					String forwardedFor = req.getHeader("X-Forwarded-For");
					if(forwardedFor.indexOf(",") > 0) {
						String[] fields = forwardedFor.split(",");
						forwardedFor = fields[fields.length - 1].trim();
					}
					if(forwardedFor.length() > 0 && Character.isDigit(forwardedFor.charAt(0))) {
						proxyAddress = InetAddress.getByAddress(forwardedFor, Worker.getAddressBytes(forwardedFor));
						logger.info("Created proxy address " + proxyAddress);
					}
				} catch(Exception e) {
					logger.error("Problem with proxy address", e);
				}
			}

			IProvider provider = Session.INSTANCE.getProvider(username, password, proxyAddress, userAgent);

			boolean isBrowser = false;

			if(path.startsWith("/browse")) {
				isBrowser = true;
				path = path.substring(7);
			}

			// Strip possible multiple slashes read for path split later
			path = path.replaceAll("/+", "/");

			if(path.equals("/")) {
				sendHelp(resp);
			} else if(provider != null) {
				try {
					logger.debug("Asking provider to provide " + method + " on " + path);
					sendContent(resp, provider.provide(method, path, data), isBrowser);
				} catch(InvalidLoginException e) {
					Session.INSTANCE.removeProvider(provider);
					sendAuthRequired(resp);
				} catch(ObjectNotFoundException e) {
					sendError(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
				} catch(InvalidCommandException e) {
					sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
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

	private void sendResponse(HttpServletResponse resp, int code, String type, String content) throws IOException {
		logger.trace("Sending response " + code);

		if(code == HttpServletResponse.SC_OK) {
			resp.setStatus(code);
		} else {
			String message = "Unknown";
			if(code == HttpServletResponse.SC_BAD_REQUEST) {
				message = "Bad Request";
			} else if(code == HttpServletResponse.SC_UNAUTHORIZED) {
				message = "Unauthorized";
			} else if(code == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
				message = "Internal server error";
			}

			resp.sendError(code, message);
		}

		if(content != null) {
			logger.trace("Sending content as " + type + ":\n" + content);

			resp.setHeader("Pragma", "no-cache");
			resp.setHeader("Cache-Control", "no-cache");
			Date now = new Date();
			resp.setDateHeader("Expires", now.getTime());

			resp.setContentType(type);

			byte[] bytes = content.getBytes();
			resp.setContentLength(bytes.length);

			resp.getOutputStream().print(content);
		}
	}

	private void sendError(HttpServletResponse resp, int code, String content) throws IOException {
		sendResponse(resp, code, TEXT_TYPE, content);
	}

	private void sendContent(HttpServletResponse resp, String content) throws IOException {
		sendResponse(resp, HttpServletResponse.SC_OK, TEXT_TYPE, content);
	}

	private void sendContent(HttpServletResponse resp, String content, boolean isBrowser) throws IOException {
		String type = TEXT_TYPE;
		if(!isBrowser) {
			type = "application/json";
		}

		sendResponse(resp, HttpServletResponse.SC_OK, type, content);
	}

	private void sendContent(HttpServletResponse resp, Object obj, boolean isBrowser) throws JSONException, IOException {
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

		sendContent(resp, str, isBrowser);
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
		if("/".equals(req.getPathInfo())) {
			sendHelp(resp);
		} else {
			processRequest(req, resp, null);
		}
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

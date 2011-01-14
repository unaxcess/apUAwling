package org.ua3.apuawling;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TestJSONClient {
	
	private final URL url;
	
	private final AbstractHttpClient client;
	
	private static final Logger logger = Logger.getLogger(TestJSONClient.class);
	
	public TestJSONClient(String url, String username, String password) throws MalformedURLException {
		this.url = new URL(url);
		
		this.client = new DefaultHttpClient();

		CredentialsProvider provider = new BasicCredentialsProvider();
		provider.setCredentials(
				new AuthScope(this.url.getHost(), AuthScope.ANY_PORT, "ua3"),
				new UsernamePasswordCredentials(username, password));
		
		this.client.setCredentialsProvider(provider);
	}
	
	private JSONWrapper send(String path) throws ParseException, IOException, JSONException {
		return send(path, null);
	}
	
	private JSONWrapper send(String path, JSONWrapper data) throws ParseException, IOException, JSONException {
		HttpUriRequest request = null;
		
		path = url.toExternalForm() + path;
		if(data != null) {
			logger.info("Creating POST " + path);
			HttpPost post = new HttpPost(path);
			HttpEntity entity = new StringEntity(data.toString());
			post.setEntity(entity);
			
			request = post;
		} else {
			logger.info("Creating GET " + path);
			HttpGet get = new HttpGet(path);
			
			request = get;
		}
		
		HttpResponse response = client.execute(request);
		HttpEntity entity = response.getEntity();
		if(entity == null) {
			throw new IOException("No entity in response");
		}
		
		String content = EntityUtils.toString(entity);
		logger.info("Response: " + content);
		return JSONWrapper.parse(content);
	}
	
	public static void main(String[] args) {
		try {
			TestJSONClient client = new TestJSONClient("https://ua2.org/uaJSON", "crackpot", "mad");
			
			JSONWrapper result = client.send("/folder/private/full");
			if(result != null) {
				//logger.info("Result:\n" + result.toString(2));
				
				int limits[] = new int[4];
				JSONArray messages = result.getArray();
				logger.info("Found " + messages.length() + " messages");
				for(int index = 0; index < messages.length(); index++) {
					int limitNum = 0;
					
					JSONObject message = messages.getJSONObject(index);
					
					message.put("_type", "add/message/full");
					
					if(message.toString().length()<= 256) {
						limits[limitNum]++;
					}
					limitNum++;

					message.put("_type", "add/message");

					if("Diverted Page".equals(message.getString("subject"))) {
						message.remove("subject");
					}
					if(message.toString().length()<= 256) {
						limits[limitNum]++;
					}
					limitNum++;
					
					message.remove("subject");
					if(message.toString().length()<= 256) {
						limits[limitNum]++;
					}
					limitNum++;

					message.remove("body");
					if(message.toString().length()<= 256) {
						limits[limitNum]++;
					}
					limitNum++;
				}

				String limitStr = "";
				for(int limit : limits) {
					if(limitStr.length() > 0) { 
						limitStr += " / ";
					}
					limitStr += limit;
				}
				logger.info("Messages within payload limit: " + limitStr);
			} else {
				logger.error("No result for private message list");
			}
		} catch (Exception e) {
			logger.error("Error during request", e);
		}
	}
}

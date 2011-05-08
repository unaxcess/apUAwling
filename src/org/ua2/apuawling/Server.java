/**
 * UA2 HTTP API server
 * 
 * Based on http://patriot.net/~tvalesky/TeenyWeb.java
 * 
 * @author techno
 */
package org.ua2.apuawling;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

public class Server implements Runnable {

	public static final String CLIENT = "apUAwling";
	public static final String VERSION = "0.7e";

	private ServerSocket listener;
	private ExecutorService service;

	private static Properties config = new Properties();

	private static final Logger logger = Logger.getLogger(Server.class);

	public Server(String filename) throws IOException {
		config.load(new FileInputStream(filename));

		if("edf".equalsIgnoreCase(config.getProperty("mode"))) {
			Session.INSTANCE.startupEDF(
					config.getProperty("edf.host"), Integer.parseInt(config.getProperty("edf.port")),
					config.getProperty("edf.username"), config.getProperty("edf.password")
			);
		}
		
		int port = Integer.parseInt(config.getProperty("port"));
		int numThreads = Integer.parseInt(config.getProperty("numThreads", "3"));

		logger.info("Creating listener on port " + port);
		listener = new ServerSocket(port);

		logger.info("Creating worker service with " + numThreads + " threads");
		service = Executors.newFixedThreadPool(numThreads);
	}

	public void run() {
		try {
			while(true) {
				/*
				 * Listen for a request. When a request comes in accept it then
				 * create a Connection object to service the request and go back
				 * to listening on the port
				 */
				final Socket client = listener.accept();
				logger.info("Got connection request " + client);

				service.execute(new Runnable() {
					public void run() {
						try {
							// Create worker for conversation with client
							logger.info("Creating request worker on " + client.getInetAddress());
							Worker worker = new Worker(config.getProperty("prefix"), client.getInetAddress(), client.getInputStream(), client.getOutputStream());

							worker.work();
						} catch(IOException e) {
							logger.error("Cannot create streams", e);
						} finally {
							try {
								client.close();
							} catch(Exception e) {
							}
						}
					}
				});
			}
		} catch(IOException e) {
			logger.error("Cannot accept client", e);
		}
	}

	public static void main(String[] args) {
		if(args.length != 1) {
			System.out.println("usage: java Server <config file>");
			return;
		}

		try {
			Server server = new Server(args[0]);

			logger.info("Running server");
			server.run();
		} catch(Exception e) {
			e.printStackTrace();
			
			System.exit(1);
		}
		
		System.exit(0);
	}
}

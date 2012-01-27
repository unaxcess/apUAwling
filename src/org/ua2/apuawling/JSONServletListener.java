package org.ua2.apuawling;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

public class JSONServletListener implements ServletContextListener {

	private ExecutorService executor;

	private static final Logger logger = Logger.getLogger(JSONServletListener.class);

	/**
	 * Hands out threads from the wrapped threadfactory with setDeamon(true), so
	 * the threads won't keep the JVM alive when it should otherwise exit.
	 */
	public class DaemonThreadFactory implements ThreadFactory {

		private final ThreadFactory factory;

		/**
		 * Construct a ThreadFactory with setDeamon(true) using
		 * Executors.defaultThreadFactory()
		 */
		public DaemonThreadFactory() {
			this(Executors.defaultThreadFactory());
		}

		/**
		 * Construct a ThreadFactory with setDeamon(true) wrapping the given
		 * factory
		 * 
		 * @param thread
		 *            factory to wrap
		 */
		public DaemonThreadFactory(ThreadFactory factory) {
			if(factory == null)
				throw new NullPointerException("factory cannot be null");
			this.factory = factory;
		}

		public Thread newThread(Runnable r) {
			final Thread t = factory.newThread(r);
			t.setDaemon(true);
			return t;
		}
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		logger.info("Initialising context");

		ThreadFactory daemonFactory = new DaemonThreadFactory();
		logger.info("Creating executor");
		executor = Executors.newSingleThreadExecutor(daemonFactory);

		try {
			Properties config = new Properties();
			InputStream stream = event.getServletContext().getResourceAsStream("edflive.properties");
			logger.info("Loading config from " + stream);
			config.load(stream);
			stream.close();

			if("edf".equalsIgnoreCase(config.getProperty("mode"))) {
				logger.info("Starting EDF session");
				Session.startEDF(
						config.getProperty("edf.host"), Integer.parseInt(config.getProperty("edf.port")),
						config.getProperty("edf.username"), config.getProperty("edf.password"),
						executor
				);
			}
		} catch(Exception e) {
			logger.error("Cannot load config", e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		logger.info("Destroying context");

		executor.shutdownNow();
	}
}

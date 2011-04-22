package org.ua2.apuawling;

import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

public class JSONServletListener implements ServletContextListener {

	private static final Logger logger = Logger.getLogger(JSONServletListener.class);

	@Override
	public void contextInitialized(ServletContextEvent event) {
		logger.info("Initialising context");
		
		try {
			Properties config = new Properties();
			InputStream stream = event.getServletContext().getResourceAsStream("edflive.properties");
			logger.info("Loading config from " + stream);
			config.load(stream);
			stream.close();

			if("edf".equalsIgnoreCase(config.getProperty("mode"))) {
				Session.INSTANCE.startupEDF(
						config.getProperty("edf.host"), Integer.parseInt(config.getProperty("edf.port")),
						config.getProperty("edf.username"), config.getProperty("edf.password")
				);
			}
		} catch(Exception e) {
			Logger.getLogger(getClass()).error("Cannot load config", e);
		}
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent event) {
		logger.info("Destroying context");
		
		Session.INSTANCE.shutdown();
	}

}

package com.coreoz;

import com.coreoz.conf.HttpGatewayConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Results;

import java.time.Duration;

/**
 * The application entry point, where it all begins.
 */
public class WebApplication {

	private static final Logger logger = LoggerFactory.getLogger(WebApplication.class);

	// maximal waiting time for the last process to execute after the JVM received a kill signal
	public static final Duration GRACEFUL_SHUTDOWN_TIMEOUT = Duration.ofSeconds(60);

	public static void main(String[] args) {
		try {
			long startTimestamp = System.currentTimeMillis();

			HttpGateway httpGateway = HttpGateway.start(new HttpGatewayConfiguration(
				8080,
				(router) -> router.GET("/test").routingTo((request) -> Results.ok("Hello world !")).build()
			));

			// Add a shutdown hook to execute some code when the JVM receive a kill signal before it stops
			addShutDownListener(httpGateway);

			logger.info("Server started in {} ms", System.currentTimeMillis() - startTimestamp);
		} catch (Throwable e) {
			logger.error("Failed to start server", e);
			// This line is important, because during initialization some libraries change the main thread type
			// to daemon, which mean that even if the project is completely down, the JVM is not stopped.
			// Stopping the JVM is important to enable production supervision tools to detect and restart the project.
			System.exit(1);
		}
	}

	private static void addShutDownListener(HttpGateway server) {
		Runtime.getRuntime().addShutdownHook(new Thread(
			() -> {
				logger.info("Stopping signal received, shutting down server and scheduler...");
				try {
					server.stop();
				} catch(Exception e) {
					logger.error("Error while shutting down server.", e);
				}
				logger.info("Server and scheduler stopped.");
			},
			"shutdownHook"
		));
	}
}

package fr.fne.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.batch.core.BatchStatus;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class BatchApplication {
	private static final Logger logger = LoggerFactory.getLogger(BatchApplication.class);

	public static void main(String[] args){
		ConfigurableApplicationContext context = SpringApplication.run(BatchApplication.class, args);

		int exitValue = SpringApplication.exit(context);

		if (exitValue == BatchStatus.COMPLETED.ordinal()) {
			exitValue = 0;
		} else if (exitValue == BatchStatus.FAILED.ordinal()) {
			exitValue = 1;
		} else if ((exitValue == BatchStatus.STOPPING.ordinal()) || (exitValue == BatchStatus.STARTING.ordinal())
				|| (exitValue == BatchStatus.STARTED.ordinal())) {
			exitValue = 2;
		}

		if (logger.isInfoEnabled()) {
			logger.info(String.format("Exit code: %s", exitValue) );
		}

		System.exit(exitValue);
	}

}

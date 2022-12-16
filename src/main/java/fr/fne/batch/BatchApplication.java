package fr.fne.batch;

import fr.fne.batch.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BatchApplication implements CommandLineRunner {
	@Autowired
	private CreationFormat creationFormat;

	@Autowired
	private ChargementParAPI chargementParAPI;

	@Autowired
	private ChargementParSQL chargementParSQL;

	@Autowired
	private IndexationES indexationES;

	@Autowired
	private IndexationSPARQL indexationSPARQL;

	private final Logger logger = LoggerFactory.getLogger(BatchApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BatchApplication.class, args);
	}

	public void run(String... args) throws Exception {		
		
		// Pour éviter d'attendre indéfiniment si une url ne répond pas
		System.setProperty("sun.net.client.defaultConnectTimeout", "20000");
		System.setProperty("sun.net.client.defaultReadTimeout", "20000");				

		// Lancer le service passé en paramètre :
		if (args.length > 0) {						
			String action = args[0];
			if (action.equalsIgnoreCase("Format")) {
				creationFormat.go();
			}
			else if (action.equalsIgnoreCase("API")) {
				chargementParAPI.go();
			}
			else if (action.equalsIgnoreCase("SQL")) {
				chargementParSQL.go();
			}
			else if (action.equalsIgnoreCase("IndexationES")) {
				indexationES.go();
			}
			else if (action.equalsIgnoreCase("IndexationSPARQL")) {
				indexationSPARQL.go();
			}
		} else {
			logger.info("BatchApplication : pas de paramètre");
			logger.info("Choisir : Format | SQL | API | IndexationES | IndexationSPARQL");
		}							
	}

}

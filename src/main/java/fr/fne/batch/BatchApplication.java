package fr.fne.batch;

import fr.fne.batch.services.ChargementParAPI;
import fr.fne.batch.services.ChargementParSQL;
import fr.fne.batch.services.CreationFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class BatchApplication implements CommandLineRunner {
	@Value("${wikibase.url}")
	private String urlWikiBase;

	@Autowired
	private CreationFormat creationFormat;

	@Autowired
	private ChargementParAPI chargementParAPI;

	@Autowired
	private ChargementParSQL chargementParSQL;

	private final Logger logger = LoggerFactory.getLogger(BatchApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BatchApplication.class, args);
	}

	public void run(String... args) throws Exception {		
		
		// Pour éviter d'attendre indéfiniment si une url ne répond pas
		System.setProperty("sun.net.client.defaultConnectTimeout", "20000");
		System.setProperty("sun.net.client.defaultReadTimeout", "20000");				

		int responseCode = -1;

		// Attente que urlWikiBase soit disponible
		while (responseCode!=200){
			TimeUnit.SECONDS.sleep(5);
			try {
				URL url = new URL(urlWikiBase);
				HttpURLConnection huc = (HttpURLConnection) url.openConnection();
				responseCode = huc.getResponseCode();
			}
			catch (Exception e){
				logger.info("Tentative de connexion à "+urlWikiBase+" erreur : "+e.getMessage());
			}

			logger.info("responseCode : "+ responseCode);
		}

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
		} else {
			logger.info("BatchApplication : pas de paramètre");
			logger.info("Choisir : Format | SQL | API");
		}							
	}

}

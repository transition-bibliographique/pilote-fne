package fr.fne.batch.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import fr.fne.batch.model.autorite.Collection;
import fr.fne.batch.model.autorite.Record;
import fr.fne.batch.util.ApiWB;
import fr.fne.batch.util.DatabaseInsert;
import fr.fne.batch.util.DtoAutoriteToItem;
import fr.fne.batch.util.Format;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikidata.wdtk.datamodel.helpers.JsonSerializer;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/*
 * Chargement d'entités dans une WB
 */
@Service
public class ChargementParSQL {

    private final Logger logger = LoggerFactory.getLogger(ChargementParSQL.class);

    @Autowired
    private ApiWB apiWB;

    @Autowired
    private Format format;

    @Autowired
    private DtoAutoriteToItem dtoAutoriteToItem;

    @Value("${abes.dump}")
    private String cheminDump;

    @Value("${mysql.url}")
    private String mysqlUrl;
    @Value("${mysql.login}")
    private String mysqlLogin;
    @Value("${mysql.pwd}")
    private String mysqlPwd;

    public void go() {

        int recordNb = 0;

        logger.info("Chargement par SQL");

        try {
            // Connextion à Wikibase et récupération du csrftoken
            String csrftoken = apiWB.connexionWB();
            logger.info("The csrftoken is : " + csrftoken);

            //Récupération de toutes les propriétés du WB
            Map<String, String> props;
            props = format.get();

            //Si pas de propriétés, alors création (pr éviter d'appeler 2x fois le BatchApplication : creationProprietes puis chargement
            if (props.size()==0){
                // Création du format
                format.createWithFile(csrftoken);
                // Map des propriétés
                props = format.get();
            }
            logger.info("Nombre de propriétés chargées : " + props.size());


            //Test chargement direct en BDD
            Connection connection = DriverManager.getConnection(mysqlUrl, mysqlLogin, mysqlPwd);

            DatabaseInsert di = new DatabaseInsert(connection);
            di.startTransaction();

            final StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            //Utilisation d'un dump des notices (5000 notices par fichier):
            //Le dump complet est disponible ici (Abes, sur KAT): /applis/portail/SitemapNoticesSudoc/noticesautorites/dump/
            //Pour tester : utiliser l'échantillon qui se trouve dans resources/dump/
            File[] fichiers = new File(cheminDump).listFiles();

            int lanceCommit = 0;
            for (int i=0;i<fichiers.length;i++) {
                logger.info("Fichier traité : "+fichiers[i].getName());

                JacksonXmlModule xmlModule = new JacksonXmlModule();
                xmlModule.setDefaultUseWrapper(false);
                ObjectMapper objectMapper = new XmlMapper(xmlModule);
                objectMapper.registerModule(new JaxbAnnotationModule());
                Collection collection = objectMapper.readValue(new FileInputStream(fichiers[i]), Collection.class);

                for(Record record : collection.getRecordList()){
                    recordNb++;

                    ItemDocument itemDocument = dtoAutoriteToItem.unmarshallerNotice(record, props);

                    if (itemDocument != null) {
                        di.createItem(JsonSerializer.getJsonString(itemDocument));
                    }
                }
                di.commit();
            }
            di.commit();

            // Ensuite, il faut indexer dans Elastic Search et dans WDQS (SPARQL),
            // avec les scripts : (scriptsIndexation) indexationES et indexationSPARQL (.ps1 ou .sh) du dépôt pilote-fne-docker

            stopWatch.stop();

            logger.info("Created "+recordNb+" items in " + stopWatch.getTime(TimeUnit.SECONDS)+" s.");
            logger.info("Speed is "+(int) (recordNb / (double) stopWatch.getTime(TimeUnit.SECONDS))+" items/second.");
        } catch (Exception e) {
            logger.error("ChargementParSQL pb : " + e.getMessage());
            e.printStackTrace();
        }
    }

}

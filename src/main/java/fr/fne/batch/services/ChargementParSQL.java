package fr.fne.batch.services;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import fr.fne.batch.model.autorite.Collection;
import fr.fne.batch.model.autorite.Record;
import fr.fne.batch.services.util.api.UtilAPI;
import fr.fne.batch.services.util.bdd.DatabaseInsert;
import fr.fne.batch.services.util.entities.EntitiesJSOUP;
import org.apache.commons.lang3.time.StopWatch;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.fne.batch.services.util.entities.Format;
import org.wikidata.wdtk.datamodel.helpers.JsonSerializer;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;

/*
 * Chargement d'entités dans une WB
 */
@Service
public class ChargementParSQL {

    private final Logger logger = LoggerFactory.getLogger(ChargementParSQL.class);

    @Autowired
    private UtilAPI util;

    @Autowired
    private Format format;

    @Autowired
    private DtoNoticeToItem dtoNoticeToItem;

    @Value("${abes.dump}")
    private String cheminDump;

    public void go() {

        int recordNb = 0;

        logger.info("Chargement par SQL");

        try {
            // Connextion à Wikibase et récupération du csrftoken
            String csrftoken = util.connexionWB();
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
            //Supprimer le paramètre &rewriteBatchedStatements=true si pas d'executeBatch utilisé dans DatabaseInsert
            Connection connection = DriverManager.getConnection(
                    "jdbc:mariadb://localhost:3306/my_wiki?characterEncoding=utf-8&rewriteBatchedStatements=true",
                    "sqluser",
                    "change-this-sqlpassword");

            DatabaseInsert di = new DatabaseInsert(connection);
            di.startTransaction();

            final StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            String entity = null;

            //Utilisation d'un dump des notices (5000 notices par fichier):
            //Le dump est disponible ici : /applis/portail/SitemapNoticesSudoc/noticesautorites/dump
            File[] fichiers = new File(cheminDump).listFiles();

            int lanceCommit = 0;
            for (int i=0;i<fichiers.length;i++) {
                JacksonXmlModule xmlModule = new JacksonXmlModule();
                xmlModule.setDefaultUseWrapper(false);
                ObjectMapper objectMapper = new XmlMapper(xmlModule);
                objectMapper.registerModule(new JaxbAnnotationModule());
                Collection collection = objectMapper.readValue(new FileInputStream(fichiers[i]), Collection.class);

                for(Record record : collection.getRecordList()){
                    recordNb++;

                    /*
                    lanceCommit++;

                    if (lanceCommit==1000){
                        di.commit();
                        lanceCommit=0;
                    }*/

                    ItemDocument itemDocument = dtoNoticeToItem.unmarshallerNotice(record, props);

                    if (itemDocument != null) {
                        di.createItem(JsonSerializer.getJsonString(itemDocument));
                    }
                }
            }
            di.commit();

            stopWatch.stop();

            logger.info("Created "+recordNb+" items in " + stopWatch.getTime(TimeUnit.SECONDS)+" s.");
            logger.info("Speed is "+(int) (recordNb / (double) stopWatch.getTime(TimeUnit.SECONDS))+" items/second.");

        } catch (Exception e) {
            logger.error("ChargementParSQL pb : " + e.getMessage());
            e.printStackTrace();
        }
    }

}

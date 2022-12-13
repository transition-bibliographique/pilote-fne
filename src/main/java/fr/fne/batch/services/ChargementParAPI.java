package fr.fne.batch.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import fr.fne.batch.model.autorite.Collection;
import fr.fne.batch.model.autorite.Record;
import fr.fne.batch.util.ApiWB;
import fr.fne.batch.util.DtoAutoriteToItem;
import fr.fne.batch.util.Format;
import org.apache.commons.lang3.time.StopWatch;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikidata.wdtk.datamodel.helpers.JsonSerializer;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/*
 * Chargement d'entités dans une WB
 */
@Service
public class ChargementParAPI {

    private final Logger logger = LoggerFactory.getLogger(ChargementParAPI.class);

    @Autowired
    private ApiWB apiWB;

    @Autowired
    private Format format;

    @Autowired
    private DtoAutoriteToItem dtoAutoriteToItem;

    @Value("${abes.dump}")
    private String cheminDump;

    public void go() {

        int recordNb = 0;

        logger.info("Chargement par API");

        try {
            // Connextion à Wikibase et récupération du csrftoken
            String csrftoken = apiWB.connexionWB();
            logger.info("The csrftoken is : " + csrftoken);

            //Récupération de toutes les propriétés du WB
            Map<String, String> props;
            props = format.get();

            //Si pas de propriétés, alors création (pr éviter d'appeler 2x fois le BatchApplication : creationFormat puis chargement
            if (props.size()==0){
                // Création du format
                format.createWithFile(csrftoken);
                // Map des propriétés
                props = format.get();
            }
            logger.info("Nombre de propriétés chargées : " + props.size());

            final StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            //Test chargement par l'API WB

            //Utilisation d'un dump des notices (5000 notices par fichier):
            //Le dump complet est disponible ici (Abes, sur KAT): /applis/portail/SitemapNoticesSudoc/noticesautorites/dump/
            //Pour tester : utiliser l'échantillon qui se trouve dans resources/dump/
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

                    ItemDocument itemDocument = dtoAutoriteToItem.unmarshallerNotice(record, props);

                    if (itemDocument != null) {
                        Map<String, String> params = new LinkedHashMap<>();
                        params.put("action", "wbeditentity");
                        params.put("new", "item");
                        params.put("token", csrftoken);
                        params.put("format", "json");
                        params.put("data", JsonSerializer.getJsonString(itemDocument));
                        JSONObject json = apiWB.postJson(params);

                        logger.info("json : "+json.toString());
                    }
                }
            }

            stopWatch.stop();

            logger.info("Created "+recordNb+" items in " + stopWatch.getTime(TimeUnit.SECONDS)+" s.");
            logger.info("Speed is "+(int) (recordNb / (double) stopWatch.getTime(TimeUnit.SECONDS))+" items/second.");

        } catch (Exception e) {
            logger.error("ChargementParAPI pb : " + e.getMessage());
            e.printStackTrace();
        }
    }

}

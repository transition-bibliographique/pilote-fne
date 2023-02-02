package fr.fne.batch.services;

import fr.fne.batch.util.ApiWB;
import fr.fne.batch.util.DatabaseInsert;
import fr.fne.batch.util.DtoAutoriteToItem;
import fr.fne.batch.util.Format;
import org.apache.commons.lang3.time.StopWatch;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/*
 * Modification d'entités dans une WB
 */
@Service
public class ModificationParSQL {

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

        logger.info("Modification par SQL");

        try {
            // Connexion à Wikibase et récupération du csrftoken
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

            //Test modification directe en BDD
            Connection connection = DriverManager.getConnection(mysqlUrl, mysqlLogin, mysqlPwd);

            DatabaseInsert di = new DatabaseInsert(connection);
            di.startTransaction();

            final StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            //Requête "simple" (pas par SPARQL ou ElasticSearch) pour remonter toutes les entités

            // Getting namespace Item id
            JSONObject json = apiWB.getJson("?action=query&format=json&meta=siteinfo&siprop=namespaces");
            Iterator<String> it = json.optJSONObject("query").optJSONObject("namespaces").keys();
            String idNamespaceItem = "";
            while (it.hasNext() && idNamespaceItem.isEmpty()) {
                String iterId = it.next();
                if (json.optJSONObject("query").optJSONObject("namespaces").optJSONObject(iterId).optString("canonical").equalsIgnoreCase("Item")) {
                    idNamespaceItem = iterId;
                }
            }

            // /!\ Attention au delà de 500, il faut utiliser la pagination /!\
            String lastItem = "";
            while (lastItem != null) {
                json = apiWB.getJson("?action=query&format=json&list=allpages&apnamespace="
                        + idNamespaceItem + "&aplimit=500&apfrom=" + lastItem);
                JSONArray liste = json.optJSONObject("query").optJSONArray("allpages");
                for (int i = 0; i < liste.length(); i++) {
                    JSONObject itemTitle = liste.getJSONObject(i);
                    //logger.info("Titre de l'item : "+itemTitle.optString("title"));
                    String title = itemTitle.optString("title");
                    String itemId = title.replace("Item:", "");

                    JSONObject item = apiWB.getJson("?action=wbgetentities&format=json&ids=" + itemId);

                    JSONObject theItem = item.optJSONObject("entities").optJSONObject(itemId);

                    //Test de modification du nom
                    if (theItem.getJSONObject("claims").optJSONArray(props.get("Nom")) != null) {
                        String nomModifie = theItem.getJSONObject("claims").optJSONArray(props.get("Nom")).optJSONObject(0).optJSONObject("mainsnak").getJSONObject("datavalue").getString("value");

                        nomModifie = nomModifie + " (modifie)";
                        theItem.getJSONObject("claims").optJSONArray(props.get("Nom")).optJSONObject(0).optJSONObject("mainsnak").getJSONObject("datavalue").put("value",nomModifie);
                        //logger.info("theItem modifie : "+theItem);

                        recordNb++;

                        di.updateItem(theItem.toString());
                    }
                }
                //di.commit();

                // logger.info("to continue :"+json.optJSONObject("continue").optString("apcontinue"));
                if (json.optJSONObject("continue") != null) {
                    lastItem = json.optJSONObject("continue").optString("apcontinue");
                } else {
                    lastItem = null;
                }
            }
            //di.commit();

            // Ensuite, il faut indexer dans Elastic Search et dans WDQS (SPARQL),
            // avec les scripts : (scriptsIndexation) indexationES et indexationSPARQL (.ps1 ou .sh) du dépôt pilote-fne-docker

            stopWatch.stop();

            logger.info("Updated "+recordNb+" items in " + stopWatch.getTime(TimeUnit.SECONDS)+" s.");
            logger.info("Speed is "+(int) (recordNb / (double) stopWatch.getTime(TimeUnit.SECONDS))+" items/second.");
        } catch (Exception e) {
            logger.error("ModificationParSQL pb : " + e.getMessage());
            e.printStackTrace();
        }
    }

}

package fr.fne.batch.services;

import fr.fne.batch.util.RunCommand;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class IndexationSPARQL {
    @Autowired
    private RunCommand runCommand;

    @Value("${docker.commande}")
    private String commandeDocker;

    @Value("${container.wikibase}")
    private String containerWikibase;

    @Value("${container.wdqs}")
    private String containerWdqs;

    private final Logger logger = LoggerFactory.getLogger(IndexationSPARQL.class);

    /*
        Chargement des données dans le WDQS / Blazegraph
        Très inspiré de cette procédure : https://wikitech.wikimedia.org/wiki/Wikidata_Query_Service#Data_reload_procedure
     */
    public void go(){
        final StopWatch stopWatch = new StopWatch();

        try {
            stopWatch.start();

            // /wdqs/runUpdate.sh -h http://${WDQS_HOST}:${WDQS_PORT} -- --wikibaseUrl ${WIKIBASE_SCHEME}://${WIKIBASE_HOST} --conceptUri ${WIKIBASE_SCHEME}://${WIKIBASE_HOST} --entityNamespaces ${WDQS_ENTITY_NAMESPACES} --init --start 20210315120000
            /*List<String> command = new ArrayList<String>();
            command.add(commandeDocker);
            command.add("exec");
            command.add(containerWdqs);
            command.add("bash");
            command.add("-c");
            command.add("/wdqs/runUpdate.sh -h http://${WDQS_HOST}:${WDQS_PORT} -- --wikibaseUrl ${WIKIBASE_SCHEME}://${WIKIBASE_HOST} --conceptUri ${WIKIBASE_SCHEME}://${WIKIBASE_HOST} --entityNamespaces ${WDQS_ENTITY_NAMESPACES} --init --start 20210315120000");
            runCommand.run(command,sortie1,"Got no real changes");*/
            // La commande ci-dessus est assez lente : 20 minutes pour 16000 notices

            //php extensions/Wikibase/repo/maintenance/dumpRdf.php --server http://${WIKIBASE_HOST} | gzip > MyWiki.ttl.gz
            List<String> command = new ArrayList<String>();
            command.add(commandeDocker);
            command.add("exec");
            command.add(containerWikibase);
            command.add("bash");
            command.add("-c");
            command.add("php extensions/Wikibase/repo/maintenance/dumpRdf.php --server http://${WIKIBASE_HOST} | gzip > MyWiki.ttl.gz");
            runCommand.run(command,"sortie2");

            //docker cp wikibase-docker_wikibase_1:/var/www/html/MyWiki.ttl.gz .
            command = new ArrayList<String>();
            command.add(commandeDocker);
            command.add("cp");
            command.add(containerWikibase+":/var/www/html/MyWiki.ttl.gz");
            command.add(".");
            runCommand.run(command,"sortie2");

            //docker cp MyWiki.ttl.gz wikibase-docker_wdqs_1:/wdqs
            command = new ArrayList<String>();
            command.add(commandeDocker);
            command.add("cp");
            command.add("MyWiki.ttl.gz");
            command.add(containerWdqs+":/wdqs");
            runCommand.run(command,"sortie2");

            //curl http://${WDQS_HOST}:${WDQS_PORT}/bigdata/namespace/wdq/sparql --data-urlencode "update=LOAD <file:///wdqs/MyWiki.ttl.gz>;"
            command = new ArrayList<String>();
            command.add(commandeDocker);
            command.add("exec");
            command.add(containerWdqs);
            command.add("bash");
            command.add("-c");
            command.add("curl http://${WDQS_HOST}:${WDQS_PORT}/bigdata/namespace/wdq/sparql --data-urlencode 'update=LOAD <file:///wdqs/MyWiki.ttl.gz>;'");
            runCommand.run(command,"sortie2");

            stopWatch.stop();
            logger.info("Temps de traitement : "+ stopWatch.getTime(TimeUnit.SECONDS) +" s.");
            //4 minutes pour 16000 notices
        }
        catch (Exception e ){
            logger.error("Erreur : "+e.getMessage());
        }
    }
}

package fr.fne.batch.services;

import fr.fne.batch.util.RunCommand;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class IndexationSPARQL {
    @Autowired
    private RunCommand runCommand;

    private final Logger logger = LoggerFactory.getLogger(IndexationSPARQL.class);

    public void go(){
        final StopWatch stopWatch = new StopWatch();

        try {
            stopWatch.start();

            // /wdqs/runUpdate.sh -h http://${WDQS_HOST}:${WDQS_PORT} -- --wikibaseUrl ${WIKIBASE_SCHEME}://${WIKIBASE_HOST} --conceptUri ${WIKIBASE_SCHEME}://${WIKIBASE_HOST} --entityNamespaces ${WDQS_ENTITY_NAMESPACES} --init --start 20210315120000
            List<String> command = new ArrayList<String>();
            command.add("C:\\Program Files\\Docker\\Docker\\resources\\bin\\docker.exe");
            command.add("exec");
            command.add("wikibase-docker_wdqs_1");
            command.add("bash");
            command.add("-c");
            command.add("/wdqs/runUpdate.sh -h http://${WDQS_HOST}:${WDQS_PORT} -- --wikibaseUrl ${WIKIBASE_SCHEME}://${WIKIBASE_HOST} --conceptUri ${WIKIBASE_SCHEME}://${WIKIBASE_HOST} --entityNamespaces ${WDQS_ENTITY_NAMESPACES} --init --start 20210315120000");
            runCommand.run(command,"Got no real changes");

            stopWatch.stop();
            logger.info("Temps de traitement : "+ stopWatch.getTime(TimeUnit.SECONDS) +" s.");
        }
        catch (Exception e ){
            logger.error("Erreur : "+e.getMessage());
        }
    }
}

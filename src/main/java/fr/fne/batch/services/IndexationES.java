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
public class IndexationES {

    @Autowired
    private RunCommand runCommand;

    @Value("${docker.commande}")
    private String commandeDocker;

    @Value("${container.wikibase}")
    private String containerWikibase;

    private final Logger logger = LoggerFactory.getLogger(IndexationES.class);

    /*
        Chargement des données dans l'Elastic Search
        Copie des commandes de : https://github.com/UB-Mannheim/RaiseWikibase/blob/main/RaiseWikibase/raiser.py
     */
    public void go(){
        final StopWatch stopWatch = new StopWatch();

        try {
            stopWatch.start();

            // php extensions/CirrusSearch/maintenance/ForceSearchIndex.php --skipLinks –indexOnSkip
            List<String> command = new ArrayList<String>();
            command.add(commandeDocker);
            command.add("exec");
            command.add(containerWikibase);
            command.add("bash");
            command.add("-c");
            command.add("php extensions/CirrusSearch/maintenance/ForceSearchIndex.php --skipLinks –indexOnSkip");
            runCommand.run(command,"sortie1");

            // php extensions/CirrusSearch/maintenance/ForceSearchIndex.php –skipParse
            command = new ArrayList<String>();
            command.add(commandeDocker);
            command.add("exec");
            command.add(containerWikibase);
            command.add("bash");
            command.add("-c");
            command.add("php extensions/CirrusSearch/maintenance/ForceSearchIndex.php –skipParse");
            runCommand.run(command,"sortie1");

            // php maintenance/runJobs.php
            command = new ArrayList<String>();
            command.add(commandeDocker);
            command.add("exec");
            command.add(containerWikibase);
            command.add("bash");
            command.add("-c");
            command.add("php maintenance/runJobs.php");
            runCommand.run(command,"sortie1");

            stopWatch.stop();
            logger.info("Temps de traitement : "+ stopWatch.getTime(TimeUnit.SECONDS) +" s.");
            //10 minutes pour 16000 notices
        }
        catch (Exception e ){
            logger.error("Erreur : "+e.getMessage());
        }
    }
}

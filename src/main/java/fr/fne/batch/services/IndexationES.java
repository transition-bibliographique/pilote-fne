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
public class IndexationES {

    @Autowired
    private RunCommand runCommand;

    private final Logger logger = LoggerFactory.getLogger(IndexationES.class);

    public void go(){
        final StopWatch stopWatch = new StopWatch();

        try {
            //https://stackoverflow.com/questions/525212/how-to-run-unix-shell-script-from-java-code

            //https://mkyong.com/java/java-processbuilder-examples/
            //http://www.java2s.com/example/java-api/java/lang/processbuilder/processbuilder-1-8.html

            String homeDirectory = System.getProperty("user.home");
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

            stopWatch.start();

            // php extensions/CirrusSearch/maintenance/ForceSearchIndex.php --skipLinks –indexOnSkip
            List<String> command = new ArrayList<String>();
            command.add("C:\\Program Files\\Docker\\Docker\\resources\\bin\\docker.exe");
            command.add("exec");
            command.add("wikibase-docker_wikibase_1");
            command.add("bash");
            command.add("-c");
            command.add("php extensions/CirrusSearch/maintenance/ForceSearchIndex.php --skipLinks –indexOnSkip");
            runCommand.run(command,null);

            // php extensions/CirrusSearch/maintenance/ForceSearchIndex.php –skipParse
            command = new ArrayList<String>();
            command.add("C:\\Program Files\\Docker\\Docker\\resources\\bin\\docker.exe");
            command.add("exec");
            command.add("wikibase-docker_wikibase_1");
            command.add("bash");
            command.add("-c");
            command.add("php extensions/CirrusSearch/maintenance/ForceSearchIndex.php –skipParse");
            runCommand.run(command,null);

            // php maintenance/runJobs.php
            command = new ArrayList<String>();
            command.add("C:\\Program Files\\Docker\\Docker\\resources\\bin\\docker.exe");
            command.add("exec");
            command.add("wikibase-docker_wikibase_1");
            command.add("bash");
            command.add("-c");
            command.add("php maintenance/runJobs.php");
            runCommand.run(command,null);

            stopWatch.stop();
            logger.info("Temps de traitement : "+ stopWatch.getTime(TimeUnit.SECONDS) +" s.");
        }
        catch (Exception e ){
            logger.error("Erreur : "+e.getMessage());
        }
    }
}

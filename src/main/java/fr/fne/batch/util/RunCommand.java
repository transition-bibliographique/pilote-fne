package fr.fne.batch.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

@Service
public class RunCommand {

    private final Logger logger = LoggerFactory.getLogger(RunCommand.class);

    /*
    command : le ArrayList<String> composant la commande à passer (voir IndexationES ou IndexationSPARQL)
    stop : le critère qui permet d'interrompre une commande (voir IndexationSPARQL)
     */
    public void run(List<String> command, String stop) throws Exception{
        Process p = Runtime.getRuntime().exec(command.toArray(new String[0]));

        logger.info("Commande lancée : "+command.toString());

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(p.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(p.getErrorStream()));

        String s;
        // read the output from the command
        StringBuilder output = new StringBuilder();
        while ((s = stdInput.readLine()) != null) {
            output.append(s+"\n");

            //logger.info(s);
            if (stop!=null && s.contains(stop)){
                p.destroy();
            }
        }
        if (!output.isEmpty()){
            logger.debug("Output : \n"+output);
        }

        StringBuilder error = new StringBuilder();
        while ((s = stdError.readLine()) != null) {
            error.append(s+"\n");
        }
        if (!error.isEmpty()){
            logger.error("Error : \n"+error);
        }
    }

}

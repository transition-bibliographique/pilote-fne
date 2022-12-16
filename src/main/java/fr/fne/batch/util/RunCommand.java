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
        Lance la commande passée en paramètre
        command : le ArrayList<String> composant la commande à passer (voir IndexationES ou IndexationSPARQL)
        sortie : la sortie à afficher en premier (pour IndexationES c'est la sortie standard, pour IndexationSPARQL c'est la sortie 2 / error)
     */
    public void run(List<String> command, String sortie) throws Exception{
        run(command, sortie, null);
    }

    /*
        Lance la commande passée en paramètre
        command : le ArrayList<String> composant la commande à passer (voir IndexationES ou IndexationSPARQL)
        sortie : la sortie à afficher en premier (pour IndexationES c'est la sortie standard, pour IndexationSPARQL c'est la sortie 2 / error)
        stop : le critère qui permet d'interrompre une commande (voir IndexationSPARQL : stop="Got no real changes")
     */
    public void run(List<String> command, String sortie, String stop) throws Exception{
        Process p = Runtime.getRuntime().exec(command.toArray(new String[0]));

        logger.info("Commande lancée : "+command.toString());

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(p.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(p.getErrorStream()));

        String s;

        if (sortie.contentEquals("sortie1")){
            while ((s = stdInput.readLine()) != null) {
                logger.info(s);

                if (stop!=null && s.contains(stop)){
                    p.destroy();
                }
            }
        }
        else { //sortie2
            while ((s = stdError.readLine()) != null) {
                logger.info(s);
            }
        }

    }

}

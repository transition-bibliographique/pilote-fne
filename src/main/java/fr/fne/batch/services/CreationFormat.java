package fr.fne.batch.services;

import fr.fne.batch.util.ApiWB;
import fr.fne.batch.util.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/*
Création du format : propriétés et type d'entités, dans une WB
 */
@Service
public class CreationFormat {
    private final Logger logger = LoggerFactory.getLogger(CreationFormat.class);

    @Autowired
    private ApiWB apiWB;

    @Autowired
    private Format format;

    public void go() {

        try {
            // Connextion à Wikibase et récupération du csrftoken
            String csrftoken = apiWB.connexionWB();
            logger.info("The csrftoken is : " + csrftoken);

            // Création du format :
            // création des propriétés (à partir du fichier ProprietesWB.txt
            // création des types d'entités : Personne Q1 et IPP Q2
            format.createWithFile(csrftoken);
        }
        catch (Exception e) {
            logger.error("CreationFormat pb : " + e.getMessage());
            e.printStackTrace();
        }

    }
}

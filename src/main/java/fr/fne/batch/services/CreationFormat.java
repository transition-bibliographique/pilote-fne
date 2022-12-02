package fr.fne.batch.services;

import fr.fne.batch.services.util.api.UtilAPI;
import fr.fne.batch.services.util.entities.Format;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/*
Création du format : propriétés et type d'entités, dans une WB
 */
@Service
public class CreationFormat {
    private final Logger logger = LoggerFactory.getLogger(CreationFormat.class);

    @Autowired
    private UtilAPI util;

    @Autowired
    private Format format;

    public void go() {

        try {
            // Connextion à Wikibase et récupération du csrftoken
            String csrftoken = util.connexionWB();
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

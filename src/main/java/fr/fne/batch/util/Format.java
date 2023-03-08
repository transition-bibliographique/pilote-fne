package fr.fne.batch.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class Format {
    private final Logger logger = LoggerFactory.getLogger(Format.class);

    /*
     Création du format :
     - création des propriétés (à partir du fichier ProprietesWB.txt
     - création des types d'entités : Personne Q1 et IPP Q2
     */
    public void create() throws Exception {

        // création des types d'entité
        //createType(csrftoken,"Personne"); //Q1
        //createType(csrftoken,"IPP"); //Q2
    }
}

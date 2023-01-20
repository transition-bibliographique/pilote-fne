package fr.fne.batch.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.fne.batch.model.autorite.Controlfield;
import fr.fne.batch.model.autorite.Datafield;
import fr.fne.batch.model.autorite.Record;
import fr.fne.batch.model.autorite.Subfield;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.ItemDocumentBuilder;
import org.wikidata.wdtk.datamodel.helpers.ReferenceBuilder;
import org.wikidata.wdtk.datamodel.helpers.StatementBuilder;
import org.wikidata.wdtk.datamodel.implementation.PropertyIdValueImpl;
import org.wikidata.wdtk.datamodel.implementation.ValueSnakImpl;
import org.wikidata.wdtk.datamodel.interfaces.*;

import java.util.Map;

@Service
public class DtoAutoriteToItem {

    private final Logger logger = LoggerFactory.getLogger(DtoAutoriteToItem.class);

    @Value("${wikibase.iri}")
    private String iriWikiBase;

    //Map contenant les labels des propriétés utilisées (en fr, ex : Nom) et leur identifiant correspondant (ex : P1)
    private Map<String,String> props;

    /**
     * DTO :
     * Notice MarcXML vers ItemDocument WB
     * @param r
     * @param proprietes
     * @return
     */
    public ItemDocument unmarshallerNotice(Record r, Map<String,String> proprietes)
    {
        props = proprietes;

        ItemDocumentBuilder itemDocumentBuilder = ItemDocumentBuilder.forItemId(ItemIdValue.NULL);
        ObjectMapper objectMapper = new ObjectMapper();

        try {

            //Il s'agit d'un type d'entité Personne (Q1)
            Statement statement = StatementBuilder
                    .forSubjectAndProperty(ItemIdValue.NULL, new PropertyIdValueImpl(props.get("Type d'entité"), iriWikiBase))
                    .withValue(Datamodel.makeItemIdValue("Q1",iriWikiBase))
                    .build();
            itemDocumentBuilder = itemDocumentBuilder.withStatement(statement);

            //200$a, 200$b, 200$f [001]
            String label = "";
            //001
            String ppn = "";
            //<valeur de zone 010 $a>
            //<valeur de zone 003>
            //<valeur de zone 033 $a>
            String description = "";
            String alias = "";

            //construction label
            int posTranslit = -1;
            int pos200 = 0;
            for (Datafield d : r.getDatafieldList()) {
                if (d.getTag().equalsIgnoreCase("200")) {
                    pos200++;
                    for (Subfield s : d.getSubfieldList()) {
                        if (s.getCode().equalsIgnoreCase("7") &&
                                s.getValue().length() > 5 &&
                                s.getValue().substring(4, 6).equalsIgnoreCase("ba")) {
                            posTranslit=pos200;
                        }
                    }
                }
            }

            pos200 = 0;
            for (Datafield d : r.getDatafieldList()) {
                if (d.getTag().equalsIgnoreCase("200")) {
                    pos200++;
                    if ((posTranslit!=-1 && posTranslit==pos200) || posTranslit==-1) {
                        for (Subfield s : d.getSubfieldList()) {
                            switch (s.getCode().toLowerCase()) {
                                case "a": label += s.getValue(); break;
                                case "b" : label += ", " + s.getValue(); break;
                                case "d" : label += " " + s.getValue(); break;
                                case "f" : label += ", " + s.getValue(); break;
                            }
                        }
                    }
                    else {
                        alias = "";
                        for (Subfield s : d.getSubfieldList()) {
                            switch (s.getCode().toLowerCase()) {
                                case "a": alias += s.getValue(); break;
                                case "b": alias += ", " + s.getValue(); break;
                                case "d": alias += " " + s.getValue(); break;
                                case "f": alias += ", " + s.getValue(); break;
                            }
                        }
                        logger.info("Alias ==>"+alias);
                        itemDocumentBuilder = itemDocumentBuilder.withAlias(alias, "fr");
                    }
                }
            }
            // construction description
            String isni = r.getDatafieldList().stream()
                    .filter(z -> z.getTag().equals("010"))
                    .flatMap(sz -> sz.getSubfieldList().stream())
                    .filter(sz -> sz.getCode().equals("a"))
                    .map(sz -> sz.getValue())
                    .findFirst()
                    .orElse("");

            String idref = r.getControlfieldList().stream()
                    .filter(c -> c.getTag().equals("003"))
                    .map(c -> c.getValue())
                    .findFirst()
                    .orElse("");

            String ark = r.getDatafieldList().stream()
                    .filter(z -> z.getTag().equals("033"))
                    .filter(z -> z.getSubfieldList().stream().anyMatch(sz -> sz.getCode().equals("2") &&  sz.getValue().equalsIgnoreCase("BNF")))
                    .flatMap(sz -> sz.getSubfieldList().stream())
                    .filter(sz -> sz.getCode().equals("a"))
                    .map(sz -> sz.getValue())
                    .findFirst()
                    .orElse("");

            if (!isni.isEmpty()) {
                description += isni ;
            }
            if (!idref.isEmpty()) {
                if (!description.isEmpty()){
                    description += " - ";
                }
                description += idref ;
            }
            if (!ark.isEmpty()) {
                if (!description.isEmpty()){
                    description += " - ";
                }
                description += ark ;
            }
            itemDocumentBuilder = itemDocumentBuilder.withDescription(description, "fr");

            //construction aliases
            // Nom 	<valeur de 700 $a>
            // Prénom 	<valeur de 700 $b>
            // Langue de l'interface	par défaut fr

            for (Datafield d : r.getDatafieldList()) {
                if (d.getTag().equalsIgnoreCase("400") || d.getTag().equalsIgnoreCase("700")) {
                    alias = "";
                    for (Subfield s : d.getSubfieldList()) {
                       switch (s.getCode().toLowerCase()) {
                           case "a": alias += s.getValue(); break;
                           case "b" : alias += ", " + s.getValue(); break;
                           case "d" : alias += " " + s.getValue(); break;
                           case "f" : alias += ", " + s.getValue(); break;
                        }
                    }
                    logger.info("Alias ==>"+alias);
                    itemDocumentBuilder = itemDocumentBuilder.withAlias(alias, "fr");
                }
            }



            //Leader :
            itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,"Zoneleader", r.getLeader(), "leader", objectMapper.writeValueAsString(r.getLeader()));

            //ControlFields :
            for (Controlfield c : r.getControlfieldList()){
                if (c.getTag().equalsIgnoreCase("001")){
                    ppn = c.getValue(); //Pour construire le label
                }

                if (c.getTag().equalsIgnoreCase("003")){
                    itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,"URL pérenne", c.getValue(), "003", objectMapper.writeValueAsString(c));
                }

                //Ajout de statements pour tous les controlFields :
                itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,"Zone"+c.getTag(),c.getValue(),c.getTag(), objectMapper.writeValueAsString(c));
            }

            //DataFields :
            for (Datafield d : r.getDatafieldList()){

                if (d.getTag().equalsIgnoreCase("010")) {
                    for (Subfield s : d.getSubfieldList()) {
                        if (s.getCode().equalsIgnoreCase("a")) {
                            itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,"Identifiant ISNI", s.getValue(), "010", objectMapper.writeValueAsString(d));
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("101")) {
                    for (Subfield s : d.getSubfieldList()) {
                        if (s.getCode().equalsIgnoreCase("a")) {
                            itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,"Langue", s.getValue(), "101", objectMapper.writeValueAsString(d));
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("103")) {
                    for (Subfield s : d.getSubfieldList()) {
                        if (s.getCode().equalsIgnoreCase("a")) {
                            itemDocumentBuilder = this.addStmtTime(itemDocumentBuilder,"Date de naissance", s.getValue(), "103", objectMapper.writeValueAsString(d));
                        }
                        else if (s.getCode().equalsIgnoreCase("b")) {
                            itemDocumentBuilder = this.addStmtTime(itemDocumentBuilder,"Date de décès", s.getValue(), "103", objectMapper.writeValueAsString(d));
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("200")){
                    for (Subfield s : d.getSubfieldList()){
                        if (s.getCode().equalsIgnoreCase("a")){
                            itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,"Nom", s.getValue(), "200", objectMapper.writeValueAsString(d));
                        }
                        else if (s.getCode().equalsIgnoreCase("b")){
                            itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,"Prénom", s.getValue(), "200", objectMapper.writeValueAsString(d));
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("240")){
                    for (Subfield s : d.getSubfieldList()){
                        if (s.getCode().equalsIgnoreCase("t")){
                            itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,"Titre de l'oeuvre", s.getValue(), "240", objectMapper.writeValueAsString(d));
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("300")){
                    for (Subfield s : d.getSubfieldList()){
                        if (s.getCode().equalsIgnoreCase("a")){
                            itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,"Note biographique", s.getValue(), "240", objectMapper.writeValueAsString(d));
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("340")){
                    for (Subfield s : d.getSubfieldList()){
                        if (s.getCode().equalsIgnoreCase("a")){
                            itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,"Activité", s.getValue(), "240", objectMapper.writeValueAsString(d));
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("500")){
                    for (Subfield s : d.getSubfieldList()){
                        if (s.getCode().equalsIgnoreCase("3")){
                            itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,"Point d'accès en relation", s.getValue(), "240", objectMapper.writeValueAsString(d));
                        }
                    }
                }

                //Ajout de statements pour tous les dataFields (la valeur affichée est celle du premier subfield trouvé d.getSubfieldList().get(0).getValue()):
                if (d.getSubfieldList().size()>0) {
                    itemDocumentBuilder = this.addStmtString(itemDocumentBuilder, "Zone" + d.getTag(), d.getSubfieldList().get(0).getValue(), d.getTag(), objectMapper.writeValueAsString(d));
                }
            }

            //Ajout du label construit : 200$a, 200$b, 200$f [001]
            itemDocumentBuilder = itemDocumentBuilder.withLabel(label+" ["+ppn+"]", "fr");

        }
        catch (Exception e) {
            logger.error("Erreur sur la notice : " + r + " :" + e.getMessage());
            e.printStackTrace();
        }
        return itemDocumentBuilder.build();
    }

    //Ajoute un statement de datatype String à "itemDocumentBuild"
    //Utilise la map "props" pour les correspondances (P1,..), de la propriété à utiliser ("propriete") de valeur ("valeur").
    //La variable "marc" est le nom de la zone de la notice pour la référence ajoutée, et "valeurBrute" est la zone en entier, aussi pour la référence
    private ItemDocumentBuilder addStmtString(ItemDocumentBuilder itemDocumentBuilder, String propriete, String valeur, String marc, String valeurBrute){
        //"references": [{
        //    "P2": ["Marc_XXX"],
        //    "P1": ["<copie intégrale de la zone XXX>"]
        //}]

        if (valeur != null) {
            Reference reference;

            //Cas particulier de la propriété "Identifiant ISNI" :
            if (propriete.contains("Identifiant ISNI")) {
                reference = ReferenceBuilder
                        .newInstance()
                        .withPropertyValue(new PropertyIdValueImpl(props.get("Source d'import"), iriWikiBase),
                                Datamodel.makeStringValue("ISNI"))
                        .withPropertyValue(new PropertyIdValueImpl(props.get("Identifiant de la zone"), iriWikiBase),
                                Datamodel.makeStringValue("Marc_" + marc))
                        .withPropertyValue(new PropertyIdValueImpl(props.get("Données source de la zone"), iriWikiBase),
                                Datamodel.makeStringValue(valeurBrute))
                        .build();
            } else { //Par défaut :
                reference = ReferenceBuilder
                        .newInstance()
                        .withPropertyValue(new PropertyIdValueImpl(props.get("Identifiant de la zone"), iriWikiBase),
                                Datamodel.makeStringValue("Marc_" + marc))
                        .withPropertyValue(new PropertyIdValueImpl(props.get("Données source de la zone"), iriWikiBase),
                                Datamodel.makeStringValue(valeurBrute))
                        .build();
            }

            //Si la propriété ZoneXXX est connue :
            if (props.get(propriete) != null) {
                Statement statement = StatementBuilder
                        .forSubjectAndProperty(ItemIdValue.NULL, new PropertyIdValueImpl(props.get(propriete), iriWikiBase))
                        .withValue(Datamodel.makeStringValue(valeur.strip()))  //Strip car en 103 par exemple, il y a des valeurs commençant ou terminant par des espaces..
                        .withReference(reference)
                        .withQualifier(new ValueSnakImpl(new PropertyIdValueImpl(props.get("Type d'entité"), iriWikiBase), Datamodel.makeItemIdValue("Q1", iriWikiBase)))
                        .build();
                itemDocumentBuilder = itemDocumentBuilder.withStatement(statement);
            }
            //Sinon si inconnue :
            else {
                logger.error("Il faut ajouter la propriété : " + propriete + " au fichier ProprietesWB.txt");
            }
        }
        return itemDocumentBuilder;
    }

    //Ajoute un statement de datatype Time à "itemDocumentBuild"
    //Utilise la map "props" pour les correspondances (P1,..), de la propriété à utiliser ("propriete") de valeur ("valeur").
    //La variable "marc" est le nom de la zone de la notice pour la référence ajoutée, et "valeurBrute" est la zone en entier, aussi pour la référence
    private ItemDocumentBuilder addStmtTime(ItemDocumentBuilder itemDocumentBuilder, String propriete, String valeur, String marc, String valeurBrute){
        //"references": [{
        //    "P2": ["Marc_XXX"],
        //    "P1": ["<copie intégrale de la zone XXX>"]
        //}]

        if (valeur != null) {
            Reference reference = ReferenceBuilder
                    .newInstance()
                    .withPropertyValue(new PropertyIdValueImpl(props.get("Identifiant de la zone"), iriWikiBase),
                            Datamodel.makeStringValue("Marc_" + marc))
                    .withPropertyValue(new PropertyIdValueImpl(props.get("Données source de la zone"), iriWikiBase),
                            Datamodel.makeStringValue(valeurBrute))
                    .build();

            Integer annee = null;
            Integer mois = null;
            Integer jour = null;
            valeur = valeur.strip();
            //Pour les dates : 19xx; 19XX; 19..,19?? :
            valeur = valeur.replaceAll("x|X|\\.|\\?", "0");
            //Pour les dates : 1877/11/01, 1958-0000 :
            valeur = valeur.replaceAll("/|\\-", "");
            //Pour les dates : 1560    0 :
            valeur = valeur.replace(" 0", "").trim();
            try {
                if (Integer.parseInt(valeur) > 0) {
                    if (valeur.length() == 8) {
                        annee = Integer.parseInt(valeur.substring(0, 4));
                        mois = Integer.parseInt(valeur.substring(4, 6));
                        jour = Integer.parseInt(valeur.substring(6, 8));
                    } else if (valeur.length() == 6) {
                        annee = Integer.parseInt(valeur.substring(0, 4));
                        mois = Integer.parseInt(valeur.substring(4, 6));
                        jour = 1;
                    } else if (valeur.length() == 4) {
                        annee = Integer.parseInt(valeur.substring(0, 4));
                        mois = 1;
                        jour = 1;
                    }
                }
            } catch (Exception e) {
                //logger.warn("addStmtTime, attention, pour la date : "+valeur+" : "+e.getMessage());
            }

            //Si la propriété ZoneXXX est connue :
            if (props.get(propriete) != null) {
                //Si une valeur pour la date a été trouvée :
                if (annee != null && mois != null && jour != null) {
                    //Gestion des propriétés de datatype Time : Datamodel.makeTimeValue
                    //https://github.com/Wikidata/Wikidata-Toolkit/blob/c43d08dc5a449b24cf68dde9a58699aff603c430/wdtk-datamodel/src/test/java/org/wikidata/wdtk/datamodel/helpers/DatamodelTest.java
                    Statement statement = StatementBuilder
                            .forSubjectAndProperty(ItemIdValue.NULL, new PropertyIdValueImpl(props.get(propriete), iriWikiBase))
                            .withValue(
                                    Datamodel.makeTimeValue(annee, (byte) mois.intValue(), (byte) jour.intValue(),
                                            //Précision à l'année :
                                            // (byte) 0, (byte) 0, (byte) 0, TimeValue.PREC_YEAR, 0, 0, 0,
                                            TimeValue.CM_GREGORIAN_PRO)
                            )
                            .withReference(reference)
                            .withQualifier(new ValueSnakImpl(new PropertyIdValueImpl(props.get("Type d'entité"), iriWikiBase), Datamodel.makeItemIdValue("Q1", iriWikiBase)))
                            .build();
                    itemDocumentBuilder = itemDocumentBuilder.withStatement(statement);
                }
            }
            //Sinon si inconnue :
            else {
                logger.error("Il faut ajouter la propriété : " + propriete + " au fichier ProprietesWB.txt");
            }
        }
        return itemDocumentBuilder;
    }

}

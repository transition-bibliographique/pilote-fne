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
import org.wikidata.wdtk.datamodel.interfaces.*;

import java.util.Map;

@Service
public class DtoAutoriteToItem {

    private final Logger logger = LoggerFactory.getLogger(DtoAutoriteToItem.class);

    @Value("${wikibase.iri}")
    private String iriWikiBase;

    /**
     * DTO :
     * Notice MarcXML vers ItemDocument WB
     * @param r
     * @param props
     * @return
     */
    public ItemDocument unmarshallerNotice(Record r, Map<String,String> props)
    {
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

            //ControlFields :
            for (Controlfield c : r.getControlfieldList()){
                if (c.getTag().equalsIgnoreCase("001")){
                    ppn = c.getValue(); //Pour construire le label
                }

                if (c.getTag().equalsIgnoreCase("003")){
                    itemDocumentBuilder = itemDocumentBuilder.withDescription(c.getValue(), "fr");
                    itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,props,"URL pérenne", c.getValue(), "003", objectMapper.writeValueAsString(c));
                }

                //Ajout de statements pour tous les controlFields :
                itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,props,"Zone"+c.getTag(),c.getValue(),c.getTag(), objectMapper.writeValueAsString(c));
            }

            //DataFields :
            for (Datafield d : r.getDatafieldList()){

                if (d.getTag().equalsIgnoreCase("010")) {
                    for (Subfield s : d.getSubfieldList()) {
                        if (s.getCode().equalsIgnoreCase("a")) {
                            itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,props,"Identifiant ISNI", s.getValue(), "010", objectMapper.writeValueAsString(d));
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("101")) {
                    for (Subfield s : d.getSubfieldList()) {
                        if (s.getCode().equalsIgnoreCase("a")) {
                            itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,props,"Lanque de l'oeuvre", s.getValue(), "101", objectMapper.writeValueAsString(d));
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("103")) {
                    for (Subfield s : d.getSubfieldList()) {
                        if (s.getCode().equalsIgnoreCase("a")) {
                            itemDocumentBuilder = this.addStmtTime(itemDocumentBuilder,props,"Date de naissance", s.getValue(), "103", objectMapper.writeValueAsString(d));
                        }
                        else if (s.getCode().equalsIgnoreCase("b")) {
                            itemDocumentBuilder = this.addStmtTime(itemDocumentBuilder,props,"Date de décès", s.getValue(), "103", objectMapper.writeValueAsString(d));
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("200")){
                    for (Subfield s : d.getSubfieldList()){
                        if (s.getCode().equalsIgnoreCase("a")){
                            label += s.getValue();
                            itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,props,"Nom", s.getValue(), "200", objectMapper.writeValueAsString(d));
                        }
                        else if (s.getCode().equalsIgnoreCase("b")){
                            label += ", " + s.getValue();
                            itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,props,"Prénom", s.getValue(), "200", objectMapper.writeValueAsString(d));
                        }
                        else if (s.getCode().equalsIgnoreCase("f")){
                            label += ", " + s.getValue();
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("240")){
                    for (Subfield s : d.getSubfieldList()){
                        if (s.getCode().equalsIgnoreCase("t")){
                            itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,props,"Titre de l'oeuvre", s.getValue(), "240", objectMapper.writeValueAsString(d));
                        }
                    }
                }

                //Ajout de statements pour tous les dataFields (la valeur affichée est la $a):
                for (Subfield s : d.getSubfieldList()){
                    if (s.getCode().equalsIgnoreCase("a")){
                        itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,props,"Zone"+d.getTag(), s.getValue(), d.getTag(), objectMapper.writeValueAsString(d));
                    }
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
    private ItemDocumentBuilder addStmtString(ItemDocumentBuilder itemDocumentBuilder, Map<String,String> props, String propriete, String valeur, String marc, String valeurBrute){
        //"references": [{
        //    "P2": ["Marc_XXX"],
        //    "P1": ["<copie intégrale de la zone XXX>"]
        //}]

        Reference reference;

        //Cas particulier de la propriété "Identifiant ISNI" :
        if (propriete.contains("Identifiant ISNI")){
            reference = ReferenceBuilder
                    .newInstance()
                    .withPropertyValue(new PropertyIdValueImpl(props.get("Source d'import"), iriWikiBase),
                            Datamodel.makeStringValue("ISNI"))
                    .withPropertyValue(new PropertyIdValueImpl(props.get("Identifiant de la zone"), iriWikiBase),
                            Datamodel.makeStringValue("Marc_"+marc))
                    .withPropertyValue(new PropertyIdValueImpl(props.get("Données source de la zone"), iriWikiBase),
                            Datamodel.makeStringValue(valeurBrute))
                    .build();
        }
        else { //Par défaut :
            reference = ReferenceBuilder
                    .newInstance()
                    .withPropertyValue(new PropertyIdValueImpl(props.get("Identifiant de la zone"), iriWikiBase),
                            Datamodel.makeStringValue("Marc_"+marc))
                    .withPropertyValue(new PropertyIdValueImpl(props.get("Données source de la zone"), iriWikiBase),
                            Datamodel.makeStringValue(valeurBrute))
                    .build();
        }

        //Si la propriété ZoneXXX est connue :
        if (props.get(propriete)!=null) {
            Statement statement = StatementBuilder
                    .forSubjectAndProperty(ItemIdValue.NULL, new PropertyIdValueImpl(props.get(propriete), iriWikiBase))
                    .withValue(Datamodel.makeStringValue(valeur.strip()))  //Strip car en 103 par exemple, il y a des valeurs commençant ou terminant par des espaces..
                    .withReference(reference)
                    .build();
            itemDocumentBuilder = itemDocumentBuilder.withStatement(statement);
        }
        //Sinon si inconnue :
        else {
            logger.error("Il faut ajouter la propriété : "+propriete+" au fichier ProprietesWB.txt");
        }
        return itemDocumentBuilder;
    }

    //Ajoute un statement de datatype Time à "itemDocumentBuild"
    //Utilise la map "props" pour les correspondances (P1,..), de la propriété à utiliser ("propriete") de valeur ("valeur").
    //La variable "marc" est le nom de la zone de la notice pour la référence ajoutée, et "valeurBrute" est la zone en entier, aussi pour la référence
    private ItemDocumentBuilder addStmtTime(ItemDocumentBuilder itemDocumentBuilder, Map<String,String> props, String propriete, String valeur, String marc, String valeurBrute){
        //"references": [{
        //    "P2": ["Marc_XXX"],
        //    "P1": ["<copie intégrale de la zone XXX>"]
        //}]

        Reference reference = ReferenceBuilder
                .newInstance()
                .withPropertyValue(new PropertyIdValueImpl(props.get("Identifiant de la zone"), iriWikiBase),
                        Datamodel.makeStringValue("Marc_"+marc))
                .withPropertyValue(new PropertyIdValueImpl(props.get("Données source de la zone"), iriWikiBase),
                        Datamodel.makeStringValue(valeurBrute))
                .build();

        Integer annee = null;
        Integer mois = null;
        Integer jour = null;
        valeur = valeur.strip();
        //Pour les dates : 19XX; 19..,19?? :
        valeur = valeur.replaceAll("X|\\.|\\?","0");
        try {
            if (Integer.parseInt(valeur)>0) {
                if (valeur.length() == 8) {
                    annee = Integer.parseInt(valeur.substring(0,4));
                    mois = Integer.parseInt(valeur.substring(4,5));
                    jour = Integer.parseInt(valeur.substring(6,7));
                }
                else if (valeur.length() == 6){
                    annee = Integer.parseInt(valeur.substring(0,4));
                    mois = Integer.parseInt(valeur.substring(4,5));
                    jour=1;
                }
                else if (valeur.length() == 4){
                    annee = Integer.parseInt(valeur.substring(0,4));
                    mois=1;
                    jour=1;
                }
            }
        }
        catch (Exception e){
            logger.error("addStmtTime erreur sur la date : "+valeur+" : "+e.getMessage());
        }

        //Si la propriété ZoneXXX est connue :
        if (props.get(propriete)!=null) {
            //Si une valeur pour la date a été trouvée :
            if (annee!=null && mois!=null && jour!=null) {
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
                        .build();
                itemDocumentBuilder = itemDocumentBuilder.withStatement(statement);
            }
        }
        //Sinon si inconnue :
        else {
            logger.error("Il faut ajouter la propriété : "+propriete+" au fichier ProprietesWB.txt");
        }
        return itemDocumentBuilder;
    }

}

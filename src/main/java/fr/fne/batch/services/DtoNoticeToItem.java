package fr.fne.batch.services;

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
import org.wikidata.wdtk.datamodel.helpers.StatementBuilder;
import org.wikidata.wdtk.datamodel.implementation.PropertyIdValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

import java.util.Map;

@Service
public class DtoNoticeToItem {

    private final Logger logger = LoggerFactory.getLogger(DtoNoticeToItem.class);

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
                    itemDocumentBuilder = this.addStmt(itemDocumentBuilder,props,"URL pérenne", c.getValue());
                }

                //Ajout de statements pour tous les controlFields :
                itemDocumentBuilder = this.addStmt(itemDocumentBuilder,props,"Zone"+c.getTag(),c.getValue());
            }

            //DataFields :
            for (Datafield d : r.getDatafieldList()){

                if (d.getTag().equalsIgnoreCase("010")) {
                    for (Subfield s : d.getSubfieldList()) {
                        if (s.getCode().equalsIgnoreCase("a")) {
                            itemDocumentBuilder = this.addStmt(itemDocumentBuilder,props,"Identifiant ISNI", s.getValue());
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("101")) {
                    for (Subfield s : d.getSubfieldList()) {
                        if (s.getCode().equalsIgnoreCase("a")) {
                            itemDocumentBuilder = this.addStmt(itemDocumentBuilder,props,"Lanque de l'oeuvre", s.getValue());
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("103")) {
                    for (Subfield s : d.getSubfieldList()) {
                        if (s.getCode().equalsIgnoreCase("a")) {
                            itemDocumentBuilder = this.addStmt(itemDocumentBuilder,props,"Date de naissance", s.getValue());
                        }
                        else if (s.getCode().equalsIgnoreCase("b")) {
                            itemDocumentBuilder = this.addStmt(itemDocumentBuilder,props,"Date de décès", s.getValue());
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("200")){
                    for (Subfield s : d.getSubfieldList()){
                        if (s.getCode().equalsIgnoreCase("a")){
                            label += s.getValue();
                            itemDocumentBuilder = this.addStmt(itemDocumentBuilder,props,"Nom", s.getValue());
                        }
                        else if (s.getCode().equalsIgnoreCase("b")){
                            label += ", " + s.getValue();
                            itemDocumentBuilder = this.addStmt(itemDocumentBuilder,props,"Prénom", s.getValue());
                        }
                        else if (s.getCode().equalsIgnoreCase("f")){
                            label += ", " + s.getValue();
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("240")){
                    for (Subfield s : d.getSubfieldList()){
                        if (s.getCode().equalsIgnoreCase("t")){
                            itemDocumentBuilder = this.addStmt(itemDocumentBuilder,props,"Titre de l'oeuvre", s.getValue());
                        }
                    }
                }

                //Ajout de statements pour tous les dataFields :
                itemDocumentBuilder = this.addStmt(itemDocumentBuilder,props,"Zone"+d.getTag(),objectMapper.writeValueAsString(d));
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


    private ItemDocumentBuilder addStmt(ItemDocumentBuilder itemDocumentBuilder, Map<String,String> props, String propriete, String valeur){
        //Manque les références : "references": [{
        //                                                "P2": ["Marc_XXX"],
        //                                                "P1": ["<copie intégrale de la zone XXX>"]
        //                                        }]
        //Si la propriété ZoneXXX est connue :

        if (props.get(propriete)!=null) {
            Statement statement = StatementBuilder
                    .forSubjectAndProperty(ItemIdValue.NULL, new PropertyIdValueImpl(props.get(propriete), iriWikiBase))
                    .withValue(Datamodel.makeStringValue(valeur))
                    .build();
            itemDocumentBuilder = itemDocumentBuilder.withStatement(statement);
        }
        else {
            logger.error("Il faut ajouter la propriété : "+propriete+" au fichier ProprietesWB.txt");
        }
        return itemDocumentBuilder;
    }

}

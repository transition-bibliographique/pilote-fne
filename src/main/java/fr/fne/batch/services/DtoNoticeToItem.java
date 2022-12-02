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
     * @param record
     * @param props
     * @return
     */
    public ItemDocument unmarshallerNotice(Record record, Map<String,String> props)
    {
        ItemDocumentBuilder itemDocumentBuilder = ItemDocumentBuilder.forItemId(ItemIdValue.NULL);
        ObjectMapper objectMapper = new ObjectMapper();

        try {

            String label = "";
            String ppn = "";
            String description = "";
            for (Controlfield controlField : record.getControlfieldList()){
                if (controlField.getTag().equalsIgnoreCase("001")){
                    ppn = controlField.getValue();
                }
            }

            for (Datafield dataField : record.getDatafieldList()){
                if (dataField.getTag().equalsIgnoreCase("200")){
                    for (Subfield subfield : dataField.getSubfieldList()){
                        if (subfield.getCode().equalsIgnoreCase("a")){
                            label += subfield.getValue();
                        }
                        else if (subfield.getCode().equalsIgnoreCase("b")){
                            label += ", " + subfield.getValue();
                        }
                        else if (subfield.getCode().equalsIgnoreCase("f")){
                            label += ", " + subfield.getValue();
                        }
                    }
                    description = label; //objectMapper.writeValueAsString(dataField);
                }
            }

            Statement statement1 = StatementBuilder
                    .forSubjectAndProperty(ItemIdValue.NULL, new PropertyIdValueImpl(props.get("Identifiant de la zone"),iriWikiBase))
                    .withValue(Datamodel.makeStringValue(description)).build();

            Statement statement2 = StatementBuilder
                    .forSubjectAndProperty(ItemIdValue.NULL, new PropertyIdValueImpl(props.get("Nom"),iriWikiBase))
                    .withValue(Datamodel.makeStringValue(label)).build();

            itemDocumentBuilder = itemDocumentBuilder
                                    .withLabel(label+" ["+ppn+"]", "fr")
                                    .withDescription(description, "fr");

            itemDocumentBuilder = itemDocumentBuilder.withStatement(statement1);
            itemDocumentBuilder = itemDocumentBuilder.withStatement(statement2);
        }
        catch (Exception e) {
            logger.error("Erreur sur la notice : " + record + " :" + e.getMessage());
            e.printStackTrace();
        }
        return itemDocumentBuilder.build();
    }

}

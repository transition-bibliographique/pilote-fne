package fr.fne.batch.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import fr.fne.batch.model.autorite.Collection;
import fr.fne.batch.model.autorite.Record;
import fr.fne.batch.services.ChargementParSQL;
import fr.fne.batch.util.ApiWB;
import fr.fne.batch.util.DtoAutoriteToItem;
import fr.fne.batch.util.Format;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CollectionItemProcessor implements ItemProcessor<File, List<ItemDocument>>  {

    private final Logger logger = LoggerFactory.getLogger(CollectionItemProcessor.class);

    @Autowired
    private ApiWB apiWB;

    @Autowired
    private DtoAutoriteToItem dtoAutoriteToItem;

    @Autowired
    private Format format;

    @Override
    public List<ItemDocument> process (@NonNull File file) throws Exception {

        //Récupération de toutes les propriétés du WB
        Map<String, String> props = format.get();

        //Si pas de propriétés, alors création (pr éviter d'appeler 2x fois le BatchApplication : creationProprietes puis chargement
        if (props.size()==0){
            // Connextion à Wikibase et récupération du csrftoken
            String csrftoken = apiWB.connexionWB();
            logger.info("The csrftoken is : " + csrftoken);

            // Création du format
            format.createWithFile(csrftoken);
            // Map des propriétés
            props = format.get();
        }
        logger.info("Nombre de propriétés chargées : " + props.size());

        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        ObjectMapper objectMapper = new XmlMapper(xmlModule);
        objectMapper.registerModule(new JaxbAnnotationModule());
        Collection collection = objectMapper.readValue(new FileInputStream(file), Collection.class);

        List<ItemDocument> itemDocumentList = new ArrayList<>();
        for (Record record : collection.getRecordList()) {
            itemDocumentList.add(dtoAutoriteToItem.unmarshallerNotice(record, props));
        }

        // On retourne une list d'itemDocument pour le writer
        return itemDocumentList;
    }
}

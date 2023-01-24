package fr.fne.batch.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import fr.fne.batch.model.autorite.Collection;
import fr.fne.batch.model.autorite.Record;
import fr.fne.batch.util.DtoAutoriteToItem;
import lombok.NonNull;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CollectionItemProcessor implements ItemProcessor<File, List<ItemDocument>>  {

    @Autowired
    private DtoAutoriteToItem dtoAutoriteToItem;

    private final Map<String, String> props;

    public CollectionItemProcessor(Map<String, String> props){
        this.props = props;
    }

    @Override
    public List<ItemDocument> process (@NonNull File file) throws Exception {
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

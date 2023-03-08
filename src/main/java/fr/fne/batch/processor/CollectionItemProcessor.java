package fr.fne.batch.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

import fr.fne.batch.model.autorite.Collection;
import fr.fne.batch.model.autorite.Record;
import fr.fne.batch.model.dto.Personne;
import fr.fne.batch.util.DtoAutoriteToItem;
import lombok.NonNull;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class CollectionItemProcessor implements ItemProcessor<File, List<Personne>>  {

    @Autowired
    private DtoAutoriteToItem dtoAutoriteToItem;

    private final ObjectMapper objectMapper;

    public CollectionItemProcessor(){
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        objectMapper = new XmlMapper(xmlModule);
        objectMapper.registerModule(new JakartaXmlBindAnnotationModule());
    }

    @Override
    public List<Personne> process (@NonNull File file) throws Exception {

        Collection collection = objectMapper.readValue(new FileInputStream(file), Collection.class);

        List<Personne> personneList = new ArrayList<>();
        for (Record record : collection.getRecordList()) {
            personneList.add(dtoAutoriteToItem.unmarshallerNotice(record));
        }
        // On retourne une list de personne pour le writer
        return personneList;
    }
}

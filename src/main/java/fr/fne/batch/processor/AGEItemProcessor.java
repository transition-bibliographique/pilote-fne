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


public class AGEItemProcessor implements ItemProcessor<Personne, Personne> {

    @Override
    public Personne process (@NonNull Personne personne) throws Exception {
        return personne;
    }
}

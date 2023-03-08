package fr.fne.batch.model.autorite;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Getter
@XmlRootElement(name = "collection")
public class Collection {
    @XmlElement(name = "record")
    private List<Record> recordList;
}

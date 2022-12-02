package fr.fne.batch.model.autorite;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@NoArgsConstructor
@Getter
@XmlRootElement(name = "collection")
public class Collection {
    @XmlElement(name = "record")
    private List<Record> recordList;
}

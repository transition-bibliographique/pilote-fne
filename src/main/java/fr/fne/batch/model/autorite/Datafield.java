package fr.fne.batch.model.autorite;


import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

@NoArgsConstructor
@Getter
public class Datafield {
    @XmlAttribute(name="tag")
    private String tag;

    @XmlAttribute(name="ind1")
    private String ind1;

    @XmlAttribute(name="ind2")
    private String ind2;

    @XmlElement(name="subfield")
    private List<Subfield> subfieldList;
}

package fr.fne.batch.model.autorite;


import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

@NoArgsConstructor
@Getter
public class Controlfield {
    @XmlAttribute(name="tag")
    private String tag;
    @XmlValue
    private String value;
}

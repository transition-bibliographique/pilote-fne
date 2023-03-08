package fr.fne.batch.model.autorite;


import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@Getter
public class Controlfield {
    @XmlAttribute(name="tag")
    private String tag;
    @XmlValue
    private String value;
}

package fr.fne.batch.model.autorite;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class Subfield {
    @XmlAttribute(name="code")
    private String code;

    @XmlValue
    private String value;
}

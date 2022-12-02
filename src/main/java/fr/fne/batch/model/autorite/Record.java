package fr.fne.batch.model.autorite;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;

@NoArgsConstructor
@Getter
public class Record {
    @XmlElement(name = "leader")
    private String leader;
    @XmlElement(name = "controlfield")
    private List<Controlfield> controlfieldList;
    @XmlElement(name = "datafield")
    private List<Datafield> datafieldList;
}

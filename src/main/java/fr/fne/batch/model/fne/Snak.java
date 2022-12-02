package fr.fne.batch.model.fne;

import lombok.Getter;
import lombok.Setter;

public class Snak {

    @Getter @Setter private String snaktype;
    @Getter @Setter private String property;
    @Getter @Setter private Datavalue datavalue;

}

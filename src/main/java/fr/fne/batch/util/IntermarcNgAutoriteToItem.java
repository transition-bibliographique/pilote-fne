package fr.fne.batch.util;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.fne.batch.bnf.entity.model.SousZoneI;
import fr.fne.batch.bnf.entity.model.ZoneI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.ItemDocumentBuilder;
import org.wikidata.wdtk.datamodel.helpers.ReferenceBuilder;
import org.wikidata.wdtk.datamodel.helpers.StatementBuilder;
import org.wikidata.wdtk.datamodel.implementation.PropertyIdValueImpl;
import org.wikidata.wdtk.datamodel.implementation.ValueSnakImpl;
import org.wikidata.wdtk.datamodel.interfaces.*;

import fr.fne.batch.bnf.entity.model.IntermarcNG;

public class IntermarcNgAutoriteToItem {

    private final Logger logger = LoggerFactory.getLogger(IntermarcNgAutoriteToItem.class);

    @Value("${wikibase.iri}")
    private String iriWikiBase;

    //Map contenant les labels des propriétés utilisées (en fr, ex : Nom) et leur identifiant correspondant (ex : P1)
    private Map<String, String> props;

    public ItemDocument unmarshallerNotice (IntermarcNG intermarcNG, Map<String, String> proprietes) {

        ItemDocumentBuilder itemDocumentBuilder = ItemDocumentBuilder.forItemId(ItemIdValue.NULL);

        props = proprietes;

        //Il s'agit d'un type d'entité Personne (Q1)
        Statement statement = StatementBuilder
                .forSubjectAndProperty(ItemIdValue.NULL, new PropertyIdValueImpl(props.get("Type d'entité"), iriWikiBase))
                .withValue(Datamodel.makeItemIdValue("Q1", iriWikiBase))
                .build();
        itemDocumentBuilder = itemDocumentBuilder.withStatement(statement);


        for (ZoneI zi : intermarcNG.getZones()) {

            if("040".equals(zi.getCode())){
                for(SousZoneI szi : zi.getSousZones()){
                    if("040$q".equals(szi.getCode()) && "<Décès>".equals(szi.getValeur())){

                    }
                    if("040$q".equals(szi.getCode()) && "<Naissance>".equals(szi.getValeur())){

                    }
                }
            }

            if("010".equals(zi.getCode())){
                for(SousZoneI szi : zi.getSousZones()){
                    if("010$a".equals(szi.getCode())){

                    }
                }
            }

            if ("100".equals(zi.getCode())) {
                for (SousZoneI szi : zi.getSousZones()) {
                    // Nom
                    if ("100$a".equals(szi.getCode())) {
                        itemDocumentBuilder = this.addStmtString(itemDocumentBuilder, "Nom", szi);
                    }
                    if ("100$m".equals(szi.getCode())) {
                        itemDocumentBuilder = this.addStmtString(itemDocumentBuilder, "Prénom", szi);
                    }
                }
            }
        }

        return itemDocumentBuilder.build();
    }

    private ItemDocumentBuilder addStmtString (ItemDocumentBuilder itemDocumentBuilder, String propriete, SousZoneI szi) {
        //"references": [{
        //    "P2": ["IntermarcNG_XXX"],
        //    "P1": ["<copie intégrale de la zone XXX>"]
        //}]

        String codeSousZone = szi.getCode();
        String valeur = szi.getValeur();

        Reference reference = ReferenceBuilder
                .newInstance()
                .withPropertyValue(new PropertyIdValueImpl(props.get("Identifiant de la zone"), iriWikiBase),
                        Datamodel.makeStringValue("IntermarcNG_Zone_" + codeSousZone))
                .withPropertyValue(new PropertyIdValueImpl(props.get("Données source de la zone"), iriWikiBase),
                        Datamodel.makeStringValue(valeur))
                .build();

        //Si la propriété ZoneXXX est connue :
        if (props.get(propriete) != null) {
            Statement statement = StatementBuilder
                    .forSubjectAndProperty(ItemIdValue.NULL, new PropertyIdValueImpl(props.get(propriete), iriWikiBase))
                    .withValue(Datamodel.makeStringValue(valeur))
                    .withReference(reference)
                    .withQualifier(new ValueSnakImpl(new PropertyIdValueImpl(props.get("Type d'entité"), iriWikiBase), Datamodel.makeItemIdValue("Q1", iriWikiBase)))
                    .build();
            itemDocumentBuilder = itemDocumentBuilder.withStatement(statement);
        }
        //Sinon si inconnue :
        else {
            logger.error("Il faut ajouter la propriété : " + propriete + " au fichier ProprietesWB.txt");
        }
        return itemDocumentBuilder;
    }
}

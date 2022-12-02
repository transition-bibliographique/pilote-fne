package fr.fne.batch;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.wikidata.wdtk.datamodel.implementation.DatatypeIdImpl.JSON_DT_STRING;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import fr.fne.batch.model.autorite.Collection;
import fr.fne.batch.model.autorite.Controlfield;
import fr.fne.batch.model.autorite.Record;
import fr.fne.batch.services.util.api.UtilAPI;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.ItemDocumentBuilder;
import org.wikidata.wdtk.datamodel.helpers.PropertyDocumentBuilder;
import org.wikidata.wdtk.datamodel.helpers.StatementBuilder;
import org.wikidata.wdtk.datamodel.interfaces.*;
import org.wikidata.wdtk.util.WebResourceFetcherImpl;
import org.wikidata.wdtk.wikibaseapi.BasicApiConnection;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootTest
@Slf4j
class BatchApplicationTests {
	@Autowired
	private UtilAPI util;


	@Test
	void testJackson(){
		try {
			//Utilisation d'un dump des notices (5000 notices par fichier):
			//Le dump est disponible ici : /applis/portail/SitemapNoticesSudoc/noticesautorites/dump
			File[] fichiers = new File("C:/dump/").listFiles();

			ObjectMapper objectMapperJson = new ObjectMapper();

			int lanceCommit = 0;
			for (int i = 0; i < fichiers.length; i++) {
				JacksonXmlModule xmlModule = new JacksonXmlModule();
				xmlModule.setDefaultUseWrapper(false);
				ObjectMapper objectMapper = new XmlMapper(xmlModule);
				objectMapper.registerModule(new JaxbAnnotationModule());
				Collection collection = objectMapper.readValue(new FileInputStream(fichiers[i]), Collection.class);

				for (Record record : collection.getRecordList()) {
					for (Controlfield controlField : record.getControlfieldList()){
						if (controlField.getTag().equalsIgnoreCase("001")){
							String ppn = controlField.getValue();
							System.out.println("001 : "+ppn);
							System.out.println("Json 001 : "+objectMapperJson.writeValueAsString(controlField));
						}
					}
				}
			}
		}
		catch (Exception e){
			System.out.println("Erreur : "+e.getMessage());
		}
	}

	@Test
    /*
	Test utilisation des librairies WDTK
     */
	void testWDTK() {
		try {
			WikibaseDataEditor wbde;

			WebResourceFetcherImpl.setUserAgent("Test BatchApplication"); // ?

			BasicApiConnection wikibaseConnection = new BasicApiConnection("http://localhost/w/api.php");
			wikibaseConnection.login("admin", "change-this-password");
			wbde = new WikibaseDataEditor(wikibaseConnection, "http://localhost/entity/");
			//https://github.com/Wikidata/Wikidata-Toolkit-Examples/blob/master/src/examples/EditOnlineDataExample.java

			//Création d'une propriété (datatype String) :
			PropertyDocument newProperty =
					PropertyDocumentBuilder.forPropertyIdAndJsonDatatype(PropertyIdValue.NULL, JSON_DT_STRING)
							.withLabel("Propriété 1"+new Date().toString(), "fr")
							.build();
			PropertyDocument propertyDocument = wbde.createPropertyDocument(newProperty, "Creation de la propriete 1", Collections.emptyList());

			//System.out.println("Ici : "+JsonSerializer.getJsonString(propertyDocument));
			//{"type":"property","labels":{"fr":{"language":"fr","value":"Propri�t� 3"}},"descriptions":{},"aliases":{},"claims":{},"datatype":"string","lastrevid":6}


			//Création d'un item :
			ItemIdValue noid = ItemIdValue.NULL; // used when creating new items
			Statement statement1 = StatementBuilder
					.forSubjectAndProperty(noid, propertyDocument.getEntityId())
					.withValue(Datamodel.makeStringValue("Valeur de la propriété créée")).build();

			ItemDocument itemDocument = ItemDocumentBuilder.forItemId(noid)
					.withLabel("Item 1"+new Date().toString(), "fr")
					.withDescription("Description Item 1", "fr")
					.withStatement(statement1)//.withStatement(statement2).withStatement(statement3)
					.build();

			//System.out.println("Ici : "+JsonSerializer.getJsonString(itemDocument));
			//{"type":"item","labels":{"fr":{"language":"fr","value":"Item 1"}},"descriptions":{"fr":{"language":"fr","value":"Description Item 1"}},"aliases":{},"claims":{"P2":[{"rank":"normal","mainsnak":{"property":"P2","datavalue":{"value":"Valeur de la propri�t� cr��e","type":"string"},"snaktype":"value"},"type":"statement"}]},"sitelinks":{}}

			ItemDocument newItemDocument = wbde.createItemDocument(itemDocument,
					"Creation de Item 1", Collections.emptyList());

			//Problème 1 :
			// /!\ Si trop de création on a le msg : o.w.wdtk.wikibaseapi.WbEditingAction     : We are editing too fast. Pausing for XX milliseconds.
			// https://github.com/Wikidata/Wikidata-Toolkit/blob/master/wdtk-wikibaseapi/src/main/java/org/wikidata/wdtk/wikibaseapi/WbEditingAction.java#L599

			//Problème 2 :
			//Comment faire pour récupérer toutes les propriétés avec WDTK ? (voir ce qui est fait dans Format.get() ) .
			//https://github.com/Wikidata/Wikidata-Toolkit/blob/83ebfbcb25999a6d5d4216c5b42a5ea70ebfcfd5/wdtk-examples/src/main/java/org/wikidata/wdtk/examples/EditOnlineDataExample.java#L231
		}
		catch (Exception e){
			System.out.println("Erreur :"+e.getMessage());
		}
	}

	@Test
    /*
    Création d'une propriété avec un datatype string
     */
	void createPropertyString() {
		try {
			String csrftoken = util.connexionWB();

			Map<String, String> params = new LinkedHashMap<>();
			params = new LinkedHashMap<>();
			params.put("action", "wbeditentity");
			params.put("new", "property");
			params.put("token", csrftoken);
			params.put("format", "json");
			params.put("data", "{\"labels\":{\"fr\":{\"language\":\"fr\",\"value\":\"Propriété String\"}},\"datatype\":\"string\"}");

			JSONObject json = util.postJson(params);
			log.info("==>" + json.getJSONObject("entity").optString("id"));
		}
		catch (Exception e){
			log.error("Erreur : "+e.getMessage());
		}
	}

	@Test
	/*
    Création d'une propriété avec un datatype url
     */
	void createPropertyUrl() {
		try {
			String csrftoken = util.connexionWB();

			Map<String, String> params = new LinkedHashMap<>();
			params = new LinkedHashMap<>();
			params.put("action", "wbeditentity");
			params.put("new", "property");
			params.put("token", csrftoken);
			params.put("format", "json");
			params.put("data", "{\"labels\":{\"fr\":{\"language\":\"fr\",\"value\":\"Propriété URL\"}},\"datatype\":\"url\"}");

			JSONObject json = util.postJson(params);
			log.info("==>" + json.getJSONObject("entity").optString("id"));
		}
		catch (Exception e){
			log.error("Erreur : "+e.getMessage());
		}
	}

	@Test
     /*
    Création d'un item avec juste un label (pour créer le type d'entité "Personne")
    */
	void createItem() {
		try {
			String csrftoken = util.connexionWB();

			Map<String, String> params = new LinkedHashMap<>();
			params = new LinkedHashMap<>();
			params.put("action", "wbeditentity");
			params.put("new", "item");
			params.put("token", csrftoken);
			params.put("format", "json");
			params.put("data",
					"{\"labels\":{\"fr\":{\"language\":\"fr\",\"value\":\"IPP\"}}}");

			//log.info("data : "+params.get("data"));
			JSONObject json = util.postJson(params);
			log.info("==>" + json.toString());
		}
		catch (Exception e){
			log.error("Erreur : "+e.getMessage());
		}
	}

    @Test
     /*
    Test ajout des qualificatifs : "regroup" (string) = P490 et ordre (string) = P492
    En suivant le schéma de données json décrit ici : https://www.mediawiki.org/wiki/Wikibase/DataModel/JSON#Qualifiers
    */
    void createItemWithQualifiers() {
        try {
            String csrftoken = util.connexionWB();

            Map<String, String> params = new LinkedHashMap<>();
            params = new LinkedHashMap<>();
            params.put("action", "wbeditentity");
            params.put("new", "item");
            params.put("token", csrftoken);
            params.put("format", "json");

            params.put("data",
                    "{\"claims\":[" +
                        "{\"mainsnak\":{\"datavalue\":{\"type\":\"string\",\"value\":\"026360608\"},\"property\":\"P114\",\"snaktype\":\"value\"},\"rank\":\"normal\",\"type\":\"statement\"," +
                            "\"qualifiers\":[" +
                                "{\"datavalue\":{\"type\":\"string\",\"value\":\"1\"},\"property\":\"P490\",\"snaktype\":\"value\",\"datatype\":\"string\"}," +
                                "{\"datavalue\":{\"type\":\"string\",\"value\":\"1\"},\"property\":\"P492\",\"snaktype\":\"value\",\"datatype\":\"string\"}" +
                            "]}," +
                        "{\"mainsnak\":{\"datavalue\":{\"type\":\"string\",\"value\":\"20200306101325.000\"},\"property\":\"P116\",\"snaktype\":\"value\"},\"rank\":\"normal\",\"type\":\"statement\"}," +
                        "{\"mainsnak\":{\"datavalue\":{\"type\":\"string\",\"value\":\"http://catalogue.bnf.fr/ark:/12148/cb11862380q\"},\"property\":\"P193\",\"snaktype\":\"value\"},\"rank\":\"normal\",\"type\":\"statement\"}," +
                        "{\"mainsnak\":{\"datavalue\":{\"type\":\"string\",\"value\":\"BNF\"},\"property\":\"P162\",\"snaktype\":\"value\"},\"rank\":\"normal\",\"type\":\"statement\"}," +
                        "{\"mainsnak\":{\"datavalue\":{\"type\":\"string\",\"value\":\"20150918\"},\"property\":\"P163\",\"snaktype\":\"value\"},\"rank\":\"normal\",\"type\":\"statement\"}," +
                        "{\"mainsnak\":{\"datavalue\":{\"type\":\"string\",\"value\":\"172396506\"},\"property\":\"P120\",\"snaktype\":\"value\"},\"rank\":\"normal\",\"type\":\"statement\"}," +
                        "{\"mainsnak\":{\"datavalue\":{\"type\":\"string\",\"value\":\"sudoc\"},\"property\":\"P121\",\"snaktype\":\"value\"},\"rank\":\"normal\",\"type\":\"statement\"}," +
                        "{\"mainsnak\":{\"datavalue\":{\"type\":\"string\",\"value\":\"frBN000003046\"},\"property\":\"P122\",\"snaktype\":\"value\"},\"rank\":\"normal\",\"type\":\"statement\"}," +
                        "{\"mainsnak\":{\"datavalue\":{\"type\":\"string\",\"value\":\"FRBNF118623803\"},\"property\":\"P123\",\"snaktype\":\"value\"},\"rank\":\"normal\",\"type\":\"statement\"}," +
                        "{\"mainsnak\":{\"datavalue\":{\"type\":\"string\",\"value\":\"http://viaf.org/viaf/144248059\"},\"property\":\"P124\",\"snaktype\":\"value\"},\"rank\":\"normal\",\"type\":\"statement\"}," +
                        "{\"mainsnak\":{\"datavalue\":{\"type\":\"string\",\"value\":\"19840116afrey50      ba0\"},\"property\":\"P126\",\"snaktype\":\"value\"},\"rank\":\"normal\",\"type\":\"statement\"}" +
                    "]," +
                    "\"labels\":{\"fr\":{\"language\":\"fr\",\"value\":\"Belgique\"}}}");

            //log.info("data : "+params.get("data"));
            JSONObject json = util.postJson(params);
            log.info("==>" + json.toString());
        }
        catch (Exception e){
            log.error("Erreur : "+e.getMessage());
        }
    }

}

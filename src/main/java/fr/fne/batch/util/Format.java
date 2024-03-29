package fr.fne.batch.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.wikidata.wdtk.datamodel.helpers.ItemDocumentBuilder;
import org.wikidata.wdtk.datamodel.helpers.JsonSerializer;
import org.wikidata.wdtk.datamodel.helpers.PropertyDocumentBuilder;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class Format {
    private final Logger logger = LoggerFactory.getLogger(Format.class);

    @Autowired
    private ApiWB util;

    /*
     Création du format :
     - création des propriétés (à partir du fichier ProprietesWB.txt
     - création des types d'entités : Personne Q1 et IPP Q2
     */
    public void createWithFile(String csrftoken) throws Exception {
        BufferedReader reader = null;
                        
        //List<String> lines = FileUtils.readLines(file, "UTF-8");
        List<String> lines = new ArrayList<>();
		try {
			InputStream inputStream = new ClassPathResource("ProprietesWB.txt").getInputStream();
			
			reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

			String line = null;
			while ((line = reader.readLine()) != null) {
				lines.add(line);

			}
		} finally {
			if (reader != null)
				reader.close();
		}
        
        
        Iterator<String> line = lines.iterator();

        while (line.hasNext()){

            String prop = line.next();
            //Structure du fichier : Label|datatype
            String[] props = prop.split("\\|");
            //logger.info("props.len:"+props[0]+" "+props[1]);

            if (props.length==2) {
                String idProp = createProp(csrftoken, props[0], props[1]);
            }
        }

        // création des types d'entité
        createType(csrftoken,"Personne"); //Q1
        createType(csrftoken,"IPP"); //Q2
    }

    /*
     * Creation de la propriété : "label" et de type "type"
     */
    public String createProp(String csrftoken, String label, String type) throws Exception {

            PropertyDocument propertyDocument = PropertyDocumentBuilder.
                                            forPropertyIdAndJsonDatatype(PropertyIdValue.NULL, type).
                                            withLabel(label, "fr").build();

            Map<String, String> params = new LinkedHashMap<>();
            params.put("action", "wbeditentity");
            params.put("new", "property");
            params.put("token", csrftoken);
            params.put("format", "json");

            params.put("data", JsonSerializer.getJsonString(propertyDocument));

            JSONObject json = util.postJson(params);
            //logger.info("==>" + json.toString());
            return json.getJSONObject("entity").optString("id");
    }

    /*
    * Retourne une map avec le label et l'ID wikibase (Pxx)
     */
    public Map<String, String> get() throws Exception {
        // Getting namespace Property id
        JSONObject json = util.getJson("?action=query&format=json&meta=siteinfo&siprop=namespaces");
        Iterator<String> it = json.optJSONObject("query").optJSONObject("namespaces").keys();
        String idNamespaceProp = "";
        while (it.hasNext() && idNamespaceProp.isEmpty()) {
            String iterId = it.next();
            if (json.optJSONObject("query").optJSONObject("namespaces").optJSONObject(iterId).optString("canonical").equalsIgnoreCase("Property")) {
                idNamespaceProp = iterId;
            }
        }

        Map<String, String> properties = new LinkedHashMap<>();
        // TODO : tester si au delà de 500, ça fonctionne :)
        String lastProp = "";
        while (lastProp != null) {
            json = util.getJson("?action=query&format=json&list=allpages&apnamespace="
                    + idNamespaceProp + "&aplimit=500&apfrom=" + lastProp);
            JSONArray liste = json.optJSONObject("query").optJSONArray("allpages");
            for (int i = 0; i < liste.length(); i++) {
                JSONObject prop = liste.getJSONObject(i);
                // logger.info("Titre de la propriété : "+item.optString("title"));
                String title = prop.optString("title");
                String propertyId = title.replace("Property:", "");

                JSONObject property = util.getJson("?action=wbgetentities&format=json&ids=" + propertyId);

                JSONObject theProperty =  property.optJSONObject("entities").optJSONObject(propertyId);
                if (theProperty!=null){
                    if (theProperty.optJSONObject("labels")!=null){
                        if (theProperty.optJSONObject("labels").optJSONObject("fr")!=null) {
                            if (theProperty.optJSONObject("labels").optJSONObject("fr").optString("value") != null) {
                                String propertyValue = theProperty.optJSONObject("labels").optJSONObject("fr").optString("value");
                                logger.info("Property : " + propertyId + " value : " + propertyValue);
                                properties.put(propertyValue, propertyId);
                            }
                        }
                    }
                }
            }

            // logger.info("to continue :"+json.optJSONObject("continue").optString("apcontinue"));
            if (json.optJSONObject("continue") != null) {
                lastProp = json.optJSONObject("continue").optString("apcontinue");
            } else {
                lastProp = null;
            }
        }

        return properties;
    }


    /*
     * Creation du type d'entité : "label"
     */
    public String createType(String csrftoken, String label) throws Exception {

        //Ajout d'une description et d'un alias, pour que ces "types" soient présents en BDD (voir DatabaseInsert.java ligne 238
        ItemDocument itemDocument = ItemDocumentBuilder.forItemId(ItemIdValue.NULL)
                .withLabel(label, "fr")
                .withDescription("Entité : "+label, "fr")
                .withAlias(label, "fr")
                .build();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("action", "wbeditentity");
        params.put("new", "item");
        params.put("token", csrftoken);
        params.put("format", "json");

        params.put("data", JsonSerializer.getJsonString(itemDocument));

        JSONObject json = util.postJson(params);
        //logger.info("==>" + json.toString());
        return json.getJSONObject("entity").optString("id");
    }

}

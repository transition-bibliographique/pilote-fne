package fr.fne.batch.services.util.entities;

import fr.fne.batch.services.util.api.UtilAPI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
/*
CETTE CLASSE N'EST PLUS UTILISEE
POUR L'INSTANT ELLE EST CONSERVEE, MAIS SERA A SUPPRIMER
ELLE EST REMPLACEE PAR Entitites
 */
public class EntitiesJSOUP {
    private final Logger logger = LoggerFactory.getLogger(EntitiesJSOUP.class);

    /** The Constant STR009C. */
    public static final String STR009C = new String(new char[]{(char) 156});
    /** The Constant STR0098. */
    public static final String STR0098 = new String(new char[]{(char) 152});

    @Autowired
    private UtilAPI util;

    @Autowired
    private Format format;

    /*
     * Get the record/entity with the properties defined (props)
     */
    public String get(String csrftoken, Map<String,String> props, String record) {
        String entity = null;
        try {
            String noticeXML = record.replace(STR009C, "").replace(STR0098, "");
            //logger.info(noticeXML);
            Document theNotice = Jsoup.parse(noticeXML, "", Parser.xmlParser());
            theNotice.outputSettings(new Document.OutputSettings().prettyPrint(false));

            //title for the entity "label"
            String title = "";
            if (theNotice.getElementsByAttributeValueMatching("tag", "900|910|915|920|930|940|950|960|980").size() > 0) {
                title = theNotice
                        .getElementsByAttributeValueMatching("tag", "900|910|915|920|930|940|950|960|980")
                        .get(0).getElementsByAttributeValue("code", "a").text();
            }
            //logger.info("Titre : "+title);

            if (!title.isEmpty()) { // "Works" Tr case without title, example : id='5420922'

                // Doc how to create entites : item, prop, etc. => https://www.wikidata.org/w/api.php?action=help&modules=wbeditentity

                //String data = "{\"type\": \"item\",\"labels\":{\"fr\":{\"language\":\"fr\",\"value\":\"" + title + "\"}},\"claims\":{";
                JSONObject dataJ =  new JSONObject().put("type","item");

                JSONObject labels = new JSONObject().put("language","fr").put("value",title);
                dataJ.put("labels",new JSONObject().put("fr",labels));

                JSONObject descriptions = new JSONObject().put("language","fr").put("value","Description "+title);
                dataJ.put("descriptions",new JSONObject().put("fr",descriptions));

                JSONObject alias = new JSONObject().put("language","fr").put("value","Alias "+title);
                dataJ.put("aliases",new JSONObject().put("fr",new JSONArray().put(alias)));

                dataJ.put("claims",new JSONObject());

                dataJ = addClaims(csrftoken, props, theNotice, dataJ);

                /*if (data.endsWith(",")){
                    data = data.substring(0,data.length()-1);
                }
                data+="}}";*/

               /* Map<String, String> params = new LinkedHashMap<>();
                params.put("action", "wbeditentity");
                params.put("new", "item");
                params.put("token", csrftoken);
                params.put("format", "json");
                params.put("data",data);

                logger.info("data : "+data);
                JSONObject json = util.postJson(params);
                logger.info("==>" + json.toString());*/
                //logger.info("==>" + dataJ.toString());

                entity = dataJ.toString();
            } else {
                logger.info("==> no title for PPN : " + noticeXML);
            }

            // "claims" (déclarations) creation needed ? :
            // => https://www.wikidata.org/w/api.php?action=help&modules=wbcreateclaim

        } catch (Exception e) {
            logger.error("Error on the record :" + e.getMessage());
            e.printStackTrace();
        }
        finally {
            return entity;
        }
    }

    /*
     * Manage cases of :
     * controlfield (element with tag and no ind attributes) and
     * datafield (element with tag and ind attributes) which contains subfield
     */
    private JSONObject addClaims(String csrftoken, Map<String,String> props, Document theNotice, JSONObject dataJ) throws Exception{
        //Leader
        //logger.info("LEADER:"+theNotice.getElementsByTag("leader").get(0).text());
        dataJ = jsonClaim(csrftoken,props,"leader",theNotice.getElementsByTag("leader").get(0).text(),1,1, dataJ);

        int ordre = 1;

        //ControlField
        Elements zones = theNotice.getElementsByTag("controlfield");
        for (int i=0;i<zones.size();i++){
            Element zone = zones.get(i);
            ordre++;
            dataJ = jsonClaim(csrftoken,props,zone.attr("tag"),zone.text(),ordre,1, dataJ);
        //    logger.info("CONTROLFIELD:"+zone.attr("tag")+" texte:"+zone.text());
        }

        int regroupement = 0;
        //DataField
        zones = theNotice.getElementsByTag("datafield");
        for (int i=0;i<zones.size();i++){
            Element zone = zones.get(i);
            ordre++;
            regroupement = 0;
            String tagEnCours = zone.attr("tag");

            //TODO : ajouter les indicateurs
            /*String ind1 = zone.attr("ind1");
            String ind2 = zone.attr("ind2");
            if ((ind1 != null && !ind1.trim().isEmpty()) || (ind2 != null && !ind2.trim().isEmpty()) ) {
                if (ind1 != null && !ind1.trim().isEmpty()) {
                    tagEnCours += ind1;
                }
                else {
                    tagEnCours += "#";
                }
                if (ind2 != null && !ind2.trim().isEmpty()) {
                    tagEnCours += ind2;
                }
                else {
                    tagEnCours += "#";
                }
            }*/


            Elements subZones = zone.getElementsByTag("subfield");
            for (int j=0;j<subZones.size();j++){
                String subZoneTag = tagEnCours+"$";
                Element subZone = subZones.get(j);
                String value = subZone.text();
                subZoneTag+=subZone.attr("code");
                regroupement++;
//logger.info("SUBFIELD : tagEnCours: "+tagEnCours+" subZoneTag:"+subZoneTag+" Value:"+value+ "regroupement:"+regroupement+" ordre:"+ordre);
                dataJ = jsonClaim(csrftoken,props,subZoneTag,value, ordre, regroupement, dataJ);
            }

        }
        return dataJ;
    }

    /*
     * Generate the json string to pass to the wikibase API
     */
    private JSONObject jsonClaim(String csrftoken, Map<String,String> props, String tag, String value, int ordre, int regroupement, JSONObject dataJ) throws Exception{
        //logger.info(tag);

        /*if (props.get(tag)==null){
            String idProp = properties.create(csrftoken,tag); // Création de la propriété
            props.put(tag, idProp); // Ajout dans la map
        }*/

        //On ne gère ici que des propriétés connues car on teste un chargement sans commit (on ne peut donc pas en créer à la volée = commit)
        if (props.get(tag)!=null) {

            JSONObject claim = new JSONObject(
                    "{\"mainsnak\":{\"snaktype\":\"value\",\"property\":\"" +
                            props.get(tag) + "\",\"datavalue\":{\"value\":\"" + value.replaceAll("\"","\\\\\"") +
                            "\",\"type\":\"string\"}},\"type\":\"statement\",\"rank\":\"normal\"," +
                            "\"qualifiers\":[" +
                            "{\"datavalue\":{\"type\":\"string\",\"value\":\"" + ordre + "\"},\"property\":\"" + props.get("Ordre") + "\",\"snaktype\":\"value\",\"datatype\":\"string\"}," +
                            "{\"datavalue\":{\"type\":\"string\",\"value\":\"" + regroupement + "\"},\"property\":\"" + props.get("Regroupement") + "\",\"snaktype\":\"value\",\"datatype\":\"string\"}" +
                            "]}"
            );
            //logger.info("Ajout de : "+ new JSONArray().put(claim).toString());

            if (dataJ.getJSONObject("claims").optJSONArray(props.get(tag))==null){
                dataJ.getJSONObject("claims").put(props.get(tag),new JSONArray().put(claim));
            }
            else {
                dataJ.getJSONObject("claims").optJSONArray(props.get(tag)).put(claim);
            }

        /*    claim = "\""+props.get(tag)+"\":"+"[{\"mainsnak\":{\"snaktype\":\"value\",\"property\":\"" +
                    props.get(tag) + "\",\"datavalue\":{\"value\":\"" + value +
                    "\",\"type\":\"string\"}},\"type\":\"statement\",\"rank\":\"normal\"," +
                    "\"qualifiers\":[" +
                    "{\"datavalue\":{\"type\":\"string\",\"value\":\"" + regroupement + "\"},\"property\":\"" + props.get("Regroupement") + "\",\"snaktype\":\"value\",\"datatype\":\"string\"}," +
                    "{\"datavalue\":{\"type\":\"string\",\"value\":\"" + ordre + "\"},\"property\":\"" + props.get("Ordre") + "\",\"snaktype\":\"value\",\"datatype\":\"string\"}" +
                    "]}],";

         */
        }
        else {
            logger.info("Tag "+tag+" à insérer dans ProprietesWB.txt");
        }
        return dataJ;
    };

}

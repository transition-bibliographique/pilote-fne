package fr.fne.batch.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Très largement inspiré de ce dépôt : https://github.com/jze/wikibase-insert
 * Et mis à jour en suivant les requêtes SQL de ce dépôt : https://github.com/UB-Mannheim/RaiseWikibase/blob/main/RaiseWikibase/dbconnection.py
 */
public class DatabaseInsert {

    private final Logger logger = LoggerFactory.getLogger(DatabaseInsert.class);
    private final static String LANG = "fr"; //Lang des insertions
    private final static int ACTOR = 1;
    private final Connection connection;
    /**
     * Do not read ids from the database for every item. Assign them once and assume no other process writes to the
     * database.
     */
    private PreparedStatement pstmtInsertText;
    private PreparedStatement pstmtInsertPage;
    private PreparedStatement pstmtInsertRevision;
    private PreparedStatement pstmtInsertComment;
    private PreparedStatement pstmtInsertRevisionComment;
    private PreparedStatement pstmtInsertRevisionActor;
    private PreparedStatement pstmtInsertContent;
    private PreparedStatement pstmtInsertSlots;
    private PreparedStatement pstmtUpdateWbIdCounters;
    private PreparedStatement pstmtSelectLastItemId;
    private PreparedStatement pstmtSelectItem;
    private PreparedStatement pstmtInsertRecentChanges;
    private PreparedStatement pstmtInsertOldRecentChanges;

    //ACT
    private PreparedStatement pstmtSelect_wbt_text;
    private PreparedStatement pstmtInsert_wbt_text;
    private PreparedStatement pstmtSelect_wbt_text_in_lang;
    private PreparedStatement pstmtInsert_wbt_text_in_lang;
    private PreparedStatement pstmtSelect_wbt_term_in_lang;
    private PreparedStatement pstmtInsert_wbt_term_in_lang;
    private PreparedStatement pstmtInsert_wbt_item_terms;
    private Map<String, String> wbt_type = new HashMap<>();

    private int lastQNumber = 0;
    private  String itemId;
    private long textId = 0;
    private long pageId = 0;
    private long revId = 0;
    private long commentId = 0;
    private long contentId = 0;
    private int contentModelItem;
    private long recentChangeId = 0;
    private String comment = "/* wbeditentity-create:2|fr */ ";
    private long oldRevId = 0;
    private long oldDataLength = 0;
    private long wbx_id_cursor = 0;
    private long wbxl_id_cursor = 0;
    private long wbtl_id_cursor = 0;



    public DatabaseInsert(Connection con) throws SQLException, IOException {
        this.connection = con;
        afterPropertiesSet();
    }

    private static String sha1base36(String s) {
        return new BigInteger(DigestUtils.sha1Hex(s), 16).toString(36);
    }


    public void afterPropertiesSet() throws SQLException {
        prepareDatabaseConnection();
    }

    public void destroy() throws Exception {
        pstmtInsertText.close();
        pstmtInsertPage.close();
        pstmtInsertRevision.close();
        pstmtInsertComment.close();
        pstmtInsertRevisionComment.close();
        pstmtInsertRevisionActor.close();
        pstmtInsertContent.close();
        pstmtInsertSlots.close();
        pstmtUpdateWbIdCounters.close();
        pstmtSelectLastItemId.close();
        pstmtSelectItem.close();
        pstmtInsertRecentChanges.close();

        //ACT
        pstmtSelect_wbt_text.close();
        pstmtInsert_wbt_text.close();
        pstmtSelect_wbt_text_in_lang.close();
        pstmtInsert_wbt_text_in_lang.close();
        pstmtSelect_wbt_term_in_lang.close();
        pstmtInsert_wbt_term_in_lang.close();
        pstmtInsert_wbt_item_terms.close();

        connection.close();
    }

    public void commit() throws SQLException {
        //Version avec executeBatch()
        pstmtInsertText.executeBatch();
        pstmtInsertPage.executeBatch();
        pstmtInsertRevision.executeBatch();
        pstmtInsertComment.executeBatch();
        pstmtInsertRevisionComment.executeBatch();
        pstmtInsertRevisionActor.executeBatch();
        pstmtInsertContent.executeBatch();
        pstmtInsertSlots.executeBatch();
        pstmtInsertRecentChanges.executeBatch();
        pstmtUpdateWbIdCounters.executeBatch();
        pstmtInsert_wbt_item_terms.executeBatch();
        //A enlever si pas executeBatch()

        connection.commit();
    }

    private void prepareDatabaseConnection() throws SQLException {

        pstmtInsertText = connection.prepareStatement("INSERT INTO text VALUES(?,?,'utf-8')");
        pstmtInsertPage = connection.prepareStatement("REPLACE INTO page VALUES(?,120,?,'',0,0,rand(1),?,?,?,?,'wikibase-item',NULL)");
        pstmtInsertComment = connection.prepareStatement("INSERT INTO comment VALUES(?,?,?,NULL)");
        pstmtInsertContent = connection.prepareStatement("INSERT INTO content VALUES(?,?,?,?,?)");

        pstmtInsertRevision = connection.prepareStatement("INSERT INTO revision VALUES(?,?,0,0,?,0,0,?,?,?)");
        pstmtInsertRevisionComment = connection.prepareStatement("INSERT INTO revision_comment_temp VALUES (?,?)");
        pstmtInsertRevisionActor = connection.prepareStatement("INSERT INTO revision_actor_temp VALUES(?,?,?,?)");

        pstmtInsertSlots = connection.prepareStatement("INSERT INTO slots VALUES( ?, 1, ?, ?)");
        pstmtUpdateWbIdCounters = connection.prepareStatement("UPDATE wb_id_counters SET id_value=? WHERE id_type='wikibase-item'");
        pstmtSelectLastItemId = connection.prepareStatement("SELECT id_value  AS next_id from wb_id_counters where id_type = 'wikibase-item'");
        pstmtSelectItem = connection.prepareStatement("SELECT * FROM page WHERE page_namespace=120 AND page_title=?");

        pstmtInsertRecentChanges = connection.prepareStatement("INSERT INTO recentchanges VALUES (?,?,?,'120',?,?,0,0,0,?,?,?,?,?,2,'172.18.0.1',?,?,0,0,NULL,'','')");
        pstmtInsertOldRecentChanges = connection.prepareStatement("SELECT rc_new_len FROM recentchanges WHERE rc_id <= ? ORDER BY rc_id DESC LIMIT 1"); //?? https://github.com/UB-Mannheim/RaiseWikibase/blob/main/RaiseWikibase/dbconnection.py#L312

        pstmtSelect_wbt_text  = connection.prepareStatement("SELECT wbx_id FROM wbt_text WHERE wbx_text=?");
        pstmtInsert_wbt_text  = connection.prepareStatement("INSERT INTO wbt_text VALUES(?,?)");

        pstmtSelect_wbt_text_in_lang = connection.prepareStatement("SELECT wbxl_id FROM wbt_text_in_lang, wbt_text " +
                "WHERE wbxl_language=? AND wbxl_text_id=wbx_id AND wbx_text=?");
        pstmtInsert_wbt_text_in_lang = connection.prepareStatement("INSERT INTO wbt_text_in_lang VALUES(?,?,?)");

        pstmtSelect_wbt_term_in_lang = connection.prepareStatement("SELECT wbtl_id FROM wbt_term_in_lang, wbt_text_in_lang, " +
                "wbt_text WHERE wbtl_text_in_lang_id=wbxl_id AND wbtl_type_id=? " +
                "AND wbxl_language=? AND wbxl_text_id=wbx_id AND wbx_text=?");
        pstmtInsert_wbt_term_in_lang = connection.prepareStatement("INSERT INTO wbt_term_in_lang VALUES(?,?,?)");

        pstmtInsert_wbt_item_terms = connection.prepareStatement("INSERT IGNORE INTO wbt_item_terms VALUES(NULL,?,?)");


        //Récupération du dernier Q créé (au départ il y a déjà les 2 Q : Personne et IPP)
        ResultSet rs = pstmtSelectLastItemId.executeQuery();
        if (rs.next()) {
            lastQNumber = rs.getInt(1);
        }
        rs.close();

        // Check if the Q-number is really unused
        while (itemExists("Q" + (lastQNumber + 1))) {
            lastQNumber++;
        }

        //ACT : le format contient déjà des types d'entité, des Q, donc pas besoin de cet insert à l'initialisation :
        //connection.createStatement().execute("INSERT INTO wb_id_counters VALUES(1, 'wikibase-item')");

        final Statement stmt = connection.createStatement();
        rs = stmt.executeQuery("SELECT max(page_id) FROM page");
        if (rs.next()) {
            pageId = rs.getLong(1);
        }
        rs.close();

        rs = stmt.executeQuery("SELECT max(old_id) FROM text");
        if (rs.next()) {
            textId = rs.getLong(1);
        }
        rs.close();

        rs = stmt.executeQuery("SELECT max(comment_id) FROM comment");
        if (rs.next()) {
            commentId = rs.getLong(1);
        }
        rs.close();

        rs = stmt.executeQuery("SELECT max(content_id) FROM content");
        if (rs.next()) {
            contentId = rs.getLong(1);
        }
        rs.close();

        rs = stmt.executeQuery("SELECT max(rc_id) FROM recentchanges");
        if (rs.next()) {
            recentChangeId = rs.getLong(1);
        }
        rs.close();

        rs = stmt.executeQuery("SELECT max(rev_id) FROM revision");
        if (rs.next()) {
            revId = rs.getLong(1);
        }
        rs.close();

        //ACT : contient label, description  et alias
        //Ajout de ces 2 lignes si pas d'Item/Q avec description et/ou alias créé au départ
        //connection.createStatement().execute("INSERT INTO wbt_type (wby_name) VALUES('description')");
        //connection.createStatement().execute("INSERT INTO wbt_type (wby_name) VALUES('alias')");

        rs = stmt.executeQuery("SELECT wby_name, wby_id FROM wbt_type");
        while (rs.next()){
            wbt_type.put(rs.getString(1),rs.getString(2));
        }
        rs.close();

        rs = stmt.executeQuery("SELECT max(wbx_id) FROM wbt_text");
        if (rs.next()) {
            wbx_id_cursor = rs.getLong(1);
        }
        rs.close();

        rs = stmt.executeQuery("SELECT max(wbxl_id) FROM wbt_text_in_lang");
        if (rs.next()) {
            wbxl_id_cursor = rs.getLong(1);
        }
        rs.close();

        rs = stmt.executeQuery("SELECT max(wbtl_id) FROM wbt_term_in_lang");
        if (rs.next()) {
            wbtl_id_cursor = rs.getLong(1);
        }
        rs.close();

        rs = stmt.executeQuery("SELECT model_id FROM content_models WHERE model_name='wikibase-item'");
        if( rs.next()) {
            contentModelItem = rs.getInt(1);
        } else {
            connection.createStatement().execute("INSERT INTO content_models (model_name ) VALUES('wikibase-item')");
            rs = connection.createStatement().executeQuery("SELECT model_id FROM content_models WHERE model_name='wikibase-item'");
            rs.next();
            contentModelItem = rs.getInt(1);
            logger.debug("Created content model for wikibase-item with id "+contentModelItem );
        }
        rs.close();
        stmt.close();
    }

    /**
     * Check if the specified item id exists.
     *
     * @param itemId a Q id
     * @return <code>true</code> if the item exists in the Wikibase database
     */
    public boolean itemExists(String itemId) throws SQLException {
        pstmtSelectItem.setString(1, itemId);
        final ResultSet rs = pstmtSelectItem.executeQuery();
        boolean result = rs.next();
        rs.close();
        return result;
    }


    public void updateItem(String jsonString) throws SQLException {
        //https://github.com/UB-Mannheim/RaiseWikibase/blob/main/RaiseWikibase/dbconnection.py

        //logger.info("JSON : "+jsonString);
        final JSONObject json = new JSONObject(jsonString);

        if (json.optString("id") != "") {
            logger.info("Id trouvé : " + json.optString("id"));
            itemId = json.optString("id");
        }

        //"""SELECT page_id, page_latest FROM page WHERE page_title=?
        //[page_id, rev_id] = self.get_page_latest(page_title=page_title, namespace=namespace)
        pstmtSelectItem.setString(1, json.optString("id"));
        ResultSet rs = pstmtSelectItem.executeQuery();
        if (rs.next()) {
            pageId = rs.getLong("page_id");
            oldRevId = rs.getLong("page_latest");
        }
        rs.close();

        pstmtInsertOldRecentChanges.setLong(1, pageId);
        rs = pstmtInsertOldRecentChanges.executeQuery();
        if (rs.next()) {
            oldDataLength = rs.getLong("rc_new_len");
        }
        rs.close();

        revId++;
        comment = "/* wbeditentity-edit:2|fr */ " ;

        insertItem(jsonString, false);
    }

    public void createItem(String jsonString) throws SQLException {
        //logger.info("JSON : "+jsonString);
        final JSONObject json = new JSONObject(jsonString);

        if (json.optString("id") != "") {
            logger.info("Id trouvé : " + json.optString("id"));
        }

        lastQNumber++;
        itemId = "Q" + lastQNumber;
        pageId++;
        revId++;
        comment = "/* wbeditentity-create:2|fr */ " ;

        insertItem(jsonString, true);
    }

    private void insertItem(String jsonString, boolean create) throws SQLException {

        /*logger.info("lastQNumber:"+lastQNumber+" itemId:"+itemId+" textId:"+textId
        +" pageId:"+pageId+" revId:"+revId+" commentId:"+commentId+" contentId:"+contentId
        +" contentModelItem:"+contentModelItem+" recentChangeId:"+recentChangeId
                + " oldRevId:"+oldRevId+" oldDataLength:"+oldDataLength);
        */
        //logger.info("JSON : "+jsonString);
        final JSONObject json = new JSONObject(jsonString);

        //Moins une heure : pour que le wdqs-updater ne traite pas ces insertions
        final String timestamp = LocalDateTime.now().minusHours(1).format(DateTimeFormatter.ISO_DATE_TIME).replaceAll("[T:-]", "").substring(0, 14);

        //Cas de caractères UTF-8 qui, passés en VARBINARY, font plus de 255 :
        String label = json.getJSONObject("labels").getJSONObject(LANG).optString("value");
        byte[] byteArrray = label.getBytes();

        //Si substring jusqu'à 254, problème dans certains cas...
        if (byteArrray.length > 253) {
            label = new String(Arrays.copyOfRange(byteArrray, 0, 253));
        }

        String description = null;
        if (json.optJSONObject("descriptions")!=null){
            description = json.optJSONObject("descriptions").optJSONObject(LANG).optString("value");
            if (description!=null && description.length() > 254)
                description = description.substring(0,254);
        }

        JSONArray aliases = null;
        if (json.optJSONObject("aliases")!=null){
            aliases = json.optJSONObject("aliases").optJSONArray(LANG);
        }

        // /!\ Un label + description ne peuvent pas être insérés 2 x . Ce test n'est pas fait par ce programme /!\

        //Le label et la description doivent être différents : ok avec le test ci-dessous
        if (!label.equals(description)) {

            // Does the specified item already have an ID?
            if (!json.has("id")) {
                json.put("id", itemId);

                // All statements also need this ID
                if (json.has("claims")) {
                    final JSONObject claims = json.getJSONObject("claims");
                    for (String claim : claims.keySet()) {
                        final JSONArray list = claims.getJSONArray(claim);
                        for (int i = 0; i < list.length(); i++) {
                            list.getJSONObject(i).put("id", itemId + "$" + UUID.randomUUID().toString());
                        }
                    }
                }
            }



            try {
                final String data = json.toString();

                //ACT ajout des tables wbt_*  au début car un doublon peut provoquer une exception :
                insert_wbt_table(label, "label");

                if (description != null) {
                    insert_wbt_table(description, "description");
                }

                if (aliases != null) {
                    for (int i = 0; i < aliases.length(); i++) {
                        String alias = aliases.getJSONObject(i).optString("value");
                        if (alias.length() > 254)
                            alias = alias.substring(0, 254);

                        insert_wbt_table(alias, "alias");
                    }
                }

                textId++;
                pstmtInsertText.setLong(1, textId);
                pstmtInsertText.setString(2, data);
                executeUpdate(pstmtInsertText);

                pstmtInsertPage.setLong(1, pageId);
                pstmtInsertPage.setString(2, itemId);
                pstmtInsertPage.setString(3, timestamp);
                pstmtInsertPage.setString(4, timestamp);
                pstmtInsertPage.setLong(5, revId);//textId OK?
                pstmtInsertPage.setInt(6, data.length());

                executeUpdate(pstmtInsertPage);

                pstmtInsertRevision.setLong(1, revId);
                pstmtInsertRevision.setLong(2, pageId);
                pstmtInsertRevision.setString(3, timestamp);
                pstmtInsertRevision.setInt(4, data.length());
                if (create) {
                    pstmtInsertRevision.setLong(5, 0);
                }
                else {
                    pstmtInsertRevision.setLong(5, revId);
                }
                pstmtInsertRevision.setString(6, sha1base36(data));
                executeUpdate(pstmtInsertRevision);

                String commentValue = comment + label + ", " + description;

                pstmtInsertComment.setInt(2, commentValue.hashCode());
                pstmtInsertComment.setString(3, commentValue);

                commentId++;
                pstmtInsertComment.setLong(1, commentId);
                executeUpdate(pstmtInsertComment);

                pstmtInsertRevisionComment.setLong(1, revId);
                pstmtInsertRevisionComment.setLong(2, commentId);
                executeUpdate(pstmtInsertRevisionComment);

                pstmtInsertRevisionActor.setLong(1, revId);
                pstmtInsertRevisionActor.setInt(2, ACTOR);
                pstmtInsertRevisionActor.setString(3, timestamp);
                pstmtInsertRevisionActor.setLong(4, pageId);
                executeUpdate(pstmtInsertRevisionActor);

                pstmtInsertContent.setInt(2, data.length());
                pstmtInsertContent.setString(3, sha1base36(data));
                pstmtInsertContent.setInt(4, contentModelItem);
                pstmtInsertContent.setString(5, "tt:" + textId);

                contentId++;
                pstmtInsertContent.setLong(1, contentId);
                executeUpdate(pstmtInsertContent);

                pstmtInsertSlots.setLong(1, textId);
                pstmtInsertSlots.setLong(2, contentId);
                pstmtInsertSlots.setLong(3, textId);
                executeUpdate(pstmtInsertSlots);

                //ACT
                recentChangeId++;
                pstmtInsertRecentChanges.setLong(1, recentChangeId);
                pstmtInsertRecentChanges.setString(2, timestamp);
                pstmtInsertRecentChanges.setInt(3, ACTOR);

                pstmtInsertRecentChanges.setString(4, itemId);
                pstmtInsertRecentChanges.setLong(5, commentId);

                pstmtInsertRecentChanges.setLong(6, pageId);
                pstmtInsertRecentChanges.setLong(7, revId);

                pstmtInsertRecentChanges.setLong(8, oldRevId); //0 si create

                if (create) {
                    pstmtInsertRecentChanges.setLong(9, 1);
                    pstmtInsertRecentChanges.setString(10, "mw.new");
                }
                else {
                    pstmtInsertRecentChanges.setLong(9, 0);
                    pstmtInsertRecentChanges.setString(10, "mw.edit");
                }

                pstmtInsertRecentChanges.setLong(11, oldDataLength); //0 si create

                pstmtInsertRecentChanges.setInt(12, data.length());
                executeUpdate(pstmtInsertRecentChanges);

                if (create) {
                    pstmtUpdateWbIdCounters.setInt(1, lastQNumber);
                    executeUpdate(pstmtUpdateWbIdCounters);
                }

                //logger.info("Nouveau Q:"+lastQNumber);
            } catch (Exception e) {
                logger.error("Erreur titre déjà présent ? : " + label + " exception:" + e.getMessage());
            }
        }
    }

    private void insert_wbt_table(String texte, String type) throws Exception{

        final Statement stmt = connection.createStatement();

        long wbx_id = 0;
        pstmtSelect_wbt_text.setString(1, texte);
        ResultSet rs = pstmtSelect_wbt_text.executeQuery();
        if (rs.next()) {
            wbx_id = rs.getLong(1);
        }
        else {
            wbx_id_cursor++;
            pstmtInsert_wbt_text.setLong(1, wbx_id_cursor);
            pstmtInsert_wbt_text.setString(2, texte);
            pstmtInsert_wbt_text.executeUpdate();
            wbx_id = wbx_id_cursor;
        }
        rs.close();

        long wbxl_id = 0;
        pstmtSelect_wbt_text_in_lang.setString(1, LANG);
        pstmtSelect_wbt_text_in_lang.setString(2, texte);
        rs = pstmtSelect_wbt_text_in_lang.executeQuery();
        if (rs.next()) {
            wbxl_id = rs.getLong(1);
        }
        else {
            wbxl_id_cursor++;
            pstmtInsert_wbt_text_in_lang.setLong(1, wbxl_id_cursor);
            pstmtInsert_wbt_text_in_lang.setString(2, LANG);
            pstmtInsert_wbt_text_in_lang.setLong(3, wbx_id);
            pstmtInsert_wbt_text_in_lang.executeUpdate();
            wbxl_id = wbxl_id_cursor;
        }
        rs.close();

        long wbtl_id = 0;
        pstmtSelect_wbt_term_in_lang.setString(1, wbt_type.get(type));
        pstmtSelect_wbt_term_in_lang.setString(2, LANG);
        pstmtSelect_wbt_term_in_lang.setString(3, texte);
        rs = pstmtSelect_wbt_term_in_lang.executeQuery();
        if (rs.next()) {
            wbtl_id = rs.getLong(1);
        }
        else {
            wbtl_id_cursor++;
            pstmtInsert_wbt_term_in_lang.setLong(1, wbtl_id_cursor);
            pstmtInsert_wbt_term_in_lang.setString(2, wbt_type.get(type));
            pstmtInsert_wbt_term_in_lang.setLong(3, wbxl_id);
            pstmtInsert_wbt_term_in_lang.executeUpdate();
            wbtl_id = wbtl_id_cursor;
        }
        rs.close();


        //logger.info("insert_wbt_table : "+texte + " "+type+" wbxId:"+wbxId+" wbxlId:"+wbxlId+" wbtlId:"+wbtlId+" wbt_type:"+wbt_type.get(type)+" lastQNumber:"+lastQNumber);
        //Si le texte est un label, il peut y avoir une exception si ce label est déjà présent (pas de doublon autorisé)
        pstmtInsert_wbt_item_terms.setInt(1, lastQNumber);
        pstmtInsert_wbt_item_terms.setLong(2, wbtl_id);
        executeUpdate(pstmtInsert_wbt_item_terms);
    }

    private void executeUpdate(final PreparedStatement pstmt) throws SQLException {
        // Here you have a chance to log the executed statement.

        //pstmt.executeUpdate();

        //Version avec executeBatch, sinon il faut utiliser la ligne ci-dessus à la place
        pstmt.addBatch();

    }
}

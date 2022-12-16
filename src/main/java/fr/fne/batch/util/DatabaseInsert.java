package fr.fne.batch.util;

import fr.fne.batch.services.ChargementParSQL;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Très largement inspiré de ce dépôt : https://github.com/jze/wikibase-insert
 * Et mis à jour en suivant les requêtes SQL de ce dépôt : https://github.com/UB-Mannheim/RaiseWikibase/blob/main/RaiseWikibase/dbconnection.py
 */
public class DatabaseInsert {

    private final Logger logger = LoggerFactory.getLogger(ChargementParSQL.class);
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

    //ACT
    private PreparedStatement pstmtInsert_wbt_text;
    private PreparedStatement pstmtInsert_wbt_text_in_lang;
    private PreparedStatement pstmtInsert_wbt_term_in_lang;
    private PreparedStatement pstmtInsert_wbt_item_terms;

    private long wbxId = 0;
    private long wbxlId = 0;
    private long wbtlId = 0;
    private long wbitId = 0;
    private Map<String, String> wbt_type = new HashMap<>();

    private int lastQNumber = 0;
    private long textId = 0;
    private long pageId = 0;
    private long commentId = 0;
    private long contentId = 0;
    private int contentModelItem;
    private long recentChangeId = 0;

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
        pstmtInsert_wbt_text.close();
        pstmtInsert_wbt_text_in_lang.close();
        pstmtInsert_wbt_term_in_lang.close();
        pstmtInsert_wbt_item_terms.close();

        connection.close();
    }

    private void work(InputStream stream) throws SQLException, IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        String line = in.readLine();
        while (line != null) {
            createItem(line);
            line = in.readLine();
        }

        in.close();
    }

    public void startTransaction() throws SQLException {
        connection.setAutoCommit(false);
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
        pstmtInsert_wbt_text.executeBatch();
        pstmtInsert_wbt_text_in_lang.executeBatch();
        pstmtInsert_wbt_term_in_lang.executeBatch();
        pstmtInsert_wbt_item_terms.executeBatch();
        //A enlever si pas executeBatch()

        connection.commit();
    }

    private void prepareDatabaseConnection() throws SQLException {

        pstmtInsertText = connection.prepareStatement("INSERT INTO text VALUES(?,?,'utf-8')");
        pstmtInsertPage = connection.prepareStatement("INSERT INTO page VALUES(?,120,?,'',0,0,rand(1),?,?,?,?,'wikibase-item',NULL)");
        pstmtInsertComment = connection.prepareStatement("INSERT INTO comment VALUES(?,?,?,NULL)");
        pstmtInsertContent = connection.prepareStatement("INSERT INTO content VALUES( ? ,?,?, ?, ?)");

        pstmtInsertRevisionComment = connection.prepareStatement("INSERT INTO revision_comment_temp VALUES (?,?)");
        pstmtInsertRevisionActor = connection.prepareStatement("INSERT INTO revision_actor_temp VALUES( ?, ?, ?,  ?)");
        //ACT
        pstmtInsertRevision = connection.prepareStatement("INSERT INTO revision VALUES(?,?,0,0,?,0,0,?,0,?)");

        pstmtInsertSlots = connection.prepareStatement("INSERT INTO slots VALUES( ?, 1, ?, ?)");
        pstmtUpdateWbIdCounters = connection.prepareStatement("UPDATE wb_id_counters SET id_value=? WHERE id_type='wikibase-item'");
        pstmtSelectLastItemId = connection.prepareStatement("SELECT id_value  AS next_id from wb_id_counters where id_type = 'wikibase-item'");
        pstmtSelectItem = connection.prepareStatement("SELECT * FROM page WHERE page_namespace=120 AND page_title=?");

        //ACT
        pstmtInsertRecentChanges = connection.prepareStatement("INSERT INTO recentchanges VALUES (?,?,?,'120',?,?,0,0,0,?,?,0,1,'mw.new',2,'172.18.0.1',0,?,0,0,NULL,'','')");
        pstmtInsert_wbt_text  = connection.prepareStatement("INSERT INTO wbt_text VALUES(?,?)");
        pstmtInsert_wbt_text_in_lang = connection.prepareStatement("INSERT INTO wbt_text_in_lang VALUES(?,?,?)");
        pstmtInsert_wbt_term_in_lang = connection.prepareStatement("INSERT INTO wbt_term_in_lang VALUES(?,?,?)");
        pstmtInsert_wbt_item_terms = connection.prepareStatement("INSERT IGNORE INTO wbt_item_terms VALUES(?,?,?)");


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

        rs = stmt.executeQuery("SELECT max(wbx_id) FROM wbt_text");
        if (rs.next()) {
            wbxId = rs.getLong(1);
        }
        rs.close();

        rs = stmt.executeQuery("SELECT max(wbxl_id) FROM wbt_text_in_lang");
        if (rs.next()) {
            wbxlId = rs.getLong(1);
        }
        rs.close();

        rs = stmt.executeQuery("SELECT max(wbtl_id) FROM wbt_term_in_lang");
        if (rs.next()) {
            wbtlId = rs.getLong(1);
        }
        rs.close();

        rs = stmt.executeQuery("SELECT max(wbit_id) FROM wbt_item_terms");
        if (rs.next()) {
            wbitId = rs.getLong(1);
        }
        rs.close();

        //ACT : contient label, description  et alias
        //Ajout de ces 2 lignes car pas d'Item/Q avec description et/ou alias créé au départ
        connection.createStatement().execute("INSERT INTO wbt_type (wby_name) VALUES('description')");
        connection.createStatement().execute("INSERT INTO wbt_type (wby_name) VALUES('alias')");

        rs = stmt.executeQuery("SELECT wby_name, wby_id FROM wbt_type");
        while (rs.next()){
            wbt_type.put(rs.getString(1),rs.getString(2));
        }
        rs.close();

        rs = stmt.executeQuery("SELECT model_id FROM content_models WHERE model_name='wikibase-item'");
        if( rs.next()) {
            contentModelItem = rs.getInt(1);
        } else {
            rs.close();
            connection.createStatement().execute("INSERT INTO content_models (model_name ) VALUES('wikibase-item')");
            rs = connection.createStatement().executeQuery("SELECT model_id FROM content_models WHERE model_name='wikibase-item'");
            rs.next();
            contentModelItem = rs.getInt(1);
            logger.debug("Created content model for wikibase-item with id "+contentModelItem );
        }
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


    public String createItem(String jsonString) throws SQLException {

        //logger.info("JSON : "+jsonString);
        final JSONObject json = new JSONObject(jsonString);

        //Moins une heure : pour que le wdqs-updater ne traite pas ces insertions
        final String timestamp = LocalDateTime.now().minusHours(1).format(DateTimeFormatter.ISO_DATE_TIME).replaceAll("[T:-]", "").substring(0, 14);

        lastQNumber++;

        final String itemId = "Q" + lastQNumber;

        // Does the specified item already have an ID?
        if (!json.has("id")) {
            json.put("id", itemId);

            // All statements also need this ID
            if( json.has("claims")) {
                final JSONObject claims = json.getJSONObject("claims");
                for (String claim : claims.keySet()) {
                    final JSONArray list = claims.getJSONArray(claim);
                    for (int i = 0; i < list.length(); i++) {
                        list.getJSONObject(i).put("id", itemId + "$" + UUID.randomUUID().toString());
                    }
                }
            }
        }

        String label = json.getJSONObject("labels").getJSONObject(LANG).optString("value");
        if (label.length()>254)
            label = label.substring(0,255);

        String description = null;
        if (json.optJSONObject("descriptions")!=null){
            description = json.optJSONObject("descriptions").optJSONObject(LANG).optString("value");
            if (description!=null && description.length()>254)
                description = description.substring(0,255);
        }

        JSONArray aliases = null;
        if (json.optJSONObject("aliases")!=null){
            aliases = json.optJSONObject("aliases").optJSONArray(LANG);
        }

        try {
            final String data = json.toString();

            //ACT ajout des tables wbt_*  au début car un doublon peut provoquer une exception :
            insert_wbt_table(label,"label");

            if (description!=null) {
                insert_wbt_table(description, "description");
            }

            if (aliases!=null) {
                for (int i = 0; i < aliases.length(); i++) {
                    String alias = aliases.getJSONObject(i).optString("value");
                    if (alias.length() > 254)
                        alias = alias.substring(0, 255);

                    insert_wbt_table(alias, "alias");
                }
            }


            textId++;
            pstmtInsertText.setLong(1, textId);
            pstmtInsertText.setString(2, data);
            executeUpdate(pstmtInsertText);

            pstmtInsertPage.setString(2, itemId);
            pstmtInsertPage.setString(3, timestamp);
            pstmtInsertPage.setString(4, timestamp);
            pstmtInsertPage.setLong(5, textId);
            pstmtInsertPage.setInt(6, data.length());

            pageId++;
            pstmtInsertPage.setLong(1, pageId);
            executeUpdate(pstmtInsertPage);

            pstmtInsertRevision.setLong(1, pageId);
            pstmtInsertRevision.setLong(2, pageId);
            pstmtInsertRevision.setString(3, timestamp);
            pstmtInsertRevision.setInt(4, data.length());
            //pstmtInsertRevision.setLong(4, textId);
            pstmtInsertRevision.setString(5, sha1base36(data));
            executeUpdate(pstmtInsertRevision);

            final String comment = "/* wbeditentity-create:2|en */ " + label + ", " + description;

            pstmtInsertComment.setInt(2, comment.hashCode());
            pstmtInsertComment.setString(3, comment);

            commentId++;
            pstmtInsertComment.setLong(1, commentId);
            executeUpdate(pstmtInsertComment);

            pstmtInsertRevisionComment.setLong(1, textId);
            pstmtInsertRevisionComment.setLong(2, commentId);
            executeUpdate(pstmtInsertRevisionComment);

            pstmtInsertRevisionActor.setLong(1, textId);
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
            pstmtInsertRecentChanges.setLong(7, pageId);

            pstmtInsertRecentChanges.setInt(8, data.length());
            executeUpdate(pstmtInsertRecentChanges);

            pstmtUpdateWbIdCounters.setInt(1, lastQNumber);
            executeUpdate(pstmtUpdateWbIdCounters);

            //logger.info("Nouveau Q:"+lastQNumber);
        }
        catch (Exception e){
            logger.error("Erreur titre déjà présent ? : "+label+" exception:"+e.getMessage());
        }
        return itemId;
    }

    private void insert_wbt_table(String texte, String type) throws Exception{

        //logger.info("insert_wbt_table : "+texte + " "+type+" wbxId:"+wbxId+" wbxlId:"+wbxlId+" wbtlId:"+wbtlId+" wbt_type:"+wbt_type.get(type)+" lastQNumber:"+lastQNumber);
        //Si le texte est un label, il peut y avoir une exception si ce label est déjà présent (pas de doublon autorisé)
        wbxId++;
        pstmtInsert_wbt_text.setLong(1, wbxId);
        pstmtInsert_wbt_text.setString(2, texte);
        executeUpdate(pstmtInsert_wbt_text);

        wbxlId++;
        pstmtInsert_wbt_text_in_lang.setLong(1, wbxlId);
        pstmtInsert_wbt_text_in_lang.setString(2, LANG);
        pstmtInsert_wbt_text_in_lang.setLong(3, wbxId);
        executeUpdate(pstmtInsert_wbt_text_in_lang);

        wbtlId++;
        pstmtInsert_wbt_term_in_lang.setLong(1, wbtlId);
        pstmtInsert_wbt_term_in_lang.setString(2, wbt_type.get(type));
        pstmtInsert_wbt_term_in_lang.setLong(3, wbxlId);
        executeUpdate(pstmtInsert_wbt_term_in_lang);

        wbitId++;
        pstmtInsert_wbt_item_terms.setLong(1, wbitId);
        pstmtInsert_wbt_item_terms.setInt(2, lastQNumber);
        pstmtInsert_wbt_item_terms.setLong(3, wbtlId);
        executeUpdate(pstmtInsert_wbt_item_terms);
    }

    private void executeUpdate(final PreparedStatement pstmt) throws SQLException {
        // Here you have a chance to log the executed statement.

        //pstmt.executeUpdate();

        //Version avec executeBatch, sinon il faut utiliser la ligne ci-dessus à la place
        pstmt.addBatch();

    }
}

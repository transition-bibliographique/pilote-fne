package fr.fne.batch.writer;

import fr.fne.batch.util.DatabaseInsert;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.wikidata.wdtk.datamodel.helpers.JsonSerializer;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ItemDocumentWriter implements ItemWriter<List<ItemDocument>> {

    private final Connection connection;

    public ItemDocumentWriter(Connection connection) throws SQLException, IOException {
        this.connection = connection;
    }

    @Override
    public void write (Chunk<? extends List<ItemDocument>> chunk) throws Exception {
        DatabaseInsert di = new DatabaseInsert(connection);
        di.startTransaction();

        for (List<ItemDocument> itemDocumentList : chunk) {
            for(ItemDocument itemDocument : itemDocumentList){
                di.createItem(JsonSerializer.getJsonString(itemDocument));
            }
            di.commit();
        }
        di.destroy();
    }
}

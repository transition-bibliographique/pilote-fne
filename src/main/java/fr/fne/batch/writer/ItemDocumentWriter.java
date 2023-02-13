package fr.fne.batch.writer;

import fr.fne.batch.conf.BatchConfiguration;
import fr.fne.batch.util.DatabaseInsert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.wikidata.wdtk.datamodel.helpers.JsonSerializer;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.time.temporal.ChronoUnit;

public class ItemDocumentWriter implements ItemWriter<List<ItemDocument>> {
    private final Logger logger = LoggerFactory.getLogger(ItemDocumentWriter.class);

    private final DatabaseInsert di;

    private ExecutionContext executionContext;

    private Date start;

    public ItemDocumentWriter(DatabaseInsert di) throws SQLException, IOException {
        this.di = di;
    }

    @Override
    public void write (Chunk<? extends List<ItemDocument>> chunk) throws Exception {
        int nbItem = 0;
        try {
            for (List<ItemDocument> itemDocumentList : chunk) {
                for (ItemDocument itemDocument : itemDocumentList) {
                    di.createItem(JsonSerializer.getJsonString(itemDocument));
                    nbItem++;
                }
                di.commit();
            }
            this.executionContext.putInt( "nbItem", this.executionContext.getInt( "nbItem", 0 ) + nbItem );
        }
        catch (Exception e){
            logger.error("Erreur : "+e.getMessage());
        }
        //di.destroy();
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution)
    {
        this.executionContext = stepExecution.getExecutionContext();
        this.start = new Date();
    }

    @AfterStep
    public void afterStep(StepExecution stepExecution)
    {
        Date end = new Date();
        long diff = end.getTime() - start.getTime();
        diff = diff / 1000; //en secondes

        int nbItem = this.executionContext.getInt( "nbItem", 0 );

        logger.info("Created "+this.executionContext.getInt( "nbItem", 0 )+" items in " + diff +" s.");
        logger.info("Speed is "+ (nbItem / diff) + " items/second.");
    }
}

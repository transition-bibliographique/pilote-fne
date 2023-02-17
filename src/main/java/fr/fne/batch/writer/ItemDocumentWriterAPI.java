package fr.fne.batch.writer;

import fr.fne.batch.util.ApiWB;
import org.json.JSONObject;
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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ItemDocumentWriterAPI implements ItemWriter<List<ItemDocument>> {
    private final Logger logger = LoggerFactory.getLogger(ItemDocumentWriterAPI.class);

    private final String csrftoken;

    private final ApiWB apiWB;

    private ExecutionContext executionContext;

    private Date start;

    public ItemDocumentWriterAPI(ApiWB apiWB) throws Exception {
        this.apiWB = apiWB;
        this.csrftoken = apiWB.connexionWB();
        //logger.info("Connexion ApiWB dans IDWriterAPI :"+csrftoken);
    }

    @Override
    public void write (Chunk<? extends List<ItemDocument>> chunk) {
        int nbItem = 0;

        Map<String, String> params = new LinkedHashMap<>();
        params.put("action", "wbeditentity");
        params.put("new", "item");
        params.put("token", csrftoken);
        params.put("format", "json");

        try {
            for (List<ItemDocument> itemDocumentList : chunk) {
                for (ItemDocument itemDocument : itemDocumentList) {
                    params.put("data", JsonSerializer.getJsonString(itemDocument));
                    JSONObject json = apiWB.postJson(params);
                    logger.info("json : "+json.toString());
                    nbItem++;
                }
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

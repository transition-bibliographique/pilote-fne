package fr.fne.batch.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.fne.batch.model.dto.Personne;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class ItemDocumentWriterLink implements ItemWriter<Personne> {
    private final Logger logger = LoggerFactory.getLogger(ItemDocumentWriterLink.class);

    private ExecutionContext executionContext;

    private Date start;

    JdbcTemplate jdbcTemplate;

    ObjectMapper objectMapper = new ObjectMapper();

    public ItemDocumentWriterLink(JdbcTemplate jdbcTemplate) throws SQLException, IOException {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void write (Chunk<? extends Personne> chunk) throws Exception {
        int nbItem = 0;
        try {
            for (Personne personne : chunk) {

                logger.info("Personne en cours : " + personne.getNom());

                jdbcTemplate.execute("select * from ag_catalog.cypher('personnes', $$\n" +
                                         " match (a:Person), (b:Person)" +
                                         " where a.ppn = '"+personne.getPpn()+"' and b.ppn = '"+personne.getPointAcces()+"'" +
                                         " create (a)-[e:LIE_A { type:'pointAcces' }]->(b)" +
                                         " return e \n" +
                                         " $$) as (relationship ag_catalog.agtype)");

                nbItem++;
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

        if (diff == 0){
            diff = 1;
        }

        int nbItem = this.executionContext.getInt( "nbItem", 0 );

        logger.info("Linked "+this.executionContext.getInt( "nbItem", 0 )+" items in " + diff +" s.");
        logger.info("Speed is "+ (nbItem / diff) + " items/second.");
    }
}

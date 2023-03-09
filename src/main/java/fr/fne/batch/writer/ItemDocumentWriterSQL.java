package fr.fne.batch.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.fne.batch.model.dto.Personne;
import fr.fne.batch.service.PersonneService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class ItemDocumentWriterSQL implements ItemWriter<List<Personne>> {
    private final Logger logger = LoggerFactory.getLogger(ItemDocumentWriterSQL.class);

    private ExecutionContext executionContext;

    private Date start;

    JdbcTemplate jdbcTemplate;

    ObjectMapper objectMapper = new ObjectMapper();

    public ItemDocumentWriterSQL(JdbcTemplate jdbcTemplate) throws SQLException, IOException {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void write (Chunk<? extends List<Personne>> chunk) throws Exception {
        int nbItem = 0;
        try {
            for (List<Personne> personneList : chunk) {
                for (Personne personne : personneList) {

                    String personneInsert = personne.getContenu().replaceAll("'","\\\\'").replaceAll("\",\"","',").replaceAll(":\"",":'").replaceAll("\"}","'}").replaceAll("\"","") ;
                    logger.info(personneInsert);

                    //logger.info(objectMapper.writeValueAsString(personne));

                    //personneService.savePersonne(personne.getNom());
                    /*jdbcTemplate.execute("select * from ag_catalog.cypher ('family_tree', $$\n" +
                                    "        create (:Person {" +
                                    "               name:'"+personne.getNom()+"'," +
                                    "               titles:['Test']," +
                                    "               year_born: 1980," +
                                    "               year_died: 2068" +
                                    "        })\n" +
                                    "$$) as (person ag_catalog.agtype)");
                    */

                    jdbcTemplate.execute("select * from ag_catalog.cypher ('family_tree', $$\n" +
                            "        create (:Person "+personneInsert+")\n" +
                            "$$) as (person ag_catalog.agtype)");

                    nbItem++;
                }
                //di.commit();
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

        logger.info("Created "+this.executionContext.getInt( "nbItem", 0 )+" items in " + diff +" s.");
        logger.info("Speed is "+ (nbItem / diff) + " items/second.");
    }
}

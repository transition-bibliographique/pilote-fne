package fr.fne.batch.tasklet;

import fr.fne.batch.util.ApiWB;
import fr.fne.batch.util.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public class FormatTasklet implements Tasklet {

    private final Logger logger = LoggerFactory.getLogger(FormatTasklet.class);

    @Autowired
    private ApiWB apiWB;

    @Autowired
    private Format format;

    @Override
    public RepeatStatus execute (StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        // Connextion à Wikibase et récupération du csrftoken
        String csrftoken = apiWB.connexionWB();
        logger.info("The csrftoken is : " + csrftoken);

        // Création du format
        format.createWithFile(csrftoken);
        // Map des propriétés
        Map<String, String> props = format.get();

        logger.info("Nombre de propriétés chargées : " + props.size());

        return RepeatStatus.FINISHED;
    }
}
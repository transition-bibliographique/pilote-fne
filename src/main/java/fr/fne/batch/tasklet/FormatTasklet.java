package fr.fne.batch.tasklet;

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
    private Format format;

    @Override
    public RepeatStatus execute (StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        // Création du format
        format.create();

        logger.info("Propriétés créées");

        return RepeatStatus.FINISHED;
    }
}
package fr.fne.batch.conf;

import fr.fne.batch.processor.CollectionItemProcessor;
import fr.fne.batch.tasklet.FormatTasklet;
import fr.fne.batch.util.ApiWB;
import fr.fne.batch.util.Format;
import fr.fne.batch.writer.ItemDocumentWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.IteratorItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@ComponentScan(basePackages = {"fr.fne.batch"})
public class BatchConfiguration {

    private final Logger logger = LoggerFactory.getLogger(BatchConfiguration.class);
    @Value("${abes.dump}")
    private String cheminDump;
    @Value("${mysql.url}")
    private String mysqlUrl;
    @Value("${mysql.login}")
    private String mysqlLogin;
    @Value("${mysql.pwd}")
    private String mysqlPwd;
    @Autowired
    private BatchArguments batchArguments;
    @Autowired
    private ApiWB apiWB;
    @Autowired
    private Format format;
    private  Map<String, String> props;
    @Bean
    public ItemReader<File> reader () throws Exception {

        //Récupération de toutes les propriétés du WB
        this.props = format.get();
        logger.info("Nombre de propriétés chargées : " + props.size());

        //Utilisation d'un dump des notices (5000 notices par fichier):
        //Le dump complet est disponible ici (Abes, sur KAT): /applis/portail/SitemapNoticesSudoc/noticesautorites/dump/
        //Pour tester : utiliser l'échantillon qui se trouve dans resources/dump/
        List<File> files = Files.walk(Paths.get(cheminDump))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());

        return new IteratorItemReader<>(files);
    }

    @Bean
    public CollectionItemProcessor processor() {
        return new CollectionItemProcessor(this.props);
    }

    @Bean
    public ItemWriter writer() throws SQLException, IOException {
        Connection connection = DriverManager.getConnection(mysqlUrl, mysqlLogin, mysqlPwd);
        return new ItemDocumentWriter(connection);
    }

    @Bean
    public FormatTasklet formatTasklet() {
        return new FormatTasklet();
    }

    /**
     * Etape de création du format
     */
    @Bean
    public Step stepFormat (JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("stepFormat", jobRepository)
                .tasklet(this.formatTasklet(), transactionManager)
                .build();
    }

    /**
     * Etape de création des données
     */
    @Bean
    public Step stepData (JobRepository jobRepository, PlatformTransactionManager transactionManager) throws Exception {
        return new StepBuilder("stepData", jobRepository)
                .<File, List<ItemDocument>> chunk(10, transactionManager)
                .reader(this.reader())
                .processor(this.processor())
                .writer(this.writer())
                .build();
    }

    @Bean
    public Job importUserJob(JobRepository jobRepository, Step stepData, Step stepFormat) {

        // Seulement le format
        if(batchArguments.isFormat() && !batchArguments.isSql()){
            return new JobBuilder("insertWikibase", jobRepository)
                    .incrementer(new RunIdIncrementer())
                    .flow(stepFormat)
                    .end()
                    .build();
        }
        // Le format + les données
        if(batchArguments.isFormat() && batchArguments.isSql()) {
            return new JobBuilder("insertWikibase", jobRepository)
                    .incrementer(new RunIdIncrementer())
                    .flow(stepFormat)
                    .next(stepData)
                    .end()
                    .build();
        }
        // Seulement les données
        return new JobBuilder("insertWikibase", jobRepository)
                .incrementer(new RunIdIncrementer())
                .flow(stepData)
                .end()
                .build();
    }
}

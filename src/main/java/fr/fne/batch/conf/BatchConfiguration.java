package fr.fne.batch.conf;

import fr.fne.batch.processor.CollectionItemProcessor;
import fr.fne.batch.tasklet.FormatTasklet;
import fr.fne.batch.util.ApiWB;
import fr.fne.batch.util.DatabaseInsert;
import fr.fne.batch.util.Format;
import fr.fne.batch.writer.ItemDocumentWriterAPI;
import fr.fne.batch.writer.ItemDocumentWriterSQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
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
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
    @Value("${chunk.size:10}")
    private int chunkSize;
    @Autowired
    private BatchArguments batchArguments;
    @Autowired
    private ApiWB apiWB;
    @Autowired
    private Format format;
    private  Map<String, String> props;
    @Value("${wikibase.url}")
    private String urlWikiBase;

    @Autowired
    JobRepository jobRepository;



    @Bean
    public ItemReader<File> reader () throws Exception {

        int responseCode = -1;

        // Attente que urlWikiBase soit disponible
        while (responseCode!=200){
            TimeUnit.SECONDS.sleep(5);
            try {
                URL url = new URL(urlWikiBase);
                HttpURLConnection huc = (HttpURLConnection) url.openConnection();
                responseCode = huc.getResponseCode();
            }
            catch (Exception e){
                logger.info("Tentative de connexion à "+urlWikiBase+" erreur : "+e.getMessage());
            }

            logger.info("responseCode : "+ responseCode);
        }

        props = format.get();
        //Si pas de propriétés, alors création (pr éviter d'appeler 2x fois le BatchApplication : creationProprietes puis chargement)
        if (props.size()==0){
            // Connextion à Wikibase et récupération du csrftoken
            String csrftoken = apiWB.connexionWB();
            logger.info("The csrftoken is : " + csrftoken);
            // Création du format
            format.createWithFile(csrftoken);
            // Map des propriétés
            props = format.get();
        }
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
    public ItemWriter writerSQL() throws SQLException, IOException {
        Connection connection = DriverManager.getConnection(mysqlUrl, mysqlLogin, mysqlPwd);
        connection.setAutoCommit(false);
        DatabaseInsert di = new DatabaseInsert(connection);
        return new ItemDocumentWriterSQL(di);
    }

    @Bean
    public ItemWriter writerAPI() throws Exception {
        return new ItemDocumentWriterAPI(apiWB);
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
     * Etape de création des données par SQL
     * (1 chunk équivaux à 5000 notices)
     */
    @Bean
    public Step stepDataSQL (JobRepository jobRepository, PlatformTransactionManager transactionManager) throws Exception {
        return new StepBuilder("stepDataSQL", jobRepository)
                .<File, List<ItemDocument>> chunk(chunkSize, transactionManager)
                .reader(this.reader())
                .processor(this.processor())
                .writer(this.writerSQL())
                .build();
    }

    /**
     * Etape de création des données par API
     * (1 chunk équivaux à 5000 notices)
     */
    @Bean
    public Step stepDataAPI (JobRepository jobRepository, PlatformTransactionManager transactionManager) throws Exception {
        return new StepBuilder("stepDataAPI", jobRepository)
                .<File, List<ItemDocument>> chunk(chunkSize, transactionManager)
                .reader(this.reader())
                .processor(this.processor())
                .writer(this.writerAPI())
                .taskExecutor(taskExecutorAsync())
                //.taskExecutor(taskExecutorMultiThread())
                .build();
    }

    @Bean
    public SimpleAsyncTaskExecutor taskExecutorAsync(){
        SimpleAsyncTaskExecutor simpleAsyncTaskExecutor = new SimpleAsyncTaskExecutor();
        //simpleAsyncTaskExecutor.setConcurrencyLimit(10); //Par défaut, illimité
        simpleAsyncTaskExecutor.setThreadNamePrefix("AsyncThread");
        return simpleAsyncTaskExecutor;
    }

    @Bean
    public ThreadPoolTaskExecutor taskExecutorMultiThread(){
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(10); //Nombre de threads
        threadPoolTaskExecutor.setQueueCapacity(10);
        threadPoolTaskExecutor.setThreadNamePrefix("MultiThread");
        return threadPoolTaskExecutor;
    }

    @Bean
    public Job importUserJob(JobRepository jobRepository, Step stepDataSQL, Step stepDataAPI, Step stepFormat) {

        // Seulement le format
        if (batchArguments.isFormat()){
            return new JobBuilder("insertWikibase", jobRepository)
                    .incrementer(new RunIdIncrementer())
                    .flow(stepFormat)
                    .end()
                    .build();
        }
        else if (batchArguments.isSql()){
            return new JobBuilder("insertWikibase", jobRepository)
                    .incrementer(new RunIdIncrementer())
                    .flow(stepDataSQL)
                    .end()
                    .build();
        }
        else if (batchArguments.isApi()){
            return new JobBuilder("insertWikibase", jobRepository)
                    .incrementer(new RunIdIncrementer())
                    .flow(stepDataAPI)
                    .end()
                    .build();
        }
        return null;
    }
}

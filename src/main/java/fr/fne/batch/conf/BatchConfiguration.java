package fr.fne.batch.conf;

import fr.fne.batch.model.dto.Personne;
import fr.fne.batch.processor.AGEItemProcessor;
import fr.fne.batch.processor.CollectionItemProcessor;
import fr.fne.batch.service.PersonneService;
import fr.fne.batch.tasklet.FormatTasklet;
import fr.fne.batch.util.Format;
import fr.fne.batch.writer.ItemDocumentWriterLink;
import fr.fne.batch.writer.ItemDocumentWriterSQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.support.IteratorItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;


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
    @Value("${chunk.size:10}")
    private int chunkSize;
    @Autowired
    private BatchArguments batchArguments;
    @Autowired
    private Format format;
    @Autowired
    JobRepository jobRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Bean
    public ItemReader<File> reader () throws Exception {
        List<File> files = Files.walk(Paths.get(cheminDump))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());

        return new IteratorItemReader<>(files);
    }

    /*@Bean
    public JdbcCursorItemReader<Personne> readerAGE() {
        return new JdbcCursorItemReaderBuilder<Personne>()
                .name("creditReader")
                .sql("select * from ag_catalog.cypher('personnes', $$\n" +
                        " MATCH(v)" +
                        " return v \n" +
                        "$$) as (v ag_catalog.agtype)")
                .rowMapper(new PersonneRowMapper())
                .build();
    }*/

    @Bean
    public ItemReader<Personne> readerAGE () throws Exception {
        List<Personne> rows = jdbcTemplate.query("select * from ag_catalog.cypher('personnes', $$\n" +
                " MATCH(v)" +
                " return v \n" +
                "$$) as (v ag_catalog.agtype)",new PersonneRowMapper());

        return new IteratorItemReader<>(rows);
    }

    @Bean
    public CollectionItemProcessor processor() {
        return new CollectionItemProcessor();
    }

    @Bean
    public AGEItemProcessor processorAGE() {
        return new AGEItemProcessor();
    }

    @Bean
    public ItemWriter writerSQL() throws SQLException, IOException {
        //Connection connection = DriverManager.getConnection(mysqlUrl, mysqlLogin, mysqlPwd);
        //connection.setAutoCommit(false);
        //DatabaseInsert di = new DatabaseInsert(connection);
        return new ItemDocumentWriterSQL(jdbcTemplate);
    }

    @Bean
    public ItemWriter writerLink() throws SQLException, IOException {
        return new ItemDocumentWriterLink(jdbcTemplate);
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
                .<File, List<Personne>> chunk(chunkSize, transactionManager)
                .reader(this.reader())
                .processor(this.processor())
                .writer(this.writerSQL())
                //.taskExecutor(taskExecutorAsync())
                //.taskExecutor(taskExecutorMultiThread())
                .build();
    }

    @Bean
    public Step stepLink (JobRepository jobRepository, PlatformTransactionManager transactionManager) throws Exception {
        return new StepBuilder("stepLink", jobRepository)
                .<Personne, Personne> chunk(chunkSize, transactionManager)
                .reader(this.readerAGE())
                .processor(this.processorAGE())
                .writer(this.writerLink())
                //.taskExecutor(taskExecutorAsync())
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
    public Job importUserJob(JobRepository jobRepository, Step stepDataSQL, Step stepFormat, Step stepLink) {

        // Seulement le format
        if (batchArguments.isFormat()){
            return new JobBuilder("insertAGE", jobRepository)
                    .incrementer(new RunIdIncrementer())
                    .flow(stepFormat)
                    .end()
                    .build();
        }
        else if (batchArguments.isSql()){
            return new JobBuilder("insertAGE", jobRepository)
                    .incrementer(new RunIdIncrementer())
                    .flow(stepDataSQL)
                    .end()
                    .build();
        }
        else if (batchArguments.isLink()){
            return new JobBuilder("linkAGE", jobRepository)
                    .incrementer(new RunIdIncrementer())
                    .flow(stepLink)
                    .end()
                    .build();
        }
        return null;
    }
}

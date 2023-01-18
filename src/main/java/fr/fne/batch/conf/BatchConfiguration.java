package fr.fne.batch.conf;

import fr.fne.batch.processor.CollectionItemProcessor;
import fr.fne.batch.writer.ItemDocumentWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.IteratorItemReader;
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
import java.util.stream.Collectors;

@Configuration
@ComponentScan(basePackages = {"fr.fne.batch"})
public class BatchConfiguration {

    @Value("${abes.dump}")
    private String cheminDump;

    @Value("${mysql.url}")
    private String mysqlUrl;
    @Value("${mysql.login}")
    private String mysqlLogin;
    @Value("${mysql.pwd}")
    private String mysqlPwd;

    @Bean
    public ItemReader<File> reader () throws IOException {

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
        return new CollectionItemProcessor();
    }

    @Bean
    public ItemWriter writer() throws SQLException, IOException {
        Connection connection = DriverManager.getConnection(mysqlUrl, mysqlLogin, mysqlPwd);
        return new ItemDocumentWriter(connection);
    }

    @Bean
    public Step step1(JobRepository jobRepository, PlatformTransactionManager transactionManager) throws SQLException, IOException {
        return new StepBuilder("step1", jobRepository)
                .<File, List<ItemDocument>> chunk(10, transactionManager)
                .reader(this.reader())
                .processor(this.processor())
                .writer(this.writer())
                .build();
    }

    @Bean
    public Job importUserJob(JobRepository jobRepository, Step step1) throws SQLException, IOException {
        return new JobBuilder("insertWikibase", jobRepository)
                .incrementer(new RunIdIncrementer())
                .flow(step1)
                .end()
                .build();
    }

}

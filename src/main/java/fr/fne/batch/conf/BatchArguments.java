package fr.fne.batch.conf;

import org.apache.commons.cli.*;
import org.springframework.batch.core.BatchStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class BatchArguments {

    @Autowired
    private ApplicationContext appContext;

    private HelpFormatter formatter = new HelpFormatter();

    private CommandLine cmd;

    private Options options = new Options();

    private boolean isSql;

    private boolean isFormat;

    /**
     * This constructor will load program's arguments at launch.
     *
     * @param args
     */
    public BatchArguments (ApplicationArguments args) {
        this.loadArgs(args.getSourceArgs());
    }

    private void loadArgs (String... args) {
        // Setup awaited options.
        this.defineOptions();

        CommandLineParser parser = new DefaultParser();

        try {
            // Initialise the representation of the command line.
            this.cmd = parser.parse(this.options, args);

            this.isFormat = this.cmd.hasOption("format");
            this.isSql = this.cmd.hasOption("sql");

            if(!isFormat() && !isSql()){
                this.formatter.printHelp("Arguments absent, utiliser -f pour la creation du format ou -s pour l'insertion sql", this.options);
                // Stop the batch.
                this.initiateShutdown();
            }

        } catch (ParseException e) {
            this.formatter.printHelp("Arguments non valides, utiliser -f pour la creation du format ou -s pour l'insertion sql", this.options);

            // Stop the batch.
            this.initiateShutdown();
        }
    }

    /**
     * Define awaited options ans load them in this singleton's inner data.
     */
    private void defineOptions() {

        Option nameOption = new Option("s", "sql", false, "Insertion des données en sql");
        nameOption.setRequired(false);
        this.options.addOption(nameOption);

        Option filepathOption = new Option("f", "format", false, "Création du format par api");
        filepathOption.setRequired(false);
        this.options.addOption(filepathOption);
    }

    public boolean isSql () {
        return isSql;
    }

    public boolean isFormat () {
        return isFormat;
    }

    /**
     * End the batch.
     */
    private void initiateShutdown() {
        SpringApplication.exit(this.appContext, () -> BatchStatus.FAILED.ordinal());
    }
}

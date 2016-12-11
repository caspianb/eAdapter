package spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import builders.LfpBuilder;
import builders.OpticonBuilder;
import builders.TextDelimitedBuilder;
import csvparser.CSVParser;

@Configuration
public class SpringConfiguration {

    @Bean
    public CSVParser csvParser() {
        return new CSVParser();
    }

    @Bean
    public LfpBuilder lfpBuilder() {
        return new LfpBuilder();
    }

    @Bean
    public OpticonBuilder optBuilder() {
        return new OpticonBuilder();
    }

    @Bean
    TextDelimitedBuilder txtBuilder() {
        return new TextDelimitedBuilder();
    }
}

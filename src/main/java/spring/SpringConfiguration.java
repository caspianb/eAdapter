package spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import builders.OpticonBuilder;
import csvparser.CSVParser;

@Configuration
public class SpringConfiguration {

    @Bean
    public CSVParser csvParser() {
        return new CSVParser();
    }

    @Bean
    public OpticonBuilder optBuilder() {
        return new OpticonBuilder();
    }

}

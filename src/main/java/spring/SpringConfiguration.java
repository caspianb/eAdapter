package spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import csvparser.CSVParser;

@Configuration
public class SpringConfiguration {

    @Bean
    public CSVParser csvParser() {
        return new CSVParser();
    }

}

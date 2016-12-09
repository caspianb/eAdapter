package spring;

import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import csvparser.CSVParser;

@Configuration
public class ServletConfig {

    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer() {
        return (container -> {
            //container.setPort(0);
        });
    }

    @Bean
    public CSVParser csvParser() {
        return new CSVParser();
    }

}

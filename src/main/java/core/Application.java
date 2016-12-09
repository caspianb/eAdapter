package core;

import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication(scanBasePackages = { "core", "spring" })
public class Application {

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(Application.class, args);

        printBeansInContext(ctx);
    }

    protected static void printBeansInContext(ApplicationContext ctx) {
        System.out.println("Let's inspect the beans provided by Spring Boot:");
        Arrays.stream(ctx.getBeanDefinitionNames())
                .sorted()
                .forEach(beanName -> System.out.println(" - " + beanName));
    }
}

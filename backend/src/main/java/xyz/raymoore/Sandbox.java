package xyz.raymoore;

import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class Sandbox {

    static void main(String[] args) {
        SpringApplication script = new SpringApplication(Sandbox.class);
        script.setBannerMode(Banner.Mode.OFF);
        script.setLogStartupInfo(false);
        script.setWebApplicationType(WebApplicationType.NONE);

        script.run(args).close();
    }

    @Bean
    CommandLineRunner execute(Environment environment) {
        return args -> {
            String s = environment.getProperty("spring.application.name");
            System.out.println("[%s] Hello!".formatted(s));
        };
    }
}

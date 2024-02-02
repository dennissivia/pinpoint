package dev.smallbit.pinpoint;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

@SpringBootApplication
public class PinpointApplication {

  public static void main(String[] args) {
    SpringApplication.run(PinpointApplication.class, args);
  }

  @Bean
  public CommandLineRunner run(ApplicationContext appContext) {
    return args -> {
      String[] beans = appContext.getBeanDefinitionNames();
      Arrays.stream(beans).sorted().forEach(System.out::println);
    };
  }
}

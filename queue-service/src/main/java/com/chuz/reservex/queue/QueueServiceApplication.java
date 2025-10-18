package com.chuz.reservex.queue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.chuz.reservex.queue", "com.chuz.reservex.common"})
public class QueueServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(QueueServiceApplication.class, args);
  }

}

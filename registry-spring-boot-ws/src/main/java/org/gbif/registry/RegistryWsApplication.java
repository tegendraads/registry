package org.gbif.registry;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"org.gbif.ws.server.filter", "org.gbif.ws.security", "org.gbif.registry"})
@MapperScan("org.gbif.registry.persistence.mapper")
public class RegistryWsApplication {
  public static void main(String[] args) {
    SpringApplication.run(RegistryWsApplication.class, args);
  }
}

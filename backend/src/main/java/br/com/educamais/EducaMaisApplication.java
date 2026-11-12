package br.com.educamais;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Ponto de entrada da API do Educa Mais. */
@SpringBootApplication
@EnableScheduling
public class EducaMaisApplication {

    public static void main(String[] args) {
        SpringApplication.run(EducaMaisApplication.class, args);
    }
}

package br.com.educamais.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * ObjectMapper dedicado aos adapters (Gemini, auditoria).
 *
 * <p>Isola a aplicacao da estrategia JSON escolhida pelo Spring Boot 4 (Jackson 3) e mantem
 * um contrato estavel de leitura de JSON nos testes, no parsing de LLM e no audit trail.</p>
 */
@Configuration
public class JsonMapperConfig {

    @Bean
    @Primary
    public ObjectMapper applicationObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}

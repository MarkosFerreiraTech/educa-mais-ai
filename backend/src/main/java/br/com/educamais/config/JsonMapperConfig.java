package br.com.educamais.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ObjectMapper dedicado aos adapters (Gemini, auditoria).
 *
 * <p><strong>Nao e {@code @Primary}</strong>: o Boot 4 usa Jackson 3 no MVC, e marcar este bean
 * (Jackson 2) como primario confundia a injecao de {@code tools.jackson.databind.json.JsonMapper}
 * usada pelos {@code HttpMessageConverter}s -- sintomas: corpo de erro serializado como ProblemDetail
 * vazio e requisicoes validas respondendo 500. O bean recebe o nome {@code adapterObjectMapper} e e
 * injetado por tipo explicito nos adapters.</p>
 */
@Configuration
public class JsonMapperConfig {

    public static final String ADAPTER_OBJECT_MAPPER = "adapterObjectMapper";

    @Bean(ADAPTER_OBJECT_MAPPER)
    public ObjectMapper adapterObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}

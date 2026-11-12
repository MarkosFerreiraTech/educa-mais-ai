package br.com.educamais.config;

import br.com.educamais.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Habilita os grupos de propriedades tipadas da aplicacao. */
@Configuration
@EnableConfigurationProperties({
        SecurityProperties.class,
        AgentProperties.class,
        WhatsAppProperties.class,
        JwtProperties.class
})
public class PropertiesConfig {
}

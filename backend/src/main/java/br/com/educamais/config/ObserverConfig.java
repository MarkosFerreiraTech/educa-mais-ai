package br.com.educamais.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;

/**
 * Multicaster de eventos de dominio (Observer).
 *
 * <p>O multicaster e sincrono (o publish ocorre dentro da transacao) e o consumidor real
 * ({@code AttendanceEventConsumer}) usa {@code @TransactionalEventListener(AFTER_COMMIT)} + {@code @Async},
 * garantindo que o agente so rode depois do commit da falta.</p>
 */
@Configuration
public class ObserverConfig {

    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster applicationEventMulticaster() {
        return new SimpleApplicationEventMulticaster();
    }
}

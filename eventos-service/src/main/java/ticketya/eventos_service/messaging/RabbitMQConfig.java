package ticketya.eventos_service.messaging;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String INVALIDACION_CACHE_Q = "invalidacion-cache.q";

    @Bean
    public Queue invalidacionCacheQueue() {
        return QueueBuilder.durable(INVALIDACION_CACHE_Q).build();
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

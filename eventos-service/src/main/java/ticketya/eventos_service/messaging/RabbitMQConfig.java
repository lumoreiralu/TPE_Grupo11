package ticketya.eventos_service.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE = "ticketya.topic";
    public static final String INVALIDACION_CACHE_Q = "invalidacion-cache.q";

    @Bean
    public TopicExchange ticketyaExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue invalidacionCacheQueue() {
        return QueueBuilder.durable(INVALIDACION_CACHE_Q).build();
    }

    @Bean
    public Binding invalidacionCacheCanceladoBinding(Queue invalidacionCacheQueue,
                                                     TopicExchange ticketyaExchange) {
        return BindingBuilder.bind(invalidacionCacheQueue).to(ticketyaExchange).with("evento.cancelado");
    }

    @Bean
    public Binding invalidacionCacheModificadoBinding(Queue invalidacionCacheQueue,
                                                      TopicExchange ticketyaExchange) {
        return BindingBuilder.bind(invalidacionCacheQueue).to(ticketyaExchange).with("evento.modificado");
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}

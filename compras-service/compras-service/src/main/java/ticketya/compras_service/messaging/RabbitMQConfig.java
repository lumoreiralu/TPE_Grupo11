package ticketya.compras_service.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    public static final String EXCHANGE = "ticketya.topic";
    public static final String NOTIFICACIONES_Q = "notificaciones.q";
    public static final String METRICAS_Q = "metricas.q";
    public static final String INVALIDACION_CACHE_Q = "invalidacion-cache.q";

    @Bean
    public TopicExchange ticketyaExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder.durable(NOTIFICACIONES_Q).build();
    }

    @Bean
    public Queue metricasQueue() {
        return QueueBuilder.durable(METRICAS_Q).build();
    }

    @Bean
    public Queue invalidacionCacheQueue() {
        return QueueBuilder.durable(INVALIDACION_CACHE_Q).build();
    }

    @Bean
    public Binding notificacionesBinding(Queue notificacionesQueue, TopicExchange ticketyaExchange) {
        return BindingBuilder.bind(notificacionesQueue).to(ticketyaExchange).with("pago.aprobado");
    }

    @Bean
    public Binding notificacionesDevolucionBinding(Queue notificacionesQueue, TopicExchange ticketyaExchange) {
        return BindingBuilder.bind(notificacionesQueue).to(ticketyaExchange).with("devolucion.aprobada");
    }

    @Bean
    public Binding notificacionesEventoCanceladoBinding(Queue notificacionesQueue, TopicExchange ticketyaExchange) {
        return BindingBuilder.bind(notificacionesQueue).to(ticketyaExchange).with("evento.cancelado");
    }

    @Bean
    public Binding notificacionesEventoModificadoBinding(Queue notificacionesQueue, TopicExchange ticketyaExchange) {
        return BindingBuilder.bind(notificacionesQueue).to(ticketyaExchange).with("evento.modificado");
    }

    @Bean
    public Binding metricasPagoAprobadoBinding(Queue metricasQueue, TopicExchange ticketyaExchange) {
        return BindingBuilder.bind(metricasQueue).to(ticketyaExchange).with("pago.aprobado");
    }

    @Bean
    public Binding metricasPagoCompensadoBinding(Queue metricasQueue, TopicExchange ticketyaExchange) {
        return BindingBuilder.bind(metricasQueue).to(ticketyaExchange).with("pago.compensado");
    }

    @Bean
    public Binding invalidacionCacheBinding(Queue invalidacionCacheQueue, TopicExchange ticketyaExchange) {
        return BindingBuilder.bind(invalidacionCacheQueue).to(ticketyaExchange).with("evento.cancelado");
    }

    @Bean
    public Binding invalidacionCacheModificadoBinding(Queue invalidacionCacheQueue, TopicExchange ticketyaExchange) {
        return BindingBuilder.bind(invalidacionCacheQueue).to(ticketyaExchange).with("evento.modificado");
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        
        var retryInterceptor = RetryInterceptorBuilder.stateless()
                .maxRetries(3)
                .backOffOptions(1000, 1.0, 1000)
                .recoverer((message, cause) -> {
                    log.error("Event processing exhausted retries. Payload: {}", new String(message.getBody()));
                })
                .build();
        factory.setAdviceChain(retryInterceptor);
        return factory;
    }
}

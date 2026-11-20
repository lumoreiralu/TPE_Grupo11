package ticketya.compras_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class QueueConfig {

    @Value("${compras.fila.modo:AUTOMATICO}")
    private String modoPorDefecto;

    @Value("${compras.fila.umbral:50}")
    private int umbralPorDefecto;

    @Value("${compras.fila.pase-ttl-segundos:600}")
    private long paseTtlSegundos;

    public String modoPorDefecto() {
        return modoPorDefecto;
    }

    public int umbralPorDefecto() {
        return umbralPorDefecto;
    }

    public Duration paseTtl() {
        return Duration.ofSeconds(paseTtlSegundos);
    }
}

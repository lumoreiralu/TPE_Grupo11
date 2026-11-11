package ticketya.compras_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ticketya.compras_service.client.EventosClient;
import ticketya.compras_service.client.EventoDTO;
import ticketya.compras_service.client.EventoDTO.PrecioSectorDTO;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ReservaConcurrenteTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Usamos @MockitoBean estándar de Spring Boot 3+ (reemplaza al viejo @MockBean)
    @MockitoBean
    private EventosClient eventosClient;

    @Test
    @DisplayName("Simular 20 compras simultáneas del mismo asiento: solo 1 debe ganar y 19 deben ser rechazadas")
    void testReservaConcurrenteMismoAsiento() throws InterruptedException {
        String eventoIdPrueba = "EVT-" + UUID.randomUUID();
        String sectorIdPrueba = "SEC-PLATEA-A";
        String filaPrueba = "1";
        int asientoPrueba = 25;

        EventoDTO eventoMock = new EventoDTO(eventoIdPrueba, "PUBLICADO", List.of(new PrecioSectorDTO(sectorIdPrueba, new BigDecimal("30000.00"))));
        when(eventosClient.obtenerEvento(anyString())).thenReturn(eventoMock);

        int cantidadHilos = 20;
        ExecutorService executor = Executors.newFixedThreadPool(cantidadHilos);

        CountDownLatch disparo = new CountDownLatch(1);
        CountDownLatch fin = new CountDownLatch(cantidadHilos);

        AtomicInteger reservasExitosas = new AtomicInteger(0);
        AtomicInteger reservasRechazadas = new AtomicInteger(0);

        for (int i = 0; i < cantidadHilos; i++) {
            final String clienteId = "CLI-CONCURRENTE-" + i + "-" + UUID.randomUUID();
            executor.submit(() -> {
                try {
                    disparo.await();
                    Map<String, Object> item = new HashMap<>();
                    item.put("sectorId", sectorIdPrueba);
                    item.put("fila", filaPrueba);
                    item.put("asiento", asientoPrueba);

                    Map<String, Object> requestBody = new HashMap<>();
                    requestBody.put("clienteId", clienteId);
                    requestBody.put("eventoId", eventoIdPrueba);
                    requestBody.put("items", Collections.singletonList(item));

                    String jsonBody = objectMapper.writeValueAsString(requestBody);

                    MvcResult result = mockMvc.perform(
                            post("/api/compras/reservas")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonBody)
                    ).andReturn();

                    int status = result.getResponse().getStatus();
                    if (status == 200 || status == 201) {
                        reservasExitosas.incrementAndGet();
                    } else {
                        reservasRechazadas.incrementAndGet();
                    }
                } catch (Exception e) {
                    reservasRechazadas.incrementAndGet();
                } finally {
                    fin.countDown();
                }
            });
        }

        disparo.countDown();
        fin.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println(">>> RESULTADOS DEL TEST DE CONCURRENCIA <<<");
        System.out.println("Reservas exitosas (ganadoras): " + reservasExitosas.get());
        System.out.println("Reservas rechazadas (anti-doble venta): " + reservasRechazadas.get());

        // Opcional: Validar que exactamente 1 haya ganado
        assertEquals(1, reservasExitosas.get(), "Debería haber exactamente una reserva exitosa");
        assertEquals(cantidadHilos - 1, reservasRechazadas.get(), "El resto de las reservas concurrentes deben ser rechazadas");
    }
}
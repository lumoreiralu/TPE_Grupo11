package ticketya.compras_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ticketya.compras_service.client.EventoDTO;
import ticketya.compras_service.client.EventosClient;
import ticketya.compras_service.dto.ItemReserva;
import ticketya.compras_service.dto.ReservaRequest;
import ticketya.compras_service.dto.ReservaResponse;
import ticketya.compras_service.exception.NegocioException;
import ticketya.compras_service.model.BloqueoAsiento;
import ticketya.compras_service.model.EstadoBloqueo;
import ticketya.compras_service.model.EstadoOrden;
import ticketya.compras_service.model.OrdenCompra;
import ticketya.compras_service.repository.BloqueoAsientoRepository;
import ticketya.compras_service.repository.OrdenCompraRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
class ReservaServiceTest {

    private static final String EVENTO_ID = "evt-1";
    private static final String SECTOR_ID = "sector-1";

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private OrdenCompraRepository ordenRepo;

    @Autowired
    private BloqueoAsientoRepository bloqueoRepo;

    @MockitoBean
    private EventosClient eventosClient;

    @BeforeEach
    void setUp() {
        bloqueoRepo.deleteAll();
        ordenRepo.deleteAll();
        when(eventosClient.obtenerEvento(EVENTO_ID))
                .thenReturn(new EventoDTO(EVENTO_ID, "PUBLICADO",
                        List.of(new EventoDTO.PrecioSectorDTO(SECTOR_ID, new BigDecimal("100.00")))));
    }

    @Test
    void reservaAsignaVencimientoDeDiezMinutos() {
        LocalDateTime antes = LocalDateTime.now();

        ReservaResponse respuesta = reservaService.reservar(request("cliente-1"), "idem-1");

        assertNotNull(respuesta.ordenId());
        assertEquals(EstadoOrden.RESERVADA_TEMPORAL, respuesta.estado());
        assertTrue(respuesta.expiraEn().isAfter(antes.plusMinutes(9)),
                "La reserva debe vencer cerca de 10 minutos despues");
        assertTrue(respuesta.segundosRestantes() > 540 && respuesta.segundosRestantes() <= 600,
                "Los segundos restantes deben rondar los 600");
        assertEquals(0, respuesta.montoTotal().compareTo(new BigDecimal("100.00")));
        assertEquals(1, bloqueoRepo.count());
    }

    @Test
    void reintentoConLaMismaClaveDeIdempotenciaDevuelveLaMismaOrden() {
        ReservaResponse primera = reservaService.reservar(request("cliente-1"), "idem-1");
        ReservaResponse segunda = reservaService.reservar(request("cliente-1"), "idem-1");

        assertEquals(primera.ordenId(), segunda.ordenId());
        assertEquals(1, ordenRepo.count());
        assertEquals(1, bloqueoRepo.count());
    }

    @Test
    void soloUnaReservaGanaElMismoAsientoEnParalelo() throws Exception {
        int intentos = 2;
        ExecutorService pool = Executors.newFixedThreadPool(intentos);
        CountDownLatch listos = new CountDownLatch(intentos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Object>> resultados = new ArrayList<>();

        try {
            for (int i = 0; i < intentos; i++) {
                final int numero = i;
                resultados.add(pool.submit(() -> {
                    listos.countDown();
                    salida.await(5, TimeUnit.SECONDS);
                    try {
                        return reservaService.reservar(request("cliente-" + numero), "idem-" + numero);
                    } catch (NegocioException e) {
                        return e;
                    }
                }));
            }

            assertTrue(listos.await(5, TimeUnit.SECONDS));
            salida.countDown();

            int exitos = 0;
            int conflictos = 0;
            for (Future<Object> futuro : resultados) {
                Object resultado = futuro.get(15, TimeUnit.SECONDS);
                if (resultado instanceof ReservaResponse) {
                    exitos++;
                } else if (resultado instanceof NegocioException e
                        && e.getStatus() == HttpStatus.CONFLICT) {
                    conflictos++;
                }
            }

            assertEquals(1, exitos, "Solo una solicitud debe quedarse con el asiento");
            assertEquals(1, conflictos, "La solicitud perdedora debe recibir 409");
            assertEquals(1, ordenRepo.count(), "La orden perdedora debe revertirse");
            assertEquals(1, bloqueoRepo.count(), "Solo debe quedar un bloqueo del asiento");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void liberaOrdenVencidaYBorraSusBloqueos() {
        OrdenCompra orden = new OrdenCompra();
        orden.setClienteId("cliente-vencido");
        orden.setEventoId(EVENTO_ID);
        orden.setFechaCreacion(LocalDateTime.now().minusMinutes(15));
        orden.setFechaExpiracionReserva(LocalDateTime.now().minusMinutes(5));
        orden.setEstado(EstadoOrden.RESERVADA_TEMPORAL);
        orden.setMontoTotal(new BigDecimal("100.00"));
        OrdenCompra guardada = ordenRepo.saveAndFlush(orden);

        BloqueoAsiento bloqueo = new BloqueoAsiento();
        bloqueo.setEventoId(EVENTO_ID);
        bloqueo.setSectorId(SECTOR_ID);
        bloqueo.setClaveAsiento(EVENTO_ID + "|" + SECTOR_ID + "|A|1");
        bloqueo.setOrdenId(guardada.getId());
        bloqueo.setEstado(EstadoBloqueo.BLOQUEADO);
        bloqueo.setExpiraEn(LocalDateTime.now().minusMinutes(5));
        bloqueoRepo.saveAndFlush(bloqueo);

        reservaService.liberarReservasVencidas();

        assertEquals(EstadoOrden.EXPIRADA,
                ordenRepo.findById(guardada.getId()).orElseThrow().getEstado());
        assertTrue(bloqueoRepo.findByOrdenId(guardada.getId()).isEmpty(),
                "Los bloqueos de una orden vencida deben eliminarse");
    }

    private ReservaRequest request(String clienteId) {
        return new ReservaRequest(clienteId, EVENTO_ID,
                List.of(new ItemReserva(SECTOR_ID, "A", 1)));
    }
}

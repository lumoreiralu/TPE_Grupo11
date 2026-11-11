package ticketya.compras_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ticketya.compras_service.client.*;
import ticketya.compras_service.dto.ConfirmacionPagoResponse;
import ticketya.compras_service.dto.ConfirmarPagoRequest;
import ticketya.compras_service.dto.ItemReserva;
import ticketya.compras_service.dto.ReservaRequest;
import ticketya.compras_service.dto.ReservaResponse;
import ticketya.compras_service.exception.NegocioException;
import ticketya.compras_service.model.BloqueoAsiento;
import ticketya.compras_service.model.CupoSector;
import ticketya.compras_service.model.EstadoBloqueo;
import ticketya.compras_service.model.EstadoOrden;
import ticketya.compras_service.model.OrdenCompra;
import ticketya.compras_service.repository.BloqueoAsientoRepository;
import ticketya.compras_service.repository.CupoSectorRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class ReservaServiceTest {

    private static final String EVENTO_ID = "evt-1";
    private static final String SECTOR_ID = "sector-1";
    private static final String SECTOR_CAMPO = "sector-campo";

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private CompraTransaccionService transaccionService;

    @Autowired
    private OrdenCompraRepository ordenRepo;

    @Autowired
    private BloqueoAsientoRepository bloqueoRepo;

    @Autowired
    private CupoSectorRepository cupoSectorRepo;

    @MockitoBean
    private EventosClient eventosClient;

    @MockitoBean
    private PasarelaClient pasarelaClient;

    @MockitoBean
    private EntradasClient entradasClient;

    @BeforeEach
    void setUp() {
        bloqueoRepo.deleteAll();
        ordenRepo.deleteAll();
        cupoSectorRepo.deleteAll();

        when(eventosClient.obtenerEvento(EVENTO_ID))
                .thenReturn(new EventoDTO(
                        EVENTO_ID,
                        "PUBLICADO",
                        List.of(
                                new EventoDTO.PrecioSectorDTO(SECTOR_ID, new BigDecimal("100.00")),
                                new EventoDTO.PrecioSectorDTO(SECTOR_CAMPO, new BigDecimal("50.00"))
                        ),
                        List.of(
                                new EventoDTO.SectorInfoDTO(SECTOR_ID, "Platea Numerada", "NUMERADO", 100),
                                new EventoDTO.SectorInfoDTO(SECTOR_CAMPO, "Campo General", "CAPACIDAD_GENERAL", 2)
                        )
                ));
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
    void reservaSectorGeneralDecrementaCupoAtomicamente() {
        ReservaRequest reqCampo = new ReservaRequest("cliente-campo", EVENTO_ID,
                List.of(new ItemReserva(SECTOR_CAMPO, null, null)));

        ReservaResponse respuesta = reservaService.reservar(reqCampo, "idem-campo-1");

        assertNotNull(respuesta.ordenId());
        assertEquals(EstadoOrden.RESERVADA_TEMPORAL, respuesta.estado());
        assertEquals(0, respuesta.montoTotal().compareTo(new BigDecimal("50.00")));

        // Cupos: capacidad inicial 2, reservado 1 -> disponible 1
        CupoSector cupo = cupoSectorRepo.findByEventoIdAndSectorId(EVENTO_ID, SECTOR_CAMPO).orElseThrow();
        assertEquals(2, cupo.getCapacidadTotal());
        assertEquals(1, cupo.getCuposDisponibles());
        assertEquals(1, bloqueoRepo.count());
    }

    @Test
    void concurrenciaSectorGeneralNoSobrevendeUltimoLugar() throws Exception {
        // Capacidad configurada en 2 para SECTOR_CAMPO
        int hilos = 4;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Object>> resultados = new ArrayList<>();

        try {
            for (int i = 0; i < hilos; i++) {
                final int idx = i;
                resultados.add(pool.submit(() -> {
                    listos.countDown();
                    salida.await(5, TimeUnit.SECONDS);
                    try {
                        ReservaRequest req = new ReservaRequest("cliente-c-" + idx, EVENTO_ID,
                                List.of(new ItemReserva(SECTOR_CAMPO, null, null)));
                        return reservaService.reservar(req, "idem-c-" + idx);
                    } catch (NegocioException e) {
                        return e;
                    }
                }));
            }

            assertTrue(listos.await(5, TimeUnit.SECONDS));
            salida.countDown();

            int exitos = 0;
            int conflictos = 0;
            for (Future<Object> f : resultados) {
                Object res = f.get(15, TimeUnit.SECONDS);
                if (res instanceof ReservaResponse) {
                    exitos++;
                } else if (res instanceof NegocioException e && e.getStatus() == HttpStatus.CONFLICT) {
                    conflictos++;
                }
            }

            // Exactamente 2 reservas exitosas (la capacidad máxima) y 2 rechazadas
            assertEquals(2, exitos, "Deben confirmarse exactamente 2 reservas para el cupo máximo");
            assertEquals(2, conflictos, "Las solicitudes excedentes deben recibir 409 CONFLICT");

            // Los cupos en BD deben ser exactamente 0 (nunca negativos)
            CupoSector cupo = cupoSectorRepo.findByEventoIdAndSectorId(EVENTO_ID, SECTOR_CAMPO).orElseThrow();
            assertEquals(0, cupo.getCuposDisponibles(), "El cupo disponible no debe bajar de 0 ni sobrevenderse");
            assertEquals(2, ordenRepo.count(), "Solo deben existir 2 órdenes guardadas");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void liberacionReservaGeneralRestauraCuposDisponibles() {
        ReservaRequest req = new ReservaRequest("cliente-exp-campo", EVENTO_ID,
                List.of(new ItemReserva(SECTOR_CAMPO, null, null)));
        ReservaResponse respuesta = reservaService.reservar(req, "idem-exp-c");

        assertEquals(1, cupoSectorRepo.findByEventoIdAndSectorId(EVENTO_ID, SECTOR_CAMPO).orElseThrow().getCuposDisponibles());

        // Forzar expiración
        OrdenCompra orden = ordenRepo.findById(respuesta.ordenId()).orElseThrow();
        orden.setFechaExpiracionReserva(LocalDateTime.now().minusMinutes(5));
        ordenRepo.saveAndFlush(orden);

        reservaService.liberarReservasVencidas();

        // El cupo debe volver a 2
        assertEquals(2, cupoSectorRepo.findByEventoIdAndSectorId(EVENTO_ID, SECTOR_CAMPO).orElseThrow().getCuposDisponibles());
        assertTrue(bloqueoRepo.findByOrdenId(respuesta.ordenId()).isEmpty());
    }

    @Test
    void rechazoPagoReservaGeneralRestauraCupos() {
        ReservaRequest req = new ReservaRequest("cliente-rechazo-c", EVENTO_ID,
                List.of(new ItemReserva(SECTOR_CAMPO, null, null)));
        ReservaResponse respuesta = reservaService.reservar(req, "idem-rechazo-c");

        assertEquals(1, cupoSectorRepo.findByEventoIdAndSectorId(EVENTO_ID, SECTOR_CAMPO).orElseThrow().getCuposDisponibles());

        when(pasarelaClient.procesarPago(any()))
                .thenReturn(new PagoResponse("txn-rechazada", "RECHAZADO", "Fondos insuficientes"));

        assertThrows(NegocioException.class, () ->
                reservaService.confirmarPago(respuesta.ordenId(), new ConfirmarPagoRequest("TARJETA_RECHAZADA")));

        // El cupo debe restaurarse a 2
        assertEquals(2, cupoSectorRepo.findByEventoIdAndSectorId(EVENTO_ID, SECTOR_CAMPO).orElseThrow().getCuposDisponibles());
        assertTrue(bloqueoRepo.findByOrdenId(respuesta.ordenId()).isEmpty());
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

    @Test
    void confirmarPagoAprobadoCompletaOrdenVendeAsientosYEmitenTickets() {
        ReservaResponse reserva = reservaService.reservar(request("cliente-pago"), "idem-pago");

        when(pasarelaClient.procesarPago(any()))
                .thenReturn(new PagoResponse("txn-12345", "APROBADO", "Pago aprobado exitosamente"));

        when(entradasClient.emitirEntradas(any()))
                .thenReturn(new EmisionResponse(
                        reserva.ordenId().toString(),
                        List.of(new EmisionResponse.EntradaDTO(
                                "ent-1", SECTOR_ID, "A", 1, "hash-qr-123", "EMITIDA", LocalDateTime.now().toString()))));

        ConfirmacionPagoResponse confirmacion = reservaService.confirmarPago(
                reserva.ordenId(),
                new ConfirmarPagoRequest("TARJETA_CREDITO"));

        assertEquals(EstadoOrden.COMPLETADA, confirmacion.estado());
        assertEquals("txn-12345", confirmacion.transaccionPagoId());
        assertEquals(1, confirmacion.entradas().size());
        assertEquals("hash-qr-123", confirmacion.entradas().get(0).codigoQrHash());

        // Verificar persistencia de orden y bloqueo en base de datos
        OrdenCompra ordenGuardada = ordenRepo.findById(reserva.ordenId()).orElseThrow();
        assertEquals(EstadoOrden.COMPLETADA, ordenGuardada.getEstado());
        assertEquals("txn-12345", ordenGuardada.getTransaccionPagoId());

        List<BloqueoAsiento> bloqueos = bloqueoRepo.findByOrdenId(reserva.ordenId());
        assertEquals(1, bloqueos.size());
        assertEquals(EstadoBloqueo.VENDIDO, bloqueos.get(0).getEstado());
    }

    @Test
    void confirmarPagoRechazadoLiberaAsientosYCancelaOrdenEnBD() {
        ReservaResponse reserva = reservaService.reservar(request("cliente-rechazo"), "idem-rechazo");

        when(pasarelaClient.procesarPago(any()))
                .thenReturn(new PagoResponse("txn-rechazada", "RECHAZADO", "Fondos insuficientes"));

        NegocioException exception = assertThrows(NegocioException.class, () ->
                reservaService.confirmarPago(reserva.ordenId(), new ConfirmarPagoRequest("TARJETA_RECHAZADA")));

        assertEquals(HttpStatus.PAYMENT_REQUIRED, exception.getStatus());

        // R5.3: Verificar que los asientos fueron liberados efectivamente en la BD
        assertTrue(bloqueoRepo.findByOrdenId(reserva.ordenId()).isEmpty(),
                "Los bloqueos deben liberarse en la base de datos cuando el pago es rechazado");

        // Verificar que la orden pasa a CANCELADA en la BD y no se hizo rollback
        OrdenCompra ordenGuardada = ordenRepo.findById(reserva.ordenId()).orElseThrow();
        assertEquals(EstadoOrden.CANCELADA, ordenGuardada.getEstado());
        assertEquals("txn-rechazada", ordenGuardada.getTransaccionPagoId());
    }

    @Test
    void confirmarPagoReservaVencidaLiberaAsientosYMarcaExpiradaEnBD() {
        ReservaResponse reserva = reservaService.reservar(request("cliente-vencido-2"), "idem-vencido-2");

        // Forzar vencimiento de la reserva en BD
        OrdenCompra orden = ordenRepo.findById(reserva.ordenId()).orElseThrow();
        orden.setFechaExpiracionReserva(LocalDateTime.now().minusMinutes(1));
        ordenRepo.saveAndFlush(orden);

        NegocioException exception = assertThrows(NegocioException.class, () ->
                reservaService.confirmarPago(reserva.ordenId(), new ConfirmarPagoRequest("TARJETA")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());

        // Verificar que la orden quedó EXPIRADA y sin bloqueos en la BD
        assertTrue(bloqueoRepo.findByOrdenId(reserva.ordenId()).isEmpty());
        assertEquals(EstadoOrden.EXPIRADA, ordenRepo.findById(reserva.ordenId()).orElseThrow().getEstado());

        // No debe haberse llamado a la pasarela
        verify(pasarelaClient, never()).procesarPago(any());
    }

    @Test
    void reintentoTrasFalloDeEmisionNoVuelveACobrar() {
        ReservaResponse reserva = reservaService.reservar(request("cliente-reintento"), "idem-reintento");

        when(pasarelaClient.procesarPago(any()))
                .thenReturn(new PagoResponse("txn-unica-123", "APROBADO", "Pago aprobado"));

        // Simular que entradas-service falla en el primer intento
        when(entradasClient.emitirEntradas(any()))
                .thenThrow(new NegocioException(HttpStatus.BAD_GATEWAY, "entradas-service 500"))
                .thenReturn(new EmisionResponse(
                        reserva.ordenId().toString(),
                        List.of(new EmisionResponse.EntradaDTO("ent-1", SECTOR_ID, "A", 1, "qr-1", "EMITIDA", LocalDateTime.now().toString()))));

        // Primer intento de confirmación falla al emitir
        assertThrows(NegocioException.class, () ->
                reservaService.confirmarPago(reserva.ordenId(), new ConfirmarPagoRequest("TARJETA")));

        // La orden debe estar en PAGO_CONFIRMADO con el ID de transacción guardado
        OrdenCompra ordenEnBD = ordenRepo.findById(reserva.ordenId()).orElseThrow();
        assertEquals(EstadoOrden.PAGO_CONFIRMADO, ordenEnBD.getEstado());
        assertEquals("txn-unica-123", ordenEnBD.getTransaccionPagoId());

        // Segundo intento: Reintento por parte del cliente
        ConfirmacionPagoResponse respuestaReintento = reservaService.confirmarPago(
                reserva.ordenId(), new ConfirmarPagoRequest("TARJETA"));

        assertEquals(EstadoOrden.COMPLETADA, respuestaReintento.estado());
        assertEquals(1, respuestaReintento.entradas().size());

        // Verificar que la pasarela se llamó exactamente UNA sola vez
        verify(pasarelaClient, times(1)).procesarPago(any());
    }

    @Test
    void confirmacionSimultaneaBloqueaDobleCobro() {
        ReservaResponse reserva = reservaService.reservar(request("cliente-simultaneo"), "idem-sim");

        // Marcar la orden como PROCESANDO_PAGO simulando una llamada concurrente en curso
        transaccionService.iniciarProcesamientoPago(reserva.ordenId());

        // La segunda llamada simultánea debe ser rechazada con 409 CONFLICT
        NegocioException exception = assertThrows(NegocioException.class, () ->
                reservaService.confirmarPago(reserva.ordenId(), new ConfirmarPagoRequest("TARJETA")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getMessage().contains("proceso"));

        // No debe llamarse a la pasarela
        verify(pasarelaClient, never()).procesarPago(any());
    }

    private ReservaRequest request(String clienteId) {
        return new ReservaRequest(clienteId, EVENTO_ID,
                List.of(new ItemReserva(SECTOR_ID, "A", 1)));
    }
}

package ticketya.eventos_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ticketya.eventos_service.component.DatosIniciales;

@SpringBootTest
class EventosServiceApplicationTests {

	@MockitoBean
	private DatosIniciales datosIniciales;

	@Test
	void contextLoads() {
	}

}

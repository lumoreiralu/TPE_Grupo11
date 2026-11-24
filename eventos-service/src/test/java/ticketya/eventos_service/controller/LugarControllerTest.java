package ticketya.eventos_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ticketya.eventos_service.repository.LugarRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(LugarController.class)
class LugarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LugarRepository lugarRepository;

    @Test
    void rechazaSectorNumeradoSinFilasNiAsientos() throws Exception {
        String json = """
                {
                  "nombre": "Estadio Central",
                  "sectores": [
                    {
                      "sectorId": "platea-a",
                      "nombre": "Platea Alta",
                      "tipo": "NUMERADO"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/lugares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El sector numerado 'Platea Alta' debe especificar filas y asientos"));
    }

    @Test
    void rechazaSectorGeneralSinCapacidadMaxima() throws Exception {
        String json = """
                {
                  "nombre": "Estadio Central",
                  "sectores": [
                    {
                      "sectorId": "campo",
                      "nombre": "Campo General",
                      "tipo": "CAPACIDAD_GENERAL",
                      "capacidadMaxima": 0
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/lugares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El sector general 'Campo General' debe especificar una capacidad máxima permitida mayor a cero"));
    }

    @Test
    void rechazaSectoresConIdsDuplicados() throws Exception {
        String json = """
                {
                  "nombre": "Estadio Central",
                  "sectores": [
                    {
                      "sectorId": "campo",
                      "nombre": "Campo",
                      "tipo": "CAPACIDAD_GENERAL",
                      "capacidadMaxima": 500
                    },
                    {
                      "sectorId": "campo",
                      "nombre": "Otra ubicación",
                      "tipo": "CAPACIDAD_GENERAL",
                      "capacidadMaxima": 100
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/lugares")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Los identificadores de sector deben ser únicos"));
    }

    @Test
    void creaLugarConSectorNumeradoYGeneral() throws Exception {
        String json = """
                {
                  "nombre": "Complejo Central",
                  "ciudad": "Tandil",
                  "sectores": [
                    {
                      "sectorId": "platea",
                      "nombre": "Platea",
                      "tipo": "NUMERADO",
                      "filas": [
                {"fila": "A", "asientos": [1, 2, 3]}
                      ]
                    },
                    {
                      "sectorId": "campo",
                      "nombre": "Campo",
                      "tipo": "CAPACIDAD_GENERAL",
                      "capacidadMaxima": 500
                    }
                  ]
                }
                """;

        when(lugarRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/lugares")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Complejo Central"))
                .andExpect(jsonPath("$.sectores.length()").value(2));
    }
}


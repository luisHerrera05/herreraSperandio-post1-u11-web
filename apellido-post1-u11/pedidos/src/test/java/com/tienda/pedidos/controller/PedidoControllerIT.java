package com.tienda.pedidos.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PedidoControllerIT {

    @Autowired
    private MockMvc mvc;

    @Test
    void postVipRetorna201ConDescuentoYSinNotasInternas() throws Exception {
        mvc.perform(post("/api/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"clienteEmail\":\"ana@correo.com\",\"tipoCliente\":\"VIP\",\"subtotal\":100000}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.descuento", closeTo(15000.0, 0.01)))
            .andExpect(jsonPath("$.iva", closeTo(16150.0, 0.01)))
            .andExpect(jsonPath("$.total", closeTo(101150.0, 0.01)))
            .andExpect(jsonPath("$.notasInternas").doesNotExist());
    }

    @Test
    void getInexistenteRetorna404ConApiError() throws Exception {
        mvc.perform(get("/api/pedidos/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.mensaje").value("Pedido con id 999 no encontrado."));
    }

    @Test
    void postVacioRetorna400ConErroresDeValidacion() throws Exception {
        mvc.perform(post("/api/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.mensaje", containsString("clienteEmail")));
    }
}

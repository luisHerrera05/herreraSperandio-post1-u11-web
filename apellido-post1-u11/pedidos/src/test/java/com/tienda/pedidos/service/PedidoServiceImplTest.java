package com.tienda.pedidos.service;

import com.tienda.pedidos.dto.PedidoRequestDTO;
import com.tienda.pedidos.dto.PedidoResponseDTO;
import com.tienda.pedidos.entity.Pedido;
import com.tienda.pedidos.exception.RecursoNoEncontradoException;
import com.tienda.pedidos.repository.PedidoRepository;
import com.tienda.pedidos.strategy.DescuentoMayorista;
import com.tienda.pedidos.strategy.DescuentoRegular;
import com.tienda.pedidos.strategy.DescuentoVip;
import com.tienda.pedidos.strategy.EstrategiaDescuento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Prueba unitaria pura (sin Spring): posible gracias a DIP, los colaboradores se sustituyen por mocks. */
class PedidoServiceImplTest {

    private PedidoRepository repo;
    private NotificacionService notificador;
    private PedidoServiceImpl service;

    @BeforeEach
    void setUp() {
        repo = mock(PedidoRepository.class);
        notificador = mock(NotificacionService.class);
        Map<String, EstrategiaDescuento> estrategias = Map.of(
            "REGULAR", new DescuentoRegular(),
            "VIP", new DescuentoVip(),
            "MAYORISTA", new DescuentoMayorista());
        service = new PedidoServiceImpl(repo, notificador, estrategias);
        when(repo.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
    }

    private PedidoRequestDTO request(String tipo, double subtotal) {
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setClienteEmail("ana@correo.com");
        dto.setTipoCliente(tipo);
        dto.setSubtotal(subtotal);
        return dto;
    }

    @Test
    void clienteVipRecibe15PorCiento() {
        PedidoResponseDTO r = service.crear(request("VIP", 100000));
        assertEquals(15000.0, r.getDescuento(), 0.01);
        assertEquals(16150.0, r.getIva(), 0.01);
        assertEquals(101150.0, r.getTotal(), 0.01);
        assertEquals("CREADO", r.getEstado());
    }

    @Test
    void clienteMayoristaRecibe25PorCiento() {
        PedidoResponseDTO r = service.crear(request("MAYORISTA", 100000));
        assertEquals(25000.0, r.getDescuento(), 0.01);
        assertEquals(89250.0, r.getTotal(), 0.01);
    }

    @Test
    void clienteRegularNoRecibeDescuento() {
        PedidoResponseDTO r = service.crear(request("REGULAR", 100000));
        assertEquals(0.0, r.getDescuento(), 0.01);
        assertEquals(119000.0, r.getTotal(), 0.01);
    }

    @Test
    void tipoDesconocidoCaeEnRegularYEsInsensibleAMayusculas() {
        assertEquals(0.0, service.crear(request("CORPORATIVO", 100000)).getDescuento(), 0.01);
        assertEquals(15000.0, service.crear(request("vip", 100000)).getDescuento(), 0.01);
    }

    @Test
    void creaPedidoNotificaAlClienteUnaVez() {
        service.crear(request("VIP", 100000));
        verify(notificador).enviarConfirmacion(eq("ana@correo.com"), anyDouble());
    }

    @Test
    void buscarPorIdInexistenteLanzaRecursoNoEncontrado() {
        when(repo.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.buscarPorId(999L));
    }
}

package com.tienda.pedidos.service;

import com.tienda.pedidos.dto.PedidoRequestDTO;
import com.tienda.pedidos.dto.PedidoResponseDTO;
import com.tienda.pedidos.entity.Pedido;
import com.tienda.pedidos.exception.RecursoNoEncontradoException;
import com.tienda.pedidos.repository.PedidoRepository;
import com.tienda.pedidos.strategy.EstrategiaDescuento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PedidoServiceImpl implements PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoServiceImpl.class);

    private static final double TASA_IVA = 0.19;

    private final PedidoRepository repo;
    private final NotificacionService notificador;
    private final Map<String, EstrategiaDescuento> estrategias;

    public PedidoServiceImpl(PedidoRepository repo,
                             NotificacionService notificador,
                             Map<String, EstrategiaDescuento> estrategias) {
        this.repo = repo;
        this.notificador = notificador;
        this.estrategias = estrategias;
    }

    @Override
    public PedidoResponseDTO crear(PedidoRequestDTO dto) {
        log.info("Creando pedido: clienteEmail={}, tipoCliente={}",
                 dto.getClienteEmail(), dto.getTipoCliente());
        EstrategiaDescuento estrategia = estrategias.getOrDefault(
            dto.getTipoCliente().toUpperCase(), estrategias.get("REGULAR"));
        double descuento = estrategia.calcularDescuento(dto.getSubtotal());
        log.info("Descuento aplicado: pedido para clienteEmail={}, tipoCliente={}, descuento={}",
                 dto.getClienteEmail(), dto.getTipoCliente(), descuento);
        double baseConDescuento = dto.getSubtotal() - descuento;
        double iva = baseConDescuento * TASA_IVA;
        double total = baseConDescuento + iva;

        Pedido pedido = new Pedido();
        pedido.setClienteEmail(dto.getClienteEmail());
        pedido.setTipoCliente(dto.getTipoCliente().toUpperCase());
        pedido.setSubtotal(dto.getSubtotal());
        pedido.setDescuento(descuento);
        pedido.setIva(iva);
        pedido.setTotal(total);
        pedido.setEstado("CREADO");
        pedido.setNotasInternas("Pedido creado automaticamente, revisar stock antes de despachar");
        pedido.setFechaCreacion(LocalDateTime.now());

        Pedido guardado = repo.save(pedido);
        log.info("Pedido creado exitosamente con id={}, total={}", guardado.getId(), guardado.getTotal());
        notificador.enviarConfirmacion(guardado.getClienteEmail(), guardado.getTotal());
        return toResponseDTO(guardado);
    }

    @Override
    public PedidoResponseDTO buscarPorId(Long id) {
        log.debug("Buscando pedido con id={}", id);
        Pedido p = repo.findById(id).orElseThrow(() -> {
            log.warn("Pedido con id={} no encontrado", id);
            return new RecursoNoEncontradoException("Pedido", id);
        });
        return toResponseDTO(p);
    }

    @Override
    public List<PedidoResponseDTO> buscarPorClienteEmail(String clienteEmail) {
        log.debug("Listando pedidos de clienteEmail={}", clienteEmail);
        List<PedidoResponseDTO> pedidos = repo.findByClienteEmail(clienteEmail).stream()
            .map(this::toResponseDTO)
            .toList();
        log.info("Se encontraron {} pedidos para clienteEmail={}", pedidos.size(), clienteEmail);
        return pedidos;
    }

    private PedidoResponseDTO toResponseDTO(Pedido p) {
        PedidoResponseDTO dto = new PedidoResponseDTO();
        dto.setId(p.getId());
        dto.setClienteEmail(p.getClienteEmail());
        dto.setTipoCliente(p.getTipoCliente());
        dto.setSubtotal(p.getSubtotal());
        dto.setDescuento(p.getDescuento());
        dto.setIva(p.getIva());
        dto.setTotal(p.getTotal());
        dto.setEstado(p.getEstado());
        return dto;
    }
}

package com.tienda.pedidos.service;

import com.tienda.pedidos.dto.PedidoRequestDTO;
import com.tienda.pedidos.dto.PedidoResponseDTO;

import java.util.List;

/** Abstraccion de la logica de pedidos: el controller depende de esta interfaz (DIP). */
public interface PedidoService {

    PedidoResponseDTO crear(PedidoRequestDTO dto);

    PedidoResponseDTO buscarPorId(Long id);

    List<PedidoResponseDTO> buscarPorClienteEmail(String clienteEmail);
}

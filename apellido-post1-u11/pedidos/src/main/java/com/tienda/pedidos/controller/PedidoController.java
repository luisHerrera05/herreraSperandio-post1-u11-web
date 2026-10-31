package com.tienda.pedidos.controller;

import com.tienda.pedidos.dto.PedidoRequestDTO;
import com.tienda.pedidos.dto.PedidoResponseDTO;
import com.tienda.pedidos.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@Tag(name = "Pedidos", description = "Creacion y consulta de pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @Operation(summary = "Crear un nuevo pedido")
    @ApiResponse(responseCode = "201", description = "Pedido creado, con descuento aplicado segun tipoCliente")
    @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponseDTO crear(@Valid @RequestBody PedidoRequestDTO dto) {
        return service.crear(dto);
    }

    @Operation(summary = "Obtener pedido por ID")
    @ApiResponse(responseCode = "200", description = "Pedido encontrado")
    @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    @GetMapping("/{id}")
    public PedidoResponseDTO obtener(
        @Parameter(description = "ID del pedido", example = "1")
        @PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @Operation(summary = "Listar los pedidos de un cliente por su correo")
    @ApiResponse(responseCode = "200", description = "Lista de pedidos del cliente (vacia si no tiene ninguno)")
    @ApiResponse(responseCode = "400", description = "Falta el parametro clienteEmail")
    @GetMapping
    public List<PedidoResponseDTO> listarPorCliente(
        @Parameter(description = "Correo del cliente", example = "ana@correo.com")
        @RequestParam String clienteEmail) {
        return service.buscarPorClienteEmail(clienteEmail);
    }
}

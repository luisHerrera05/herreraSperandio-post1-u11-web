package com.tienda.pedidos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificacionEmailServiceImpl implements NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionEmailServiceImpl.class);

    @Override
    public void enviarConfirmacion(String clienteEmail, double total) {
        // Simulacion de envio de correo (fuera del alcance de este laboratorio)
        log.info("Correo de confirmacion enviado a {} por un total de {}", clienteEmail, total);
    }
}

#!/usr/bin/env bash
# Genera la evidencia de los checkpoints (la aplicacion debe estar corriendo en :8080)
B=http://localhost:8080/api/pedidos
echo "== POST VIP (descuento 15000, iva 16150, total 101150) =="
curl -s -X POST $B -H "Content-Type: application/json" \
  -d '{"clienteEmail":"ana@correo.com","tipoCliente":"VIP","subtotal":100000}'; echo
echo "== POST MAYORISTA (descuento 25000) =="
curl -s -X POST $B -H "Content-Type: application/json" \
  -d '{"clienteEmail":"luis@correo.com","tipoCliente":"MAYORISTA","subtotal":100000}'; echo
echo "== POST REGULAR (descuento 0) =="
curl -s -X POST $B -H "Content-Type: application/json" \
  -d '{"clienteEmail":"eva@correo.com","tipoCliente":"REGULAR","subtotal":100000}'; echo
echo "== GET /1 (200, sin notasInternas) =="
curl -s $B/1; echo
echo "== GET /999 (404 con ApiError) =="
curl -s -i $B/999; echo
echo "== POST {} (400 con errores de validacion) =="
curl -s -i -X POST $B -H "Content-Type: application/json" -d '{}'; echo
echo "== GET ?clienteEmail=ana@correo.com (lista) =="
curl -s "$B?clienteEmail=ana@correo.com"; echo

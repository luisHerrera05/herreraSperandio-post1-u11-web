# Post-contenido — Unidad 11: Buenas Prácticas y Patrones de Diseño

## Descripción
Repositorio del laboratorio de la Unidad 11 de Programación Web — Séptimo Semestre (UDES).
Contiene un único proyecto Maven Spring Boot (`pedidos/`, paquete raíz `com.tienda.pedidos`) que parte de un código
con violaciones reales de SOLID (SRP y DIP), SQL embebido y una entidad JPA expuesta directamente en la API, y lo
refactoriza aplicando arquitectura en capas, el patrón DTO, inyección de dependencias por constructor y el patrón
Strategy para el descuento por tipo de cliente. Se endurece con manejo centralizado de errores, logging
(SLF4J/Logback) y documentación de la API (springdoc-openapi/Swagger).

**Stack:** Java 17 · Spring Boot 3.2.5 · Maven · Spring Web · Spring Data JPA · H2 · Bean Validation · springdoc-openapi 2.3.0

## Arquitectura resultante (diagrama de clases en texto)

```
Cliente HTTP (JSON)
        |
        v
+--------------------+
| PedidoController   |  controller/  -> solo traduce HTTP <-> servicio
+--------------------+
        | depende de la INTERFAZ (DIP)
        v
+--------------------+
| <<interface>>      |  service/
| PedidoService      |
+--------------------+
        ^ implements
        |
+--------------------+   constructor (DIP)   +---------------------------+
| PedidoServiceImpl  |---------------------->| PedidoRepository          |  repository/
| orquesta: estrategia                       | (extends JpaRepository)   |------> Pedido (entity/)
| -> calculo -> guardar|                      +---------------------------+
| -> notificar       |   constructor (DIP)   +---------------------------+
|                    |---------------------->| <<interface>>             |
|                    |                       | NotificacionService       |
|                    |                       +---------------------------+
|                    |                                   ^ implements
|                    |                       NotificacionEmailServiceImpl
|                    |   constructor (DIP)   +---------------------------+
|                    |---------------------->| Map<String,               |  strategy/
+--------------------+                       |   EstrategiaDescuento>    |
                                             +---------------------------+
                                                 |  "REGULAR"  -> DescuentoRegular   (0%)
                                                 |  "VIP"      -> DescuentoVip       (15%)
                                                 |  "MAYORISTA"-> DescuentoMayorista (25%)

dto/        PedidoRequestDTO (entrada, validado) | PedidoResponseDTO (salida, SIN notasInternas)
exception/  GlobalExceptionHandler (@RestControllerAdvice) | ApiError | RecursoNoEncontradoException
```

```
src/main/java/com/tienda/pedidos/
├── PedidosApplication.java
├── controller/   PedidoController
├── service/      PedidoService, PedidoServiceImpl, NotificacionService, NotificacionEmailServiceImpl
├── repository/   PedidoRepository
├── dto/          PedidoRequestDTO, PedidoResponseDTO
├── entity/       Pedido
├── strategy/     EstrategiaDescuento, DescuentoRegular, DescuentoVip, DescuentoMayorista
└── exception/    ApiError, GlobalExceptionHandler, RecursoNoEncontradoException
```

## Violaciones identificadas en el código de partida y cómo se corrigieron

| # | Violación en el código de partida | Corrección aplicada |
|---|-----------------------------------|---------------------|
| 1 | **SRP**: `crearPedido` validaba, calculaba el descuento, armaba el SQL, persistía, notificaba y logueaba (6 responsabilidades). | Cada responsabilidad en su clase: validación → Bean Validation en `PedidoRequestDTO`; descuento → `strategy/`; persistencia → `PedidoRepository`; orquestación → `PedidoServiceImpl`; notificación → `NotificacionService`; logging → SLF4J. |
| 2 | **DIP**: `new NotificacionEmailService()` dentro del controller (acoplamiento a la implementación, imposible de mockear). | `NotificacionService` (interfaz) inyectada por constructor en `PedidoServiceImpl`; el controller depende de `PedidoService` (interfaz). Ninguna clase usa `new` para un colaborador. Se demuestra con `PedidoServiceImplTest` (mocks). |
| 3 | **Sin capa de acceso a datos**: SQL concatenado en el controller (además vulnerable a inyección SQL). | `PedidoRepository extends JpaRepository`: cero SQL escrito a mano, consultas parametrizadas por Spring Data. |
| 4 | **Sin DTO**: se devolvía la entidad `Pedido`, filtrando `notasInternas`. | `PedidoRequestDTO` / `PedidoResponseDTO` con mapeo explícito; la respuesta no contiene `notasInternas`. |
| 5 | **Manejo de errores disperso**: `try/catch` que silenciaba con `println` y devolvía `null` (200 vacío para un id inexistente). | Se eliminó el `try/catch`; `RecursoNoEncontradoException` + `GlobalExceptionHandler` (`@RestControllerAdvice`) → 404/400/500 con cuerpo `ApiError`. |
| 6 | **`if/else` de descuento en el controller** (cerrado a la extensión, viola OCP). | Patrón **Strategy**: `EstrategiaDescuento` + 3 `@Component` con nombre, inyectados como `Map<String, EstrategiaDescuento>`. Un nuevo tipo = una clase nueva, sin tocar el servicio. |
| 7 | **`System.out.println`** como logging. | SLF4J con placeholders `{}` y niveles INFO/WARN/ERROR/DEBUG + `logback-spring.xml`. |

## Decisiones de diseño
**Parte 1**
- **Strategy en vez de un segundo patrón creacional (Factory/Mapper) para la conversión a DTO.** El descuento tiene una
  variación real de comportamiento y es candidato a crecer (un futuro `CORPORATIVO` solo requiere una clase nueva con
  `@Component`, cumpliendo OCP). La conversión entidad→DTO es un mapeo 1:1 sin lógica condicional, resuelto con un método
  privado `toResponseDTO()` para evitar sobre-ingeniería.
- **`JpaRepository` en vez de mantener `JdbcTemplate`**: elimina el SQL manual y la inyección SQL, y da consultas derivadas
  (`findByClienteEmail`) con seguridad de tipos.

**Parte 2**
- **El registro del descuento aplicado vive en `PedidoServiceImpl`, no en cada Strategy**: un único punto de auditoría
  por pedido en lugar de dispersarlo en tres clases.
- **`notasInternas` nunca se registra en los logs ni se expone en el DTO de respuesta**: es información operativa de
  logística y los logs pueden compartirse con soporte o retenerse 30 días.

> Extra: se añadió `GET /api/pedidos?clienteEmail=...` (usa `findByClienteEmail` del repositorio) para tener 3 endpoints
> documentados en Swagger, y un handler 400 adicional para JSON mal formado.

## Cómo compilar y ejecutar
Requisitos: Java 17+ y Maven 3.9.x.
1. Clonar el repositorio: `git clone https://github.com/[usuario]/apellido-post1-u11.git`
2. Entrar al proyecto: `cd apellido-post1-u11/pedidos`
3. Compilar: `mvn compile` (o ejecutar las pruebas: `mvn test`)
4. Ejecutar: `mvn spring-boot:run` (o `./mvnw spring-boot:run` si se generó el wrapper desde start.spring.io)
5. Probar los endpoints con Postman o curl en `http://localhost:8080/api/pedidos`
   (con la app corriendo, `../pruebas-curl.sh` ejecuta todos los casos de los checkpoints).
6. Logs en consola y en `pedidos/logs/pedidos.log` (carpeta ignorada por git).
7. Documentación interactiva: http://localhost:8080/swagger-ui.html (JSON OpenAPI en `/api-docs`).
8. Consola H2 opcional: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:pedidosdb`, usuario `sa`, sin contraseña).

### Endpoints
| Método | Ruta | Respuestas |
|--------|------|-----------|
| POST | `/api/pedidos` | 201 creado · 400 datos inválidos |
| GET | `/api/pedidos/{id}` | 200 · 404 no encontrado |
| GET | `/api/pedidos?clienteEmail=` | 200 lista (vacía si no hay pedidos) · 400 |

Ejemplo: `{"clienteEmail":"ana@correo.com","tipoCliente":"VIP","subtotal":100000}` → `201` con `descuento=15000`, `iva=16150`, `total=101150`.


# Invoice Assistant

Base técnica de un SaaS de facturación conversacional. Release 1 demuestra un flujo hexagonal mínimo para crear y persistir un borrador; WhatsApp será la interfaz del producto en releases posteriores.

## Arquitectura

- `domain`: modelo y reglas puras, sin dependencias de frameworks.
- `application`: puertos de entrada/salida y casos de uso.
- `infrastructure`: adaptadores REST y PostgreSQL, además de configuración Spring.

Stack: Java 25, Spring Boot 4.1.1, Spring MVC, Maven, PostgreSQL, JPA, Flyway, Validation, Actuator, OpenAPI, JUnit 5, Mockito y Testcontainers.

## Ejecución local

Requisitos: JDK 25 y Docker. El repositorio incluye Maven Wrapper.

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

La configuración acepta `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` y `DB_PASSWORD`, con valores locales predeterminados alineados con Compose.

En Windows use `mvnw.cmd` en lugar de `./mvnw`. La prueba de persistencia usa un PostgreSQL efímero de Testcontainers y se omite si Docker no está disponible.

```bash
./mvnw test
./mvnw verify
```

Construcción y ejecución de la imagen de aplicación:

```bash
./mvnw clean package
docker build -t invoice-automation-service .
```

## API

Crear un borrador sin cuerpo:

```bash
curl -i -X POST http://localhost:8080/api/v1/invoice-drafts
```

Swagger UI: `http://localhost:8080/swagger-ui.html`. Salud: `http://localhost:8080/actuator/health`.

## Alcance de Release 1

Incluye bootstrap, separación hexagonal, `InvoiceDraft` mínimo, creación, persistencia PostgreSQL con migración, endpoint, Docker y pruebas. No incluye Company, Customer, líneas, impuestos, validaciones de facturación, aprobación, proveedor electrónico, SUNAT, autenticación, WhatsApp, IA ni almacenamiento documental.

El siguiente release previsto incorpora Company y la base de configuración multiempresa; no está implementado aquí.

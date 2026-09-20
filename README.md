# Invoice Assistant

Servicio backend de un SaaS de facturación conversacional. El estado actual cubre los Releases 1 al 8: base hexagonal, configuración multiempresa, borradores con ítems, persistencia PostgreSQL, API REST, validaciones, aprobación y emisión mediante un proveedor simulado. WhatsApp y la integración real de emisión electrónica se incorporarán en releases posteriores.

## Arquitectura

- `domain`: modelo y reglas puras, sin dependencias de frameworks.
- `application`: puertos de entrada/salida y casos de uso.
- `infrastructure`: adaptadores REST y PostgreSQL, además de configuración Spring.

Stack: Java 25, Spring Boot 4.1.1, Spring MVC, Maven, PostgreSQL, JPA, Flyway, Validation, Actuator, OpenAPI, JUnit 5 y Mockito.

## Ejecución local

Requisitos: JDK 25 y Docker. El repositorio incluye Maven Wrapper.

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

La configuración acepta `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` y `DB_PASSWORD`, con valores locales predeterminados alineados con Compose.

En Windows use `mvnw.cmd` en lugar de `./mvnw`. Las pruebas unitarias y web no requieren Docker; PostgreSQL sí es necesario para ejecutar la aplicación completa.

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

Crear un borrador completo:

```bash
curl -i -X POST http://localhost:8080/api/v1/invoice-drafts \
  -H "Content-Type: application/json" \
  -d '{"companyId":"company-id","documentType":"SALES_RECEIPT","recipientDocumentType":"DNI","recipientDocumentNumber":"12345678","currency":"PEN","items":[{"description":"Servicio de consultoría","unitCode":"NIU","quantity":2,"unitPrice":150.00,"discount":0,"taxAffectation":"TAXED"}]}'
```

Crear una empresa:

```bash
curl -i -X POST http://localhost:8080/api/v1/companies \
  -H "Content-Type: application/json" \
  -d '{"legalName":"Dark Bit SAC","tradeName":"Dark Bit","taxId":"20123456789","address":"Lima"}'
```

Consultar empresas:

```bash
curl -i http://localhost:8080/api/v1/companies
curl -i http://localhost:8080/api/v1/companies/{id}
```

Consultar un borrador:

```bash
curl -i http://localhost:8080/api/v1/invoice-drafts/{draftId}
```

Aprobar y emitir un borrador mediante el proveedor mock:

```bash
curl -i -X POST http://localhost:8080/api/v1/invoice-drafts/{draftId}/approve
curl -i -X POST http://localhost:8080/api/v1/invoice-drafts/{draftId}/issue
```

Swagger UI: `http://localhost:8080/swagger-ui.html`. Salud: `http://localhost:8080/actuator/health`.

## Colección Postman

El repositorio incluye una colección ejecutable con todos los endpoints disponibles hasta Release 8:

- Colección: `docs/postman/invoice-automation-service.postman_collection.json`
- Environment local: `docs/postman/local.postman_environment.json`

Importe ambos archivos en Postman, seleccione el environment `Invoice Automation Service - Local` y ejecute la colección completa en el orden definido. El flujo realiza automáticamente lo siguiente:

1. Comprueba el estado de la aplicación.
2. Inicia sesión con el usuario local creado por Flyway V7 y guarda el JWT.
3. Consulta los datos del usuario autenticado.
4. Crea una empresa y guarda su ID.
5. Configura y consulta el perfil tributario del emisor.
6. Consulta y lista empresas.
7. Conserva los endpoints administrativos de clientes.
8. Resuelve automáticamente un receptor por DNI y guarda su ID interno.
9. Crea una boleta borrador con dos ítems, sin enviar `customerId`.
10. Recupera el borrador persistido y valida sus ítems y totales.
11. Aprueba el borrador y comprueba su cambio de estado.
12. Emite el borrador con el proveedor mock y valida la referencia generada.

Las credenciales locales iniciales son `haroldqc` / `password`. Son exclusivamente para desarrollo y pueden sobrescribirse en el environment de Postman. Los identificadores fiscales y documentos usados por la colección se generan dinámicamente para permitir varias ejecuciones.

## Aislamiento multiempresa

Cada empresa creada queda asociada automáticamente al usuario autenticado. Las consultas de empresas, clientes y borradores verifican esa pertenencia y responden como recurso inexistente cuando el identificador pertenece a otro usuario, evitando revelar datos entre negocios.

Los clientes pertenecen a una empresa concreta. Si el usuario tiene una sola empresa, el backend puede resolverla automáticamente. Cuando tiene varias, debe enviar el contexto mediante el header:

```http
X-Company-Id: <companyId>
```

La colección Postman configura este header automáticamente con la empresa creada durante el flujo. Los borradores siguen recibiendo `companyId`, pero el backend valida que la empresa y el cliente pertenezcan al usuario autenticado y al mismo tenant.

## Preparación tributaria previa al proveedor real

Antes de integrar el proveedor real se incorporaron tres capacidades:

- **Onboarding del emisor:** `POST /api/v1/companies/{id}/tax-profile` registra tipo de contribuyente, domicilio fiscal, ubigeo y ubicación. Un borrador no puede crearse hasta completar este perfil.
- **Resolución del receptor:** `POST /api/v1/recipients/resolve` busca por empresa, tipo y número de documento. Si no existe, usa `RecipientLookupProvider` y lo persiste internamente. El adaptador predeterminado es `mock`; se cambia mediante `RECIPIENT_LOOKUP_PROVIDER`.
- **Tipo de comprobante:** `INVOICE` representa factura/código SUNAT `01` y exige receptor con RUC. `SALES_RECEIPT` representa boleta/código SUNAT `03` y admite DNI o RUC.

El alta manual de un cliente ya no es necesaria para crear un borrador. El request recibe `recipientDocumentType` y `recipientDocumentNumber`; el backend crea o reutiliza el `Customer` interno y conserva su identificador para integridad e historial.

## Impuestos, numeración y comprobante definitivo

Los ítems del borrador incluyen unidad de medida, descuento y afectación tributaria. Se soportan `TAXED` (IGV 18 %, código 10), `EXEMPT` (código 20) y `UNAFFECTED` (código 30), junto con unidades `NIU`, `ZZ`, `KGM` y `LTR`. El dominio calcula por ítem y consolida:

- importe bruto;
- descuento total;
- base imponible;
- IGV;
- total final.

Las series se configuran por empresa y tipo mediante `POST /api/v1/companies/{id}/document-series`. Las facturas utilizan series `F...` y las boletas series `B...`. Al emitir, el correlativo siguiente se reserva dentro de una transacción con bloqueo pesimista, evitando duplicados en emisiones concurrentes.

La emisión crea un `ElectronicDocument` separado del borrador. Este objeto conserva una copia inmutable del receptor, ítems, impuestos, serie, correlativo, número completo y respuesta del proveedor. Puede consultarse mediante `GET /api/v1/electronic-documents/{id}`; no existe API para modificarlo.

## Alcance de Release 1

Establece la base técnica y las convenciones sobre las que se construyen los siguientes releases:

- Proyecto Spring Boot con Java 25 y Maven Wrapper para ejecuciones reproducibles.
- Organización hexagonal en `domain`, `application` e `infrastructure`.
- Puertos de entrada y salida para evitar que el dominio dependa de Spring, HTTP o JPA.
- Respuestas HTTP uniformes mediante `ApiResponse` y manejo centralizado de excepciones.
- Configuración local con Docker Compose, PostgreSQL, Actuator y documentación OpenAPI.
- Utilidades compartidas, validadores y configuración web reutilizables por los módulos funcionales.

Este release no desarrolla todavía reglas completas de facturación; su objetivo es dejar preparada una estructura extensible y comprobable.

## Alcance de Release 2

Introduce `Company` como raíz de la configuración multiempresa:

- Modelo de empresa con razón social, nombre comercial, identificador fiscal, dirección y estado activo.
- Caso de uso para crear empresas, consultar una empresa por ID y listar empresas activas.
- Persistencia mediante un puerto de repositorio, adaptador JPA y tabla `companies` administrada por Flyway.
- Generación interna de identificadores para mantener esa responsabilidad fuera del controlador.
- Validación de razón social e identificador fiscal obligatorios.
- Respuesta `404` controlada cuando la empresa solicitada no existe.
- Pruebas del servicio, controlador y adaptador de persistencia.

La empresa queda disponible como referencia del agregado de facturación que se incorpora posteriormente.

## Alcance de Release 3

Define el primer agregado de facturación útil, formado por `InvoiceDraft` e `InvoiceItem`:

- `InvoiceDraft` representa una factura todavía editable y nace con estado `DRAFT`.
- Cada borrador referencia una empresa emisora y un cliente, además de indicar su moneda.
- El catálogo `Customer` existente se utiliza como referencia del receptor de la factura.
- `InvoiceItem` representa un concepto facturable con descripción, cantidad y precio unitario.
- Cada ítem calcula su importe con `BigDecimal`, evitando errores de precisión de punto flotante.
- El borrador calcula `subtotal` y `total` a partir de sus ítems. En esta etapa ambos valores son iguales porque todavía no se aplican impuestos ni descuentos.
- Los ítems se exponen como una colección inmutable para proteger la consistencia del agregado.
- Se registran las fechas de creación y última actualización utilizando un reloj inyectable y UTC.

En este release se define el comportamiento del dominio; los detalles de almacenamiento pertenecen al siguiente.

## Alcance de Release 4

Implementa la persistencia PostgreSQL del agregado completo:

- Entidades JPA separadas para el borrador y sus ítems.
- Guardado del encabezado y todos sus ítems dentro de una misma transacción.
- Reconstrucción del agregado completo al consultar un borrador.
- Claves foráneas hacia `companies`, `customers` e `invoice_draft` para preservar integridad referencial.
- Eliminación en cascada de ítems cuando se elimina su borrador en la base de datos.
- Posición persistente y única para conservar el orden original de los ítems.
- Columnas `NUMERIC` con escalas explícitas para cantidades, precios e importes.
- Migraciones Flyway versionadas para `invoice_draft`, `companies`, `customers` e `invoice_items`.
- Hibernate configurado con `ddl-auto: validate`: Flyway crea el esquema y Hibernate comprueba que coincida con las entidades.

El puerto `InvoiceDraftRepository` continúa ocultando JPA al dominio y a los casos de uso.

## Alcance de Release 5

Expone el agregado mediante una API REST:

- `POST /api/v1/invoice-drafts` crea y persiste un borrador completo con sus ítems.
- `GET /api/v1/invoice-drafts/{id}` recupera el encabezado, los ítems, totales, estado y fechas.
- La creación devuelve HTTP `201 Created`; la consulta devuelve HTTP `200 OK`.
- Las respuestas siguen el formato común `ApiResponse`.
- Un identificador de borrador inexistente produce HTTP `404 Not Found`.
- DTOs específicos separan el contrato HTTP de los modelos del dominio y de las entidades JPA.
- Pruebas web con `MockMvc` verifican códigos HTTP y estructura JSON.

La API trabaja con el borrador como una unidad; los ítems no se administran mediante endpoints independientes en esta etapa.

## Alcance de Release 6

Refuerza las reglas necesarias para impedir la creación de borradores inconsistentes:

- La empresa y el cliente indicados deben existir y estar activos.
- La moneda debe usar un código ISO de tres letras mayúsculas, por ejemplo `PEN` o `USD`.
- Todo borrador debe contener al menos un ítem.
- La descripción del ítem es obligatoria y se almacena sin espacios exteriores.
- La cantidad debe ser mayor que cero y admite hasta cuatro decimales.
- El precio unitario no puede ser negativo y admite hasta dos decimales.
- El dominio vuelve a comprobar sus invariantes aunque el objeto no provenga del controlador HTTP.
- Los errores de validación se convierten en respuestas HTTP `400 Bad Request` uniformes y legibles.
- Las pruebas cubren datos inválidos, referencias inactivas, cálculo de importes e inmutabilidad del agregado.

## Alcance de Release 7

Incorpora la aprobación explícita del borrador:

- Añade el estado `APPROVED` al ciclo de vida de `InvoiceDraft`.
- Implementa la transición de dominio `DRAFT → APPROVED` como una operación inmutable.
- Impide aprobar nuevamente un borrador aprobado o emitido.
- Actualiza `updatedAt` utilizando el reloj UTC inyectado.
- Persiste el nuevo estado dentro de una transacción.
- Expone `POST /api/v1/invoice-drafts/{id}/approve`.
- Devuelve HTTP `409 Conflict` cuando se intenta ejecutar una transición desde un estado incorrecto.
- Incluye pruebas de dominio, servicio y controlador para transiciones válidas e inválidas.

La aprobación solamente confirma que el agregado está listo para emitirse; no llama todavía a un proveedor externo.

## Alcance de Release 8

Introduce la abstracción necesaria para emitir una factura sin acoplar el caso de uso a un proveedor específico:

- Define el puerto de salida `BillingProvider`, que recibe un borrador aprobado y devuelve una referencia y fecha de emisión.
- Implementa `MockBillingProvider`, un adaptador local determinista que genera referencias con el formato `MOCK-{invoiceDraftId}`.
- Selecciona el proveedor mediante `BILLING_PROVIDER`; el valor predeterminado es `mock`.
- Añade el estado `ISSUED` y la transición `APPROVED → ISSUED`.
- Persiste `providerReference` e `issuedAt` mediante una migración Flyway incremental.
- Exige que todo borrador emitido tenga referencia y fecha del proveedor.
- Expone `POST /api/v1/invoice-drafts/{id}/issue` para ejecutar el flujo completo con el mock.
- Incluye pruebas del adaptador mock, del caso de uso, del dominio y del contrato HTTP.

La futura integración con SUNAT implementará el mismo puerto `BillingProvider`, permitiendo sustituir el mock sin modificar el dominio ni el controlador.

Hasta este punto no se incluyen impuestos, descuentos, proveedor SUNAT real, autenticación, WhatsApp ni inteligencia artificial.

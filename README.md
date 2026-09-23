# Invoice Assistant

Servicio backend de un SaaS de facturación conversacional. El estado actual incluye la base hexagonal, seguridad JWT, aislamiento multiempresa, onboarding tributario, resolución de receptores, borradores con impuestos, series y correlativos, aprobación, comprobantes electrónicos inmutables y seguimiento del proveedor mediante un adaptador simulado. WhatsApp y las integraciones externas reales se incorporarán posteriormente.

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

El repositorio incluye una colección ejecutable con los endpoints actualmente disponibles:

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

La emisión crea un `ElectronicDocument` separado del borrador. Este objeto conserva una copia inmutable del emisor, receptor, ítems, impuestos, serie, correlativo, número completo y respuesta del proveedor. Puede consultarse mediante `GET /api/v1/electronic-documents/{id}`; no existe API para modificar sus datos fiscales.

### Snapshot tributario del comprobante

En el momento de emitir se copian al comprobante los datos que no deben cambiar aunque luego se actualice la configuración maestra:

- RUC, razón social, nombre comercial y tipo de contribuyente del emisor;
- domicilio fiscal, ubigeo, departamento, provincia, distrito y país;
- tipo y número de documento, nombre o razón social, dirección y correo del receptor;
- detalle de ítems, unidades, descuentos, afectaciones e importes tributarios.

`BillingSubmission` se construye exclusivamente desde este snapshot. Por tanto, una modificación posterior de `Company`, `IssuerTaxProfile` o `Customer` no altera el JSON correspondiente a un comprobante ya numerado. La migración Flyway `V14__snapshot_issuer_and_recipient.sql` amplía los clientes con dirección y correo opcionales, y completa el snapshot de los comprobantes existentes.

### Idempotencia de emisión

`POST /api/v1/invoice-drafts/{id}/issue` es idempotente por borrador. Si ya existe un comprobante para el mismo `draftId`, devuelve exactamente ese documento sin consumir otro correlativo ni invocar nuevamente a `BillingProvider`.

La emisión adquiere un bloqueo pesimista sobre el borrador para serializar solicitudes concurrentes. El identificador de `ElectronicDocument` se deriva de forma determinística del ID del borrador y se envía como `BillingSubmission.idempotencyKey`; el adaptador real debe transmitirlo al campo equivalente del proveedor, por ejemplo `codigo_unico` en NUBEFACT. La restricción única de `electronic_documents.draft_id` permanece como última defensa en la base de datos.

### Separación entre persistencia y proveedor

La emisión no mantiene una transacción de base de datos abierta mientras espera una respuesta HTTP del proveedor. El flujo se divide en tres fases:

1. Una transacción corta bloquea el borrador, reserva el correlativo y confirma el `ElectronicDocument` como `SENDING`.
2. `BillingProvider.submit(...)` se ejecuta fuera de cualquier transacción de persistencia, usando el ID determinístico como clave de idempotencia.
3. Otra transacción corta bloquea el comprobante y guarda la respuesta. Si el proveedor falla con una excepción, se persiste el estado `ERROR` junto con el código `PROVIDER_CALL_FAILED`; si responde `SENT` o `ACCEPTED`, el borrador pasa a `ISSUED`.

De esta forma, una lentitud o caída externa no retiene conexiones ni bloqueos de PostgreSQL y tampoco revierte la numeración ya asignada. Un cierre abrupto entre las fases deja un registro auditable en `SENDING`, recuperable mediante el mecanismo de reintentos controlados.

### Reintentos y errores recuperables

`POST /api/v1/electronic-documents/{id}/retry` vuelve a enviar un comprobante en `ERROR` o `PENDING_SEND` reutilizando exactamente el mismo ID, serie, correlativo, número e `idempotencyKey`. Nunca crea otro comprobante ni consume una nueva numeración.

Antes de llamar al proveedor, una transacción bloquea el registro y lo cambia a `SENDING`. Mientras ese intento tenga menos de cinco minutos, otra solicitud recibe `409 Conflict`; esto evita envíos concurrentes sin mantener una transacción abierta durante la llamada HTTP. Si el proceso se interrumpe y queda en `SENDING`, podrá recuperarse una vez cumplida esa ventana de seguridad.

Los fallos de comunicación se guardan como `ERROR` con código `PROVIDER_CALL_FAILED` y son recuperables. `ACCEPTED` y `REJECTED` son definitivos y no admiten reintento. Cada intento conserva la clave idempotente para que el proveedor pueda devolver el resultado original si llegó a procesar una solicitud cuya respuesta se perdió.

### Edición y cancelación del borrador

`PUT /api/v1/invoice-drafts/{id}` reemplaza receptor, tipo de comprobante, moneda e ítems mientras el borrador permanezca en `DRAFT`. El backend vuelve a resolver el receptor por DNI/RUC, valida la pertenencia a la empresa y recalcula descuentos, bases imponibles, IGV y totales; `companyId` no es editable.

`POST /api/v1/invoice-drafts/{id}/cancel` cambia un borrador `DRAFT` o `APPROVED` a `CANCELLED`. La transición es definitiva: un borrador cancelado no puede editarse, aprobarse ni emitirse. Si ya se asignó serie y correlativo, la cancelación se rechaza con `409 Conflict`, porque ese caso corresponde a la futura anulación de un comprobante electrónico y no a la cancelación de un borrador.

### Listados, búsqueda y paginación

Los listados paginados devuelven `content`, `page`, `size`, `totalElements`, `totalPages`, `first` y `last`. `page` comienza en cero y `size` admite valores entre 1 y 100. Todas las consultas respetan las empresas asociadas al usuario y, cuando corresponde, el header `X-Company-Id`.

- `GET /api/v1/companies/search`: filtros `query` (RUC, razón social o nombre comercial) y `active`.
- `GET /customers/search`: filtros `query` (DNI/RUC o nombre) y `active`.
- `GET /api/v1/invoice-drafts`: filtros `status` y `recipientDocumentNumber`.
- `GET /api/v1/electronic-documents`: filtros `status` y `documentNumber`.

Los borradores y comprobantes se ordenan del más reciente al más antiguo; empresas y clientes se ordenan alfabéticamente. Los filtros se ejecutan en PostgreSQL mediante consultas paginadas, no cargando todos los registros en memoria.

### Contrato tributario mínimo de NUBEFACT

`BillingSubmission` contiene los datos necesarios para construir la operación `generar_comprobante` del ejemplo oficial de NUBEFACT, manteniendo el puerto independiente del transporte HTTP:

- tipo de comprobante NUBEFACT (`1` factura, `2` boleta), serie y correlativo;
- transacción SUNAT `1`, fecha de emisión inmutable y moneda (`1` PEN, `2` USD);
- receptor con tipo NUBEFACT (`1` DNI, `6` RUC), denominación, dirección y correo;
- porcentaje de IGV, total gravado, exonerado, inafecto, descuentos, IGV y total;
- ítems con código estable, unidad, cantidad, valor unitario sin IGV, precio unitario con IGV, subtotal, descuento, IGV y total;
- tipo de IGV NUBEFACT (`1` gravado, `8` exonerado, `9` inafecto);
- envío automático a SUNAT activado, envío automático al cliente desactivado y `codigo_unico` representado por `idempotencyKey`.

La fecha de emisión se guarda en `ElectronicDocument.emissionAt` y no cambia durante los reintentos. La migración `V17__add_electronic_document_emission_date.sql` completa ese snapshot para bases existentes. El adaptador NUBEFACT real únicamente deberá serializar estos valores con los nombres JSON del proveedor y añadir URL/token de cada empresa.

### Notas de crédito, débito y anulaciones

Las notas son nuevos `ElectronicDocument` inmutables y numerados que referencian al comprobante original mediante tipo, serie y correlativo. Solo pueden generarse sobre facturas o boletas en estado `ACCEPTED`, pertenecientes a la empresa autenticada.

- `POST /api/v1/electronic-documents/{id}/credit-notes`: nota de crédito completa con motivos `01` a `10`.
- `POST /api/v1/electronic-documents/{id}/debit-notes`: nota de débito completa con motivos `01` intereses, `02` aumento de valor y `03` penalidades.
- `POST /api/v1/electronic-documents/{id}/cancel`: anulación mediante nota de crédito motivo `01`.

Cada operación conserva el snapshot del emisor, receptor, moneda, ítems e impuestos; genera IDs de ítem nuevos y usa una serie propia `CREDIT_NOTE` o `DEBIT_NOTE` con el mismo prefijo `F`/`B` del documento afectado. La combinación documento original, tipo de nota y motivo es idempotente. El comprobante original nunca se elimina ni se modifica.

La migración `V18__add_credit_debit_notes.sql` habilita los tipos `07`/`08`, referencias tributarias y restricciones de integridad. En esta etapa el proveedor mock acepta las notas; la comunicación real con NUBEFACT se realizará a través del mismo `BillingProvider`.

### Auditoría

Las operaciones de negocio dejan una bitácora inmutable y separada de las entidades operativas. Cada evento conserva la empresa, el usuario autenticado, la acción, el tipo e identificador del recurso, el resultado, un detalle funcional y la fecha UTC. Se auditan la creación de empresas y clientes, el onboarding tributario, la configuración de series; creación, edición, aprobación y cancelación de borradores; emisión y reintento de comprobantes; sincronización con el proveedor; notas de crédito, débito y anulaciones.

`GET /api/v1/audit-events` devuelve únicamente eventos de una empresa accesible para el usuario. Acepta `X-Company-Id` y los filtros opcionales `action`, `resourceType`, `resourceId`, `from`, `to`, `page` y `size`. Las fechas usan ISO-8601, por ejemplo `2026-09-01T00:00:00Z`.

La migración `V19__create_audit_events.sql` crea los índices de consulta y un trigger que rechaza cualquier `UPDATE` o `DELETE`, haciendo que la bitácora sea append-only incluso ante un acceso accidental desde la aplicación.

### Onboarding, roles y permisos

`POST /api/v1/onboarding` es el único registro público: crea de forma atómica el usuario propietario, su primera empresa y la membresía `OWNER`. Después de autenticarse, el propietario continúa con el perfil tributario y las series. El antiguo registro genérico `/api/auth/register` fue retirado para impedir usuarios sin empresa ni permisos definidos.

Los roles son por empresa, de modo que el mismo usuario puede tener responsabilidades diferentes en cada negocio:

- `OWNER`: control total, incluyendo propietarios y administradores.
- `ADMIN`: administración operativa y de miembros, pero no puede administrar membresías `OWNER`.
- `BILLING`: clientes, borradores, emisión, consulta y notas.
- `VIEWER`: consulta de empresas, borradores y comprobantes.

Los permisos se verifican en la capa de aplicación además de validar la pertenencia a la empresa. Una membresía puede desactivarse sin deshabilitar al usuario en sus otras empresas y nunca se permite retirar o desactivar al último propietario activo.

La administración utiliza `POST/GET /api/v1/companies/{companyId}/members`, `POST /api/v1/companies/{companyId}/members/existing`, `PATCH /api/v1/companies/{companyId}/members/{userId}` y `DELETE /api/v1/companies/{companyId}/members/{userId}`. Un usuario existente puede asociarse a varias empresas sin duplicar sus credenciales. Las altas reciben una contraseña inicial cifrada con BCrypt; las respuestas nunca exponen credenciales ni hashes. La migración `V20__add_company_roles_and_permissions.sql` actualiza las membresías existentes como `OWNER` y crea el rol global mínimo `ROLE_USER` para autenticación JWT.

### Conversation y Message

`Conversation` representa el hilo de comunicación de una empresa y puede vincularse opcionalmente con un cliente y un `InvoiceDraft`. Conserva el canal (`REST`, `WHATSAPP`, `WEB` o `INTERNAL`), el identificador externo del participante, estado `OPEN`/`CLOSED` y fechas de actividad. Las referencias a cliente y borrador se validan dentro de la misma empresa.

`Message` conserva dirección (`INBOUND`, `OUTBOUND`, `SYSTEM`), tipo (`TEXT`, `IMAGE`, `DOCUMENT`, `SYSTEM`), contenido o URL del medio, identificador externo, estado de entrega y fecha UTC. Una conversación cerrada no acepta mensajes. Las escrituras bloquean la conversación para evitar que un cierre y un mensaje concurrentes dejen un estado inconsistente.

Los endpoints disponibles son:

- `POST /api/v1/conversations` y `GET /api/v1/conversations` para crear y buscar conversaciones.
- `GET /api/v1/conversations/{id}` para consultar una conversación.
- `POST /api/v1/conversations/{id}/messages` y `GET /api/v1/conversations/{id}/messages` para agregar y paginar mensajes en orden cronológico.
- `POST /api/v1/conversations/{id}/close` para cerrar el hilo.

El acceso usa `CONVERSATION_READ` y `CONVERSATION_MANAGE`: `OWNER` y `ADMIN` poseen ambos; `BILLING` puede operar conversaciones; `VIEWER` solo consultarlas. La migración `V21__create_conversations_and_messages.sql` crea las tablas, claves foráneas, restricciones, índices y protección contra mensajes externos duplicados dentro de una conversación. En este punto solo existe el modelo y su API; la interpretación conversacional independiente del canal corresponde al punto 12.

### Envío y aceptación del proveedor

El comprobante mantiene un estado de entrega independiente: `PENDING_SEND`, `SENDING`, `SENT`, `ACCEPTED`, `REJECTED` o `ERROR`. También conserva la referencia, fechas de envío y respuesta, código y mensaje devueltos por el proveedor. Los datos fiscales permanecen inmutables mientras estos metadatos evolucionan.

`POST /api/v1/electronic-documents/{id}/refresh-status` consulta nuevamente un documento enviado. Los estados `ACCEPTED` y `REJECTED` son finales y no generan llamadas adicionales.

El puerto `BillingProvider` expone dos operaciones neutrales al proveedor:

- `submit(BillingSubmission)`, con emisor, receptor, comprobante, ítems e importes tributarios completos;
- `checkStatus(providerReference)`, para proveedores con procesamiento asíncrono.

La integración real debe implementar únicamente este puerto y mapear `BillingSubmission` al JSON externo. Se activa mediante `BILLING_PROVIDER`; mientras no exista el adaptador real se utiliza `mock`.

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

- Define el puerto de salida `BillingProvider`, que recibe un `BillingSubmission` construido desde el comprobante numerado y devuelve su resultado de envío.
- Implementa `MockBillingProvider`, un adaptador local determinista que genera referencias con el formato `MOCK-{fullNumber}`.
- Selecciona el proveedor mediante `BILLING_PROVIDER`; el valor predeterminado es `mock`.
- Añade el estado `ISSUED` y la transición `APPROVED → ISSUED`.
- Persiste `providerReference` e `issuedAt` mediante una migración Flyway incremental.
- Exige que todo borrador emitido tenga referencia y fecha del proveedor.
- Expone `POST /api/v1/invoice-drafts/{id}/issue` para ejecutar el flujo completo con el mock.
- Incluye pruebas del adaptador mock, del caso de uso, del dominio y del contrato HTTP.

La futura integración con el proveedor de facturación implementará el mismo puerto `BillingProvider`, permitiendo sustituir el mock sin modificar el dominio ni el controlador. Los apartados posteriores de este README describen las capacidades tributarias y de seguridad añadidas después de la versión inicial de este release.

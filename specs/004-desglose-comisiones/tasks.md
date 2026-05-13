# Tasks: AB-25 — Visualización y Desglose de Comisiones

**Input**: `specs/004-desglose-comisiones/` (plan.md, spec.md, data-model.md, contracts/commission-api.md, research.md)
**Module**: `order` — `com.accioneselbosque.order_service`
**Branch**: `004-desglose-comisiones`
**Prerequisito**: AB-24 (order existe; tabla `"order"` creada)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Preview de desglose de comisiones antes de confirmar orden (P1)

---

## Phase 1: Setup — Migración DB con seed

- [ ] T001 Crear migración `backend/order/src/main/resources/db/migration/V2__create_commission_rate_table.sql` — tabla `commission_rate` (id UUID PK, subscription_type VARCHAR(20) UNIQUE CHECK('STANDARD','PREMIUM'), rate_percent DECIMAL(5,2) NOT NULL CHECK > 0); INSERT seed: STANDARD=1.50, PREMIUM=0.80

**Checkpoint Setup**: `./mvnw flyway:migrate` aplica V2 con los 2 registros seed.

---

## Phase 2: Foundational — Modelo y repositorio

- [ ] T002 [P] Crear enum `SubscriptionType` en `backend/order/src/main/java/com/accioneselbosque/order_service/model/SubscriptionType.java` — valores: `STANDARD`, `PREMIUM`
- [ ] T003 Crear entidad JPA `CommissionRate` en `backend/order/src/main/java/com/accioneselbosque/order_service/model/CommissionRate.java` — id (UUID), subscriptionType (@Enumerated STRING, UNIQUE), ratePercent (BigDecimal) (depende de T002)
- [ ] T004 Crear `CommissionRateRepository` en `backend/order/src/main/java/com/accioneselbosque/order_service/repository/CommissionRateRepository.java` — `findBySubscriptionType(SubscriptionType)` retorna `Optional<CommissionRate>`
- [ ] T005 Crear excepción `CommissionRateNotFoundException` en `exception/` y registrarla en el `GlobalExceptionHandler` existente (→ 500 si las tasas seed no existen en DB)

**Checkpoint Foundational**: `CommissionRate` mapeado; `findBySubscriptionType(STANDARD)` retorna 1.50.

---

## Phase 3: User Story 1 — Preview de comisiones (P1) 🎯 MVP

**Goal**: `POST /orders/preview` calcula y retorna el desglose completo (grossValue, commissionAmount, netTotal) según la suscripción del investor. No persiste nada.

**Independent Test**: `POST /orders/preview` con STANDARD → commissionAmount = quantity × unitPrice × 0.015 (exacto, BigDecimal). Con PREMIUM → × 0.008. BUY: netTotal = grossValue + commission. SELL: netTotal = grossValue - commission.

### Tests — US1

- [ ] T006 [P] [US1] Escribir `OrderPreviewControllerTest` en `src/test/java/.../controller/OrderPreviewControllerTest.java` con `@WebMvcTest`: test STANDARD BUY 10×39500 → ratePercent=1.50, commissionAmount=5925.00, netTotal=400925.00; test PREMIUM BUY mismos datos → commissionAmount=3160.00, netTotal=398160.00; test SELL → netTotal=grossValue-commission
- [ ] T007 [P] [US1] Escribir `CommissionCalculatorServiceTest` en `src/test/java/.../service/CommissionCalculatorServiceTest.java` — test cálculo exacto con BigDecimal HALF_UP; test BUY vs SELL formula; test que STANDARD y PREMIUM usan tasas de DB y no valores hard-coded

### Implementación — US1

- [ ] T008 [P] [US1] Crear `OrderPreviewRequest` DTO en `dto/OrderPreviewRequest.java` — symbol (@NotBlank), quantity (@Positive int), orderType (@NotBlank), unitPrice (@Positive @DecimalMin("0.01") BigDecimal)
- [ ] T009 [P] [US1] Crear `OrderPreviewResponse` DTO en `dto/OrderPreviewResponse.java` — symbol, quantity, orderType, unitPrice, grossValue, subscriptionType, ratePercent, commissionAmount, netTotal (todos BigDecimal con escala 2)
- [ ] T010 [US1] Implementar `CommissionCalculatorService` en `service/CommissionCalculatorService.java` — método `calculate(OrderPreviewRequest, SubscriptionType)`: (1) cargar tasa de `CommissionRateRepository.findBySubscriptionType()`; (2) `grossValue = qty × unitPrice`; (3) `commissionAmount = grossValue × (ratePercent/100)` con `RoundingMode.HALF_UP` escala 2; (4) BUY: `netTotal = grossValue + commissionAmount`, SELL: `netTotal = grossValue - commissionAmount`; retornar `OrderPreviewResponse` (depende de T004)
- [ ] T011 [US1] Implementar `OrderPreviewController` en `controller/OrderPreviewController.java` con `POST /orders/preview` — extraer `subscriptionType` del JWT claim; `@Valid @RequestBody OrderPreviewRequest`; delegar a `CommissionCalculatorService.calculate()`; retornar 200 con `OrderPreviewResponse` (depende de T010)

**Checkpoint US1**: Tests T006–T007 pasan. Cálculos exactos con BigDecimal; anti-tampering garantizado por recálculo en `OrderService.createOrder()` (módulo AB-24).

---

## Phase 4: Polish

- [ ] T012 Verificar que `CommissionCalculatorService` es el único lugar donde viven las fórmulas — `OrderService.createOrder()` (AB-24) debe inyectar y reutilizar este mismo servicio en vez de recalcular con lógica propia
- [ ] T013 [P] Ejecutar suite: `./mvnw test -pl backend/order`

---

## Dependencias clave

- T003 (CommissionRate entity) → depende de T001 (V2 migration) y T002 (SubscriptionType enum)
- T010 (CommissionCalculatorService) → depende de T004 (CommissionRateRepository)
- T011 (OrderPreviewController) → extrae `subscriptionType` del JWT; requiere que el claim esté presente (configurado en AB-15 SecurityConfig)
- `OrderService.createOrder()` (AB-24, T011) debe inyectar `CommissionCalculatorService` de este módulo in-process para garantizar anti-tampering

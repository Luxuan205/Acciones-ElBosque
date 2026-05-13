# Research: AB-15 — Registro de Inversionista

**Phase 0 output** | **Date**: 2026-05-10

## Decisión 1: Hash de contraseñas

**Decision**: BCryptPasswordEncoder (factor de costo 12)
**Rationale**: Spring Security lo provee out-of-the-box; es el estándar de la industria
para Java; el módulo ya tiene `spring-boot-starter-security` como dependencia.
**Alternatives considered**: Argon2 (más moderno pero requiere dependencia externa),
PBKDF2 (más débil que BCrypt al mismo costo computacional).

---

## Decisión 2: Generación del token de verificación

**Decision**: UUID v4 generado con `java.util.UUID.randomUUID()`, almacenado en DB
con TTL de 24 horas calculado como `LocalDateTime.now().plusHours(24)`.
**Rationale**: UUID v4 es suficientemente aleatorio (122 bits de entropía) para un
token de uso único; sin dependencias externas; fácil de validar por expiración con
una consulta simple (`WHERE token = ? AND expires_at > NOW() AND used = false`).
**Alternatives considered**: JWT firmado como token de verificación (overhead
innecesario para un token de un solo uso), token numérico de 6 dígitos (demasiado
corto, susceptible a fuerza bruta).

---

## Decisión 3: Envío de correo electrónico

**Decision**: Spring Boot Mail (`spring-boot-starter-mail`) con plantilla HTML simple.
El envío se hace de forma síncrona en el servicio para simplificar el flujo en esta
versión académica.
**Rationale**: Dependencia ya compatible con Spring Boot 4.x; configuración via
`application.yaml` con variables de entorno (sin hardcodear credenciales).
**Alternatives considered**: Envío asíncrono con `@Async` o cola de mensajes (mejor
para producción, pero añade complejidad innecesaria para el alcance académico).
**Nota**: Para producción real se recomendaría envío asíncrono.

---

## Decisión 4: Validación de número de documento

**Decision**: Regex `^\d{6,10}$` aplicado con Bean Validation (`@Pattern`) en el DTO.
**Rationale**: Cédula colombiana es numérica, entre 6 y 10 dígitos. Sin dígito
verificador estándar públicamente definido — validación de formato es suficiente.
**Alternatives considered**: Validación de dígito verificador (no estandarizada),
integración con RNEC (fuera de alcance académico).

---

## Decisión 5: Estructura de paquetes

**Decision**: Layered architecture plana dentro de `auth-security-service`:
`controller` → `service` → `repository` → `model`.
**Rationale**: Para un módulo de tamaño pequeño (registro + verificación), la
arquitectura por capas es directa y comprensible para el equipo.
**Alternatives considered**: Arquitectura hexagonal (más flexible pero añade
abstracciones que no se justifican para el tamaño del módulo).

---

## Decisión 6: Esquema de base de datos

**Decision**: Schema dedicado `auth_db` en PostgreSQL, gestionado con Flyway.
**Rationale**: Ya configurado en `application.yaml`; Flyway garantiza migraciones
reproducibles y versionadas; schema separado aísla los datos de autenticación.
**Alternatives considered**: Schema compartido con otros módulos (viola Principio I
de cohesión del módulo), Liquibase (ambos son válidos, Flyway ya configurado).

---

## Dependencia adicional requerida

`spring-boot-starter-mail` debe agregarse al `pom.xml` de `auth-security-service`.
No está presente en la versión actual del pom.

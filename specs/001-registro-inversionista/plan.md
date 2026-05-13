# Implementation Plan: AB-15 — Registro de Inversionista en la Plataforma

**Branch**: `001-registro-inversionista` | **Date**: 2026-05-10 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `specs/001-registro-inversionista/spec.md`

## Summary

Permitir que un visitante no autenticado se registre como inversionista proporcionando
datos personales y credenciales. El sistema valida duplicados, almacena la cuenta en
estado pendiente y envía un correo con enlace de verificación único con vigencia de 24h.
La cuenta se activa solo tras la verificación del correo. Todo vive en el módulo
`auth-security-service` (bounded context de autenticación).

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Security 6.x, Spring Data JPA,
  Lombok, Spring Boot Validation, Spring Boot Mail (para envío de correo)
**Storage**: PostgreSQL — schema `auth_db` (gestionado con Flyway)
**Testing**: JUnit 5 + Spring Boot Test slices (`@WebMvcTest`, `@DataJpaTest`,
  `@SpringBootTest`), Mockito, GreenMail (SMTP embebido para tests de email)
**Target Platform**: Web application backend — REST API consumida por frontend
**Project Type**: Módulo del monolito modular (`auth-security-service`)
**Performance Goals**: Registro completado en < 3 minutos (SC-001 del spec);
  correo de verificación entregado en < 60 segundos (SC-003)
**Constraints**: Sin secretos en repositorio (env vars via `application.yaml`);
  SonarCloud gate activo; tokens de verificación con TTL 24h
**Scale/Scope**: Proyecto académico — equipo de 4 desarrolladores; carga esperada baja

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Requisito | Estado | Evidencia |
|-----------|-----------|--------|-----------|
| I. Module Cohesion | Registro vive íntegramente en `auth-security-service`; cero llamadas HTTP cross-módulo durante el flujo de registro | ✅ PASS | Todo el dominio (Investor, VerificationToken, RegistrationService) está en `com.accioneselbosque.auth` |
| II. API Contract-First | Contratos REST definidos antes de implementar el frontend | ✅ PASS | Ver `contracts/registration-api.md` (Phase 1) |
| III. Test-Before-Ship | Tests unitarios e integración requeridos antes del merge | ✅ PASS | Plan incluye test tasks explícitas — deben fallar antes de implementar |
| IV. Security & Compliance | Contraseñas hasheadas con BCrypt; no se almacenan en texto plano; tokens de verificación son únicos y con TTL; registro en audit-compliance al crear cuenta | ✅ PASS | Spring Security provee BCrypt; `VerificationToken` tiene campo `expiresAt`; evento de auditoría emitido post-registro |
| V. Conventional Workflow | Rama sigue convención `feature/HU-XX`; commits en Conventional Commits | ✅ PASS | Rama `001-registro-inversionista` — nomenclatura acordada por el equipo |

**Resultado: GATE PASSED — sin violaciones. No se requiere justificación de complejidad.**

## Project Structure

### Documentation (this feature)

```text
specs/001-registro-inversionista/
├── plan.md              ← este archivo
├── research.md          ← decisiones técnicas
├── data-model.md        ← entidades y esquema DB
├── quickstart.md        ← cómo probar el flujo
├── contracts/
│   └── registration-api.md   ← contratos REST
└── tasks.md             ← generado por /speckit-tasks
```

### Source Code (módulo auth-security-service)

```text
backend/auth/
├── src/main/java/com/accioneselbosque/auth_security_service/
│   ├── AuthSecurityServiceApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java          ← permite POST /auth/register sin auth
│   │   └── MailConfig.java              ← configuración SMTP
│   ├── controller/
│   │   └── RegistrationController.java  ← POST /auth/register, GET /auth/verify,
│   │                                       POST /auth/resend-verification
│   ├── service/
│   │   ├── RegistrationService.java     ← lógica de negocio de registro
│   │   └── VerificationTokenService.java ← generación y validación de tokens
│   ├── model/
│   │   ├── Investor.java                ← entidad JPA
│   │   └── VerificationToken.java       ← entidad JPA
│   ├── repository/
│   │   ├── InvestorRepository.java
│   │   └── VerificationTokenRepository.java
│   ├── dto/
│   │   ├── RegisterRequest.java         ← request body con validaciones Bean Validation
│   │   └── RegisterResponse.java
│   └── exception/
│       ├── DuplicateEmailException.java
│       ├── DuplicateDocumentException.java
│       └── TokenExpiredException.java
├── src/main/resources/
│   ├── application.yaml
│   ├── db/migration/
│   │   ├── V1__create_investor_table.sql
│   │   └── V2__create_verification_token_table.sql
│   └── templates/email/
│       └── verification-email.html      ← plantilla del correo
└── src/test/java/com/accioneselbosque/auth_security_service/
    ├── controller/
    │   └── RegistrationControllerTest.java   ← @WebMvcTest
    ├── service/
    │   └── RegistrationServiceTest.java      ← @ExtendWith(MockitoExtension)
    └── repository/
        └── InvestorRepositoryTest.java       ← @DataJpaTest
```

**Structure Decision**: Módulo único `auth-security-service` con layered architecture
(Controller → Service → Repository). Sin llamadas a otros módulos durante el registro;
el envío de email se hace dentro del mismo módulo usando Spring Mail.

## Complexity Tracking

> No hay violaciones de constitución — sección vacía.

# Implementation Plan: AB-16 — Inicio de Sesión con MFA

**Branch**: `012-inicio-sesion-mfa` | **Date**: 2026-05-13 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/012-inicio-sesion-mfa/spec.md`

## Summary

Autenticación en dos factores para inversionistas: primer factor email+contraseña, segundo factor OTP
de un solo uso enviado por correo (o TOTP). El módulo `auth` gestiona la sesión de pre-autenticación,
el bloqueo por intentos fallidos y la emisión del JWT final solo tras ambos factores validados.
Se añaden los estados SUSPENDED y BLOCKED al enum AccountStatus.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Security 6.x, Spring Data JPA, Lombok,
  jjwt 0.12.6, Spring Boot Mail
**Storage**: PostgreSQL — migraciones Flyway en `app/src/main/resources/db/migration/` (V12–V15)
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito, GreenMail (SMTP embebido)
**Target Platform**: REST API backend
**Project Type**: Módulo `auth`
**Performance Goals**: Flujo completo < 60 segundos (SC-001); OTP válido máx 5 minutos (SC-002)
**Constraints**: Mensajes de error genéricos; OTP single-use; bloqueo tras 5 intentos fallidos (configurable)
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Login y MFA íntegramente en `auth`; cero HTTP cross-módulo | ✅ PASS |
| II. API Contract-First | `contracts/login-api.md` define POST /auth/login y POST /auth/mfa/verify | ✅ PASS |
| III. Test-Before-Ship | Tests: login correcto, OTP incorrecto, cuenta bloqueada, OTP expirado | ✅ PASS |
| IV. Security & Compliance | Mensajes genéricos; OTP single-use con TTL; bloqueo automático; auditoría de cada intento | ✅ PASS |
| V. Conventional Workflow | Rama `012-inicio-sesion-mfa` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Documentation (this feature)

```text
specs/012-inicio-sesion-mfa/
├── plan.md
├── research.md
├── data-model.md
├── contracts/
│   └── login-api.md
└── tasks.md             ← /speckit-tasks
```

### Source Code (auth)

```text
backend/auth/src/main/java/com/accioneselbosque/auth/
├── controller/
│   └── LoginController.java        ← POST /auth/login, POST /auth/mfa/verify, POST /auth/mfa/resend
├── service/
│   ├── LoginService.java           ← validar credenciales, bloqueo, sesión pre-auth
│   ├── MfaService.java             ← generar OTP, validar OTP, reenvío
│   └── JwtService.java             ← emitir JWT con rol del usuario
├── model/
│   ├── AccountStatus.java          ← AÑADIR: SUSPENDED, BLOCKED
│   ├── OtpCode.java                ← código OTP con TTL y estado usado/no-usado
│   └── MfaSession.java             ← sesión temporal entre factor 1 y factor 2
├── dto/
│   ├── LoginRequest.java
│   ├── LoginResponse.java          ← { sessionToken } (pre-auth) o { accessToken } (si no hay MFA)
│   ├── MfaVerifyRequest.java
│   └── MfaVerifyResponse.java      ← { accessToken, role }
└── repository/
    ├── OtpCodeRepository.java
    └── MfaSessionRepository.java

app/src/main/resources/db/migration/
├── V12__add_account_status_suspended_blocked.sql   ← ADD VALUE a enum o varchar
├── V13__add_investor_failed_attempts.sql           ← failed_attempts, locked_until
├── V14__create_otp_code_table.sql
└── V15__create_mfa_session_table.sql
```

**Structure Decision**: `MfaSession` modela el estado entre el factor 1 aprobado y el factor 2 pendiente
(TTL propio). `OtpCode` es single-use con campo `usedAt`. El bloqueo se almacena en `Investor` con
`failedAttempts` y `lockedUntil`; el desbloqueo manual por ADMIN solo limpia esos campos.

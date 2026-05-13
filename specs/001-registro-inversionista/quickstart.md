# Quickstart: AB-15 — Registro de Inversionista

**Cómo verificar que la funcionalidad funciona end-to-end**

## Prerequisitos

1. PostgreSQL corriendo con la base de datos y schema `auth_db` creados (Flyway los crea automáticamente al arrancar).
2. Servidor SMTP configurado (o usar MailHog / Mailtrap para desarrollo).
3. `auth-security-service` corriendo en `http://localhost:8080`.
4. Variables de entorno configuradas (ver `application.yaml`).

## Variables de entorno requeridas

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/acciones_elbosque
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<password>
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=test@test.com
MAIL_PASSWORD=
APP_BASE_URL=http://localhost:8080
```

## Flujo 1: Registro exitoso + verificación

### Paso 1 — Registrar inversionista

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Juan Pérez",
    "documentNumber": "1234567890",
    "email": "juan@ejemplo.com",
    "password": "Segura123",
    "confirmPassword": "Segura123"
  }'
```

**Respuesta esperada (HTTP 201)**:
```json
{
  "message": "Registro exitoso. Revisa tu correo para verificar tu cuenta.",
  "email": "juan@ejemplo.com"
}
```

### Paso 2 — Verificar en DB que el estado es PENDING

```sql
SELECT id, full_name, email, account_status FROM auth_db.investor
WHERE email = 'juan@ejemplo.com';
-- account_status debe ser 'PENDING'
```

### Paso 3 — Obtener el token de verificación (en dev, buscar en DB o revisar MailHog)

```sql
SELECT token, expires_at, used FROM auth_db.verification_token
WHERE investor_id = (SELECT id FROM auth_db.investor WHERE email = 'juan@ejemplo.com');
```

### Paso 4 — Verificar cuenta con el token

```bash
curl -X GET "http://localhost:8080/auth/verify?token=<TOKEN-OBTENIDO>"
```

**Respuesta esperada (HTTP 200)**:
```json
{
  "message": "Cuenta verificada exitosamente. Ya puedes iniciar sesión."
}
```

### Paso 5 — Confirmar en DB que el estado es ACTIVE

```sql
SELECT account_status FROM auth_db.investor WHERE email = 'juan@ejemplo.com';
-- account_status debe ser 'ACTIVE'
```

---

## Flujo 2: Registro con correo duplicado

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Otro Usuario",
    "documentNumber": "9876543210",
    "email": "juan@ejemplo.com",
    "password": "Segura123",
    "confirmPassword": "Segura123"
  }'
```

**Respuesta esperada (HTTP 409)**:
```json
{
  "error": "El correo ya está registrado. ¿Desea iniciar sesión?"
}
```

---

## Flujo 3: Reenvío de correo de verificación

```bash
curl -X POST http://localhost:8080/auth/resend-verification \
  -H "Content-Type: application/json" \
  -d '{ "email": "juan@ejemplo.com" }'
```

**Respuesta esperada (HTTP 200)**:
```json
{
  "message": "Correo de verificación reenviado."
}
```

---

## Flujo 4: Token expirado

Modificar manualmente el `expires_at` del token en DB para que esté en el pasado
y usar ese token en el endpoint de verificación.

**Respuesta esperada (HTTP 400)**:
```json
{
  "error": "El enlace de verificación ha expirado. Solicita uno nuevo."
}
```

---

## Ejecutar los tests

```bash
cd backend/auth
./mvnw test
```

Los tests deben cubrir: registro exitoso, duplicado de correo, duplicado de documento,
contraseña débil, verificación exitosa, token expirado, reenvío de correo.

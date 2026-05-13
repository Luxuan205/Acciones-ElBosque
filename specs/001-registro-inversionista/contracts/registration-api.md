# API Contract: Registro de Inversionista

**Módulo**: `auth-security-service` | **Base URL**: `http://localhost:8080`
**Autenticación requerida**: No (endpoints públicos)

---

## POST /auth/register

Registra un nuevo inversionista en la plataforma.

### Request

**Content-Type**: `application/json`

```json
{
  "fullName":        "string (1–150 chars, requerido)",
  "documentNumber":  "string (6–10 dígitos numéricos, requerido)",
  "email":           "string (formato email válido, requerido)",
  "password":        "string (min 8 chars, 1 mayúscula, 1 minúscula, 1 número, requerido)",
  "confirmPassword": "string (debe coincidir con password, requerido)"
}
```

**Ejemplo**:
```json
{
  "fullName": "Juan Diego González",
  "documentNumber": "1020304050",
  "email": "juan@ejemplo.com",
  "password": "Segura123",
  "confirmPassword": "Segura123"
}
```

### Responses

#### 201 Created — Registro exitoso

```json
{
  "message": "Registro exitoso. Revisa tu correo para verificar tu cuenta.",
  "email": "juan@ejemplo.com"
}
```

#### 400 Bad Request — Datos inválidos

```json
{
  "error": "Datos de registro inválidos",
  "details": [
    "El nombre completo es requerido",
    "La contraseña debe tener al menos 8 caracteres",
    "Las contraseñas no coinciden"
  ]
}
```

#### 409 Conflict — Correo ya registrado

```json
{
  "error": "El correo ya está registrado. ¿Desea iniciar sesión?"
}
```

#### 409 Conflict — Documento ya registrado

```json
{
  "error": "El número de documento ya tiene una cuenta asociada."
}
```

---

## GET /auth/verify

Verifica la cuenta del inversionista usando el token enviado por correo.

### Request

**Query parameter**: `token` (string, UUID v4, requerido)

**Ejemplo**: `GET /auth/verify?token=550e8400-e29b-41d4-a716-446655440000`

### Responses

#### 200 OK — Verificación exitosa

```json
{
  "message": "Cuenta verificada exitosamente. Ya puedes iniciar sesión."
}
```

#### 400 Bad Request — Token expirado

```json
{
  "error": "El enlace de verificación ha expirado. Solicita uno nuevo."
}
```

#### 404 Not Found — Token inválido o no existe

```json
{
  "error": "El enlace de verificación no es válido."
}
```

#### 409 Conflict — Token ya utilizado

```json
{
  "error": "Este enlace ya fue usado. La cuenta puede estar activa; intenta iniciar sesión."
}
```

---

## POST /auth/resend-verification

Reenvía el correo de verificación a un inversionista en estado PENDING.

### Request

**Content-Type**: `application/json`

```json
{
  "email": "string (formato email válido, requerido)"
}
```

### Responses

#### 200 OK — Correo reenviado

```json
{
  "message": "Correo de verificación reenviado. Revisa tu bandeja de entrada."
}
```

#### 400 Bad Request — Cuenta ya activa

```json
{
  "error": "Esta cuenta ya está verificada. Puedes iniciar sesión."
}
```

#### 404 Not Found — Email no registrado

> **Nota de seguridad**: Por razones de privacidad, no se confirma si el email
> existe o no. Se devuelve siempre 200 para evitar enumeración de usuarios.

```json
{
  "message": "Si el correo está registrado y pendiente de verificación, recibirás un nuevo enlace."
}
```

#### 429 Too Many Requests — Reenvío muy frecuente

```json
{
  "error": "Debes esperar al menos 2 minutos antes de solicitar otro correo de verificación."
}
```

---

## Notas de implementación para el frontend

- El endpoint `POST /auth/register` siempre devuelve el mismo mensaje de éxito sin
  revelar si el correo existía previamente (solo el 409 lo indica).
- El endpoint `POST /auth/resend-verification` siempre devuelve 200 para el caso de
  email no encontrado (anti-enumeración).
- Los errores 400 incluyen el array `details` con mensajes específicos por campo
  que el frontend puede mapear a los campos del formulario.

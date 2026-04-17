# Acciones-ElBosque
Este es un repositorio para el proyecto final de ingeniería de software 2.

**Equipo:** Juan González · Dylan Hospital · José David Buitrago · David Salazar

---

## Estrategia de Ramas

###  Estructura

| Rama | Sale de | Propósito |
|---|---|---|
| `main` | — | Código estable y entregable. Solo se actualiza al cierre de cada sprint. |
| `develop` | `main` | Rama de integración diaria del equipo. Debe compilar siempre. |
| `feature/xxx` | `develop` | Una rama por Historia de Usuario o tarea. Vuelve a develop vía PR. |
| `docs/xxx` | `develop` | Cambios exclusivos de documentación, diagramas o archivos de análisis. |
| `fix/xxx` | `develop` | Corrección de bugs encontrados durante el sprint. |
| `hotfix/xxx` | `main` | Bug crítico en main. Mergea tanto a main como a develop. |

###  Reglas de ramas

- Nunca hacer push directo a `main` ni a `develop`.
- Crear la rama desde `develop` actualizado (`git pull` antes).
- Una rama = una tarea. No mezclar cambios no relacionados.
- Eliminar la rama después de hacer merge exitoso.
- El nombre debe ser descriptivo, en minúsculas y con guiones.

### Ejemplos de nombres válidos

```
feature/HU-03-iniciar-sesion
feature/HU-07-ver-portafolio
docs/diagramas-uml-componentes
fix/error-token-expirado
hotfix/login-produccion-caido
```

---

## Política de Commits

### Formato — Conventional Commits

```
<tipo>(<alcance>): <descripción corta>
```

La descripción debe estar en **infinitivo, minúscula y sin punto final**. Máximo **72 caracteres** en la primera línea.

### Tipos permitidos

| Tipo | Cuándo usarlo |
|---|---|
| `feat` | Nueva funcionalidad o Historia de Usuario implementada. |
| `fix` | Corrección de un bug o comportamiento incorrecto. |
| `docs` | Cambios en documentación, diagramas, README o archivos de análisis. |
| `refactor` | Reestructuración de código sin cambiar funcionalidad. |
| `test` | Agregar o modificar pruebas unitarias o de integración. |
| `chore` | Configuración, dependencias, pipelines, archivos de entorno. |
| `style` | Formato, espacios, puntos y coma. Sin cambio de lógica. |

### Ejemplos válidos

```
feat(auth): agregar login con JWT
fix(portafolio): corregir cálculo de ganancia neta
docs(diagramas): agregar diagrama de componentes UML
refactor(orders): extraer servicio de ejecución de órdenes
chore(ci): configurar GitHub Actions con SonarCloud
test(auth): agregar pruebas unitarias al módulo de login
```

### Reglas de oro

- Un commit = una sola cosa. No mezclar `feat` con `fix` en el mismo commit.
- Descripción en infinitivo y minúscula: `agregar`, `corregir`, `extraer`.
- Máximo 72 caracteres en la primera línea.
- Si el cambio es complejo, agregar cuerpo explicativo dejando una línea en blanco después del título.
- Referenciar el issue o HU cuando aplique: `Closes #12`, `Refs #7`.

---

## Pull Requests

### Reglas

- Nadie aprueba su propio PR. Siempre requiere revisión de otro integrante.
- El PR debe tener un título descriptivo siguiendo el mismo formato de commits.
- Referenciar el issue o HU que resuelve: `Closes #12`.
- El código debe compilar y probarse localmente antes de abrir el PR.
- Si hay conflictos, el autor del PR los resuelve antes de pedir revisión.

### Checklist obligatorio en cada PR

- [ ] El código compila sin errores.
- [ ] Se probó localmente el cambio.
- [ ] Se actualizó la documentación si el cambio lo requiere.
- [ ] No se subieron archivos sensibles (`.env`, credenciales, API keys).
- [ ] El nombre de la rama sigue la convención definida.

---


*Acordado por el equipo de desarrollo — Sprint 1 · 2026*  
*Juan González · Dylan Hospital · José David Buitrago · David Salazar*
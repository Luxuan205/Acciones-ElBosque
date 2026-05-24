# Frontend Revamp — Editorial Almanac Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the dark-forest visual skin of the Angular 18 app with the Editorial Almanac design — warm cream paper, softened forest green, amber accent, editorial serif typography. Zero business logic changes.

**Architecture:** Token-first — replace all CSS custom properties in `styles.scss` in one pass, then restyle each component HTML/SCSS screen by screen. No new services, routes, guards, or models. ApexCharts color configs in `dashboard.component.ts` are the only `.ts` changes allowed.

**Tech Stack:** Angular 18 standalone components, SCSS with BEM, `@if`/`@for` control flow, signals, Google Fonts (Instrument Serif + Geist + JetBrains Mono), ApexCharts via `ng-apexcharts`.

---

## File Map

| Status | File |
|--------|------|
| Modify | `frontend/src/styles.scss` |
| Modify | `frontend/src/app/shared/components/layout/layout.component.scss` |
| Modify | `frontend/src/app/shared/components/navbar/navbar.component.html` |
| Modify | `frontend/src/app/shared/components/navbar/navbar.component.scss` |
| Modify | `frontend/src/app/features/auth/login/login.component.html` |
| Modify | `frontend/src/app/features/auth/login/login.component.scss` |
| Modify | `frontend/src/app/features/auth/register/register.component.html` |
| Modify | `frontend/src/app/features/auth/register/register.component.scss` |
| Modify | `frontend/src/app/features/dashboard/dashboard.component.html` |
| Modify | `frontend/src/app/features/dashboard/dashboard.component.scss` |
| Modify | `frontend/src/app/features/dashboard/dashboard.component.ts` (ApexCharts config only) |
| Modify | `frontend/src/app/features/market/market-list/market-list.component.html` |
| Modify | `frontend/src/app/features/market/market-list/market-list.component.scss` |
| Modify | `frontend/src/app/features/portfolio/positions/positions.component.html` |
| Modify | `frontend/src/app/features/portfolio/positions/positions.component.scss` |
| Modify | `frontend/src/app/features/orders/buy/buy.component.html` |
| Modify | `frontend/src/app/features/orders/buy/buy.component.scss` |
| Modify | `frontend/src/app/features/orders/sell/sell.component.html` |
| Modify | `frontend/src/app/features/orders/sell/sell.component.scss` |
| Modify | `frontend/src/app/features/orders/history/order-history.component.html` |
| Modify | `frontend/src/app/features/orders/history/order-history.component.scss` |

**Do NOT touch:** `core/services/**`, `app.routes.ts`, `core/guards/**`, `core/models/**`, any `.ts` except `dashboard.component.ts`.

---

## Task 1: Global tokens — `styles.scss`

**Files:**
- Modify: `frontend/src/styles.scss`

- [ ] **Step 1: Replace the Google Fonts import and `:root` block**

Replace the entire file from line 1 through the closing `}` of the first `:root` block (lines 1–75) with:

```scss
@import url('https://fonts.googleapis.com/css2?family=Instrument+Serif:ital@0;1&family=Geist:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap');

// ============================================================
// Acciones El Bosque — Editorial Almanac Design System
// Universidad El Bosque
// ============================================================

:root {
  // Surfaces
  --paper:           #FAF6EC;
  --paper-warmer:    #F4ECDB;
  --card:            #FFFCF5;
  --line:            #E6DDC6;
  --line-soft:       #EFE7D0;

  // Ink
  --ink:             #1F2A1C;
  --ink-2:           #4B5645;
  --ink-3:           #7E8A77;
  --ink-4:           #A8B0A1;

  // Brand — Moss
  --moss:            #3F7A4E;
  --moss-deep:       #2F5C3A;
  --moss-tint:       #E6F0E2;
  --moss-line:       #C6DCC1;

  // Brand — Amber
  --amber:           #D89154;
  --amber-deep:      #B5743C;
  --amber-tint:      #FBE8CF;
  --amber-line:      #F0D2A2;

  // Semantic
  --berry:           #B5453C;
  --berry-tint:      #F8DCD8;
  --sky:             #5B7D9A;

  // Typography
  --serif: 'Instrument Serif', Georgia, serif;
  --sans:  'Geist', system-ui, sans-serif;
  --mono:  'JetBrains Mono', Consolas, monospace;

  // Radii
  --r-sm: 6px;
  --r:    10px;
  --r-lg: 18px;
  --r-xl: 28px;

  // Shadows (warm-tinted, light)
  --sh-1: 0 1px 0 rgba(32,40,24,0.04), 0 2px 8px rgba(32,40,24,0.04);
  --sh-2: 0 1px 0 rgba(32,40,24,0.05), 0 8px 24px rgba(32,40,24,0.06);
  --sh-3: 0 12px 40px rgba(32,40,24,0.10);

  // Transitions
  --t-fast: 0.12s ease;
  --t-base: 0.2s ease;
  --t-slow: 0.35s ease;
  --transition: var(--t-base);
}
```

- [ ] **Step 2: Update the second `:root` block (type scale, line 127–138) and `body`**

Replace the second `:root` block and `body` rule:

```scss
:root {
  --fs-3xl:  2rem;
  --fs-2xl:  1.625rem;
  --fs-xl:   1.25rem;
  --fs-lg:   1rem;
  --fs-md:   0.875rem;
  --fs-sm:   0.75rem;
  --fs-xs:   0.6875rem;

  --ff-sans: var(--sans);
  --ff-mono: var(--mono);
  --ff-serif: var(--serif);
}

body {
  font-family: var(--sans);
  background-color: var(--paper);
  color: var(--ink);
  line-height: 1.6;
  min-height: 100vh;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
```

Remove the `background-image: radial-gradient(...)` lines from the old `body` rule. Keep the `a`, `img`, `ul`, `button/input/select/textarea` resets unchanged.

- [ ] **Step 3: Update heading and text defaults**

Replace the heading block (h1–h6) color references:

```scss
h1, h2, h3, h4, h5, h6 {
  font-family: var(--sans);
  font-weight: 700;
  line-height: 1.2;
  color: var(--ink);
  letter-spacing: -0.015em;
}

p {
  color: var(--ink-2);
  margin-bottom: 0.75rem;
  line-height: 1.7;
  max-width: 72ch;
  &:last-child { margin-bottom: 0; }
}

small { font-size: 0.75rem; color: var(--ink-3); }

code, pre {
  font-family: var(--mono);
  font-size: 0.8125rem;
  background: var(--moss-tint);
  border-radius: var(--r-sm);
  padding: 0.15em 0.45em;
  color: var(--moss-deep);
}
```

- [ ] **Step 4: Replace `.card`**

```scss
.card {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  padding: 1.5rem;
  box-shadow: var(--sh-1);
  transition: border-color 200ms ease, box-shadow 200ms ease;
  position: relative;

  &:hover {
    border-color: rgba(0,0,0,0.12);
    box-shadow: var(--sh-2);
  }

  &__title {
    font-size: 0.72rem;
    font-weight: 600;
    color: var(--ink-3);
    text-transform: uppercase;
    letter-spacing: 0.08em;
    margin-bottom: 0.6rem;

    &--lg {
      font-size: 1rem;
      color: var(--ink);
      text-transform: none;
      letter-spacing: normal;
    }
  }

  &__value {
    font-size: 1.85rem;
    font-weight: 700;
    color: var(--ink);
    letter-spacing: -0.02em;
    font-variant-numeric: tabular-nums;
    font-family: var(--mono);

    &--sm { font-size: 0.9rem; letter-spacing: normal; }
  }

  &__subtitle {
    font-size: 0.82rem;
    color: var(--ink-3);
    margin-top: 0.2rem;
  }
}

.card--flush { padding: 0; overflow: hidden; }
.card--empty { text-align: center; padding: 3rem; }
```

- [ ] **Step 5: Replace button base and all `.btn-*` classes**

Replace the `%btn-base` extend and all `.btn-*` classes:

```scss
%btn-base {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  padding: 0.65rem 1.4rem;
  border: none;
  border-radius: 999px;
  font-family: var(--sans);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: filter var(--t-fast), box-shadow var(--t-base), opacity var(--t-base);
  text-decoration: none;
  white-space: nowrap;
  line-height: 1.4;
  letter-spacing: 0.01em;

  &:disabled { opacity: 0.35; cursor: not-allowed; }
  &:active:not(:disabled) { transform: translateY(1px) scale(0.99); }
}

.btn-primary {
  @extend %btn-base;
  background: var(--moss);
  color: var(--paper);
  &:hover:not(:disabled) { filter: brightness(1.1); }
}

.btn-secondary {
  @extend %btn-base;
  background: var(--card);
  color: var(--ink-2);
  border: 1px solid var(--line);
  &:hover:not(:disabled) { background: var(--paper-warmer); border-color: rgba(0,0,0,0.12); }
}

.btn-ghost {
  @extend %btn-base;
  background: transparent;
  color: var(--ink-2);
  border: 1px solid var(--line);
  &:hover:not(:disabled) { background: var(--card); }
}

.btn-danger {
  @extend %btn-base;
  background: var(--berry);
  color: #fff;
  &:hover:not(:disabled) { filter: brightness(1.1); }
}

.btn-success {
  @extend %btn-base;
  background: var(--moss);
  color: var(--paper);
  &:hover:not(:disabled) { filter: brightness(1.1); }
}

.btn-amber {
  @extend %btn-base;
  background: var(--amber);
  color: #fff;
  &:hover:not(:disabled) { filter: brightness(1.1); }
}

.btn-outline {
  @extend %btn-base;
  background: transparent;
  color: var(--moss);
  border: 1px solid var(--moss-line);
  &:hover:not(:disabled) { background: var(--moss-tint); }
}

.btn-sm  { padding: 7px 12px; font-size: 0.8rem; }
.btn-lg  { padding: 14px 24px; font-size: 1rem; }
.btn-xs  { padding: 0.25rem 0.6rem; font-size: 0.75rem; }
.btn-block { width: 100%; }

.btn-icon {
  @extend %btn-base;
  padding: 0.5rem;
  border-radius: var(--r-sm);
  background: var(--card);
  border: 1px solid var(--line);
  color: var(--ink-2);
  &:hover:not(:disabled) { background: var(--paper-warmer); }
}
```

- [ ] **Step 6: Replace `.form-control` and `.form-group`**

```scss
.form-group {
  margin-bottom: 1.25rem;

  label {
    display: block;
    font-size: 0.8rem;
    font-weight: 500;
    color: var(--ink-2);
    margin-bottom: 0.45rem;
  }
}

.form-control {
  width: 100%;
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: 12px;
  color: var(--ink);
  padding: 0.7rem 1rem;
  font-family: var(--sans);
  font-size: 0.9rem;
  transition: border-color var(--t-base), box-shadow var(--t-base);
  outline: none;
  appearance: none;

  &::placeholder { color: var(--ink-4); }

  &:focus {
    border-color: var(--moss);
    box-shadow: 0 0 0 3px var(--moss-tint);
    background: var(--card);
  }

  &.is-invalid {
    border-color: var(--berry);
    box-shadow: 0 0 0 3px var(--berry-tint);
  }
}

.form-error {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.78rem;
  color: var(--berry);
  margin-top: 0.35rem;
  font-weight: 500;
}

.form-hint {
  display: block;
  font-size: 0.78rem;
  color: var(--ink-3);
  margin-top: 0.35rem;
}
```

- [ ] **Step 7: Replace `.data-table`**

```scss
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
  font-variant-numeric: tabular-nums;

  thead tr {
    background: var(--paper-warmer);
    border-bottom: 1.5px solid var(--ink);
  }

  thead th {
    padding: 0.7rem 1rem;
    text-align: left;
    font-size: 0.7rem;
    font-weight: 600;
    color: var(--ink-3);
    text-transform: uppercase;
    letter-spacing: 0.08em;
    font-family: var(--mono);
    white-space: nowrap;
  }

  tbody tr {
    border-bottom: 1px dashed var(--line-soft);

    &:last-child { border-bottom: none; }
    &:hover { background: var(--paper-warmer); }
  }

  tbody td {
    padding: 0.85rem 1rem;
    color: var(--ink);
    vertical-align: middle;
  }
}
```

- [ ] **Step 8: Replace badge classes**

```scss
%tag-base {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.22rem 0.65rem;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: 0.03em;
}

.tag-moss, .badge-green {
  @extend %tag-base;
  background: var(--moss-tint);
  color: var(--moss-deep);
  border: 1px solid var(--moss-line);
}

.tag-amber, .badge-orange {
  @extend %tag-base;
  background: var(--amber-tint);
  color: var(--amber-deep);
  border: 1px solid var(--amber-line);
}

.tag-berry, .badge-red {
  @extend %tag-base;
  background: var(--berry-tint);
  color: var(--berry);
  border: 1px solid rgba(181,69,60,0.3);
}

.tag-neutral, .badge-neutral {
  @extend %tag-base;
  background: var(--paper-warmer);
  color: var(--ink-2);
  border: 1px solid var(--line);
}

.badge-yellow {
  @extend %tag-base;
  background: var(--amber-tint);
  color: var(--amber-deep);
  border: 1px solid var(--amber-line);
}
```

- [ ] **Step 9: Add new Editorial utility classes**

Add after the existing utility section:

```scss
// ── Editorial utilities ───────────────────────────────────────
.num    { font-family: var(--mono); font-variant-numeric: tabular-nums; }
.serif  { font-family: var(--serif); }
.up     { color: var(--moss) !important; }
.down   { color: var(--berry) !important; }
.muted  { color: var(--ink-3) !important; }
.tiny   { font-size: 11px; text-transform: uppercase; letter-spacing: 0.08em; color: var(--ink-3); font-weight: 500; }

.dash-divider {
  border: none;
  border-top: 1.5px dashed var(--line);
  margin: 1.5rem 0;
}

.dotted {
  background-image: radial-gradient(circle, var(--ink-3) 1px, transparent 1px);
  background-size: 4px 1px;
  background-repeat: repeat-x;
  background-position: 0 100%;
  text-decoration: none;
}
```

- [ ] **Step 10: Update existing text color utilities to use new tokens**

Find and replace in the utility block:

```scss
// Old:         New:
.text-muted     { color: var(--ink-3)  !important; }
.text-secondary { color: var(--ink-2)  !important; }
.text-green     { color: var(--moss)   !important; }
.text-red       { color: var(--berry)  !important; }
.text-cyan      { color: var(--moss)   !important; }
.text-orange    { color: var(--amber)  !important; }
.text-primary-color { color: var(--ink) !important; }

.positive { color: var(--moss);  font-weight: 600; &::before { content: '+'; } }
.negative { color: var(--berry); font-weight: 600; }
```

- [ ] **Step 11: Update scrollbar, spinner, modal, alert colors**

```scss
// Scrollbar
::-webkit-scrollbar-thumb {
  background: var(--line);
  &:hover { background: var(--line-soft); }
}

// Spinner
.spinner {
  border-color: var(--moss-tint);
  border-top-color: var(--moss);
}

// Modal
.modal {
  background: var(--card);
  border-color: var(--line);
  &::before { display: none; }  // remove the old gradient top line
  &__footer { border-top-color: var(--line); }
}
.modal-backdrop { background: rgba(31,42,28,0.5); backdrop-filter: blur(4px); }

// Alerts
.alert {
  &-success { background: var(--moss-tint);  border-color: var(--moss-line);  color: var(--moss-deep); }
  &-danger  { background: var(--berry-tint); border-color: rgba(181,69,60,0.3); color: var(--berry); }
  &-warning { background: var(--amber-tint); border-color: var(--amber-line); color: var(--amber-deep); }
  &-info    { background: var(--moss-tint);  border-color: var(--moss-line);  color: var(--moss-deep); }
}

// Page header bottom border
.page-header { border-bottom-color: var(--line); }

// Divider
.divider { border-top-color: var(--line); }

// Pagination
.pagination button {
  background: var(--card);
  border-color: var(--line);
  color: var(--ink-2);
  &.active { background: var(--moss); color: var(--paper); }
}
```

- [ ] **Step 12: Commit**

```bash
git add frontend/src/styles.scss
git commit -m "style: replace dark-forest tokens with Editorial Almanac palette in styles.scss"
```

---

## Task 2: Layout shell background

**Files:**
- Modify: `frontend/src/app/shared/components/layout/layout.component.scss`

- [ ] **Step 1: Update background token**

Replace the file content with:

```scss
.layout__content {
  min-height: calc(100vh - 64px);
  background: var(--paper);
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/app/shared/components/layout/layout.component.scss
git commit -m "style: set layout shell background to var(--paper)"
```

---

## Task 3: Navbar

**Files:**
- Modify: `frontend/src/app/shared/components/navbar/navbar.component.html`
- Modify: `frontend/src/app/shared/components/navbar/navbar.component.scss`

- [ ] **Step 1: Update navbar HTML — logo, wordmark, search, avatar**

Replace the full file content with:

```html
<nav class="navbar" role="navigation" aria-label="Navegación principal">
  <div class="navbar__container">
    <!-- Brand -->
    <a class="navbar__brand" routerLink="/dashboard" aria-label="Acciones El Bosque — inicio">
      <img class="navbar__logo-img" src="logo.png" alt="Universidad El Bosque" />
      <span class="navbar__divider" aria-hidden="true"></span>
      <span class="navbar__wordmark">Acciones <em>El Bosque</em></span>
    </a>

    <!-- Desktop Nav Links -->
    @if (loggedIn) {
      <ul class="navbar__links" role="list">
        <li>
          <a routerLink="/dashboard" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}">
            Dashboard
          </a>
        </li>
        <li><a routerLink="/market" routerLinkActive="active">Mercado</a></li>
        <li><a routerLink="/portfolio/positions" routerLinkActive="active">Portafolio</a></li>
        <li><a routerLink="/orders/history" routerLinkActive="active">Órdenes</a></li>
        <li><a routerLink="/notifications" routerLinkActive="active">Alertas</a></li>
        <li><a routerLink="/watchlist" routerLinkActive="active">Watchlist</a></li>

        @if (isAdmin) {
          <li class="navbar__dropdown" [class.open]="adminOpen()">
            <button class="navbar__dropdown-toggle" type="button"
              [attr.aria-expanded]="adminOpen()" aria-haspopup="true" (click)="toggleAdmin()">
              Admin
              <svg class="chevron" width="10" height="6" viewBox="0 0 10 6" fill="none" aria-hidden="true">
                <path d="M1 1L5 5L9 1" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
            <ul class="navbar__dropdown-menu" role="menu" (click)="closeMenus()">
              <li role="none"><a routerLink="/admin/dashboard" role="menuitem">Dashboard Admin</a></li>
              <li role="none"><a routerLink="/admin/users" role="menuitem">Usuarios</a></li>
              <li role="none"><a routerLink="/admin/parameters" role="menuitem">Parámetros</a></li>
              <li role="none"><a routerLink="/admin/markets" role="menuitem">Mercados</a></li>
              <li role="none"><a routerLink="/admin/audit" role="menuitem">Auditoría</a></li>
            </ul>
          </li>
        }

        @if (isBroker && !isAdmin) {
          <li class="navbar__dropdown" [class.open]="brokerOpen()">
            <button class="navbar__dropdown-toggle" type="button"
              [attr.aria-expanded]="brokerOpen()" aria-haspopup="true" (click)="toggleBroker()">
              Clientes
              <svg class="chevron" width="10" height="6" viewBox="0 0 10 6" fill="none" aria-hidden="true">
                <path d="M1 1L5 5L9 1" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
            <ul class="navbar__dropdown-menu" role="menu" (click)="closeMenus()">
              <li role="none"><a routerLink="/broker/clients" role="menuitem">Mis Clientes</a></li>
              <li role="none"><a routerLink="/broker/orders" role="menuitem">Órdenes</a></li>
            </ul>
          </li>
        }
      </ul>
    }

    <!-- Right Section -->
    <div class="navbar__right">
      @if (loggedIn) {
        <a class="navbar__icon-btn" routerLink="/watchlist" aria-label="Watchlist" title="Watchlist">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path d="M8 1.5L9.8 5.6L14.3 6.1L11.1 9.1L12 13.5L8 11.3L4 13.5L4.9 9.1L1.7 6.1L6.2 5.6L8 1.5Z"
              stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/>
          </svg>
        </a>

        <div class="navbar__user" [class.open]="userMenuOpen()">
          <button class="navbar__avatar" type="button"
            [attr.aria-expanded]="userMenuOpen()" aria-haspopup="true"
            aria-label="Menú de usuario" (click)="toggleUserMenu()">
            {{ profile()?.fullName?.charAt(0) ?? 'U' }}
          </button>
          <div class="navbar__user-menu" role="menu"
            [class.open]="userMenuOpen()" (click)="closeAll()">
            @if (profile()) {
              <div class="navbar__user-name" role="none">{{ profile()!.fullName }}</div>
              <div class="navbar__user-email" role="none">{{ profile()!.email }}</div>
            }
            <hr role="separator" />
            <a routerLink="/profile" role="menuitem">Mi Perfil</a>
            <a routerLink="/profile/password" role="menuitem">Contraseña</a>
            <a routerLink="/preferences" role="menuitem">Preferencias</a>
            <a routerLink="/subscription" role="menuitem">Suscripción</a>
            <hr role="separator" />
            <button class="logout-btn" type="button" role="menuitem" (click)="logout()">Cerrar Sesión</button>
          </div>
        </div>
      } @else {
        <a class="btn-ghost btn-sm" routerLink="/login">Iniciar Sesión</a>
        <a class="btn-primary btn-sm" routerLink="/register">Registrarse</a>
      }

      <button class="navbar__mobile-toggle" type="button"
        [attr.aria-expanded]="mobileOpen()" aria-label="Abrir menú de navegación"
        (click)="toggleMobile()">
        <span aria-hidden="true"></span>
        <span aria-hidden="true"></span>
        <span aria-hidden="true"></span>
      </button>
    </div>
  </div>

  @if (mobileOpen()) {
    <nav class="navbar__mobile-menu" aria-label="Menú móvil" (click)="toggleMobile()">
      @if (loggedIn) {
        <a routerLink="/dashboard">Dashboard</a>
        <a routerLink="/market">Mercado</a>
        <a routerLink="/portfolio/positions">Portafolio</a>
        <a routerLink="/orders/history">Órdenes</a>
        <a routerLink="/notifications">Alertas</a>
        <a routerLink="/watchlist">Watchlist</a>
        <a routerLink="/profile">Perfil</a>
        @if (isAdmin) { <a routerLink="/admin/dashboard">Admin</a> }
        @if (isBroker) { <a routerLink="/broker/clients">Clientes</a> }
        <button class="logout-btn" type="button" (click)="logout()">Cerrar Sesión</button>
      } @else {
        <a routerLink="/login">Iniciar Sesión</a>
        <a routerLink="/register">Registrarse</a>
      }
    </nav>
  }
</nav>
```

- [ ] **Step 2: Replace navbar SCSS**

Replace the full file with:

```scss
.navbar {
  position: sticky;
  top: 0;
  z-index: 500;
  height: 64px;
  background: var(--paper);
  border-bottom: 1px solid var(--line);
  box-shadow: var(--sh-1);

  &__container {
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 1.5rem;
    height: 100%;
    display: flex;
    align-items: center;
    gap: 1.5rem;
  }

  // ── Brand ─────────────────────────────────────────────────
  &__brand {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    text-decoration: none;
    flex-shrink: 0;
    &:hover { opacity: 0.85; text-decoration: none; }
  }

  &__logo-img {
    height: 40px;
    width: auto;
    display: block;
  }

  &__divider {
    display: block;
    width: 1px;
    height: 24px;
    background: var(--line);
  }

  &__wordmark {
    font-family: var(--serif);
    font-size: 1rem;
    color: var(--ink);
    font-style: normal;
    em {
      font-style: italic;
      color: var(--moss);
    }
  }

  // ── Nav Links ─────────────────────────────────────────────
  &__links {
    display: flex;
    align-items: center;
    gap: 0.1rem;
    list-style: none;
    margin: 0;
    padding: 0;
    flex: 1;

    li > a {
      display: block;
      padding: 0.4rem 0.85rem;
      border-radius: 999px;
      font-size: 0.86rem;
      font-weight: 400;
      color: var(--ink-2);
      text-decoration: none;
      transition: color 0.15s, background 0.15s, border-color 0.15s;
      border: 1px solid transparent;

      &:hover {
        color: var(--ink);
        background: var(--paper-warmer);
        text-decoration: none;
      }

      &.active {
        color: var(--ink);
        background: var(--card);
        border-color: var(--line);
        font-weight: 500;
      }
    }
  }

  // ── Dropdown ──────────────────────────────────────────────
  &__dropdown {
    position: relative;
    &.open &-menu { display: block; }
    &.open &-toggle .chevron { transform: rotate(180deg); }
  }

  &__dropdown-toggle {
    display: flex;
    align-items: center;
    gap: 0.3rem;
    padding: 0.4rem 0.85rem;
    border-radius: 999px;
    font-size: 0.86rem;
    font-weight: 400;
    color: var(--ink-2);
    background: none;
    border: 1px solid transparent;
    cursor: pointer;
    transition: color 0.15s, background 0.15s;

    &:hover { color: var(--ink); background: var(--paper-warmer); }

    .chevron { opacity: 0.6; transition: transform 0.2s ease; }
  }

  &__dropdown-menu {
    display: none;
    position: absolute;
    top: calc(100% + 8px);
    left: 0;
    min-width: 196px;
    background: var(--card);
    border: 1px solid var(--line);
    border-radius: var(--r-lg);
    padding: 0.4rem;
    box-shadow: var(--sh-2);
    z-index: 100;
    animation: slideDown 0.15s ease;

    li a {
      display: block;
      padding: 0.5rem 0.85rem;
      font-size: 0.84rem;
      font-weight: 400;
      color: var(--ink-2);
      border-radius: var(--r-sm);
      text-decoration: none;
      transition: background 0.12s, color 0.12s;

      &:hover { background: var(--paper-warmer); color: var(--ink); text-decoration: none; }
    }
  }

  // ── Right ─────────────────────────────────────────────────
  &__right {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    margin-left: auto;
  }

  &__icon-btn {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    color: var(--ink-3);
    text-decoration: none;
    transition: color 0.15s, background 0.15s;
    border: 1px solid var(--line);
    background: var(--card);

    &:hover { color: var(--ink); background: var(--paper-warmer); text-decoration: none; }
  }

  // ── User Menu ─────────────────────────────────────────────
  &__user { position: relative; }

  &__avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: linear-gradient(135deg, #3F7A4E, #D89154);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    font-size: 0.8rem;
    cursor: pointer;
    border: none;
    padding: 0;
    transition: opacity 0.15s;
    &:hover { opacity: 0.85; }
  }

  &__user-menu {
    display: none;
    position: absolute;
    top: calc(100% + 10px);
    right: 0;
    min-width: 210px;
    background: var(--card);
    border: 1px solid var(--line);
    border-radius: var(--r-lg);
    padding: 0.5rem;
    box-shadow: var(--sh-2);
    z-index: 100;

    &.open { display: block; animation: slideDown 0.15s ease; }

    a, button {
      display: block;
      width: 100%;
      padding: 0.5rem 0.9rem;
      font-size: 0.84rem;
      font-weight: 400;
      color: var(--ink-2);
      border-radius: var(--r-sm);
      text-decoration: none;
      text-align: left;
      background: none;
      border: none;
      cursor: pointer;
      transition: background 0.12s, color 0.12s;

      &:hover { background: var(--paper-warmer); color: var(--ink); text-decoration: none; }
    }

    hr { border: none; border-top: 1px solid var(--line); margin: 0.4rem 0; }
  }

  &__user-name { padding: 0.5rem 0.9rem 0.1rem; font-size: 0.875rem; font-weight: 600; color: var(--ink); }
  &__user-email { padding: 0 0.9rem 0.5rem; font-size: 0.72rem; color: var(--ink-3); }

  .logout-btn { color: var(--berry) !important; &:hover { background: var(--berry-tint) !important; } }

  // ── Mobile toggle ─────────────────────────────────────────
  &__mobile-toggle {
    display: none;
    flex-direction: column;
    gap: 4px;
    background: none;
    border: none;
    cursor: pointer;
    padding: 0.5rem;
    min-width: 44px;
    min-height: 44px;
    align-items: center;
    justify-content: center;

    span {
      display: block;
      width: 20px;
      height: 1.5px;
      background: var(--ink-3);
      border-radius: 2px;
    }
    &:hover span { background: var(--ink); }
  }

  &__mobile-menu {
    display: flex;
    flex-direction: column;
    background: var(--paper);
    border-top: 1px solid var(--line);
    padding: 0.75rem;
    gap: 0.15rem;
    animation: slideDown 0.2s ease;

    a, button {
      display: block;
      padding: 0.75rem 0.85rem;
      font-size: 0.9rem;
      color: var(--ink-2);
      border-radius: var(--r);
      text-decoration: none;
      background: none;
      border: none;
      cursor: pointer;
      text-align: left;
      min-height: 44px;
      transition: background 0.12s, color 0.12s;
      &:hover { background: var(--paper-warmer); color: var(--ink); text-decoration: none; }
    }

    .logout-btn { color: var(--berry) !important; &:hover { background: var(--berry-tint) !important; } }
  }
}

@media (max-width: 768px) {
  .navbar {
    height: auto;
    min-height: 64px;
    &__container { height: 64px; }
    &__links { display: none; }
    &__mobile-toggle { display: flex; }
    &__right .btn-ghost,
    &__right .btn-primary { display: none; }
  }
}

@keyframes slideDown {
  from { transform: translateY(-6px); opacity: 0; }
  to   { transform: translateY(0);    opacity: 1; }
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/shared/components/navbar/navbar.component.html
git add frontend/src/app/shared/components/navbar/navbar.component.scss
git commit -m "style: restyle navbar — cream bg, pill links, logo.png, serif wordmark"
```

---

## Task 4: Login

**Files:**
- Modify: `frontend/src/app/features/auth/login/login.component.html`
- Modify: `frontend/src/app/features/auth/login/login.component.scss`

- [ ] **Step 1: Replace login HTML — two-column split layout**

Keep all Angular form bindings (`[formGroup]`, `formControlName`, `(ngSubmit)`, `loading()`, `showPassword()`, `togglePassword()`, `errorMsg()`). Only change structure and classes.

```html
<div class="auth-split">
  <!-- Left panel — green gradient hero -->
  <div class="auth-split__left" aria-hidden="true">
    <div class="auth-split__left-inner">
      <p class="auth-split__eyebrow tiny">Plataforma de inversión</p>
      <h1 class="auth-split__hero serif">
        El dinero que<br>siembras hoy,<br><em>crece mañana.</em>
      </h1>
      <p class="auth-split__tagline">
        Invierte en las empresas que mueven el mundo.<br>
        Con el rigor académico de <strong>El Bosque.</strong>
      </p>
      <p class="auth-split__footer-note num">Acciones El Bosque · v2.0 · 2026</p>
    </div>
  </div>

  <!-- Right panel — form -->
  <div class="auth-split__right">
    <img class="auth-split__logo" src="logo.png" alt="Universidad El Bosque" />

    <div class="auth-split__form-wrap">
      <p class="tiny auth-split__eyebrow-right">Inicia sesión</p>
      <h2 class="serif auth-split__heading">Bienvenido / <em class="up">de vuelta.</em></h2>

      @if (errorMsg()) {
        <div class="alert alert-danger">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true" style="flex-shrink:0">
            <path d="M8 2.5L14.2 13H1.8L8 2.5Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
            <path d="M8 7v2.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <circle cx="8" cy="11.5" r="0.75" fill="currentColor"/>
          </svg>
          {{ errorMsg() }}
        </div>
      }

      <form [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
        <div class="form-group">
          <label class="label" for="email">Correo electrónico</label>
          <input id="email" type="email" class="form-control field"
            [class.is-invalid]="f['email'].touched && f['email'].invalid"
            formControlName="email" placeholder="tu@correo.com" autocomplete="email" />
          @if (f['email'].touched && f['email'].errors?.['required']) {
            <span class="form-error">El correo es obligatorio.</span>
          }
          @if (f['email'].touched && f['email'].errors?.['email']) {
            <span class="form-error">Ingresa un correo válido.</span>
          }
        </div>

        <div class="form-group">
          <div class="form-label-row">
            <label class="label" for="password">Contraseña</label>
            <a class="dotted muted" href="#" tabindex="-1" style="font-size:0.78rem">¿Olvidaste?</a>
          </div>
          <div class="input-icon-group">
            <input id="password" [type]="showPassword() ? 'text' : 'password'"
              class="form-control field"
              [class.is-invalid]="f['password'].touched && f['password'].invalid"
              formControlName="password" placeholder="Tu contraseña" autocomplete="current-password" />
            <button type="button" class="input-icon-btn"
              (click)="togglePassword()"
              [attr.aria-label]="showPassword() ? 'Ocultar contraseña' : 'Mostrar contraseña'">
              @if (showPassword()) {
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                  <path d="M1 8C2.3 5.5 4.9 4 8 4s5.7 1.5 7 4c-1.3 2.5-3.9 4-7 4S2.3 10.5 1 8Z" stroke="currentColor" stroke-width="1.4"/>
                  <circle cx="8" cy="8" r="2" stroke="currentColor" stroke-width="1.4"/>
                </svg>
              } @else {
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                  <path d="M1.5 1.5l13 13" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
                  <path d="M6.4 6.5a2 2 0 002.9 2.9M4.5 4.6C2.8 5.6 1.7 6.8 1 8c1.3 2.5 3.9 4 7 4 1.2 0 2.4-.3 3.4-.9m2-1.8C14.3 9.2 15 8.1 15 8c-1.3-2.5-3.9-4-7-4-.6 0-1.2.1-1.7.2" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
                </svg>
              }
            </button>
          </div>
          @if (f['password'].touched && f['password'].errors?.['required']) {
            <span class="form-error">La contraseña es obligatoria.</span>
          }
        </div>

        <button type="submit" class="btn-primary btn-block"
          [disabled]="loading() || form.invalid">
          @if (loading()) {
            <span class="spinner spinner--sm"></span> Iniciando sesión...
          } @else {
            Entrar al bosque
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
              <path d="M2 7h10M8 3l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          }
        </button>
      </form>

      <div class="auth-split__or">
        <span class="dash-divider" style="flex:1"></span>
        <span class="muted" style="font-size:0.8rem;padding:0 0.75rem">o</span>
        <span class="dash-divider" style="flex:1"></span>
      </div>

      <button type="button" class="btn-ghost btn-block">Continuar con cuenta institucional</button>

      <p class="auth-split__register-link">
        ¿Aún no eres parte? <a routerLink="/register">Regístrate gratis</a>
      </p>
    </div>
  </div>
</div>
```

- [ ] **Step 2: Replace login SCSS**

```scss
.auth-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 100dvh;

  // ── Left ────────────────────────────────────────────────
  &__left {
    background: linear-gradient(160deg, #3F7A4E 0%, #2F5C3A 100%);
    display: flex;
    align-items: center;
    padding: 4rem 3rem;
  }

  &__left-inner { max-width: 400px; }

  &__eyebrow { color: rgba(255,255,255,0.6); margin-bottom: 1.5rem; display: block; }

  &__hero {
    font-size: clamp(2.5rem, 5vw, 4rem);
    color: #fff;
    line-height: 1.15;
    margin-bottom: 1.5rem;

    em {
      font-style: italic;
      color: #F4DDB8;
    }
  }

  &__tagline {
    font-size: 1rem;
    color: rgba(255,255,255,0.75);
    line-height: 1.7;
    font-style: italic;
    max-width: 340px;
    margin-bottom: 3rem;
  }

  &__footer-note {
    font-size: 11px;
    color: rgba(255,255,255,0.4);
    letter-spacing: 0.06em;
  }

  // ── Right ───────────────────────────────────────────────
  &__right {
    background: var(--paper);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 4rem 3rem;
    position: relative;
  }

  &__logo {
    position: absolute;
    top: 36px;
    right: 48px;
    height: 60px;
    width: auto;
  }

  &__form-wrap {
    width: 100%;
    max-width: 380px;
  }

  &__eyebrow-right {
    display: block;
    margin-bottom: 0.5rem;
  }

  &__heading {
    font-size: 2.25rem;
    color: var(--ink);
    margin-bottom: 2rem;
    line-height: 1.2;

    em { color: var(--moss); }
  }

  &__or {
    display: flex;
    align-items: center;
    margin: 1.25rem 0;

    .dash-divider { margin: 0; }
  }

  &__register-link {
    text-align: center;
    font-size: 0.85rem;
    color: var(--ink-3);
    margin-top: 1.25rem;

    a { color: var(--moss); font-weight: 500; }
  }
}

.input-icon-group {
  position: relative;

  .form-control { padding-right: 2.8rem; }

  .input-icon-btn {
    position: absolute;
    right: 0.75rem;
    top: 50%;
    transform: translateY(-50%);
    background: none;
    border: none;
    color: var(--ink-3);
    cursor: pointer;
    padding: 0.25rem;
    border-radius: 4px;
    display: flex;
    align-items: center;
    &:hover { color: var(--ink); }
  }
}

.form-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.45rem;
  label { margin-bottom: 0; }
}

@media (max-width: 768px) {
  .auth-split {
    grid-template-columns: 1fr;
    &__left { display: none; }
    &__right { padding: 2.5rem 1.5rem; }
    &__logo { top: 24px; right: 24px; height: 44px; }
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/features/auth/login/login.component.html
git add frontend/src/app/features/auth/login/login.component.scss
git commit -m "style: login — two-column Editorial Almanac split layout"
```

---

## Task 5: Register

**Files:**
- Modify: `frontend/src/app/features/auth/register/register.component.html`
- Modify: `frontend/src/app/features/auth/register/register.component.scss`

- [ ] **Step 1: Replace register HTML — same two-column split**

Keep all Angular bindings (`[formGroup]`, `formControlName`, `(ngSubmit)`, `loading()`, `showPassword()`, `showConfirm()`, `togglePassword()`, `toggleConfirm()`, `errorMsg()`, `successMsg()`).

```html
<div class="auth-split">
  <div class="auth-split__left" aria-hidden="true">
    <div class="auth-split__left-inner">
      <p class="auth-split__eyebrow tiny">Plataforma de inversión</p>
      <h1 class="auth-split__hero serif">
        Planta tu<br>primera<br><em>semilla.</em>
      </h1>
      <p class="auth-split__tagline">
        Crea tu cuenta y empieza a construir<br>
        tu portafolio desde cero.
      </p>
      <p class="auth-split__footer-note num">Acciones El Bosque · v2.0 · 2026</p>
    </div>
  </div>

  <div class="auth-split__right">
    <img class="auth-split__logo" src="logo.png" alt="Universidad El Bosque" />

    <div class="auth-split__form-wrap">
      <p class="tiny auth-split__eyebrow-right">Crear cuenta</p>
      <h2 class="serif auth-split__heading">Crea tu <em class="up">cuenta.</em></h2>

      @if (successMsg()) {
        <div class="alert alert-success">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true" style="flex-shrink:0">
            <path d="M2 8l5 5 7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <div><strong>¡Registro exitoso!</strong><br />{{ successMsg() }}</div>
        </div>
        <a routerLink="/login" class="btn-primary btn-block">Ir a Iniciar Sesión</a>
      }

      @if (!successMsg()) {
        @if (errorMsg()) {
          <div class="alert alert-danger">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true" style="flex-shrink:0">
              <path d="M8 2.5L14.2 13H1.8L8 2.5Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
              <path d="M8 7v2.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              <circle cx="8" cy="11.5" r="0.75" fill="currentColor"/>
            </svg>
            {{ errorMsg() }}
          </div>
        }

        <form [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
          <div class="form-group">
            <label class="label" for="fullName">Nombre completo</label>
            <input id="fullName" type="text" class="form-control field"
              [class.is-invalid]="f['fullName'].touched && f['fullName'].invalid"
              formControlName="fullName" placeholder="Ej. Juan García" autocomplete="name" />
            @if (f['fullName'].touched && f['fullName'].errors?.['required']) {
              <span class="form-error">El nombre es obligatorio.</span>
            }
            @if (f['fullName'].touched && f['fullName'].errors?.['minlength']) {
              <span class="form-error">Mínimo 3 caracteres.</span>
            }
          </div>

          <div class="form-group">
            <label class="label" for="documentNumber">Número de documento</label>
            <input id="documentNumber" type="text" class="form-control field"
              [class.is-invalid]="f['documentNumber'].touched && f['documentNumber'].invalid"
              formControlName="documentNumber" placeholder="Ej. 12345678" autocomplete="off" />
            @if (f['documentNumber'].touched && f['documentNumber'].errors?.['required']) {
              <span class="form-error">El documento es obligatorio.</span>
            }
            @if (f['documentNumber'].touched && f['documentNumber'].errors?.['pattern']) {
              <span class="form-error">Ingresa entre 6 y 12 dígitos numéricos.</span>
            }
          </div>

          <div class="form-group">
            <label class="label" for="email">Correo electrónico</label>
            <input id="email" type="email" class="form-control field"
              [class.is-invalid]="f['email'].touched && f['email'].invalid"
              formControlName="email" placeholder="tu@correo.com" autocomplete="email" />
            @if (f['email'].touched && f['email'].errors?.['required']) {
              <span class="form-error">El correo es obligatorio.</span>
            }
            @if (f['email'].touched && f['email'].errors?.['email']) {
              <span class="form-error">Ingresa un correo válido.</span>
            }
          </div>

          <div class="form-group">
            <label class="label" for="password">Contraseña</label>
            <div class="input-icon-group">
              <input id="password" [type]="showPassword() ? 'text' : 'password'"
                class="form-control field"
                [class.is-invalid]="f['password'].touched && f['password'].invalid"
                formControlName="password" placeholder="Mínimo 8 caracteres" autocomplete="new-password" />
              <button type="button" class="input-icon-btn" (click)="togglePassword()"
                [attr.aria-label]="showPassword() ? 'Ocultar' : 'Mostrar'">
                @if (showPassword()) {
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                    <path d="M1 8C2.3 5.5 4.9 4 8 4s5.7 1.5 7 4c-1.3 2.5-3.9 4-7 4S2.3 10.5 1 8Z" stroke="currentColor" stroke-width="1.4"/>
                    <circle cx="8" cy="8" r="2" stroke="currentColor" stroke-width="1.4"/>
                  </svg>
                } @else {
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                    <path d="M1.5 1.5l13 13" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
                    <path d="M6.4 6.5a2 2 0 002.9 2.9M4.5 4.6C2.8 5.6 1.7 6.8 1 8c1.3 2.5 3.9 4 7 4 1.2 0 2.4-.3 3.4-.9m2-1.8C14.3 9.2 15 8.1 15 8c-1.3-2.5-3.9-4-7-4-.6 0-1.2.1-1.7.2" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
                  </svg>
                }
              </button>
            </div>
            @if (f['password'].touched && f['password'].errors?.['required']) {
              <span class="form-error">La contraseña es obligatoria.</span>
            }
            @if (f['password'].touched && f['password'].errors?.['minlength']) {
              <span class="form-error">Mínimo 8 caracteres.</span>
            }
          </div>

          <div class="form-group">
            <label class="label" for="confirmPassword">Confirmar contraseña</label>
            <div class="input-icon-group">
              <input id="confirmPassword" [type]="showConfirm() ? 'text' : 'password'"
                class="form-control field"
                [class.is-invalid]="f['confirmPassword'].touched && f['confirmPassword'].invalid"
                formControlName="confirmPassword" placeholder="Repite la contraseña" autocomplete="new-password" />
              <button type="button" class="input-icon-btn" (click)="toggleConfirm()"
                [attr.aria-label]="showConfirm() ? 'Ocultar' : 'Mostrar'">
                @if (showConfirm()) {
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                    <path d="M1 8C2.3 5.5 4.9 4 8 4s5.7 1.5 7 4c-1.3 2.5-3.9 4-7 4S2.3 10.5 1 8Z" stroke="currentColor" stroke-width="1.4"/>
                    <circle cx="8" cy="8" r="2" stroke="currentColor" stroke-width="1.4"/>
                  </svg>
                } @else {
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                    <path d="M1.5 1.5l13 13" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
                    <path d="M6.4 6.5a2 2 0 002.9 2.9M4.5 4.6C2.8 5.6 1.7 6.8 1 8c1.3 2.5 3.9 4 7 4 1.2 0 2.4-.3 3.4-.9m2-1.8C14.3 9.2 15 8.1 15 8c-1.3-2.5-3.9-4-7-4-.6 0-1.2.1-1.7.2" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
                  </svg>
                }
              </button>
            </div>
            @if (f['confirmPassword'].touched && f['confirmPassword'].errors?.['required']) {
              <span class="form-error">Confirma tu contraseña.</span>
            }
            @if (f['confirmPassword'].touched && f['confirmPassword'].errors?.['passwordMismatch']) {
              <span class="form-error">Las contraseñas no coinciden.</span>
            }
          </div>

          <button type="submit" class="btn-primary btn-block"
            [disabled]="loading() || form.invalid">
            @if (loading()) {
              <span class="spinner spinner--sm"></span> Registrando...
            } @else {
              Crear cuenta
            }
          </button>
        </form>

        <p class="auth-split__register-link">
          ¿Ya tienes cuenta? <a routerLink="/login">Iniciar sesión</a>
        </p>
      }
    </div>
  </div>
</div>
```

- [ ] **Step 2: Replace register SCSS**

The register component shares all `.auth-split` styles. Import/extend them or copy the same SCSS from login. Since Angular compiles component SCSS separately, copy the full SCSS:

```scss
// Same layout as login — Editorial Almanac two-column split
.auth-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 100dvh;

  &__left {
    background: linear-gradient(160deg, #3F7A4E 0%, #2F5C3A 100%);
    display: flex;
    align-items: center;
    padding: 4rem 3rem;
  }

  &__left-inner { max-width: 400px; }
  &__eyebrow { color: rgba(255,255,255,0.6); margin-bottom: 1.5rem; display: block; }

  &__hero {
    font-size: clamp(2.5rem, 5vw, 4rem);
    color: #fff;
    line-height: 1.15;
    margin-bottom: 1.5rem;
    em { font-style: italic; color: #F4DDB8; }
  }

  &__tagline {
    font-size: 1rem;
    color: rgba(255,255,255,0.75);
    line-height: 1.7;
    font-style: italic;
    max-width: 340px;
    margin-bottom: 3rem;
  }

  &__footer-note { font-size: 11px; color: rgba(255,255,255,0.4); letter-spacing: 0.06em; }

  &__right {
    background: var(--paper);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 4rem 3rem;
    position: relative;
    overflow-y: auto;
  }

  &__logo { position: absolute; top: 36px; right: 48px; height: 60px; width: auto; }

  &__form-wrap { width: 100%; max-width: 380px; }

  &__eyebrow-right { display: block; margin-bottom: 0.5rem; }

  &__heading {
    font-size: 2.25rem;
    color: var(--ink);
    margin-bottom: 2rem;
    line-height: 1.2;
    em { color: var(--moss); }
  }

  &__register-link {
    text-align: center;
    font-size: 0.85rem;
    color: var(--ink-3);
    margin-top: 1.25rem;
    a { color: var(--moss); font-weight: 500; }
  }
}

.input-icon-group {
  position: relative;
  .form-control { padding-right: 2.8rem; }
  .input-icon-btn {
    position: absolute;
    right: 0.75rem;
    top: 50%;
    transform: translateY(-50%);
    background: none;
    border: none;
    color: var(--ink-3);
    cursor: pointer;
    padding: 0.25rem;
    display: flex;
    align-items: center;
    &:hover { color: var(--ink); }
  }
}

@media (max-width: 768px) {
  .auth-split {
    grid-template-columns: 1fr;
    &__left { display: none; }
    &__right { padding: 2.5rem 1.5rem; align-items: flex-start; padding-top: 5rem; }
    &__logo { top: 24px; right: 24px; height: 44px; }
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/features/auth/register/register.component.html
git add frontend/src/app/features/auth/register/register.component.scss
git commit -m "style: register — two-column Editorial Almanac split layout"
```

---

## Task 6: Dashboard HTML and SCSS

**Files:**
- Modify: `frontend/src/app/features/dashboard/dashboard.component.html`
- Modify: `frontend/src/app/features/dashboard/dashboard.component.scss`

- [ ] **Step 1: Replace dashboard HTML — newspaper almanac layout**

Wire all data through existing signals: `balance()`, `loadingBalance()`, `loadingOrders()`, `loadingProfile()`, `loadingCharts()`, `profile()`, `positions()`, `sparklines()`, `activeOrders`, `portfolioSeries`, `portfolioChart`, `portfolioStroke`, `portfolioFill`, `portfolioXAxis`, `portfolioYAxis`, `portfolioGrid`, `portfolioTooltip`, `donutSeries`, `donutChart`, `donutLabels`, `donutColors`, `donutPlotOptions`, `donutLegend`, `donutDataLabels`, `donutTooltip`, `sparkChart`, `sparkStroke`, `sparkFill()`, `sparkColor()`, `sparkTooltip`, `formatCurrency()`, `formatCurrencyShort()`, `formatPercent()`.

```html
<div class="almanac">
  <div class="container">

    <!-- Masthead -->
    <header class="almanac__masthead">
      <div class="almanac__masthead-col">
        <span class="num tiny">Vol. 2026 · No. 1</span>
        <span class="muted tiny">Universidad El Bosque</span>
      </div>
      <div class="almanac__masthead-title">
        <h1 class="serif">El Almanaque del Inversionista</h1>
      </div>
      <div class="almanac__masthead-col almanac__masthead-col--right">
        <span class="num tiny">Hoy — {{ today | date:'dd MMMM yyyy' }}</span>
        <span class="tiny up">● Mercado abierto</span>
      </div>
    </header>

    <!-- Hero section: balance + quick buy/sell -->
    <section class="almanac__hero">
      <div class="almanac__hero-main">
        <p class="tiny" style="margin-bottom:0.5rem">Balance disponible</p>
        @if (loadingBalance()) {
          <div class="spinner"></div>
        } @else {
          <div class="almanac__hero-number num">{{ formatCurrency(balance()?.availableBalance) }}</div>
          <p class="almanac__drop-cap">
            Tu portafolio vale <strong class="num">{{ formatCurrency(balance()?.totalPortfolioValue) }}</strong>
            con una ganancia no realizada de
            <strong [class.up]="(balance()?.unrealizedGain ?? 0) >= 0"
                    [class.down]="(balance()?.unrealizedGain ?? 0) < 0"
                    class="num">
              {{ formatCurrencyShort(balance()?.unrealizedGain ?? 0) }}
            </strong>
            ({{ formatPercent(balance()?.unrealizedGainPercent) }}).
          </p>
        }
      </div>
      <aside class="almanac__hero-aside">
        <div class="almanac__stat-rows">
          <div class="almanac__stat-row">
            <span class="muted">Reservado</span>
            <span class="num">{{ formatCurrency(balance()?.reservedForOrders) }}</span>
          </div>
          <div class="almanac__stat-row">
            <span class="muted">Órdenes activas</span>
            <span class="num">{{ activeOrders.length }}</span>
          </div>
          <div class="almanac__stat-row">
            <span class="muted">Suscripción</span>
            <span [class.tag-moss]="profile()?.subscriptionType === 'PREMIUM'"
                  [class.tag-neutral]="profile()?.subscriptionType !== 'PREMIUM'">
              {{ profile()?.subscriptionType ?? '—' }}
            </span>
          </div>
        </div>
        <div class="almanac__hero-btns">
          <a routerLink="/orders/buy" class="btn-primary">
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" aria-hidden="true">
              <path d="M6 1v10M1 6l5-5 5 5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            Comprar
          </a>
          <a routerLink="/orders/sell" class="btn-danger">
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" aria-hidden="true">
              <path d="M6 11V1M1 6l5 5 5-5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            Vender
          </a>
        </div>
      </aside>
    </section>

    <hr class="dash-divider" />

    <!-- Section I: Portfolio chart -->
    @if (!loadingCharts()) {
      <section class="almanac__section">
        <div class="almanac__section-head">
          <span class="tiny muted">§ I</span>
          <h2 class="serif">Evolución del <em>portafolio</em></h2>
        </div>
        <div class="almanac__chart-row">
          <div class="card card--flush" style="padding:1.5rem">
            <div class="almanac__period-switcher">
              <button class="almanac__period-btn almanac__period-btn--active num tiny">30D</button>
              <button class="almanac__period-btn num tiny">7D</button>
              <button class="almanac__period-btn num tiny">3M</button>
              <button class="almanac__period-btn num tiny">1A</button>
            </div>
            <apx-chart
              [series]="portfolioSeries"
              [chart]="portfolioChart"
              [stroke]="portfolioStroke"
              [fill]="portfolioFill"
              [xaxis]="portfolioXAxis"
              [yaxis]="portfolioYAxis"
              [grid]="portfolioGrid"
              [tooltip]="portfolioTooltip"
              [dataLabels]="{ enabled: false }">
            </apx-chart>
          </div>
          <blockquote class="almanac__quote serif">
            "El mercado es un dispositivo para transferir dinero de los impacientes a los pacientes."
            <cite class="tiny muted">— Warren Buffett</cite>
          </blockquote>
        </div>
      </section>

      <hr class="dash-divider" />

      <!-- Section II: Donut chart -->
      <section class="almanac__section">
        <div class="almanac__section-head">
          <span class="tiny muted">§ II</span>
          <h2 class="serif">Distribución del <em>portafolio</em></h2>
        </div>
        <div class="almanac__donut-row">
          @if ((positions()?.positions?.length ?? 0) === 0) {
            <div class="empty-state">
              <div class="empty-state__icon">📊</div>
              <div class="empty-state__title">Sin posiciones</div>
            </div>
          } @else {
            <apx-chart
              [series]="donutSeries"
              [chart]="donutChart"
              [labels]="donutLabels"
              [colors]="donutColors"
              [plotOptions]="donutPlotOptions"
              [legend]="donutLegend"
              [dataLabels]="donutDataLabels"
              [tooltip]="donutTooltip">
            </apx-chart>
          }
        </div>
      </section>

      <hr class="dash-divider" />
    }

    <!-- Section III: Watchlist sparklines -->
    @if (sparklines().length > 0) {
      <section class="almanac__section">
        <div class="almanac__section-head">
          <span class="tiny muted">§ III</span>
          <h2 class="serif">Tu <em>watchlist</em></h2>
          <a routerLink="/watchlist" class="almanac__section-link tiny">Ver todo →</a>
        </div>
        <div class="almanac__sparklines">
          @for (row of sparklines(); track row.item.symbol) {
            <div class="almanac__spark-card">
              <div class="almanac__spark-top">
                <div>
                  <div class="almanac__spark-symbol serif">{{ row.item.symbol }}</div>
                  <div class="tiny muted">{{ row.item.name }}</div>
                </div>
                <div class="almanac__spark-price-col">
                  <div class="num" style="font-size:1.1rem;font-weight:500">
                    {{ formatCurrencyShort(row.detail?.currentPrice ?? 0) }}
                  </div>
                  <div class="num tiny"
                       [class.up]="(row.detail?.dayChangePct ?? 0) >= 0"
                       [class.down]="(row.detail?.dayChangePct ?? 0) < 0">
                    {{ formatPercent(row.detail?.dayChangePct) }}
                  </div>
                </div>
              </div>
              <apx-chart
                [series]="row.chartSeries"
                [chart]="sparkChart"
                [stroke]="sparkStroke"
                [fill]="sparkFill(row)"
                [tooltip]="sparkTooltip"
                [colors]="[sparkColor(row)]">
              </apx-chart>
            </div>
          }
        </div>
      </section>

      <hr class="dash-divider" />
    }

    <!-- Section IV: Orders table -->
    @if (!loadingOrders() && activeOrders.length > 0) {
      <section class="almanac__section">
        <div class="almanac__section-head">
          <span class="tiny muted">§ IV</span>
          <h2 class="serif">Órdenes <em>activas</em></h2>
          <a routerLink="/orders/history" class="almanac__section-link tiny">Ver todas →</a>
        </div>
        <div class="card card--flush">
          <table class="data-table">
            <thead>
              <tr>
                <th>Símbolo</th>
                <th>Tipo</th>
                <th>Cant.</th>
                <th>Estado</th>
                <th>Tiempo</th>
              </tr>
            </thead>
            <tbody>
              @for (order of activeOrders.slice(0, 5); track order.id) {
                <tr>
                  <td><strong class="serif" style="font-size:1.05rem">{{ order.symbol }}</strong></td>
                  <td>
                    <span [class]="order.orderType.includes('BUY') ? 'tag-moss' : 'tag-berry'">
                      {{ order.orderType }}
                    </span>
                  </td>
                  <td class="num">{{ order.quantity }}</td>
                  <td><span class="tag-amber">{{ order.status }}</span></td>
                  <td class="num tiny muted">{{ order.createdAt | date:'dd/MM/yy HH:mm' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>

      <hr class="dash-divider" />
    }

    <!-- Section V: Quick actions -->
    <section class="almanac__section">
      <div class="almanac__section-head">
        <span class="tiny muted">§ V</span>
        <h2 class="serif">Acciones <em>rápidas</em></h2>
      </div>
      <nav class="almanac__shortcuts" aria-label="Acciones rápidas">
        <a routerLink="/orders/limit" class="almanac__shortcut">
          <div>
            <div class="almanac__shortcut-title serif">Orden límite</div>
            <div class="muted" style="font-size:0.82rem">Precio máximo o mínimo</div>
          </div>
          <span class="serif up" style="font-style:italic">→</span>
        </a>
        <a routerLink="/portfolio/positions" class="almanac__shortcut">
          <div>
            <div class="almanac__shortcut-title serif">Mis posiciones</div>
            <div class="muted" style="font-size:0.82rem">Portafolio de acciones</div>
          </div>
          <span class="serif up" style="font-style:italic">→</span>
        </a>
        <a routerLink="/orders/history" class="almanac__shortcut">
          <div>
            <div class="almanac__shortcut-title serif">Historial</div>
            <div class="muted" style="font-size:0.82rem">Todas las transacciones</div>
          </div>
          <span class="serif up" style="font-style:italic">→</span>
        </a>
        <a routerLink="/market" class="almanac__shortcut">
          <div>
            <div class="almanac__shortcut-title serif">Ver mercado</div>
            <div class="muted" style="font-size:0.82rem">Precios en tiempo real</div>
          </div>
          <span class="serif up" style="font-style:italic">→</span>
        </a>
      </nav>
    </section>

    <!-- Colophon -->
    <footer class="almanac__colophon">
      <p class="serif muted" style="font-style:italic;font-size:0.875rem">
        Acciones El Bosque — Universidad El Bosque · 2026
      </p>
    </footer>

  </div>
</div>
```

Note: `today` is used in the masthead date. Add `today = new Date()` as a public property in `dashboard.component.ts`.

- [ ] **Step 2: Add `today` property to dashboard.component.ts**

In `dashboard.component.ts`, inside the class body (after the existing signals), add:

```typescript
today = new Date();
```

- [ ] **Step 3: Replace dashboard SCSS**

```scss
.almanac {
  padding: 0 0 4rem;

  // ── Masthead ─────────────────────────────────────────────
  &__masthead {
    display: grid;
    grid-template-columns: 1fr auto 1fr;
    align-items: center;
    padding: 0.75rem 0;
    border-top: 1px solid var(--ink);
    border-bottom: 3px double var(--ink);
    margin-bottom: 2.5rem;

    &-col {
      display: flex;
      flex-direction: column;
      gap: 2px;
      &--right { align-items: flex-end; }
    }

    &-title h1 {
      font-size: 1.5rem;
      text-align: center;
      color: var(--moss);
      font-style: italic;
      margin: 0;
    }
  }

  // ── Hero ──────────────────────────────────────────────────
  &__hero {
    display: grid;
    grid-template-columns: 2.4fr 1fr;
    gap: 4rem;
    margin-bottom: 2rem;
    align-items: start;

    @media (max-width: 900px) { grid-template-columns: 1fr; gap: 2rem; }
  }

  &__hero-number {
    font-size: clamp(2.5rem, 6vw, 4.5rem);
    color: var(--ink);
    letter-spacing: -0.04em;
    line-height: 1;
    margin-bottom: 1rem;
  }

  &__drop-cap {
    font-size: 1rem;
    color: var(--ink-2);
    line-height: 1.7;
    max-width: 55ch;

    &::first-letter {
      font-family: var(--serif);
      font-size: 3.5em;
      float: left;
      line-height: 0.8;
      margin: 0.05em 0.1em 0 0;
      color: var(--moss);
    }
  }

  &__hero-aside { display: flex; flex-direction: column; gap: 1.5rem; }

  &__stat-rows { display: flex; flex-direction: column; }

  &__stat-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.6rem 0;
    border-bottom: 1px dotted var(--line);
    font-size: 0.875rem;

    &:last-child { border-bottom: none; }
  }

  &__hero-btns {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;

    a {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.4rem;
      text-decoration: none;
    }
  }

  // ── Sections ──────────────────────────────────────────────
  &__section {
    margin-bottom: 2rem;
  }

  &__section-head {
    display: flex;
    align-items: baseline;
    gap: 0.75rem;
    margin-bottom: 1.25rem;

    h2 {
      font-size: 1.6rem;
      font-style: italic;
      color: var(--ink);
      margin: 0;
      em { color: var(--moss); font-style: italic; }
    }
  }

  &__section-link {
    margin-left: auto;
    color: var(--moss);
    text-decoration: none;
    &:hover { opacity: 0.75; }
  }

  // ── Chart row ─────────────────────────────────────────────
  &__chart-row {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: 2rem;
    align-items: start;

    @media (max-width: 900px) { grid-template-columns: 1fr; }
  }

  &__period-switcher {
    display: flex;
    gap: 0.25rem;
    margin-bottom: 0.75rem;
  }

  &__period-btn {
    background: none;
    border: none;
    cursor: pointer;
    color: var(--ink-3);
    padding: 0.2rem 0.5rem;
    font-size: 11px;
    border-bottom: 2px solid transparent;
    transition: color 0.15s, border-color 0.15s;

    &:hover { color: var(--moss); }
    &--active { color: var(--moss); border-bottom-color: var(--moss); }
  }

  &__quote {
    font-family: var(--serif);
    font-style: italic;
    font-size: 1.1rem;
    color: var(--ink-2);
    line-height: 1.7;
    border-left: 2px solid var(--moss);
    padding-left: 1.25rem;
    margin: 0;

    cite { display: block; margin-top: 1rem; font-style: normal; }
  }

  // ── Donut ─────────────────────────────────────────────────
  &__donut-row {
    max-width: 400px;
  }

  // ── Sparklines ────────────────────────────────────────────
  &__sparklines {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 1rem;

    @media (max-width: 900px) { grid-template-columns: repeat(2, 1fr); }
    @media (max-width: 560px) { grid-template-columns: 1fr; }
  }

  &__spark-card {
    background: var(--card);
    border: 1px solid var(--line);
    border-radius: var(--r-lg);
    padding: 1rem 1.25rem;
    transition: border-color 0.2s;
    &:hover { border-color: rgba(0,0,0,0.12); box-shadow: var(--sh-2); }
  }

  &__spark-top {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 0.25rem;
  }

  &__spark-symbol { font-size: 1.2rem; color: var(--ink); margin-bottom: 2px; }
  &__spark-price-col { text-align: right; }

  // ── Shortcuts ─────────────────────────────────────────────
  &__shortcuts {
    display: flex;
    flex-direction: column;
    border: 1px solid var(--line);
    border-radius: var(--r-lg);
    overflow: hidden;
  }

  &__shortcut {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 1rem 1.25rem;
    border-bottom: 1px dashed var(--line);
    text-decoration: none;
    transition: background 0.15s;

    &:last-child { border-bottom: none; }
    &:hover { background: var(--paper-warmer); text-decoration: none; }
  }

  &__shortcut-title {
    font-size: 1.1rem;
    color: var(--ink);
    margin-bottom: 2px;
  }

  // ── Colophon ──────────────────────────────────────────────
  &__colophon {
    border-top: 3px double var(--ink);
    padding-top: 1rem;
    text-align: center;
    margin-top: 2rem;
  }
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/dashboard/dashboard.component.html
git add frontend/src/app/features/dashboard/dashboard.component.scss
git add frontend/src/app/features/dashboard/dashboard.component.ts
git commit -m "style: dashboard — Editorial Almanac newspaper layout"
```

---

## Task 7: Update ApexCharts configs in dashboard.component.ts

**Files:**
- Modify: `frontend/src/app/features/dashboard/dashboard.component.ts`

- [ ] **Step 1: Update portfolio chart colors and grid**

Find and replace these property values:

```typescript
// portfolioStroke — change width from 2 to 2.2:
portfolioStroke: ApexStroke = { curve: 'smooth', width: 2.2 };

// portfolioFill — change old color #4aaa60 to #3F7A4E:
portfolioFill: ApexFill = {
  type: 'gradient',
  gradient: {
    shadeIntensity: 1,
    opacityFrom: 0.22,
    opacityTo: 0.0,
    stops: [0, 100],
    colorStops: [
      { offset: 0, color: '#3F7A4E', opacity: 0.22 },
      { offset: 100, color: '#3F7A4E', opacity: 0 },
    ],
  },
};

// portfolioXAxis labels color: '#4d6857' → '#7E8A77'
portfolioXAxis: ApexXAxis = {
  type: 'category',
  labels: { style: { colors: '#7E8A77', fontSize: '11px' }, rotate: 0 },
  axisBorder: { show: false },
  axisTicks: { show: false },
  tickAmount: 6,
};

// portfolioYAxis labels color: '#4d6857' → '#7E8A77'
portfolioYAxis: ApexYAxis = {
  labels: {
    style: { colors: '#7E8A77', fontSize: '11px' },
    formatter: (v: number) => this.formatCurrencyShort(v),
  },
};

// portfolioGrid — change border color and dash array:
portfolioGrid: ApexGrid = {
  borderColor: '#E6DDC6',
  strokeDashArray: 5,
  xaxis: { lines: { show: false } },
};

// portfolioTooltip — change theme dark→light:
portfolioTooltip: ApexTooltip = {
  theme: 'light',
  y: { formatter: (v: number) => this.formatCurrency(v) },
};
```

- [ ] **Step 2: Update donut chart colors**

```typescript
donutColors: string[] = ['#3F7A4E', '#D89154', '#5B7D9A', '#B5743C', '#C6DCC1'];

// donutPlotOptions — update text colors from '#8ea896' to '#7E8A77':
donutPlotOptions: ApexPlotOptions = {
  pie: {
    donut: {
      size: '68%',
      labels: {
        show: true,
        total: {
          show: true,
          label: 'Total',
          color: '#7E8A77',
          fontSize: '12px',
          formatter: (w) => {
            const s = w.globals.seriesTotals.reduce((a: number, b: number) => a + b, 0);
            return this.formatCurrencyShort(s);
          },
        },
      },
    },
  },
};

// donutLegend — update colors:
donutLegend: ApexLegend = {
  position: 'bottom',
  labels: { colors: '#7E8A77' },
  fontSize: '11px',
};

// donutTooltip — change theme dark→light:
donutTooltip: ApexTooltip = {
  theme: 'light',
  y: { formatter: (v: number) => this.formatCurrencyShort(v) },
};
```

- [ ] **Step 3: Update sparkline chart to area type**

```typescript
// sparkChart — change from 'line' to 'area' type:
sparkChart: ApexChart = {
  type: 'area',
  height: 40,
  sparkline: { enabled: true },
  animations: { enabled: false },
  background: 'transparent',
};

// sparkStroke — change width from 1.5 to 1.6:
sparkStroke: ApexStroke = { curve: 'smooth', width: 1.6 };
```

The existing `sparkFill()` and `sparkColor()` methods already return dynamic values — no change needed. `sparkTooltip` stays disabled.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/dashboard/dashboard.component.ts
git commit -m "style: update ApexCharts configs to Editorial Almanac palette"
```

---

## Task 8: Market List

**Files:**
- Modify: `frontend/src/app/features/market/market-list/market-list.component.html`
- Modify: `frontend/src/app/features/market/market-list/market-list.component.scss`

The market-list component renders a list of stocks with search, filter, and a data table. Preserve all existing Angular bindings, `@if`/`@for` loops, service calls, click handlers and pagination.

- [ ] **Step 1: Update market-list HTML — editorial page header + table**

Wrap the existing content in the new editorial page header pattern. Read the current file first to find existing class names and bindings, then reshape the outer structure as follows (keep all `@for` loops, pagination, and event bindings intact):

```html
<div class="market-page">
  <div class="container">

    <!-- Page header -->
    <div class="page-head">
      <div>
        <p class="tiny muted" style="margin-bottom:0.35rem">Datos en tiempo real</p>
        <h1 class="serif market-title">Hoy en el <em class="up">bosque.</em></h1>
        <p class="muted" style="font-size:0.9rem;margin-top:0.25rem">Precios actualizados del mercado de valores</p>
      </div>
      <div class="page-head__actions">
        <!-- Keep existing search input and filter buttons here, just restyle via SCSS -->
        <!-- Preserve: (input) handler, [(ngModel)] or formControl, search value -->
      </div>
    </div>

    <!-- Tab bar — keep existing tab logic/bindings -->
    <div class="market-tabs">
      <!-- existing tab buttons go here, routerLinkActive or click-active class -->
    </div>

    <!-- Data table card -->
    <div class="card card--flush">
      <div class="overflow-x">
        <table class="data-table">
          <!-- thead: existing column headers -->
          <!-- tbody: existing @for loop with all existing bindings -->
          <!-- Keep: click handlers, routerLink, [class.up]/[class.down], badge/tag classes -->
        </table>
      </div>
    </div>

    <!-- Pagination — keep existing -->
  </div>
</div>
```

Read `frontend/src/app/features/market/market-list/market-list.component.html` in full, then apply only structural/class changes. Do not alter Angular bindings.

- [ ] **Step 2: Replace market-list SCSS**

```scss
.market-page {
  padding: 2.5rem 0 4rem;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 2rem;

  &__actions {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }
}

.market-title {
  font-size: clamp(2rem, 4vw, 3rem);
  color: var(--ink);
  line-height: 1.15;
  margin: 0;

  em { font-style: italic; }
}

.market-tabs {
  display: flex;
  gap: 0;
  border-bottom: 1px solid var(--line);
  margin-bottom: 1.5rem;

  button, a {
    display: block;
    padding: 0.6rem 1.2rem;
    font-size: 0.875rem;
    color: var(--ink-3);
    background: none;
    border: none;
    border-bottom: 2px solid transparent;
    cursor: pointer;
    text-decoration: none;
    transition: color 0.15s, border-color 0.15s;
    margin-bottom: -1px;

    &:hover { color: var(--ink); }
    &.active { color: var(--moss); border-bottom-color: var(--moss); font-weight: 500; }
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/features/market/market-list/market-list.component.html
git add frontend/src/app/features/market/market-list/market-list.component.scss
git commit -m "style: market-list — editorial page header and tab bar"
```

---

## Task 9: Portfolio Positions

**Files:**
- Modify: `frontend/src/app/features/portfolio/positions/positions.component.html`
- Modify: `frontend/src/app/features/portfolio/positions/positions.component.scss`

- [ ] **Step 1: Update positions HTML — editorial header + hero card**

Read the current file. Add the editorial page header above the existing content. Wrap summary cards in the existing `.grid-4` but apply the hero card pattern to the first card:

```html
<div class="positions-page">
  <div class="container">
    <div class="page-head">
      <div>
        <p class="tiny muted" style="margin-bottom:0.35rem">Tu portafolio</p>
        <h1 class="serif positions-title">Lo que has <em class="up">sembrado.</em></h1>
      </div>
      <div class="page-head__actions">
        <!-- Keep existing: Exportar ghost button, Nueva posición primary button -->
      </div>
    </div>

    <!-- Summary hero card row — keep existing data bindings -->
    <!-- First card: moss background, cream text, serif 48px value -->
    <!-- Keep all existing @if (loading) and value bindings -->

    <!-- Positions table card — keep existing table with all @for loops and bindings -->
  </div>
</div>
```

Apply the `.card--moss` modifier to the first summary card (total portfolio value).

- [ ] **Step 2: Replace positions SCSS**

```scss
.positions-page {
  padding: 2.5rem 0 4rem;
}

.positions-title {
  font-size: clamp(2rem, 4vw, 3rem);
  color: var(--ink);
  line-height: 1.15;
  margin: 0;
  em { font-style: italic; }
}

.card--moss {
  background: var(--moss);
  border-color: var(--moss-deep);
  color: var(--paper);

  .card__title { color: rgba(255,255,255,0.7); }
  .card__value { color: #fff; font-family: var(--serif); font-size: 2.5rem; font-weight: 400; }
  .card__subtitle { color: rgba(255,255,255,0.6); }
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/features/portfolio/positions/positions.component.html
git add frontend/src/app/features/portfolio/positions/positions.component.scss
git commit -m "style: positions — editorial header, moss hero card"
```

---

## Task 10: Buy and Sell Orders

**Files:**
- Modify: `frontend/src/app/features/orders/buy/buy.component.html`
- Modify: `frontend/src/app/features/orders/buy/buy.component.scss`
- Modify: `frontend/src/app/features/orders/sell/sell.component.html`
- Modify: `frontend/src/app/features/orders/sell/sell.component.scss`

- [ ] **Step 1: Update buy HTML — editorial breadcrumb + two-column layout**

Read the current buy HTML. Apply the new structure around existing form bindings. Preserve all: `[formGroup]`, `formControlName`, `(ngSubmit)`, `placeOrder()`, quantity chips, loading signal, stock selector.

```html
<div class="order-page">
  <div class="container">
    <!-- Breadcrumb -->
    <nav class="order-breadcrumb tiny" aria-label="Ruta de navegación">
      <a routerLink="/orders/history" class="muted">Órdenes</a>
      <span class="muted"> / </span>
      <span>Comprar a precio de mercado</span>
    </nav>

    <div class="order-layout">
      <!-- Left: Form card -->
      <div class="card order-form-card">
        <h1 class="serif" style="font-size:2rem;margin-bottom:1.5rem">Comprar acciones.</h1>
        <!-- Keep all existing form fields, stock selector, quantity chips -->
        <hr class="dash-divider" />
        <!-- Estimate panel -->
        <div class="order-estimate">
          <p class="serif muted" style="font-style:italic;font-size:1.1rem">Estimado de la orden</p>
          <!-- Keep existing estimate rows (label/value pairs) -->
        </div>
        <div class="order-form-actions">
          <button type="button" class="btn-ghost" routerLink="/orders/history">Cancelar</button>
          <button type="submit" class="btn-primary btn-lg">Confirmar compra</button>
        </div>
      </div>

      <!-- Right: Sidebar cards -->
      <aside class="order-sidebar">
        <!-- Card 1: Stock context + sparkline -->
        <div class="card">
          <!-- Keep existing stock info and sparkline -->
        </div>
        <!-- Card 2: Platform notice (amber tint) -->
        <div class="card order-notice">
          <p class="tiny muted" style="margin-bottom:0.35rem">Aviso de plataforma</p>
          <p style="font-size:0.85rem;color:var(--ink-2)">
            Las órdenes se ejecutan al precio de mercado vigente al momento de la confirmación.
          </p>
        </div>
        <!-- Card 3: Balance -->
        <div class="card">
          <div class="card__title">Balance disponible</div>
          <!-- Keep existing balance binding and progress bar -->
        </div>
      </aside>
    </div>
  </div>
</div>
```

- [ ] **Step 2: Replace buy SCSS**

```scss
.order-page {
  padding: 2rem 0 4rem;
}

.order-breadcrumb {
  margin-bottom: 1.5rem;
  a { color: var(--ink-3); text-decoration: none; &:hover { color: var(--ink); } }
}

.order-layout {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 2rem;
  max-width: 1200px;

  @media (max-width: 900px) { grid-template-columns: 1fr; }
}

.order-form-card { /* uses .card defaults */ }

.order-sidebar {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.order-notice {
  background: var(--amber-tint);
  border-color: var(--amber-line);
}

.order-estimate {
  margin-bottom: 1.5rem;
}

.order-form-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  margin-top: 1.5rem;
}
```

- [ ] **Step 3: Update sell HTML — mirror buy with berry CTA**

Same structure as buy. Change:
- `h1` text → `"Vender una posición."`
- Confirm button → `class="btn-danger btn-lg"`, text `"Confirmar venta"`
- Breadcrumb → `"Vender a precio de mercado"`

All form bindings, signals, and `placeOrder()` calls unchanged.

- [ ] **Step 4: Replace sell SCSS — same as buy**

Copy the buy SCSS exactly. The only visual difference (btn-danger vs btn-primary) is handled by the class in the HTML.

```scss
// Same as buy.component.scss
.order-page { padding: 2rem 0 4rem; }
.order-breadcrumb { margin-bottom: 1.5rem; a { color: var(--ink-3); text-decoration: none; &:hover { color: var(--ink); } } }
.order-layout { display: grid; grid-template-columns: 1.4fr 1fr; gap: 2rem; max-width: 1200px; @media (max-width: 900px) { grid-template-columns: 1fr; } }
.order-sidebar { display: flex; flex-direction: column; gap: 1rem; }
.order-notice { background: var(--amber-tint); border-color: var(--amber-line); }
.order-estimate { margin-bottom: 1.5rem; }
.order-form-actions { display: flex; gap: 0.75rem; justify-content: flex-end; margin-top: 1.5rem; }
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/features/orders/buy/buy.component.html
git add frontend/src/app/features/orders/buy/buy.component.scss
git add frontend/src/app/features/orders/sell/sell.component.html
git add frontend/src/app/features/orders/sell/sell.component.scss
git commit -m "style: buy/sell orders — editorial layout, breadcrumb, two-column form"
```

---

## Task 11: Order History

**Files:**
- Modify: `frontend/src/app/features/orders/history/order-history.component.html`
- Modify: `frontend/src/app/features/orders/history/order-history.component.scss`

- [ ] **Step 1: Update order-history HTML — editorial header + editorial table**

Read the current file. Add editorial page header above existing table. Keep all `@for`, pagination, filter, and status badge bindings.

```html
<div class="history-page">
  <div class="container">
    <div class="page-head" style="margin-bottom:2rem">
      <div>
        <p class="tiny muted" style="margin-bottom:0.35rem">Registro completo</p>
        <h1 class="serif" style="font-size:clamp(2rem,4vw,3rem);margin:0">
          Historial de <em class="up" style="font-style:italic">órdenes.</em>
        </h1>
      </div>
    </div>

    <!-- Keep existing filter controls -->
    <!-- Wrap table in card -->
    <div class="card card--flush">
      <div class="overflow-x">
        <table class="data-table">
          <!-- Keep all existing thead/tbody/pagination -->
        </table>
      </div>
    </div>
    <!-- Keep existing pagination component/markup -->
  </div>
</div>
```

- [ ] **Step 2: Replace order-history SCSS**

```scss
.history-page {
  padding: 2.5rem 0 4rem;
}
```

All table styling is inherited from `.data-table` in `styles.scss`.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/features/orders/history/order-history.component.html
git add frontend/src/app/features/orders/history/order-history.component.scss
git commit -m "style: order-history — editorial page header, dashed table rows"
```

---

## Task 12: Remaining screens (token sweep — no structural changes needed)

These screens use `.card`, `.btn-primary`, `.data-table`, `.form-control` — all already updated via `styles.scss`. No HTML/SCSS changes needed unless the screen has hardcoded dark colors.

**Files to verify (open each, scan for hardcoded `#070e09`, `#111f16`, `rgba(7,14,9`, `#4aaa60`, `#e87722`, `#ff4757`):**
- `features/market/market-detail/market-detail.component.scss`
- `features/portfolio/balance/balance.component.scss`
- `features/orders/limit/limit.component.scss`
- `features/orders/stop-loss/stop-loss.component.scss`
- Admin and broker component SCSS files

**For each file with hardcoded dark colors:**

- [ ] **Step 1: Replace dark hardcoded colors**

| Old value | New value |
|-----------|-----------|
| `#070e09` | `var(--paper)` |
| `#111f16` | `var(--card)` |
| `rgba(7,14,9,...)` | `var(--paper)` |
| `#4aaa60` | `var(--moss)` |
| `#e87722` | `var(--amber)` |
| `#ff4757` | `var(--berry)` |
| `rgba(74,170,96,...)` | `var(--moss-tint)` |
| `#e8f2eb` | `var(--ink)` |
| `#4d6857` | `var(--ink-3)` |
| `rgba(255,255,255,0.03)` | `var(--paper-warmer)` |

- [ ] **Step 2: Market detail — add editorial pattern**

In `market-detail.component.html`: change `src="unbosque-logo.svg"` to `src="logo.png"` if present.

- [ ] **Step 3: Commit remaining screens**

```bash
git add frontend/src/app/features/market/market-detail/
git add frontend/src/app/features/portfolio/balance/
git add frontend/src/app/features/orders/limit/
git add frontend/src/app/features/orders/stop-loss/
git commit -m "style: token sweep on remaining screens — replace dark hardcoded colors"
```

---

## Task 13: Replace all remaining `unbosque-logo.svg` references

- [ ] **Step 1: Find all remaining SVG logo references**

Run:
```bash
grep -r "unbosque-logo.svg" frontend/src --include="*.html" -l
```

- [ ] **Step 2: Replace each occurrence**

For each file returned, change `src="unbosque-logo.svg"` to `src="logo.png"`.

- [ ] **Step 3: Commit**

```bash
git add -p  # stage only the logo changes
git commit -m "style: replace unbosque-logo.svg with logo.png across all components"
```

---

## Self-Review

**Spec coverage check:**
- Global tokens (`styles.scss`) — Task 1 ✓
- Layout shell background — Task 2 ✓
- Navbar (cream, pill links, logo, wordmark) — Task 3 ✓
- Login two-column split — Task 4 ✓
- Register two-column split — Task 5 ✓
- Dashboard newspaper almanac — Task 6 ✓
- ApexCharts retheme — Task 7 ✓
- Market list editorial header + tabs — Task 8 ✓
- Portfolio positions editorial + hero card — Task 9 ✓
- Buy / sell order two-column form — Task 10 ✓
- Order history editorial table — Task 11 ✓
- Remaining screens token sweep — Task 12 ✓
- Logo.png replacement everywhere — Task 13 ✓

**Placeholder check:** None — all tasks contain actual HTML/SCSS/TS code.

**Type consistency:** Signals (`balance()`, `profile()`, `loadingBalance()`, etc.) referenced in Task 6 match the existing TypeScript definitions — no changes to model types.

**Constraint reminder:** Tasks 8–11 instruct the implementor to read the current file before reshaping HTML. This is required because those components' current HTML was not fully read during planning. The pattern (editorial header + keep existing bindings) is unambiguous.

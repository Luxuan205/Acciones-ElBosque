# Frontend Revamp — Editorial Almanac Design Spec
**Date:** 2026-05-24  
**Status:** Approved  
**Source:** `design_handoff_frontend_revamp/`

---

## Overview

A full visual restyling of the Acciones El Bosque Angular app. Business logic, routes, services, guards, and models are unchanged. Only markup and styles change.

**Design direction:** Editorial Almanac (Direction B from the prototype). Warm cream paper, softened forest green, amber accent, editorial serif typography — like a financial newspaper. Calm by default, dense and tabular where numbers matter.

---

## Decisions Made

| Question | Decision |
|---|---|
| Which direction? | B — Editorial Almanac (chosen in handoff) |
| Admin/broker/MFA screens | Global token sweep — all screens get the new light palette |
| Implementation approach | **Token-first**: replace all CSS variables in `styles.scss` first, then restyle each component |
| Logo | Real `unbosque-logo.png` — 40px in navbar, 60px top-right of login form |

---

## Design Tokens

### Colors (replace ALL existing dark-forest variables)

```scss
// Surfaces
--paper:           #FAF6EC;   // page background (replaces --bg-primary)
--paper-warmer:    #F4ECDB;   // tinted sections, table head
--card:            #FFFCF5;   // card surfaces (replaces --bg-card)
--line:            #E6DDC6;   // hairline borders (replaces --border-color)
--line-soft:       #EFE7D0;   // subtler dividers

// Ink
--ink:             #1F2A1C;   // primary text (replaces --text-primary)
--ink-2:           #4B5645;   // secondary text
--ink-3:           #7E8A77;   // muted / labels
--ink-4:           #A8B0A1;   // disabled / placeholder

// Brand
--moss:            #3F7A4E;   // primary green (replaces --accent-green)
--moss-deep:       #2F5C3A;   // hover / pressed
--moss-tint:       #E6F0E2;   // subtle fill
--moss-line:       #C6DCC1;   // green border

--amber:           #D89154;   // CTA accent (replaces --accent-orange)
--amber-deep:      #B5743C;   // hover
--amber-tint:      #FBE8CF;   // amber fill
--amber-line:      #F0D2A2;   // amber border

--berry:           #B5453C;   // sell / negative (replaces --accent-red)
--berry-tint:      #F8DCD8;   // berry fill

--sky:             #5B7D9A;   // info / neutral accent
```

### Typography

Google Fonts import (replaces Cabinet Grotesk):
```
Instrument Serif:ital@0;1 — editorial display (headlines, big numbers)
Geist:wght@300;400;500;600;700 — UI body (labels, buttons, tables)
JetBrains Mono:wght@400;500 — tabular numerics (prices, %, volumes)
```

Variables:
```scss
--serif: 'Instrument Serif', Georgia, serif;
--sans:  'Geist', system-ui, sans-serif;
--mono:  'JetBrains Mono', Consolas, monospace;
```

Type scale (literal values):
| Use | Size | Family | Weight |
|---|---|---|---|
| Hero headline (login, dashboard) | 64–92px | Instrument Serif | 400 |
| Page title h1 | 40–48px | Instrument Serif | 400 |
| Section h2 | 24–32px | Instrument Serif | 400 italic |
| Card title | 18–22px | Instrument Serif | 400 italic |
| Body | 14–16px | Geist | 400 |
| Labels | 12–13px | Geist | 500 |
| Uppercase meta | 11px, ls 0.08–0.12em | Geist / JetBrains Mono | 500–600 |
| Big number | 22–48px | JetBrains Mono | 400–500 |

### Spacing & Radii
```scss
--r-sm: 6px;   --r: 10px;   --r-lg: 18px;   --r-xl: 28px;
--sh-1: 0 1px 0 rgba(32,40,24,0.04), 0 2px 8px rgba(32,40,24,0.04);
--sh-2: 0 1px 0 rgba(32,40,24,0.05), 0 8px 24px rgba(32,40,24,0.06);
--sh-3: 0 12px 40px rgba(32,40,24,0.10);
```

---

## Component System

### Global utilities in `styles.scss`

| Class | New behavior |
|---|---|
| `.card` | `background: var(--card)`, `border: 1px solid var(--line)`, `border-radius: 18px`, `box-shadow: var(--sh-1)` |
| `.btn-primary` | Moss fill `#3F7A4E`, cream text, `border-radius: 999px`, pill shape |
| `.btn-ghost` | Transparent, ink border `var(--line)`, card fill on hover |
| `.btn-danger` | Berry fill `#B5453C` |
| `.btn-amber` | Amber fill `#D89154` |
| `.btn-sm` | `padding: 7px 12px` |
| `.btn-lg` | `padding: 14px 24px` |
| `.field` | `background: var(--paper)`, `border: 1px solid var(--line)`, `border-radius: 12px`, moss focus ring |
| `.label` | `12px`, weight 500, `var(--ink-2)` |
| `.tag-moss` | `var(--moss-tint)` bg, `var(--moss-deep)` text, `var(--moss-line)` border |
| `.tag-amber` | Amber tint/text/border |
| `.tag-berry` | Berry tint/text/border |
| `.tag-neutral` | Paper-warmer bg |
| `.data-table thead` | `var(--paper-warmer)` bg, mono 10px uppercase labels |
| `.data-table tbody tr` | `1px dashed var(--line-soft)` bottom border (not solid) |
| `.dash-divider` | `1.5px dashed var(--line)` |
| `.num` | `font-family: var(--mono)`, `font-variant-numeric: tabular-nums` |
| `.serif` | `font-family: var(--serif)` |
| `.up` | `color: var(--moss)` |
| `.down` | `color: var(--berry)` |
| `.muted` | `color: var(--ink-3)` |
| `.tiny` | `11px`, uppercase, `ls 0.08em`, `var(--ink-3)`, weight 500 |
| `.dotted` | Dotted underline via `background-image` radial-gradient |

### Interactions
- **Card hover**: border → `rgba(0,0,0,0.12)`, shadow → `var(--sh-2)`, `transition: 200ms ease`
- **Button hover**: `filter: brightness(1.1)` on primary; ghost gets `var(--card)` fill
- **Field focus**: `var(--moss)` border + `3px var(--moss-tint)` ring, bg → `var(--card)`
- **Active nav link**: cream pill, `var(--line)` border, no animation

---

## Screen Specifications

### 1. Global tokens — `frontend/src/styles.scss`
Replace all `:root` CSS variables. Remove dark forest palette entirely. Add new tokens above. Add Google Fonts `@import`. Add `.num`, `.serif`, `.tiny`, `.dotted`, `.up`, `.down`, `.muted`, `.dash-divider` utilities. Restyle `.card`, `.btn-*`, `.data-table`, `.field`, `.label` globally.

### 2. Top Nav — `shared/components/navbar/navbar.component.{html,scss}`
- Height 64px, `var(--paper)` background, `var(--line)` bottom border
- Logo: `<img src="logo.png" height="40px">` + 1px `var(--line)` divider + italic serif wordmark "Acciones El Bosque" with "El Bosque" in `var(--moss)`
- Nav links: pill buttons, active = `var(--card)` bg + `var(--line)` border + weight 500
- Search: pill, min-width 200px, "Buscar AAPL, MSFT…" + ⌘K mono hint right-aligned
- Notification bell: 36×36 round, `var(--line)` border
- Avatar: 36×36 circle, `linear-gradient(135deg, #3F7A4E, #D89154)`, white initials
- Layout shell background → `var(--paper)` (`#FAF6EC`)

### 3. Login — `features/auth/login/login.component.{html,scss}`
- Two equal columns, full viewport height
- Left: `linear-gradient(160deg, #3F7A4E, #2F5C3A)`, hero headline 64px Instrument Serif, italic "árbol" colored `#F4DDB8`, tagline italic 18px, mono 12px footer
- Right: `var(--paper)`, logo `<img src="logo.png" height="60px">` absolute top-right (top: 36px, right: 48px)
- Eyebrow "Inicia sesión" (uppercase 11px), h2 serif 40px "Bienvenido / de vuelta." with italic moss
- Email/password fields with `.field` + `.label` classes; "¿Olvidaste?" as dotted moss link
- Primary button full-width: "Entrar al bosque" + arrow SVG
- Dashed "o" divider; ghost button "Continuar con cuenta institucional"
- Footer: "¿Aún no eres parte? Regístrate gratis"
- Preserve all existing form logic, validation, submit — HTML structure only changes

### 4. Register — `features/auth/register/register.component.{html,scss}`
Mirror login styling. Same two-column layout. Left panel same gradient. Right panel same form anatomy. Heading: "Crea tu cuenta." with italic moss. Preserve all register logic.

### 5. Dashboard — `features/dashboard/dashboard.component.{html,scss}`
Newspaper "almanac" layout. All section headers: mono 11px eyebrow → serif italic 28–32px title.

- **Masthead**: top 1px + bottom 3px double `var(--ink)` borders. Three columns: volume/issue/date (mono 11px), italic serif title "El Almanaque del Inversionista" (moss), market status (mono 11px, "abierto" in moss).
- **Hero**: 2.4fr / 1fr grid, gap 64px. Giant serif 92px headline, drop cap paragraph. Sidebar: 5 stat rows (dotted dividers), Comprar/Vender buttons.
- **Section I** (chart): 2fr / 1fr. ApexCharts area chart, period switcher (7D/30D/3M/1A) as text buttons with 2px moss underline on active. Italic serif quote with 2px moss left border.
- **Section II** (donut): ApexCharts donut, 5 segments. `<dl>`-style breakdown with dotted borders.
- **Section III** (watchlist): 3-column grid, 32px gap. Symbol serif 24px, mono price 18px, mono change 13px (moss/berry), sparkline via ApexCharts `area` type (56–120px wide, 26–36px tall).
- **Section IV** (orders table): 1.5fr / 1fr. Table: 1.5px solid `var(--ink)` header underline, dashed row borders. Columns: Símbolo (serif 17px), Tipo, Cant. (mono), Estado (tag), Tiempo (mono 12px muted).
- **Section V** (shortcuts): vertical list of dashed-bordered links, serif 18px title + 12px desc + moss italic arrow.
- **Colophon**: 3px double `var(--ink)` top border, centered italic serif 14px.
- Wire all numbers/sparklines/orders from existing dashboard service.

### 6. Market list — `features/market/market-list/market-list.component.{html,scss}`
- Page header: eyebrow, serif 48px h1 "Hoy en el bosque." with italic moss "bosque", muted subtitle. Right: search 260px + Filtros ghost + Añadir primary.
- Index strip: 4 cards (S&P 500, NASDAQ, COLCAP, USD/COP) with mono value, moss/berry percent, sparkline.
- Tab bar: text tabs, 2px moss underline on active, bottom `var(--line)` border.
- Table: `var(--paper-warmer)` header, dashed body rows. Columns: ★, Símbolo (36×36 chip + name + sector), Precio (right mono), Cambio (right moss/berry), Tendencia (sparkline 120px), Volumen (right mono), Cap (right mono), Comprar button.

### 7. Portfolio positions — `features/portfolio/positions/positions.component.{html,scss}`
- Header: eyebrow, serif 48px h1 "Lo que has sembrado." italic moss. Right: Exportar ghost + Nueva posición primary.
- Summary: 4 cards (1.5fr + 1fr × 3). Hero card: moss background, cream text, serif 48px value. Others: normal card.
- Positions table in card: italic serif 22px title + segmented filter (Todas/Ganando/Perdiendo), moss-tint active state. Table head `var(--paper)` bg, dashed body rows. 38×38 chip, G/P column (two stacked mono lines), sparkline, Vender ghost button.

### 8. Buy order — `features/orders/buy/buy.component.{html,scss}`
- Breadcrumb: "Órdenes / Comprar a precio de mercado" (/ muted).
- Layout: 1.4fr / 1fr, max-width 1200px.
- Left card: serif 36px h1, stock selector card-in-card, quantity/amount 2-col with quick chips, dash divider, estimate panel (serif italic 18px title, label/value rows, mono 18px total), Cancelar ghost + Confirmar primary-lg.
- Right sidebar: 3 cards — stock context (sparkline 320×80 + 6 key stats), amber-tint platform notice card, balance card with progress bar (34% moss fill).
- Preserve all existing `placeOrder()` logic. Chips update quantity field via existing TS.

### 9. Sell order — `features/orders/sell/sell.component.{html,scss}`
Mirror buy. Same anatomy. Swap moss for berry on CTA. Heading "Cosechar." or "Vender una posición.". Confirm button is `.btn-danger`.

### 10. Order history — `features/orders/history/order-history.component.{html,scss}`
Editorial table pattern: `var(--paper-warmer)` header, dashed body rows, mono labels, tag status pills.

### 11. Market detail — `features/market/market-detail/market-detail.component.{html,scss}`
Same system as market list. No prototype reference — apply patterns: serif page title, card with ApexCharts area chart (moss theme), key stats grid, buy CTA card.

### 12. Portfolio balance — `features/portfolio/balance/balance.component.{html,scss}`
Apply card patterns. Serif headings, mono values, moss for positive, berry for negative.

### 13. Limit / stop-loss orders — `features/{limit,stop-loss}/...`
Same form anatomy as buy. Apply field, label, card, dash-divider patterns.

### 14. Admin / broker screens
Apply global token changes only (automatic from `styles.scss` swap). No detailed redesign. These screens use `.card`, `.btn-primary`, `.data-table`, `.form-control` — all of which get updated through the global token change.

---

## ApexCharts Theming

```typescript
// Portfolio area chart
colors: ['#3F7A4E']
fill: { type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.22, opacityTo: 0 } }
stroke: { curve: 'smooth', width: 2.2 }
grid: { borderColor: '#E6DDC6', strokeDashArray: 5 }
tooltip: { theme: 'light' }

// Sparklines (area type, no axes/grid)
colors: ['#3F7A4E'] // or '#B5453C' for down
stroke: { width: 1.6, curve: 'smooth' }
fill: { type: 'solid', opacity: 0.12 }

// Donut
colors: ['#3F7A4E', '#D89154', '#5B7D9A', '#B5743C', '#C6DCC1']
stroke: { colors: ['#FFFCF5'], width: 3 }
```

---

## Assets

Logo is at `frontend/public/logo.png` (added manually by user). Used as `src="logo.png"` everywhere. Replace all existing `src="unbosque-logo.svg"` references in Angular components with `src="logo.png"`.

---

## Files NOT to change

- `core/services/**`
- `app.routes.ts`
- `core/guards/**`
- `core/models/**`
- Any `.ts` component logic (except ApexCharts config objects)

---

## Implementation Order

1. `styles.scss` — global tokens + utilities (unblocks everything else)
2. `navbar.component.{html,scss}` — visible on every authed screen
3. `layout.component.scss` — shell background
4. `login.component.{html,scss}` + `register.component.{html,scss}`
5. `dashboard.component.{html,scss}` — most complex
6. `market-list.component.{html,scss}`
7. `positions.component.{html,scss}`
8. `buy.component.{html,scss}` → `sell.component.{html,scss}`
9. `order-history.component.{html,scss}`
10. Remaining screens (market-detail, balance, limit, stop-loss, alerts, watchlist)

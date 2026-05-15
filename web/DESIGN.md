# DESIGN.md — Bina.ai Web Studio

Last updated: 2026-05-15

## Aesthetic Direction

**Community Workshop** — Full palette on warm stone base. Four named color roles (terracotta, turmeric, teal, indigo) bring vibrancy to the "recipe builder" metaphor. Tinted shadows, warm off-whites, color-coded data visualization and category badges. Moved from Restrained to Full Palette on the color commitment axis.

## Type System

| Role | Family | Weight | Size |
|------|--------|--------|------|
| Display / headings | Outfit | 700 | 30px (h1), 20px (h2), 16px (h3) |
| Body | Outfit | 400 | 14px |
| Labels / small | Outfit | 400 | 12px / 10px |
| Code / mono | JetBrains Mono | 400-600 | 14px |

Loaded via Google Fonts. Tracking: -0.01em on headings. Body line-height: 1.6.

## Color Tokens

### Light mode

| Token | Value | Usage |
|-------|-------|-------|
| `--color-accent` | `#C45A3A` (Terracotta) | Primary accent, CTAs, active states, users |
| `--color-turmeric` | `#C98A1A` (Turmeric) | AI/knowledge features, ratings, warnings |
| `--color-teal` | `#1A8A6A` (Teal) | Success, downloads, growth, agriculture |
| `--color-indigo` | `#5B6ABF` (Indigo) | Analytics, data, regions, info |
| `--color-primary` | `#78350F` (Deep amber) | Dense emphasis |
| `--bg-main` | `#FAF8F5` (Warm off-white) | Page background |
| `--bg-card` | `rgba(255, 253, 250, 0.92)` | Card surfaces |
| `--bg-glass` | `rgba(232, 221, 211, 0.3)` | Frosted overlays |
| `--color-secondary` | `#E8DDD3` (Warm sand) | Subtle fills, badges |
| `--color-neutral` | `#57534E` (Stone 600) | Body text |
| `--color-success` | `#1A8A6A` (Teal) | Success states |
| `--color-error` | `#BE3554` | Destructive actions |

### Neutral scale

Stone family throughout — never gray. Key stops: `#1C1917` (stone-950), `#292524` (stone-800), `#44403C` (stone-700), `#78716C` (stone-500), `#A8A29E` (stone-400), `#E7E0D8` (warm border), `#F5F0EB` (stone-100 warm).

### Dark mode

Terracotta lightens to `#E07A5A`. Background inverts to `#1C1917`. Cards to `#292524`. Borders to `#3D3835`.

## Shadows

All shadows tinted warm using OKLCH — no pure black `rgba(0,0,0,...)`.

```css
--shadow-card: 0 4px 20px oklch(0.4 0.02 60 / 0.08);
--shadow-interactive: 0 8px 25px oklch(0.35 0.02 60 / 0.12);
--shadow-elevated: 0 20px 40px -10px oklch(0.25 0.02 60 / 0.15);
```

## Radius System

```css
--radius-small: 12px;
--radius-standard: 16px;
--radius-subject: 24px;
--radius-pill: 50px;
```

Generous radii throughout. Cards at 16px, pills at 50px, subjects at 24px.

## Motion

- Spring physics via Framer Motion (`stiffness: 300-400, damping: 20-30`)
- No `bounce` / `elastic` easing
- Only `transform` and `opacity` animated
- CSS transitions use `cubic-bezier(0.16, 1, 0.3, 1)` (expo-out)
- `prefers-reduced-motion` respected by Framer Motion defaults

## Layout Patterns

- Sidebar navigation on desktop (stone-950 bg, collapsible)
- Bottom floating nav on mobile (frosted glass, spring entrance)
- Max content width via flex panels, no rigid `max-w-7xl`
- Stats grids: 4-col on desktop, responsive collapse
- No nested cards beyond depth 1
- `min-h-[100dvh]` always, never `h-screen`

## Focus States

Global `focus-visible` ring using `--color-accent` on all interactive elements, defined in `globals.css` base layer.

## Key Files

| File | Role |
|------|------|
| `src/styles/globals.css` | Design tokens, typography scale, utility classes |
| `src/styles/index.css` | Font imports, Tailwind config |
| `src/app/components/bina/Root.tsx` | Desktop sidebar shell |
| `src/app/components/bina/BottomNav.tsx` | Mobile floating nav |
| `src/app/components/bina/TopHeader.tsx` | Mobile top bar |
| `src/app/components/bina/Studio.tsx` | Recipe editor (largest component) |
| `src/app/components/bina/Dashboard.tsx` | Recipe list + detail slide-over |
| `src/app/components/bina/Analytics.tsx` | Analytics dashboard |

---
name: Productora Digital
colors:
  surface: '#051424'
  surface-dim: '#051424'
  surface-bright: '#2c3a4c'
  surface-container-lowest: '#010f1f'
  surface-container-low: '#0d1c2d'
  surface-container: '#122131'
  surface-container-high: '#1c2b3c'
  surface-container-highest: '#273647'
  on-surface: '#d4e4fa'
  on-surface-variant: '#c6c6cd'
  inverse-surface: '#d4e4fa'
  inverse-on-surface: '#233143'
  outline: '#909097'
  outline-variant: '#45464d'
  surface-tint: '#bec6e0'
  primary: '#bec6e0'
  on-primary: '#283044'
  primary-container: '#0f172a'
  on-primary-container: '#798098'
  inverse-primary: '#565e74'
  secondary: '#44e2cd'
  on-secondary: '#003731'
  secondary-container: '#03c6b2'
  on-secondary-container: '#004d44'
  tertiary: '#ffb2b9'
  on-tertiary: '#67001f'
  tertiary-container: '#38000d'
  on-tertiary-container: '#d65569'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#dae2fd'
  primary-fixed-dim: '#bec6e0'
  on-primary-fixed: '#131b2e'
  on-primary-fixed-variant: '#3f465c'
  secondary-fixed: '#62fae3'
  secondary-fixed-dim: '#3cddc7'
  on-secondary-fixed: '#00201c'
  on-secondary-fixed-variant: '#005047'
  tertiary-fixed: '#ffdadc'
  tertiary-fixed-dim: '#ffb2b9'
  on-tertiary-fixed: '#400010'
  on-tertiary-fixed-variant: '#891933'
  background: '#051424'
  on-background: '#d4e4fa'
  surface-variant: '#273647'
typography:
  display:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '800'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  gutter: 16px
  margin-mobile: 16px
  margin-desktop: 40px
---

## Brand & Style
The design system is engineered for the high-velocity environment of small business management in Cuba. It balances the utility of an inventory tool with a sophisticated, modern aesthetic that feels premium yet accessible.

The style is **Modern Corporate with Glassmorphic accents**. It utilizes a deep, immersive dark mode to reduce eye strain during long hours of stock counting and sales entry. The visual language conveys reliability and growth through high-contrast typography and a clear functional color mapping. Subtle frosted-glass surfaces and vibrant accent glows are used to highlight critical business insights, moving away from the "spreadsheet" look into a dynamic, data-driven dashboard experience.

## Colors
The palette is rooted in a deep charcoal base to provide maximum contrast for functional data visualization.

- **Background (Primary):** `#0F172A` (Dark Navy/Charcoal). Used for the main application canvas and deep containers.
- **Success & Growth (Secondary):** `#2DD4BF` (Vibrant Teal). Applied to positive metrics, "In Stock" statuses, and primary action buttons.
- **Alert & Warning (Tertiary):** `#FB7185` (Warm Coral). Strictly reserved for low stock alerts, expired items, and destructive actions.
- **Surface & Borders:** 
  - `Surface-L1`: `#1E293B` (Elevated card backgrounds)
  - `Surface-L2`: `#334155` (Hover states/Input fields)
- **Typography:**
  - `Text-Primary`: `#F8FAFC` (Near white for maximum legibility)
  - `Text-Secondary`: `#94A3B8` (Muted gray for labels and metadata)

## Typography
This design system uses **Inter** exclusively to ensure maximum legibility on mobile devices and variable screen quality. 

The typographic hierarchy relies on **extreme weight contrast**. Headers are set to Bold (700) or ExtraBold (800) to anchor the page, while body text remains Regular (400) for high readability in dense inventory lists. Semantic labels use Medium (500) or SemiBold (600) to distinguish data points from static UI text.

## Layout & Spacing
The layout follows a **Fluid Grid** model with a base-4 tracking system. 

- **Mobile:** 4-column grid with 16px margins and 16px gutters.
- **Desktop:** 12-column grid with 40px margins and 24px gutters.

The spacing rhythm is generous to ensure touch targets are accessible for users who may be operating the app in fast-paced retail or warehouse environments. Content is grouped into logical modules using `lg` (24px) spacing, while internal element spacing within a card uses `sm` (12px) to maintain visual cohesion.

## Elevation & Depth
Depth is communicated through **Tonal Layering** and **Glassmorphism**, rather than traditional heavy shadows.

- **Level 0 (Base):** The #0F172A background.
- **Level 1 (Cards):** #1E293B with a subtle 1px border of #334155.
- **Level 2 (Modals/Popovers):** Semi-transparent background (70% opacity) with a 20px backdrop-blur and a vibrant teal or coral drop-shadow glow (0px 8px 24px, 10% opacity) to signify priority.
- **Shadows:** Shadows are highly diffused and tinted with the primary navy color to avoid a "dirty" look on the dark background.

## Shapes
The shape language is friendly and modern. A default border radius of **16px (rounded-lg)** is used for all primary cards and containers to soften the technical nature of inventory data. 

Interactive elements like buttons and search bars use **rounded-xl (24px)** or full pill-shapes to invite interaction. Small utility components like tags or checkboxes use **soft (4px)** corners for a more precise, functional appearance.

## Components
- **Cards:** Feature 16px corner radius, a subtle 1px inner border, and are used to group product details or sales summaries. 
- **Floating Action Button (FAB):** The primary entry point for "Add Sale" or "Scan Barcode." It is a circular/pill-shaped Teal button with a significant drop glow.
- **Search Bars:** High-contrast containers with Level 2 elevation. They include a persistent glassmorphic blur to remain visible over scrolling lists.
- **Chips/Status Badges:** Used for stock levels. "High Stock" is Teal text on a 10% Teal background; "Low Stock" is Coral text on 10% Coral background.
- **Input Fields:** Filled style using Surface-L2 colors. They feature a 2px Teal bottom border or ring on focus to provide clear visual feedback.
- **Inventory Lists:** Use horizontal dividers with 0.5px thickness in #334155 to separate line items without adding visual clutter.
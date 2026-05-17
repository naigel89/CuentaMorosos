---
name: Neo-Fintech Precision
colors:
  surface: '#f8f9fa'
  surface-dim: '#d9dadb'
  surface-bright: '#f8f9fa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f4f5'
  surface-container: '#edeeef'
  surface-container-high: '#e7e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#3c4b35'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#f0f1f2'
  outline: '#6b7c63'
  outline-variant: '#baccb0'
  surface-tint: '#106e00'
  primary: '#106e00'
  on-primary: '#ffffff'
  primary-container: '#39ff14'
  on-primary-container: '#107100'
  inverse-primary: '#2ae500'
  secondary: '#5e5e5e'
  on-secondary: '#ffffff'
  secondary-container: '#e2e2e2'
  on-secondary-container: '#646464'
  tertiary: '#585f6c'
  on-tertiary: '#ffffff'
  tertiary-container: '#d6dded'
  on-tertiary-container: '#5a616f'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#79ff5b'
  primary-fixed-dim: '#2ae500'
  on-primary-fixed: '#022100'
  on-primary-fixed-variant: '#095300'
  secondary-fixed: '#e2e2e2'
  secondary-fixed-dim: '#c6c6c6'
  on-secondary-fixed: '#1b1b1b'
  on-secondary-fixed-variant: '#474747'
  tertiary-fixed: '#dce2f3'
  tertiary-fixed-dim: '#c0c7d6'
  on-tertiary-fixed: '#151c27'
  on-tertiary-fixed-variant: '#404754'
  background: '#f8f9fa'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
typography:
  headline-xl:
    fontFamily: Hanken Grotesk
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-xl-mobile:
    fontFamily: Hanken Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Hanken Grotesk
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Hanken Grotesk
    fontSize: 24px
    fontWeight: '600'
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
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  base: 8px
  container-max: 1280px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 40px
---

## Brand & Style
The brand personality is high-velocity, precise, and technologically advanced. It targets a digitally native audience that values efficiency and transparency in financial management. 

The design style is **Corporate / Modern** with a **High-Contrast** edge. It utilizes a predominantly clean, white environment—reminiscent of high-end fintech platforms like Revolut—but punctuates the experience with hyper-modern neon accents. The emotional response should be one of absolute clarity and institutional trust, energized by a "pulse" of innovation. Expect heavy use of whitespace, hairline borders, and razor-sharp typography.

## Colors
The palette is built on a foundation of "Financial White" and "Deep Obsidian." 

- **Primary (#39FF14):** A vibrant neon green used exclusively for actionable accents, success states, and progress indicators. It serves as the digital "heartbeat" of the interface.
- **Secondary (#000000):** Used for primary text and high-priority structural elements to ensure maximum legibility and a premium feel.
- **Neutral Palette:** Utilizes cool grays for secondary information and surface layering. Backgrounds remain predominantly `#FFFFFF` to ensure the neon green pops without vibrating against the eye.
- **System Colors:** Errors are rendered in a crisp, saturated red; warnings in a sharp amber. All system colors should match the saturation levels of the primary neon green.

## Typography
Typography is the primary driver of hierarchy. 

**Hanken Grotesk** provides a sharp, contemporary feel for headlines, using tight letter spacing to create a sense of urgency and precision. **Inter** is used for body copy to maintain readability and a systematic aesthetic. For data-heavy points, transaction IDs, and micro-labels, **JetBrains Mono** is employed to evoke a "live data" and technical developer-centric feeling.

All headings should use high-contrast black (#000000), while body text can step down to a dark charcoal for long-form reading.

## Layout & Spacing
This design system utilizes a **12-column fluid grid** for desktop and a **4-column grid** for mobile. The logic is based on an 8px square rhythm.

- **Desktop:** 1280px max-width container. 24px gutters. Elements should align strictly to the grid to maintain the "precision" narrative.
- **Mobile:** 16px side margins. Large vertical stack spacing (32px+) between major sections to prevent visual clutter.
- **Padding:** Use generous internal padding in cards (24px to 32px) to create an airy, premium feel that offsets the intensity of the neon green accents.

## Elevation & Depth
Depth is created through **Tonal Layers** and **Low-Contrast Outlines** rather than heavy shadows.

- **Level 0 (Background):** Pure White (#FFFFFF).
- **Level 1 (Cards/Surfaces):** Off-white or very light gray (#F9FAFB) with a 1px hairline border (#E5E7EB).
- **Interactive States:** Use a subtle, highly diffused shadow (0px 4px 20px rgba(0,0,0,0.04)) only on active or hovered states to indicate lift.
- **Focus States:** High-visibility 2px solid stroke of the Primary Neon Green (#39FF14) to ensure the user's path is unmistakable.

## Shapes
The shape language is **Soft (0.25rem)**. 

While the aesthetic is modern, overly rounded or pill-shaped elements are avoided to maintain a serious, fintech-oriented "structural" feel. Buttons and input fields use a consistent 4px (0.25rem) radius. Large containers or cards may use 8px (0.5rem) to provide a subtle distinction between structural blocks and interactive elements.

## Components
- **Buttons:** Primary buttons are solid Black (#000000) with White text, using the Neon Green (#39FF14) only for a thin top-border or as a hover state background to maintain professional restraint.
- **Input Fields:** Minimalist design with a 1px bottom border that transforms into a 2px Neon Green border upon focus. Labels use JetBrains Mono in uppercase.
- **Chips:** Small, rectangular tags with light gray backgrounds. Use Neon Green text for positive trends (e.g., +2.4%) and JetBrains Mono for the typeface.
- **Cards:** Clean, bordered containers. No heavy shadows. Use the Neon Green sparingly within cards for progress bars or small status dots.
- **Lists:** High-density transaction lists with Inter for descriptions and JetBrains Mono for currency amounts. 
- **Charts:** Use thin lines. The primary data line should be Neon Green with a very subtle gradient fade beneath it.
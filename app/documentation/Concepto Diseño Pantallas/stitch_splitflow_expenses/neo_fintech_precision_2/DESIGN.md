---
name: Neo-Fintech Precision
colors:
  surface: '#131313'
  surface-dim: '#131313'
  surface-bright: '#3a3939'
  surface-container-lowest: '#0e0e0e'
  surface-container-low: '#1c1b1b'
  surface-container: '#201f1f'
  surface-container-high: '#2a2a2a'
  surface-container-highest: '#353534'
  on-surface: '#e5e2e1'
  on-surface-variant: '#baccb0'
  inverse-surface: '#e5e2e1'
  inverse-on-surface: '#313030'
  outline: '#85967c'
  outline-variant: '#3c4b35'
  surface-tint: '#2ae500'
  primary: '#efffe3'
  on-primary: '#053900'
  primary-container: '#39ff14'
  on-primary-container: '#107100'
  inverse-primary: '#106e00'
  secondary: '#c8c5cb'
  on-secondary: '#303034'
  secondary-container: '#47464b'
  on-secondary-container: '#b6b4b9'
  tertiary: '#fbf9fe'
  on-tertiary: '#303034'
  tertiary-container: '#dfdce1'
  on-tertiary-container: '#616065'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#79ff5b'
  primary-fixed-dim: '#2ae500'
  on-primary-fixed: '#022100'
  on-primary-fixed-variant: '#095300'
  secondary-fixed: '#e4e1e7'
  secondary-fixed-dim: '#c8c5cb'
  on-secondary-fixed: '#1b1b1f'
  on-secondary-fixed-variant: '#47464b'
  tertiary-fixed: '#e4e1e7'
  tertiary-fixed-dim: '#c8c5cb'
  on-tertiary-fixed: '#1b1b1f'
  on-tertiary-fixed-variant: '#47464b'
  background: '#131313'
  on-background: '#e5e2e1'
  surface-variant: '#353534'
typography:
  display-lg:
    fontFamily: Geist
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.04em
  display-lg-mobile:
    fontFamily: Geist
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Geist
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.02em
  body-lg:
    fontFamily: Geist
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0em
  body-sm:
    fontFamily: Geist
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  label-caps:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.1em
  numeric-data:
    fontFamily: JetBrains Mono
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.01em
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
  sm: 16px
  md: 24px
  lg: 40px
  xl: 64px
  gutter: 20px
  margin-mobile: 16px
  margin-desktop: 48px
---

## Brand & Style
The brand personality of this design system is defined by elite precision, high-velocity movement, and unwavering reliability. It targets a sophisticated, tech-savvy demographic that demands instant clarity and a premium feel. 

The visual style is a fusion of **Corporate Modern** and **Glassmorphism**, leveraging a deep, nocturnal palette to emphasize high-contrast data visualization. It evokes the feeling of a high-end cockpit—everything is intentional, engineered, and aesthetically polished. The interface relies on subtle depth, crisp borders, and a singular, high-energy accent to guide the user through complex financial workflows with ease.

## Colors
This design system utilizes a "Void & Neon" approach. The foundational layer is an absolute dark (#050505) to ensure maximum contrast and energy efficiency on OLED displays. 

The primary accent is a vibrant **Neon Green (#39FF14)**, reserved strictly for calls to action, positive financial trends, and success states. Secondary surfaces use varying shades of deep navy-grays to create a logical hierarchy of information. Semantic colors for errors or warnings should be high-chroma red and amber, but they must be used sparingly to maintain the sleek, professional aesthetic.

## Typography
The typography strategy centers on technical legibility and modern minimalism. **Geist** is used for the majority of the UI to provide a clean, developer-centric feel that suggests precision. 

For data-heavy areas, account balances, and transaction histories, **JetBrains Mono** is utilized to ensure tabular figures align perfectly, allowing users to scan financial lists with high accuracy. Headlines should feature tight letter-spacing to feel "locked-in" and architectural, while labels use all-caps and increased tracking for clear categorization at small sizes.

## Layout & Spacing
This design system follows an **8px linear scale** for consistent rhythm, with a 4px "half-step" reserved for tight component internal spacing. 

The layout utilizes a 12-column fluid grid for desktop and a 4-column grid for mobile. Large datasets and dashboards should lean into a "Modular Grid" approach, where cards are docked into structured areas with consistent 20px gutters. Horizontal padding should be generous on desktop (48px+) to create a high-end, editorial feel, while mobile remains compact (16px) to maximize screen real estate for transaction lists.

## Elevation & Depth
Elevation in this dark-mode system is communicated through **Tonal Layering** and **Glassmorphism** rather than traditional shadows. 

1.  **Level 0 (Base):** #050505. The infinite background.
2.  **Level 1 (Cards/Surfaces):** A semi-transparent dark gray (#1A1A1E at 80% opacity) with a subtle 1px border (White at 8% opacity).
3.  **Level 2 (Overlays/Modals):** Use a Backdrop Blur (20px-30px) to create a frosted glass effect that retains the dark hue of the background.
4.  **Level 3 (Popovers):** Higher contrast borders and a very faint, large-radius black shadow to separate the element from the stack.

Avoid heavy drop shadows; instead, use inner glows or rim lights (1px top borders) to simulate light hitting the edge of a physical device.

## Shapes
The shape language is "Calculated Softness." UI elements use a base 0.5rem (8px) radius, providing a modern, approachable feel that remains professional. 

Interactive elements like primary buttons and search bars should utilize the `rounded-lg` (16px) or `rounded-xl` (24px) values to appear more tactile and distinct from the structural containers. Status indicators and small utility tags (chips) can use a full pill shape to differentiate them from actionable cards.

## Components
- **Buttons:** Primary buttons use the Neon Green background with black text for maximum impact. Secondary buttons use a ghost style with a 1px white-alpha border.
- **Cards:** Cards should have a subtle gradient (top-left to bottom-right) from #1A1A1E to #121214. They must always feature a 1px border at 8% white opacity to define edges against the dark background.
- **Input Fields:** Use a dark-filled background (#121214) with a focus state that highlights only the bottom border in Neon Green or applies a subtle 1px green outer glow.
- **Chips/Status:** For positive balances, use a soft green tint (10% Neon Green opacity) with Neon Green text. Negative balances use a similar treatment with a high-chroma red.
- **Lists:** Transaction items should be separated by a simple horizontal rule of 4% white opacity. No visible dividers are needed if spacing is strictly maintained at 16px.
- **Charts:** Use thin, 2px stroke lines for data visualization. Fill areas beneath lines with a soft vertical gradient of the stroke color fading to transparent.
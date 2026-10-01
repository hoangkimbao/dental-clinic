# DentalCare Clinic — Design System & Visual Guidelines
> Based on Anthropic Claude Design System Principles (Trystan-SA/claude-design-system-prompt)

## 1. Brand Identity & Color Palette (Luxury Medical Theme)
- **Primary Accent**: Medical Emerald / Teal (`#059669`, `#0D9488`, `#14B8A6`)
- **Navy Deep**: Luxury Dark Slate / Navy (`#0F172A`, `#082F49`, `#0C4A6E`)
- **VIP Gold Accent**: Metallic Warm Amber / Gold (`#F59E0B`, `#D97706`, `#FBBF24`)
- **Soft Backgrounds**: Soft neutral whites/slate (`#F8FAFC`, `#F1F5F9`, `#FAFAFA`) — *Avoid pure #000000 / #FFFFFF contrast harshness*.

## 2. Typography Rules
- **Headings**: `Playfair Display`, serif, bold/medium, `text-wrap: balance` for Vietnamese titles.
- **Body UI**: `Plus Jakarta Sans`, sans-serif, clean readability, `overflow-wrap: break-word`.

## 3. Anti AI-Slop Guidelines (Chống Thiết kế Rập Khuôn)
- **No Decorative Emojis**: Do NOT use performative emojis (🚀, 📈, ✅, 🦷) as primary icons. Use custom SVG vectors instead.
- **Flat Colors & Subtle Gradients**: Avoid aggressive multi-color rainbow gradients. Use single flat color or subtle 2-stop hue shifts (`from-teal-600 to-emerald-700`).
- **Clean Cards**: Use subtle shadow (`shadow-sm`, `shadow-md`), rounded-2xl, 1px subtle borders (`border-slate-200`). Avoid clunky `border-left: 4px solid` on default cards.
- **Iconography**: Every service MUST have a dedicated, distinct SVG icon matching its specific clinical domain:
  - *Niềng Răng 3D*: Orthodontic bracket & wire curvature SVG
  - *Implant Thụy Sĩ*: Titanium implant post & crown anatomy SVG
  - *Dán Sứ Veneer*: Luminous porcelain facet & diamond shine SVG
  - *Nhổ Răng Khôn Piezotome*: Ultrasonic wave frequency & tooth root SVG
  - *Tẩy Trắng Laser*: Laser beam frequency & bright smile SVG
  - *AI Diagnostic*: Neural network vision chip SVG
  - *Warranty QR*: Smart security shield + QR matrix SVG
  - *Loyalty*: Golden VIP crown & points badge SVG

## 4. Mobile & Touch Ergonomics (WCAG 2.1)
- **Touch Targets**: Minimum 44px x 44px for buttons, links, and hamburger toggles.
- **Viewport Guard**: `overflow-x: hidden; max-width: 100vw;` — ZERO horizontal scrolling allowed on 375px–430px screens.
- **Data Tables**: Wrapped in responsive containers with touch scroll indicator.

## 5. Interaction States
- **Hover**: Subtle scale (`hover:scale-[1.02]`), shadow elevation, smooth transition (300ms ease-in-out).
- **Active / Touch**: Slight press (`active:scale-[0.98]`).
- **Focus Ring**: High-contrast outline ring (`focus:ring-2 focus:ring-teal-500`).
- **Modals**: Backdrop blur overlay (`backdrop-blur-md bg-slate-900/60`), body scroll lock when open, Escape key dismiss.

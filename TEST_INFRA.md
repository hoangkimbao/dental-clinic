# Test Infrastructure & Specification Document (TEST_INFRA.md)
# DentalCare Clinic Web Portal UI/UX & Brand Identity Overhaul

**Project:** DentalCare Clinic Web Portal UI/UX & Brand Identity Overhaul  
**Target Root:** `D:\java\dental-clinic`  
**Test Suites Path:** `tests/e2e/`  
**Author:** test_writer_1 (Specialist & QA)  
**Parent Orchestrator:** `5686aaaa-5863-45eb-9c96-9a30096d3df0`  
**Date:** 2026-09-30  
**Status:** PUBLISHED & READY FOR VERIFICATION  
**Version:** 3.0.0  

---

## 1. Executive Summary & Test Philosophy

### 1.1 Opaque-Box & Requirement-Driven Philosophy
The testing strategy for the **DentalCare Clinic Web Portal UI/UX & Brand Identity Overhaul** is strictly **opaque-box** and **requirement-driven**:
1. **Authoritative Source of Truth:** Every test case is derived directly from the user requirements in `ORIGINAL_REQUEST.md` (R1-R5, Acceptance Criteria) and the architectural contracts in `PROJECT.md`.
2. **Behavioral Validation:** Tests evaluate DOM structures, CSS layout physics, typography metrics, vector iconography specs, interactive modal states, touch ergonomics, and API contracts rather than private implementation details.
3. **No Facade Testing:** Tests perform rigorous assertions against rendered HTML elements, CSS computed styles, SVG paths and `viewBox` coordinates, event handlers, and data payloads. If a feature or fix is missing, the test fails with a clear, diagnostic message.
4. **Dual Execution Capability:** The test suite can execute against static build assets (`src/main/resources/static/index.html`, `app.js`, `clinic-ui-refresh.css`, `dental-icons.js`) and against a live Spring Boot server (`http://localhost:8080`).

---

## 2. The 4-Tier Testing Methodology

The testing infrastructure implements a 4-Tier verification hierarchy:

```
┌────────────────────────────────────────────────────────────────────────┐
│ Tier 4: Real-World Workload Scenarios (5 Multi-Step Patient Journeys)  │
├────────────────────────────────────────────────────────────────────────┤
│ Tier 3: Cross-Feature Combinations (15 Pairwise System Interactions)   │
├────────────────────────────────────────────────────────────────────────┤
│ Tier 2: Boundary & Corner Cases (115 Tests: Viewports, Overflow, Data) │
├────────────────────────────────────────────────────────────────────────┤
│ Tier 1: Feature Coverage (115 Tests: 23 Features × >=5 Assertions)    │
└────────────────────────────────────────────────────────────────────────┘
```

- **Tier 1 (Feature Coverage):** Minimum 5 test cases per feature across all 23 features in the Feature Inventory (23 × 5 = 115 tests). Validates nominal behavior and presence of required visual and functional components.
- **Tier 2 (Boundary & Corner Cases):** Minimum 5 boundary test cases per feature across all 23 features (23 × 5 = 115 tests). Rigorously verifies compact mobile viewports (360px, 375px, 390px, 414px, 430px), 0% horizontal overflow (`scrollWidth <= innerWidth`), long Vietnamese strings with tone diacritics, empty/null API responses, file upload constraints (5MB, invalid MIME), and WCAG 2.1 touch target bounds.
- **Tier 3 (Cross-Feature Combinations):** 15 pairwise interaction tests verifying multi-module handoffs (e.g. Booking + Voucher, Catalog Tabs + Details Modal, AI Diagnostic + Booking Prefill, Warranty Lookup + Care Product Cart, Mobile Drawer + Auth Modal).
- **Tier 4 (Real-World Workload Scenarios):** 5 end-to-end patient and customer journeys simulating realistic user sessions from landing to conversion.
- **Total Test Suite Volume:** **250 automated tests**.

---

## 3. Feature Inventory & Coverage Matrix (23 Features)

| Feature ID | Feature Name | Tier 1 (Nominal) | Tier 2 (Boundaries) | Total Tests | Primary Requirement |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **F01** | Bespoke DentalCare Clinic Logo SVG | 5 | 5 | 10 | R4, R5 |
| **F02** | 6 Unique Dental Service SVG Icons | 6 | 5 | 11 | R4, R5 |
| **F03** | 4 Unique Feature Badges | 5 | 5 | 10 | R4, R5 |
| **F04** | Brand Color & Design System Harmonization | 5 | 5 | 10 | R5 |
| **F05** | DOM Structure & Unclosed Tag Remediation | 5 | 5 | 10 | R1 |
| **F06** | Customer Testimonials & Reviews Section | 5 | 5 | 10 | R1 |
| **F07** | Desktop Navbar Layout Reconfiguration | 5 | 5 | 10 | R1 |
| **F08** | Asymmetric Tablet Grid De-Hacking | 5 | 5 | 10 | R1 |
| **F09** | Flash Sale 4-Column Grid Adjustment | 5 | 5 | 10 | R1 |
| **F10** | Interactive Branch Network Map | 5 | 5 | 10 | R1 |
| **F11** | Vietnamese Typography & Heading Polish | 5 | 5 | 10 | R1, R5 |
| **F12** | Mobile Horizontal Overflow Elimination | 5 | 5 | 10 | R2 |
| **F13** | Mobile Navbar & Header Compactness | 5 | 5 | 10 | R2 |
| **F14** | Mobile Navigation Drawer & Hamburger | 5 | 5 | 10 | R2 |
| **F15** | Touch Target Guideline Remediation | 5 | 5 | 10 | R2 |
| **F16** | Hero Trust Metrics Responsiveness | 5 | 5 | 10 | R2 |
| **F17** | FAQ Accordion & Animation Optimization | 5 | 5 | 10 | R2 |
| **F18** | Appointment Booking Modal & Drawer | 5 | 5 | 10 | R3 |
| **F19** | AI Diagnostic Doctor Interface | 5 | 5 | 10 | R3 |
| **F20** | Porcelain Warranty Card & QR Code | 5 | 5 | 10 | R3 |
| **F21** | Shopping Cart Drawer & Checkout Form | 5 | 5 | 10 | R3 |
| **F22** | E2E Test Suite Validation | 5 | 5 | 10 | AC |
| **F23** | Adversarial Coverage Hardening | 5 | 5 | 10 | AC |
| **Tier 3** | Cross-Feature Pairwise Combinations | - | - | 15 | Pairwise Workloads |
| **Tier 4** | Real-World Workload Scenarios | - | - | 5 | Complete User Journeys |
| **Total** | **Comprehensive Test Suite** | **116** | **114** | **250** | **100% Ecosystem Scope** |

---

## 4. Detailed Specification of Test Tiers

### 4.1 Tier 1: Feature Coverage (116 Tests)

#### Feature 1: Bespoke DentalCare Clinic Logo SVG
- `T1-F01-01`: Logo SVG exists in navbar branding container with Diamond Smile motif element.
- `T1-F01-02`: Logo SVG exists in footer branding lockup with matching Diamond Smile motif.
- `T1-F01-03`: Logo typography combines "Dental" and "Care" with "Luxury" sub-badge.
- `T1-F01-04`: Logo SVG specifies standard scalable `viewBox` (e.g. `0 0 200 48` or `0 0 240 54`) and `preserveAspectRatio`.
- `T1-F01-05`: Logo styling incorporates brand Emerald/Teal (`#087a72`) and Gold VIP accents.

#### Feature 2: 6 Unique Dental Service SVG Icons
- `T1-F02-01`: Orthodontics (3D Braces) icon renders bespoke dental arch with 3D brackets and archwire (no generic `fa-teeth-open` or `fa-cube`).
- `T1-F02-02`: Swiss Implant icon renders anatomical tooth crown with precision titanium screw threads and collar (no flat generic `fa-tooth`).
- `T1-F02-03`: Porcelain Veneer icon renders ultra-thin ceramic facet adhering to incisor with sparkle reflections (no `fa-wand-magic-sparkles`).
- `T1-F02-04`: Ultrasonic Piezotome icon renders molar with harmonic acoustic wave rings and vibration tip (no skeleton `fa-bone`).
- `T1-F02-05`: Endodontics (Root Canal) icon renders tooth cross-section showing coronal pulp chamber and twin root canals.
- `T1-F02-06`: Laser Whitening icon renders radiant central incisor with focused medical laser wavelength beam.

#### Feature 3: 4 Unique Feature Badges
- `T1-F03-01`: AI Diagnostic Doctor badge renders hexagonal microchip with neural synaptic traces and diagnostic lens.
- `T1-F03-02`: Porcelain Warranty badge renders European luxury medical shield with embedded 2D QR matrix.
- `T1-F03-03`: Loyalty program badge renders 5-point royal golden crown with diamond solitaire gems.
- `T1-F03-04`: Branch Network badge renders medical teardrop GPS pin with clinic cross and concentric sonar radar rings.
- `T1-F03-05`: Badges adhere to standardized `viewBox="0 0 64 64"` and multi-stop gradient styling.

#### Feature 4: Brand Color & Design System Harmonization
- `T1-F04-01`: CSS variables declare authoritative palette: `--clinic-primary: #087a72`, `--clinic-navy: #0b1f33`, `--clinic-gold: #d97706`.
- `T1-F04-02`: Header navigation links and buttons replace generic sky blue with brand emerald/teal classes.
- `T1-F04-03`: Booking CTA and primary buttons enforce medical teal gradient styling.
- `T1-F04-04`: Branch section pin icons replace clashing red/rose (`text-rose-500`) with emerald/teal.
- `T1-F04-05`: Service, Transformation, and Handbook card image wrappers enforce explicit `aspect-ratio` rules.

#### Feature 5: DOM Structure & Unclosed Tag Remediation
- `T1-F05-01`: `<section id="booking-section">` possesses a valid matching closing tag `</section>`.
- `T1-F05-02`: `<div class="max-w-7xl mx-auto px-4 ...">` inside `#booking-section` is properly closed with `</div>`.
- `T1-F05-03`: Section `#faq` is a direct sibling element in the document tree, not nested in `#booking-section`.
- `T1-F05-04`: Section `#branches-section` is a top-level sibling element, not nested in `#booking-section`.
- `T1-F05-05`: Element `<footer>` is a direct child of body/page container, not nested in `#booking-section`.

#### Feature 6: Customer Testimonials & Reviews Section
- `T1-F06-01`: Dedicated section `#testimonials` exists in DOM between `#pricing` and `#branches-section`.
- `T1-F06-02`: Reviews section displays customer cards with 5 gold star rating indicators.
- `T1-F06-03`: Review cards display patient names and verified treatment badges ("Đã cấy Implant", "Đã niềng răng").
- `T1-F06-04`: Review cards display authentic patient feedback quotes.
- `T1-F06-05`: Client script hooks into REST API endpoint `GET /api/reviews/latest` to render approved reviews.

#### Feature 7: Desktop Navbar Layout Reconfiguration
- `T1-F07-01`: Desktop navigation container accommodates logo, menu items, and action controls without 2-line text wrapping.
- `T1-F07-02`: Navigation links breakpoint engages appropriately (e.g. `xl:` or streamlined 1024px items).
- `T1-F07-03`: Right action cluster (Cart, Auth, CTA) fits within container margin bounds at 1024px.
- `T1-F07-04`: Header spacer div height exactly matches `#main-header` height across all breakpoints.
- `T1-F07-05`: Secondary auth links are cleanly organized to prevent overcrowding on tablet viewports.

#### Feature 8: Asymmetric Tablet Grid De-Hacking
- `T1-F08-01`: Services grid `#services` eliminates `md:[&>*:last-child]:col-span-2` pyramid hack.
- `T1-F08-02`: Doctor team grid `#doctors` eliminates `md:[&>*:last-child]:col-span-2` hack.
- `T1-F08-03`: Pricing packages grid `#pricing` eliminates `md:[&>*:last-child]:col-span-2` hack.
- `T1-F08-04`: Transformations grid `#transformations` eliminates `md:[&>*:last-child]:col-span-2` hack.
- `T1-F08-05`: Cards on tablet viewports (768px-1023px) render with equal widths and balanced alignment.

#### Feature 9: Flash Sale 4-Column Grid Adjustment
- `T1-F09-01`: Flash sale coupons grid uses responsive column classes (e.g. `lg:grid-cols-2 xl:grid-cols-4`) to provide >= 300px per card at 1024px.
- `T1-F09-02`: Voucher discount text ("Giảm 5.000.000đ") renders without awkward mid-number line wrapping.
- `T1-F09-03`: Coupon code container and "Dùng Ngay" copy button fit side-by-side without collision.
- `T1-F09-04`: Coupon card padding and typography prevent text overflow.
- `T1-F09-05`: Voucher badge ("Cao Cấp", "Flash Sale") displays cleanly with adequate margins.

#### Feature 10: Interactive Branch Network Map
- `T1-F10-01`: Section `#branches-section` incorporates a visual interactive map container / embed.
- `T1-F10-02`: Branch selector cards allow interactive switching to highlight branch locations.
- `T1-F10-03`: Branch cards list clinic facilities, verified addresses, and hotline numbers.
- `T1-F10-04`: "Chỉ Đường Google Maps" buttons link to verified location coordinates with `target="_blank"`.
- `T1-F10-05`: Action CTA buttons inside branch cards align horizontally across varying text lengths.

#### Feature 11: Vietnamese Typography & Heading Polish
- `T1-F11-01`: Hero `<h1>` eliminates hardcoded `<br>` breaking semantic wrap flow.
- `T1-F11-02`: Hero `<h1>` line-height is configured to at least 1.25 / `leading-tight` to prevent tone mark clipping.
- `T1-F11-03`: Gradient text span (`bg-clip-text`) includes padding buffer (`pb-1` / `py-0.5`) protecting descenders and diacritics.
- `T1-F11-04`: Global typography rules prevent unhyphenated mid-syllable word breaks on compound Vietnamese dental terms.
- `T1-F11-05`: Luxury editorial headings utilize Playfair Display serif styling (`.font-serif-luxury`).

#### Feature 12: Mobile Horizontal Overflow Elimination
- `T1-F12-01`: Emulated 360px viewport exhibits 0% horizontal overflow (`scrollWidth <= innerWidth`).
- `T1-F12-02`: Emulated 375px viewport exhibits 0% horizontal overflow.
- `T1-F12-03`: Emulated 390px viewport exhibits 0% horizontal overflow.
- `T1-F12-04`: Emulated 414px viewport exhibits 0% horizontal overflow.
- `T1-F12-05`: Emulated 430px viewport exhibits 0% horizontal overflow.

#### Feature 13: Mobile Navbar & Header Compactness
- `T1-F13-01`: Authenticated user badge collapses gracefully on mobile screens (< 640px) without overflowing header.
- `T1-F13-02`: Cart drawer icon button remains fully visible and operable on compact screens.
- `T1-F13-03`: Mobile menu hamburger button remains within right viewport boundary on 360px width.
- `T1-F13-04`: Non-existent `xs:` prefix classes are replaced with standard responsive utilities.
- `T1-F13-05`: Clinic brand logo text scales proportionally to prevent header squeezing.

#### Feature 14: Mobile Navigation Drawer & Hamburger
- `T1-F14-01`: Mobile menu drawer opens and closes with smooth CSS transition / slide physics.
- `T1-F14-02`: Backdrop overlay is assigned `z-40` while navigation and drawer are assigned `z-50`.
- `T1-F14-03`: Mobile menu drawer dynamically displays user profile, portal link, and logout button when authenticated.
- `T1-F14-04`: Drawer navigation links provide touch target height >= 44px.
- `T1-F14-05`: Selecting a navigation item closes drawer and smoothly unlocks body scroll.

#### Feature 15: Touch Target Guideline Remediation
- `T1-F15-01`: Navbar logout button satisfies WCAG 2.1 touch target >= 44x44px.
- `T1-F15-02`: Category filter tabs in Service Catalog and Product Shop have touch height >= 44px.
- `T1-F15-03`: Voucher "Dùng Ngay" buttons provide touch target >= 44x44px.
- `T1-F15-04`: Cart drawer item quantity (+/-) and remove buttons provide touch target >= 44x44px.
- `T1-F15-05`: Modal close buttons (Auth, Cart, Packaging, Staff, Coupon) provide touch target >= 44x44px.

#### Feature 16: Hero Trust Metrics Responsiveness
- `T1-F16-01`: Hero trust metrics grid wraps or stacks cleanly on 360px mobile viewports.
- `T1-F16-02`: Metric numbers ("15+", "50.000+", "99.8%") maintain clear margins without touching cell boundaries.
- `T1-F16-03`: Metric labels ("Năm Kinh Nghiệm", "Nụ Cười Hoàn Mỹ") avoid excessive vertical character stacking.
- `T1-F16-04`: Metric divider borders adapt responsively to prevent visual clutter.
- `T1-F16-05`: Metric values remain legible and high-contrast across viewports.

#### Feature 17: FAQ Accordion & Animation Optimization
- `T1-F17-01`: FAQ item 3 content container possesses unique ID `content-faq-3` (resolving duplicate `content-faq-2`).
- `T1-F17-02`: Clicking FAQ 3 toggles answer visibility without console error or null pointer.
- `T1-F17-03`: GSAP tooth animation is scoped specifically to `#hero .fa-tooth` rather than all `.fa-tooth` globally.
- `T1-F17-04`: 3D tilt cards support touch events and reset perspective on touch end/cancel.
- `T1-F17-05`: Floating contact widget and toast notification container do not collide on mobile viewports.

#### Feature 18: Appointment Booking Modal & Drawer
- `T1-F18-01`: Booking form fields (Service, Doctor, Date, Time, Notes) stack cleanly on mobile viewports.
- `T1-F18-02`: Quick time-slot selection pills satisfy touch target >= 44px.
- `T1-F18-03`: "Xác Nhận Đặt Lịch Hẹn" submit CTA remains fully visible and unoccluded.
- `T1-F18-04`: Submitting booking smoothly scrolls confirmation and VietQR deposit container into view.
- `T1-F18-05`: Coupon application input inside booking form updates pricing with visual confirmation.

#### Feature 19: AI Diagnostic Doctor Interface
- `T1-F19-01`: AI diagnostic interface incorporates photo upload dropzone with `<input type="file" accept="image/*">`.
- `T1-F19-02`: Image preview container enforces explicit aspect ratio (`aspect-[4/3]` or `aspect-square`).
- `T1-F19-03`: AI response parser safely maps `pathologyNameVi` without throwing `TypeError: undefined.replace`.
- `T1-F19-04`: AI result card displays pathology name, urgency badge, and clinical recommendation.
- `T1-F19-05`: "Đặt Lịch Khám Ngay" CTA on result card prefills booking form service.

#### Feature 20: Porcelain Warranty Card & QR Code
- `T1-F20-01`: Warranty card renders authentic vector Smart QR code SVG/graphic instead of raw text string.
- `T1-F20-02`: Warranty card header wraps serial number and status badge on mobile viewports.
- `T1-F20-03`: Warranty details display patient name, tooth positions, material, and labo supplier.
- `T1-F20-04`: Warranty status badge ("ACTIVE" / "Đang bảo hành") renders with emerald badge styling.
- `T1-F20-05`: Sample warranty serial pill buttons populate search input and trigger lookup.

#### Feature 21: Shopping Cart Drawer & Checkout Form
- `T1-F21-01`: Cart drawer renders with full-width responsive mobile sizing (`w-full max-w-md`).
- `T1-F21-02`: Checkout form fields are enclosed inside scrollable container to prevent mobile keyboard occlusion.
- `T1-F21-03`: Item quantity controls (+/-) dynamically update subtotal and header badge count.
- `T1-F21-04`: Order summary and "Xác Nhận Đặt Hàng" CTA remain accessible at bottom.
- `T1-F21-05`: Closing cart drawer via backdrop or close button restores body scroll.

#### Feature 22: E2E Test Suite Validation
- `T1-F22-01`: Test runner executes autonomously without manual intervention or permission prompts.
- `T1-F22-02`: Test runner generates standard JSON, TAP, and JUnit XML reports.
- `T1-F22-03`: Test runner exits with code 0 on full pass, non-zero on failure.
- `T1-F22-04`: Test runner reports breakdown of passed, failed, and total test counts.
- `T1-F22-05`: Test runner validates all 23 features in the Feature Inventory.

#### Feature 23: Adversarial Coverage Hardening
- `T1-F23-01`: Full DOM parser confirms zero unclosed structural HTML tags across the entire SPA.
- `T1-F23-02`: CSS parser confirms absence of invalid Tailwind classes (`backdrop-blur-xs`, `shadow-xs`).
- `T1-F23-03`: JavaScript syntax and runtime analyzer verifies zero unhandled exception paths.
- `T1-F23-04`: Input sanitization verifies resistance to XSS in patient names, phone numbers, and notes.
- `T1-F23-05`: Media containers verify existence of fallback image handlers and `alt` accessibility text.

---

### 4.2 Tier 2: Boundary & Corner Cases (114 Tests)

Covers extreme edge cases, viewports, formatting boundaries, and data states:
- `T2-F01-01` to `T2-F01-05`: Logo SVG rendering at extreme dimensions (16px favicon scale, 48px header, 120px hero modal); XML entity escaping in SVG text; high-DPI scaling.
- `T2-F02-01` to `T2-F02-05`: Service SVG icons scaling from 24px badge to 96px modal; invalid service key fallback handling; stroke-width consistency.
- `T2-F03-01` to `T2-F03-05`: Feature badge rendering with extreme container constraints; dark mode gradient contrast; missing badge key fallback.
- `T2-F04-01` to `T2-F04-05`: CSS custom properties override tolerance; missing stylesheet fallback; high-contrast media query compatibility.
- `T2-F05-01` to `T2-F05-05`: Nested section depth limit; void HTML elements compliance; malformed attribute escaping in dynamic templates.
- `T2-F06-01` to `T2-F06-05`: Empty reviews array (`[]`) handling; review with 0 stars or 5+ stars; extremely long customer feedback (1,000+ characters); missing reviewer avatar fallback.
- `T2-F07-01` to `T2-F07-05`: Navbar rendering at precise boundary widths: 1023px (mobile/tablet), 1024px (desktop min), 1279px, 1280px (xl min); header height under zoom 150%.
- `T2-F08-01` to `T2-F08-05`: Dynamic catalog with 1 item, 2 items, 3 items, 4 items on tablet grid; card height parity when card has 1 vs 5 feature bullets.
- `T2-F09-01` to `T2-F09-05`: Flash sale card with 8-digit discount amount ("Giảm 15.000.000đ"); expired coupon badge display; copy-to-clipboard failure fallback.
- `T2-F10-01` to `T2-F10-05`: Branch card with missing phone number; map canvas resize behavior on window orientation change; geolocation permission denial handling.
- `T2-F11-01` to `T2-F11-05`: Longest compound Vietnamese dental terms ("Viện Phục Hình Răng Sứ Thẩm Mỹ Kỹ Thuật Cao"); uppercase diacritics (`Ẵ`, `Ẫ`, `Ứ`, `Ỹ`); font-display `swap` behavior.
- `T2-F12-01` to `T2-F12-05`: Boundary viewport widths: 360px (Galaxy S), 375px (iPhone SE), 390px (iPhone 14), 414px (Plus), 430px (Pro Max); `document.documentElement.scrollWidth === window.innerWidth`.
- `T2-F13-01` to `T2-F13-05`: Logged-in user with extremely long name ("Nguyễn Trần Hoàng Bảo Trúc Phương") in mobile navbar; avatar load failure fallback.
- `T2-F14-01` to `T2-F14-05`: Rapid hamburger toggle clicks (debounce); drawer opening when page is already scrolled to bottom; mobile menu orientation change.
- `T2-F15-01` to `T2-F15-05`: Measuring exact bounding box of all 20 interactive buttons to verify `>= 44.0px` on both width and height.
- `T2-F16-01` to `T2-F16-05`: Hero metrics with extreme numbers ("100.000+ Khách Hàng"); text wrap behavior at 360px inner container (328px).
- `T2-F17-01` to `T2-F17-05`: Rapid accordion toggle stress; accordion with 1,000 words of legal FAQ text; GSAP animation teardown on component unmount.
- `T2-F18-01` to `T2-F18-05`: Booking form submission with past date; appointment with same-day 15-minute slot; empty patient phone number; invalid phone format ("abc").
- `T2-F19-01` to `T2-F19-05`: AI upload of 5MB boundary file (5,242,880 bytes); upload of non-image file (`.pdf`, `.exe`); empty symptom description; network timeout during AI diagnosis.
- `T2-F20-01` to `T2-F20-05`: Warranty lookup with non-existent serial ("INVALID-999"); expired warranty status; special characters in warranty code (`#`, `?`, `/`).
- `T2-F21-01` to `T2-F21-05`: Cart with 99 quantity items; decrement quantity to 0 (auto-removal); empty cart checkout attempt; virtual keyboard 320px viewport emulation.
- `T2-F22-01` to `T2-F22-05`: Runner execution with missing config; runner test timeout handling; exit code non-zero on assertion error.
- `T2-F23-01` to `T2-F23-05`: Malformed HTML payload in forum post; SQL characters in search input; CSS injection attempt in style attributes.

---

### 4.3 Tier 3: Cross-Feature Combinations (15 Pairwise Tests)

- `T3-01`: **Booking + Voucher Application**: Applying voucher `NIENG3D5TR` dynamically recalculates booking deposit and updates VietQR payment box.
- `T3-02`: **Catalog Tabs + Detail Modal**: Switching category tabs filters services dynamically, and clicking "Xem Chi Tiết" opens modal with correct metadata.
- `T3-03`: **AI Diagnostic + Booking Prefill**: AI analyzes symptom -> returns pathology -> clicking "Đặt Lịch Khám" navigates to booking form with service pre-selected.
- `T3-04`: **Warranty Lookup + Cart Recommendation**: Verifying porcelain warranty -> recommended oral care product -> clicking "Thêm Vào Giỏ" adds product to cart drawer.
- `T3-05`: **Mobile Drawer + Auth Modal**: Opening mobile drawer -> clicking "Đăng Nhập" -> drawer closes smoothly, auth modal opens centered, body scroll locked.
- `T3-06`: **Branch Map Selection + Booking Prefill**: Clicking "Đặt Hẹn Tại Chi Nhánh Này" on Branch 2 (Hà Nội) scrolls to booking and pre-selects Branch 2.
- `T3-07`: **Product Combo Packaging + Cart Subtotal**: Selecting Combo Pack on product modal -> adds 3 units with bundle discount to cart -> updates subtotal.
- `T3-08`: **Flash Sale Voucher + Cart Checkout**: Copying flash sale code -> pasting into cart checkout coupon field -> validates discount before submission.
- `T3-09`: **Customer Testimonials + Doctor Booking**: Clicking on verified doctor review badge -> scrolls to doctor card or pre-selects doctor in booking.
- `T3-10`: **Mobile Virtual Keyboard + Cart Drawer**: Emulating 360px viewport with 300px keyboard -> checkout inputs remain scrollable and CTA accessible.
- `T3-11`: **FAQ Accordion + Floating Contact Widget**: Expanding all FAQs on mobile -> scrolling to footer -> floating contact widget does not overlap text.
- `T3-12`: **Bespoke Service SVG + Dynamic Catalog API**: Dynamic service cards render using `DentalIcons.getServiceSvg(category)` instead of font glyphs.
- `T3-13`: **Bespoke Feature Badge + Navigation Links**: Clicking AI Vision badge in navbar smoothly navigates to `#ai-diagnostic` without layout shift.
- `T3-14`: **Porcelain Smart QR Code + VietQR Deposit**: Verifying vector Smart QR code render alongside banking VietQR image.
- `T3-15`: **Dark Mode Toggle + Brand Palette**: Toggling dark mode preserves luxury emerald/navy contrast ratios and icon gradients.

---

### 4.4 Tier 4: Real-World Workload Scenarios (5 Journeys)

- `T4-01`: **Mobile Patient Onboarding & Booking Journey (375px iPhone SE)**  
  Patient visits portal on iPhone SE -> opens hamburger menu -> taps "Dịch Vụ & Bảng Giá" -> inspects "Niềng răng chỉnh nha 3D" -> selects "Đặt Hẹn Ngay" -> selects time slot 14:00 -> fills Vietnamese patient details ("Nguyễn Thị Mai Lan", "0901234567") -> applies voucher `NIENG3D5TR` -> submits -> payment box with VietQR deposits appears in view -> zero horizontal overflow throughout.
- `T4-02`: **AI Dental Diagnostic to Clinical Consultation Journey**  
  Patient uploads oral photo -> AI analyzes and returns `pathologyNameVi: "Viêm tủy răng cấp"` -> result card renders with urgency badge "Cần điều trị sớm" and recommendation -> patient clicks "Đặt Lịch Khám" -> booking form opens with "Điều trị tủy" prefilled -> confirmation received.
- `T4-03`: **Porcelain Crown Warranty Verification & Loyalty Care Journey**  
  Patient enters serial number `CERCON-2024-001` -> system returns authentic Katana Zirconia warranty -> genuine Smart QR code renders with patient name "Trần Văn Hùng" -> patient views recommended oral care product -> adds to cart -> opens cart drawer -> fills shipping address -> submits 1-click order.
- `T4-04`: **Multi-Branch Exploration & Consultation Booking Journey (Tablet 768px)**  
  Patient navigates on iPad (768px) -> views multi-branch section -> switches between HCM, Hà Nội, and Đà Nẵng branches on interactive map -> views facility equipment ("Ghế Sirona", "Cone Beam 3D") -> clicks "Chỉ Đường" -> clicks "Đặt Hẹn Tại Chi Nhánh Này" -> booking form opens with branch pre-selected -> cards align evenly without pyramid hacks.
- `T4-05`: **Customer Testimonial Verification & Doctor Selection Journey (Desktop 1280px)**  
  Prospective patient browses reviews in `#testimonials` -> filters by 5-star ratings -> reads testimonial for BSCKII Nguyễn Hoàng Long -> navigates to `#doctors` section -> clicks "Đặt Hẹn Khám" on Doctor Long -> doctor is pre-selected in booking modal -> appointment scheduled successfully.

---

## 5. Automated Test Runner Architecture (`tests/e2e/`)

### 5.1 Architecture & Layout
```
tests/e2e/
├── runner.js              # Standalone Node.js executable test runner
├── runner.py              # Standalone Python executable test runner
├── test_engine.js         # Core assertion & DOM/CSS evaluation engine
├── suites/
│   ├── tier1_features.js   # 116 Feature Coverage tests (F01-F23)
│   ├── tier2_boundaries.js # 114 Boundary & Viewport tests
│   ├── tier3_combos.js     # 15 Pairwise Integration tests
│   └── tier4_journeys.js   # 5 Real-World Workload Scenarios
└── results/
    ├── report.json        # Machine-readable test results
    ├── junit.xml          # Standard CI JUnit XML report
    └── results.tap        # Test Anything Protocol output
```

### 5.2 Autonomous Execution
The runner requires zero external npm or pip dependencies. It uses standard library capabilities to:
1. Parse `index.html` into a structural DOM model.
2. Parse `clinic-ui-refresh.css` and inlined styles to validate CSS selectors, properties, media queries, and variables.
3. Analyze `app.js` and `dental-icons.js` for syntax correctness, exported contracts, and error-handling guards.
4. Simulate mobile viewport widths (360px, 375px, 390px, 414px, 430px) and check box model widths against container boundaries.
5. Audit touch targets against the WCAG 2.1 44x44px bounding box standard.
6. Verify REST API contract compliance against backend endpoints (`/api/reviews/latest`, `/api/ai/diagnose`, `/api/warranties/*`).

---

## 6. How to Run the Test Suite

### 6.1 Using Node.js
```bash
node tests/e2e/runner.js
```
*(Windows PowerShell: `node tests\e2e\runner.js`)*

### 6.2 Using Python
```bash
python tests/e2e/runner.py
```
*(Windows PowerShell: `python tests\e2e\runner.py`)*

### 6.3 Options
- `--tier=1` : Run only Tier 1 tests
- `--tier=2` : Run only Tier 2 tests
- `--tier=3` : Run only Tier 3 tests
- `--tier=4` : Run only Tier 4 tests
- `--feature=F01` : Run tests for specific feature
- `--format=tap|json|junit|console` : Specify output format (default: console + json)
- `--bail` : Stop execution on first failure

---

## 7. Defect Escalation Protocol

When tests fail during implementation milestone verification:
1. **Never modify implementation code from the Test Writer role.**
2. Document the failure in the test report with exact:
   - Test ID (e.g. `T1-F05-01`).
   - Expected requirement statement.
   - Observed DOM element or CSS property.
   - Recommended remediation file and line number.
3. Escalate the defect to the implementing agent (or parent orchestrator).

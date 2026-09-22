# Handoff Report: Customer Dental Experience Survey

**Agent:** `explorer_survey_customer`  
**Date:** 2026-09-22T17:27:00Z  
**Recipient:** Parent Orchestrator (`4110e379-52ae-4437-9f08-bb3a919ba41c`)  
**Type:** Hard Handoff (Investigation & Synthesis Complete)  
**Deliverable Document:** `D:\java\dental-clinic\.agents\explorer_survey_customer\report.md`  

---

## 1. Observation

1. **Appointment Booking Feature:**
   - In `src/main/java/com/dentalclinic/model/Appointment.java` (lines 12-46): Entity contains fields `id`, `patient` (`User`), `patientName`, `patientPhone`, `patientEmail`, `serviceName`, `appointmentTime`, `dentist` (`User`), `status`, `depositAmount`, `notes`, `version`.
   - In `src/main/java/com/dentalclinic/controller/AppointmentController.java` (lines 35-43): Endpoint `POST /api/appointments/book` is public.
   - In `src/main/java/com/dentalclinic/service/AppointmentService.java` (lines 81-120): Automatically creates a new `ROLE_PATIENT` user (`username = cleanPhone`, `password = 123`) if not existing, returning JWT in `BookingResultDto`.
   - In `src/main/resources/static/index.html` (lines 1368-1376): Service options are hardcoded in `<select id="serviceName">`: `"Niềng Răng Mắc Cài Kim Loại Tự Buộc"`, `"Chỉnh Nha Khay Trong Suốt Invisalign"`, `"Nhổ Răng Khôn Sóng Siêu Âm Piezotome"`, `"Cấy Ghép Implant Kỹ Thuật Số"`, `"Trám Răng & Cạo Vôi Siêu Âm"`, `"Tẩy Trắng Răng Laser Whitening"`. No `DentalService` entity exists in the database.
   - In `mobile-app/` directory: Only a staff app exists. `package.json` line 2: `"name": "dental-staff-app"`. In `mobile-app/App.js` lines 38-39: Login defaults to `username = 'letan'`, `password = '123'`, tabs are `['appointments', 'emr', 'shifts', 'stats']`. There is no patient booking screen or API in `mobile-app/src/services/api.js`.

2. **Dental Care E-Commerce & Shopping Cart:**
   - In `src/main/java/com/dentalclinic/model/`: No classes exist for `Product`, `Category`, `Cart`, `CartItem`, `Order`, `OrderItem`, or `PackagingOption`.
   - In `src/main/resources/static/index.html` and `src/main/resources/static/js/app.js`: No e-commerce UI, no shopping cart, no checkout modal.

3. **QR Code Scanning for Porcelain Crown Warranty & Loyalty Points:**
   - In `src/main/java/com/dentalclinic/model/`: No entity exists for `PorcelainCrownWarranty` or `LoyaltyPoint` / `LoyaltyAccount`.
   - In `src/main/resources/static/index.html`: Only static promotional text on warranty ("Bảo hành chính hãng 15 năm") and a 100k payment QR placeholder (`#qr-active`). No warranty verification input, no QR camera scanner, no loyalty point dashboard.
   - In `mobile-app/package.json` lines 11-18: Only dependencies are `@expo/vector-icons`, `expo`, `expo-asset`, `expo-status-bar`, `react`, `react-native`. Neither `expo-camera` nor `expo-barcode-scanner` is installed.

4. **AI Dental Diagnostic (Image Analysis for Oral Pathology):**
   - In `src/main/java/com/dentalclinic/model/DentalImageAttachment.java` (lines 7-36): Entity stores uploaded files (`fileUrl`, `imageType`, `notes`).
   - In `src/main/java/com/dentalclinic/service/AiBlogService.java` (lines 43-90) & `com/dentalclinic/itteam/service/NineRouterAiClient.java`: 9Router AI connection exists at `http://localhost:20128` (models: `combo_toc_do`, `fast-combo`, `vip-combo`), but is used exclusively for generating SEO blog text and IT agent chat.
   - In the entire codebase: No AI dental vision / oral pathology diagnostic pipeline (detecting caries/sâu răng, calculus/vôi răng, gingivitis/viêm nướu, impacted wisdom tooth/răng khôn mọc lệch) exists.

5. **Dental Community Forum / Marketplace & Clinic Branch Map:**
   - In `src/main/java/com/dentalclinic/model/`: Only `Article.java` (blog) and `DoctorReview.java` (reviews) exist. No `ForumPost`, `ForumComment`, or `MarketplaceListing`.
   - In `src/main/resources/static/index.html` (lines 2650-2688): Section `#location-map` contains a hardcoded static iframe of 1 clinic location at `"36/9/12/7 Nguyễn Triệu Luật, Khu Phố 3, Phường Bình Tân, TP. Hồ Chí Minh"`. No `ClinicBranch` entity exists.

---

## 2. Logic Chain

1. **Premise:** The user request (`ORIGINAL_REQUEST.md` follow-ups from 2026-09-22) mandates an omnichannel ecosystem for the DentalCare domain, explicitly requiring customer features: appointment booking for specialized dental services, e-commerce oral care products with packaging options (box/combo) and cart, QR scanning for porcelain crown warranty and loyalty rewards, AI Dental Diagnostic for oral pathology image analysis, dental community forum/marketplace, and clinic branch map with directions.
2. **Analysis of Feature 1 (Booking):** While Web appointment booking exists (`AppointmentController`, `AppointmentService`, `index.html`), the service catalog is hardcoded in frontend HTML without database backing, and the mobile app (`dental-staff-app`) completely lacks patient booking functionality.
3. **Analysis of Feature 2 (E-Commerce):** Zero backend models, endpoints, or frontend views exist for products (electric toothbrushes, water flossers, specialty toothpaste, dental floss, retainers), shopping cart, packaging options, or order processing.
4. **Analysis of Feature 3 (QR Crown Warranty & Loyalty):** Zero models, endpoints, or scanner UI exist for porcelain crown warranty verification (Zirconia, Cercon, Lava, Emax, labo origin, FDI tooth numbering) or loyalty point earning and redemption.
5. **Analysis of Feature 4 (AI Dental Diagnostic):** The system has image upload capabilities (`FileUploadService`) and AI text connectivity (`NineRouterAiClient`), but no AI vision diagnostic pipeline or pathology detection exists for patients.
6. **Analysis of Feature 5 (Community Forum & Branch Map):** Community forum is absent (only 1-way blog articles and doctor reviews exist), and the clinic map is a single hardcoded iframe without multi-branch support or GPS turn-by-turn routing.
7. **Synthesis:** Therefore, the Customer Dental Experience requires a substantial extension across backend JPA entities, REST APIs, Web Portal UI components, and mobile app architecture (transforming the staff-only app into a dual-role customer & staff application).

---

## 3. Caveats

- **Existing Data Safety:** As mandated by R5 and Enterprise Security, modifications to data models must not alter or corrupt existing clinic data directories (`data/`, `uploads/`, `target/`), and existing booking, authentication, and IT team features must remain 100% functional.
- **External AI Dependency:** The AI Dental Diagnostic pipeline should utilize the existing 9Router gateway (`http://localhost:20128`) when available, but MUST implement deterministic rule-based clinical fallbacks (as demonstrated in `NineRouterAiClient`) to ensure 100% uptime when 9Router is offline.
- **Expo Camera Requirements:** Adding QR scanning and camera diagnostics to `mobile-app` will require adding `expo-camera` or `expo-barcode-scanner` to `package.json`.

---

## 4. Conclusion

The comprehensive survey and architectural design is complete. The detailed analysis report has been authored and published at:
`D:\java\dental-clinic\.agents\explorer_survey_customer\report.md`

The report includes:
1. Complete inventory of 14 existing entities, 13 controllers, and all frontend views.
2. Detailed gap analysis across all 5 requested customer functional areas.
3. Complete technical specification for 9 recommended entities (`DentalServiceCatalog`, `DentalProduct`, `ProductPackagingOption`, `DentalOrder`, `PorcelainCrownWarranty`, `LoyaltyAccount`, `AiDentalDiagnosticLog`, `DentalCommunityPost`, `ClinicBranch`).
4. Complete OpenAPI REST API endpoint specifications with RBAC security mappings.
5. Comprehensive UI/UX wireframe specifications for Web Portal sections and Mobile App dual-role tab architecture.
6. Full alignment with the Enterprise 20-Checklist Security standards.

---

## 5. Verification Method

To independently verify the observations and findings documented in this report:

1. **Verify Backend Models and Endpoints:**
   - Inspect existing entities: `view_file` on `src/main/java/com/dentalclinic/model/Appointment.java`, `User.java`, `Coupon.java`. Note the absence of Product, Order, Warranty, Loyalty, Branch, or Forum entities.
   - Inspect controllers: `view_file` on `src/main/java/com/dentalclinic/controller/AppointmentController.java`, `CouponController.java`, `FileUploadController.java`. Note the absence of e-commerce, warranty, or diagnostic endpoints.
2. **Verify Static Web Frontend:**
   - Inspect `src/main/resources/static/index.html`: lines 1368-1376 (hardcoded service list), lines 2650-2688 (single static map iframe).
   - Inspect `src/main/resources/static/js/app.js`: lines 415-432 (patient portal only exposes appointments and EMR).
3. **Verify Mobile App Scope:**
   - Inspect `mobile-app/package.json`: line 2 confirms `"name": "dental-staff-app"`.
   - Inspect `mobile-app/App.js`: lines 33-36 and 88-105 confirm it is exclusively a staff management app for receptionists and dentists.
4. **Invalidation Conditions:**
   - This report would be invalidated if customer e-commerce entities, porcelain crown warranty controllers, or mobile patient screens were discovered in uncommitted branches or alternative folders outside `src/` and `mobile-app/`.

## 2026-09-22T17:25:27Z
Read D:\java\dental-clinic\ORIGINAL_REQUEST.md, D:\java\dental-clinic\PROJECT.md, and D:\java\dental-clinic\.agents\explorer_survey_customer\report.md.
Your working directory is D:\java\dental-clinic\.agents\worker_m1_customer.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Your mission is to implement Milestone 1: Customer Dental Ecosystem (Mobile & Web):
1. Domain Entities in src/main/java/com/dentalclinic/model/ (extending BaseEntity):
   - DentalServiceCatalog: code, name, category (ORTHODONTICS, IMPLANT, PORCELAIN_CROWNS, WHITENING, WISDOM_TEETH, GENERAL), description, price, durationMinutes, isFeatured.
   - DentalProduct: code, name, category (BRUSH, FLOSSER, TOOTHPASTE, FLOSS, RETAINER), brand, description, basePrice, stockQuantity, imageUrl.
   - ProductPackagingOption: product, packagingType (BOX, COMBO), unitName, itemsPerPackage, discountPercent, price.
   - DentalOrder: orderCode, customerName, customerPhone, customerEmail, shippingAddress, totalAmount, packagingType, paymentMethod, status (PENDING, PAID, SHIPPED, DELIVERED, CANCELLED), notes.
   - DentalOrderItem: order, product, packagingOption, quantity, unitPrice, subtotal.
   - PorcelainCrownWarranty: warrantyCode, patientName, patientPhone, crownType (ZIRCONIA, CERCON_HT, LAVA_PLUS, EMAX), toothPositions (FDI e.g. "11, 21"), laboOrigin, warrantyYears, startDate, endDate, qrCodeString, status (ACTIVE, EXPIRED, VOID).
   - LoyaltyAccount: patient (User), phone, pointsBalance, membershipTier (SILVER, GOLD, PLATINUM, DIAMOND), totalPointsEarned.
   - AiDentalDiagnosticLog: patientName, patientPhone, imageUrl, detectedPathology (CARIES, CALCULUS, GINGIVITIS, IMPACTED_WISDOM_TOOTH, HEALTHY), severityLevel, confidenceScore, clinicalRecommendation, analyzedAt.
   - DentalCommunityPost: author (User), authorName, authorPhone, title, content, category (EXPERIENCE, RECOVERY_TIPS, DENTAL_QA), likesCount, approved.
   - ClinicBranch: code, name, address, district, city, phone, hotline, latitude, longitude, openingHours, servicesOffered.
2. Repositories in src/main/java/com/dentalclinic/repository/ for each entity.
3. Services in src/main/java/com/dentalclinic/service/:
   - DentalServiceCatalogService: dynamic catalog management.
   - DentalProductService & DentalOrderService: order calculation with packaging discounts.
   - PorcelainCrownWarrantyService: lookup & QR verification.
   - LoyaltyService: points accumulation and tier calculation.
   - AiDentalDiagnosticService: multi-condition pathology detection (9Router gateway with deterministic clinical rules fallback).
   - DentalCommunityPostService: forum discussion posting & retrieval.
   - ClinicBranchService: multi-branch directory with distance/routing data.
4. Data Seeding in DataInitializer.java:
   - Seed realistic dental services (Niềng răng Invisalign, Cấy ghép Implant Straumann, Bọc răng sứ Lava Plus, Tẩy trắng răng Laser, Nhổ răng khôn Piezotome...).
   - Seed dental care products (Bàn chải điện Oral-B iO, Máy tăm nước Waterpik, Kem đánh răng Marvis/Sensodyne, Chỉ nha khoa Oral-B, Máng duy trì sau niềng) with Box & Combo options.
   - Seed sample porcelain warranties and clinic branches in Hanoi & HCMC with GPS coordinates.
5. Controllers in src/main/java/com/dentalclinic/controller/:
   - DentalServiceController (`/api/dental-services`)
   - DentalProductController (`/api/dental-products`)
   - DentalOrderController (`/api/dental-orders`)
   - PorcelainCrownWarrantyController (`/api/warranties`)
   - LoyaltyController (`/api/loyalty`)
   - AiDentalDiagnosticController (`/api/dental-ai`)
   - DentalCommunityPostController (`/api/forum`)
   - ClinicBranchController (`/api/branches`)
6. Frontend Web UI in src/main/resources/static/index.html & js/app.js:
   - Dynamic Dental Services catalog section with service booking trigger.
   - Dental Care Shop & Cart with Box/Combo packaging selection, cart drawer/modal, and checkout.
   - QR Porcelain Crown Warranty lookup modal/section.
   - AI Dental Diagnostic modal with image upload and pathology diagnosis visualization.
   - Dental Community Forum section with topic filtering and post submission.
   - Multi-Branch Clinic Map with branch cards and Google Maps navigation links.
7. Mobile App in mobile-app/App.js & mobile-app/src/:
   - Add dual-role patient interface with tabs: Dịch Vụ (Services), Đặt Lịch (Booking), Mua Sắm (Shop & Cart), Bảo Hành (Warranty), AI Chẩn Đoán (AI Diagnostic).
8. Verify everything compiles and unit tests pass with `.\mvnw.cmd test-compile`.
9. Write handoff.md in your working directory and notify the parent orchestrator with send_message.

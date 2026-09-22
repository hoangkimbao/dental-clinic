# BRIEFING — 2026-09-22T17:50:00Z

## Mission
Implement Milestone 2: Staff, B2B Tier-2 Agent & EMR/Field Operations for DentalCare Clinic.

## 🔒 My Identity
- Archetype: worker
- Roles: implementer, qa, specialist
- Working directory: D:\java\dental-clinic\.agents\worker_m2_staff
- Original parent: 4110e379-52ae-4437-9f08-bb3a919ba41c
- Milestone: M2 - Staff, B2B Tier-2 Agent & EMR/Field Operations

## 🔒 Key Constraints
- DO NOT CHEAT: No hardcoding test results, dummy implementations, or circumventing tasks.
- Keep 100% genuine domain logic, real JPA entities extending BaseEntity, real repository methods, transactional services.
- Adhere to DentalCare dental clinic domain concepts (mắc cài, trụ implant, vật tư tiêu hao, chi nhánh vệ tinh, bác sĩ, chấm công GPS/IP, KPI, khám sàng lọc học đường).
- Do not use Lombok; follow codebase pattern with explicit getters/setters/constructors.
- Write tests in src/test/java/com/dentalclinic/StaffAndOperationsTest.java.
- Verify compilation with `.\mvnw.cmd test-compile` (or equivalent test runner).
- Write handoff.md upon completion and notify parent orchestrator via send_message.

## Current Parent
- Conversation ID: 4110e379-52ae-4437-9f08-bb3a919ba41c
- Updated: 2026-09-22T17:50:00Z

## Task Summary
- **What to build**:
  1. JPA Entities (extending BaseEntity): Tier2Agent, DentalMaterial, MaterialOrder, MaterialOrderItem, StaffAttendance, DoctorKpiRecord, FieldPatientIntake.
  2. Spring Data JPA Repositories for each entity.
  3. Service Layer: Tier2AgentService, DentalMaterialService, MaterialOrderService, StaffAttendanceService, DoctorKpiService, FieldPatientIntakeService.
  4. REST Controllers: Tier2AgentController, DentalMaterialController, MaterialOrderController, StaffAttendanceController, DoctorKpiController, FieldPatientIntakeController.
  5. Seed Data in DataInitializer.java.
  6. Web Management Portal UI & Mobile App UI updates (Inventory, B2B Orders, Attendance, KPIs, Field Leads).
  7. Comprehensive Unit & Integration Tests in StaffAndOperationsTest.java.
- **Success criteria**: Clean compilation, complete business logic, tests pass, UI updated.
- **Interface contracts**: PROJECT.md § M2: Staff & B2B Tier-2 Agent Operations.
- **Code layout**: PROJECT.md § Code Layout.

## Change Tracker
- **Files modified**: [TBD]
- **Build status**: Pending
- **Pending issues**: None

## Quality Status
- **Build/test result**: Pending
- **Lint status**: Clean
- **Tests added/modified**: Pending

## Loaded Skills
- None required to be loaded locally at this stage.

## Key Decisions Made
- Use standard Java 17 patterns consistent with existing codebase (no Lombok, explicit getters/setters, BigDecimal/Double for amounts).
- Use proper Spring Data JPA queries and Spring Security annotations.
- Provide comprehensive seed data in DataInitializer.java.

## Artifact Index
- DISPATCH.md — Assignment instructions
- BRIEFING.md — Situational awareness index
- progress.md — Liveness & task execution log
- handoff.md — Final handoff report

# OfficeZen — Master Product Blueprint (Version 1.0.0)

**Tagline:** One App. Complete Office. Anywhere.  
**Platform:** Android-first (Google Play Store)  
**Design Direction:** Premium Emerald Atelier & Burnished Marigold (`#0A4D46`, `#F5F4EF`, `#D97706`, `#091010`)  
**Typography:** `Outfit` (Display/Headings), `Plus Jakarta Sans` (Body/UI), `JetBrains Mono` (Numeric KPIs, Currency, Coordinates, Timestamps)  
**Core Architecture:** Offline-first Android application (Room SQLite + WorkManager + DataStore) with zero-trust Firebase Cloud Synchronization (`officezen-d42ad`).

---

## 1. Strategic Architecture & Core Pillars

1. **Multi-Workspace & Multi-Department Tenant Isolation** — Single or multiple offices per owner, department hierarchy, and strict office-scoped data isolation.
2. **Dual Device Strategy** — Shared Office Phone (Attendance Kiosk Mode with isolated session & PIN/admin exit) and Employee Personal Phone Mode (device registry, Play Integrity attestation, remote revocation).
3. **Offline-First Face Recognition & Liveness (Phase 5)** — CameraX `ImageAnalysis` + bundled Apache-2.0/commercial-safe face detection + LiteRT embedding matching against Android Keystore AES-256-GCM encrypted templates + multi-signal liveness checks.
4. **Custom Role-Based Permissions (Phase 3)** — Configurable role templates (`Owner`, `Admin`, `Manager`, `HR`, `Accountant`, `Supervisor`, `Employee`, `Receptionist`, and Custom Roles) with module × action (`view`, `create`, `edit`, `approve`, `export`, `delete`) and scope (`OWN_ONLY`, `DEPARTMENT_ONLY`, `OFFICE_ONLY`, `ALL_OFFICES`).
5. **Global Localization** — Multi-currency, configurable date/time formats, first day of week, IANA time zones, 22+ languages with RTL support, and country-configurable payroll rules with zero hardcoded company or region defaults.
6. **Complete Operations Suite** — Home Dashboard, Employees, Attendance & Kiosk, Payroll, Leave, Documents, Expenses, Tea & Coffee, Tasks, Inventory & Assets, Visitors, Notices & Announcements, Meetings & Calendar, Reports & Analytics, Notifications, Office Management, User Roles & Permissions, Subscription & Billing, Settings & Privacy.

---

## 2. Implementation Schedule (Phases 0–11)

- **Phase 0 (Completed & Verified):** Existing Project Audit (`officezen-d42ad`, Firestore DB `ai-studio-android-officeze-7dd93b37-6073-4473-9d46-73f59ebdea79`, 13 Room v3 entities, Subscription Phase 2 reconciliation).
- **Phase 1 (Current):** Design System (Emerald Atelier & Burnished Marigold, Outfit + Plus Jakarta Sans + JetBrains Mono, Light/Dark themes), Responsive Navigation (`NavigationBar` / `NavigationRail` / `PermanentNavigationDrawer`), and Global Currency, Date & Timezone Infrastructure.
- **Phase 2:** Generic Bidirectional Sync Engine (`sync_outbox` + Room `MIGRATION_3_4`) & Multi-Office Core.
- **Phase 3:** Custom Roles, Granular RBAC Matrix, Department Scoping & High-Risk Security Gates.
- **Phase 4:** Device Management & Isolated Shared Kiosk Mode.
- **Phase 5:** Offline On-Device Face Recognition, Liveness & Encrypted Biometric Templates.
- **Phase 6:** HR, Attendance Anomaly Detection & Global Configurable Payroll.
- **Phase 7:** Enhanced Documents, General Office Expenses & Refreshments Ledger.
- **Phase 8:** Tasks, Inventory/Assets, Visitors, Notices & Meetings/Calendar.
- **Phase 9:** Cross-Module Reports, Notification Center & Global Localization Expansion.
- **Phase 10:** Google Play Billing (`billing-ktx`), Server-Authoritative Cloud Functions & Subscription Enforcement.
- **Phase 11:** Privacy Policy, Account/Data Deletion, R8 Hardening, QA & Play Store Release Readiness.

---

## 3. Revision History

| Version | Date | Summary |
| :--- | :--- | :--- |
| `v1.0.0` | 2026-11-27 | Master Blueprint versioned in repository; Phase 0 Audit verified; Phase 1 initiated with pre-Phase-1 backup checkpoint. |

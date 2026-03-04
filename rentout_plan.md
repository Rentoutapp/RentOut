# RentOut – Kotlin Multiplatform (KMP) Application Development Roadmap
## 1-Month Phased Plan | Android · iOS · Web | Firebase Backend

---

> ## ⚡ URGENT: 4-DAY MVP SPRINT — Target Date: 7 March 2026
> This section supersedes Week 1 of the main roadmap below. The MVP is a **30% milestone payment deliverable**. It must demonstrate a complete, working workflow across all three interfaces to the client.

---

## 🚀 4-DAY MVP SPRINT PLAN (March 4–7, 2026)

### 🎯 MVP Goal
Deliver a **fully functional, demonstrable MVP** covering the exact client workflow:

```
Landlord lists property
       ↓
Admin verifies & approves (property gets VERIFIED badge)
       ↓
Property appears publicly in Tenant interface
       ↓
Tenant pays $10 → contact details unlocked
       ↓
Landlord marks property as UNAVAILABLE
```

---

### 📦 MVP Scope & Pragmatic Tech Decisions

> **Experienced Developer Note:** In 4 days, every hour counts. The decisions below are deliberate trade-offs between correctness, speed, and demonstrability — without compromising the core workflow or security model.

| Concern | Full Plan (Month) | MVP Decision (4 Days) | Reason |
|---|---|---|---|
| **Android + iOS** | Compose Multiplatform KMP | ✅ **Android-first KMP** — iOS follows Week 2 | Client needs fully functional Android APK for testing by March 7th |
| **Admin Web Panel** | Compose Web (wasmJs) | ✅ Firebase Hosting + HTML/Tailwind CSS + Firebase JS SDK | wasmJs setup alone takes 1–2 days; a clean HTML/JS admin panel is production-quality and deployable in hours |
| **Primary MVP Platform** | Android + iOS (KMP) | ✅ **Android-first** — full functionality, installable APK for client testing | Client explicitly requested fully functional Android version for demo/testing; iOS follows in Week 2 |
| **Payment** | Stripe + Cloud Function server verification | ✅ **PesePay** (Zimbabwean gateway) — REST API + WebView hosted checkout | Stripe is sanctioned/banned in Zimbabwe. PesePay supports USD + ZWL, uses a redirect/WebView checkout flow with a Firebase Cloud Function webhook receiver for payment confirmation |
| **Image Upload** | Firebase Storage multi-image | ✅ Single image upload via Firebase Storage | Reduces complexity; core workflow unchanged |
| **Navigation** | Voyager/Decompose | ✅ Compose Navigation (built-in, less setup) | Fewer dependencies = faster build |
| **DI** | Koin Multiplatform | ✅ Manual DI / simple singletons | Koin setup overhead avoided for MVP speed |
| **Auth** | Email/Password + Google OAuth | ✅ Email/Password only | Google OAuth requires SHA config per device; adds friction |
| **Push Notifications** | FCM full integration | ❌ Deferred to Week 2 | Not part of MVP workflow |
| **Firestore Security Rules** | Full production rules | ✅ Core rules (role checks + contactNumber protection) | Must be correct from day 1 — non-negotiable |

---

### 🗓️ DAY-BY-DAY MVP EXECUTION PLAN

---

## 📅 DAY 1 — March 4 (Tuesday): Foundation + Auth + Firebase

### Morning (4 hrs): Project Scaffold
- [ ] Create KMP project via JetBrains KMP Wizard
  - Targets: `androidMain`, `iosMain`
  - Enable Compose Multiplatform
- [ ] Set up `libs.versions.toml` with MVP dependencies:
  ```toml
  [versions]
  kotlin = "2.0.21"
  compose = "1.7.0"
  gitlive-firebase = "2.1.0"
  ktor = "3.0.0"
  kotlinx-serialization = "1.7.3"
  kotlinx-coroutines = "1.9.0"
  kotlinx-datetime = "0.6.1"
  multiplatform-settings = "1.2.0"
  compose-navigation = "2.8.0-alpha10"
  coil = "3.0.4"

  [libraries]
  firebase-auth = { module = "dev.gitlive:firebase-auth", version.ref = "gitlive-firebase" }
  firebase-firestore = { module = "dev.gitlive:firebase-firestore", version.ref = "gitlive-firebase" }
  firebase-storage = { module = "dev.gitlive:firebase-storage", version.ref = "gitlive-firebase" }
  kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
  kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
  kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
  multiplatform-settings = { module = "com.russhwolf:multiplatform-settings", version.ref = "multiplatform-settings" }
  compose-navigation = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "compose-navigation" }
  coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
  coil-network = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil" }
  ```
- [ ] Configure `composeApp/build.gradle.kts` — add all dependencies to `commonMain`, `androidMain`, `iosMain`
- [ ] Add `google-services.json` to `androidApp/` and `GoogleService-Info.plist` to `iosApp/`
- [ ] Create base package structure:
  ```
  commonMain/kotlin/com/rentout/
  ├── data/model/
  ├── data/repository/
  ├── data/firebase/
  ├── domain/usecase/
  ├── presentation/
  └── ui/
      ├── screens/auth/
      ├── screens/landlord/
      ├── screens/tenant/
      ├── components/
      ├── navigation/
      └── theme/
  ```

### Afternoon (4 hrs): Firebase Setup + Design System + Auth

**Firebase Setup:**
- [ ] Create Firebase project → enable Email/Password Auth, Firestore, Storage
- [ ] Write initial `FirebaseModule.kt` (singleton Firebase instances for Auth, Firestore, Storage)
- [ ] Write core Firestore Security Rules (deploy immediately):
  ```javascript
  rules_version = '2';
  service cloud.firestore {
    match /databases/{database}/documents {
      match /users/{userId} {
        allow read, write: if request.auth.uid == userId;
        allow read: if request.auth != null
                    && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == "admin";
      }
      match /properties/{propertyId} {
        allow read: if request.auth != null && resource.data.status == "approved";
        allow create: if request.auth != null
                      && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == "landlord";
        allow update, delete: if request.auth.uid == resource.data.landlordId;
        allow read, write: if request.auth != null
                           && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == "admin";
      }
      match /transactions/{txId} {
        allow create: if request.auth != null;
        allow read: if request.auth.uid == resource.data.tenantId
                    || get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == "admin";
      }
      match /unlocks/{unlockId} {
        allow read: if request.auth.uid == resource.data.tenantId;
        allow create: if request.auth != null;
      }
    }
  }
  ```

**Design System (Theme.kt):**
- [ ] Define brand colors: Primary `#1B4FFF` (deep blue), Secondary `#FF6B35` (orange CTA), Background `#F8F9FA`, Surface White, Error Red
- [ ] Define typography scale (using default Compose fonts for MVP)
- [ ] Define spacing constants (`8dp` grid system)

**Auth Data Layer (commonMain):**
- [ ] `User.kt` data model:
  ```kotlin
  @Serializable
  data class User(
      val uid: String = "",
      val name: String = "",
      val email: String = "",
      val role: String = "",       // "landlord" | "tenant" | "admin"
      val status: String = "active", // "active" | "suspended"
      val createdAt: Long = 0L
  )
  ```
- [ ] `AuthRepository.kt` interface: `login()`, `register()`, `logout()`, `currentUser()`
- [ ] `FirebaseAuthRepository.kt` implementation using GitLive SDK
- [ ] `AuthViewModel.kt` with `StateFlow<AuthState>` (Idle, Loading, Success, Error, Suspended)

**Auth UI Screens:**
- [ ] `SplashScreen.kt` — logo + auto-navigate after checking auth state
- [ ] `LoginScreen.kt` — email/password, login button, "Create account" link
- [ ] `RegisterScreen.kt` — name, email, password, confirm password
- [ ] `RoleSelectionScreen.kt` — two large cards: "I'm a Landlord" / "I'm a Tenant"
- [ ] Post-login router: reads `role` from Firestore → navigates to correct dashboard

**End of Day 1 checkpoint:** ✅ App launches, user can register, select role, and land on correct (empty) dashboard on both Android and iOS.

---

## 📅 DAY 2 — March 5 (Wednesday): Landlord Interface + Admin Web Panel

### Morning (4 hrs): Landlord Module (KMP)

**Data Layer:**
- [ ] `Property.kt` data model:
  ```kotlin
  @Serializable
  data class Property(
      val id: String = "",
      val landlordId: String = "",
      val title: String = "",
      val location: String = "",
      val city: String = "",
      val price: Double = 0.0,
      val rooms: Int = 0,
      val description: String = "",
      val contactNumber: String = "",   // ← NEVER sent to tenant without unlock
      val imageUrl: String = "",
      val status: String = "pending",   // "pending" | "approved" | "rejected"
      val isAvailable: Boolean = true,
      val isVerified: Boolean = false,
      val createdAt: Long = 0L
  )
  ```
- [ ] `PropertyRepository.kt` interface
- [ ] `FirebasePropertyRepository.kt`:
  - `addProperty(property, imageBytes)` → uploads image to Storage → saves to Firestore with `status: "pending"`
  - `getLandlordProperties(landlordId)` → real-time Flow
  - `updateProperty(property)` → updates Firestore doc
  - `deleteProperty(propertyId)` → deletes doc + Storage image
  - `setAvailability(propertyId, isAvailable)` → updates `isAvailable` field

**Landlord ViewModels:**
- [ ] `LandlordViewModel.kt` — manages property list state, CRUD operations
- [ ] `AddPropertyViewModel.kt` — form state, image picking, upload progress

### Late Morning / Afternoon (4 hrs): Landlord UI Screens

- [ ] **`LandlordDashboardScreen.kt`:**
  - Stats row: Total / Approved / Pending / Rejected property counts
  - "Add New Property" FAB button
  - LazyColumn of `PropertyCard` components showing own listings
  - Each card shows status badge chip (🟡 Pending / ✅ Approved / ❌ Rejected)
  - Each card: Edit button, Delete button (with confirm dialog), **"Mark Unavailable / Available"** toggle

- [ ] **`AddPropertyScreen.kt`:**
  - Fields: Title, City, Location, Price, No. of Rooms, Description, Contact Number
  - Single image picker (`expect/actual` — Android: `ActivityResultContracts.GetContent`, iOS: `PHPickerViewController`)
  - Image preview after selection
  - "Submit Listing" button → uploads image → saves to Firestore → shows success message: *"Listing submitted for admin review"*

- [ ] **`EditPropertyScreen.kt`:**
  - Pre-filled form from existing property data
  - Save → updates Firestore, resets status to `"pending"` (must be re-approved)

- [ ] **Reusable `PropertyCard.kt` component:**
  - Image thumbnail, Title, City, Price, Rooms
  - Status badge (color-coded chip)
  - Verified badge: `✅ Verified` green badge if `isVerified == true`
  - Availability chip: `🟢 Available` / `🔴 Unavailable`

**End of morning checkpoint:** ✅ Landlord can add a property, see it listed as "Pending", and mark it available/unavailable.

### Afternoon (4 hrs): Admin Web Panel

> **Tech choice:** Firebase Hosting + plain HTML + Tailwind CSS + Firebase JS SDK v9 (modular). This is deployable in hours, looks professional, and works perfectly as a web admin panel for the MVP. No Compose Web complexity.

**Admin Panel Pages (single HTML file with JS routing, or separate pages):**

- [ ] **`admin/index.html`** — Login page:
  - Firebase email/password login
  - Role check: if `role != "admin"` → show "Access Denied" and sign out
  - On success → redirect to dashboard

- [ ] **`admin/dashboard.html`** — Overview:
  - Stat cards: Total Users, Landlords, Tenants, Total Properties, Pending Properties, Total Unlocks, Total Revenue ($)
  - Navigation sidebar: Dashboard | Properties | Users | Transactions

- [ ] **`admin/properties.html`** — Property Management (most critical for MVP):
  - Table of ALL properties with columns: Image | Title | City | Price | Landlord | Status | Actions
  - Status filter tabs: All | Pending | Approved | Rejected
  - **Approve button** → sets `status: "approved"`, `isVerified: true` → property gets verified badge in app
  - **Reject button** → sets `status: "rejected"`
  - **Delete button** → removes document from Firestore
  - Pending properties highlighted with yellow row background

- [ ] **`admin/users.html`** — User Management:
  - Table: Name | Email | Role | Status | Joined | Actions
  - **Suspend** button → sets `status: "suspended"`
  - **Reactivate** button → sets `status: "active"`

- [ ] **`admin/transactions.html`** — Transaction Monitor:
  - Table: Tenant | Property | Amount | Date | Status

- [ ] **Shared `admin/style.css`** — Tailwind CDN + custom overrides for brand colors
- [ ] **Shared `admin/firebase-config.js`** — Firebase app initialization
- [ ] **Deploy:** `firebase deploy --only hosting`
- [ ] **Create admin account** in Firebase Auth → set `role: "admin"` in Firestore manually (or via Firebase Console)

**End of Day 2 checkpoint:** ✅ Admin web panel live on Firebase Hosting. Admin can log in, see pending properties, approve/reject them. Landlord gets status update in app.

---

## 📅 DAY 3 — March 6 (Thursday): Tenant Interface + Payment + Unlock Flow

### Morning (4 hrs): Tenant Data Layer + Home + Search

**Data Layer:**
- [ ] `Unlock.kt` model:
  ```kotlin
  @Serializable
  data class Unlock(
      val id: String = "",
      val tenantId: String = "",
      val propertyId: String = "",
      val transactionId: String = "",
      val unlockedAt: Long = 0L
  )
  ```
- [ ] `Transaction.kt` model:
  ```kotlin
  @Serializable
  data class Transaction(
      val id: String = "",
      val tenantId: String = "",
      val propertyId: String = "",
      val landlordId: String = "",
      val amount: Double = 10.0,
      val currency: String = "USD",
      val status: String = "success",
      val createdAt: Long = 0L
  )
  ```
- [ ] `TenantRepository.kt`:
  - `getApprovedProperties(city, minPrice, maxPrice, rooms)` — **never returns `contactNumber`** (fetch only required fields)
  - `checkUnlockStatus(tenantId, propertyId): Flow<Boolean>`
  - `getPropertyContactNumber(propertyId): String` — only called post-unlock verification
  - `getUnlockedProperties(tenantId): Flow<List<Property>>`
  - `saveUnlock(unlock)` + `saveTransaction(transaction)`
- [ ] `TenantViewModel.kt` — property list state, search/filter state, unlock state

**Tenant UI — Home + Search:**
- [ ] **`TenantHomeScreen.kt`:**
  - Search bar at top (filters by city/location as user types)
  - Filter row: Price Range chips (e.g. <$500 / $500–$1000 / $1000+), Rooms dropdown
  - `LazyVerticalGrid` of `PropertyCard` — approved + available properties only
  - Real-time Firestore listener (updates as admin approves new listings)
  - Empty state: "No properties found. Try adjusting your filters."
  - Each card clearly shows `✅ Verified` badge if `isVerified == true`

### Afternoon (4 hrs): Property Details + Payment + Unlock

- [ ] **`PropertyDetailScreen.kt`:**
  - Full-width image at top
  - Property details: Title, City, Location, Price, Rooms, Description
  - Availability chip: `🟢 Available` / `🔴 Unavailable`
  - **Verified badge section**
  - **Contact Section (the core business logic):**
    ```
    IF unlock exists for this tenant + property:
        → Show: 📞 [actual phone number]  [Call Button]  [WhatsApp Button]
    ELSE:
        → Show: 📞 ••••••••  [Unlock Contact – $10 →]
    ```
  - "Report this listing" text button → simple reason dialog → saves to `reports/` collection

- [ ] **`PaymentScreen.kt`:**
  - Order summary card: Property title, price, unlock fee `$10.00 USD`
  - Payment method display: PesePay logo + supported methods (Visa, Mastercard, Ecocash, OneMoney, Telecash)
  - **"Pay $10 to Unlock"** button
  - Loading state during payment initiation
  - **MVP Payment Flow (PesePay REST API + WebView):**
    ```
    STEP 1 — Initiate Payment (Firebase Cloud Function: initiatePesePayPayment)
      App calls HTTPS callable Cloud Function →
      Cloud Function calls PesePay REST API:
        POST https://api.pesepay.com/api/payments-engine/v1/payments/initiate
        Body: { amountDetails: { amount: 10, currencyCode: "USD" },
                reasonForPayment: "RentOut Property Unlock",
                resultUrl: "https://us-central1-rentout.cloudfunctions.net/pesepayCallback",
                returnUrl: "rentout://payment-result" }
      PesePay returns: { redirectUrl, pollUrl, referenceNumber }
      Cloud Function saves pending transaction to Firestore, returns redirectUrl

    STEP 2 — WebView Checkout (Android)
      App opens PesePay redirectUrl in an in-app WebView (full-screen)
      Tenant completes payment on PesePay hosted page
      (Supports: Visa/Mastercard card entry, Ecocash, OneMoney, Telecash mobile money)
      PesePay redirects browser to returnUrl: rentout://payment-result

    STEP 3 — Payment Confirmation (Firebase Cloud Function: pesepayCallback)
      PesePay POSTs webhook to resultUrl Cloud Function
      Cloud Function verifies payment status via PesePay poll endpoint:
        GET https://api.pesepay.com/api/payments-engine/v1/payments/check-payment?pollUrl={pollUrl}
      If status == "SUCCESS":
        → Update transaction doc: status "success"
        → Write unlock doc to Firestore
      If status == "FAILED" or "CANCELLED":
        → Update transaction doc: status "failed"

    STEP 4 — App Result Handling
      App intercepts returnUrl deep link (rentout://payment-result)
      App polls Firestore transaction doc for status update (real-time listener)
      If "success" → dismiss WebView → reveal contact number
      If "failed" → dismiss WebView → show error with retry
    ```
  - Success state: animated checkmark + "Contact Unlocked! 🎉" message
  - Error state: "Payment was not completed. Please try again." with retry button
  - Cancel state: "Payment cancelled." with back button

  > **PesePay API Keys:** Store `PESEPAY_API_KEY` and `PESEPAY_ENCRYPTION_KEY` exclusively in Firebase Cloud Function environment config — NEVER in the Android app or any client-side code.

  > **Android WebView Setup:** Configure `WebViewClient` with `shouldOverrideUrlLoading` to intercept the `rentout://` deep link scheme and close the WebView, returning control to the app.

- [ ] **`UnlockedPropertiesScreen.kt`:**
  - List of all properties this tenant has paid to unlock
  - Each card shows the contact number directly (no unlock button)
  - Tap → goes to Property Detail (shows contact immediately)

- [ ] **`TenantProfileScreen.kt`:**
  - Name, email display
  - "My Unlocked Properties" shortcut
  - Logout button

**End of Day 3 checkpoint:** ✅ Tenant can browse approved properties, tap a property, pay $10 (test card), see contact number revealed. Full core workflow functional end-to-end.

---

## 📅 DAY 4 — March 7 (Friday): Polish + Property Availability + Integration Testing + Client Demo

### Morning (3 hrs): Landlord "Mark Unavailable" + Verified Badge + Polish

- [ ] **Property Availability Toggle (Landlord side):**
  - On `LandlordDashboardScreen`, each property card has an availability switch/toggle
  - `setAvailability(propertyId, false)` → sets `isAvailable: false` in Firestore
  - On Tenant side: `isAvailable == false` properties still show in list but with `🔴 Unavailable` badge and the Unlock button is disabled with message: *"This property is no longer available"*

- [ ] **Verified Badge implementation (final pass):**
  - Admin approves → `isVerified: true` is set simultaneously with `status: "approved"`
  - Both Tenant Property Card and Property Detail show: **✅ Verified by RentOut Admin** green badge with shield icon
  - Landlord's own listing card shows the same badge on approval

- [ ] **Suspended User Block:**
  - On login: after Firebase Auth success, fetch user doc from Firestore
  - If `status == "suspended"` → sign out immediately → show `SuspendedScreen.kt`: "Your account has been suspended. Contact support."

- [ ] **UI Polish pass:**
  - Consistent padding (16dp horizontal), card elevation/shadow
  - Loading skeletons on list screens (simple shimmer effect)
  - Form validation error messages (empty fields, invalid price, short description)
  - Empty state illustrations on all list screens
  - Pull-to-refresh on all list screens
  - Back navigation on all detail screens

### Midday (2 hrs): End-to-End Integration Test

Run the complete client workflow from scratch:

- [ ] **Test Run 1 — Full Workflow:**
  1. Register as Landlord → select Landlord role
  2. Add a property with image, contact number, all fields filled
  3. See property in "My Listings" with 🟡 Pending badge
  4. Log into Admin web panel → see property in Pending tab
  5. Admin approves property → property gets ✅ Verified badge in app
  6. Register as Tenant → browse home screen → see approved property with ✅ badge
  7. Tap property → see blurred contact number → tap "Unlock Contact – $10"
  8. Tap Pay → PesePay WebView opens → complete payment with PesePay test credentials (Ecocash sandbox or test card)
  9. WebView closes → app detects payment success via Firestore real-time listener → contact number revealed
  10. Go to "My Unlocked Properties" → property listed with contact number visible
  11. Log back into Landlord account → mark property as Unavailable
  12. Check Tenant side → property shows 🔴 Unavailable, unlock button disabled

- [ ] **Test Run 2 — Edge Cases:**
  - Try to unlock same property twice → check no duplicate unlock / no second charge
  - Try to log in as suspended user → confirm blocked
  - Try to access admin panel as non-admin → confirm access denied
  - Register with mismatched passwords → confirm validation error
  - Submit property with empty required fields → confirm validation errors

- [ ] **Cross-device test:**
  - Android (emulator or physical device)
  - iOS (simulator)
  - Admin panel: Chrome browser

### Afternoon (2 hrs): Final Prep + Demo Build

- [ ] Fix any P0 bugs found in testing
- [ ] Build release-mode APK for Android (for client demo installation)
- [ ] Build iOS app for simulator (or TestFlight if time permits)
- [ ] Ensure Admin web panel is live on Firebase Hosting URL
- [ ] Prepare demo credentials:
  ```
  Landlord:  landlord@rentout.demo / Demo1234!
  Tenant:    tenant@rentout.demo / Demo1234!
  Admin:     admin@rentout.demo / Demo1234!
  PesePay:   Use PesePay sandbox/test environment credentials
             Test Ecocash number: 0771234567 (PesePay sandbox)
             Test card: use PesePay test card details from their developer portal
  ```
- [ ] Git tag: `v1.0.0-mvp` on `main` branch

**End of Day 4 / Client Demo Checkpoint:** ✅ Full MVP delivered.

---

### 📋 Updated MVP Screens Delivery Summary (Client Workflow)

| # | Screen | Platform | Interface | Purpose |
|---|---|---|---|---|
| 1 | **Intro Screen** | Android + iOS | All | Welcome/onboarding to app |
| 2 | **Role Selection Screen** | Android + iOS | All | Choose Landlord or Tenant role |
| 3 | **Auth Screen** (Login / Register tabs) | Android + iOS | All | Email/password authentication |
| 4 | **Splash Screen** | Android + iOS | All | Loading state + auto-redirect to dashboard |
| 5 | **Landlord Dashboard** | Android + iOS | Landlord | Property management hub |
| 6 | **Add Property Screen** | Android + iOS | Landlord | Submit new property listing |
| 7 | **Edit Property Screen** | Android + iOS | Landlord | Modify existing property |
| 8 | **Tenant Home / Browse** | Android + iOS | Tenant | Search and discover properties |
| 9 | **Property Detail Screen** | Android + iOS | Tenant | View property, unlock contact |
| 10 | **Payment Screen** | Android + iOS | Tenant | PesePay $10 unlock payment |
| 11 | **Unlocked Properties Screen** | Android + iOS | Tenant | View all unlocked contacts |
| 12 | **Tenant Profile Screen** | Android + iOS | Tenant | Profile and logout |
| 13 | **Suspended Account Screen** | Android + iOS | All | Blocked access for suspended users |
| 14 | **Admin Login** | Web | Admin | Admin web panel access |
| 15 | **Admin Dashboard** | Web | Admin | System overview and stats |
| 16 | **Admin Properties (Approve/Reject)** | Web | Admin | Property approval workflow |
| 17 | **Admin Users (Suspend/Reactivate)** | Web | Admin | User management |
| 18 | **Admin Transactions** | Web | Admin | Revenue monitoring |

---

### 📱 Client App Navigation Flow (Android + iOS)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           APP ENTRY POINT                                   │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  1. INTRO SCREEN                                                            │
│     • App branding / logo animation                                         │
│     • Brief tagline about the app                                            │
│     • "Get Started" primary button                                           │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  2. CHOOSE ROLE SCREEN                                                      │
│     • Large card: "I'm a Landlord" → Lists properties, earn from tenants  │
│     • Large card: "I'm a Tenant" → Find rentals, pay $10 to unlock contact │
│     • Selected role stored for auth flow                                    │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  3. AUTH SCREEN (Tab-based: Login | Register)                              │
│                                                                             │
│     LOGIN TAB:                        REGISTER TAB:                         │
│     • Email input                     • Full name input                      │
│     • Password input                  • Email input                        │
│     • "Forgot Password?" link         • Password input                     │
│     • "Sign In" button                • Confirm password input             │
│                                       • "Create Account" button            │
│     • Role is auto-assigned from previous screen                            │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  4. SPLASH SCREEN                                                           │
│     • Animated logo                                                         │
│     • Loading state: "Setting up your experience..."                        │
│     • Auto-checks: auth state, user role, account status                   │
│     • Redirects to appropriate dashboard                                    │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                     ┌───────────────┴───────────────┐
                     ▼                               ▼
        ┌─────────────────────────┐       ┌─────────────────────────┐
        │   LANDLORD DASHBOARD    │       │    TENANT DASHBOARD     │
        │   (Role: Landlord)      │       │    (Role: Tenant)       │
        ├─────────────────────────┤       ├─────────────────────────┤
        │ • My Listings           │       │ • Browse Properties     │
        │ • Add New Property      │       │ • Search & Filters      │
        │ • Stats (Pending/      │       │ • My Unlocked Contacts  │
        │   Approved/Total)       │       │ • Profile & Settings    │
        └─────────────────────────┘       └─────────────────────────┘
```

---

## 📋 Original MVP Screens Delivery Summary

| # | Screen | Platform | Interface | Status |
|---|---|---|---|---|
| 1 | Splash Screen | Android + iOS | All | Day 1 |
| 2 | Login Screen | Android + iOS | All | Day 1 |
| 3 | Register Screen | Android + iOS | All | Day 1 |
| 4 | Role Selection Screen | Android + iOS | All | Day 1 |
| 5 | Landlord Dashboard | Android + iOS | Landlord | Day 2 |
| 6 | Add Property Screen | Android + iOS | Landlord | Day 2 |
| 7 | Edit Property Screen | Android + iOS | Landlord | Day 2 |
| 8 | Tenant Home / Browse | Android + iOS | Tenant | Day 3 |
| 9 | Property Detail Screen | Android + iOS | Tenant | Day 3 |
| 10 | Payment Screen | Android + iOS | Tenant | Day 3 |
| 11 | Unlocked Properties Screen | Android + iOS | Tenant | Day 3 |
| 12 | Tenant Profile Screen | Android + iOS | Tenant | Day 3 |
| 13 | Suspended Account Screen | Android + iOS | All | Day 4 |
| 14 | Admin Login | Web | Admin | Day 2 |
| 15 | Admin Dashboard | Web | Admin | Day 2 |
| 16 | Admin Properties (Approve/Reject) | Web | Admin | Day 2 |
| 17 | Admin Users (Suspend/Reactivate) | Web | Admin | Day 2 |
| 18 | Admin Transactions | Web | Admin | Day 2 |

---

## ⚠️ MVP Critical Rules (Non-Negotiable)

| Rule | Enforcement |
|---|---|
| `contactNumber` NEVER returned in property list queries | Only fetch selected fields; contactNumber only fetched after unlock verification |
| No duplicate unlocks | Check `unlocks` collection before writing new unlock |
| Only `approved` + `isAvailable == true` properties visible to tenants | Firestore query filter |
| Suspended users blocked immediately on login | Auth flow checks `status` field before routing |
| Admin-only access to admin panel | Role check on every admin page load; redirects on failure |
| PesePay API keys NEVER on the client | All PesePay API calls go through Firebase Cloud Functions only |
| Payment must be server-confirmed before unlock is written | Cloud Function verifies PesePay status via poll endpoint before writing unlock doc |
| Android APK is the primary MVP deliverable | Fully functional, installable APK handed to client for testing by March 7th |

---

## 🔁 MVP → Production Upgrade Path (Week 2 onward)

After the client approves and pays the 30%:

| Item | MVP State | Production Upgrade |
|---|---|---|
| Payment gateway | PesePay REST API + WebView + Cloud Function webhook (already production-grade) | Add ZWL currency support, receipt emails via Cloud Function |
| iOS support | Deferred | Full iOS build, TestFlight distribution — WebView payment works identically on iOS |
| Admin panel framework | HTML/Tailwind/JS | Optionally migrate to Compose Web or keep as-is (it works great) |
| Image upload | Single image | Multi-image (up to 6) with Firebase Storage |
| Auth providers | Email/Password | + Google OAuth |
| Navigation | Compose Navigation | + Voyager/Decompose (if needed for complex flows) |
| DI | Manual singletons | Koin Multiplatform |
| Notifications | None | FCM full integration (property approved, payment success, new enquiry) |
| Security | Core rules | Full rules + App Check + Custom Claims |
| Performance | Basic | Pagination, Firestore offline persistence, image caching |

---

## 🏁 MVP Definition of Done

The MVP is complete and ready for client presentation when:

1. ✅ A landlord can register, log in, and add a property listing with an image
2. ✅ The property appears in the admin web panel as "Pending"
3. ✅ The admin can approve the property — it receives a **Verified badge**
4. ✅ The approved property appears on the tenant's home screen with the Verified badge
5. ✅ A tenant can register, log in, browse properties, and tap one for full details
6. ✅ The contact number is hidden/redacted on the property detail screen
7. ✅ The tenant can pay $10 via PesePay (WebView checkout — card, Ecocash, OneMoney, or Telecash) and the contact number is revealed
8. ✅ The unlocked property appears in "My Unlocked Properties" with the contact number visible
9. ✅ The landlord can mark their property as Unavailable
10. ✅ The admin can suspend a user and that user is blocked on next login
11. ✅ The admin web panel is live and accessible via a Firebase Hosting URL
12. ✅ App runs correctly on Android (physical or emulator) AND iOS (simulator)

---


---

## 📋 Project Overview

| Item | Detail |
|---|---|
| **App Name** | RentOut |
| **Business Model** | Tenants pay $10 to unlock landlord contact details per property |
| **Platforms** | Android, iOS, Web |
| **Framework** | Kotlin Multiplatform (KMP) + Compose Multiplatform |
| **Backend** | Firebase (Auth, Firestore, Storage, Cloud Functions) |
| **User Roles** | Landlord, Tenant, Admin |
| **Revenue Engine** | Per-unlock transaction ($10 each) |

---

## 🏗️ Technology Stack (KMP-Aligned)

### Shared (commonMain)
| Layer | Library / Tool |
|---|---|
| **UI** | Compose Multiplatform (JetBrains) |
| **Navigation** | Compose Navigation (Voyager or Decompose) |
| **State Management** | Kotlin Coroutines + StateFlow + ViewModel (KMP) |
| **Networking** | Ktor Client (multiplatform) |
| **Serialization** | kotlinx.serialization |
| **DI** | Koin Multiplatform |
| **Image Loading** | Coil 3 (multiplatform) or Kamel |
| **Firebase SDK** | GitLive Firebase KMP SDK (dev.gitlive:firebase-*) |
| **Local Storage** | Multiplatform Settings (russhwolf) |
| **Date/Time** | kotlinx-datetime |
| **Testing** | kotlin.test + Turbine |

### Android (androidMain)
| Layer | Library / Tool |
|---|---|
| **Payment** | PesePay WebView + Firebase Cloud Function |
| **Push Notifications** | Firebase Cloud Messaging (FCM) |
| **Build** | Gradle + Android Gradle Plugin |

### iOS (iosMain)
| Layer | Library / Tool |
|---|---|
| **Payment** | PesePay WebView + Firebase Cloud Function |
| **Push Notifications** | APNs via FCM |
| **Build** | Xcode + KMP framework |

### Web (wasmJsMain / jsMain)
| Layer | Library / Tool |
|---|---|
| **Target** | Kotlin/Wasm (Compose for Web) or Kotlin/JS |
| **Hosting** | Firebase Hosting |
| **Admin UI** | Compose Multiplatform Web |

### Backend (Firebase)
| Service | Purpose |
|---|---|
| Firebase Authentication | Email/Password, Google OAuth |
| Firestore | All structured data |
| Firebase Storage | Property images |
| Cloud Functions (Node.js/Kotlin) | Payment validation, unlock logic, admin triggers |
| Firebase Hosting | Web admin dashboard hosting |
| Firebase Security Rules | Role-based data access enforcement |

---

## 🗄️ Firestore Database Schema (Production-Grade)

\`\`\`
firestore/
├── users/
│   └── {userId}/
│       ├── name: String
│       ├── email: String
│       ├── role: "landlord" | "tenant" | "admin"
│       ├── status: "active" | "suspended"
│       ├── profilePhotoUrl: String?
│       ├── phoneNumber: String?
│       └── createdAt: Timestamp
│
├── properties/
│   └── {propertyId}/
│       ├── landlordId: String
│       ├── title: String
│       ├── location: String
│       ├── city: String
│       ├── price: Number
│       ├── rooms: Number
│       ├── description: String
│       ├── contactNumber: String        ← NEVER returned in list queries
│       ├── imageUrls: [String]
│       ├── status: "pending" | "approved" | "rejected"
│       ├── isFlagged: Boolean
│       ├── amenities: [String]
│       ├── propertyType: "apartment" | "house" | "room" | "commercial"
│       ├── availableFrom: Timestamp
│       └── createdAt: Timestamp
│
├── transactions/
│   └── {transactionId}/
│       ├── tenantId: String
│       ├── propertyId: String
│       ├── landlordId: String
│       ├── amount: Number (10)
│       ├── currency: "USD"
│       ├── status: "pending" | "success" | "failed"
│       ├── paymentProvider: "stripe" | "paypal"
│       ├── paymentReference: String
│       └── createdAt: Timestamp
│
├── unlocks/
│   └── {unlockId}/
│       ├── tenantId: String
│       ├── propertyId: String
│       ├── transactionId: String
│       └── unlockedAt: Timestamp
│
├── reports/
│   └── {reportId}/
│       ├── reportedBy: String (userId)
│       ├── propertyId: String
│       ├── reason: String
│       ├── status: "open" | "resolved"
│       └── createdAt: Timestamp
│
└── analytics/
    └── {dateKey}/            ← e.g., "2026-03-01"
        ├── totalUnlocks: Number
        ├── totalRevenue: Number
        └── newUsers: Number
\`\`\`

---

## 🔐 Firebase Security Rules Summary

\`\`\`
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Users: can only read/write own doc; admin reads all
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
      allow read, update: if request.auth.token.role == "admin";
    }

    // Properties: landlord CRUD own; tenant reads approved only (no contactNumber)
    match /properties/{propertyId} {
      allow read: if resource.data.status == "approved"
                  && !("contactNumber" in request.query.fieldMask);
      allow create: if request.auth.token.role == "landlord";
      allow update, delete: if request.auth.uid == resource.data.landlordId;
      allow read, write: if request.auth.token.role == "admin";
    }

    // Transactions: tenant creates own; admin reads all
    match /transactions/{txId} {
      allow create: if request.auth.token.role == "tenant";
      allow read: if request.auth.uid == resource.data.tenantId
                  || request.auth.token.role == "admin";
    }

    // Unlocks: tenant reads own; validated via Cloud Function
    match /unlocks/{unlockId} {
      allow read: if request.auth.uid == resource.data.tenantId;
    }
  }
}
\`\`\`

---

## 📱 Screens Inventory (All Platforms)

### Shared (Landlord + Tenant on Android/iOS)
| # | Screen | Role |
|---|---|---|
| 1 | Splash Screen | All |
| 2 | Onboarding / Welcome | All |
| 3 | Login Screen | All |
| 4 | Register Screen | All |
| 5 | Role Selection Screen | All |
| 6 | Landlord Dashboard | Landlord |
| 7 | Add Property Screen | Landlord |
| 8 | My Listings Screen | Landlord |
| 9 | Edit Property Screen | Landlord |
| 10 | Tenant Dashboard / Home | Tenant |
| 11 | Search & Filter Screen | Tenant |
| 12 | Property Details Screen | Tenant |
| 13 | Payment Screen | Tenant |
| 14 | My Unlocked Properties Screen | Tenant |
| 15 | Profile / Settings Screen | All |

### Web Admin Panel (Separate Compose Web App)
| # | Screen | Role |
|---|---|---|
| 1 | Admin Login | Admin |
| 2 | Dashboard Overview | Admin |
| 3 | Manage Users | Admin |
| 4 | Manage Properties (Approve/Reject) | Admin |
| 5 | Transaction Monitor | Admin |
| 6 | Revenue Dashboard | Admin |
| 7 | Reports & Flags | Admin |

---

## 🗓️ ONE-MONTH PHASED ROADMAP

> **Duration:** 4 Weeks (28 Days)
> **Team Assumption:** 1–2 Senior KMP Developers + 1 Firebase/Backend Developer
> **Working Hours:** ~8 hrs/day
> **Methodology:** Agile sprints (1 week = 1 sprint)

---

## WEEK 1 (Days 1–7): Foundation & Project Scaffolding

### 🎯 Goal: Working KMP project structure, Firebase connected, Auth screens live on all platforms.

### Day 1–2: Project Setup & Architecture

- [ ] Initialize KMP project using the JetBrains KMP Wizard (`kotlinMultiplatform` + `composeMultiplatform`)
- [ ] Configure targets: `androidMain`, `iosMain`, `wasmJsMain` (web)
- [ ] Set up Gradle version catalog (`libs.versions.toml`) with all dependencies:
  - Compose Multiplatform, Ktor, kotlinx.serialization, Koin, Coroutines, GitLive Firebase SDK, Coil, Voyager/Decompose, Multiplatform Settings, kotlinx-datetime
- [ ] Configure `build.gradle.kts` for all targets
- [ ] Set up project folder architecture:
  ```
  composeApp/
  ├── commonMain/
  │   ├── data/         (repositories, Firebase data sources, models)
  │   ├── domain/       (use cases, interfaces)
  │   ├── presentation/ (ViewModels, UI state)
  │   └── ui/           (Compose screens, components)
  ├── androidMain/
  ├── iosMain/
  └── wasmJsMain/
  ```
- [ ] Set up Git repository with `.gitignore` for KMP, `google-services.json` excluded
- [ ] Configure CI/CD pipeline skeleton (GitHub Actions)

### Day 3: Firebase Integration

- [ ] Create Firebase project on Firebase Console
- [ ] Enable: Authentication (Email/Password + Google), Firestore, Storage, Cloud Functions, Hosting
- [ ] Add `google-services.json` (Android) and `GoogleService-Info.plist` (iOS) to platform targets
- [ ] Integrate GitLive Firebase KMP SDK (`dev.gitlive:firebase-auth`, `firebase-firestore`, `firebase-storage`)
- [ ] Write `FirebaseModule.kt` Koin module for DI injection across platforms
- [ ] Test Firebase connection on Android emulator and iOS simulator

### Day 4–5: Authentication Module

- [ ] Design `AuthRepository` interface in `commonMain`
- [ ] Implement `FirebaseAuthRepository` using GitLive SDK
- [ ] Implement use cases: `LoginUseCase`, `RegisterUseCase`, `LogoutUseCase`, `GetCurrentUserUseCase`
- [ ] Build `AuthViewModel` with StateFlow: `Loading`, `Success`, `Error` states
- [ ] Build Compose UI screens (shared):
  - **Splash Screen** – animated logo, auto-navigate based on auth state
  - **Login Screen** – email/password fields, Google Sign-In button, form validation
  - **Register Screen** – name, email, password, confirm password, validation
  - **Role Selection Screen** – Landlord / Tenant card buttons, saves role to Firestore
- [ ] Implement deep link navigation: post-login role-based redirect
- [ ] Write unit tests for `AuthViewModel` and use cases

### Day 6: Navigation Architecture

- [ ] Set up Voyager (or Decompose) navigation in `commonMain`
- [ ] Define navigation graph:
  - `AuthGraph` → Splash → Login → Register → Role Selection
  - `LandlordGraph` → Dashboard → AddProperty → MyListings → EditProperty → Profile
  - `TenantGraph` → Home → Search → PropertyDetails → Payment → UnlockedProperties → Profile
  - `AdminGraph` (Web only) → Dashboard → Users → Properties → Transactions → Revenue → Reports
- [ ] Implement bottom navigation bar (shared Compose component) for Landlord and Tenant
- [ ] Test navigation flow on Android + iOS + Web

### Day 7: Design System & Reusable Components

- [ ] Define brand color palette, typography, spacing tokens in `Theme.kt`
- [ ] Build reusable Compose components in `commonMain/ui/components/`:
  - `RentOutButton` (primary, secondary, outlined variants)
  - `RentOutTextField` (with validation error state)
  - `PropertyCard` (image, title, price, rooms, location)
  - `LoadingSpinner` / `FullScreenLoader`
  - `ErrorState` (with retry button)
  - `EmptyState` (with illustration placeholder)
  - `RentOutTopBar` (with back navigation)
  - `ConfirmationDialog`
- [ ] Test component rendering across platforms

---

## WEEK 2 (Days 8–14): Landlord Module + Property Management

### 🎯 Goal: Full landlord workflow live — list, add, edit, delete properties on Android, iOS, and Web.

### Day 8–9: Landlord Data Layer

- [ ] Define `Property` data model in `commonMain` using `@Serializable`
- [ ] Implement `PropertyRepository` interface and `FirebasePropertyRepository`
- [ ] Implement use cases:
  - `AddPropertyUseCase`
  - `GetLandlordPropertiesUseCase`
  - `UpdatePropertyUseCase`
  - `DeletePropertyUseCase`
  - `UploadPropertyImagesUseCase` (Firebase Storage)
- [ ] Implement image compression utility (platform-specific `expect/actual`)
- [ ] Set Firestore security rules: only landlords can write properties; `contactNumber` field excluded from list projections
- [ ] Write unit tests for repository and use cases

### Day 10–11: Landlord UI Screens

- [ ] **Landlord Dashboard Screen:**
  - Stats cards: Total Listings, Approved, Pending, Rejected
  - Quick-action buttons: Add Property, View Listings
  - Recent listings preview
- [ ] **Add Property Screen:**
  - Form fields: Title, Location, City, Price, Rooms, Property Type, Description, Contact Number, Amenities (chips), Available From (date picker)
  - Multi-image picker (up to 6 images) with preview grid
  - Upload progress indicator
  - Submit → creates Firestore doc with `status: "pending"`, uploads images to Storage
- [ ] **My Listings Screen:**
  - List of landlord's properties using `PropertyCard`
  - Status badge: Pending (yellow) / Approved (green) / Rejected (red)
  - Swipe-to-delete with confirmation dialog
  - Pull-to-refresh
- [ ] **Edit Property Screen:**
  - Pre-filled form from existing property data
  - Image management: remove existing, add new images
  - Save → updates Firestore doc, re-sets status to `"pending"` for re-approval

### Day 12: Image Upload & Firebase Storage

- [ ] Implement `expect/actual` pattern for image picking:
  - Android: ActivityResultContracts
  - iOS: PHPickerViewController wrapper
  - Web: `<input type="file">` via Kotlin/JS interop
- [ ] Upload images to `gs://rentout/properties/{propertyId}/{imageIndex}.jpg`
- [ ] Store returned download URLs in Firestore property doc
- [ ] Implement delete image logic (removes from Storage + Firestore array)

### Day 13: Landlord Profile & Settings

- [ ] **Profile Screen:**
  - Display name, email, profile photo (upload via Storage)
  - Edit profile details
  - Logout button (with confirmation)
  - Account deletion option
- [ ] Implement `UserRepository` and `UpdateProfileUseCase`

### Day 14: Testing & Sprint Review

- [ ] Run integration tests for full landlord flow (add → view → edit → delete)
- [ ] Test image upload on Android and iOS devices/simulators
- [ ] Fix all identified bugs and UI inconsistencies
- [ ] Code review and refactor
- [ ] Git tag: `v0.2.0-landlord-module`

---

## WEEK 3 (Days 15–21): Tenant Module + Payment Integration

### 🎯 Goal: Full tenant workflow live — search, view, pay $10, unlock contact details.

### Day 15–16: Tenant Data Layer & Search

- [ ] Implement `SearchPropertiesUseCase` with Firestore queries:
  - Filter by: city/location, price range (min/max), rooms count, property type
  - Only fetch `approved` properties
  - **Critically:** field projection MUST exclude `contactNumber`
- [ ] Implement `GetPropertyDetailsUseCase` (fetches full doc — contactNumber only revealed after unlock check)
- [ ] Implement `CheckUnlockStatusUseCase` (queries `unlocks` collection for tenantId + propertyId)
- [ ] Implement `GetUnlockedPropertiesUseCase` (for "My Unlocked" screen)
- [ ] Write unit tests

### Day 17: Tenant UI Screens — Home & Search

- [ ] **Tenant Home / Dashboard Screen:**
  - Featured / Recently Added properties grid
  - Search bar shortcut
  - Category filter chips (Apartment, House, Room, Commercial)
  - Pull-to-refresh with real-time Firestore listener
- [ ] **Search & Filter Screen:**
  - Search text field (location/title full-text approximation via Firestore)
  - Filter panel (collapsible): Price Range (RangeSlider), Rooms (dropdown), Property Type (chips), City
  - Paginated results list using `PropertyCard`
  - "No results" empty state

### Day 18: Property Details & Unlock Flow

- [ ] **Property Details Screen:**
  - Image carousel/pager (swipeable)
  - Full property info: Title, Price, Rooms, Type, Location, Description, Amenities chips, Available From
  - Landlord section: avatar placeholder
  - **Contact section:**
    - If unlocked → show phone number with "Call" and "WhatsApp" action buttons
    - If NOT unlocked → blurred/masked placeholder + "Unlock Contact – $10" CTA button
  - Report listing button (opens reason dialog → saves to `reports/`)
- [ ] Implement real-time unlock status check on screen entry

### Day 19: Payment Integration

- [ ] Integrate **PesePay** payment gateway (Zimbabwe-compliant, no Stripe):
  - No native SDK required — PesePay uses a REST API + hosted WebView checkout
  - Android: `WebView` component with deep link interception (`rentout://payment-result`)
  - iOS: `WKWebView` via UIViewRepresentable
  - Web: redirect to PesePay hosted page
- [ ] Implement `PesePayRepository` in `commonMain` (Ktor HTTP client calls to Cloud Functions)
- [ ] **Firebase Cloud Functions (Node.js):**
  - `initiatePesePayPayment` (HTTPS callable):
    - Receives: `{ propertyId, tenantId, amount: 10, currency: "USD" }`
    - Calls PesePay REST API to create payment
    - Saves pending transaction to Firestore
    - Returns `{ redirectUrl, referenceNumber }` to client
  - `pesepayCallback` (HTTPS trigger — PesePay webhook):
    - Receives PesePay webhook POST
    - Polls PesePay status endpoint to verify payment
    - On SUCCESS: writes unlock doc, updates transaction to "success"
    - On FAILURE: updates transaction to "failed"
- [ ] **Payment Screen:**
  - Order summary: Property title, amount ($10.00 USD)
  - Supported methods display: Visa/Mastercard, Ecocash, OneMoney, Telecash
  - "Pay $10 to Unlock" button → calls `initiatePesePayPayment` Cloud Function
  - Opens PesePay `redirectUrl` in full-screen WebView
  - Firestore real-time listener on transaction doc for status
  - On `rentout://payment-result` deep link intercept → close WebView → check Firestore status
  - Success → contact number revealed
  - Failure → retry option
- [ ] Implement `PaymentViewModel` and `UnlockViewModel`

### Day 20: My Unlocked Properties & Tenant Profile

- [ ] **My Unlocked Properties Screen:**
  - List of all properties this tenant has unlocked
  - Each card shows contact number directly (already unlocked)
  - Tap to go to Property Details Screen (pre-unlocked state)
- [ ] **Tenant Profile Screen:**
  - Display name, email, profile photo
  - Edit profile, logout
- [ ] Implement offline persistence: cache unlocked property IDs in `MultiplatformSettings` for quick local check before Firestore query

### Day 21: Testing & Sprint Review

- [ ] Test full payment flow end-to-end (use PesePay sandbox/test credentials)
- [ ] Test unlock logic: duplicate unlock prevention, contact reveal
- [ ] Test search + filter combinations across property types
- [ ] Test on real Android device + iOS simulator
- [ ] Fix bugs, UI polish pass
- [ ] Git tag: `v0.3.0-tenant-payment-module`

---

## WEEK 4 (Days 22–28): Admin Web Panel + Security + QA + Deployment

### 🎯 Goal: Admin web dashboard live, security hardened, app deployed to Play Store & App Store.

### Day 22–23: Admin Web Dashboard

> The Admin panel is a **Compose Multiplatform Web** app (wasmJs target), hosted on Firebase Hosting. Admin logs in with an account where `role == "admin"` in Firestore.

- [ ] **Admin Login Screen** – email/password, role guard (reject non-admin)
- [ ] **Dashboard Overview Screen:**
  - KPI cards: Total Users, Landlords, Tenants, Properties, Unlocks, Revenue
  - Revenue chart (daily/monthly bar chart using Canvas Compose drawing)
  - Recent transactions table
- [ ] **Manage Users Screen:**
  - Paginated user table: Name, Email, Role, Status, Joined Date
  - Filter by role dropdown
  - Actions: Suspend / Reactivate / Delete user
  - Firestore update: `users/{userId}/status`
  - Suspended user check on app login (redirect to "Account Suspended" screen)
- [ ] **Manage Properties Screen:**
  - Paginated property table: Title, Landlord, City, Price, Status, Date
  - Filter by status (Pending / Approved / Rejected)
  - Actions: Approve / Reject / Delete / Flag
  - Approve → `status: "approved"` → property becomes visible to tenants
- [ ] **Transaction Monitor Screen:**
  - Table: Tenant, Property, Amount, Status, Date
  - Filter by date range
  - CSV export button (generates downloadable report)
- [ ] **Revenue Dashboard Screen:**
  - Total revenue, daily revenue, monthly revenue
  - Top 5 performing properties
  - Most active landlords
- [ ] **Reports Screen:**
  - Flagged/reported listings table
  - Mark as resolved / delete property actions

### Day 24: Security Hardening

- [ ] Write and deploy final **Firestore Security Rules** covering all collections
- [ ] Write and deploy **Firebase Storage Security Rules** (only authenticated landlords upload; images are publicly readable)
- [ ] Implement **Firebase Custom Claims** via Cloud Function `setUserRole`:
  - On user register → Cloud Function sets `role` as a custom claim on the Firebase Auth token
  - Admin panel uses `request.auth.token.role == "admin"` check
  - App checks suspended status on every login
- [ ] Validate that `contactNumber` is NEVER exposed in Firestore list queries or client-side without unlock
- [ ] Enable Firebase App Check (Android SafetyNet / iOS DeviceCheck) to block abuse
- [ ] Rate-limit Cloud Functions (max 10 calls/minute per user)
- [ ] Security audit checklist:
  - [ ] No API keys in source code (use `local.properties` / env vars)
  - [ ] ProGuard/R8 rules for Android release build
  - [ ] HTTPS enforced everywhere
  - [ ] Input validation on all form fields (client + Cloud Function)

### Day 25: Push Notifications

- [ ] Integrate **Firebase Cloud Messaging (FCM)** via GitLive Firebase Messaging SDK
- [ ] Implement `expect/actual` for notification permission request (Android 13+ / iOS)
- [ ] Cloud Functions triggers for notifications:
  - **To Landlord:** "Your property has been approved/rejected by admin"
  - **To Landlord:** "Someone unlocked your property contact" (revenue alert)
  - **To Tenant:** "Payment successful – contact details unlocked"
  - **To Admin:** "New property submitted for review"
- [ ] Store FCM token in `users/{userId}/fcmToken` on login

### Day 26: Final UI Polish & Accessibility

- [ ] Full UI pass across all screens:
  - Consistent padding, spacing, elevation
  - Dark mode support (optional if time permits)
  - Loading skeletons instead of plain spinners on list screens
  - Haptic feedback on key interactions (Android/iOS platform-specific)
- [ ] Accessibility:
  - Content descriptions on all images and icon buttons
  - Minimum 44dp touch targets
  - Sufficient color contrast (WCAG AA)
- [ ] Offline handling:
  - Show offline banner when no network
  - Firestore offline persistence enabled (default in SDK)
  - Disable payment button when offline

### Day 27: QA & End-to-End Testing

- [ ] **Full regression test suite:**
  - Auth flow (register, login, logout, role assignment)
  - Landlord flow (add → pending → approved → edit → delete)
  - Tenant flow (search → filter → property details → pay → unlock → view contact)
  - Admin flow (login → approve property → suspend user → view revenue)
  - Payment flow (success, failure, duplicate unlock prevention)
- [ ] **Cross-platform testing matrix:**
  - Android: API 26, 30, 34 (physical device if possible)
  - iOS: iOS 16, 17 (simulator)
  - Web: Chrome, Firefox, Safari
- [ ] **Edge case tests:**
  - Network loss mid-payment
  - Duplicate unlock attempt
  - Suspended user login attempt
  - Property deleted after unlock
  - Image upload failure
- [ ] Fix all P0 and P1 bugs found

### Day 28: Deployment & Launch

- [ ] **Android:**
  - Configure `build.gradle.kts` for release: version code, signing config, ProGuard
  - Generate signed APK / AAB
  - Create Google Play Console listing: description, screenshots, privacy policy URL, content rating
  - Submit to Google Play internal test track → promote to production
- [ ] **iOS:**
  - Configure Xcode project: bundle ID, version, signing certificates, App Store capabilities
  - Archive and upload via Xcode Organizer or `xcodebuild`
  - Create App Store Connect listing: metadata, screenshots (all required device sizes), privacy policy
  - Submit for TestFlight → App Store review
- [ ] **Web Admin Panel:**
  - Build Compose Web app for production (`wasmJs` release build)
  - Deploy to Firebase Hosting: `firebase deploy --only hosting`
  - Configure Firebase Hosting rewrites for single-page app routing
  - Set custom domain (optional)
- [ ] **Final Firebase setup:**
  - Enable Firestore backups (daily automated export to Cloud Storage)
  - Set Firestore indexes for all compound queries (location + price, landlordId + status, etc.)
  - Set up Firebase Usage & Billing alerts

---

## 📊 Milestone Summary Table

| Week | Days | Milestone | Deliverable |
|---|---|---|---|
| **Week 1** | 1–7 | Foundation | KMP project, Firebase connected, Auth screens, Design system |
| **Week 2** | 8–14 | Landlord Module | Full CRUD property management, image upload, Landlord UI |
| **Week 3** | 15–21 | Tenant + Payments | Search, property details, PesePay payment, unlock logic |
| **Week 4** | 22–28 | Admin + Security + Launch | Admin web panel, security rules, FCM, QA, store deployment |

---

## ⚠️ Critical KMP-Specific Considerations

### 1. `expect`/`actual` Patterns Required
The following platform-specific implementations must use `expect`/`actual`:
| Feature | Android | iOS | Web |
|---|---|---|---|
| Image Picker | `ActivityResultContracts` | `PHPickerViewController` | `<input type=file>` |
| Payment Handler | PesePay (WebView-based checkout) | PesePay (WebView-based checkout) | PesePay (WebView-based checkout) |
| Push Notifications | FCM | APNs | Web Push (optional) |
| File System | `java.io.File` | `NSFileManager` | N/A |
| Haptic Feedback | `VibrationEffect` | `UIImpactFeedbackGenerator` | N/A |
| Share Sheet | `Intent.ACTION_SEND` | `UIActivityViewController` | Web Share API |

### 2. GitLive Firebase KMP SDK Notes
- Use `dev.gitlive:firebase-auth`, `firebase-firestore`, `firebase-storage`, `firebase-functions`, `firebase-messaging`
- All Firebase operations are `suspend` functions — always call from `CoroutineScope`
- On iOS, Firebase must be initialized in `AppDelegate.swift` via `FirebaseApp.configure()`
- On Web (wasmJs), Firebase JS SDK interop is handled by the GitLive wrapper

### 3. Compose Multiplatform Web (wasmJs) Limitations
- Not all Compose APIs are available on Web yet — test frequently
- Use `Modifier.pointerInput` carefully (touch vs mouse events differ)
- Fonts must be explicitly loaded for Web target
- Admin panel may need fallback to Kotlin/JS + HTML/CSS if Compose Web proves too limiting

### 4. Payment Architecture (Security-Critical)
```
CLIENT (KMP App — Android/iOS)
    │
    ▼
Firebase Cloud Function: initiatePesePayPayment (HTTPS Callable)
    │ Calls PesePay REST API server-side (API key never on client)
    │ Saves pending transaction to Firestore
    │ Returns redirectUrl to client
    │
    ▼
PesePay WebView (in-app — full screen)
    │ Tenant completes payment (card / Ecocash / OneMoney / Telecash)
    │ PesePay redirects to rentout:// deep link
    │
    ▼
PesePay Webhook → Firebase Cloud Function: pesepayCallback (HTTPS trigger)
    │ Verifies payment via PesePay poll endpoint
    │ On SUCCESS: writes transaction (status: success) + unlock doc
    │ On FAILURE: writes transaction (status: failed)
    │
    ▼
CLIENT — Firestore real-time listener detects unlock doc written
    │ Fetches contactNumber from property doc
    ▼
CLIENT → Displays contact number
```
**NEVER** store the PesePay API key or encryption key on the client — all PesePay API calls are made exclusively from Firebase Cloud Functions. **NEVER** write unlock docs from the client directly — only the `pesepayCallback` Cloud Function may write unlock documents after server-side payment verification.

### 5. Firestore Index Requirements
Create composite indexes for:
- `properties` where `status == "approved"` + orderBy `createdAt DESC`
- `properties` where `landlordId == X` + orderBy `createdAt DESC`
- `properties` where `city == X` + `status == "approved"` + `price >= min` + `price <= max`
- `unlocks` where `tenantId == X` + orderBy `unlockedAt DESC`
- `transactions` where `tenantId == X` + orderBy `createdAt DESC`

---

## 💰 Business Logic Rules (Developer Must Enforce)

| Rule | Implementation |
|---|---|
| Only `approved` properties visible to tenants | Firestore query filter + Security Rules |
| `contactNumber` never in list results | Firestore field projection / Security Rules |
| One unlock per tenant per property | Cloud Function checks `unlocks` before writing |
| Admin sets property `status` | Admin panel only — Security Rules block tenant/landlord |
| Suspended users blocked on login | Login → check `status` field → redirect to suspended screen |
| Payment must be server-verified | Cloud Function verifies with PesePay before writing unlock |
| Admin custom claim enforced | Firebase Custom Claims + Security Rules |

---

## 🚀 Phase 2 Roadmap (Post-Launch)

| Feature | Complexity | Priority |
|---|---|---|
| Ratings & Reviews system | Medium | High |
| Property availability calendar | Medium | High |
| In-app messaging (Landlord ↔ Tenant) | High | High |
| KYC verification for landlords | High | Medium |
| Featured listings (paid promotion) | Medium | Medium |
| Subscription model for landlords | High | Medium |
| Push notification campaigns (Admin) | Medium | Low |
| Analytics dashboard (advanced) | Medium | Low |
| Multi-currency support | Medium | Low |
| Map-based property search (MapBox/Google Maps) | High | High |
| Property virtual tours (360° images) | High | Low |

---

## 📁 Recommended KMP Project Structure

```
RentOut/
├── composeApp/
│   ├── src/
│   │   ├── commonMain/
│   │   │   ├── kotlin/com/rentout/
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/          (Property, User, Transaction, Unlock)
│   │   │   │   │   ├── repository/     (interfaces)
│   │   │   │   │   └── firebase/       (FirebaseAuthRepo, FirebasePropertyRepo, etc.)
│   │   │   │   ├── domain/
│   │   │   │   │   └── usecase/        (all use cases)
│   │   │   │   ├── presentation/
│   │   │   │   │   ├── auth/           (AuthViewModel, AuthState)
│   │   │   │   │   ├── landlord/       (LandlordViewModel, LandlordState)
│   │   │   │   │   ├── tenant/         (TenantViewModel, TenantState)
│   │   │   │   │   └── admin/          (AdminViewModel, AdminState)
│   │   │   │   └── ui/
│   │   │   │       ├── screens/
│   │   │   │       │   ├── auth/
│   │   │   │       │   ├── landlord/
│   │   │   │       │   ├── tenant/
│   │   │   │       │   └── admin/
│   │   │   │       ├── components/     (shared reusable composables)
│   │   │   │       ├── navigation/     (NavGraph, Routes)
│   │   │   │       └── theme/          (Color, Typography, Theme)
│   │   │   └── resources/
│   │   ├── androidMain/
│   │   │   └── kotlin/com/rentout/
│   │   │       ├── payment/            (PesePayAndroidPaymentHandler)
│   │   │       ├── imagepicker/        (AndroidImagePicker)
│   │   │       └── notifications/      (FCMService)
│   │   ├── iosMain/
│   │   │   └── kotlin/com/rentout/
│   │   │       ├── payment/            (PesePayIosPaymentHandler)
│   │   │       └── imagepicker/        (IosImagePicker)
│   │   └── wasmJsMain/
│   │       └── kotlin/com/rentout/
│   │           ├── payment/            (PesePayWebPaymentHandler)
│   │           └── imagepicker/        (WebImagePicker)
│   └── build.gradle.kts
├── iosApp/
│   ├── iosApp.xcodeproj
│   └── iosApp/
│       ├── AppDelegate.swift           (Firebase.configure())
│       └── ContentView.swift
├── firebase/
│   ├── functions/                      (Cloud Functions: processUnlock, setUserRole, sendNotification)
│   ├── firestore.rules
│   ├── storage.rules
│   └── firestore.indexes.json
├── gradle/
│   └── libs.versions.toml              (version catalog)
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🧰 Tools & Services Checklist

| Tool / Service | Purpose | Cost |
|---|---|---|
| Android Studio (KMP plugin) | IDE for development | Free |
| Xcode | iOS build & archive | Free (Mac required) |
| Firebase (Blaze plan) | Backend services | Pay-as-you-go |
| PesePay | Payment processing (Zimbabwe) | Transaction-based fees — confirm with PesePay |
| Google Play Console | Android app publishing | $25 one-time |
| Apple Developer Program | iOS app publishing | $99/year |
| GitHub / GitLab | Source control + CI/CD | Free tier |
| GitHub Actions | CI/CD automation | Free tier |
| Figma | UI design reference | Free tier |

---

## ✅ Definition of Done (Per Feature)

A feature is considered **done** when:
1. ✅ Implemented in `commonMain` (shared logic) with platform-specific `expect/actual` where needed
2. ✅ Unit tested (use cases + ViewModels)
3. ✅ Tested on Android (emulator or device)
4. ✅ Tested on iOS (simulator)
5. ✅ Tested on Web (Chrome browser)
6. ✅ Firestore Security Rules validated for that feature
7. ✅ No P0/P1 bugs open
8. ✅ Code reviewed and merged to `main` branch

---

## 🔑 Key Success Factors

1. **Start with `commonMain` first** – write all business logic platform-agnostic from day one. Resist the urge to put logic in `androidMain`.
2. **Payment security is non-negotiable** – the Cloud Function must be the single source of truth for unlock creation. Never trust the client.
3. **Firestore rules from day 1** – write and test security rules in parallel with feature development, not at the end.
4. **Test on iOS early and often** – KMP iOS builds surface issues that Android won't. Don't leave iOS testing to the last week.
5. **Admin panel is a business asset** – without property approval workflow, the platform is uncontrolled. Prioritize it.
6. **Version your API / Firestore schema** – use a `schemaVersion` field so future migrations are clean.
7. **Keep `google-services.json` and `GoogleService-Info.plist` out of Git** – use CI/CD secrets injection.

---

*Roadmap created: March 2026 | RentOut v1.0 MVP | Kotlin Multiplatform + Compose Multiplatform + Firebase*

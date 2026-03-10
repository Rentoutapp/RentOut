# RentOut — Provider Roles Implementation Plan
## Freelancer Agent & Brokerage Integration

**Version:** 1.0  
**Date:** 2026-03-10  
**Status:** Approved for Implementation  
**Prepared by:** RentOut Dev Team

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Architectural Decision](#2-architectural-decision)
3. [Data Model Changes](#3-data-model-changes)
4. [Firestore Schema Changes](#4-firestore-schema-changes)
5. [Screen-by-Screen Implementation](#5-screen-by-screen-implementation)
6. [Navigation Changes](#6-navigation-changes)
7. [ViewModel & Business Logic Changes](#7-viewmodel--business-logic-changes)
8. [Web Admin Changes](#8-web-admin-changes)
9. [Firestore Rules Changes](#9-firestore-rules-changes)
10. [Testing Checklist](#10-testing-checklist)
11. [Implementation Order](#11-implementation-order)
12. [Field Reference Tables](#12-field-reference-tables)

---

## 1. Executive Summary

The client requires two new user types on the RentOut platform in addition to the existing Landlord and Tenant:

- **Freelancer Agent** — An individual who lists properties on behalf of property owners and earns commission
- **Brokerage** — A registered company/agency with multiple agents that manages property portfolios

### Decision
All three property-listing roles (Landlord, Freelancer Agent, Brokerage) are consolidated under a single **"Property Provider"** umbrella role in Firestore (`role = "landlord"`) with a new `providerSubtype` field (`"landlord"` | `"agent"` | `"brokerage"`). They share the same dashboard, property management screens, and navigation graph. Only the registration form and profile screen show subtype-specific fields.

**This avoids:**
- Tripling the codebase with duplicate screens
- Fragmenting the Firestore `properties` collection
- Creating 3 separate navigation graphs
- Breaking the existing tenant property browsing flow

---

## 2. Architectural Decision

### Why a Single "Provider" Role with Subtypes

| Concern | Separate Interfaces | Single Role + Subtype ✅ |
|---|---|---|
| Screens to maintain | ~10 × 3 = 30 | 10 (shared) |
| Firestore collections | Fragmented | Unified `properties` |
| Nav graphs | 3 | 1 |
| Adding future subtypes | Major refactor | Add 1 enum value |
| Tenant experience | Complex joins | No change |
| Registration UX | 3 separate flows | 1 form, animated sections |

### Role Mapping (Firestore)

| UI Label | `role` field | `providerSubtype` field |
|---|---|---|
| Landlord | `"landlord"` | `"landlord"` |
| Freelancer Agent | `"landlord"` | `"agent"` |
| Brokerage | `"landlord"` | `"brokerage"` |
| Tenant | `"tenant"` | `""` (not applicable) |
| Admin | `"admin"` | `""` (not applicable) |

> The `role` field stays as `"landlord"` for all three provider subtypes so that all existing Firestore queries, rules, and the web admin panel continue to work without changes to query logic.

---

## 3. Data Model Changes

### File: `composeApp/src/commonMain/kotlin/org/example/project/data/model/User.kt`

**Current:**
```kotlin
@Serializable
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val status: String = "active",
    val profilePhotoUrl: String = "",
    val phoneNumber: String = "",
    val createdAt: Long = 0L,
    val gender: String = "",
    val nationalId: String = ""
)
```

**New:**
```kotlin
@Serializable
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val providerSubtype: String = "",       // NEW: "landlord" | "agent" | "brokerage" | ""
    val status: String = "active",
    val profilePhotoUrl: String = "",
    val phoneNumber: String = "",
    val createdAt: Long = 0L,
    val gender: String = "",
    val nationalId: String = "",
    // Agent-specific fields
    val agentLicenseNumber: String = "",    // NEW: agent license/accreditation number
    val yearsOfExperience: String = "",     // NEW: years of experience as agent
    // Brokerage-specific fields
    val companyName: String = "",           // NEW: registered company/agency name
    val companyRegNumber: String = "",      // NEW: Companies Registry number
    val companyAddress: String = "",        // NEW: physical office address
    val taxId: String = ""                  // NEW: tax clearance/ZIMRA number (optional)
)
```

### Helper Extension (add to User.kt or a new ProviderUtils.kt)
```kotlin
// Convenience helpers used across screens
val User.isProvider: Boolean get() = role == "landlord"
val User.isAgent: Boolean get() = role == "landlord" && providerSubtype == "agent"
val User.isBrokerage: Boolean get() = role == "landlord" && providerSubtype == "brokerage"
val User.isLandlord: Boolean get() = role == "landlord" && (providerSubtype == "landlord" || providerSubtype == "")
val User.providerDisplayName: String get() = when (providerSubtype) {
    "agent"    -> "Freelancer Agent"
    "brokerage"-> "Brokerage"
    else       -> "Landlord"
}
val User.providerEmoji: String get() = when (providerSubtype) {
    "agent"    -> "🤝"
    "brokerage"-> "🏢"
    else       -> "🏠"
}
```

---

## 4. Firestore Schema Changes

### `users` Collection — New Fields

Every provider document in Firestore will gain these new fields. Existing documents without them will default to empty strings (backward compatible):

| Field | Type | Values | Who has it |
|---|---|---|---|
| `providerSubtype` | String | `"landlord"` `"agent"` `"brokerage"` `""` | All providers |
| `agentLicenseNumber` | String | e.g. `"ZREA-2024-00123"` | Agent only |
| `yearsOfExperience` | String | e.g. `"5"` | Agent only |
| `companyName` | String | e.g. `"Apex Realty (Pvt) Ltd"` | Brokerage only |
| `companyRegNumber` | String | e.g. `"CR-2019-7821"` | Brokerage only |
| `companyAddress` | String | e.g. `"14 Samora Machel, Harare"` | Brokerage only |
| `taxId` | String | e.g. `"2019347821"` (optional) | Brokerage only |

### `properties` Collection — New Field

Each property document gains one new field to trace which subtype listed it:

| Field | Type | Values | Notes |
|---|---|---|---|
| `providerSubtype` | String | `"landlord"` `"agent"` `"brokerage"` | Copied from the lister's profile at submit time |

This allows the admin web panel and tenant detail screens to display a "Listed by Agent" or "Listed by Brokerage" badge on property cards without a separate Firestore join.

### No Changes To:
- `transactions` collection
- `unlocks` collection
- All existing Firestore indexes

---

## 5. Screen-by-Screen Implementation

### 5.1 RoleSelectionScreen.kt

**File:** `composeApp/src/commonMain/kotlin/org/example/project/ui/screens/auth/RoleSelectionScreen.kt`

**Current state:** Two `RoleCard` items — Landlord and Tenant. Selecting one calls `onRoleSelected(role)`.

**Required changes:**

1. **Add a third top-level state variable:** `var selectedSubtype by remember { mutableStateOf("") }`
2. **Replace the Landlord `RoleCard` with an expandable `ProviderCard`:**
   - The card shows the same Landlord card design when not selected
   - When selected (tapped), the card animates open (using `AnimatedVisibility` + `expandVertically`) to reveal three **subtype radio pills** inside:
     - 🏠 Landlord — *I own the properties I list*
     - 🤝 Freelancer Agent — *I list on behalf of property owners*
     - 🏢 Brokerage — *I represent a company or agency*
   - Each pill uses the existing `RoleCard` micro-interaction style (border glow, checkmark, scale)
   - Selecting a pill sets `selectedSubtype` to `"landlord"` | `"agent"` | `"brokerage"` and also sets `selectedRole = "landlord"` so the Continue button activates
3. **The Tenant `RoleCard` remains unchanged.** Selecting it sets `selectedRole = "tenant"` and clears `selectedSubtype`
4. **The `ProgressButton` passes both role and subtype** via the `onRoleSelected` callback — change the callback signature to `onRoleSelected: (role: String, subtype: String) -> Unit`
5. **Staggered entrance animation:** Add a 4th animation step for the new provider sub-pills (`providerSubtypeVisible`)

**Subtype pill design:**
```
┌─────────────────────────────────────────────────────┐
│  🏠  Landlord                              ◉ selected│
│      I own the properties I list                     │
├─────────────────────────────────────────────────────┤
│  🤝  Freelancer Agent                      ○         │
│      I list on behalf of property owners             │
├─────────────────────────────────────────────────────┤
│  🏢  Brokerage                             ○         │
│      I represent a company or agency                 │
└─────────────────────────────────────────────────────┘
```

Each pill is a `Surface` with `RoundedCornerShape(16.dp)`, animated border glow matching the existing `RoleCard` pattern, and a radio button on the right.

---

### 5.2 AuthScreen.kt

**File:** `composeApp/src/commonMain/kotlin/org/example/project/ui/screens/auth/AuthScreen.kt`

**Current state:** The register tab has 8 fields: Name, Email, Phone, Gender, National ID, Password, Confirm Password, Photo.

**Required changes:**

1. **Add `selectedSubtype: String` parameter** to `AuthScreen()` composable (passed from `App.kt`)
2. **Add new state variables for conditional fields:**
   ```kotlin
   // Agent fields
   var regLicenseNumber   by remember { mutableStateOf("") }
   var regYearsExp        by remember { mutableStateOf("") }
   var licenseError       by remember { mutableStateOf("") }
   // Brokerage fields
   var regCompanyName     by remember { mutableStateOf("") }
   var regCompanyReg      by remember { mutableStateOf("") }
   var regCompanyAddress  by remember { mutableStateOf("") }
   var regTaxId           by remember { mutableStateOf("") }
   var companyNameError   by remember { mutableStateOf("") }
   var companyRegError    by remember { mutableStateOf("") }
   ```
3. **Add a "Provider Type" display badge** at the top of the Register tab, just above the Full Name field. This is read-only and shows which subtype was selected. Example:
   - `🤝 Registering as Freelancer Agent` in a tinted info chip
   - Uses `AnimatedContent` to swap between subtypes smoothly
4. **Agent-specific fields block** — wrapped in `AnimatedVisibility(visible = selectedSubtype == "agent")` with `expandVertically() + fadeIn()` / `shrinkVertically() + fadeOut()`. Insert after the National ID field:
   ```
   ── Agent Details ──────────────────────────────
   RentOutTextField: "Agent License / Accreditation Number"
     leadingIcon = Icons.Default.Badge
     leadingIconTint = RentOutColors.IconPurple
     isError = licenseError.isNotEmpty()
     errorMessage = licenseError
     keyboardType = KeyboardType.Text

   RentOutTextField: "Years of Experience"
     leadingIcon = Icons.Default.WorkHistory
     leadingIconTint = RentOutColors.IconAmber
     keyboardType = KeyboardType.Number
   ```
5. **Brokerage-specific fields block** — wrapped in `AnimatedVisibility(visible = selectedSubtype == "brokerage")` with the same animation. Insert after the National ID field:
   ```
   ── Company Details ────────────────────────────
   RentOutTextField: "Company / Agency Name"
     leadingIcon = Icons.Default.Business
     leadingIconTint = RentOutColors.IconBlue

   RentOutTextField: "Company Registration Number"
     leadingIcon = Icons.Default.Numbers
     leadingIconTint = RentOutColors.IconTeal
     hint = "e.g. CR-2019-7821"

   RentOutTextField: "Company / Office Address"
     leadingIcon = Icons.Default.LocationCity
     leadingIconTint = RentOutColors.IconGreen

   RentOutTextField: "Tax ID / ZIMRA Number (Optional)"
     leadingIcon = Icons.Default.Receipt
     leadingIconTint = RentOutColors.IconAmber
     required = false
   ```
6. **Section dividers** — Between the shared fields and the subtype-specific block, add a styled `HorizontalDivider` with a label pill ("Agent Details" / "Company Details") — same visual style as is used in the AddPropertyScreen section headers.
7. **Update the `onRegister` callback signature** to include the new fields:
   ```kotlin
   onRegister: (
       name: String, email: String, password: String,
       phoneNumber: String, profilePhotoUrl: String, photoBytes: ByteArray?,
       gender: String, nationalId: String,
       providerSubtype: String,
       agentLicenseNumber: String, yearsOfExperience: String,
       companyName: String, companyRegNumber: String,
       companyAddress: String, taxId: String
   ) -> Unit
   ```
8. **Validation rules:**
   - Agent: `agentLicenseNumber` is **required**; `yearsOfExperience` is optional
   - Brokerage: `companyName` and `companyRegNumber` are **required**; `companyAddress` is **required**; `taxId` is optional
   - All existing fields retain their current validation logic unchanged
9. **Update the role display text** at the top of the screen:
   ```kotlin
   // Current:
   text = if (selectedRole == "landlord") "🏠 Landlord" else "🔑 Tenant"
   // New:
   text = when {
       selectedRole == "tenant"                   -> "🔑 Tenant"
       selectedSubtype == "agent"                 -> "🤝 Freelancer Agent"
       selectedSubtype == "brokerage"             -> "🏢 Brokerage"
       else                                       -> "🏠 Landlord"
   }
   ```

---

### 5.3 LandlordProfileScreen.kt

**File:** `composeApp/src/commonMain/kotlin/org/example/project/ui/screens/landlord/LandlordProfileScreen.kt`

**Required changes:**

1. **Replace the hardcoded "Landlord" role badge** in the profile header with a dynamic subtype badge:
   ```kotlin
   // Reads from currentUser.providerSubtype
   Text(
       text = "${currentUser.providerEmoji} ${currentUser.providerDisplayName}",
       ...
   )
   ```
2. **Add a "Professional Details" card section** below the personal info section. This card is only visible when `providerSubtype` is `"agent"` or `"brokerage"`. Use `AnimatedVisibility` wrapping a `Card`:
   - **Agent:** Shows License Number, Years of Experience
   - **Brokerage:** Shows Company Name, Company Reg Number, Office Address, Tax ID (if present)
   - Each row uses the same info-row style already used for phone/email/ID rows in the profile
3. **Update the header gradient badge** that currently says "Landlord" to show the correct subtype label

---

### 5.4 LandlordDashboardScreen.kt

**File:** `composeApp/src/commonMain/kotlin/org/example/project/ui/screens/landlord/LandlordDashboardScreen.kt`

**Required changes (minimal):**

1. **Update the greeting/subtitle** that currently says "Landlord Dashboard" to dynamically show:
   - `"🏠 Your Properties"` for landlord
   - `"🤝 Your Listings"` for agent
   - `"🏢 Portfolio"` for brokerage
   - Read from `user.providerSubtype` via the extension helpers
2. **No other changes needed.** The dashboard, property cards, add property button, etc. all work identically for all subtypes.

---

### 5.5 AddPropertyScreen.kt

**File:** `composeApp/src/commonMain/kotlin/org/example/project/ui/screens/landlord/AddPropertyScreen.kt`

**Required changes (minimal):**

1. **Pass `providerSubtype` to the submit call** so it gets written to the `properties` Firestore document. The `Property` data model already has all fields needed — we just need to add `providerSubtype` as a new field (see Section 4).
2. **For agents: add an optional "Property Owner Name" field** in the Contact section:
   ```
   RentOutTextField: "Property Owner Name (optional)"
     leadingIcon = Icons.Default.Person
     hint = "Name of the property owner you represent"
     visible only when providerSubtype == "agent"
   ```
3. **No structural changes** to the form, validation, or image upload flow.

---

### 5.6 SplashScreen.kt (No Changes)

The SplashScreen routes based on `role == "landlord"` which covers all three provider subtypes. No changes required.

---

## 6. Navigation Changes

### File: `composeApp/src/commonMain/kotlin/org/example/project/ui/navigation/NavRoutes.kt`

**No new routes required.** All three provider subtypes share the existing landlord navigation graph.

### File: `composeApp/src/example/project/App.kt`

**Changes required:**

1. **`RoleSelectionScreen` callback** — update to pass both role and subtype:
   ```kotlin
   composable(NavRoutes.ROLE_SELECTION) {
       RoleSelectionScreen(
           onRoleSelected = { role, subtype ->
               authViewModel.selectRole(role)
               authViewModel.selectSubtype(subtype)   // NEW
               navController.navigate("auth?prefillEmail=&prefillPassword=")
           }
       )
   }
   ```
2. **`AuthScreen` composable call** — pass `selectedSubtype`:
   ```kotlin
   val selectedSubtype by authViewModel.selectedSubtype.collectAsState()  // NEW
   AuthScreen(
       selectedRole    = selectedRole,
       selectedSubtype = selectedSubtype,   // NEW
       ...
       onRegister = { name, email, password, phone, photoUrl, photoBytes,
                      gender, nationalId, subtype,
                      licenseNumber, yearsExp,
                      companyName, companyReg, companyAddress, taxId ->
           authViewModel.onEvent(AuthEvent.Register(
               name = name, email = email, password = password,
               role = selectedRole, providerSubtype = subtype,
               phoneNumber = phone, photoBytes = photoBytes,
               gender = gender, nationalId = nationalId,
               agentLicenseNumber = licenseNumber,
               yearsOfExperience = yearsExp,
               companyName = companyName, companyRegNumber = companyReg,
               companyAddress = companyAddress, taxId = taxId
           ))
       }
   )
   ```
3. **`SplashScreen` routing** — no change; still routes `role == "landlord"` to `LANDLORD_DASHBOARD`.

---

## 7. ViewModel & Business Logic Changes

### File: `composeApp/src/commonMain/kotlin/org/example/project/presentation/AuthViewModel.kt`

**Add to `AuthViewModel`:**

1. **New StateFlow for selected subtype:**
   ```kotlin
   private val _selectedSubtype = MutableStateFlow("")
   val selectedSubtype: StateFlow<String> = _selectedSubtype.asStateFlow()

   fun selectSubtype(subtype: String) {
       _selectedSubtype.value = subtype
   }
   ```

2. **Update `AuthEvent.Register`** to include new fields:
   ```kotlin
   data class Register(
       val name: String,
       val email: String,
       val password: String,
       val role: String,
       val providerSubtype: String = "",
       val phoneNumber: String = "",
       val profilePhotoUrl: String = "",
       val photoBytes: ByteArray? = null,
       val gender: String = "",
       val nationalId: String = "",
       val agentLicenseNumber: String = "",
       val yearsOfExperience: String = "",
       val companyName: String = "",
       val companyRegNumber: String = "",
       val companyAddress: String = "",
       val taxId: String = ""
   ) : AuthEvent()
   ```

3. **Update the `register()` function** — add new fields to the Firestore `set()` call:
   ```kotlin
   firestore.collection("users").document(firebaseUser.uid).set(
       mapOf(
           "uid"                 to user.uid,
           "name"                to user.name,
           "email"               to user.email,
           "role"                to user.role,
           "providerSubtype"     to providerSubtype,   // NEW
           "status"              to user.status,
           "phoneNumber"         to user.phoneNumber,
           "profilePhotoUrl"     to user.profilePhotoUrl,
           "createdAt"           to user.createdAt,
           "gender"              to user.gender,
           "nationalId"          to user.nationalId,
           "agentLicenseNumber"  to agentLicenseNumber,  // NEW
           "yearsOfExperience"   to yearsOfExperience,   // NEW
           "companyName"         to companyName,          // NEW
           "companyRegNumber"    to companyRegNumber,     // NEW
           "companyAddress"      to companyAddress,       // NEW
           "taxId"               to taxId                 // NEW
       )
   )
   ```

4. **Update `checkSession()` and `login()` functions** — both already do a full `doc.get()` from Firestore into a `User` object. Add reads for new fields:
   ```kotlin
   val user = User(
       ...existing fields...,
       providerSubtype    = doc.get("providerSubtype")    as? String ?: "",
       agentLicenseNumber = doc.get("agentLicenseNumber") as? String ?: "",
       yearsOfExperience  = doc.get("yearsOfExperience")  as? String ?: "",
       companyName        = doc.get("companyName")         as? String ?: "",
       companyRegNumber   = doc.get("companyRegNumber")    as? String ?: "",
       companyAddress     = doc.get("companyAddress")      as? String ?: "",
       taxId              = doc.get("taxId")               as? String ?: ""
   )
   ```
   Apply the same to `refreshUser()`.

5. **Update `deleteAccount()`** — The existing delete logic already deletes the entire `users/{uid}` document and all associated properties/transactions. No additional changes needed for the new fields since they live on the user document itself.

---

### File: `composeApp/src/commonMain/kotlin/org/example/project/presentation/PropertyViewModel.kt`

**Update `submitProperty()`** to write `providerSubtype` to the `properties` Firestore document. The caller (AddPropertyScreen) passes it via the `Property` model after it is injected from the current user's profile.

---

## 8. Web Admin Changes

### File: `webApp/admin/user-detail.html`

**Required changes:**

1. **Update `getRoleIcon()` function** to handle subtypes:
   ```javascript
   function getRoleIcon(role, subtype) {
       if (role === 'landlord') {
           if (subtype === 'agent')    return '🤝';
           if (subtype === 'brokerage') return '🏢';
           return '🏠';
       }
       const icons = { tenant: '👤', admin: '🛡' };
       return icons[role] || '👤';
   }
   function getProviderLabel(role, subtype) {
       if (role === 'landlord') {
           if (subtype === 'agent')    return 'Freelancer Agent';
           if (subtype === 'brokerage') return 'Brokerage';
           return 'Landlord';
       }
       return capitalizeFirst(role);
   }
   ```

2. **Update `renderUserProfile()`** to show the correct badge:
   ```javascript
   document.getElementById('user-role-badge').innerHTML =
       getRoleIcon(u.role, u.providerSubtype) + ' ' + getProviderLabel(u.role, u.providerSubtype);
   ```

3. **Add a "Professional Details" info section** in the User Information card. After the National ID row, add a conditional block that renders only when `u.providerSubtype === 'agent'` or `u.providerSubtype === 'brokerage'`:

   **Agent block:**
   ```html
   <div class="info-row ..."> <span>🪪 License Number</span> <span>{agentLicenseNumber}</span> </div>
   <div class="info-row ..."> <span>📅 Years of Experience</span> <span>{yearsOfExperience || '—'}</span> </div>
   ```

   **Brokerage block:**
   ```html
   <div class="info-row ..."> <span>🏢 Company Name</span>    <span>{companyName}</span>    </div>
   <div class="info-row ..."> <span>📋 Reg. Number</span>     <span>{companyRegNumber}</span></div>
   <div class="info-row ..."> <span>📍 Office Address</span>  <span>{companyAddress}</span>  </div>
   <div class="info-row ..."> <span>🧾 Tax ID</span>          <span>{taxId || '—'}</span>    </div>
   ```

4. **Update `loadUserStats()`** — the stats logic checks `u.role === 'landlord'` which already covers all subtypes. No change needed.

5. **Update `loadProperties()`** — already queries `where('landlordId', '==', userId)`. No change needed.

6. **Update `info-role` display** to show the subtype label:
   ```javascript
   document.getElementById('info-role').textContent = getProviderLabel(u.role, u.providerSubtype);
   ```

### File: `webApp/admin/users.html`

1. **Update the role badge rendering** in the users list table to show subtype icons:
   ```javascript
   // In the user row rendering function, replace:
   getRoleIcon(u.role)
   // With:
   getRoleIcon(u.role, u.providerSubtype)
   ```

2. **Add a filter option** in the existing role filter dropdown: "Agent" and "Brokerage" as filter values (filter by `providerSubtype` field).

### File: `webApp/admin/property-detail.html`

1. **Add a "Listed by" badge** in the property details section showing the provider subtype. Query the `providerSubtype` field from the property document and show:
   - 🏠 Listed by Landlord
   - 🤝 Listed by Agent
   - 🏢 Listed by Brokerage

---

## 9. Firestore Rules Changes

### File: `firestore.rules`

**No breaking changes required.** The existing rules grant `landlord` role users full read/write on their own properties. Since all three subtypes have `role = "landlord"`, they automatically inherit the same permissions.

**Optional enhancement** — Add a rule that validates the `providerSubtype` field on write:
```
function isValidProviderSubtype(data) {
  return !('providerSubtype' in data) ||
         data.providerSubtype in ['landlord', 'agent', 'brokerage', ''];
}
```
Add this check to the `users` collection write rule.

---

## 10. Testing Checklist

### Registration Flow
- [ ] Selecting "Landlord" sub-pill → registration shows no extra fields → Firestore writes `providerSubtype: "landlord"`
- [ ] Selecting "Freelancer Agent" → agent fields appear with animation → license number required validation fires → Firestore writes all agent fields
- [ ] Selecting "Brokerage" → company fields appear → company name + reg + address required → Firestore writes all brokerage fields
- [ ] Switching between sub-pills in RoleSelectionScreen → animation works correctly
- [ ] Switching from Provider to Tenant → sub-pills disappear → agent/brokerage fields disappear from AuthScreen
- [ ] Photo upload works for all three subtypes
- [ ] National ID formatter works for all three subtypes

### Login & Session
- [ ] Existing Landlord accounts (no `providerSubtype` field) log in without errors → defaults to `""` / treated as landlord
- [ ] Agent account logs in → dashboard shows "Your Listings"
- [ ] Brokerage account logs in → dashboard shows "Portfolio"
- [ ] Remember Me works for all three subtypes
- [ ] Session restore works for all three subtypes

### Dashboard & Properties
- [ ] All three subtypes can add/edit/delete properties
- [ ] Property cards display correctly for all subtypes
- [ ] Property detail screen works for all subtypes
- [ ] Image upload/edit works for all subtypes

### Profile Screen
- [ ] Landlord profile → shows "🏠 Landlord" badge, no Professional Details card
- [ ] Agent profile → shows "🤝 Freelancer Agent" badge + license/experience card
- [ ] Brokerage profile → shows "🏢 Brokerage" badge + company details card

### Web Admin
- [ ] Users list shows correct icon for each subtype
- [ ] Agent user-detail page shows license number + experience rows
- [ ] Brokerage user-detail page shows all 4 company fields
- [ ] Property detail shows "Listed by Agent" / "Listed by Brokerage" badge
- [ ] Suspend/Reinstate works for all subtypes

### Tenant Experience
- [ ] Tenant can browse properties listed by all three subtypes without visible difference
- [ ] Property detail shows landlord contact info regardless of subtype
- [ ] Payment/unlock flow works for properties listed by all subtypes

---

## 11. Implementation Order

Execute in this exact order to minimise merge conflicts and broken states:

| Step | Task | File(s) | Risk |
|---|---|---|---|
| 1 | Update `User.kt` data model | `User.kt` | Low |
| 2 | Update `AuthEvent.Register` + `AuthViewModel` | `AuthViewModel.kt` | Medium |
| 3 | Update `RoleSelectionScreen` with expandable provider card | `RoleSelectionScreen.kt` | Medium |
| 4 | Update `App.kt` to pass subtype through nav | `App.kt` | Medium |
| 5 | Update `AuthScreen` with conditional field sections | `AuthScreen.kt` | High |
| 6 | Update `LandlordProfileScreen` with subtype badge + details card | `LandlordProfileScreen.kt` | Low |
| 7 | Update `LandlordDashboardScreen` greeting | `LandlordDashboardScreen.kt` | Low |
| 8 | Update `AddPropertyScreen` agent owner field | `AddPropertyScreen.kt` | Low |
| 9 | Update `webApp/admin/user-detail.html` | `user-detail.html` | Low |
| 10 | Update `webApp/admin/users.html` | `users.html` | Low |
| 11 | Update `webApp/admin/property-detail.html` | `property-detail.html` | Low |
| 12 | Deploy Firebase Hosting | Firebase CLI | Low |
| 13 | Full regression test on Android device | — | — |

---

## 12. Field Reference Tables

### Registration Fields by Subtype

| Field | Landlord | Agent | Brokerage | Required |
|---|---|---|---|---|
| Full Name | ✅ | ✅ | ✅ | Yes |
| Email | ✅ | ✅ | ✅ | Yes |
| Phone | ✅ | ✅ | ✅ | Yes |
| Gender | ✅ | ✅ | ✅ | Yes |
| National ID | ✅ | ✅ | ✅ | Yes |
| Profile Photo | ✅ | ✅ | ✅ | Yes |
| Password | ✅ | ✅ | ✅ | Yes |
| Agent License Number | ❌ | ✅ | ❌ | Yes (agent) |
| Years of Experience | ❌ | ✅ | ❌ | No |
| Company Name | ❌ | ❌ | ✅ | Yes (brokerage) |
| Company Reg Number | ❌ | ❌ | ✅ | Yes (brokerage) |
| Company Address | ❌ | ❌ | ✅ | Yes (brokerage) |
| Tax ID / ZIMRA | ❌ | ❌ | ✅ | No |

### Firestore `users` Document — Complete Field List (Post-Implementation)

| Field | Type | Source | Notes |
|---|---|---|---|
| `uid` | String | Firebase Auth | Document ID |
| `name` | String | Registration | Full name |
| `email` | String | Firebase Auth | Login email |
| `role` | String | Registration | `"landlord"` `"tenant"` `"admin"` |
| `providerSubtype` | String | Registration | `"landlord"` `"agent"` `"brokerage"` `""` |
| `status` | String | Default | `"active"` `"suspended"` |
| `profilePhotoUrl` | String | Storage upload | Firebase Storage URL |
| `phoneNumber` | String | Registration | With country code |
| `createdAt` | Long | Server time | Epoch milliseconds |
| `gender` | String | Registration | See gender options |
| `nationalId` | String | Registration | ZW format |
| `agentLicenseNumber` | String | Registration | Agent only |
| `yearsOfExperience` | String | Registration | Agent only |
| `companyName` | String | Registration | Brokerage only |
| `companyRegNumber` | String | Registration | Brokerage only |
| `companyAddress` | String | Registration | Brokerage only |
| `taxId` | String | Registration | Brokerage only, optional |

---

## Notes for Future Developers

1. **Never change `role` from `"landlord"` for agents or brokerages.** The entire permission system and query logic depends on `role == "landlord"`. Use `providerSubtype` for display and conditional UI only.

2. **All three subtypes share the same Firestore `properties` collection.** Do not create separate collections for agent-listed or brokerage-listed properties.

3. **The `deleteAccount()` function in `AuthViewModel` already handles full deletion** of all properties, transactions, and unlocks. No changes needed for the new subtypes.

4. **Backward compatibility:** Existing Landlord accounts without a `providerSubtype` field will read it as `""` (empty string). All extension helpers treat `""` the same as `"landlord"`. No migration script is needed.

5. **iOS:** All Kotlin changes in `commonMain` automatically apply to iOS since this is Kotlin Multiplatform. No iOS-specific changes are needed.

6. **The web admin is read-only for these fields** — it displays them but does not allow editing. If an admin needs to change a user's subtype, this must be done directly in the Firestore console.

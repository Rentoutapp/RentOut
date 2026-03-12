# Brokerage Subscription Float System — Implementation Design

## 1. Data Model

### 1.1 User Document Enhancement
Add to `User` model in `composeApp/src/commonMain/kotlin/org/example/project/data/model/User.kt`:

```kotlin
// Brokerage float tracking
val brokerageFloatBalance: Double = 100.0,     // Current float balance (starts at $100)
val brokerageFloatLastUpdated: Long = 0L,      // Last deduction timestamp
val brokerageIsFloatFrozen: Boolean = false,    // True if balance < $40 (read-only, derived)
```

### 1.2 New Collection: `brokerage_float_ledger`

Immutable audit trail for every float deduction and top-up.

**Document ID:** `{brokerage_uid}_{timestamp}_{action}` (auto-generated or sequential)

```typescript
{
  brokerage_uid: string,           // Brokerage user ID
  action: "deduction" | "topup",  // Type of ledger entry
  amount: number,                  // $1.50 for deduction, top-up varies
  reason: string,                  // "unlock_deduction_$10" or "manual_topup"
  before_balance: number,          // Float balance BEFORE this action
  after_balance: number,           // Float balance AFTER this action
  is_frozen_before: boolean,       // Was frozen before?
  is_frozen_after: boolean,        // Is frozen after?
  related_transaction_id?: string, // Link to transaction if deduction
  related_property_id?: string,    // Link to property if deduction
  created_at: FieldValue.serverTimestamp()
}
```

## 2. Server-Side Invariants & Cloud Functions

### 2.1 New Cloud Function: `onTenantUnlockPayment`

Trigger: `onDocumentCreated("unlocks/{unlockId}")`

**Logic:**
1. Get brokerage UID from property (property.landlordId → user doc → check if `providerSubtype == "brokerage"`)
2. If not brokerage, skip
3. Deduct $1.50 from `brokerageFloatBalance` (15% of $10)
4. If resulting balance < $40, set `brokerageIsFloatFrozen = true` on brokerage user doc
5. Create ledger entry in `brokerage_float_ledger` with before/after balance and frozen state
6. Log the action with property ID and tenant ID for disputes

**Error Handling:**
- If deduction fails, log but do NOT block the tenant's unlock (float is a courtesy system, not a hard requirement on payment)
- Idempotent via document ID uniqueness

### 2.2 New Cloud Function: `brokerageToppingUpFloat`

Callable: `onCall` (protected—brokerage user only)

**Input:**
```typescript
{
  amount: number,  // Top-up amount in USD (e.g., $60 to go from $30 → $90)
}
```

**Logic:**
1. Verify caller is a brokerage user (role='landlord' && providerSubtype='brokerage')
2. Add amount to `brokerageFloatBalance`
3. If new balance ≥ $40, set `brokerageIsFloatFrozen = false`
4. Create ledger entry with before/after state
5. Return new balance and frozen status

**Validation:**
- Amount must be > $0
- No upper limit (UX can suggest sensible values like $50–$100)

### 2.3 Firestore Rule Addition

In `firestore.rules`, add after property create rules:

```text
// Brokerage float ledger — read-only for brokerage owners, auditable by admin
match /brokerage_float_ledger/{ledgerId} {
  allow read: if isAuthenticated() && (
    // Brokerage can read their own ledger entries
    (isProvider() && ledgerId.beginsWith(request.auth.uid + '_'))
    // Admin can read all ledger entries for audit
    || isAdmin()
  );
  allow list: if isAuthenticated() && (
    isAdmin()
    || (isProvider() && ledgerId.beginsWith(request.auth.uid + '_'))
  );
  allow write: if false;  // Cloud Functions only
}
```

## 3. Frozen Listings Enforcement

### 3.1 Property Creation Block

In Cloud Function `onCall` for property creation (or in existing validation):

```typescript
// Before allowing property creation for a brokerage
const brokerage = await db.collection('users').doc(landlordId).get();
if (brokerage.data()?.providerSubtype === 'brokerage' 
    && brokerage.data()?.brokerageIsFloatFrozen === true) {
  throw new HttpsError(
    'permission-denied',
    'Your brokerage float is below $40. Top up to resume listings.'
  );
}
```

### 3.2 Tenant Unlock Block

In Cloud Function `initiatePayment`:

```typescript
// Before proceeding with unlock
const property = await db.collection('properties').doc(propertyId).get();
const landlord = await db.collection('users').doc(property.data().landlordId).get();
if (landlord.data()?.providerSubtype === 'brokerage' 
    && landlord.data()?.brokerageIsFloatFrozen === true) {
  throw new HttpsError(
    'permission-denied',
    'This property's brokerage has frozen listings due to insufficient float.'
  );
}
```

## 4. UI Surfaces

### 4.1 Brokerage Account Dashboard

**New Screen:** `LandlordProfileScreen.kt` (extend if brokerage) or new `BrokerageAccountScreen.kt`

**Components:**
- **Float Status Card (prominent)**
  - Display: `Balance: $XX.XX` (green if ≥ $40, red if < $40)
  - Animated progress bar: 0–100, with red zone at 0–40
  - Status badge: "Active" (green) or "⚠️ Frozen" (red)
  - CTA button: "Top Up Float" (only if frozen, or always available)

- **Top-Up Modal**
  - Input field: amount to add
  - Display calculation: "New balance: $YY.YY"
  - "Top Up" button → calls `brokerageToppingUpFloat`
  - Success toast: "Float topped up to $XX.XX"

- **Payment History / Float Ledger**
  - List of last 50 ledger entries (paginated)
  - Columns: Date | Type (Deduction/Top-up) | Amount | Related Property | Before | After | Frozen?
  - Filter by date range (optional)
  - Export to CSV (optional, for accounting)

### 4.2 Property Creation Flow (Brokerage)

**In `AddPropertyScreen.kt`:**
- Before form, show float warning if frozen:
  ```
  ⚠️ Your brokerage float is at $XX.XX (below $40 minimum).
  You cannot list properties until you top up.
  [Top Up Float] [View Details]
  ```
- Disable "Create Listing" button if frozen

### 4.3 Tenant Unlock Attempt

**In `PaymentScreen.kt`:**
- If property is from frozen brokerage, show error before checkout:
  ```
  ❌ This property is temporarily unavailable for unlock.
  The brokerage has insufficient float. Try another property.
  ```

### 4.4 Brokerage Profile View (for tenants)

**In `PropertyDetailScreen.kt` or brokerage card:**
- Show brokerage name, logo, contact
- **NO float information visible** (tenant-facing, not sensitive)

## 5. API Summary

### Cloud Functions
| Function | Type | Purpose |
|----------|------|---------|
| `onTenantUnlockPayment` | Trigger (Firestore) | Deduct $1.50 from brokerage float on unlock |
| `brokerageToppingUpFloat` | Callable | Allow brokerage to manually top up float |
| *(Enhance)* `initiatePayment` | Callable | Add frozen check before unlock allowed |

### Data Changes
| Collection | Field | Type | Notes |
|----------|-------|------|-------|
| `users` | `brokerageFloatBalance` | Double | $100 initial, ≥ $0 |
| `users` | `brokerageFloatLastUpdated` | Long | Timestamp of last change |
| `users` | `brokerageIsFloatFrozen` | Boolean | True if balance < $40 |
| *(new)* `brokerage_float_ledger` | *(see 1.2)* | Document | Audit trail |

## 6. Success Criteria Checklist

```
Mechanics:
☐ Every brokerage starts with $100 float on user creation
☐ Each tenant unlock deducts exactly $1.50 (15% of $10)
☐ Float minimum floor is $40 (validated in Cloud Functions)
☐ Frozen state set automatically when balance < $40
☐ Frozen state cleared automatically when balance ≥ $40 (after top-up)

Enforcement:
☐ Brokerage with frozen float cannot create new listings
☐ Tenant cannot unlock properties from frozen brokerage
☐ Idempotent float deductions (no double-charging on duplicate triggers)

Visibility:
☐ Brokerage sees float balance + frozen warning on dashboard
☐ Brokerage can top up via callable function
☐ Complete ledger history visible for audit / accounting
☐ Tenant sees friendly error if property is from frozen brokerage

Data Integrity:
☐ All float changes logged in brokerage_float_ledger
☐ Ledger entries immutable (created by Cloud Functions only)
☐ Admin can query full ledger for disputes / debugging
```

## 7. Implementation Order

1. **Add User model fields** → compile & test
2. **Create brokerage_float_ledger collection** → Firestore rules
3. **Implement `onTenantUnlockPayment` function** → test with demo unlock
4. **Enhance `initiatePayment` with frozen check** → test blocking
5. **Implement `brokerageToppingUpFloat` function** → test top-up flow
6. **Build dashboard float status card** → Compose UI
7. **Build top-up modal** → Compose UI
8. **Build ledger history list** → Compose UI
9. **Add frozen warnings to property create & unlock flows** → UX Polish
10. **Deploy functions + rules** → Firebase deploy
11. **End-to-end testing** → unlock multiple times, verify deductions, freeze, top up, unfreeze

---

**Notes:**
- Float is **advisory**, not hard-blocking on payment (float deduction failures don't cancel unlocks)
- Ledger is **immutable** for audit compliance
- All deductions are **deterministic** ($1.50 per unlock, no exceptions)
- Frozen state is **automatic**, no manual intervention needed
- Top-up is **manual** and brokerage-controlled (no auto-recharge)

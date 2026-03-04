# RentOut App - Client Journey Roadmap
## March 4–7, 2026 MVP Sprint

---

## Your Vision, Our Execution

This roadmap shows you exactly how I'll take RentOut from concept to a working MVP by **March 7, 2026**. this is considering the already set requirements shared on Whatsapp

---

### App Flow (As Per Your Requirements)

```
┌─────────────────┐
│   1. INTRO      │  ← Welcome screen with app branding
│    SCREEN       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  2. CHOOSE      │  ← User selects: Landlord OR Tenant
│     ROLE        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  3. AUTH        │  ← Login or Register (tab-based or button-based)
│    SCREEN       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  4. SPLASH      │  ← Loading, checks auth & role
│    SCREEN       │
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌────────┐  ┌────────┐
│LANDLORD│  │ TENANT │
│DASHBOARD│  │DASHBOARD│
└────────┘  └────────┘
```

---

## Day-by-Day Delivery Plan (March 4–7)

### 📅 Day 1 – Tuesday, March 4
**Foundation + Auth Flow**

| Time | What You Get | Status |
|------|-------------|--------|
| Morning | Project setup, Firebase connected | 🔄 In Progress |
| Afternoon | **1. Intro Screen** – Branding & welcome | ⏳ Pending |
| Afternoon | **2. Role Selection** – Landlord/Tenant cards | ⏳ Pending |
| Evening | **3. Auth Screen** – Login + Register tabs | ⏳ Pending |

**End of Day 1:** You can install the app and walk through Intro → Role → Auth screens.

---

### 📅 Day 2 – Wednesday, March 5
**Splash + Landlord Interface + Admin Panel**

| Time | What You Get | Status |
|------|-------------|--------|
| Morning | **4. Splash Screen** – Auto-redirect to dashboard | ⏳ Pending |
| Morning | **Landlord Dashboard** – Property overview | ⏳ Pending |
| Afternoon | **Add Property Screen** – Form to list properties | ⏳ Pending |
| Afternoon | **Admin Web Panel** – Approve/reject properties | ⏳ Pending |

**End of Day 2:** Landlords can add properties. Admin can view and approve them via web.

---

### 📅 Day 3 – Thursday, March 6
**Tenant Interface + Payment Flow**

| Time | What You Get | Status |
|------|-------------|--------|
| Morning | **Tenant Home** – Browse approved properties | ⏳ Pending |
| Morning | **Property Detail** – View with locked contact | ⏳ Pending |
| Afternoon | **Payment Screen** – $10 PesePay integration | ⏳ Pending |
| Evening | **Unlocked Properties** – View paid contacts | ⏳ Pending |

**End of Day 3:** Full workflow works: Landlord lists → Admin approves → Tenant browses → Pays $10 → Contact unlocked.

---

### 📅 Day 4 – Friday, March 7
**Polish + Testing + Client Demo**

| Time | What You Get | Status |
|------|-------------|--------|
| Morning | UI polish, animations, verified badges | ⏳ Pending |
| Midday | End-to-end testing of all flows | ⏳ Pending |
| Afternoon | **Signed APK delivered for your testing** | ⏳ Pending |
| Evening | **Live demo + handover session** | ⏳ Pending |

**End of Day 4:** MVP complete. You have a working Android app + Admin web panel.

---

## What You'll Have by March 7

### Mobile App (Android)
✅ **Intro Screen** – Welcomes users to the app  
✅ **Role Selection** – Clean choice between Landlord/Tenant  
✅ **Auth Screen** – Login & Register in one place  
✅ **Splash Screen** – Smooth transition to dashboard  
✅ **Landlord Dashboard** – Manage all property listings  
✅ **Add/Edit Property** – Full property form with image upload  
✅ **Tenant Browse** – Search and filter approved properties  
✅ **Property Details** – All info with unlockable contact  
✅ **Payment (PesePay)** – $10 to unlock contact via Ecocash/Card  
✅ **My Unlocked Properties** – List of all paid contacts  

### Admin Web Panel
✅ **Dashboard** – Stats overview  
✅ **Properties** – Approve/reject listings  
✅ **Users** – Suspend/reactivate accounts  
✅ **Transactions** – View all $10 payments  

---

## Demo Credentials (For March 7)

| Account | Email | Password | Purpose |
|---------|-------|----------|---------|
| Landlord | landlord@rentout.demo | demo1234! | Test listing a property |
| Tenant | tenant@rentout.demo | demo1234! | Test browsing & paying |
| Admin | admin@rentout.demo | demo1234! | Access web panel |

---

## Your Core Business Workflow

```
LANDLORD                    ADMIN                    TENANT
    │                         │                         │
    ▼                         │                         │
┌──────────┐                  │                         │
│  ADD     │ ──────────────►  │                         │
│ PROPERTY │   (pending)       │                         │
└──────────┘                  ▼                         │
                        ┌──────────┐                   │
                        │ APPROVE  │ ──────────────►   │
                        │ PROPERTY │   (verified)       │
                        └──────────┘                   ▼
                                              ┌──────────┐
                                              │  VIEW    │
                                              │ PROPERTY │
                                              └────┬─────┘
                                                   │
                                                   ▼
                                              ┌──────────┐
                                              │ PAY $10  │
                                              │  (unlock)│
                                              └────┬─────┘
                                                   │
                                                   ▼
                                              ┌──────────┐
                                              │ CONTACT  │
                                              │ REVEALED │
                                              └──────────┘
```

---

## March 7 Deliverables

1. **Android APK** – Installable on your device
2. **Admin Web Panel URL** – Access from any browser - NB: accessed locally since domain hasn't been created yet for external access
3. **Source Code** – Full project on GitHub

---

**Target: March 7, 2026 | Status: On Track**

---

*RentOut MVP Sprint | Kotlin Multiplatform + Firebase | 4-Day Delivery*

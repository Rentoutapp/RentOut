# RentOut App Workflow Diagram
## Client-Friendly Flowchart | March 2026

---

## User Journey Through the App

```mermaid
flowchart TD
    A[📱 App Launch] --> B[1️⃣ Intro Screen]
    B --> C[Get Started Button]
    C --> D[2️⃣ Choose Role Screen]
    
    D --> E[🏠 I'm a Landlord]
    D --> F[🔑 I'm a Tenant]
    
    E --> G[3️⃣ Auth Screen]
    F --> G
    
    G --> H{Have Account?}
    H -->|Yes| I[Login Tab]
    H -->|No| J[Register Tab]
    
    I --> K[Email + Password]
    J --> L[Name + Email + Password]
    
    K --> M[4️⃣ Splash Screen]
    L --> M
    
    M --> N{Check Role}
    N -->|Landlord| O[🏠 Landlord Dashboard]
    N -->|Tenant| P[🔑 Tenant Dashboard]
    
    O --> O1[My Listings]
    O --> O2[Add Property]
    O --> O3[Property Stats]
    
    P --> P1[Browse Properties]
    P --> P2[Search & Filter]
    P --> P3[My Unlocked Contacts]
    
    style A fill:#1B4FFF,color:#fff
    style B fill:#FF6B35,color:#fff
    style D fill:#FF6B35,color:#fff
    style G fill:#FF6B35,color:#fff
    style M fill:#FF6B35,color:#fff
    style O fill:#22C55E,color:#fff
    style P fill:#22C55E,color:#fff
```

---

## Core Business Workflow (Property Listing → Unlock)

```mermaid
flowchart LR
    subgraph Landlord
        L1[Add Property] --> L2[Submit for Review]
        L2 --> L3[Pending Status]
    end
    
    subgraph Admin
        A1[Review Property] --> A2{Approve?}
        A2 -->|Yes| A3[Mark Approved + Verified]
        A2 -->|No| A4[Reject Property]
    end
    
    subgraph Tenant
        T1[Browse Properties] --> T2[View Details]
        T2 --> T3{Contact Unlocked?}
        T3 -->|No| T4[Pay $10 via PesePay]
        T4 --> T5[Contact Revealed]
        T3 -->|Yes| T5
        T5 --> T6[Call / WhatsApp Landlord]
    end
    
    L3 --> A1
    A3 --> T1
    
    style L2 fill:#FCD34D
    style A3 fill:#22C55E,color:#fff
    style T4 fill:#1B4FFF,color:#fff
    style T5 fill:#22C55E,color:#fff
```

---

## Screen-by-Screen Breakdown

### 1. INTRO SCREEN
```
┌─────────────────────────┐
│                         │
│      [LOGO/ICON]        │
│                         │
│   Welcome to RentOut    │
│                         │
│   Find your perfect     │
│   rental or list your   │
│   property              │
│                         │
│   [Get Started →]       │
│                         │
└─────────────────────────┘
```

### 2. CHOOSE ROLE SCREEN
```
┌─────────────────────────┐
│    I want to...         │
│                         │
│  ┌─────────────────┐    │
│  │    🏠           │    │
│  │   LANDLORD      │    │
│  │                 │    │
│  │ List properties │    │
│  │ & earn money    │    │
│  └─────────────────┘    │
│                         │
│  ┌─────────────────┐    │
│  │    🔑           │    │
│  │    TENANT       │    │
│  │                 │    │
│  │ Find rentals    │    │
│  │ Pay $10 to      │    │
│  │ unlock contacts │    │
│  └─────────────────┘    │
│                         │
└─────────────────────────┘
```

### 3. AUTH SCREEN (Tabs)
```
┌─────────────────────────┐
│  ┌───────┐ ┌───────┐   │
│  │ LOGIN │ │REGISTER│   │
│  └───┬───┘ └───────┘   │
│      │                 │
│  Email: [           ]   │
│                         │
│  Password: [        ]   │
│                         │
│  [  Sign In  ]          │
│                         │
│  Forgot Password?       │
│                         │
└─────────────────────────┘
```

### 4. SPLASH SCREEN
```
┌─────────────────────────┐
│                         │
│      [ANIMATED          │
│        LOGO]            │
│                         │
│   Setting up your       │
│   experience...         │
│                         │
│      [spinner]          │
│                         │
└─────────────────────────┘
```

---

## Landlord Dashboard Flow

```
┌─────────────────────────┐
│  🏠 LANDLORD DASHBOARD  │
├─────────────────────────┤
│                         │
│  [STATS OVERVIEW]       │
│  ┌────┐┌────┐┌────┐    │
│  │ 5  ││ 3  ││ 2  │    │
│  │Total││Appr││Pend│    │
│  └────┘└────┘└────┘    │
│                         │
│  [MY LISTINGS]          │
│  ┌─────────────────┐    │
│  │ 🏠 Villa in... │ ✅ │
│  │    $500/mo     │Verif│
│  │    [Edit] [Del]│    │
│  └─────────────────┘    │
│                         │
│  ┌─────────────────┐    │
│  │ 🏠 Apartment... │ ⏳ │
│  │    $350/mo     │Pend │
│  │    [Edit] [Del]│    │
│  └─────────────────┘    │
│                         │
│  [  + Add Property  ]   │
└─────────────────────────┘
```

---

## Tenant Dashboard Flow

```
┌─────────────────────────┐
│   🔑 TENANT DASHBOARD   │
├─────────────────────────┤
│                         │
│  🔍 Search...           │
│                         │
│  [Filters] [City ▼]     │
│                         │
│  ┌─────────────────┐    │
│  │  [🏠 IMAGE]     │    │
│  │  Villa in Avondale│   │
│  │  $500/mo • 3 bed  │   │
│  │  ✅ Verified      │   │
│  │  📍 Harare        │    │
│  └─────────────────┘    │
│                         │
│  ┌─────────────────┐    │
│  │  [🏠 IMAGE]     │    │
│  │  Apartment in...│    │
│  │  $350/mo • 2 bed  │   │
│  │  ✅ Verified      │   │
│  └─────────────────┘    │
│                         │
│  [My Unlocked 🔑]       │
└─────────────────────────┘
```

---

## Property Detail & Unlock Flow

```
┌─────────────────────────┐     ┌─────────────────────────┐
│   PROPERTY DETAILS      │     │     PAYMENT SCREEN      │
├─────────────────────────┤     ├─────────────────────────┤
│  [FULL WIDTH IMAGE]     │────▶│                         │
│                         │     │  Unlock Contact         │
│  🏠 Villa in Avondale   │     │                         │
│  ✅ Verified by RentOut │     │  Property:             │
│  $500/month             │     │  Villa in Avondale      │
│  3 bedrooms • 2 bath    │     │                         │
│                         │     │  Amount:                │
│  📍 Location: Avondale  │     │  $10.00 USD             │
│  📅 Available: Now      │     │                         │
│                         │     │  ┌─────────────────┐    │
│  📞 Contact:            │     │  │  PesePay        │    │
│  ••••••••••            │     │  │  Visa/Mastercard│    │
│                         │     │  │  Ecocash        │    │
│  [Unlock - $10 →]       │     │  │  OneMoney       │    │
│                         │     │  └─────────────────┘    │
│  [Report Listing]       │     │                         │
│                         │     │  [ Pay $10 to Unlock ]  │
└─────────────────────────┘     └─────────────────────────┘
                                          │
                                          ▼
                          ┌─────────────────────────┐
                          │    PAYMENT SUCCESS      │
                          ├─────────────────────────┤
                          │                         │
                          │      ✅ UNLOCKED!       │
                          │                         │
                          │  📞 0772 123 456        │
                          │                         │
                          │  [  Call  ] [WhatsApp]  │
                          │                         │
                          │  [← Back to Property]   │
                          └─────────────────────────┘
```

---

## Admin Web Panel Flow

```mermaid
flowchart TD
    A[Admin Login] --> B[Dashboard]
    B --> C[Properties]
    B --> D[Users]
    B --> E[Transactions]
    
    C --> C1[Pending Tab]
    C --> C2[Approved Tab]
    C --> C3[Rejected Tab]
    
    C1 --> C4[Review Property]
    C4 --> C5[Approve Button]
    C4 --> C6[Reject Button]
    
    D --> D1[Suspend User]
    D --> D2[Reactivate User]
    
    E --> E1[View Revenue]
    E --> E2[Export CSV]
    
    style B fill:#1B4FFF,color:#fff
    style C5 fill:#22C55E,color:#fff
    style C6 fill:#EF4444,color:#fff
```

---

## Simple Text Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                      USER APP FLOW                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. INTRO        →  Welcome + Get Started                      │
│       │                                                         │
│       ▼                                                         │
│  2. CHOOSE ROLE  →  Landlord OR Tenant                         │
│       │                                                         │
│       ▼                                                         │
│  3. AUTH         →  Login OR Register                            │
│       │                                                         │
│       ▼                                                         │
│  4. SPLASH       →  Loading + Auto-redirect                    │
│       │                                                         │
│       ▼                                                         │
│  5. DASHBOARD    →  Role-based:                                 │
│       • Landlord → Manage properties                            │
│       • Tenant   → Browse + Pay $10 for contact               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Key Interactions Summary

| Screen | User Action | Result |
|--------|-------------|--------|
| Intro | Tap "Get Started" | Go to Role Selection |
| Role | Select Landlord/Tenant | Go to Auth (role saved) |
| Auth | Login/Register | Go to Splash (auth check) |
| Splash | Wait | Auto-redirect to Dashboard |
| Landlord Dashboard | Tap "Add Property" | Property form opens |
| Landlord Dashboard | Submit property | Goes to "Pending" for admin |
| Tenant Dashboard | Tap property card | View details |
| Property Detail | Tap "Unlock $10" | Payment screen opens |
| Payment | Complete PesePay | Contact number revealed |
| Admin Panel | Click "Approve" | Property gets Verified badge |

---

*Diagram created for RentOut MVP | March 2026*

# PropertyDetailScreen.kt — isBrokerage & Contact Section Analysis

## Summary

File: `composeApp/src/commonMain/kotlin/org/example/project/ui/screens/tenant/PropertyDetailScreen.kt`

---

## 1. PROVIDER SUBTYPE BADGE ("Listed by" section)

### Location: Lines 243–253

```kotlin
// Line 243: Comment
// Provider subtype badge — full width row

// Line 244–248: providerLabel when-expression
val providerLabel = when (property.providerSubtype) {
    "agent"     -> "🤝 Listed by Agent"
    "brokerage" -> "🏢 Listed by Brokerage"
    else        -> "🏠 Listed by Landlord"
}

// Line 249–253: providerColor when-expression
val providerColor = when (property.providerSubtype) {
    "agent"     -> Color(0xFF00897B)
    "brokerage" -> Color(0xFF7C5CBF)
    else        -> MaterialTheme.colorScheme.primary
}
```

**Key Details:**
- **Line 244**: `property.providerSubtype` is checked
- **Line 246**: `"brokerage" -> "🏢 Listed by Brokerage"` — exact display text for brokerage
- **Line 251**: `"brokerage" -> Color(0xFF7C5CBF)` — brokerage color (purple #7C5CBF)
- This badge appears at the top of the property card, centered

---

## 2. CONTACT SECTION FUNCTION SIGNATURE

### Location: Lines 1041–1047

```kotlin
@Composable
private fun TenantContactContent(
    property: Property,
    isUnlocked: Boolean,
    onUnlock: () -> Unit,
    onCall: (String) -> Unit,
    onWhatsApp: (String) -> Unit
) {
```

---

## 3. KEY VARIABLE DECLARATIONS IN TenantContactContent

### Location: Lines 1048–1057

```kotlin
// Line 1048–1052: Animation state
val contactAlpha by animateFloatAsState(
    targetValue = if (isUnlocked) 1f else 0f,
    animationSpec = tween(600, easing = FastOutSlowInEasing),
    label = "contact_reveal"
)

// Line 1054: isAgent check
val isAgent     = property.providerSubtype == "agent"

// Line 1055: isBrokerage check
val isBrokerage = property.providerSubtype == "brokerage"

// Line 1056: agentColor definition
val agentColor     = Color(0xFF00897B)

// Line 1057: brokerageColor definition
val brokerageColor = Color(0xFF7C5CBF)
```

**Critical Lines:**
- **Line 1054**: `val isAgent = property.providerSubtype == "agent"`
- **Line 1055**: `val isBrokerage = property.providerSubtype == "brokerage"`

---

## 4. CONTACT CONTENT STRUCTURE (isUnlocked state)

### Location: Lines 1071–1342

The contact content displays different layouts based on `when` conditions:

#### 4A. BROKERAGE SECTION
**Lines 1074–1223**

```kotlin
isBrokerage -> {
    // ── Brokerage logo + company name header ──────────────
    // Lines 1076–1124: Header with logo and brokerage name
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(brokerageColor.copy(alpha = 0.07f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo box (lines 1085–1107)
        Box(...) {
            if (property.brokerageLogoUrl.isNotBlank()) {
                AsyncImage(...)
            } else {
                Icon(Icons.Default.Business, "Brokerage Logo", tint = brokerageColor, ...)
            }
        }
        
        // Company name (lines 1109–1123)
        Column {
            if (property.brokerageName.isNotBlank()) {
                Text(property.brokerageName, ..., color = brokerageColor)
            }
            Text("🏢 Brokerage", ...)
        }
    }
    
    // ── Broker Details ────────────────────────────────────
    // Lines 1131–1138: Broker name
    ContactRevealRow(
        label = "Broker",
        value = property.brokerName.ifBlank { property.landlordName },
        icon = Icons.Default.Person,
        iconBg = brokerageColor.copy(alpha = 0.12f),
        iconTint = brokerageColor,
        alpha = contactAlpha
    )
    
    // Lines 1143–1151: Broker contact
    ContactRevealRow(
        label = "Broker Contact",
        value = property.brokerContactNumber,
        icon = Icons.Default.Call,
        iconBg = brokerageColor.copy(alpha = 0.15f),
        iconTint = brokerageColor,
        alpha = contactAlpha,
        isPhone = true
    )
    
    // ── Brokerage Office Details ──────────────────────────
    // Lines 1159–1166: Office address (conditional)
    ContactRevealRow(
        label = "Office Address",
        value = property.brokerageAddress,
        ...
    )
    
    // Lines 1173–1181: Office contact (conditional)
    ContactRevealRow(
        label = "Office Contact",
        value = property.brokerageContactNumber,
        ...
    )
    
    // Lines 1188–1195: Office email (conditional)
    ContactRevealRow(
        label = "Office Email",
        value = property.brokerageEmail,
        ...
    )
    
    // Lines 1201–1222: Action buttons
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RentOutPrimaryButton("📞 Call Broker", onClick = { onCall(property.brokerContactNumber) }, ...)
        RentOutSecondaryButton("💬 WhatsApp", onClick = { onWhatsApp(property.brokerContactNumber) }, ...)
    }
    // Office call button if different number (lines 1214–1222)
    if (property.brokerageContactNumber.isNotBlank() &&
        property.brokerageContactNumber != property.brokerContactNumber) {
        RentOutSecondaryButton("📞 Call Office", onClick = { onCall(property.brokerageContactNumber) }, ...)
    }
}
```

#### 4B. AGENT SECTION
**Lines 1226–1289**

```kotlin
isAgent -> {
    // Lines 1228–1235: Agent name
    ContactRevealRow(
        label = "Agent",
        value = property.agentName.ifBlank { property.landlordName },
        icon = Icons.Default.Person,
        iconBg = agentColor.copy(alpha = 0.12f),
        iconTint = agentColor,
        alpha = contactAlpha
    )
    
    // Lines 1240–1248: Agent contact
    ContactRevealRow(
        label = "Agent Contact",
        value = property.agentContactNumber,
        icon = Icons.Default.Call,
        iconBg = agentColor.copy(alpha = 0.15f),
        iconTint = agentColor,
        alpha = contactAlpha,
        isPhone = true
    )
    
    // Lines 1254–1261: Property address
    ContactRevealRow(
        label = "Property Address",
        value = property.location,
        ...
    )
    
    // Lines 1267–1275: Landlord contact
    ContactRevealRow(
        label = "Landlord Contact",
        value = property.contactNumber,
        icon = Icons.Default.Call,
        iconBg = RentOutColors.StatusApproved.copy(alpha = 0.15f),
        iconTint = RentOutColors.StatusApproved,
        alpha = contactAlpha,
        isPhone = true
    )
    
    // Lines 1277–1289: Call/WhatsApp buttons
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RentOutPrimaryButton("📞 Call Agent", onClick = { onCall(property.agentContactNumber) }, ...)
        RentOutSecondaryButton("💬 WhatsApp", onClick = { onWhatsApp(property.agentContactNumber) }, ...)
    }
}
```

#### 4C. LANDLORD SECTION (default/else)
**Lines 1292–1341**

```kotlin
else -> {
    // Lines 1294–1301: Landlord name
    ContactRevealRow(
        label = "Landlord",
        value = property.landlordName,
        icon = Icons.Default.Person,
        iconBg = DetailNavy.copy(alpha = 0.12f),
        iconTint = DetailNavy,
        alpha = contactAlpha
    )
    
    // Lines 1307–1314: Property address
    ContactRevealRow(
        label = "Property Address",
        value = property.location,
        icon = Icons.Default.LocationOn,
        iconBg = DetailNavy.copy(alpha = 0.10f),
        iconTint = DetailNavy,
        alpha = contactAlpha
    )
    
    // Lines 1319–1327: Contact number
    ContactRevealRow(
        label = "Contact Number",
        value = property.contactNumber,
        icon = Icons.Default.Call,
        iconBg = RentOutColors.StatusApproved.copy(alpha = 0.15f),
        iconTint = RentOutColors.StatusApproved,
        alpha = contactAlpha,
        isPhone = true
    )
    
    // Lines 1329–1340: Call/WhatsApp buttons
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RentOutPrimaryButton("📞 Call", onClick = { onCall(property.contactNumber) }, ...)
        RentOutSecondaryButton("💬 WhatsApp", onClick = { onWhatsApp(property.contactNumber) }, ...)
    }
}
```

---

## 5. LOCKED STATE CONTACT SECTION

### Location: Lines 1343–1412

```kotlin
} else {
    // ── LOCKED state — generic masked rows ────────────────────────
    
    // Lines 1346–1369: First masked row
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(...) {
            Icon(Icons.Default.Lock, null, tint = RentOutColors.IconAmber, ...)
        }
        Column {
            Text(
                when {
                    isBrokerage -> "Broker"
                    isAgent     -> "Agent"
                    else        -> "Landlord"
                },
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("••••••••••••", ...)  // Masked text
        }
    }
    
    // Lines 1373–1390: Second masked row
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(...) {
            Icon(Icons.Default.Lock, null, tint = RentOutColors.IconAmber, ...)
        }
        Column {
            Text("Contact Number", fontSize = 12.sp, ...)
            Text("•••••••••••••", ...)  // Masked text
        }
    }
    
    // Lines 1392–1411: Unlock button or unavailable message
    if (property.isAvailable) {
        RentOutPrimaryButton(
            text = "🔓 Unlock Contact — $10",
            onClick = onUnlock,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Card(...) {
            Text(
                "🔴 This property is no longer available",
                color = RentOutColors.StatusRejected,
                ...
            )
        }
    }
}
```

**Key Lines in Locked State:**
- **Lines 1357–1359**: Label determination using `isBrokerage` and `isAgent`:
  ```kotlin
  when {
      isBrokerage -> "Broker"
      isAgent     -> "Agent"
      else        -> "Landlord"
  }
  ```

---

## SUMMARY TABLE

| Aspect | Location | Details |
|--------|----------|---------|
| **providerSubtype checks** | 244, 249, 1054, 1055 | Used in when-expressions and boolean assignments |
| **isBrokerage definition** | Line 1055 | `val isBrokerage = property.providerSubtype == "brokerage"` |
| **isAgent definition** | Line 1054 | `val isAgent = property.providerSubtype == "agent"` |
| **"Listed by" badge text** | Line 246 | `"🏢 Listed by Brokerage"` |
| **Brokerage color** | Line 251, 1057 | `Color(0xFF7C5CBF)` (purple) |
| **Agent color** | Line 250, 1056 | `Color(0xFF00897B)` (teal) |
| **Brokerage section** | Lines 1074–1223 | Shows: logo, company name, broker, broker contact, office details |
| **Agent section** | Lines 1226–1289 | Shows: agent name, agent contact, property address, landlord contact |
| **Landlord section** | Lines 1292–1341 | Shows: landlord name, property address, contact number |
| **Locked state labels** | Lines 1357–1359 | Dynamic label based on `isBrokerage`/`isAgent` |
| **Contact reveal animation** | Lines 1048–1052 | `contactAlpha` animates from 0 to 1 when unlocked |


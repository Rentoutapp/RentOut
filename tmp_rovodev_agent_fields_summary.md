# Agent & Landlord Contact Fields in AddPropertyScreen.kt

## Summary
This document shows all code related to: `agentName`, `agentContactNumber`, `agentLicenseNumber`, `landlordPhoneNumber`, `providerSubtype`, and `agent` in the AddPropertyScreen.kt file.

> **Note:** No references to `agentLicenseNumber` were found in this file.

---

## 1. Function Signature (Lines 122–133)

```kotlin
fun AddPropertyScreen(
    formState: PropertyFormState,
    onSubmit: (Property) -> Unit,
    onBack: () -> Unit,
    onNavigateToImages: (Property) -> Unit = {},
    landlordPhoneNumber: String = "",                    // ← Parameter
    providerSubtype: String = "landlord",               // ← Parameter ("landlord" | "agent" | "brokerage")
    draft: PropertyDraft = PropertyDraft(),
    onSaveDraft: (PropertyDraft) -> Unit = {},
    isEditMode: Boolean = false,
    existingImageUrls: List<String> = emptyList()
)
```

---

## 2. State Variables & Initialization (Lines 167–186)

### Contact Field (Lines 167–170)
```kotlin
var contact by remember {
    mutableStateOf(draft.contact.ifEmpty { landlordPhoneNumber })  // ← Uses landlordPhoneNumber as fallback
}
val isContactAutoFilled = landlordPhoneNumber.isNotBlank()  // ← Tracks if contact was auto-filled
```

### Provider Type Flags (Lines 173–174)
```kotlin
val isAgent     = providerSubtype == "agent"      // ← Derived from providerSubtype
val isBrokerage = providerSubtype == "brokerage"  // ← Derived from providerSubtype
```

### Agent Fields (Lines 175–176)
```kotlin
var agentName            by remember { mutableStateOf(draft.agentName) }
var agentContactNumber   by remember { mutableStateOf(draft.agentContactNumber.ifEmpty { landlordPhoneNumber }) }
```

### Brokerage Fields (Lines 177–180)
```kotlin
var brokerName           by remember { mutableStateOf(draft.brokerName) }
var brokerContactNumber  by remember { mutableStateOf(draft.brokerContactNumber.ifEmpty { landlordPhoneNumber }) }
var brokerageAddress     by remember { mutableStateOf(draft.brokerageAddress) }
var brokerageContactNumber by remember { mutableStateOf(draft.brokerageContactNumber) }
```

### Error State Variables (Lines 181–186)
```kotlin
var agentNameErr         by remember { mutableStateOf("") }
var agentContactErr      by remember { mutableStateOf("") }
var brokerNameErr        by remember { mutableStateOf("") }
var brokerContactErr     by remember { mutableStateOf("") }
var brokerageAddressErr  by remember { mutableStateOf("") }
var brokerageContactErr  by remember { mutableStateOf("") }
```

---

## 3. Validation Logic (Lines 255–269)

### Agent Validation (Lines 255–259)
```kotlin
isAgent -> {
    if (agentName.isBlank())          { agentNameErr    = "Agent full name is required";    valid = false }
    if (agentContactNumber.isBlank()) { agentContactErr = "Agent contact number is required"; valid = false }
    if (contact.isBlank())            { contactErr      = "Landlord contact number is required"; valid = false }
}
```

### Brokerage Validation (Lines 260–265)
```kotlin
isBrokerage -> {
    if (brokerName.isBlank())             { brokerNameErr      = "Broker full name is required";      valid = false }
    if (brokerContactNumber.isBlank())    { brokerContactErr   = "Broker contact number is required"; valid = false }
    if (brokerageAddress.isBlank())       { brokerageAddressErr = "Brokerage address is required";    valid = false }
    if (brokerageContactNumber.isBlank()) { brokerageContactErr = "Brokerage contact is required";   valid = false }
}
```

### Default Validation (Lines 266–268)
```kotlin
else -> {
    if (contact.isBlank())            { contactErr = "Contact number is required"; valid = false }
}
```

---

## 4. Property Construction (Lines 295–303)

```kotlin
Property(
    // ... other fields ...
    contactNumber          = contact.trim(),
    // Agent fields
    agentName              = if (isAgent) agentName.trim() else "",
    agentContactNumber     = if (isAgent) agentContactNumber.trim() else "",
    // Brokerage fields
    brokerName             = if (isBrokerage) brokerName.trim() else "",
    brokerContactNumber    = if (isBrokerage) brokerContactNumber.trim() else "",
    brokerageAddress       = if (isBrokerage) brokerageAddress.trim() else "",
    brokerageContactNumber = if (isBrokerage) brokerageContactNumber.trim() else "",
    // ... other fields ...
)
```

---

## 5. UI Sections

### Agent Details UI (Lines 776–811)

**Condition:** `isAgent -> { ... }`

```kotlin
isAgent -> {
    // Agent: agent name + agent contact + landlord contact
    Text("Agent Details", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        color = Color(0xFF00897B))
    Spacer(Modifier.height(8.dp))
    
    // Agent Name Field
    RentOutTextField(
        value         = agentName,
        onValueChange = { agentName = it; agentNameErr = "" },
        label         = "Agent Full Name",
        leadingIcon   = Icons.Default.Person,
        leadingIconTint = Color(0xFF00897B),
        isError       = agentNameErr.isNotEmpty(),
        errorMessage  = agentNameErr
    )
    Spacer(Modifier.height(10.dp))
    
    // Agent Contact Number Field
    RentOutTextField(
        value           = agentContactNumber,
        onValueChange   = { agentContactNumber = it; agentContactErr = "" },
        label           = "Agent Contact Number",
        leadingIcon     = Icons.Default.Phone,
        leadingIconTint = Color(0xFF00897B),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        isError         = agentContactErr.isNotEmpty(),
        errorMessage    = agentContactErr
    )
    Spacer(Modifier.height(16.dp))
    
    // Landlord Contact (nested component)
    Text("Landlord Contact", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    ContactDetailsSection(
        contact      = contact,
        onContact    = { contact = it; contactErr = "" },
        contactErr   = contactErr,
        isAutoFilled = isContactAutoFilled
    )
}
```

**Line Ranges:** 776–811

---

### Brokerage Details UI (Lines 812–...)

**Condition:** `isBrokerage -> { ... }`

```kotlin
isBrokerage -> {
    // Brokerage: broker name + broker contact + brokerage address + brokerage contact
    Text("Broker Details", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        color = Color(0xFF7C5CBF))
    Spacer(Modifier.height(8.dp))
    RentOutTextField(
        value         = brokerName,
        onValueChange = { brokerName = it; brokerNameErr = "" },
        // ... more fields ...
    )
    // ... additional fields ...
}
```

**Line Ranges:** 812+ (continues beyond the expanded section)

---

## 6. Draft Saving (Lines 940–950)

```kotlin
PropertyDraft(
    // ... other fields ...
    contact                = contact,
    amenityKeys            = selectedAmenityKeys,
    agentName              = agentName,
    agentContactNumber     = agentContactNumber,
    brokerName             = brokerName,
    brokerContactNumber    = brokerContactNumber,
    brokerageAddress       = brokerageAddress,
    brokerageContactNumber = brokerageContactNumber
)
```

**Context:** These fields are passed to `onSaveDraft()` callback at lines 940–950.

---

## 7. ContactDetailsSection Composable (Lines 3145–3179)

```kotlin
@Composable
private fun ContactDetailsSection(
    contact: String,
    onContact: (String) -> Unit,
    contactErr: String,
    isAutoFilled: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (isAutoFilled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RentOutColors.Tertiary.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = RentOutColors.Tertiary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Auto-filled from your profile", fontSize = 11.sp, color = RentOutColors.Tertiary, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
        }
        RentOutTextField(
            value           = contact,
            onValueChange   = onContact,
            label           = "Contact Number",
            leadingIcon     = Icons.Default.Phone,
            leadingIconTint = RentOutColors.IconGreen,
            isError         = contactErr.isNotEmpty(),
            errorMessage    = contactErr,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            labelFontSize   = 12.sp
        )
    }
}
```

---

## Field Usage Summary

| Field | Initialized From | Used In | Purpose |
|-------|------------------|---------|---------|
| `providerSubtype` | Parameter | Lines 173, 174, 256, 260, 296, 298, 299, 300, 301, 302 | Determines if user is landlord/agent/brokerage |
| `landlordPhoneNumber` | Parameter | Lines 168, 170, 176, 178 | Auto-fill contact fields from user profile |
| `contact` | Draft or `landlordPhoneNumber` | Lines 168, 169, 170, 256, 258, 294, 805, 806, 807, 942 | Main landlord contact number |
| `isAgent` | Derived from `providerSubtype` | Lines 173, 256, 296, 297, 776 | Conditional UI/validation for agent mode |
| `isBrokerage` | Derived from `providerSubtype` | Lines 174, 260, 299, 300, 301, 302, 812 | Conditional UI/validation for brokerage mode |
| `agentName` | Draft | Lines 175, 256, 296, 783, 944 | Agent's full name (agent mode only) |
| `agentContactNumber` | Draft or `landlordPhoneNumber` | Lines 176, 257, 297, 793, 945 | Agent's contact number (agent mode only) |
| `agentNameErr` | Initialized empty | Lines 181, 256, 783 | Error message for agent name validation |
| `agentContactErr` | Initialized empty | Lines 182, 257, 793 | Error message for agent contact validation |

---

## Key Observations

1. **No `agentLicenseNumber`** field exists in this file
2. **`landlordPhoneNumber`** is used as a fallback for `agentContactNumber` and `brokerContactNumber`
3. **Auto-fill functionality** is tracked via `isContactAutoFilled` flag
4. **Conditional rendering** based on `isAgent` and `isBrokerage` flags
5. **Agent details** are only saved to the Property object if `isAgent` is true
6. **ContactDetailsSection** shows auto-fill indicator when `landlordPhoneNumber` is pre-filled

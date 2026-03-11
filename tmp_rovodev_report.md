# TenantHomeScreen.kt Analysis Report

## File Location
`composeApp/src/commonMain/kotlin/org/example/project/ui/screens/tenant/TenantHomeScreen.kt`

---

## 1. "Listed by" Text References

### In TenantHomeScreen.kt
No direct "Listed by" text found in TenantHomeScreen.kt. The property cards are rendered using the `PropertyCard` composable.

### In PropertyCard.kt
**Line 154-179:** "Listed by" banner at bottom-right of property card image

```kotlin
   154                 // Listed by banner — bottom right of image
   155                 val listedByText = when (property.providerSubtype) {
   156                     "agent"     -> "🤝 Agent"
   157                     "brokerage" -> "🏢 Brokerage"
   158                     else        -> "🏠 Landlord"
   159                 }
   160                 val listedByColor = when (property.providerSubtype) {
   161                     "agent"     -> Color(0xFF00897B)
   162                     "brokerage" -> Color(0xFF7C5CBF)
   163                     else        -> RentOutColors.Primary
   164                 }
   165                 Box(
   166                     modifier = Modifier
   167                         .align(Alignment.BottomEnd)
   168                         .padding(10.dp)
   169                         .clip(RoundedCornerShape(10.dp))
   170                         .background(listedByColor.copy(alpha = 0.88f))
   171                         .padding(horizontal = 9.dp, vertical = 5.dp)
   172                 ) {
   173                     Text(
   174                         text = listedByText,
   175                         color = Color.White,
   176                         fontSize = 10.sp,
   177                         fontWeight = FontWeight.SemiBold
   178                     )
   179                 }
```

---

## 2. providerSubtype, isBrokerage, isAgent Logic

### providerSubtype Usage in PropertyCard.kt

**Line 155-159:** Determines "Listed by" display text
```kotlin
   155                 val listedByText = when (property.providerSubtype) {
   156                     "agent"     -> "🤝 Agent"
   157                     "brokerage" -> "🏢 Brokerage"
   158                     else        -> "🏠 Landlord"
   159                 }
```

**Line 160-164:** Determines badge color based on provider subtype
```kotlin
   160                 val listedByColor = when (property.providerSubtype) {
   161                     "agent"     -> Color(0xFF00897B)       // Teal
   162                     "brokerage" -> Color(0xFF7C5CBF)       // Purple
   163                     else        -> RentOutColors.Primary   // Default (Landlord)
   164                 }
```

**Line 290:** VerifiedBadge receives providerSubtype parameter
```kotlin
   290                     if (property.isVerified) VerifiedBadge(property.providerSubtype)
```

### In TenantHomeScreen.kt - Active Filter Chips
**Lines 409-419:** Filter chip rendering for providerTypes

```kotlin
   409                                 activeFilter.providerTypes.forEach { pt ->
   410                                     item {
   411                                         val label = when (pt) {
   412                                             "landlord"  -> "🏠 Landlord"
   413                                             "agent"     -> "🤝 Agent"
   414                                             "brokerage" -> "🏢 Brokerage"
   415                                             else        -> pt.replaceFirstChar { it.uppercase() }
   416                                         }
   417                                         ActiveFilterChip(label, onRemove = { onFilterChange(activeFilter.copy(providerTypes = activeFilter.providerTypes - pt)) })
   418                                     }
   419                                 }
```

### In TenantHomeScreen.kt - Filter Sheet
**Line 1418:** Triple definition in PropertyFilterSheet (based on grep result)
```
1418                         Triple("brokerage", "🏢", "Brokerage")
```

**Lines 414, 414:** Filter section references to "brokerage"
```kotlin
   414                             "brokerage" -> "🏢 Brokerage"
```

---

## 3. Property Card Rendering

### PropertyCard Composable - Signature
**Lines 30-39 (PropertyCard.kt):**
```kotlin
    30 fun PropertyCard(
    31     property: Property,
    32     onClick: () -> Unit,
    33     modifier: Modifier = Modifier,
    34     showActions: Boolean = false,
    35     isUnlocked: Boolean = true,          // false = tenant view, address hidden until paid
    36     onEdit: (() -> Unit)? = null,
    37     onDelete: (() -> Unit)? = null,
    38     onToggleAvailability: (() -> Unit)? = null
    39 ) {
```

### PropertyCard Usage in TenantHomeScreen.kt
**Lines 487-494 (TenantHomeScreen.kt):**
```kotlin
   487                 else -> items(filtered, key = { it.id }) { property ->
   488                     PropertyCard(
   489                         property = property,
   490                         onClick = { onPropertyClick(property) },
   491                         modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).animateItem(),
   492                         isUnlocked = unlockedPropertyIds.contains(property.id)
   493                     )
   494                 }
```

### PropertyCard Layout Structure (PropertyCard.kt)
**Lines 64-339:**

#### Card Container
- **Lines 64-77:** Card wrapper with shadow, shape, and clickable modifiers
- Elevation and scale animation based on press state

#### Image Section
- **Lines 79-196:** Box containing property image with overlays
  - **Lines 86-91:** AsyncImage with ContentScale.Crop
  - **Lines 92-102:** Vertical gradient overlay (transparent to black)
  - **Lines 103-118:** Price chip (top-right) - "$ {price}/mo"
  - **Lines 119-152:** Classification + Property type badges (top-left)
  - **Lines 154-179:** "Listed by" banner (bottom-right) - based on providerSubtype
  - **Lines 180-195:** City pill (bottom-left)

#### Details Section
- **Lines 198-337:** Column with property details
  - **Lines 200-223:** Title with suburb suffix
  - **Lines 226-264:** Location section (unlocked shows full address, locked shows lock badge)
  - **Lines 266-275:** Stats row (beds, bathrooms, location type)
  - **Lines 276-286:** Bills inclusive row
  - **Lines 288-293:** Badges row (verified, availability, status)
  - **Lines 295-336:** Landlord actions section (if showActions is true)

---

## Summary

| Aspect | Details |
|--------|---------|
| **"Listed by" Display** | Lines 155-179 in PropertyCard.kt; shows emoji + text based on providerSubtype ("agent", "brokerage", default "landlord") |
| **Color Coding** | Agent: teal (0xFF00897B), Brokerage: purple (0xFF7C5CBF), Landlord: Primary color |
| **providerSubtype Check** | Direct string matching: "agent", "brokerage", else defaults to "landlord" |
| **isBrokerage** | Not explicitly used; logic: `property.providerSubtype == "brokerage"` |
| **isAgent** | Not explicitly used; logic: `property.providerSubtype == "agent"` |
| **Card Rendering** | PropertyCard composable (lines 30-340 in PropertyCard.kt); used in TenantHomeScreen at lines 488-493 |
| **Badge Integration** | VerifiedBadge receives providerSubtype at line 290 for styling |

# PNG Image Loading in RentOut Compose Multiplatform (commonMain)

## Overview
RentOut uses **two complementary approaches** for loading images in Compose Multiplatform:
1. **`painterResource()` + `Image()`** — for PNG assets from Android drawables
2. **`AsyncImage()`** — for remote URLs (via Coil3)

---

## 1. Loading PNG Assets with `painterResource()` and `Image()`

### Import Statements (Required)
```kotlin
// For the Image composable
import androidx.compose.foundation.Image

// For loading drawable resources
import androidx.compose.ui.res.painterResource

// For sizing and layout
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
```

### Basic Pattern
```kotlin
Image(
    painter = painterResource(id = org.example.project.R.drawable.tenant),
    contentDescription = "Tenant role icon",
    contentScale = ContentScale.Fit,
    modifier = Modifier.size(80.dp)
)
```

### Complete Real-World Example from RoleSelectionScreen.kt
```kotlin
Image(
    painter            = painterResource(id = tile.drawableRes),
    contentDescription = tile.label,
    modifier           = Modifier
        .padding(top = 4.dp)
        .size(if (isSelected) 90.dp else if (anySelected) 56.dp else 72.dp),
    contentScale       = ContentScale.Fit
)
```

### Key Properties
| Property | Purpose | Example |
|----------|---------|---------|
| `painter` | The actual drawable resource | `painterResource(id = org.example.project.R.drawable.landlord)` |
| `contentDescription` | Accessibility text (required) | `"Landlord icon"` |
| `modifier` | Size, padding, scale, etc. | `Modifier.size(80.dp).padding(4.dp)` |
| `contentScale` | How image fits in bounds | `ContentScale.Fit`, `ContentScale.Crop`, `ContentScale.Fill` |

### Android Resource Reference Syntax
The drawable resources are referenced using the **fully qualified package path**:
```kotlin
org.example.project.R.drawable.{drawable_name}
```

Available drawables in `composeApp/src/androidMain/res/drawable/`:
- `org.example.project.R.drawable.landlord` → `landlord.png`
- `org.example.project.R.drawable.agent` → `agent.png`
- `org.example.project.R.drawable.brokerage` → `brokerage.png`
- `org.example.project.R.drawable.tenant` → `tenant.png`

---

## 2. Loading Remote Images with `AsyncImage()`

### Import Statements (Required)
```kotlin
// Coil3 async image loader
import coil3.compose.AsyncImage

// Optional: for layout control
import androidx.compose.ui.layout.ContentScale
```

### Basic Pattern
```kotlin
AsyncImage(
    model = property.imageUrl,
    contentDescription = property.title,
    contentScale = ContentScale.Crop,
    modifier = Modifier.fillMaxSize()
)
```

### Real-World Example from PropertyCard.kt
```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
) {
    AsyncImage(
        model = property.imageUrl,
        contentDescription = property.title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
    // Overlay gradient...
}
```

### Key Properties
| Property | Purpose | Example |
|----------|---------|---------|
| `model` | Image source (URL or drawable) | `property.imageUrl`, `painterResource(...)` |
| `contentDescription` | Accessibility text | `"Property image"` |
| `contentScale` | How image fits in bounds | `ContentScale.Crop`, `ContentScale.Fit`, `ContentScale.Fill` |
| `modifier` | Size, padding, clipping | `Modifier.fillMaxSize()` |

---

## 3. Compose Resources Setup

### In `composeApp/build.gradle.kts`
```gradle
dependencies {
    commonMain.dependencies {
        // Compose UI & Resources
        implementation(libs.compose.components.resources)
        
        // Image Loading
        implementation(libs.coil.compose)
        implementation(libs.coil.network.ktor)
    }
}
```

### Current Drawable Files Location
```
composeApp/src/androidMain/res/drawable/
├── agent.png
├── brokerage.png
├── landlord.png
├── tenant.png
├── ic_launcher_background.xml
└── ic_launcher_foreground.png
```

### Compose Resources (Vector Only)
```
composeApp/src/commonMain/composeResources/drawable/
└── compose-multiplatform.xml  (Vector graphic, not PNG)
```

---

## 4. How RentOut Uses These Patterns

### Example 1: RoleSelectionScreen.kt
Uses `painterResource()` with `Image()` to display role selection icons:

```kotlin
private data class ProviderTileData(
    val subtype: String,
    val emoji: String,
    val drawableRes: Int,       // ← Android drawable resource ID
    val label: String,
    val sublabel: String,
    val greeting: String,
    val color: Color
)

private fun ProviderRoleCard(...) {
    val tiles = remember {
        listOf(
            ProviderTileData(
                "landlord", "🏠", 
                org.example.project.R.drawable.landlord,  // ← Reference
                "Landlord", ...
            ),
            ProviderTileData(
                "agent", "🤝", 
                org.example.project.R.drawable.agent,     // ← Reference
                "Freelancer Agent", ...
            ),
            ProviderTileData(
                "brokerage", "🏢", 
                org.example.project.R.drawable.brokerage, // ← Reference
                "Brokerage", ...
            )
        )
    }
}

private fun ProviderSubtypeTile(...) {
    Image(
        painter = painterResource(id = tile.drawableRes),  // ← Used here
        contentDescription = tile.label,
        modifier = Modifier
            .padding(top = 4.dp)
            .size(if (isSelected) 90.dp else if (anySelected) 56.dp else 72.dp),
        contentScale = ContentScale.Fit
    )
}
```

### Example 2: RoleCard (Tenant Selection)
Also uses `painterResource()` for conditional PNG display:

```kotlin
@Composable
private fun RoleCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    emoji: String,
    tenantDrawableRes: Int? = null,  // ← Optional PNG drawable ID
    isSelected: Boolean,
    selectedBorderColor: Color,
    onClick: () -> Unit
) {
    // ...
    if (tenantDrawableRes != null) {
        Image(
            painter = painterResource(id = tenantDrawableRes),  // ← Load PNG
            contentDescription = title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer {
                    scaleX = emojiScale
                    scaleY = emojiScale
                }
        )
    } else {
        // Fallback to emoji in a tinted box
        Box(...) { Text(emoji, ...) }
    }
}

// Called with:
RoleCard(
    ...,
    tenantDrawableRes = org.example.project.R.drawable.tenant,  // ← Reference
    ...
)
```

### Example 3: PropertyCard.kt
Uses `AsyncImage()` from Coil3 for remote property images:

```kotlin
@Composable
fun PropertyCard(
    property: Property,  // ← Has imageUrl: String
    onClick: () -> Unit,
    ...
) {
    Card(...) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                AsyncImage(
                    model = property.imageUrl,  // ← Remote URL
                    contentDescription = property.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient overlay...
            }
        }
    }
}
```

---

## 5. ContentScale Options

RentOut uses these `ContentScale` values:

| Option | Behavior | Use Case |
|--------|----------|----------|
| `ContentScale.Fit` | Entire image visible, may have empty space | Icons, logos |
| `ContentScale.Crop` | Fill bounds, may crop edges | Card thumbnails, backgrounds |
| `ContentScale.Fill` | Stretch to fill (may distort) | Rare; usually avoided |

---

## 6. Complete Working Example (For New Screens)

If you need to add a PNG image to a new screen in commonMain:

### Step 1: Add PNG to Android Resources
Place your PNG in:
```
composeApp/src/androidMain/res/drawable/my_icon.png
```

### Step 2: Reference in Your Composable
```kotlin
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size

@Composable
fun MyScreen() {
    Image(
        painter = painterResource(id = org.example.project.R.drawable.my_icon),
        contentDescription = "My icon description",
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(64.dp)
    )
}
```

### Step 3: Build & Test
The drawable will be automatically discovered by Gradle and accessible via `org.example.project.R.drawable.my_icon`.

---

## 7. Key Patterns & Best Practices

### ✅ DO
- Use `painterResource()` for **static PNG assets** stored in Android resources
- Use `AsyncImage()` for **remote URLs** (images from Firebase Storage, Firestore, etc.)
- Always provide a meaningful `contentDescription` for accessibility
- Use `ContentScale.Fit` for icons/logos, `ContentScale.Crop` for thumbnails
- Store PNG files in `composeApp/src/androidMain/res/drawable/`
- Reference drawables with fully qualified path: `org.example.project.R.drawable.{name}`

### ❌ DON'T
- Don't hardcode image URLs without `AsyncImage()`
- Don't forget `contentDescription` (accessibility requirement)
- Don't place PNG files in `commonMain/composeResources/drawable/` if you want to use them with `painterResource()` on Android (use XML vectors there instead)
- Don't try to use `painterResource()` from URLs; use `AsyncImage()` instead

---

## 8. Dependencies Summary

| Library | Version | Purpose | Usage |
|---------|---------|---------|-------|
| `compose.components.resources` | 1.7.3 | Compose resource loading | `painterResource()` |
| `coil-compose` | 3.0.4 | Async image loading | `AsyncImage()` |
| `coil-network-ktor3` | 3.0.4 | Network support for Coil | HTTP/HTTPS image URLs |
| `ktor-client-okhttp` | 3.0.3 | Android HTTP engine | Powers Coil on Android |
| `ktor-client-darwin` | 3.0.3 | iOS HTTP engine | Powers Coil on iOS |

All versions are defined in `gradle/libs.versions.toml`.

---

## 9. Troubleshooting

### "Cannot find symbol: org.example.project.R"
- **Cause**: Drawable not in `composeApp/src/androidMain/res/drawable/`
- **Fix**: Add PNG file and rebuild project (`./gradlew build`)

### Image appears distorted or stretched
- **Cause**: Wrong `ContentScale` value
- **Fix**: Use `ContentScale.Fit` or `ContentScale.Crop` instead of `ContentScale.Fill`

### AsyncImage shows placeholder forever
- **Cause**: Network issue or malformed URL
- **Fix**: Check URL is valid HTTPS, check Firebase Storage rules allow public read

### painterResource() works on Android but image is missing on iOS
- **Cause**: PNG files in `res/drawable/` are Android-only
- **Fix**: This is expected; iOS uses the commonMain code but needs drawable support via expect/actual if needed

---

## Summary

**For PNG assets stored locally:** Use `painterResource(id = org.example.project.R.drawable.{name})` with `Image()`

**For remote images:** Use `AsyncImage(model = url)` from Coil3

Both are available in `commonMain` and work across Android and iOS.

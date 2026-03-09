# RentOut AI Agent Instructions

**Purpose:** This file provides automatic context and instructions for AI agents working on the RentOut project.

---

## 🚨 CRITICAL: Read This First

Before performing ANY development task on this project, you MUST:

1. **Thoroughly explore the workspace** — traverse every folder and read every relevant source file before touching anything.
2. **Reference existing implementations** before creating new code — never duplicate what already exists.
3. **Follow all rules** outlined in this document without exception.
4. **Use established patterns** and utilities already in the project.

---

## 🎯 Project Overview

**Project Name:** RentOut
**Type:** Kotlin Multiplatform (KMP) Application
**Root Package:** `org.example.project`
**Architecture:** Shared logic + platform-specific UI

### Target Platforms
| Platform | Entry Point | UI Framework |
|----------|-------------|--------------|
| Android  | `composeApp/src/androidMain/.../MainActivity.kt` | Jetpack Compose (Multiplatform) |
| iOS      | `composeApp/src/iosMain/.../MainViewController.kt` + `iosApp/` (Swift) | Compose Multiplatform / SwiftUI |
| Web      | `webApp/src/index.tsx` | React + TypeScript (Vite) |

### Key Modules
- **`composeApp/`** — Compose Multiplatform app (Android + iOS UI)
  - `src/commonMain/` — Shared Compose UI (`App.kt` is the root composable)
  - `src/androidMain/` — Android-specific code and resources
  - `src/iosMain/` — iOS-specific Compose entry point
- **`shared/`** — Shared Kotlin Multiplatform library (Android, iOS, JS)
  - `src/commonMain/` — Platform-agnostic business logic (`Greeting.kt`, `Platform.kt`)
  - `src/androidMain/` / `src/iosMain/` / `src/jsMain/` — Platform `actual` implementations
- **`webApp/`** — React/TypeScript web app consuming the `shared` Kotlin/JS library
  - `src/index.tsx` — Web entry point
  - `src/components/Greeting/` — Greeting component
  - `src/components/JSLogo/` — JS Logo component

---

## 🏗️ Project Structure Reference

```
RentOut/
├── composeApp/
│   └── src/
│       ├── commonMain/kotlin/org/example/project/
│       │   └── App.kt                    ← Root Compose UI entry point
│       ├── androidMain/
│       │   ├── kotlin/org/example/project/
│       │   │   └── MainActivity.kt       ← Android Activity
│       │   └── res/                      ← Android resources (drawables, layouts, etc.)
│       └── iosMain/kotlin/org/example/project/
│           └── MainViewController.kt     ← iOS Compose entry point
├── shared/
│   └── src/
│       ├── commonMain/kotlin/org/example/project/
│       │   ├── Platform.kt               ← Platform interface (expect/actual)
│       │   └── Greeting.kt               ← @JsExport shared greeting logic
│       ├── androidMain/kotlin/org/example/project/
│       │   └── Platform.android.kt       ← Android actual implementation
│       ├── iosMain/kotlin/org/example/project/
│       │   └── Platform.ios.kt           ← iOS actual implementation
│       └── jsMain/kotlin/org/example/project/
│           └── Platform.js.kt            ← JS/Web actual implementation
├── webApp/
│   ├── index.html                        ← Web HTML shell with loading spinner
│   ├── vite.config.ts                    ← Vite bundler config
│   ├── tsconfig.json
│   └── src/
│       ├── index.tsx                     ← React entry point
│       └── components/
│           ├── Greeting/
│           │   ├── Greeting.tsx          ← React Greeting component
│           │   └── Greeting.css
│           └── JSLogo/
│               ├── JSLogo.tsx            ← React JS Logo component
│               └── JSLogo.css
├── iosApp/                               ← Swift iOS app entry point
│   ├── iosApp/
│   │   ├── iOSApp.swift
│   │   ├── ContentView.swift
│   │   └── Info.plist
│   └── iosApp.xcodeproj/
├── gradle/
│   └── libs.versions.toml               ← Version catalog (single source of truth)
├── build.gradle.kts                      ← Root Gradle build
├── composeApp/build.gradle.kts           ← composeApp module build
├── shared/build.gradle.kts               ← shared module build
├── settings.gradle.kts                   ← Project settings (rootProject.name = "RentOut")
└── gradle.properties                     ← Gradle/Android/Kotlin flags
```

---

## ⚡ Quick Checklist for Every Task

Before writing any code:
```
□ Explore the full workspace tree first
□ Open and read all relevant existing files
□ Check what already exists — do NOT duplicate
□ Identify which platform(s) the change affects (Android / iOS / Web / All)
□ Determine the correct module (composeApp, shared, webApp)
□ For shared logic — put it in shared/src/commonMain/
□ For platform-specific logic — use expect/actual pattern
□ For web — use React/TypeScript in webApp/src/
□ Verify no duplicate implementations exist before creating new ones
□ Follow the patterns already established in the codebase
```

---

## 🛠️ Tech Stack & Versions

| Technology | Version | Notes |
|------------|---------|-------|
| Kotlin | 2.3.0 | Multiplatform, coroutines, K2 compiler |
| Compose Multiplatform | 1.10.0 | Shared UI for Android + iOS |
| Material3 | 1.10.0-alpha05 | `compose.material3` |
| Android compileSdk | 36 | |
| Android minSdk | 24 | |
| Android targetSdk | 36 | |
| AndroidX Lifecycle | 2.9.6 | ViewModel + Runtime Compose |
| AndroidX Activity | 1.12.2 | `activity-compose` |
| AGP | 9.1.0 | Android Gradle Plugin |
| React | (via webApp/package.json) | TypeScript + Vite |
| JVM target | 11 | Both composeApp and shared |

All versions are managed in **`gradle/libs.versions.toml`** — never hardcode versions in build files.

---

## 📐 Architecture & Code Patterns

### 1. Shared Logic (expect/actual)

Cross-platform interfaces live in `shared/src/commonMain/`:
```kotlin
// Platform.kt — common interface
interface Platform {
    val name: String
}
expect fun getPlatform(): Platform
```

Platform implementations live in their respective source sets:
```kotlin
// Platform.android.kt
class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}
actual fun getPlatform(): Platform = AndroidPlatform()
```

### 2. JS Export for Web

Shared Kotlin classes consumed by the React web app MUST be annotated:
```kotlin
@OptIn(ExperimentalJsExport::class)
@JsExport
class MySharedClass {
    fun myFunction(): String = "result"
}
```

### 3. Compose UI (commonMain)

The root composable is `App()` in `composeApp/src/commonMain/.../App.kt`. All shared Compose UI goes here or in composables called from here.

```kotlin
@Composable
@Preview
fun App() {
    MaterialTheme {
        // Your UI here
    }
}
```

### 4. Android Entry Point

`MainActivity` simply calls `App()` — no business logic here:
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}
```

### 5. iOS Entry Point

`MainViewController.kt` wraps `App()` for the Swift side:
```kotlin
fun MainViewController() = ComposeUIViewController { App() }
```

### 6. Web (React + TypeScript)

Web components import from the `shared` Kotlin/JS module:
```tsx
import { Greeting as KotlinGreeting } from 'shared';

export function MyComponent() {
    const greeting = new KotlinGreeting();
    return <div>{greeting.greet()}</div>;
}
```

---

## 🎨 UI/UX Standards

### Compose (Android + iOS)

- **Always** use `MaterialTheme` tokens — never hardcode colors.
- Cards: `≥ 12.dp` corner radius, subtle `elevation`/`tonalElevation`.
- Spacing: **8dp grid** (8, 16, 24, 32, 48dp increments).
- Typography: follow Material3 type scale (Display → Headline → Title → Body → Label).
- Dark mode: support via `MaterialTheme` — never hardcode light-only colors.
- `LazyColumn`/`LazyRow` with `animateItemPlacement()` for scrollable lists.
- Empty states: illustration + animated hint.
- Input fields: floating label pattern.
- Success/Error: animated icons (checkmark draw-on, shake for error).

### Web (React)

- Follow the patterns in `webApp/src/components/Greeting/` as a reference.
- Use CSS modules or component-scoped `.css` files (one per component).
- Responsive design — mobile-first.

---

## ✨ Animation Standards

### Compose Buttons & Clickable Elements
```kotlin
val interactionSource = remember { MutableInteractionSource() }
val isPressed by interactionSource.collectIsPressedAsState()
val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.93f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
)
Box(
    modifier = Modifier
        .scale(scale)
        .clickable(interactionSource = interactionSource, indication = null) { /* action */ }
)
```

### Compose Progress Bars
```kotlin
val animatedProgress by animateFloatAsState(
    targetValue = progress,
    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
)
LinearProgressIndicator(progress = { animatedProgress })
```

### Compose Loading Spinner
```kotlin
val infiniteTransition = rememberInfiniteTransition()
val rotation by infiniteTransition.animateFloat(
    initialValue = 0f, targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing))
)
```

### Compose FAB Entry
```kotlin
AnimatedVisibility(
    visible = isFabVisible,
    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
    exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut()
) {
    FloatingActionButton(onClick = { /* ... */ }) { Icon(Icons.Default.Add, null) }
}
```

### Compose Back Button (MANDATORY)
```kotlin
IconButton(
    onClick = {
        scope.launch {
            delay(200)
            navController.popBackStack()
        }
    },
    modifier = Modifier.graphicsLayer {
        scaleX = backButtonScale   // animate 1f → 0.8f
        scaleY = backButtonScale
        rotationZ = backButtonRotation  // animate 0f → -45f
    }
) {
    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
}
```

### Web Button Animations (CSS)
```css
.btn {
    transition: transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1),
                box-shadow 0.15s ease;
}
.btn:hover  { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.15); }
.btn:active { transform: scale(0.93);      box-shadow: 0 2px 6px  rgba(0,0,0,0.12); }
```

### Web Loading Spinner (CSS)
```css
@keyframes spin {
    to { transform: rotate(360deg); }
}
.spinner {
    animation: spin 0.9s linear infinite;
    border: 3px solid rgba(0,0,0,0.1);
    border-top-color: #007AFF;
    border-radius: 50%;
    width: 36px; height: 36px;
}
```

---

## 🧑‍💻 Senior Developer Standards

Think and code like a **top-level senior developer**:

- **Separation of concerns:** ViewModel / Repository / UI layers — no business logic in Composables.
- **Single Responsibility Principle:** every function does one thing.
- **UI State:** use `sealed class` with `Loading`, `Success`, `Error` states.
- **Coroutines:** prefer `StateFlow` + `collectAsStateWithLifecycle()`. Never use `GlobalScope` — use `viewModelScope`.
- **Null Safety:** leverage Kotlin's type system fully — no `!!` without a compelling reason.
- **Self-documenting code:** add comments only where the *why* is non-obvious.
- **Edge cases:** always handle empty state, error state, and loading state — all three.
- **Reusability:** keep composables small and extract repeated UI into shared components.
- **DRY:** check for existing implementations before creating anything new.

---

## 💡 Development Principles

### 1. DRY (Don't Repeat Yourself)
- Check for existing implementations first.
- Reuse shared Kotlin logic across all platforms via the `shared` module.
- Extract common Compose UI patterns into reusable composables.

### 2. Consistency
- Follow existing naming conventions (`camelCase` for Kotlin, `PascalCase` for components/classes).
- Use the version catalog (`libs.versions.toml`) for all dependency versions.
- Apply consistent animation patterns everywhere.

### 3. Quality
- Write clean, readable code.
- Handle errors gracefully with user-friendly messages.
- Test your implementations using the existing test structure.

### 4. Modern Patterns
- Use Compose `StateFlow` + ViewModel for state management.
- Kotlin coroutines for async operations.
- React hooks (`useState`, `useEffect`) for web state.
- Material Design 3 guidelines for Compose UI.

---

## 📦 Large File Construction — 3-Stage Build Protocol

When a file is **too large to construct in one pass** (risk of hitting token/context limits):

| Stage | Coverage | Temp File Naming |
|-------|----------|-----------------|
| Stage 1 | First 30% | `tmp_rovodev_FileName_stage1.kt` |
| Stage 2 | Next 30% | `tmp_rovodev_FileName_stage2.kt` |
| Stage 3 | Final 40% | `tmp_rovodev_FileName_stage3.kt` |

After all 3 stages are written, **merge into one final file** and **delete all temp files**.

```
tmp_rovodev_MyScreen_stage1.kt  ← 30%
tmp_rovodev_MyScreen_stage2.kt  ← 30%
tmp_rovodev_MyScreen_stage3.kt  ← 40%
                ↓ merge
MyScreen.kt                     ← 100% final
```

> ⚠️ Never leave `tmp_rovodev_*` files in the project. Always clean up after merging.

---

## 🔨 Build & Run Commands

### Android
```shell
# macOS/Linux
./gradlew :composeApp:assembleDebug

# Windows
.\gradlew.bat :composeApp:assembleDebug
```

### Web
```shell
# Step 1: Build Kotlin/JS shared library
./gradlew :shared:jsBrowserDevelopmentLibraryDistribution   # macOS/Linux
.\gradlew.bat :shared:jsBrowserDevelopmentLibraryDistribution  # Windows

# Step 2: Install Node deps and start dev server
npm install
npm run start
```

### iOS
Open `iosApp/iosApp.xcodeproj` in Xcode and run, or use the IDE run configuration.

### Tests
```shell
./gradlew :composeApp:commonTest
./gradlew :shared:commonTest
```

---

## 🚀 MANDATORY: Firebase Deployment Rules

**These rules apply after EVERY task that touches Firebase-related files. A task is NOT complete until deployed.**

| Changed File(s) | Required Deploy Command |
|---|---|
| Any file in `webApp/admin/` or `webApp/` | `npx firebase deploy --only hosting` |
| `firestore.rules` | `npx firebase deploy --only firestore:rules` |
| `firestore.indexes.json` | `npx firebase deploy --only firestore:indexes` |
| Multiple of the above | `npx firebase deploy --only hosting,firestore:rules,firestore:indexes` |

### Deployment Checklist (run after every Firebase-related task)
```
□ Web app files changed?       → deploy hosting
□ firestore.rules changed?     → deploy firestore:rules
□ firestore.indexes.json changed? → deploy firestore:indexes
□ Confirm "Deploy complete!" in terminal output before marking task done
```

> ⚠️ **NEVER** consider a Firebase-related task complete without running the appropriate deploy command and confirming success.

---

## ⌨️ Keyboard Overlap Prevention (IME Padding)

### Problem
On Android (and iOS via Compose Multiplatform), when the software keyboard (IME — Input Method Editor) appears, it slides up from the bottom of the screen. Without proper handling, it covers/overlaps text fields that are near the bottom of the screen, making it impossible for the user to see what they are typing.

### The Fix — Two Required Modifiers

Every screen that contains **one or more text input fields** MUST apply both of the following:

#### 1. `imePadding()` on the root container
Add `.imePadding()` to the outermost layout container (usually a `Box`). This pushes the entire screen content up by exactly the height of the keyboard when it appears.

#### 2. `verticalScroll()` on the content column
Wrap the inner content `Column` in `Modifier.verticalScroll(rememberScrollState())`. This makes the content scrollable so the user can reach any field even when the keyboard is open.

### Canonical Pattern

```kotlin
// ✅ CORRECT — root Box gets imePadding()
Box(
    modifier = Modifier
        .fillMaxSize()
        .imePadding()                          // ← pushes content above keyboard
        .background(MaterialTheme.colorScheme.background)
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())  // ← content becomes scrollable
            .padding(horizontal = 24.dp)
    ) {
        // Text fields go here
        OutlinedTextField(...)
        OutlinedTextField(...)
    }
}
```

### Required Import
`imePadding()` and all layout modifiers come from:
```kotlin
import androidx.compose.foundation.layout.*
```
This wildcard import already covers `imePadding()`, `fillMaxSize()`, `padding()`, etc. — no extra import needed if the wildcard is present.

### Step-by-Step Implementation Checklist

When building or editing any screen that has text fields:

```
□ 1. Identify the root container of the screen (usually a Box).
□ 2. Add .imePadding() to that root container's Modifier chain.
□ 3. Identify the inner Column that holds the text fields.
□ 4. Add .verticalScroll(rememberScrollState()) to that Column's Modifier chain.
□ 5. Confirm androidx.compose.foundation.layout.* is imported (covers imePadding()).
□ 6. Test: tap each text field — the keyboard must NOT cover the focused field.
```

### Screens Where This Is Applied in RentOut

| Screen | File | Status |
|--------|------|--------|
| Auth (Login / Register) | `ui/screens/auth/AuthScreen.kt` | ✅ Applied |
| Add Property | `ui/screens/landlord/AddPropertyScreen.kt` | ✅ Applied |
| Payment | `ui/screens/tenant/PaymentScreen.kt` | ✅ Applied |

> ⚠️ Any **new screen** added to the project that contains text input fields MUST follow this pattern immediately. Do not add text fields to a screen without also applying `imePadding()` and `verticalScroll()`.

### What NOT to Do

```kotlin
// ❌ WRONG — no imePadding, keyboard will overlap bottom fields
Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(...)
    }
}

// ❌ WRONG — imePadding on the Column instead of the root Box
Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().imePadding()) { // wrong placement
        OutlinedTextField(...)
    }
}

// ❌ WRONG — imePadding present but no verticalScroll, fields still unreachable
Box(modifier = Modifier.fillMaxSize().imePadding()) {
    Column(modifier = Modifier.fillMaxSize()) { // not scrollable
        OutlinedTextField(...)
    }
}
```

### Why `imePadding()` Must Be on the Root `Box`, Not the `Column`
`imePadding()` works by reserving space equal to the keyboard height at the bottom of the composable it is applied to. If it is placed on an inner `Column`, the `Box` behind it still has no awareness of the keyboard, so the layout does not shift correctly. Placing it on the outermost container ensures the entire screen layout reacts to the keyboard appearing and disappearing.

### Android Manifest Requirement
For `imePadding()` to work correctly, the `MainActivity` must use `enableEdgeToEdge()` (already set in this project). The `AndroidManifest.xml` window soft input mode should be left as default (`adjustResize` or unset) — do **not** set `windowSoftInputMode="adjustNothing"` as that will prevent `imePadding()` from functioning.

---

## 🚫 Common Mistakes to Avoid

1. ❌ Hardcoding colors — always use `MaterialTheme.colorScheme` tokens.
2. ❌ Hardcoding dependency versions in build files — use `libs.versions.toml`.
3. ❌ Placing platform-specific code in `commonMain` — use `expect/actual`.
4. ❌ Forgetting `@JsExport` on shared classes used by the web app.
5. ❌ Adding business logic directly into Composables or React components.
6. ❌ Using `GlobalScope` — always use structured concurrency.
7. ❌ Creating duplicate implementations — check existing code first.
8. ❌ Leaving `tmp_rovodev_*` temp files in the project after merging.
9. ❌ Not handling all three states: Loading, Success, Error.
10. ❌ Making UI elements non-interactive (no press/hover feedback).
11. ❌ Ignoring the back button animation pattern in Compose navigation.
12. ❌ Static progress bars — all progress indicators must animate.
13. ❌ Making web app changes without deploying hosting (`npx firebase deploy --only hosting`).
14. ❌ Changing `firestore.rules` without deploying rules.
15. ❌ Changing `firestore.indexes.json` without deploying indexes.
16. ❌ Adding text input fields to a screen without `.imePadding()` on the root container and `.verticalScroll()` on the content column — the keyboard will overlap the fields.

---

## ✅ Success Criteria

Your implementation is successful when:

- ✓ Code compiles on all target platforms (Android, iOS, Web).
- ✓ Shared logic is in `shared/src/commonMain/` with proper `expect/actual` for platform differences.
- ✓ Web-exported classes use `@JsExport`.
- ✓ All animations are visible and smooth.
- ✓ Colors use `MaterialTheme` tokens — no hardcoded values.
- ✓ Back buttons use the mandatory animation pattern.
- ✓ No code duplication (DRY principle followed).
- ✓ Proper error handling with Loading/Success/Error states.
- ✓ UI feels modern, responsive, and interactive.
- ✓ Dependency versions managed via `libs.versions.toml`.
- ✓ No `tmp_rovodev_*` files left in the project.
- ✓ Existing utilities and patterns are reused.
- ✓ Firebase deployed — hosting, rules, and/or indexes as required by the changes made.
- ✓ Every screen with text input fields uses `.imePadding()` on the root `Box` and `.verticalScroll(rememberScrollState())` on the content `Column`.

---

## 🔄 Workflow Summary

```
1. Explore full workspace tree — read every relevant file
   ↓
2. Identify which platform(s) and module(s) are affected
   ↓
3. Check for existing implementations — do NOT duplicate
   ↓
4. Plan your approach (use todo list for complex tasks)
   ↓
5. Write code following established patterns
   ↓
6. Apply required animations and UI feedback
   ↓
7. Test your implementation (build for Android/iOS, verify web)
   ↓
8. Clean up all tmp_rovodev_* temporary files
   ↓
9. Verify all rules are followed
   ↓
10. Deploy to Firebase (MANDATORY if any Firebase files changed)
    • Web app changes    → npx firebase deploy --only hosting
    • firestore.rules    → npx firebase deploy --only firestore:rules
    • firestore.indexes  → npx firebase deploy --only firestore:indexes
    • Confirm "Deploy complete!" before marking task done
```

---

## 📞 Key Reference Files

| File | Purpose |
|------|---------|
| `composeApp/src/commonMain/.../App.kt` | Root Compose UI — start here for shared UI |
| `shared/src/commonMain/.../Platform.kt` | `expect/actual` pattern reference |
| `shared/src/commonMain/.../Greeting.kt` | `@JsExport` pattern reference |
| `webApp/src/components/Greeting/Greeting.tsx` | React component pattern reference |
| `gradle/libs.versions.toml` | All dependency versions (single source of truth) |
| `composeApp/build.gradle.kts` | composeApp dependencies |
| `shared/build.gradle.kts` | Shared module dependencies + JS config |
| `settings.gradle.kts` | Module inclusion + project name |
| `gradle.properties` | JVM, Gradle, and Android flags |

---

## 📝 Notes

- This project uses **Kotlin** exclusively on the native side (Android, iOS, shared logic).
- The web app uses **TypeScript + React** and consumes the Kotlin/JS output from the `shared` module.
- **ViewBinding** is not applicable here — this project uses Compose and React, not XML layouts.
- Gradle **Configuration Cache** and **Build Cache** are enabled — avoid cache-busting patterns.
- The `shared` module generates **TypeScript definitions** automatically (`generateTypeScriptDefinitions()`).
- iOS framework is built as a **static framework** (`isStatic = true`) named `ComposeApp`.

---

**Project:** RentOut | **Type:** Kotlin Multiplatform  
**Last Updated:** 2026-03-04 | **Maintained by:** Rovo Dev

# 🧠 Rovo Dev — Global Rules (Auto-loaded on Every Session)

> These rules are **always active** from the moment Rovo Dev starts — before any code is created or changed.
> They apply across **all platforms** in this project: Android (Compose), iOS (SwiftUI/Compose), and Web (React/TypeScript).

---

## 0. 🔍 Pre-Work Audit (MANDATORY — Do This First, Every Time)

**Before creating or changing ANYTHING**, perform an exhaustive audit of all project files:

1. Traverse every folder and open every relevant source file.
2. Acknowledge what **already exists** — components, animations, styles, resources, utilities.
3. Acknowledge what **does not exist yet** — missing screens, missing animations, missing helpers.
4. Only after this audit is complete, proceed with creation or modification.

> ⚠️ Never assume a file exists or doesn't exist. Always verify by reading the workspace tree first.

---

## 1. 🎨 Icon Colors — Subtle & Distinct

- Every icon in the UI must use a **unique, subtle color** — no two icon categories should share the same tint.
- Use soft, desaturated palette variants (e.g., muted teal, dusty rose, slate blue, warm amber).
- Icons should feel cohesive as a set but clearly distinguishable from one another.
- In Compose: use `tint = Color(...)` or theme-aware `MaterialTheme.colorScheme` tokens per icon type.
- In React/CSS: use distinct `color` or `fill` values per icon class.

---

## 2. 🖱️ Button & Clickable Text Animations — Visible & Satisfying

All buttons and clickable text elements must have **visible, tactile animations**:

### Android / Compose:
```kotlin
// Scale + elevation spring on press
val interactionSource = remember { MutableInteractionSource() }
val isPressed by interactionSource.collectIsPressedAsState()
val scale by animateFloatAsState(if (isPressed) 0.93f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
val elevation by animateDpAsState(if (isPressed) 2.dp else 6.dp)

Box(
    modifier = Modifier
        .scale(scale)
        .shadow(elevation, shape = RoundedCornerShape(12.dp))
        .clickable(interactionSource = interactionSource, indication = null) { /* action */ }
)
```

### Web / React:
```css
.btn {
  transition: transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.15s ease;
}
.btn:hover  { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.15); }
.btn:active { transform: scale(0.93);      box-shadow: 0 2px 6px  rgba(0,0,0,0.12); }
```

---

## 3. ⏳ Progress Bar Animations — Always Animated

Progress bars must **never be static**. Requirements:

- Animated fill using `animateFloatAsState` (Compose) or CSS `transition`/`@keyframes` (Web).
- Indeterminate progress bars use a sweeping shimmer or sliding bar animation.
- Use easing: `FastOutSlowInEasing` (Compose) / `cubic-bezier(0.4, 0, 0.2, 1)` (CSS).
- Duration: 400–800 ms for determinate, continuous loop for indeterminate.

### Compose example:
```kotlin
val animatedProgress by animateFloatAsState(
    targetValue = progress,
    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
)
LinearProgressIndicator(progress = { animatedProgress })
```

### CSS example:
```css
.progress-fill {
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}
```

---

## 4. 🔄 Loading Spinners — Smooth & Branded

All loading spinners must:

- Use smooth, continuous rotation — never jerky or stuttering.
- Be visually branded (use app accent color, not plain gray).
- In Compose: use `CircularProgressIndicator` with custom `color` and `strokeWidth`, or a custom `Canvas`-drawn spinner with `rememberInfiniteTransition`.
- In Web: use CSS `@keyframes` with `animation-timing-function: linear` for perfectly smooth spin.

### Compose infinite spinner:
```kotlin
val infiniteTransition = rememberInfiniteTransition()
val rotation by infiniteTransition.animateFloat(
    initialValue = 0f, targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing))
)
```

### CSS spinner:
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

## 5. 🚀 Floating Action Button (FAB) Animations

All FABs must have:

- **Entry animation**: slide-up + fade-in on screen load (`AnimatedVisibility` with `slideInVertically + fadeIn`).
- **Press animation**: scale down to 0.88f with a spring bounce back.
- **Extended FAB**: animates label width on expand/collapse.
- **Ripple**: always enabled, use accent color ripple.

### Compose FAB entry:
```kotlin
AnimatedVisibility(
    visible = isFabVisible,
    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
    exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut()
) {
    FloatingActionButton(onClick = { /* ... */ }) { Icon(Icons.Default.Add, null) }
}
```

---

## 6. ✨ Interactive & Modern UI/UX Feel

Every screen must feel **premium and alive**. Checklist:

- [ ] Use `MaterialTheme` tokens — never hard-code colors that belong in the theme.
- [ ] Cards have subtle elevation shadows and rounded corners (≥ 12.dp / 12px).
- [ ] Screen transitions use `AnimatedContent` or `NavHost` with enter/exit animations.
- [ ] Empty states have an illustration + animated hint (e.g., gentle pulse).
- [ ] Scroll lists use `LazyColumn`/`LazyRow` with `animateItemPlacement()`.
- [ ] Input fields have animated label (floating label pattern).
- [ ] Success/error states use animated icons (checkmark draw-on, shake for error).
- [ ] Use `Surface` with `tonalElevation` for depth layering.
- [ ] Dark mode support via `MaterialTheme` — never hard-code light-only colors.

---

## 7. ⬅️ Back Button Animation — Standard Configuration

**Every back button** in the project (Android/Compose) must use this exact animation configuration:

```kotlin
// XML View system (legacy screens):
backButton.setOnClickListener { view ->
    view.animate()
        .scaleX(0.8f)
        .scaleY(0.8f)
        .rotation(-45f)
        .setDuration(200)
        .withEndAction {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
}

// Required animation resource files:
// res/anim/slide_in_left.xml   → translateX from -100% to 0%
// res/anim/slide_out_right.xml → translateX from 0% to +100%
```

```kotlin
// Compose Navigation back button equivalent:
val navController = rememberNavController()
IconButton(
    onClick = {
        // Trigger scale + rotation animation then pop back stack
        scope.launch {
            delay(200)
            navController.popBackStack()
        }
    },
    modifier = Modifier
        .graphicsLayer {
            scaleX = backButtonScale
            scaleY = backButtonScale
            rotationZ = backButtonRotation
        }
) {
    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
}
// Animate: scale 1f→0.8f, rotation 0f→-45f over 200ms, then navigate
```

> Required anim files to always create when back navigation exists:
> - `composeApp/src/androidMain/res/anim/slide_in_left.xml`
> - `composeApp/src/androidMain/res/anim/slide_out_right.xml`

---

## 8. 🎯 Realistic & Refined Design

All designs must feel **realistic, tactile, and production-ready**:

- Use real shadows (`elevation`, `drop-shadow`) — not flat placeholder UI.
- Gradients should be subtle (5–15% color shift), not garish.
- Typography must follow a clear hierarchy: Display → Headline → Title → Body → Label.
- Spacing follows an **8dp grid** (8, 16, 24, 32, 48dp increments).
- Images/icons should have proper aspect ratios — no stretching.
- Use `ShapeDefaults` or custom `Shape` tokens — avoid raw rectangles everywhere.
- Refine continuously: after the first pass, always review and polish spacing, alignment, and color contrast.

---

## 9. 🧑‍💻 Senior Developer Standards

Think and code like a **top-level senior developer**:

- Separation of concerns: ViewModel / Repository / UI layers.
- No business logic in Composables or UI components.
- Every function does one thing (Single Responsibility Principle).
- Use `sealed class` for UI state (`Loading`, `Success`, `Error`).
- Write self-documenting code; add comments only where the *why* is non-obvious.
- Handle edge cases: empty states, error states, loading states — always all three.
- Prefer `StateFlow` + `collectAsStateWithLifecycle()` over raw `LiveData`.
- Never use `GlobalScope` — use structured concurrency with `viewModelScope`.
- Keep composables small and reusable — extract repeated UI patterns into shared components.

---

## 10. 📦 Large File Construction — 3-Stage Build Protocol

When a file is **too large to construct in one pass** (or risks hitting token limits), build it in 3 stages:

| Stage | Coverage | File Suffix |
|-------|----------|-------------|
| Stage 1 | First 30% of content  | `_part1` |
| Stage 2 | Next 30% of content   | `_part2` |
| Stage 3 | Final 40% of content  | `_part3` |

After all 3 stages are written, **compile them into one final file** and delete the part files.

```
MyScreen_part1.kt  ← 30%
MyScreen_part2.kt  ← 30%
MyScreen_part3.kt  ← 40%
          ↓ merge
MyScreen.kt        ← 100% final
```

> Never leave part files in the project. Always clean up after merging.

---

## 11. 📐 Project Structure Reference (RentOut — KMP)

This is a **Kotlin Multiplatform** project named **RentOut** targeting:
- **Android** → `composeApp/src/androidMain/` + Compose UI in `commonMain`
- **iOS** → `composeApp/src/iosMain/` + `iosApp/` (Swift entry point)
- **Web** → `webApp/` (React + TypeScript, consumes `shared` Kotlin/JS lib)
- **Shared logic** → `shared/src/commonMain/` (platform-agnostic Kotlin)

Key existing files:
- `composeApp/src/commonMain/.../App.kt` — root Compose UI entry point
- `composeApp/src/androidMain/.../MainActivity.kt` — Android entry
- `composeApp/src/iosMain/.../MainViewController.kt` — iOS Compose entry
- `shared/src/commonMain/.../Greeting.kt` — shared greeting logic
- `shared/src/commonMain/.../Platform.kt` — platform interface
- `webApp/src/components/Greeting/` — React Greeting component
- `webApp/src/components/JSLogo/` — React JS logo component
- `webApp/index.html` — Web entry with inline loading spinner

---

*Last updated: 2026-03-04 | Maintained by Rovo Dev*

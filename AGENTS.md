# RentOut — Rovo Dev Agent Bootstrap

**This file is auto-loaded by Rovo Dev at the start of every session.**

---

## 📖 Primary Reference Document

> **MANDATORY:** Load and read `RentOutAgents.md` before performing any task in this project.

**File:** `RentOutAgents.md` (located in project root)

This document contains everything you need to work on the RentOut project:
- ✅ Full project overview and architecture
- ✅ Complete project structure reference
- ✅ Tech stack and version details
- ✅ Code patterns (expect/actual, @JsExport, Compose, React)
- ✅ UI/UX and animation standards
- ✅ Senior developer coding standards
- ✅ Build & run commands for all platforms
- ✅ Common mistakes to avoid
- ✅ Success criteria checklist
- ✅ Workflow summary

---

## ⚡ Initialization Checklist

On every new session, before any work begins:

```
□ Read RentOutAgents.md in full
□ Explore the workspace tree
□ Identify which platform(s) are affected by the task
□ Check existing implementations before creating new code
```

---

## 🚀 MANDATORY: Firebase Deployment Rules

**These rules apply after EVERY task that touches Firebase-related files.**

### 1. Web App Changes → Deploy Hosting
Any change to files in `webApp/admin/` or `webApp/` MUST be followed by:
```
npx firebase deploy --only hosting
```

### 2. Firestore Rules Changes → Deploy Rules
Any change to `firestore.rules` MUST be followed by:
```
npx firebase deploy --only firestore:rules
```

### 3. Firestore Index Changes → Deploy Indexes
Any change to `firestore.indexes.json` MUST be followed by:
```
npx firebase deploy --only firestore:indexes
```

### 4. Combined Deployment (when multiple files change)
If web + rules + indexes all change in one task:
```
npx firebase deploy --only hosting,firestore:rules,firestore:indexes
```

> ⚠️ **NEVER** consider a Firebase-related task complete without deploying.
> A task is only done when the changes are live on Firebase.

---

**Project:** RentOut | **Type:** Kotlin Multiplatform (Android · iOS · Web)

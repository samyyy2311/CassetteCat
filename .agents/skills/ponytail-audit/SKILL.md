---
name: ponytail-audit
description: "Perform a codebase-wide audit to identify over-engineering, hand-rolled standard library / platform reimplementations, unnecessary wrappers, dead code, and bloat."
---

# Ponytail Codebase Audit

Perform an audit of the repository to identify and flag over-engineering, redundant code, and simplification opportunities.

---

## 🔎 Audit Categories

1. **`yagni:`** Speculative code, unused parameters, dead feature flags, and premature generalizations.
2. **`stdlib:`** Hand-rolled algorithms, formatting, or collections helpers that the Kotlin standard library or Android platform already provides.
3. **`native:`** Custom UI components or platform wrappers where native Jetpack Compose / Android platform primitives already exist.
4. **`shrink:`** Over-abstracted patterns (excessive factories, single-implementation interfaces, redundant state wrappers).
5. **`dep:`** Unnecessary dependencies or custom helper dependencies.

---

## 📋 Audit Report Structure

Group recommendations by file/component, ordered by potential line count reduction and simplicity gains:
- **Location:** `[File path](file:///...)` (with line ranges)
- **Tag:** `[yagni | stdlib | native | shrink | dep]`
- **Diagnosis:** What over-engineering or redundancy is present.
- **Ponytail Fix:** Simplest replacement or deletion.
- **Estimated Savings:** Lines of code reduced / complexity eliminated.

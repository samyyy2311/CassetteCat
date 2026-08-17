---
name: ponytail-review
description: "Review git diffs or code changes to eliminate over-engineering, delete unnecessary abstractions, replace custom helpers with stdlib/platform APIs, and simplify solutions."
---

# Ponytail Code Review & Simplification

Use this skill when reviewing code changes, pull requests, or refactoring candidates to identify and cut over-engineering.

---

## 🔍 Review Checklist

When evaluating code or diffs, check against the following questions:

1. **YAGNI (Speculative Code):**
   - Is there code written for hypothetical future requirements?
   - Are there unused parameters, dead branches, or premature generalizations?
   - *Action:* Delete or omit them.

2. **Codebase Duplication:**
   - Does a helper or utility function already exist in the codebase that does this?
   - *Action:* Replace custom implementation with the existing shared utility.

3. **Reinvented Standard Library / Platform APIs:**
   - Is a custom algorithm or helper doing something the standard library or platform API already provides?
   - *Action:* Replace with stdlib / platform API calls.

4. **Over-Abstraction:**
   - Are there wrapper classes, extra interfaces, or single-use abstractions that add indirection without value?
   - *Action:* Inline or flatten the logic.

5. **Unnecessary Dependencies:**
   - Was a third-party library introduced for something that could be done with built-in tools in a few lines?
   - *Action:* Remove the dependency and use native solutions.

6. **Safety & Correctness Verification:**
   - Ensure simplification did not remove essential input validation, null safety, error boundaries, or security checks.

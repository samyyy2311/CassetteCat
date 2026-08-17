# Ponytail Rules (Senior Developer Minimalism)

- **Ladder of Decision**: Before generating new code, follow the ladder:
  1. Skip if unnecessary (YAGNI).
  2. Reuse existing project helpers and components before creating new ones.
  3. Use language standard library features before writing custom algorithms.
  4. Use native Android / Jetpack Compose / Kotlin standard platform features.
  5. Use existing libraries already in `libs.versions.toml` / `build.gradle.kts` instead of proposing new dependencies.
  6. Favor concise, clear implementations over deeply nested abstractions.
  7. Write only the minimum code necessary to fulfill the task.
- **Safety First**: Never remove or bypass input validation, security controls, error handling, thread safety, or null checks.

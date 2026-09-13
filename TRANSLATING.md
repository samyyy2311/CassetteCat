# Translating CassetteCat

English is CassetteCat's source language. Translations use normal Android string resources.

## Where translations live

- English source strings: `app/app/src/main/res/values/strings.xml`
- Translations: `app/app/src/main/res/values-<locale>/strings.xml`

Examples:

- Hindi: `values-hi/strings.xml`
- German: `values-de/strings.xml`
- Brazilian Portuguese: `values-pt-rBR/strings.xml`

Do not copy entries marked `translatable="false"` into translation files.

## Adding or updating a translation

1. Keep every resource name unchanged.
2. Preserve placeholders exactly, including `%s`, `%d`, `%1$s`, and `%2$s`.
3. Preserve plural resources and their quantity keys.
4. Leave product, service, protocol, and brand names unchanged when the source resource is marked non-translatable.
5. Translate naturally. Do not force word-for-word wording if it sounds awkward.
6. Keep labels short enough for small screens, widgets, notifications, and Android Auto.

If a translated entry is missing, Android falls back to English.

## Adding new UI text

User-facing text belongs in Android resources, not directly inside Kotlin or XML layouts.

In Compose, use `stringResource(...)` for strings and `pluralStringResource(...)` for plurals. Outside Compose, use the normal Android resource APIs through an available `Context`.

Do not move internal identifiers, URLs, file names, protocol values, log messages, or data-model constants into translation resources unless users actually see them.

## Per-app language support

The default resource locale is declared in `app/app/src/main/res/resources.properties`.

CassetteCat will only enable Android's per-app language picker once real translated resource directories are shipped. When that is enabled, the build must explicitly limit supported locales so languages coming from dependencies do not appear as if CassetteCat supports them.

## Testing

Before opening a translation pull request:

```bash
cd app
./gradlew :app:compileDebugKotlin
./gradlew :app:lintDebug
```

When possible, also check the translated screens on a device or emulator for clipped text, plurals, and right-to-left layout issues.

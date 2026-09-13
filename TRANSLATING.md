# Translating CassetteCat

CassetteCat uses Android string resources for localization. English is the source language.

## Where translations live

- Source strings: `app/app/src/main/res/values/strings.xml`
- Translations: `app/app/src/main/res/values-<locale>/strings.xml`

Examples:

- Hindi: `values-hi/strings.xml`
- German: `values-de/strings.xml`
- Brazilian Portuguese: `values-pt-rBR/strings.xml`

Do not duplicate strings that are intentionally marked `translatable="false"`.

## Adding or updating a translation

1. Copy only the translatable entries you need from the English `strings.xml`.
2. Keep every resource name unchanged.
3. Preserve formatting placeholders exactly, including `%s`, `%d`, `%1$s`, and `%2$s`.
4. Preserve plural resources and their quantity keys.
5. Keep product, service, protocol, and brand names unchanged when the English resource is intentionally non-translatable.
6. Prefer natural wording over word-for-word translation.
7. Keep labels concise enough for small Android screens, widgets, notifications, and Android Auto.

Missing translated entries automatically fall back to English, so a translation file does not need to copy unchanged resources.

## Adding new UI text

User-facing text must be defined in Android resources instead of being hardcoded in Kotlin or XML.

In Compose, use `stringResource(...)` for strings and `pluralStringResource(...)` for plurals. For non-UI code, use the Android resource APIs through an available `Context`.

Do not move technical identifiers, URLs, file names, protocol values, log messages, or data-model constants into translation resources unless they are actually shown to users.

## Per-app language support

CassetteCat uses Android Gradle Plugin's generated locale configuration. The default locale is declared in `app/app/src/main/res/resources.properties`, and supported locales are discovered from `values-*` resource directories at build time.

On Android 13 and newer, supported languages can therefore appear in the system's per-app language settings without maintaining a separate locale list.

## Testing

Before opening a translation pull request:

```bash
cd app
./gradlew :app:compileDebugKotlin
./gradlew :app:lintDebug
```

Also check the translated screens on a device or emulator when possible, especially for clipped text, pluralization, and right-to-left layout behavior.

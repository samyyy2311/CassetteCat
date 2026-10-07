# CI and releasing

## Workflows

All workflows are in `.github/workflows/`. Every action is pinned to a commit, with its version in a comment so Dependabot can update it.

| Workflow | Runs | What it does |
|---|---|---|
| `android-ci.yml` | Pull requests, and pushes to `main` that change `app/` | Unit tests, a debug build and Android lint. |
| `codeql.yml` | Changes to `app/`, and weekly | CodeQL security scanning of the Kotlin code. |
| `dependency-review.yml` | Pull requests | Fails if a pull request adds a dependency with a known vulnerability. |
| `workflow-lint.yml` | When workflow files change | Checks workflows with actionlint. |
| `scorecard.yml` | Weekly | OpenSSF Scorecard check of the repository's security practices. |
| `nightly.yml` | Daily | Builds an APK from `main` and publishes it as the `nightly` pre-release. Skipped when `main` hasn't changed. |
| `release.yml` | By hand | Builds, signs and publishes a release. See below. |

## Making a release

1. **Update `CHANGELOG.md`.** Add a section headed `## [1.8.0]` with the changes, written for people using the app. The release workflow copies this section into the GitHub release notes and fails if it's missing.
2. **Update `versionName`'s default** in `app/app/build.gradle.kts` and the version badge in `README.md`, so local builds and the README match. Merge this to `main`.
3. **Run the workflow**: in GitHub, open **Actions > Release Build > Run workflow** and enter:
   - **version_name**, for example `1.8.0`. The release is tagged `v1.8.0`.
   - **version_code**, a whole number greater than the last one uploaded to Google Play. Play rejects anything equal or lower.
4. **Check the release**: the workflow creates the GitHub release `v1.8.0` with `CassetteCat-v1.8.0.apk` and its `.sha256` file. Obtainium users get it from there.

The workflow refuses to publish without signing secrets or with an invalid version code, so an unsigned or mis-numbered APK can't go out.

### Google Play

The workflow also builds an Android App Bundle (`bundleRelease`) but doesn't upload it anywhere. For a Play Store release, build the bundle locally with the same version code and signing key (see [Building and testing](building.md#release-signing)) and upload it in the Play Console.

### Signing secrets

The release workflow needs these repository secrets:

| Secret | Contents |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | The release keystore, base64-encoded: `base64 -w0 release.keystore` |
| `KEYSTORE_PASSWORD` | The keystore password |
| `KEY_ALIAS` | The key's alias |
| `KEY_PASSWORD` | The key's password |

The workflow writes the keystore to a temporary file, signs, verifies the APK with `apksigner`, and deletes the keystore whether or not the build succeeded. Losing the keystore means existing installs can't be updated, so keep a backup of it outside GitHub.


# SpendWise

SpendWise is a Kotlin Multiplatform expense app for Android and iOS, built with shared Compose UI in `shared`.

The app is designed around fast entry and fast retrieval: expenses can be tagged directly in notes, then found later through tag filters, text search, category filters, calendar views, and reports.

## Features

- Add, edit, and delete expenses quickly.
- Tag expenses while writing notes, with recent tag suggestions for faster entry.
- Search transactions by note text and filter by tags, categories, currencies, and dates.
- Combine filters across reports and transaction lists to narrow spending history quickly.
- Calendar view with monthly and annual summaries plus transaction details.
- Monthly, annual, category, and tag usage reports.
- Category management with custom names, colors, icons, and ordering.
- Base currency settings with exchange-rate conversion.
- Expense reminders.
- Local persistence with shared Room database code.

## Project Structure

- `androidApp`: Android app shell.
- `shared`: Shared Kotlin Multiplatform code, Compose UI, domain logic, and persistence.
- `iosApp`: iOS app shell.

## Development

### Prerequisites

- JDK 17+
- Android SDK (compileSdk 37, minSdk 28, targetSdk 37)
- Xcode 15+ (for iOS)

### Useful Verification Commands

```shell
# Android debug APK
./gradlew :androidApp:assembleDebug

# Shared tests
./gradlew :shared:allTests

# iOS simulator framework
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

## GitHub Release APK

Releases are tag-driven. Pushing any Git tag starts the GitHub Actions workflow, builds a signed Android APK, and attaches it to a GitHub Release.

The workflow builds APK only; it does not build an Android App Bundle (`.aab`).

```shell
git tag v1.0.1
git push origin v1.0.1
```

### Required GitHub Secrets

Before using the release workflow, add these repository secrets in GitHub under Settings > Secrets and variables > Actions:

- `ANDROID_KEYSTORE_BASE64`: base64-encoded release keystore.
- `ANDROID_KEYSTORE_PASSWORD`: keystore password.
- `ANDROID_KEY_ALIAS`: release key alias.
- `ANDROID_KEY_PASSWORD`: release key password.
- `GOOGLE_SERVICES_JSON`: optional Firebase `google-services.json` content. If omitted, CI uses a dummy config so the direct-download APK can still be built.

Update the `GOOGLE_SERVICES_JSON` secret from the local Android config with:

```shell
gh secret set GOOGLE_SERVICES_JSON < androidApp/google-services.json
```

### Release Keystore

To create a new release keystore:

```shell
keytool -genkeypair -v \
  -keystore spendwise-release.jks \
  -alias spendwise \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

On macOS, copy its base64 value for the `ANDROID_KEYSTORE_BASE64` GitHub secret:

```shell
base64 -i spendwise-release.jks | pbcopy
```

## Local Release Build

The generated release keystore for this workspace is stored at:

```text
.secrets/spendwise-release.jks
```

Signing values are stored in:

```text
.secrets/release-signing.env
```

The `.secrets` directory is ignored by Git and must not be committed.

To build a signed release APK locally with that keystore:

```shell
set -a
source .secrets/release-signing.env
set +a

ANDROID_KEYSTORE_PATH="$PWD/.secrets/spendwise-release.jks" \
VERSION_NAME=1.0.1 \
VERSION_CODE=101 \
./gradlew :androidApp:assembleRelease
```

The APK is written to:

```text
androidApp/build/outputs/apk/release/
```

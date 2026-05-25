# SpendWise

Kotlin Multiplatform expense app with shared Compose UI for Android and iOS.

## Features

- Add, edit, and delete expenses.
- Track expenses by category, note, date, currency, and tags.
- Calendar view with daily totals and transaction history.
- Search and filter transactions by text, category, currency, and tags.
- Monthly, annual, and category spending reports.
- Category management with custom names, colors, icons, and ordering.
- Base currency settings with exchange-rate conversion.
- Expense reminders.
- Local persistence with shared Room database code.

## Build

### Prerequisites

- JDK 17+
- Android SDK (compileSdk 37, minSdk 28, targetSdk 37)
- Xcode 15+ (for iOS)

### Commands

```shell
# Android debug APK
./gradlew :androidApp:assembleDebug

# Shared tests
./gradlew :shared:allTests

# iOS simulator framework
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

## GitHub Release APK

Pushing any Git tag builds a signed Android APK and attaches it to a GitHub Release. The workflow builds APK only; it does not build an Android App Bundle (`.aab`).

```shell
git tag v1.0.1
git push origin v1.0.1
```

Before using the workflow, add these repository secrets in GitHub:

- `ANDROID_KEYSTORE_BASE64`: base64-encoded release keystore.
- `ANDROID_KEYSTORE_PASSWORD`: keystore password.
- `ANDROID_KEY_ALIAS`: release key alias.
- `ANDROID_KEY_PASSWORD`: release key password.

To create a new release keystore locally:

```shell
keytool -genkeypair -v \
  -keystore spendwise-release.jks \
  -alias spendwise \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

On macOS, copy the base64 keystore value with:

```shell
base64 -i spendwise-release.jks | pbcopy
```

### Modules

- `androidApp`: Android app shell.
- `shared`: Shared Kotlin Multiplatform code and Compose UI.
- `iosApp`: iOS app shell.

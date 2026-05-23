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

### Modules

- `androidApp`: Android app shell.
- `shared`: Shared Kotlin Multiplatform code and Compose UI.
- `iosApp`: iOS app shell.

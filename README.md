# RideLedger

RideLedger is an Android app for ride-sharing drivers to record trips and track daily income, expenses, finance settlements, and net earnings. It uses Jetpack Compose, Room local storage, and Android navigation, with optional fingerprint sign-in and reminder notifications.

> Repository: `fibron85/Rideledger-`  
> Package: `com.aistudio.rideledger.k9m4xp`  
> Current version: `1.0.1` (`versionCode 2`)

## Features

- **Driver authentication** — Register, sign in, reset a password, complete a profile, and optionally use fingerprint/biometric authentication.
- **Ride logging** — Record date, time, platform, pickup point, payment method, distance, fare, tips, toll/salik, commission, booking fees, and notes.
- **Automatic earnings calculation** — Calculates fuel cost, fare totals, deductions, and net income.
- **Dashboard** — Displays daily and monthly/21st–20th billing-cycle summaries, platform breakdowns, trends, fuel rate, and recent rides.
- **Daily summaries** — Reviews income and ride totals grouped by day for the selected billing cycle.
- **Reports and filters** — Search rides and filter by date, platform, and payment method; edit or delete entries.
- **Finance settlement** — Tracks cash on hand and amounts due by Card, InAppPaid, or Account, then submits individual shifts or all pending shifts.
- **Excel export/import** — Exports report data and imports ride records through Android storage access.
- **Local backup** — Syncs driver data to an app-managed local `data` folder or a user-selected folder.
- **Settings** — Configure profile details, password, currency, universal fuel rate, shift timing, platforms, pickup points, reminders, and biometric login.
- **Reminders** — Schedules ride and finance reminders at configurable app-defined times or in-app prompt slots.
- **Responsive UI** — Uses a navigation bar on compact screens and a navigation rail on larger screens.
- **Dark-by-default Material 3 theme** — Built with Jetpack Compose and custom RideLedger branding.

## Screenshots

Screenshots are not currently included in this repository. The app package `app-debug.apk` is available at the repository root for installation and testing.

## Technology

- Kotlin
- Android SDK 36
- Jetpack Compose / Material 3
- AndroidX Navigation Compose
- Room / SQLite
- AndroidX DataStore
- ViewModel and Kotlin Coroutines
- Android Biometrics
- Firebase Google Services integration (optional configuration)
- Gradle Kotlin DSL
- GitHub Actions for APK builds

## Requirements

### Build from source

- Android Studio with Android SDK support
- JDK 21 (used by the GitHub Actions workflow)
- Android SDK 36
- Android device or emulator running API 24 or newer
- An internet connection for Gradle dependency resolution

> The project compiles Java/Kotlin bytecode with Java 11 compatibility.

### Install the prebuilt APK

- An Android device running Android 7.0 (API 24) or newer
- APK installation from unknown sources enabled for the installer application
- A supported browser or file manager to download/open `app-debug.apk`

## Installation

### Option 1: Download the repository APK

1. Download [`app-debug.apk`](app-debug.apk) from the repository.
2. Copy it to your Android device if necessary.
3. Open the APK using a file manager or the device package installer.
4. Approve the requested installation and open **RideLedger**.

The APK is a debug build and may be replaced by newer builds generated in GitHub Actions.

### Option 2: Build in Android Studio

```bash
git clone https://github.com/fibron85/Rideledger-.git
cd Rideledger-
```

1. Open the repository directory in Android Studio.
2. Allow Gradle synchronization to finish.
3. Select the `app` run configuration.
4. Choose an emulator or connected device.
5. Click **Run**.

### Option 3: Build with Gradle

On Linux or macOS:

```bash
./gradlew :app:assembleDebug
```

On Windows:

```powershell
gradlew.bat :app:assembleDebug
```

The debug APK will be generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Configuration

### Gemini API secret

The project includes a server/Gemini capability in `metadata.json`, but the checked-in Android dependencies do not currently enable the commented Gemini or Firebase AI libraries.

If future code uses `GEMINI_API_KEY`, copy the example environment file:

```bash
cp .env.example .env
```

Then uncomment and set the value:

```dotenv
GEMINI_API_KEY=MY_GEMINI_API_KEY
```

Do not commit a real API key or package secrets in APK builds. For AI Studio builds, configure secrets through the AI Studio Secrets panel. The Secrets Gradle Plugin reads `.env` and falls back to `.env.example`.

### Firebase configuration

The app module applies `google-services`, and Firebase platform dependencies are present. If adding or enabling Firebase services:

1. Create or select a Firebase project.
2. Register Android application ID `com.aistudio.rideledger.k9m4xp`.
3. Download `google-services.json`.
4. Place it in `app/google-services.json`.
5. Enable the required Firebase/Auth services in the Firebase console.

Without `google-services.json`, the build is configured to warn rather than fail.

### Release signing

The release signing configuration reads environment variables:

```bash
export KEYSTORE_PATH=/absolute/path/to/my-upload-key.jks
export STORE_PASSWORD='your-store-password'
export KEY_PASSWORD='your-key-password'
```

For local builds, the default keystore path is `my-upload-key.jks` at the project root. Store credentials outside source control and never commit a production keystore or password.

## Usage

### First launch

1. Open RideLedger.
2. Create a driver account with a driver ID and 6-digit password, or sign in if one already exists.
3. Complete the initial profile with the driver's name and mobile number.
4. Choose whether to use the app-owned local storage folder or grant access to a selected Android folder.
5. Configure fuel rate, currency, and shift timing in Settings if needed.

### Add a ride

1. Open **New Ride**.
2. Select the ride date and time.
3. Choose the platform and pickup point.
4. Select the payment method.
5. Enter distance, gross fare, tips, toll/salik, commission, and booking fee.
6. Add optional notes.
7. Save the ride.

The app calculates fuel cost from the configured fuel cost per kilometer and derives net income by subtracting ride costs and fees from the fare.

### Review earnings

- Use **Dashboard** for daily and monthly/21st–20th cycle totals.
- Open **Shift** to filter and search rides, export data, edit entries, or submit shifts to finance.
- Open **Summary** for day-wise results and platform breakdown information.
- Open **Finance** to review dues and submitted settlement history.

### Export data

1. Open **Shift** or **Settings**.
2. Grant folder access if prompted.
3. Start the Excel export/import action.
4. Review the generated file in the selected storage location.

### Import data

1. Open **Shift** or **Settings**.
2. Choose **Import Excel**.
3. Select a compatible spreadsheet file.
4. Allow the import process to validate and add ride records.

Keep a local backup before replacing or importing a large dataset.

## Billing cycle

RideLedger uses a 21st–20th billing cycle by default. Reports group rides by this cycle, and cycle navigation shifts the displayed period while preserving records in local storage.

## Local data and privacy

RideLedger stores operational data locally in Room/SQLite and preferences in DataStore. Driver records, rides, settings, and finance submission state remain on the device unless exported or backed up by the user.

The app can:

- Create an app-managed `data` folder
- Synchronize driver, settings, and ride backup files
- Request access to a user-selected folder
- Export/import Excel-compatible reports

The app may request notification permission for ride/finance reminders and biometric hardware access for optional fingerprint sign-in. Neither feature is required to register or use basic local ride logging.

## Testing

Run JVM unit tests:

```bash
./gradlew test
```

Run instrumentation tests on a connected device or emulator:

```bash
./gradlew connectedAndroidTest
```

The repository includes example unit, Robolectric, and instrumented test classes under:

```text
app/src/test/
app/src/androidTest/
```

## GitHub Actions

The workflow [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml):

- Runs on pushes and manual dispatch
- Uses JDK 21
- Installs the Android SDK
- Builds with Gradle 9.3.1
- Produces `app/build/outputs/apk/debug/app-debug.apk`
- Uploads the APK as the `RideLedger-debug-apk` artifact

To trigger a manual build, open **Actions → Build Android Debug APK → Run workflow**.

## Project structure

```text
app/src/main/java/com/example/
├── data/
│   ├── local/                 # Room database, DAOs, entities, session manager
│   └── repository/            # RideLedger repository
├── domain/                    # Finance and ride calculation logic
├── ui/
│   ├── components/            # Shared UI components and dialogs
│   ├── screens/               # Dashboard, ride, reports, finance, and settings screens
│   ├── theme/                 # Compose theme, colors, and typography
│   └── viewmodel/             # Authentication and main app state
├── util/                      # Biometrics, export/import, storage, and reminders
├── MainActivity.kt            # Navigation and app shell
└── RideLedgerApp.kt           # Application and repository setup
```

## Troubleshooting

### Gradle cannot resolve Android SDK 36

Install Android SDK Platform 36 and the matching build tools through Android Studio's SDK Manager. SDK 36.1 is declared by the module configuration.

### The debug APK cannot be installed

- Uninstall an incompatible previous package if package signatures differ.
- Enable installation from unknown sources for the app used to open the APK.
- Confirm the device is API 24 or newer.

### Folder access is unavailable

RideLedger can still use its app-managed local `data` folder. Use **Allow app storage** or **Use app data folder** in the storage setup dialog if document-tree access is unavailable.

### Notifications are not appearing

Android 13 and newer require notification permission. In the app, allow notifications when prompted or open:

```text
Android Settings → Apps → RideLedger → Notifications
```

Then enable **Reminders** in RideLedger Settings.

### Build signing errors

For debug builds, the project expects `debug.keystore` at the repository root and may use the generated/debug configuration. For release builds, set `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` before invoking Gradle.

## Contributing

Contributions are welcome.

1. Fork the repository.
2. Create a feature branch.
3. Keep changes focused and Kotlin/Compose style consistent.
4. Add or update tests when changing calculations or persistence behavior.
5. Build and test the app before opening a pull request.

## License

No license is currently defined in this repository. Add a license file before distributing derivative builds or accepting contributions under a specific open-source license.

## Support

For bugs or feature requests, open an issue in the repository and include:

- Android device model and OS version
- RideLedger app version
- Steps to reproduce
- Relevant logs or screenshots
- Expected and actual behavior

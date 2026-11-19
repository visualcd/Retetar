# Rețetar (Kotlin Multiplatform)

Aplicație locală (100% offline) de rețete, planificator săptămânal de mese și listă automată de cumpărături, construită cu **Kotlin Multiplatform** și **Compose Multiplatform** (Material 3) pentru **Android**, **Desktop (JVM)**, **Web (`wasmJs` și `js`)** și **iOS**.

## Funcționalități MVP

- **Rețete:** CRUD complet într-o tranzacție, validare strictă inline, favorite, etichete, căutare cu normalizare de diacritice după titlu sau ingredient, filtrare după favorit/timp maxim/etichetă, scalare live a porțiilor (1–100) cu formatare adaptată locale-ului (virgulă zecimală în RO, fracții `¼ ½ ¾ ⅓ ⅔`).
- **Plan săptămânal:** navigare pe săptămâni (luni–duminică, ISO 8601), layout adaptiv (listă pe zile pe ecran îngust, grilă cu 7 coloane pe ecran lat), 4 sloturi pe zi (`Mic dejun`, `Prânz`, `Cină`, `Gustare`), ajustare porții per intrare, „Copiază ziua”, „Repetă săptămâna anterioară”.
- **Listă de cumpărături:** generare și agregare automată din planul săptămânii (conversie unități compatibile masă/volum, promovare `g → kg` / `ml → l`, `după gust` deduplicat), grupare pe raion, bifare cu păstrarea stării la regenerare, articole manuale păstrate la regenerare, „Șterge bifatele”, copiere ca text.
- **Setări & portabilitate:** temă (Sistem / Luminoasă / Întunecată), limbă (Română / English cu comutare live), sistem de unități (Metric / Imperial), încărcare/ștergere 10 rețete demo în română, export/import JSON versionat (`schemaVersion = 1`) cu politică pentru duplicate (`SKIP` / `KEEP_BOTH`).

## Cerințe

- **JDK 21** (necesar pentru AGP 9.5.1 și Kotlin 2.4.21; ținta bytecode JVM este 11).
- **Android Studio** cu suport Kotlin Multiplatform și Compose Multiplatform.
- **Android SDK:** `compileSdk = 37`, `targetSdk = 37`, `minSdk = 24`.
- **Google Chrome / Chromium:** necesar pentru rularea locală a testelor Web (`jsBrowserTest`, `wasmJsBrowserTest`).
- **Desktop packaging:** JDK cu `jpackage` (pe Windows, WiX Toolset este necesar pentru `.msi`; pe macOS pentru `.dmg`; pe Linux pentru `.deb`).
- **iOS:** macOS + Xcode (nu este necesar pentru Android, Desktop sau Web; rulat automat în CI pe `macos-latest`).

## Structura proiectului

```text
AmInvatatro/
├── core/                         # Logică de domeniu și persistență (KMP, fără UI)
│   └── src/
│       ├── commonMain/           # core/ (unități, text, timp), domain/ (modele, validare, repository, use case-uri), data/ (SQLDelight, JSON fallback, demo, import/export), di/
│       ├── androidMain/          # DriverFactory (AndroidSqliteDriver), Koin Android
│       ├── jvmMain/              # DriverFactory (JdbcSqliteDriver ~/.retetar/retetar.db)
│       ├── iosMain/              # DriverFactory (NativeSqliteDriver)
│       ├── jsMain/ & wasmJsMain/ # Fallback JSON în localStorage (SettingsJsonKeyValueStore)
│       └── *Test/                # Teste unitare, integrare SQLDelight in-memory, browser localStorage
├── app/
│   ├── shared/                   # UI partajat Compose Multiplatform + ViewModel-uri + resurse RO/EN
│   │   └── src/
│   │       ├── commonMain/       # presentation/ (ecrane + ViewModel), ui/ (temă, navigare adaptivă, componente, locale), composeResources/ (values + values-ro)
│   │       ├── commonTest/       # Teste ViewModel (Turbine) + teste UI Compose (RetetarFlowsUiTest)
│   │       ├── jvmTest/          # Paritate i18n (LocalizationParityTest) + smoke checklist E2E pe SQLite real
│   │       └── androidDeviceTest/# Manifest pentru rularea RetetarFlowsUiTest pe emulator/dispozitiv Android
│   ├── androidApp/               # Punct de intrare Android (MainActivity, manifest, iconițe)
│   ├── desktopApp/               # Punct de intrare Desktop JVM (Window + nativeDistributions)
│   ├── webApp/                   # Punct de intrare Web (ComposeViewport, index.html, wasmJs + js)
│   └── iosApp/                   # Proiect Xcode pentru iOS (MainViewController)
├── server/                       # Generat de wizard — neutilizat și nemodificat
├── .github/workflows/ci.yml      # Pipeline GitHub Actions (Linux + macOS)
└── gradle/libs.versions.toml     # Version catalog
```

## Cum rulezi aplicația

- **Android (emulator sau dispozitiv):**
  ```bash
  ./gradlew :app:androidApp:installDebug
  ```
  *(sau din Android Studio: configurația `app.androidApp`)*
- **Desktop (Windows / Linux / macOS — JVM):**
  ```bash
  ./gradlew :app:desktopApp:run
  ```
- **Web (WasmJS — recomandat pentru browsere moderne):**
  ```bash
  ./gradlew :app:webApp:wasmJsBrowserDevelopmentRun
  ```
- **Web (JS — compatibilitate extinsă):**
  ```bash
  ./gradlew :app:webApp:jsBrowserDevelopmentRun
  ```
- **iOS (doar pe macOS):**
  Deschide `app/iosApp/iosApp.xcodeproj` în Xcode și rulează pe un Simulator sau dispozitiv iOS.

## Cum rulezi testele

- **Build complet și toate testele disponibile pe platforma curentă:**
  ```bash
  ./gradlew build allTests
  ```
- **Pe ținte individuale:**
  - Desktop (JVM — domeniu, SQLDelight in-memory, ViewModel, paritate i18n, UI Compose, smoke checklist pe SQLite real):
    ```bash
    ./gradlew :core:jvmTest :app:shared:jvmTest
    ```
  - Android host tests (domeniu + ViewModel):
    ```bash
    ./gradlew :core:testAndroidHostTest :app:shared:testAndroidHostTest
    ```
  - Android teste instrumentate (inclusiv suita Compose UI `RetetarFlowsUiTest` pe emulator/dispozitiv conectat):
    ```bash
    ./gradlew :app:shared:connectedAndroidDeviceTest
    ```
  - Web (WasmJS & JS în Chrome headless — domeniu, `localStorage` real, ViewModel):
    ```bash
    ./gradlew :core:wasmJsBrowserTest :app:shared:wasmJsBrowserTest :core:jsBrowserTest :app:shared:jsBrowserTest
    ```
  - iOS Simulator (pe macOS / CI):
    ```bash
    ./gradlew :core:iosSimulatorArm64Test :app:shared:iosSimulatorArm64Test
    ```

## Cum împachetezi aplicația

- **Android (Release APK și Android App Bundle AAB):**
  ```bash
  ./gradlew :app:androidApp:assembleRelease :app:androidApp:bundleRelease
  ```
  Artefacte generate în `app/androidApp/build/outputs/apk/release/` și `app/androidApp/build/outputs/bundle/release/`. *(Pentru publicare în Google Play, adaugă `signingConfigs` în `app/androidApp/build.gradle.kts` folosind variabile de mediu sau `keystore.properties` — fără secrete în Git.)*
- **Desktop (instalator nativ pentru sistemul de operare curent — `.msi` pe Windows, `.dmg` pe macOS, `.deb` pe Linux):**
  ```bash
  ./gradlew :app:desktopApp:packageDistributionForCurrentOS
  ```
  Artefacte generate în `app/desktopApp/build/compose/binaries/main/`. Configurația include modulele JDK `java.sql`, `java.prefs` și `java.logging` necesare pentru SQLDelight și `multiplatform-settings`.
- **Web (bundle static de producție pentru hosting static):**
  ```bash
  ./gradlew :app:webApp:wasmJsBrowserDistribution :app:webApp:jsBrowserDistribution
  ```
  Artefacte generate în `app/webApp/build/dist/wasmJs/productionExecutable/` și `app/webApp/build/dist/js/productionExecutable/`.
- **iOS (pe macOS):**
  Deschide `app/iosApp/iosApp.xcodeproj` în Xcode și rulează **Product → Archive**.

## Decizii

### 1. Persistența pe Web (fallback JSON în `localStorage`)
- **Decizie:** Android, iOS și Desktop (JVM) folosesc **SQLDelight** (`android-driver`, `native-driver`, `sqlite-driver`) cu `PRAGMA foreign_keys = ON` și migrări verificate `.sqm`. Pe țintele Web (`wasmJs` și `js`), aceleași interfețe de repository din `commonMain` sunt implementate peste un store JSON persistent în `window.localStorage` (`JsonDatabaseStorage` + `SettingsJsonKeyValueStore` cu `kotlinx.serialization` și `multiplatform-settings`).
- **Motivație:**
  1. `web-worker-driver` din SQLDelight rulează SQL.js implicit în memorie (fără persistență între reîncărcări fără un worker custom cu OPFS + headere COOP/COEP) și impune `generateAsync = true`, schimbând semnătura interogărilor pe toate țintele.
  2. Pentru volumul unei aplicații personale offline (sute de KB), serializarea JSON în `localStorage` este sincronă, persistentă între sesiuni pe ambele ținte (`js` și `wasmJs`) și ușor de testat în Chrome headless.

### 2. Ștergere în cascadă și actualizare sigură
- `PRAGMA foreign_keys = ON` este activat pe toate driverele SQLDelight și replicat logic în repository-urile JSON de pe Web.
- Ștergerea unei rețete șterge în cascadă ingredientele, pașii, asocierile de etichete și intrările `MealPlanEntry` care o referențiază; înainte de ștergere, UI-ul afișează dialog de confirmare cu numărul intrărilor afectate.
- Actualizarea unei rețete folosește `INSERT OR IGNORE` + `UPDATE` în aceeași tranzacție (compatibil SQLite 3.9+ / Android API 24+) pentru a evita ștergerea accidentală în cascadă a planului la editare (ceea ce ar produce `INSERT OR REPLACE`).

### 3. Arhitectură, DI și testabilitate
- **Clean Architecture + MVVM UDF:** dependențe unidirecționale `presentation → domain ← data`, fără importuri Android/JVM în `commonMain`.
- **Injecție dependențe:** Koin (`coreModules` + `presentationModule`), cu `Clock` și `IdGenerator` injectabile pentru teste deterministe.
- **Localizare live:** `ProvideAppLocale` (`expect/actual`) aplică schimbarea RO/EN fără repornirea aplicației, păstrând starea de navigare.

### 4. Strategie de testare Compose UI multiplatformă
- Suita [RetetarFlowsUiTest.kt](app/shared/src/commonTest/kotlin/com/ro/aminvatatro/ui/RetetarFlowsUiTest.kt) folosește `androidx.compose.ui.test.v2.runComposeUiTest` și rulează pe **Desktop JVM (Skiko)**, **Android (instrumentat pe emulator/dispozitiv)** și **iOS Simulator (CI macOS)**.
- Este exclusă din `testAndroidHostTest` (fără runtime Activity/Robolectric) și din `jsBrowserTest`/`wasmJsBrowserTest` (runner-ul Karma nu inițializează Skiko JS și nu servește arhivele `.cvr` ale Compose Resources). Pe aceste ținte rulează în continuare toate testele de domeniu, persistență și ViewModel.

## Limitări cunoscute

- **Web:**
  - `localStorage` are o limită tipică de ~5 MB per origine; pentru backup sau biblioteci foarte mari se poate folosi exportul/importul JSON din Setări.
  - Testele UI Compose (`RetetarFlowsUiTest`) nu rulează în runner-ul Karma (`jsBrowserTest`/`wasmJsBrowserTest`); acoperirea Web automatizată acoperă complet stratul de domeniu, persistența `localStorage` și ViewModel-urile.
  - `wasmJs` necesită browsere moderne cu suport WasmGC; `js` este disponibil ca fallback.
- **iOS:**
  - Verificat doar în CI pe `macos-latest` (`iosSimulatorArm64Test`); dezvoltarea locală s-a realizat pe Windows (fără Xcode local pentru compilare/arhivare iOS).
- **Sinonime și plural la ingrediente:**
  - Normalizarea elimină diacriticele (inclusiv variante cu sedilă `ş`/`ţ`), diferențele de majuscule și spațiile multiple, dar nu conține dicționar de sinonime sau lemmatizare (de ex. „ou” vs. „ouă” sau „roșie” vs. „roșii” nu sunt contopite automat; utilizatorul poate edita numele ingredientelor).
- **Limbă:**
  - Toate textele UI sunt localizate în RO și EN (paritate verificată automat prin `LocalizationParityTest`), dar cele 10 rețete demo incluse și rețetele create de utilizator nu sunt traduse automat.
- **Import / Export:**
  - Exportul și importul JSON funcționează prin copiere/lipire în ecranul Setări (fără file picker nativ în MVP).

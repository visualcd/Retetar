\# PLAN.md — Rețetar (rețete și planificator săptămânal de mese)

Document de lucru pentru agent. Regulile generale sunt în AGENTS.md. Lucrezi o singură fază odată (secțiunea 8).

\## 1. Obiectiv

Aplicație locală (offline), fără cont, fără rețea și fără cloud, pentru Android, iOS, Desktop (Windows/Linux/macOS prin JVM) și Web, cu UI partajat (Compose Multiplatform). Proiectul este generat din wizard-ul KMP (Gradle, Android, iOS, Desktop, Web, teste incluse). Dacă există modul \`server\`, nu-l folosi și nu-l modifica.

\## 2. Funcționalități (MVP)

1\. \*\*Rețete.\*\* Titlu, descriere, porții de bază (1–100), timp de pregătire și de gătire (minute), ingrediente (nume, cantitate, unitate, raion opțional), pași ordonați, etichete, note, favorite. CRUD complet cu validare. Căutare după titlu și ingredient (fără diacritice). Filtre: etichete, timp total maxim, favorite.

2\. \*\*Scalare porții.\*\* Pagina de detaliu permite alegerea numărului de porții. Toate cantitățile se recalculează proporțional și se afișează rotunjit prietenos: maxim 2 zecimale, separator zecimal după limbă, fracții ¼ ⅓ ½ ⅔ ¾ când valoarea e aproape exactă.

3\. \*\*Plan săptămânal.\*\* Săptămână de luni până duminică, 4 sloturi pe zi (mic dejun, prânz, cină, gustare). Fiecare slot are zero sau mai multe intrări (rețetă + număr de porții). Acțiuni: adaugă, schimbă porțiile, șterge, copiază ziua pe altă zi, repetă săptămâna anterioară, navighează între săptămâni. Fără drag-and-drop (dialoguri și butoane).

4\. \*\*Listă de cumpărături\*\* pe săptămâna selectată, generată din plan (algoritmul din secțiunea 6). Articole bifabile, articole manuale, „șterge bifatele”, grupare pe raion, copiere în clipboard ca text. La regenerare se păstrează starea „bifat” și articolele manuale.

5\. \*\*Import/export JSON\*\* cu \`schemaVersion\`. Export pentru toate rețetele sau pentru cele selectate; import cu validare și politică de duplicate (sari peste / păstrează ambele). Implementarea comună folosește text (clipboard sau câmp de text). Selectorul de fișiere doar dacă există o bibliotecă KMP validată pe toate țintele; altfel se documentează ca limitare.

6\. \*\*Setări.\*\* Temă (sistem / luminoasă / întunecată), limbă (RO / EN), sistem de unități la afișare (metric / imperial), date demo încărcabile și ștergibile. Dacă schimbarea limbii în timpul rulării nu e fiabilă pe o țintă, folosește limba sistemului pe acea țintă și documentează limitarea.

\## 3. Model de domeniu

\- \`Recipe\`, \`Ingredient\`, \`Step\`, \`Tag\`.

\- \`MealSlot\`: BREAKFAST, LUNCH, DINNER, SNACK.

\- \`MealPlanEntry(date, slot, recipeId, servings)\`.

\- \`ShoppingItem(weekStart, itemKey, name, normalizedName, quantity?, unit, aisle, source AUTO\|MANUAL, checked)\`.

\- \`Unit\` cu categorie și factor spre unitatea de bază:

  \- MASS (bază: g): G, KG, OZ, LB

  \- VOLUME (bază: ml): ML, L, TSP, TBSP, CUP

  \- COUNT: PIECE

  \- OTHER: PINCH, TO\_TASTE

\- \`Aisle\` (raion): PRODUCE, MEAT\_FISH, DAIRY, BAKERY, PANTRY, SPICES, FROZEN, OTHER.

\- Cantitățile culinare pot fi Double, cu rotunjire doar la afișare. Id-uri UUID ca text.

\## 4. Structura proiectului

Dacă wizard-ul a generat altă structură (de exemplu fără modul \`shared\`), păstrează separarea pe pachete domain / data / presentation.

\`\`\`

retetar/

├─ composeApp/                    # UI (Compose Multiplatform) + ținte Android/Desktop/Web

│  └─ src/

│     ├─ commonMain/kotlin/.../presentation/{recipes,detail,editor,planner,picker,shopping,settings}

│     ├─ commonMain/kotlin/.../ui/{theme,components,navigation}

│     ├─ commonMain/composeResources/{values,values-ro}

│     └─ commonTest/, desktopTest/    # teste ViewModel și teste UI Compose

├─ shared/                        # domain + data

│  └─ src/

│     ├─ commonMain/kotlin/.../domain/{model,usecase,repository}

│     ├─ commonMain/kotlin/.../core/{units,text,time}

│     ├─ commonMain/kotlin/.../data/{local,repository,importexport,settings}

│     ├─ commonMain/sqldelight/.../\*.sq și migrations/\*.sqm

│     ├─ androidMain/, iosMain/, desktopMain/, wasmJsMain/   # drivere DB, setări, clipboard

│     └─ commonTest/, desktopTest/    # driver in-memory pentru teste de integrare

├─ iosApp/

├─ gradle/libs.versions.toml

├─ .github/workflows/ci.yml

├─ PLAN.md

└─ README.md

\`\`\`

\## 5. Schema bazei de date (SQLDelight)

\`\`\`sql

CREATE TABLE Recipe (

  id TEXT NOT NULL PRIMARY KEY,

  title TEXT NOT NULL,

  normalizedTitle TEXT NOT NULL,

  description TEXT NOT NULL DEFAULT '',

  baseServings INTEGER NOT NULL,

  prepMinutes INTEGER NOT NULL DEFAULT 0,

  cookMinutes INTEGER NOT NULL DEFAULT 0,

  notes TEXT NOT NULL DEFAULT '',

  isFavorite INTEGER NOT NULL DEFAULT 0,

  createdAt INTEGER NOT NULL,

  updatedAt INTEGER NOT NULL

);

CREATE TABLE RecipeIngredient (

  id TEXT NOT NULL PRIMARY KEY,

  recipeId TEXT NOT NULL REFERENCES Recipe(id) ON DELETE CASCADE,

  position INTEGER NOT NULL,

  name TEXT NOT NULL,

  normalizedName TEXT NOT NULL,

  quantity REAL,

  unit TEXT NOT NULL,

  aisle TEXT NOT NULL DEFAULT 'OTHER'

);

CREATE TABLE RecipeStep (

  id TEXT NOT NULL PRIMARY KEY,

  recipeId TEXT NOT NULL REFERENCES Recipe(id) ON DELETE CASCADE,

  position INTEGER NOT NULL,

  text TEXT NOT NULL

);

CREATE TABLE Tag (

  id TEXT NOT NULL PRIMARY KEY,

  name TEXT NOT NULL UNIQUE

);

CREATE TABLE RecipeTag (

  recipeId TEXT NOT NULL REFERENCES Recipe(id) ON DELETE CASCADE,

  tagId TEXT NOT NULL REFERENCES Tag(id) ON DELETE CASCADE,

  PRIMARY KEY (recipeId, tagId)

);

CREATE TABLE MealPlanEntry (

  id TEXT NOT NULL PRIMARY KEY,

  date TEXT NOT NULL,            -- yyyy-MM-dd

  slot TEXT NOT NULL,            -- BREAKFAST \| LUNCH \| DINNER \| SNACK

  recipeId TEXT NOT NULL REFERENCES Recipe(id) ON DELETE CASCADE,

  servings INTEGER NOT NULL

);

CREATE INDEX idx\_plan\_date ON MealPlanEntry(date);

CREATE TABLE ShoppingItem (

  id TEXT NOT NULL PRIMARY KEY,

  weekStart TEXT NOT NULL,       -- data de luni, yyyy-MM-dd

  itemKey TEXT NOT NULL,

  name TEXT NOT NULL,

  normalizedName TEXT NOT NULL,

  quantity REAL,

  unit TEXT NOT NULL,

  aisle TEXT NOT NULL DEFAULT 'OTHER',

  source TEXT NOT NULL,          -- AUTO \| MANUAL

  checked INTEGER NOT NULL DEFAULT 0

);

CREATE INDEX idx\_shopping\_week ON ShoppingItem(weekStart);

\`\`\`

\- Activează \`PRAGMA foreign\_keys = ON\` la deschiderea conexiunii pe fiecare driver și verifică asta într-un test (altfel ștergerea în cascadă nu funcționează).

\- Orice schimbare ulterioară de schemă se face prin fișier \`.sqm\`, cu \`verifyMigrations\` activ.

\- Ștergerea unei rețete folosite în plan cere confirmare și șterge intrările de plan asociate (comportament documentat în README).

\## 6. Reguli de domeniu

\*\*Unități.\*\*

\- MASS: G = 1 g, KG = 1000 g, OZ ≈ 28,3495 g, LB ≈ 453,592 g.

\- VOLUME: ML = 1 ml, L = 1000 ml, TSP = 5 ml, TBSP = 15 ml, CUP = 240 ml.

\- Conversia se face doar în interiorul aceleiași categorii. Între categorii (masă ↔ volum) rezultatul este nul, fără densități.

\*\*Normalizare text.\*\* lowercase, trim, spații multiple reduse la unul, diacritice eliminate (ă, â, î, ș, ț și variantele cu sedilă ş, ţ). Pluralele și sinonimele nu sunt tratate (roșie vs. roșii); limitarea se documentează în README, iar utilizatorul poate edita numele.

\*\*Săptămâna.\*\* \`weekStartOf(date)\` = data de luni a săptămânii care conține \`date\`; săptămâna are 7 zile consecutive. Se testează trecerea de an și săptămâna 53.

\*\*Validări de rețetă.\*\* Titlu 1–100 de caractere; porții 1–100; minim 1 ingredient și 1 pas; cantitate mai mare ca 0 (nulă doar pentru TO\_TASTE); timp 0–1440 minute.

\*\*Cheia articolului din listă (\`itemKey\`).\*\*

\- MASS, VOLUME, COUNT: \`normalizedName\|CATEGORIE\`.

\- OTHER: \`normalizedName\|UNITATE\` (de exemplu \`sare\|TO\_TASTE\`).

\*\*Generarea listei de cumpărături.\*\*

1\. Citește intrările de plan din intervalul \`\[weekStart, weekStart + 6\]\`.

2\. Pentru fiecare intrare, \`factor = servings / baseServings\`. Fiecare ingredient devine \`quantity × factor\`, convertit în unitatea de bază a categoriei.

3\. Grupează după \`itemKey\` și sumează cantitățile.

4\. Unitatea de afișare: dacă toate sursele folosesc aceeași unitate, o păstrezi; altfel folosești unitatea de bază, promovată la kg sau l de la 1000 în sus.

5\. TO\_TASTE apare o singură dată, fără cantitate.

6\. Îmbină cu starea existentă: articolele AUTO cu același \`itemKey\` își actualizează cantitatea și își păstrează \`checked\`; articolele AUTO fără corespondent dispar; articolele MANUAL rămân neatinse.

\*\*Import/export.\*\* JSON cu \`schemaVersion = 1\`, \`exportedAt\` și lista de rețete (cu ingrediente, pași, etichete). La import: validare completă înainte de orice scriere, scriere într-o singură tranzacție, politică de duplicate (după \`id\`, apoi după titlu normalizat). JSON corupt sau \`schemaVersion\` necunoscut produce eroare tipizată, nu excepție.

\## 7. Ecrane și navigare

\- \*\*Rețete.\*\* Listă cu căutare, filtre, buton de rețetă nouă. Stări: încărcare, gol, eroare, listă.

\- \*\*Detaliu rețetă.\*\* Stepper de porții cu recalcularea cantităților, ingrediente, pași, favorite, „Adaugă în plan”, editare, ștergere cu confirmare.

\- \*\*Editor rețetă.\*\* Câmpuri cu validare inline, ingrediente și pași adăugabili, reordonabili prin butoane sus/jos, etichete.

\- \*\*Plan săptămânal.\*\* Selector de săptămână; listă pe zile pe ecran îngust, grilă cu 7 coloane pe ecran lat; acțiuni pe intrare (porții, șterge), copiază ziua, repetă săptămâna anterioară.

\- \*\*Selector rețetă.\*\* Căutare + alegerea porțiilor, pentru un slot dat.

\- \*\*Listă de cumpărături.\*\* Grupare pe raion, bifare, adăugare manuală, „șterge bifatele”, regenerare, copiere ca text.

\- \*\*Setări.\*\* Temă, limbă, unități, date demo, import/export.

Navigare principală cu patru destinații: Rețete, Plan, Listă, Setări (bară jos pe telefon, navigation rail pe tabletă/desktop, prin WindowSizeClass). Detaliul, editorul și selectorul sunt destinații secundare.

\## 8. Faze

Lucrezi o singură fază odată. La final: ./gradlew build, ./gradlew allTests, rularea aplicației pe țintele disponibile, remediere până totul e verde, raport scurt, oprire.

\### Faza 0. Setup și verificarea țintelor

1\. Verifică Git și \`.gitignore\` (build/, .gradle/, local.properties, \*.iml, xcuserdata/).

2\. Rulează template-ul gol pe fiecare țintă disponibilă: Android (emulator), Desktop (\`./gradlew :composeApp:run\`), Web (task-ul de rulare în browser al țintei Web), iOS (doar pe macOS).

3\. Adaugă în \`libs.versions.toml\`: Koin, SQLDelight (plugin + drivere Android, native, JVM, Web dacă există), kotlinx-datetime, kotlinx-serialization-json, kotlinx-coroutines-test, Turbine, DataStore sau multiplatform-settings, Navigation Compose, lifecycle-viewmodel.

4\. Spike persistență pe Web: un tabel minim în SQLDelight sau un store JSON în localStorage. Notează decizia în README, secțiunea „Decizii”.

5\. Adaugă schelet \`ci.yml\` (secțiunea 10).

\- Teste: un test banal în commonTest, rulat de \`allTests\`.

\- Ieșire: build și teste verzi; aplicația goală pornește pe țintele disponibile; decizia pentru Web este documentată.

\### Faza 1. Domain core

1\. Modele: Recipe, Ingredient, Step, Tag, MealSlot, MealPlanEntry, ShoppingItem, Aisle.

2\. \`Unit\`, \`UnitCategory\`, factori de conversie, funcții de scalare și conversie.

3\. \`TextNormalizer\`, \`QuantityFormatter\` (locale, fracții), \`RecipeValidator\` cu erori tipizate, \`WeekUtils\`.

4\. Interfețe \`Clock\` și \`IdGenerator\` injectabile.

\- Teste: conversii (1 kg = 1000 g, 3 TSP = 1 TBSP, 1 CUP = 240 ml, masă ↔ volum nul), scalare (4 → 6 porții, 0,5 bucăți, PINCH), formatare (1,5; 0,33 → ⅓; virgulă ca separator în RO), normalizare (Făină, FAINĂ, făină → faina), validări (fiecare regulă, în ambele sensuri), săptămâna (luni–duminică, 31 decembrie, săptămâna 53).

\- Ieșire: acoperire domain ≥ 85%; niciun import de platformă în commonMain.

\### Faza 2. Persistență

1\. Schema din secțiunea 5, prima migrare \`.sqm\`, interogări: căutare după titlu și ingredient normalizat, filtre, plan pe interval, listă pe săptămână.

2\. \`DriverFactory\` pe țintă (expect/actual) cu \`PRAGMA foreign\_keys = ON\`.

3\. Mappere și implementările \`RecipeRepository\`, \`MealPlanRepository\`, \`ShoppingRepository\`; fallback Web dacă Faza 0 a decis asta; \`SettingsRepository\`.

4\. Date demo: 8–10 rețete în română, în unități metrice (de exemplu paste carbonara, supă cremă de legume, salată grecească, clătite, ciorbă de perișoare, omletă cu legume, tocăniță de pui, ovăz cu fructe, pilaf de legume, mămăligă cu brânză). Cel puțin 3 rețete trebuie să împartă ingrediente (făină, ouă, ulei, ceapă), ca să demonstreze agregarea listei.

5\. Module Koin.

\- Teste (JVM, driver in-memory): CRUD rețetă cu ingrediente, pași și etichete într-o tranzacție; ștergere în cascadă; căutare cu diacritice; filtre; plan pe interval; migrări; foreign keys active.

\- Ieșire: repository-urile trec toate testele; modulele Koin pornesc pe toate țintele.

\### Faza 3. Use case-uri

1\. Rețete: GetRecipes, GetRecipe, SaveRecipe, DeleteRecipe (cu numărul intrărilor de plan afectate), ToggleFavorite, ScaleRecipe.

2\. Plan: SetPlanEntry, RemovePlanEntry, ChangeServings, CopyDay, RepeatPreviousWeek.

3\. Listă: GenerateShoppingList (secțiunea 6), ToggleItem, AddManualItem, RemoveItem, ClearChecked.

4\. ExportRecipes și ImportRecipes (DTO-uri cu \`schemaVersion = 1\`).

\- Teste: agregare (500 g + 1 kg făină = 1,5 kg; Făină + faina într-un singur rând; ml + l; ouă în bucăți separate de ouă în grame), regenerare (bifat păstrat, manual păstrat, rețetă scoasă din plan = articole dispărute), CopyDay și RepeatPreviousWeek, import/export (round-trip, JSON invalid, versiune necunoscută, duplicate).

\- Ieșire: toate testele de use case verzi, fără acces direct la UI sau driver.

\### Faza 4. UI și navigare adaptivă

1\. Tema Material 3 (luminoasă/întunecată), resurse RO și EN (\`composeResources/values\` și \`values-ro\`).

2\. Scaffold adaptiv cu WindowSizeClass: bară jos pe telefon, navigation rail pe ecran lat.

3\. Ecranele din secțiunea 7, fiecare cu ViewModel, \`UiState\` și \`UiEvent\`, plus stări de încărcare, gol și eroare.

4\. Componente comune: IngredientRow, ServingsStepper, TagChips, EmptyState, ConfirmDialog, ErrorBanner.

5\. Ordinea de lucru: Setări și temă → Rețete (listă) → Editor → Detaliu → Plan → Selector → Listă de cumpărături.

\- Teste: ViewModel-uri cu Turbine pe fiecare ecran, pe măsură ce îl construiești.

\- Ieșire: fluxul complet funcționează manual pe Desktop; toate testele ViewModel sunt verzi.

\### Faza 5. Teste UI, i18n, accesibilitate

1\. Teste Compose (compose.uiTest) pentru patru fluxuri: creează rețetă, scalează porțiile, planifică o masă, generează lista și bifează un articol.

2\. Verifică lipsa șirurilor RO/EN și textele tăiate pe ecran îngust.

3\. Verifică \`contentDescription\`, ținte tactile de minim 48dp, navigare cu Tab și Enter pe Desktop/Web.

4\. Testează layout-ul forțat pe ecran îngust și pe ecran lat.

\- Ieșire: testele UI verzi; nicio acțiune importantă fără etichetă de accesibilitate.

\### Faza 6. Verificare și remediere pe toate țintele

1\. Rulează build, allTests și task-urile de rulare pe fiecare țintă. La fiecare eroare: cauză rădăcină, corecție, rulare din nou.

2\. Parcurge checklist-ul din secțiunea 11 pe fiecare țintă disponibilă.

3\. Împachetare (verifică numele exacte ale task-urilor): Android APK/AAB de release, Desktop prin \`packageDistributionForCurrentOS\`, Web prin task-ul de distribuție, iOS prin Xcode (archive).

4\. Dacă testele Web rulează în browser și nu pot rula în mediul curent, rulează-le în CI și spune asta explicit.

\- Ieșire: build și teste verzi; checklist-ul bifat pe țintele verificate local.

\### Faza 7. Documentație și predare

1\. README: cerințe, cum rulezi pe fiecare platformă, cum împachetezi, secțiunea „Decizii”, limitări cunoscute (Web, iOS, sinonime ingrediente, limba).

2\. \`ci.yml\` final.

3\. Raport final: structura proiectului, rezultatul testelor, ce a fost verificat local și ce doar în CI.

\## 9. Teste obligatorii (rezumat)

\- Unități și cantități: conversii în categorie, refuz între categorii, scalare, formatare cu fracții și locale.

\- Text: normalizare cu diacritice și variante cu sedilă, spații multiple.

\- Validare: fiecare regulă, valoare limită inclusă și exclusă.

\- Săptămână: luni–duminică, trecere de an, săptămâna 53.

\- Persistență: CRUD în tranzacție, cascade, căutare, filtre, migrări, foreign keys.

\- Listă de cumpărături: agregare, merge la regenerare, TO\_TASTE unic, articole manuale.

\- Import/export: round-trip, JSON invalid, versiune necunoscută, duplicate, scriere atomică.

\- ViewModel-uri: stări de încărcare/gol/eroare, evenimente, validare în editor.

\- UI: cele patru fluxuri principale, layout îngust și larg.

\## 10. Schelet CI (GitHub Actions)

Potrivește versiunea JDK cu cerințele proiectului și verifică ultimele versiuni ale acțiunilor.

\`\`\`yaml

name: CI

on: \[push, pull\_request\]

jobs:

  linux:

   runs-on: ubuntu-latest

   steps:

   \- uses: actions/checkout@v4

   \- uses: actions/setup-java@v4

   with: { distribution: temurin, java-version: 17 }

   \- uses: gradle/actions/setup-gradle@v4

   \- run: ./gradlew build allTests

  macos:

   runs-on: macos-latest

   steps:

   \- uses: actions/checkout@v4

   \- uses: actions/setup-java@v4

   with: { distribution: temurin, java-version: 17 }

   \- uses: gradle/actions/setup-gradle@v4

   \- run: ./gradlew iosSimulatorArm64Test

\`\`\`

\## 11. Checklist de smoke test (pe fiecare țintă)

1\. Pornește aplicația cu date demo încărcate din Setări.

2\. Creează o rețetă nouă cu 3 ingrediente și 2 pași; încearcă să salvezi una invalidă și verifică mesajele.

3\. Deschide rețeta, schimbă porțiile și verifică recalcularea.

4\. Adaugă două rețete în plan, pe zile și sloturi diferite.

5\. Generează lista și verifică agregarea; bifează un articol; adaugă unul manual.

6\. Regenerează lista și verifică că bifatul și articolul manual au rămas.

7\. Închide și redeschide aplicația: datele trebuie să persiste.

8\. Exportă rețetele ca JSON, șterge una, importă din nou.

9\. Schimbă limba și tema; verifică ambele ecrane principale.

10\. Redimensionează fereastra (Desktop/Web) și verifică trecerea între layout îngust și larg.

\## 12. Riscuri și măsuri

\- \*\*Persistență pe Web.\*\* SQLDelight poate să nu fie disponibil pe ținta Web. Măsură: spike în Faza 0 și interfață de repository comună, cu fallback JSON.

\- \*\*Maturitatea țintei Web (Compose).\*\* Rulează în browsere moderne și are mai puține biblioteci disponibile. Măsură: verifică stadiul în documentația oficială; dacă blochează proiectul, țintele principale rămân Android și Desktop, iar Web e documentat ca limitare.

\- \*\*iOS fără Mac.\*\* Măsură: CI pe runner macOS; în README, iOS e marcat „verificat doar în CI”.

\- \*\*Versiuni incompatibile (Kotlin, Compose, AGP, SQLDelight).\*\* Măsură: nu actualiza versiuni pe parcursul fazelor; schimbi doar dacă un build eșuează și doar cu aprobarea mea.

\- \*\*Sinonime și plural la ingrediente.\*\* Măsură: limitare documentată, nume editabil.

\- \*\*Câmpuri de text pe Desktop/Web (focus, IME).\*\* Măsură: testare manuală a editorului în Fazele 5 și 6.

\- \*\*Extinderea domeniului.\*\* Măsură: doar MVP-ul din secțiunea 2.

\## 13. Definiția „gata”

\- \`./gradlew build allTests\` verde local și în CI (Linux și macOS).

\- Acoperire domain de minim 85%; toate fluxurile UI din Faza 5 verzi.

\- Checklist-ul de smoke test bifat pe Android și Desktop, plus Web și iOS acolo unde sunt disponibile.

\- Fără erori, teste picate, TODO-uri sau cod mort.

\- README complet, cu limitările pe țintă și instrucțiuni de împachetare.

Backlog după MVP (nu se implementează acum): fotografii la rețete, temporizatoare pentru pași, valori nutriționale, înlocuiri de ingrediente, partajarea listei, sincronizare între dispozitive, dicționar de sinonime.

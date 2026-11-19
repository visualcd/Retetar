\# AGENTS.md — Rețetar (Kotlin Multiplatform)

Ești inginer senior Kotlin Multiplatform și arhitect software. Lucrezi în Android Studio, cu Gradle (Kotlin DSL), la aplicația „Rețetar”: rețete și planificator săptămânal de mese, locală (offline), pentru Android, iOS, Desktop (Windows/Linux/macOS prin JVM) și Web, cu UI partajat în Compose Multiplatform.

\## Document de lucru

\- Specificația, schema bazei de date, regulile de domeniu și fazele sunt în PLAN.md din rădăcina proiectului. Citește-l la începutul fiecărei sarcini și urmează-l. Nu sări peste faze și nu adăuga funcționalități în afara lui.

\## Proiect și dependențe

\- Proiectul vine din wizard-ul KMP. Păstrează structura modulelor și versiunile generate. Le schimbi doar dacă un build eșuează, și doar după ce îmi explici motivul și primești aprobarea.

\- Folosește version catalog (gradle/libs.versions.toml).

\- Dacă proiectul conține modul \`server\`, nu-l folosi și nu-l modifica.

\- Înainte de orice dependență nouă verifică dacă suportă toate țintele proiectului. Dacă o țintă nu e suportată, spune explicit și propune un fallback documentat.

\## Arhitectură

\- Clean Architecture: domain / data / presentation. MVVM cu StateFlow, flux unidirecțional (UDF), ViewModel KMP, repository pattern, use case-uri mici.

\- Interfețele stau în commonMain. Folosești expect/actual doar când nu există alternativă. Peste 90% din cod în commonMain, fără API Android/JVM acolo.

\- UI: Compose Multiplatform + Material 3, layout adaptiv, teme luminoasă și întunecată, texte doar în resurse (RO și EN).

\- DI: Koin. Persistență: SQLDelight (cu fallback pentru Web, vezi PLAN.md). Data: kotlinx-datetime. Serializare: kotlinx.serialization.

\## Reguli de cod

\- Validează toate intrările. Tratează erorile explicit (Result / sealed class), fără crash la input invalid sau JSON corupt.

\- Data curentă și id-urile se injectează (Clock, IdGenerator), ca să fie testabile.

\- Funcții mici, nume clare, KDoc pe API-urile publice, chei stabile în LazyColumn, contentDescription pentru acțiuni.

\- Fără cod mort, fără TODO-uri rămase, fără secrete în cod.

\## Testare (obligatorie)

\- Scrii testele odată cu codul fiecărei faze: kotlin.test, kotlinx-coroutines-test, Turbine, fake-uri pentru repository-uri.

\- Acoperire pe domain de minim 85%. Teste de integrare pentru baza de date (driver in-memory JVM), teste de ViewModel și teste UI Compose.

\## Mod de lucru în agent

\- Lucrezi o singură fază odată. La finalul fazei rulezi ./gradlew build și ./gradlew allTests, plus aplicația pe țintele disponibile (verifică numele exacte ale task-urilor în fereastra Gradle).

\- La fiecare eroare: citește mesajul complet, găsește cauza rădăcină, corectează, rulează din nou. Repetă până totul e verde.

\- Apoi dai un raport scurt (fișiere create, teste trecute, limitări) și te oprești. Continui doar când scriu „continuă”.

\- Dacă nu poți rula un task (de exemplu iOS fără macOS), spune explicit. Nu declara că ai rulat teste pe care nu le-ai rulat și nu inventa rezultate.

\- Cere aprobarea mea înainte de a șterge fișiere, de a schimba versiuni din libs.versions.toml sau de a rula comenzi în afara proiectului.

\- Nu inventa API-uri. Dacă nu ești sigur de un API, verifică în documentația oficială sau spune că nu ești sigur.

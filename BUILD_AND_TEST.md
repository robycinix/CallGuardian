# CallGuardian - Build e test

## Requisiti

- Android Studio Ladybug o superiore.
- Android SDK con `compileSdk 35`.
- JDK 17.
- Gradle 8.9 o superiore.

Il progetto usa Kotlin, Jetpack Compose, Material 3, Room SQLite cifrato con SQLCipher, MVVM, Hilt, WorkManager e `CallScreeningService`.

## Build locale

Su questa macchina il build e' stato verificato con:

```powershell
& "C:\Users\rober\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9\bin\gradle.bat" :app:assembleDebug
```

Output verificato:

```text
BUILD SUCCESSFUL
```

APK generato:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Permessi e limiti Android

`CallScreeningService` non puo' bloccare chiamate finche' l'utente non assegna a CallGuardian il ruolo Android `ROLE_CALL_SCREENING`, visibile come app Caller ID & Spam. L'app apre la richiesta ruolo tramite `RoleManager`.

Permessi runtime implementati:

- `READ_PHONE_STATE`: stato telefonico necessario al contesto chiamate.
- `READ_CONTACTS`: consente la regola "estero sconosciuto non in rubrica".
- `POST_NOTIFICATIONS`: richiesto da Android 13 per gli avvisi heads-up.
- `SYSTEM_ALERT_WINDOW`: solo per popup overlay, da abilitare manualmente nelle impostazioni Android.

Android puo' revocare permessi o ruolo: la schermata Protezione mostra lo stato e offre azioni guidate.

## Test manuale consigliato

1. Installa `app-debug.apk` su un dispositivo o emulatore con servizi telefonici.
2. Apri CallGuardian e concedi i permessi runtime.
3. Assegna il ruolo Caller ID & Spam.
4. Inserisci una regola blacklist o prefisso nella sezione Regole.
5. Effettua una chiamata di prova verso il dispositivo.
6. Verifica che Log eventi mostri numero, azione, motivo, punteggio e regola.
7. Modifica una nazione in "BLOCKED" e verifica la classificazione di un numero con quel prefisso.

## Smoke test ADB

Con un dispositivo collegato via USB e debug ADB attivo:

```powershell
.\scripts\test-adb.ps1
```

Lo script esegue:

- verifica device ADB;
- `:app:testDebugUnitTest` e `:app:assembleDebug`;
- installazione di `app-debug.apk`;
- grant dei permessi runtime principali;
- verifica di overlay e ruolo `CALL_SCREENING`;
- avvio app;
- dump UI e screenshot della schermata corrente;
- cattura automatica delle tab Protezione, Regole, Registro, Statistiche e Opzioni;
- `dumpsys package`, `dumpsys telecom` e logcat filtrato.

Gli artefatti vengono salvati in:

```text
build/adb-smoke/
```

Opzioni utili:

```powershell
.\scripts\test-adb.ps1 -SkipBuild -SkipInstall
.\scripts\test-adb.ps1 -DeviceId 45fa7c4d
.\scripts\test-adb.ps1 -TrySetCallScreeningRole
.\scripts\test-adb.ps1 -ClearLogcat
.\scripts\test-adb.ps1 -SkipNavigationCapture
```

Nota: su telefono reale ADB non simula una chiamata cellulare in ingresso completa. Per validare davvero `CallScreeningService` serve una chiamata reale da un secondo telefono/SIM, oppure un emulatore che supporti comandi telefonici. Dopo la chiamata, rilanciare:

```powershell
.\scripts\test-adb.ps1 -SkipBuild -SkipInstall
```

e controllare `build/adb-smoke/telecom.txt`, `build/adb-smoke/logcat-callguardian.txt` e la schermata Log eventi.

## Test automatici futuri

La struttura e' pronta per aggiungere:

- unit test di `PhoneNumberNormalizer`;
- unit test di `CallClassifier`;
- DAO test con Room in-memory;
- UI test Compose per navigazione e regole.

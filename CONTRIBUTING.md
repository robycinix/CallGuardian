# Contribuire a CallGuardian

Grazie per voler migliorare CallGuardian. Il progetto tratta dati sensibili come numeri, regole e log chiamate: ogni modifica deve mantenere privacy locale, chiarezza per l'utente e comportamento Android prevedibile.

## Setup

1. Installa Android Studio Ladybug o superiore.
2. Usa JDK 17.
3. Apri la root del progetto e lascia sincronizzare Gradle.
4. Esegui i test prima di aprire una pull request.

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

## Linee Guida

- Mantieni la logica di classificazione testabile e separata dalla UI.
- Non introdurre servizi remoti obbligatori per funzioni core.
- Non loggare numeri completi fuori dai log locali dell'app.
- Aggiorna README o documentazione quando cambia un flusso utente.
- Per permessi Android, spiega sempre il motivo in UI e documentazione.

## Pull Request

Una buona PR include:

- sintesi chiara del cambiamento;
- motivazione;
- test eseguiti;
- screenshot o video se cambia la UI;
- note su permessi, privacy o compatibilità Android.

## Test Manuale Consigliato

Per modifiche a `CallScreeningService`, permessi o UI di onboarding, valida su dispositivo reale quando possibile. ADB non simula sempre una chiamata cellulare in ingresso completa su telefono reale.

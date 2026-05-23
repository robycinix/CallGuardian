# CallGuardian - Data Safety Play Console

Queste risposte sono preparate per la versione Play senza invio SMS automatici e senza raccolta dati off-device.

## Raccolta e condivisione

L'app raccoglie o condivide dati utente?

Risposta consigliata: No, l'app non raccoglie e non condivide dati utente con server o terze parti.

Motivazione: numeri, rubrica, regole, log e statistiche restano sul dispositivo. Non sono presenti account, analytics, advertising SDK o backend.

## Sicurezza dati

- I dati sono cifrati in transito: Non applicabile, perche' non vengono inviati a server esterni.
- L'utente puo' richiedere l'eliminazione dei dati: Si', i dati locali possono essere cancellati dall'app o disinstallando l'app.
- L'app segue criteri di sicurezza moderni: Si', database locale cifrato con SQLCipher e chiave protetta da Android Keystore.

## Tipi di dati usati solo localmente

Questi dati non sono da dichiarare come raccolti se restano esclusivamente sul dispositivo e non vengono trasmessi off-device, ma devono essere coerenti con privacy policy e permessi:

- Contatti: letti localmente per riconoscere numeri salvati e applicare gruppi di blocco.
- Numeri telefonici in arrivo: valutati localmente dal servizio Android di filtro chiamate.
- Log chiamate dell'app: generati localmente dall'app, non letti dal registro chiamate di sistema.
- Regole e preferenze: salvate nel database locale cifrato.
- Statistiche: derivate localmente dai log dell'app.

## Permessi da spiegare in Play Console

### READ_CONTACTS

Uso: riconoscere contatti salvati e permettere gruppi di blocco basati sulla rubrica.

Testo breve: CallGuardian usa la rubrica solo sul dispositivo per riconoscere contatti salvati e applicare regole locali.

### POST_NOTIFICATIONS

Uso: mostrare l'esito di blocchi, avvisi e stato protezione.

Testo breve: le notifiche mostrano decisioni del filtro chiamate e stato della protezione.

### SYSTEM_ALERT_WINDOW

Uso: popup opzionale sopra altre app durante una chiamata sospetta.

Testo breve: il popup opzionale mostra un avviso visibile durante chiamate sospette; puo' essere abilitato o disabilitato dall'utente dalle impostazioni Android.

Nota: questo permesso e' sensibile. Se vogliamo ridurre al massimo il rischio review, valutare una build Play senza overlay.

### CallScreeningService / ruolo ID chiamante e spam

Uso: consentire ad Android di inviare le chiamate in arrivo a CallGuardian prima dello squillo, cosi' l'app puo' consentire, avvisare, silenziare o bloccare secondo le regole locali.

Testo breve: necessario per filtrare realmente le chiamate in arrivo tramite le API Android.

## Dichiarazioni da mantenere coerenti

- Nessun SMS automatico.
- Nessun accesso al registro chiamate di sistema.
- Nessuna registrazione audio.
- Nessuna localizzazione.
- Nessun account utente.
- Nessuna pubblicita'.
- Nessun acquisto in-app, salvo decisione futura.

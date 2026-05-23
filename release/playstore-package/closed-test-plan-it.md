# CallGuardian - Piano test chiuso Play

## Obiettivo

Validare CallGuardian prima della produzione Play Store con tester reali, controllando installazione, onboarding, ruolo ID chiamante e spam, regole di blocco, notifiche, log e stabilita' generale.

## Tester

Requisito Play per account personali nuovi: almeno 12 tester opt-in per 14 giorni continuativi prima della richiesta di produzione.

Consiglio operativo: invitare 16 tester per avere margine se qualcuno non installa o esce dal test.

## Istruzioni per tester

1. Apri il link di opt-in del test chiuso.
2. Accetta di partecipare al test.
3. Installa CallGuardian dal Play Store.
4. Apri l'app e completa l'onboarding.
5. Concedi rubrica e notifiche.
6. Attiva CallGuardian come app ID chiamante e spam.
7. Crea almeno una regola di blocco numero o prefisso.
8. Crea un gruppo di blocco e aggiungi un contatto di prova.
9. Apri Registro e Statistiche dopo alcune prove.
10. Invia feedback con modello sotto.

## Scenario test minimo

- Installazione pulita.
- Primo avvio e schermata permessi.
- Attivazione ruolo ID chiamante e spam.
- Creazione blacklist con un numero.
- Creazione whitelist con un numero.
- Creazione gruppo di blocco da rubrica.
- Cambio livello protezione.
- Apertura log e statistiche.
- Cambio tema o lingua.
- Esportazione impostazioni.

## Scenario test chiamata reale

Quando possibile:

1. Inserire in blacklist il numero di un secondo telefono.
2. Chiamare il dispositivo con quel secondo telefono.
3. Verificare blocco o avviso.
4. Verificare presenza evento nel registro locale.

## Feedback richiesto

Oggetto: Feedback CallGuardian test chiuso

- Modello telefono:
- Versione Android:
- Installazione riuscita: si/no
- Onboarding chiaro: si/no
- Ruolo ID chiamante e spam trovato: si/no
- Regole create senza problemi: si/no
- Log/statistiche visibili: si/no
- Crash o blocchi:
- Screenshot eventuali:
- Suggerimenti:

## Domande per richiesta accesso produzione

Quando Play Console chiede cosa e' stato testato, usare questa sintesi:

Abbiamo testato installazione, onboarding, permessi, ruolo ID chiamante e spam, creazione regole, gruppi di blocco contatti, notifiche, log, statistiche, cambio lingua/tema ed esportazione locale. I tester hanno verificato stabilita' generale e chiarezza del flusso iniziale su dispositivi Android diversi.

Quando chiede quali problemi sono stati trovati:

Indicare problemi reali emersi, oppure: Non sono emersi crash bloccanti durante il test. Abbiamo raccolto feedback su chiarezza dell'onboarding e compatibilita' del ruolo ID chiamante e spam sui diversi dispositivi.

Quando chiede perche' l'app e' pronta:

La build e' stabile, i test unitari passano, il bundle release e' stato generato correttamente e le funzioni principali sono state validate dai tester. L'app non raccoglie dati off-device e usa i permessi solo per funzioni dichiarate nella scheda Play e nella privacy policy.

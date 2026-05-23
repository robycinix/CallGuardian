# Security Policy

CallGuardian gestisce dati potenzialmente sensibili: numeri telefonici, regole locali, log chiamate e statistiche. Le vulnerabilità vanno trattate con discrezione.

## Segnalazione

Per problemi di sicurezza, usa GitHub Private Vulnerability Reporting se disponibile nel repository. Se non è attivo, apri una issue senza dettagli sensibili chiedendo un canale privato di contatto.

Non includere pubblicamente:

- numeri reali;
- database esportati;
- chiavi o keystore;
- log completi con dati personali;
- dettagli exploit riproducibili prima della correzione.

## Ambito

Sono particolarmente importanti:

- esposizione accidentale di rubrica, log o regole;
- bypass del blocco locale;
- backup non cifrati o inclusi in cloud backup;
- gestione insicura della chiave SQLCipher;
- permessi Android richiesti senza necessità.

## Principi

- Dati locali per default.
- Nessun server obbligatorio per la protezione core.
- Minimo accesso ai permessi Android.
- Log utili ma non invasivi.

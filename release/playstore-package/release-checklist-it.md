# CallGuardian - Checklist pubblicazione Play

## Stato tecnico locale

- [x] `targetSdk = 35`
- [x] Build release AAB generabile
- [x] Unit test eseguiti
- [x] `SEND_SMS` rimosso
- [x] `READ_PHONE_STATE` rimosso
- [x] Upload keystore locale creato
- [x] Firma release configurata tramite `keystore.properties`
- [ ] AAB caricato su Play Console
- [ ] Privacy policy pubblicata su URL pubblico
- [ ] Internal test avviato
- [ ] Closed test avviato, se richiesto dall'account
- [ ] Produzione richiesta/approvata

## File locali importanti

- AAB release: `app/build/outputs/bundle/release/app-release.aab`
- Upload keystore: `release/callguardian-upload.jks`
- Proprietà firma: `keystore.properties`
- Privacy policy: `docs/play/privacy-policy-it.html`
- URL privacy policy pubblico: `https://robycinix.github.io/CallGuardian/privacy-policy-it.html`
- Scheda store: `docs/play/store-listing-it.md`
- Data safety: `docs/play/data-safety-it.md`
- Piano test: `docs/play/closed-test-plan-it.md`
- Icona Play 512x512: `docs/play/assets/icon-512.png`
- Feature graphic 1024x500: `docs/play/assets/feature-graphic-1024x500.png`

## Play Console - Setup app

1. Creare app in Play Console.
2. Lingua predefinita: Italiano.
3. Tipo app: App.
4. Gratuita o a pagamento: consigliato Gratuita per prima release.
5. Categoria: Strumenti.
6. Dichiarare assenza pubblicita'.
7. Inserire email supporto.
8. Inserire URL privacy policy pubblica.

## Play Console - App content

1. Data safety: usare `docs/play/data-safety-it.md`.
2. Content rating: compilare questionario; app senza contenuti violenti, sessuali, gambling o user generated content.
3. Target audience: adulti/generale, non specificamente bambini.
4. News app: No.
5. Government app: No.
6. Financial features: No.
7. Health features: No.
8. Ads: No.
9. App access: tutte le funzioni principali sono accessibili senza login.
10. Sensitive permissions: spiegare `READ_CONTACTS`, `SYSTEM_ALERT_WINDOW` e ruolo ID chiamante e spam.

## Store listing

1. Titolo: CallGuardian.
2. Descrizione breve e completa da `docs/play/store-listing-it.md`.
3. Icona 512x512: usare `docs/play/assets/icon-512.png`.
4. Feature graphic 1024x500: usare `docs/play/assets/feature-graphic-1024x500.png`.
5. Almeno 2 screenshot telefono, consigliati 4-6.
6. Screenshot consigliati:
   - Protezione pronta.
   - Regole/blacklist.
   - Gruppi di blocco.
   - Registro eventi.
   - Statistiche.
   - Impostazioni/privacy.

## Test prima produzione

1. Caricare AAB su Internal testing.
2. Installare su almeno un dispositivo reale.
3. Verificare onboarding e ruolo ID chiamante e spam.
4. Verificare regole e log.
5. Se account personale nuovo, caricare su Closed testing.
6. Invitare almeno 12 tester opt-in per 14 giorni continuativi.
7. Richiedere accesso produzione.

## Decisione aperta

`SYSTEM_ALERT_WINDOW` e' ancora presente per il popup in chiamata. E' coerente con una funzione reale, ma resta un permesso sensibile. Se la prima review dovesse essere prudente, preparare una variante senza overlay.

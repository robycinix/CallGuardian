# CallGuardian — Specifica Tecnica Completa

## Visione del Progetto

CallGuardian è un'applicazione Android progettata per la protezione intelligente dalle chiamate indesiderate, spam e potenzialmente fraudolente.

L'applicazione deve offrire:

- massima privacy;
- funzionamento locale;
- controllo avanzato delle chiamate;
- gestione intelligente delle regole;
- interfaccia professionale e rassicurante;
- elevata configurabilità.

Il progetto deve essere sviluppato come applicazione reale e professionale.

---

# Architettura Tecnologica

## Stack principale

- Linguaggio: Kotlin
- UI Framework: Jetpack Compose
- Database locale: Room SQLite
- Pattern architetturale: MVVM
- Dependency Injection: Hilt
- Background tasks: WorkManager
- Gestione chiamate: CallScreeningService
- Sicurezza locale: SQLCipher / cifratura database
- Logging: locale
- Nessun server obbligatorio

---

# Filosofia del Progetto

## Privacy First

CallGuardian deve funzionare completamente in locale.

Nessun caricamento obbligatorio di:

- rubrica;
- cronologia chiamate;
- blacklist;
- statistiche;
- numeri telefonici.

Ogni eventuale funzione cloud futura dovrà essere opzionale.

---

# UI / UX

## Obiettivi UI

L'interfaccia deve:

- trasmettere sicurezza;
- risultare professionale;
- essere ordinata;
- evitare colori aggressivi;
- essere facilmente navigabile.

---

# Temi Grafici

## Modalità supportate

- Tema chiaro
- Tema scuro
- Tema automatico sistema

## Palette selezionabili

- Blu sicurezza
- Verde protezione
- Grigio professionale
- Viola tecnico

## Colore predefinito

Blu scuro professionale.

---

# Accessibilità

Supportare:

- font scalabili;
- alto contrasto;
- TalkBack Android;
- pulsanti grandi;
- navigazione chiara.

---

# Navigazione Principale

Bottom Navigation Bar con:

1. Protezione
2. Regole
3. Log
4. Statistiche
5. Configurazione

---

# Home / Protezione

Mostrare:

- stato protezione;
- numero chiamate bloccate;
- livello protezione;
- ultimi eventi;
- stato permessi Android;
- azioni rapide.

---

# Regole

La sezione Regole deve permettere:

- blocco numeri;
- blocco prefissi;
- blocco range;
- blocco chiamate anonime;
- blocco chiamate estere;
- whitelist;
- regole orarie;
- gestione paesi.

---

# Gestione Chiamate Estere

## Modalità supportate

- Solo avviso
- Blocca numeri esteri sconosciuti
- Blocca tutti i numeri esteri
- Blocca per nazione
- Modalità oraria

## Regola intelligente

Se attivato:

"Blocca chiamate estere non presenti in rubrica"

CallGuardian deve:

- consentire numeri salvati;
- consentire whitelist;
- consentire nazioni autorizzate;
- bloccare numeri internazionali sconosciuti.

---

# Selettore Nazioni

Il selettore deve mostrare:

- bandiera;
- nome nazione;
- prefisso internazionale.

## Funzioni aggiuntive

- ricerca rapida;
- filtro;
- stato regola.

## Stati supportati

- consentito;
- monitorato;
- bloccato.

---

# Gestione Blocchi

Ogni blocco deve essere:

- attivabile;
- disattivabile;
- eliminabile;
- modificabile.

## Priorità regole

1. Whitelist
2. Rubrica
3. Regole consentite
4. Blacklist diretta
5. Range/prefissi/nazioni
6. Regole automatiche
7. Solo avviso

---

# Popup Chiamata

Quando arriva una chiamata sconosciuta:

Mostrare popup basso o heads-up notification.

## Informazioni visualizzate

- numero;
- nazione;
- classificazione;
- motivo rischio.

## Azioni rapide

- Consenti
- Blocca
- Blocca range
- Segnala
- Consenti sempre

---

# Sistema Classificazione Chiamate

Sistema a punteggio.

## Esempio

| Evento | Punti |
|---|---|
| Numero blacklist | +100 |
| Range sospetto | +40 |
| Numero estero sconosciuto | +25 |
| Chiamate ravvicinate | +30 |
| Numero anonimo | +50 |
| Numero whitelist | -100 |

## Livelli

- 0–30: normale
- 31–70: sospetto
- 71+: spam probabile

---

# Modalità Protezione

## Livelli

- Disattivata
- Leggera
- Bilanciata
- Aggressiva
- Personalizzata

---

# Gestione Numeri Anonimi

Opzioni:

- avviso;
- silenzia;
- blocca;
- consenti dopo tentativi ripetuti.

---

# Sistema Anti-Falsi Positivi

Se l'utente richiama un numero bloccato:

Mostrare:

"Vuoi considerare questo numero sicuro?"

---

# Log Eventi

Registrare:

- data;
- ora;
- numero;
- azione;
- motivo;
- regola applicata.

---

# Statistiche

Mostrare:

- chiamate bloccate oggi;
- chiamate settimanali;
- categorie spam;
- nazioni più bloccate;
- range più attivi.

La grafica deve essere sobria.

---

# Sistema Aiuto Contestuale

Ogni funzione avanzata deve avere:

- pulsante ? o i;
- popup informativo.

## Ogni popup deve includere

- spiegazione funzione;
- vantaggi;
- svantaggi;
- consigli utilizzo;
- eventuali limiti Android.

---

# Backup Locale

Supportare:

- esportazione database;
- importazione database;
- backup manuale.

Formati:

- JSON
- backup cifrato.

---

# Sicurezza

Il database deve essere:

- locale;
- cifrato;
- protetto.

---

# Prestazioni

CallGuardian deve:

- minimizzare consumo batteria;
- evitare polling inutili;
- usare API Android ufficiali;
- ridurre wake lock.

---

# Wizard Primo Avvio

Configurazione guidata:

1. permessi chiamate;
2. notifiche;
3. overlay;
4. whitelist iniziale;
5. livello protezione.

---

# Gestione Errori Android

Se Android:

- revoca permessi;
- blocca servizi;
- limita background;

CallGuardian deve:

- notificare chiaramente;
- guidare l'utente;
- mostrare istruzioni risoluzione.

---

# Identità Grafica

## Linea stilistica

- professionale;
- tecnica;
- rassicurante;
- minimal.

## Evitare

- grafica aggressiva;
- effetti hacker;
- colori eccessivi;
- animazioni invasive.

---

# Futuri Sviluppi Possibili

- sincronizzazione opzionale;
- community blacklist;
- machine learning locale;
- riconoscimento vocale robocall;
- protezione SMS spam.

---

# Obiettivo Finale

CallGuardian deve diventare un sistema professionale di protezione telefonica Android orientato a:

- sicurezza;
- privacy;
- affidabilità;
- controllo avanzato;
- semplicità d'uso.

Il codice generato deve essere modulare, mantenibile e pronto per evoluzioni future.


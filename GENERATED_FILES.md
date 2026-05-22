# File generati

Ogni file sotto contiene codice completo, non placeholder. Questa pagina spiega sinteticamente il ruolo dei file creati.

| Percorso | Spiegazione sintetica |
|---|---|
| `settings.gradle.kts` | Configura plugin repository, dependency resolution e modulo `:app`. |
| `build.gradle.kts` | Dichiara plugin Android, Kotlin, Kapt e Hilt a livello root. |
| `gradle.properties` | Imposta opzioni Gradle, AndroidX e stile Kotlin. |
| `local.properties` | Punta all'SDK Android locale della macchina. |
| `app/build.gradle.kts` | Configura app Android Kotlin/Compose, Java 17 e dipendenze Room, Hilt, SQLCipher, WorkManager, Material 3. |
| `app/src/main/AndroidManifest.xml` | Dichiara permessi, `MainActivity` e `GuardianCallScreeningService` con `BIND_SCREENING_SERVICE`. |
| `app/src/main/res/values/strings.xml` | Stringhe app e canali notifica. |
| `app/src/main/res/values/colors.xml` | Colore base icona. |
| `app/src/main/res/values/styles.xml` | Tema host Android no-action-bar per Compose. |
| `app/src/main/res/xml/backup_rules.xml` | Esclude database cifrato dal backup cloud automatico. |
| `app/src/main/res/xml/data_extraction_rules.xml` | Esclude database cifrato da backup e device transfer automatici. |
| `app/src/main/res/drawable/ic_guardian_shield.xml` | Icona vettoriale usata come small icon per le notifiche Android. |
| `app/src/main/res/drawable/ic_callguardian_logo_vector.xml` | Versione VectorDrawable fedele all'asset `CallGuardian.png`, utile per UI e anteprime scalabili. |
| `app/src/main/res/mipmap-*/ic_launcher.png` | Icona launcher generata dall'asset `CallGuardian.png` gia' presente nel progetto. |
| `app/src/main/res/mipmap-*/ic_launcher_round.png` | Variante round icon generata dall'asset `CallGuardian.png`. |
| `app/src/main/java/com/callguardian/app/CallGuardianApp.kt` | Application Hilt e inizializzazione canali notifica. |
| `app/src/main/java/com/callguardian/app/MainActivity.kt` | Activity Compose, azioni permessi, ruolo call screening e overlay. |
| `app/src/main/java/com/callguardian/app/core/model/Models.kt` | Enum e modelli dominio: regole, azioni, rischio, temi, paesi supportati. |
| `app/src/main/java/com/callguardian/app/core/permissions/PermissionChecker.kt` | Lettura stato permessi, notifiche, overlay e ruolo `ROLE_CALL_SCREENING`. |
| `app/src/main/java/com/callguardian/app/data/local/Entities.kt` | Entita' Room per regole, log eventi, paesi e impostazioni. |
| `app/src/main/java/com/callguardian/app/data/local/Converters.kt` | TypeConverter Room per enum salvati come stringhe. |
| `app/src/main/java/com/callguardian/app/data/local/CallGuardianDao.kt` | DAO Room con query Flow e operazioni CRUD. |
| `app/src/main/java/com/callguardian/app/data/local/CallGuardianDatabase.kt` | Database Room principale. |
| `app/src/main/java/com/callguardian/app/data/local/SecureDatabaseKeyProvider.kt` | Genera passphrase SQLCipher e la protegge con Android Keystore. |
| `app/src/main/java/com/callguardian/app/data/local/DatabaseSeeder.kt` | Seed iniziale di impostazioni, paesi e regola anonimi. |
| `app/src/main/java/com/callguardian/app/data/repository/CallClassifier.kt` | Motore punteggio rischio e priorita' regole. |
| `app/src/main/java/com/callguardian/app/data/repository/GuardianRepository.kt` | Repository MVVM per regole, log, statistiche, impostazioni e screening chiamate. |
| `app/src/main/java/com/callguardian/app/data/backup/LocalBackupManager.kt` | Export JSON locale dei dati principali. |
| `app/src/main/java/com/callguardian/app/di/AppModule.kt` | Provider Hilt per database SQLCipher, DAO e Gson. |
| `app/src/main/java/com/callguardian/app/telephony/PhoneNumberNormalizer.kt` | Normalizzazione numeri e rilevamento paese/prefisso. |
| `app/src/main/java/com/callguardian/app/telephony/ContactLookup.kt` | Lookup locale in rubrica tramite `ContactsContract.PhoneLookup`. |
| `app/src/main/java/com/callguardian/app/telephony/NotificationHelper.kt` | Canali notifica e avvisi locali per chiamate sospette. |
| `app/src/main/java/com/callguardian/app/telephony/GuardianCallScreeningService.kt` | Implementazione reale di `CallScreeningService` con risposta allow/warn/silence/block. |
| `app/src/main/java/com/callguardian/app/viewmodel/ProtectionViewModel.kt` | Stato protezione, permessi, bloccate oggi e ultimi eventi. |
| `app/src/main/java/com/callguardian/app/viewmodel/RulesViewModel.kt` | Stato e azioni CRUD per regole e nazioni. |
| `app/src/main/java/com/callguardian/app/viewmodel/LogsViewModel.kt` | Stato log eventi, contatori e statistiche paesi. |
| `app/src/main/java/com/callguardian/app/viewmodel/SettingsViewModel.kt` | Gestione impostazioni protezione, anonimi, estero, tema e aiuti. |
| `app/src/main/java/com/callguardian/app/worker/LogMaintenanceWorker.kt` | Worker periodico locale che elimina log piu' vecchi di 180 giorni senza polling chiamate. |
| `app/src/main/java/com/callguardian/app/ui/theme/Theme.kt` | Tema Material 3 chiaro/scuro blu sicurezza. |
| `app/src/main/java/com/callguardian/app/ui/components/HelpDialog.kt` | Sistema aiuti contestuali con popup informativo completo. |
| `app/src/main/java/com/callguardian/app/ui/components/Section.kt` | Componente sezione/card riusabile. |
| `app/src/main/java/com/callguardian/app/ui/navigation/CallGuardianNavHost.kt` | Navigazione Compose con bottom bar: Protezione, Regole, Log, Statistiche, Config. |
| `app/src/main/java/com/callguardian/app/ui/screens/ProtectionScreen.kt` | Home protezione con stato, permessi, azioni rapide e ultimi eventi. |
| `app/src/main/java/com/callguardian/app/ui/screens/RulesScreen.kt` | Gestione numeri, prefissi, whitelist/blacklist e paesi. |
| `app/src/main/java/com/callguardian/app/ui/screens/LogScreen.kt` | Registro eventi con data, azione, motivo e regola applicata. |
| `app/src/main/java/com/callguardian/app/ui/screens/StatsScreen.kt` | Statistiche sobrie su blocchi, paesi e categorie. |
| `app/src/main/java/com/callguardian/app/ui/screens/SettingsScreen.kt` | Impostazioni protezione, anonimi, estero, tema, accessibilita' e backup. |

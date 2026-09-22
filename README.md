# Trotta Bus (non ufficiale)

App Android per il trasporto pubblico urbano di Trotta Bus Services nelle reti di Fiumicino e Campobasso. Serve a consultare fermate, linee e orari, a vedere le fermate vicine e a comprare e validare i biglietti tramite il sito del gestore, ticketonbus.trotta.it.

Questa applicazione non è ufficiale e non ha alcun rapporto con Trotta Bus Services S.p.A.

## Funzioni

- Elenco fermate con ricerca per nome o per linea, ordinamento per distanza quando la posizione è disponibile e fermate preferite salvate sul dispositivo.
- Scheda fermata con i prossimi passaggi divisi tra oggi e domani, il tempo mancante, gli orari per linea e la mappa.
- Elenco linee con numero di corse, giorni di servizio e stagione (invernale o estiva), più una scheda per linea con tutte le fermate e i relativi orari.
- Mappa con le fermate della rete e, selezionando una linea, il suo percorso.
- Distanza e indicazioni a piedi verso una fermata, con apertura di Google Maps.
- Biglietti: prenotazione, pagamento sulla pagina Nexi del gestore, borsellino, storico e validazione a bordo tramite il codice vettura.
- Accesso con Google facoltativo, usato solo per indicare chi sta usando l'app sul dispositivo.

## Dati su fermate e linee

Il file `app/src/main/assets/transit.json` è incluso nell'APK e non richiede connessione. Contiene due reti:

| Rete | Fermate | Linee | Corse |
| --- | --- | --- | --- |
| Fiumicino | 411 | 18 | 108 |
| Campobasso | 362 | 33 | 119 |

Le fermate, l'ordine delle fermate, i giorni di servizio e gli orari di partenza provengono dalle pagine ufficiali delle linee invernali ed estive e dai PDF pubblicati dal gestore. Le coordinate delle fermate vengono da OpenStreetMap; quando un nome non è geolocalizzabile la posizione è interpolata lungo il percorso tra le fermate vicine e la fermata è segnalata come approssimativa.

### Orari pubblicati e orari stimati

Campobasso ha gli orari per singola fermata, ricavati dal PDF ufficiale del piano di bacino invernale. Per le corse estive esiste solo l'orario di partenza dal capolinea.

Per Fiumicino il gestore pubblica unicamente gli orari di partenza dal capolinea. Le partenze dal capolinea sono mostrate come ufficiali; gli orari di passaggio alle altre fermate sono calcolati a partire da quelle partenze in base alla distanza progressiva lungo il percorso e sono contrassegnati come stimati sia nella scheda fermata sia nella scheda linea. Non sono orari ufficiali.

Nessun dato è in tempo reale: l'app non conosce la posizione dei mezzi.

## Mappa

La mappa integrata usa osmdroid con le tile di OpenStreetMap e funziona senza chiavi. Se nel build è presente una chiave `MAPS_API_KEY`, l'app usa il Maps SDK di Google al posto di OpenStreetMap. Le indicazioni verso una fermata aprono Google Maps.

## Accesso con Google

L'accesso con Google usa Credential Manager e mostra il selettore degli account. Serve solo a indicare chi usa l'app sul dispositivo: non esiste un backend e il token di identità non viene inviato né salvato. L'account dei biglietti su ticketonbus.trotta.it resta separato.

## Lingue

L'interfaccia è tradotta in inglese (predefinito), italiano, spagnolo, francese, tedesco e rumeno. La lingua si sceglie dalla sezione Info dell'app e viene applicata all'avvio. I testi di errore che arrivano dal sito del gestore restano in italiano.

## Compilazione

Richiede JDK 21 e Android SDK con API 36.

```
JAVA_HOME=/percorso/del/jdk21 ./gradlew :app:assembleDebug
```

APK generato in `app/build/outputs/apk/debug/app-debug.apk`.

Test unitari:

```
JAVA_HOME=/percorso/del/jdk21 ./gradlew :app:testDebugUnitTest
```

### Configurazione

Le due voci seguenti sono facoltative e si mettono in `local.properties`, che non viene versionato:

```
MAPS_API_KEY=chiave del Maps SDK per Android
GOOGLE_WEB_CLIENT_ID=client ID web di un progetto Google Cloud
```

Senza `MAPS_API_KEY` la mappa usa OpenStreetMap. Senza `GOOGLE_WEB_CLIENT_ID` il pulsante di accesso con Google non viene mostrato. Per l'accesso con Google servono, nello stesso progetto Google Cloud, un client OAuth di tipo Android per il pacchetto `it.trotta.ticketonbus` con l'impronta SHA-1 della chiave di firma e un client OAuth di tipo Web, il cui ID va in `GOOGLE_WEB_CLIENT_ID`.

## Struttura

```
app/src/main/java/it/trotta/ticketonbus/
  MainActivity.kt            attività principale, apre i link esterni
  data/                      client del sito, parser HTML, modelli, preferenze
  data/transit/              modello del dataset, logica di orari e distanze, GPS
  ui/                        schermate Compose, view model, mappa, messaggi
app/src/main/assets/         dataset delle reti
app/src/main/res/values*/    stringhe tradotte
app/src/test/                test unitari e pagine HTML catturate dal sito
tools/strings_table.py       genera i file di stringhe da un'unica tabella
```

I file in `app/src/test/resources/fixtures` sono risposte reali catturate da ticketonbus.trotta.it, con i dati personali rimossi. Contengono i commenti usati dal sito come marcatori di inizio e fine elenco: il parser li usa come riferimento, quindi non vanno eliminati.

## Attribuzioni

- Linee, fermate e orari: Trotta Bus Services S.p.A., dalle pagine e dai PDF ufficiali del gestore.
- Coordinate delle fermate: OpenStreetMap contributors, licenza ODbL.
- Indicazioni: Google Maps.
- I marchi Trotta Bus e Ticket on Bus appartengono ai rispettivi titolari e sono citati solo per descrivere il servizio.

## Limiti noti

- Gli orari di passaggio per Fiumicino sono stimati, come descritto sopra.
- La validazione di un biglietto richiede il codice vettura esposto a bordo e viene rifiutata dal sistema se il codice è errato.
- L'app dipende dall'HTML del sito del gestore per l'area clienti e se il sito cambia struttura (IMPROBABILE perche' trotta fa cagare), i parser vanno aggiornati.

import os
import xml.sax.saxutils as sx

RES = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "app", "src", "main", "res")

S = {
    "app_name": ("Trotta Bus (unofficial)", "Trotta Bus (non ufficiale)", "Trotta Bus (no oficial)",
                 "Trotta Bus (non officiel)", "Trotta Bus (inoffiziell)", "Trotta Bus (neoficial)"),
    "app_operator": ("Trotta Bus Services S.p.A.",) * 6,
    "disclaimer_short": (
        "Unofficial app, not affiliated with, endorsed by or connected to Trotta Bus Services S.p.A.",
        "App non ufficiale: non affiliata, approvata o collegata a Trotta Bus Services S.p.A.",
        "App no oficial: no está afiliada, aprobada ni vinculada a Trotta Bus Services S.p.A.",
        "Application non officielle : non affiliée, approuvée ou liée à Trotta Bus Services S.p.A.",
        "Inoffizielle App: nicht mit Trotta Bus Services S.p.A. verbunden und von ihr nicht gebilligt.",
        "Aplicație neoficială: nu este afiliată, aprobată sau legată de Trotta Bus Services S.p.A."),
    "about_title": ("About", "Informazioni", "Información", "À propos", "Über", "Despre"),
    "about_disclaimer_title": ("Not affiliated", "Nessuna affiliazione", "Sin afiliación",
                               "Aucune affiliation", "Keine Verbindung", "Fără afiliere"),
    "about_sources_title": ("Where the data comes from", "Da dove arrivano i dati", "De dónde vienen los datos",
                            "D'où viennent les données", "Woher die Daten kommen", "De unde vin datele"),
    "about_sources_body": (
        "Lines, stops and departure times are read from the official Trotta Bus pages and PDFs when they are "
        "loaded. Stop coordinates come from OpenStreetMap contributors (ODbL), with Google Maps used for "
        "directions. Tickets are issued and validated by Trotta Bus Services S.p.A. on ticketonbus.trotta.it.",
        "Linee, fermate e orari di partenza vengono letti dalle pagine e dai PDF ufficiali Trotta Bus quando "
        "vengono caricati. Le coordinate delle fermate provengono da OpenStreetMap contributors (ODbL), mentre "
        "Google Maps è usato per le indicazioni. I ticket sono emessi e validati da Trotta Bus Services S.p.A. "
        "su ticketonbus.trotta.it.",
        "Las líneas, paradas y horarios de salida se leen de las páginas y PDF oficiales de Trotta Bus al "
        "cargarlos. Las coordenadas de las paradas provienen de OpenStreetMap contributors (ODbL) y Google Maps "
        "se usa para las indicaciones. Los billetes los emite y valida Trotta Bus Services S.p.A. en "
        "ticketonbus.trotta.it.",
        "Les lignes, arrêts et horaires de départ sont lus depuis les pages et PDF officiels Trotta Bus lors de "
        "leur chargement. Les coordonnées des arrêts proviennent d'OpenStreetMap contributors (ODbL), Google Maps "
        "servant aux itinéraires. Les billets sont émis et validés par Trotta Bus Services S.p.A. sur "
        "ticketonbus.trotta.it.",
        "Linien, Haltestellen und Abfahrtszeiten werden beim Laden aus den offiziellen Trotta-Bus-Seiten und "
        "-PDFs gelesen. Haltestellenkoordinaten stammen von OpenStreetMap contributors (ODbL), Google Maps wird "
        "für die Routenführung genutzt. Tickets werden von Trotta Bus Services S.p.A. auf ticketonbus.trotta.it "
        "ausgegeben und entwertet.",
        "Liniile, stațiile și orele de plecare sunt citite din paginile și PDF-urile oficiale Trotta Bus la "
        "încărcare. Coordonatele stațiilor provin de la OpenStreetMap contributors (ODbL), iar Google Maps este "
        "folosit pentru indicații. Biletele sunt emise și validate de Trotta Bus Services S.p.A. pe "
        "ticketonbus.trotta.it."),
    "about_estimates_title": ("Estimated times", "Orari stimati", "Horarios estimados", "Horaires estimés",
                              "Geschätzte Zeiten", "Ore estimate"),
    "about_estimates_body": (
        "For Fiumicino the operator publishes departure times from the terminus only. Passage times shown as "
        "\"estimated\" are calculated from those departures along the route and are not official.",
        "Per Fiumicino il gestore pubblica solo gli orari di partenza dal capolinea. Gli orari indicati come "
        "\"stimati\" sono calcolati da quelle partenze lungo il percorso e non sono ufficiali.",
        "Para Fiumicino el operador publica solo las salidas desde la cabecera. Los horarios marcados como "
        "\"estimados\" se calculan a partir de esas salidas a lo largo del recorrido y no son oficiales.",
        "Pour Fiumicino, l'exploitant ne publie que les départs depuis le terminus. Les horaires indiqués comme "
        "\"estimés\" sont calculés à partir de ces départs le long du parcours et ne sont pas officiels.",
        "Für Fiumicino veröffentlicht der Betreiber nur Abfahrtszeiten ab der Endhaltestelle. Als \"geschätzt\" "
        "markierte Zeiten werden daraus entlang der Strecke berechnet und sind nicht offiziell.",
        "Pentru Fiumicino operatorul publică doar plecările de la capăt. Orele marcate \"estimat\" sunt calculate "
        "din acele plecări de-a lungul traseului și nu sunt oficiale."),
    "about_trademark": (
        "Trotta Bus, Ticket on Bus and the operator's marks belong to their owners and are used here only to "
        "describe the service.",
        "Trotta Bus, Ticket on Bus e i marchi del gestore appartengono ai rispettivi titolari e sono usati qui "
        "solo per descrivere il servizio.",
        "Trotta Bus, Ticket on Bus y las marcas del operador pertenecen a sus titulares y se usan aquí solo para "
        "describir el servicio.",
        "Trotta Bus, Ticket on Bus et les marques de l'exploitant appartiennent à leurs titulaires et ne servent "
        "ici qu'à décrire le service.",
        "Trotta Bus, Ticket on Bus und die Marken des Betreibers gehören ihren Inhabern und dienen hier nur der "
        "Beschreibung des Angebots.",
        "Trotta Bus, Ticket on Bus și mărcile operatorului aparțin deținătorilor lor și sunt folosite aici doar "
        "pentru a descrie serviciul."),
    "about_version": ("Version %1$s", "Versione %1$s", "Versión %1$s", "Version %1$s", "Version %1$s", "Versiunea %1$s"),
    "language_title": ("Language", "Lingua", "Idioma", "Langue", "Sprache", "Limbă"),
    "language_system": ("System default", "Come il sistema", "Como el sistema", "Comme le système",
                        "Wie das System", "Ca sistemul"),

    "login_intro": ("Sign in to buy and validate your tickets.",
                    "Accedi per acquistare e validare i tuoi ticket.",
                    "Inicia sesión para comprar y validar tus billetes.",
                    "Connectez-vous pour acheter et valider vos billets.",
                    "Anmelden, um Tickets zu kaufen und zu entwerten.",
                    "Autentifică-te pentru a cumpăra și valida biletele."),
    "login_service": ("Service", "Servizio", "Servicio", "Service", "Betrieb", "Serviciu"),
    "field_email": ("E-mail",) * 6,
    "field_password": ("Password",) * 6,
    "action_show": ("Show", "Mostra", "Mostrar", "Afficher", "Anzeigen", "Arată"),
    "action_hide": ("Hide", "Nascondi", "Ocultar", "Masquer", "Verbergen", "Ascunde"),
    "action_sign_in": ("Sign in", "Accedi", "Iniciar sesión", "Se connecter", "Anmelden", "Autentificare"),
    "login_privacy": (
        "Credentials are sent only to ticketonbus.trotta.it and kept in memory for the session. "
        "The app never stores your password.",
        "Le credenziali vengono inviate solo a ticketonbus.trotta.it e restano in memoria per la sessione. "
        "L'app non salva la password.",
        "Las credenciales se envían solo a ticketonbus.trotta.it y se mantienen en memoria durante la sesión. "
        "La app no guarda tu contraseña.",
        "Les identifiants sont envoyés uniquement à ticketonbus.trotta.it et gardés en mémoire le temps de la "
        "session. L'app ne stocke jamais votre mot de passe.",
        "Zugangsdaten gehen nur an ticketonbus.trotta.it und bleiben für die Sitzung im Speicher. "
        "Die App speichert Ihr Passwort nicht.",
        "Datele de acces sunt trimise doar către ticketonbus.trotta.it și păstrate în memorie pe durata "
        "sesiunii. Aplicația nu salvează parola."),
    "transit_guest": ("Stops and lines (no sign-in)", "Fermate e linee (senza login)",
                      "Paradas y líneas (sin cuenta)", "Arrêts et lignes (sans compte)",
                      "Haltestellen und Linien (ohne Anmeldung)", "Stații și linii (fără cont)"),

    "home_title": ("Account", "Area riservata", "Cuenta", "Compte", "Konto", "Cont"),
    "home_greeting": ("Hi %1$s", "Ciao %1$s", "Hola %1$s", "Bonjour %1$s", "Hallo %1$s", "Salut %1$s"),
    "stat_active": ("Active", "Attivi", "Activos", "Actifs", "Aktiv", "Active"),
    "stat_to_validate": ("To validate", "Da validare", "Por validar", "À valider", "Zu entwerten", "De validat"),
    "stat_to_pay": ("To pay", "Da pagare", "Por pagar", "À payer", "Zu zahlen", "De plătit"),
    "tile_buy": ("Buy tickets", "Acquista ticket", "Comprar billetes", "Acheter des billets", "Tickets kaufen",
                 "Cumpără bilete"),
    "tile_buy_sub": ("Reserve and pay by card", "Prenota e paga con carta", "Reserva y paga con tarjeta",
                     "Réservez et payez par carte", "Reservieren und mit Karte zahlen", "Rezervă și plătește cu cardul"),
    "tile_wallet": ("Ticket wallet", "Borsellino ticket", "Cartera de billetes", "Portefeuille de billets",
                    "Ticket-Börse", "Portofel de bilete"),
    "tile_cart": ("Ticket cart", "Carrello ticket", "Carrito de billetes", "Panier de billets", "Ticket-Warenkorb",
                  "Coș de bilete"),
    "tile_cart_sub_empty": ("Nothing waiting for payment", "Nessun pagamento in sospeso", "Ningún pago pendiente",
                            "Aucun paiement en attente", "Keine offene Zahlung", "Nicio plată în așteptare"),
    "tile_history": ("Past tickets", "Storico ticket", "Billetes pasados", "Billets passés", "Frühere Tickets",
                     "Bilete anterioare"),
    "tile_transit": ("Stops and lines", "Fermate e linee", "Paradas y líneas", "Arrêts et lignes",
                     "Haltestellen und Linien", "Stații și linii"),
    "tile_transit_sub": ("Nearby stops, times and live map", "Fermate vicine, orari e mappa",
                         "Paradas cercanas, horarios y mapa", "Arrêts proches, horaires et carte",
                         "Haltestellen in der Nähe, Zeiten und Karte", "Stații apropiate, ore și hartă"),
    "section_running_ticket": ("Current ticket", "Ticket in corso", "Billete en curso", "Billet en cours",
                               "Laufendes Ticket", "Bilet în curs"),

    "buy_title": ("Buy tickets", "Acquista ticket", "Comprar billetes", "Acheter des billets", "Tickets kaufen",
                  "Cumpără bilete"),
    "buy_intro": (
        "Choose how many tickets to reserve. Payment is completed on the operator's secure Nexi page.",
        "Scegli quanti ticket vuoi prenotare. Il pagamento si completa sulla pagina sicura Nexi del gestore.",
        "Elige cuántos billetes reservar. El pago se completa en la página segura Nexi del operador.",
        "Choisissez le nombre de billets à réserver. Le paiement se fait sur la page Nexi sécurisée de l'exploitant.",
        "Wählen Sie die Anzahl der Tickets. Die Zahlung erfolgt auf der sicheren Nexi-Seite des Betreibers.",
        "Alege câte bilete rezervi. Plata se face pe pagina securizată Nexi a operatorului."),
    "action_book_pay": ("Reserve and pay", "Prenota e paga", "Reservar y pagar", "Réserver et payer",
                        "Reservieren und zahlen", "Rezervă și plătește"),
    "buy_note": (
        "Buy before boarding and validate within 1 minute of getting on. After paying you can validate it in the "
        "wallet.",
        "Il ticket va acquistato prima di salire sull'autobus e validato entro 1 minuto dalla salita. Dopo il "
        "pagamento potrai attivarlo dal Borsellino.",
        "Compra el billete antes de subir y valídalo en el plazo de 1 minuto. Tras el pago podrás activarlo en la "
        "cartera.",
        "Achetez avant de monter et validez dans la minute suivant la montée. Après paiement, activez-le dans le "
        "portefeuille.",
        "Vor dem Einsteigen kaufen und innerhalb von 1 Minute entwerten. Nach der Zahlung in der Ticket-Börse "
        "entwerten.",
        "Cumpără înainte de urcare și validează în 1 minut. După plată îl poți activa din portofel."),

    "wallet_title": ("Ticket wallet", "Borsellino ticket", "Cartera de billetes", "Portefeuille de billets",
                     "Ticket-Börse", "Portofel de bilete"),
    "wallet_empty": ("No tickets in the wallet yet.", "Non hai ticket nel borsellino.", "No hay billetes en la cartera.",
                     "Aucun billet dans le portefeuille.", "Noch keine Tickets in der Börse.",
                     "Nu ai bilete în portofel."),
    "action_validate_on_bus": ("Validate on the bus", "Valida sul bus", "Validar en el bus", "Valider dans le bus",
                               "Im Bus entwerten", "Validează în autobuz"),
    "action_qr": ("Ticket code / QR", "Codice / QR del ticket", "Código / QR del billete", "Code / QR du billet",
                  "Ticket-Code / QR", "Cod / QR bilet"),
    "dialog_validate_title": ("Validate the ticket", "Valida il ticket", "Validar el billete", "Valider le billet",
                              "Ticket entwerten", "Validează biletul"),
    "dialog_validate_body": (
        "Enter the vehicle code of the bus you are boarding and confirm within 1 minute of getting on.",
        "Inserisci il codice vettura del bus su cui stai salendo e conferma entro 1 minuto dalla salita.",
        "Introduce el código de vehículo del bus al que subes y confirma en el plazo de 1 minuto.",
        "Saisissez le code véhicule du bus que vous montez et confirmez dans la minute.",
        "Geben Sie den Fahrzeugcode des Busses ein und bestätigen Sie innerhalb von 1 Minute.",
        "Introdu codul vehiculului autobuzului în care urci și confirmă în 1 minut."),
    "field_vehicle_code": ("Vehicle code", "Codice vettura", "Código de vehículo", "Code véhicule",
                           "Fahrzeugcode", "Cod vehicul"),
    "dialog_validate_hint": (
        "The vehicle code is displayed on board; ask the driver if you cannot see it. A wrong code is refused by "
        "the system.",
        "Il codice vettura è esposto a bordo; se non lo vedi, chiedilo all'autista. Un codice errato viene "
        "rifiutato dal sistema.",
        "El código de vehículo se muestra a bordo; pídelo al conductor si no lo ves. Un código erróneo es "
        "rechazado por el sistema.",
        "Le code véhicule est affiché à bord ; demandez-le au conducteur sinon. Un code erroné est refusé par le "
        "système.",
        "Der Fahrzeugcode steht im Fahrzeug; fragen Sie sonst die Fahrerin oder den Fahrer. Ein falscher Code "
        "wird abgelehnt.",
        "Codul vehiculului este afișat la bord; cere-l șoferului dacă nu îl vezi. Un cod greșit este respins."),
    "action_validate": ("Validate", "Valida", "Validar", "Valider", "Entwerten", "Validează"),
    "action_cancel": ("Cancel", "Annulla", "Cancelar", "Annuler", "Abbrechen", "Anulează"),
    "label_booking": ("Reserved", "Prenotazione", "Reserva", "Réservation", "Reservierung", "Rezervare"),
    "label_purchase": ("Purchased", "Acquisto", "Compra", "Achat", "Kauf", "Achiziție"),
    "label_validity": ("Validity", "Validità", "Validez", "Validité", "Gültigkeit", "Valabilitate"),
    "label_valid_from": ("Valid from", "Valido dal", "Válido desde", "Valable du", "Gültig ab", "Valabil din"),
    "label_expires": ("Expires", "Scadenza", "Caduca", "Expire le", "Läuft ab", "Expiră"),
    "label_remaining": ("Time left", "Tempo rimasto", "Tiempo restante", "Temps restant", "Restzeit", "Timp rămas"),
    "label_price": ("Price", "Prezzo", "Precio", "Prix", "Preis", "Preț"),
    "label_transaction": ("Transaction", "Transazione", "Transacción", "Transaction", "Transaktion", "Tranzacție"),
    "label_payment_id": ("Payment id", "ID pagamento", "ID de pago", "ID de paiement", "Zahlungs-ID", "ID plată"),
    "validity_minutes": ("%1$d minutes", "%1$d minuti", "%1$d minutos", "%1$d minutes", "%1$d Minuten",
                         "%1$d minute"),
    "expired_label": ("expired", "scaduto", "caducado", "expiré", "abgelaufen", "expirat"),
    "status_not_active": ("Not active", "Non attivo", "No activo", "Non actif", "Nicht entwertet", "Neactiv"),
    "status_active": ("Active", "Attivo", "Activo", "Actif", "Entwertet", "Activ"),
    "status_expired": ("Expired", "Scaduto", "Caducado", "Expiré", "Abgelaufen", "Expirat"),
    "status_pending": ("Awaiting payment", "In attesa di pagamento", "Pendiente de pago", "En attente de paiement",
                       "Zahlung ausstehend", "În așteptarea plății"),
    "status_unknown": ("Unknown", "Sconosciuto", "Desconocido", "Inconnu", "Unbekannt", "Necunoscut"),
    "msg_ticket_validated": ("Ticket validated. Have a nice trip!", "Ticket validato. Buon viaggio!",
                             "Billete validado. ¡Buen viaje!", "Billet validé. Bon voyage !",
                             "Ticket entwertet. Gute Fahrt!", "Bilet validat. Călătorie plăcută!"),
    "msg_ticket_state": ("State updated: %1$s", "Stato aggiornato: %1$s", "Estado actualizado: %1$s",
                         "État mis à jour : %1$s", "Status aktualisiert: %1$s", "Stare actualizată: %1$s"),

    "history_title": ("Past tickets", "Storico ticket", "Billetes pasados", "Billets passés", "Frühere Tickets",
                      "Bilete anterioare"),
    "history_empty": ("No past tickets.", "Nessun ticket scaduto.", "No hay billetes pasados.",
                      "Aucun billet passé.", "Keine früheren Tickets.", "Nu există bilete anterioare."),
    "cart_title": ("Ticket cart", "Carrello ticket", "Carrito de billetes", "Panier de billets", "Ticket-Warenkorb",
                   "Coș de bilete"),
    "cart_empty": ("No reservation waiting for payment.", "Nessuna prenotazione in attesa di pagamento.",
                   "Ninguna reserva pendiente de pago.", "Aucune réservation en attente de paiement.",
                   "Keine Reservierung offen.", "Nicio rezervare în așteptare."),
    "cart_note": ("Until the payment goes through, the tickets cannot be validated.",
                  "Finché il pagamento non va a buon fine i ticket restano non attivabili.",
                  "Hasta que el pago se complete, los billetes no se pueden validar.",
                  "Tant que le paiement n'aboutit pas, les billets ne peuvent pas être validés.",
                  "Bis zur erfolgreichen Zahlung können die Tickets nicht entwertet werden.",
                  "Până la finalizarea plății, biletele nu pot fi validate."),
    "action_pay_nexi": ("Pay with Nexi", "Paga con Nexi", "Pagar con Nexi", "Payer avec Nexi", "Mit Nexi zahlen",
                        "Plătește cu Nexi"),
    "cart_pay_unavailable": ("Payment not available", "Pagamento non disponibile", "Pago no disponible",
                             "Paiement indisponible", "Zahlung nicht verfügbar", "Plată indisponibilă"),
    "cart_reserved_on": ("Reserved on %1$s", "Prenotato il %1$s", "Reservado el %1$s", "Réservé le %1$s",
                         "Reserviert am %1$s", "Rezervat la %1$s"),
    "cart_total": ("Total %1$s", "Totale %1$s", "Total %1$s", "Total %1$s", "Gesamt %1$s", "Total %1$s"),
    "cart_reservation": ("Reservation", "Prenotazione", "Reserva", "Réservation", "Reservierung", "Rezervare"),

    "transit_title": ("Stops and lines", "Fermate e linee", "Paradas y líneas", "Arrêts et lignes",
                      "Haltestellen und Linien", "Stații și linii"),
    "transit_nearby": ("Nearby stops", "Fermate vicine", "Paradas cercanas", "Arrêts proches",
                       "Haltestellen in der Nähe", "Stații apropiate"),
    "transit_locating": ("Finding your position…", "Ricerca della posizione…", "Buscando tu posición…",
                         "Recherche de votre position…", "Position wird gesucht…", "Se caută poziția…"),
    "transit_location_denied": (
        "Location permission denied. Enable it in the system settings to sort stops by distance.",
        "Permesso di localizzazione negato. Attivalo dalle impostazioni per ordinare le fermate per distanza.",
        "Permiso de ubicación denegado. Actívalo en los ajustes para ordenar las paradas por distancia.",
        "Autorisation de localisation refusée. Activez-la dans les réglages pour trier les arrêts par distance.",
        "Standort verweigert. Aktivieren Sie ihn in den Einstellungen, um Haltestellen nach Entfernung zu sortieren.",
        "Permisiune de locație refuzată. Activeaz-o din setări pentru a sorta stațiile după distanță."),
    "transit_location_prompt": (
        "Allow location to see the closest stops, walking distance and next buses.",
        "Consenti la posizione per vedere le fermate più vicine, la distanza a piedi e i prossimi bus.",
        "Permite la ubicación para ver las paradas más cercanas, la distancia a pie y los próximos autobuses.",
        "Autorisez la localisation pour voir les arrêts les plus proches, la distance à pied et les prochains bus.",
        "Standort erlauben, um nächste Haltestellen, Fußweg und nächste Busse zu sehen.",
        "Permite locația pentru a vedea stațiile apropiate, distanța pe jos și următoarele autobuze."),
    "action_use_location": ("Use my location", "Usa la mia posizione", "Usar mi ubicación", "Utiliser ma position",
                            "Meinen Standort verwenden", "Folosește locația mea"),
    "action_see_map": ("See the stops on the map", "Vedi le fermate sulla mappa", "Ver las paradas en el mapa",
                       "Voir les arrêts sur la carte", "Haltestellen auf der Karte", "Vezi stațiile pe hartă"),
    "transit_nearby_empty": ("No stops in the dataset for this network.", "Nessuna fermata nel dataset per questa rete.",
                             "No hay paradas en los datos de esta red.", "Aucun arrêt dans les données de ce réseau.",
                             "Keine Haltestellen in diesem Netz.", "Nu există stații în datele acestei rețele."),
    "action_search_stops": ("Search stops", "Cerca fermate", "Buscar paradas", "Rechercher un arrêt",
                            "Haltestellen suchen", "Caută stații"),
    "action_lines_times": ("Lines and times", "Linee e orari", "Líneas y horarios", "Lignes et horaires",
                           "Linien und Zeiten", "Linii și ore"),
    "stops_title": ("Stops", "Fermate", "Paradas", "Arrêts", "Haltestellen", "Stații"),
    "stops_search_hint": ("Search by name or line (e.g. Focene)", "Cerca per nome o linea (es. Focene)",
                          "Busca por nombre o línea (p. ej. Focene)", "Rechercher par nom ou ligne (ex. Focene)",
                          "Nach Name oder Linie suchen (z. B. Focene)", "Caută după nume sau linie (ex. Focene)"),
    "stop_next": ("Next departures", "Prossimi passaggi", "Próximas salidas", "Prochains passages",
                  "Nächste Abfahrten", "Următoarele plecări"),
    "label_today": ("Today", "Oggi", "Hoy", "Aujourd'hui", "Heute", "Astăzi"),
    "label_tomorrow": ("Tomorrow", "Domani", "Mañana", "Demain", "Morgen", "Mâine"),
    "stops_none_scheduled": ("No service scheduled at this stop.", "Nessuna corsa in programma per questa fermata.",
                             "No hay servicio programado en esta parada.", "Aucun service prévu à cet arrêt.",
                             "Keine Fahrt an dieser Haltestelle geplant.", "Nicio cursă programată la această stație."),
    "transit_estimates_banner": (
        "Times with ~ are calculated from the official departure time at the terminus: the operator does not "
        "publish passage times per stop.",
        "Gli orari con ~ sono calcolati dall'orario ufficiale di partenza dal capolinea: il gestore non pubblica "
        "gli orari di passaggio per singola fermata.",
        "Los horarios con ~ se calculan desde la salida oficial en la cabecera: el operador no publica horarios "
        "por parada.",
        "Les horaires avec ~ sont calculés depuis le départ officiel au terminus : l'exploitant ne publie pas "
        "d'horaires par arrêt.",
        "Zeiten mit ~ werden aus der offiziellen Abfahrt an der Endhaltestelle berechnet: Der Betreiber "
        "veröffentlicht keine Zeiten je Haltestelle.",
        "Orele cu ~ sunt calculate din plecarea oficială de la capăt: operatorul nu publică ore pe stație."),
    "transit_map_reach": ("Map and how to get there", "Mappa e come raggiungerla", "Mapa y cómo llegar",
                          "Carte et accès", "Karte und Anfahrt", "Hartă și cum ajungi"),
    "transit_reach_prompt": ("Turn on location to work out distance and walking directions.",
                             "Attiva la posizione per calcolare distanza e indicazioni a piedi.",
                             "Activa la ubicación para calcular la distancia y las indicaciones a pie.",
                             "Activez la localisation pour calculer la distance et l'itinéraire à pied.",
                             "Standort aktivieren für Entfernung und Fußweg.",
                             "Activează locația pentru distanță și indicații pe jos."),
    "action_directions": ("Directions on Google Maps", "Indicazioni su Google Maps", "Indicaciones en Google Maps",
                          "Itinéraire sur Google Maps", "Route in Google Maps", "Indicații pe Google Maps"),
    "transit_times_per_line": ("Times by line", "Orari per linea", "Horarios por línea", "Horaires par ligne",
                               "Zeiten je Linie", "Ore pe linie"),
    "transit_no_lines_for_stop": ("No line linked to this stop.", "Nessuna linea associata a questa fermata.",
                                  "Ninguna línea asociada a esta parada.", "Aucune ligne liée à cet arrêt.",
                                  "Keine Linie mit dieser Haltestelle verknüpft.", "Nicio linie asociată acestei stații."),
    "chip_estimated": ("estimated", "stimato", "estimado", "estimé", "geschätzt", "estimat"),
    "chip_official": ("official", "ufficiale", "oficial", "officiel", "offiziell", "oficial"),
    "chip_approx": ("Approximate position", "Posizione approssimativa", "Posición aproximada",
                    "Position approximative", "Ungefähre Position", "Poziție aproximativă"),
    "lines_title": ("Lines and times", "Linee e orari", "Líneas y horarios", "Lignes et horaires",
                    "Linien und Zeiten", "Linii și ore"),
    "lines_filter_hint": ("Filter lines", "Filtra linea", "Filtrar líneas", "Filtrer les lignes",
                          "Linien filtern", "Filtrează liniile"),
    "lines_none": ("No line found.", "Nessuna linea trovata.", "No se encontró ninguna línea.",
                   "Aucune ligne trouvée.", "Keine Linie gefunden.", "Nicio linie găsită."),
    "line_from_to": ("From %1$s to %2$s", "Da %1$s a %2$s", "De %1$s a %2$s", "De %1$s à %2$s",
                     "Von %1$s nach %2$s", "De la %1$s la %2$s"),
    "line_summary": ("%1$d timetables · %2$d runs · %3$d stops", "%1$d tabelle · %2$d corse · %3$d fermate",
                     "%1$d tablas · %2$d servicios · %3$d paradas", "%1$d tableaux · %2$d courses · %3$d arrêts",
                     "%1$d Fahrpläne · %2$d Fahrten · %3$d Haltestellen",
                     "%1$d tabele · %2$d curse · %3$d stații"),
    "service_stops_times": ("Stops and times", "Fermate e orari", "Paradas y horarios", "Arrêts et horaires",
                            "Haltestellen und Zeiten", "Stații și ore"),
    "map_title": ("Map", "Mappa", "Mapa", "Carte", "Karte", "Hartă"),
    "map_all_count": ("All (%1$d)", "Tutte (%1$d)", "Todas (%1$d)", "Toutes (%1$d)", "Alle (%1$d)", "Toate (%1$d)"),
    "map_showing_all": ("Showing every stop on the network.", "Mostro tutte le fermate della rete.",
                        "Mostrando todas las paradas de la red.", "Tous les arrêts du réseau sont affichés.",
                        "Alle Haltestellen des Netzes werden angezeigt.", "Afișez toate stațiile din rețea."),
    "action_refresh": ("Refresh", "Aggiorna", "Actualizar", "Actualiser", "Aktualisieren", "Reîncarcă"),
    "action_back": ("Back", "Indietro", "Atrás", "Retour", "Zurück", "Înapoi"),
    "action_map": ("Map", "Mappa", "Mapa", "Carte", "Karte", "Hartă"),
    "countdown_now": ("now", "ora", "ahora", "maintenant", "jetzt", "acum"),
    "countdown_minutes": ("in %1$d min", "tra %1$d min", "en %1$d min", "dans %1$d min", "in %1$d Min.",
                          "în %1$d min"),
    "countdown_hours": ("in %1$dh %2$dm", "tra %1$dh %2$dm", "en %1$dh %2$dm", "dans %1$dh %2$dm",
                        "in %1$dh %2$dm", "în %1$dh %2$dm"),
    "label_tomorrow_lower": ("tomorrow", "domani", "mañana", "demain", "morgen", "mâine"),
    "ticket_unknown": ("Ticket %1$s", "Ticket %1$s", "Billete %1$s", "Billet %1$s", "Ticket %1$s", "Bilet %1$s"),
    "transit_works_offline": (
        "The list of stops and lines is bundled and works without a connection; times come from the operator's "
        "published timetables.",
        "L'elenco di fermate e linee è incluso nell'app e funziona senza connessione; gli orari provengono dai "
        "tabulati pubblicati dal gestore.",
        "La lista de paradas y líneas está incluida y funciona sin conexión; los horarios provienen de los "
        "cuadros publicados por el operador.",
        "La liste des arrêts et lignes est intégrée et fonctionne hors ligne ; les horaires proviennent des "
        "tableaux publiés par l'exploitant.",
        "Die Liste der Haltestellen und Linien ist enthalten und funktioniert offline; Zeiten stammen aus den "
        "veröffentlichten Fahrplänen.",
        "Lista stațiilor și liniilor este inclusă și funcționează offline; orele provin din tabelele publicate."),
    "nav_home": ("Home", "Home", "Inicio", "Accueil", "Start", "Acasă"),
    "nav_transit": ("Stops", "Fermate", "Paradas", "Arrêts", "Haltestellen", "Stații"),
    "nav_tickets": ("Tickets", "Ticket", "Billetes", "Billets", "Tickets", "Bilete"),
    "nav_info": ("Info", "Info", "Info", "Infos", "Info", "Info"),
    "favourites_title": ("Favourite stops", "Fermate preferite", "Paradas favoritas", "Arrêts favoris",
                         "Favoriten", "Stații favorite"),
    "favourites_empty": ("No favourite stops yet. Open a stop and tap the star.",
                         "Nessuna fermata preferita. Apri una fermata e tocca la stella.",
                         "Aún no hay paradas favoritas. Abre una parada y toca la estrella.",
                         "Aucun arrêt favori. Ouvrez un arrêt et touchez l'étoile.",
                         "Noch keine Favoriten. Haltestelle öffnen und Stern antippen.",
                         "Nicio stație favorită. Deschide o stație și atinge steaua."),
    "favourite_add": ("Add to favourites", "Aggiungi ai preferiti", "Añadir a favoritos", "Ajouter aux favoris",
                      "Zu Favoriten hinzufügen", "Adaugă la favorite"),
    "favourite_remove": ("Remove from favourites", "Rimuovi dai preferiti", "Quitar de favoritos",
                         "Retirer des favoris", "Aus Favoriten entfernen", "Elimină din favorite"),
}

S.update({
    "msg_login_missing": ("Enter e-mail and password.", "Inserisci e-mail e password.",
                          "Introduce correo y contraseña.", "Saisissez e-mail et mot de passe.",
                          "E-Mail und Passwort eingeben.", "Introdu e-mailul și parola."),
    "msg_login_failed": ("Sign-in failed.", "Accesso non riuscito.", "No se pudo iniciar sesión.",
                         "Connexion impossible.", "Anmeldung fehlgeschlagen.", "Autentificare eșuată."),
    "msg_network_error": ("Something went wrong. Check your connection and try again.",
                          "Si è verificato un errore di rete. Controlla la connessione e riprova.",
                          "Se ha producido un error de red. Comprueba la conexión e inténtalo de nuevo.",
                          "Une erreur réseau est survenue. Vérifiez votre connexion et réessayez.",
                          "Netzwerkfehler. Verbindung prüfen und erneut versuchen.",
                          "Eroare de rețea. Verifică conexiunea și încearcă din nou."),
    "msg_logged_out": ("Signed out.", "Sessione chiusa.", "Sesión cerrada.", "Déconnecté.",
                       "Abgemeldet.", "Deconectat."),
    "msg_reservation_created": ("Reservation created: complete the payment to activate the tickets.",
                                "Prenotazione creata: completa il pagamento per attivare i ticket.",
                                "Reserva creada: completa el pago para activar los billetes.",
                                "Réservation créée : terminez le paiement pour activer les billets.",
                                "Reservierung erstellt: Zahlung abschließen, um die Tickets zu aktivieren.",
                                "Rezervare creată: finalizează plata pentru a activa biletele."),
    "msg_reservation_failed": ("Reservation failed.", "Prenotazione non riuscita.", "No se pudo reservar.",
                               "Réservation impossible.", "Reservierung fehlgeschlagen.", "Rezervare eșuată."),
    "msg_location_unavailable": ("Position not available. Check that GPS is on.",
                                 "Posizione non disponibile. Controlla il GPS.",
                                 "Ubicación no disponible. Comprueba el GPS.",
                                 "Position indisponible. Vérifiez le GPS.",
                                 "Position nicht verfügbar. GPS prüfen.",
                                 "Poziție indisponibilă. Verifică GPS-ul."),
    "msg_dataset_error": ("Could not read the transit data.", "Impossibile leggere i dati di transito.",
                          "No se pudieron leer los datos de tránsito.", "Impossible de lire les données de transport.",
                          "Verkehrsdaten konnten nicht gelesen werden.", "Datele de transport nu au putut fi citite."),
    "msg_activation_unconfirmed": ("Activation not confirmed by the server. Try again.",
                                   "Attivazione non confermata dal server. Riprova.",
                                   "El servidor no confirmó la activación. Inténtalo de nuevo.",
                                   "Activation non confirmée par le serveur. Réessayez.",
                                   "Aktivierung vom Server nicht bestätigt. Erneut versuchen.",
                                   "Activare neconfirmată de server. Încearcă din nou."),
    "msg_ticket_not_activatable": ("This ticket cannot be validated.", "Ticket non attivabile.",
                                  "Este billete no se puede validar.", "Ce billet ne peut pas être validé.",
                                  "Dieses Ticket kann nicht entwertet werden.", "Acest bilet nu poate fi validat."),
    "msg_vehicle_code_invalid": ("Enter the vehicle code: digits only, 1 to 4.",
                                 "Inserisci il codice vettura: solo cifre, da 1 a 4.",
                                 "Introduce el código de vehículo: solo dígitos, de 1 a 4.",
                                 "Saisissez le code véhicule : chiffres uniquement, de 1 à 4.",
                                 "Fahrzeugcode eingeben: nur Ziffern, 1 bis 4.",
                                 "Introdu codul vehiculului: doar cifre, de la 1 la 4."),
    "msg_ticket_expired": ("Ticket expired.", "Ticket scaduto.", "Billete caducado.", "Billet expiré.",
                           "Ticket abgelaufen.", "Bilet expirat."),
    "msg_nothing_to_pay": ("Nothing to pay.", "Nessun pagamento in sospeso.", "Nada que pagar.",
                           "Rien à payer.", "Keine Zahlung offen.", "Nimic de plătit."),
    "msg_fetch_failed": ("Could not update the data.", "Impossibile aggiornare i dati.",
                         "No se pudieron actualizar los datos.", "Impossible de mettre à jour les données.",
                         "Daten konnten nicht aktualisiert werden.", "Datele nu au putut fi actualizate."),
})

S.update({
    "tenant_fiumicino": ("TPL Fiumicino",) * 6,
    "tenant_campobasso": ("TPL Campobasso",) * 6,
    "day_feriale": ("Weekdays", "Feriali", "Laborables", "Jours ouvrables", "Werktags", "Zile lucrătoare"),
    "day_festivo": ("Sundays and holidays", "Festivi", "Domingos y festivos", "Dimanches et fêtes",
                    "Sonntage und Feiertage", "Duminici și sărbători"),
    "day_scolastico": ("School days", "Scolastico", "Días lectivos", "Jours scolaires", "Schultage",
                       "Zile școlare"),
    "season_annual": ("All year", "Tutto l'anno", "Todo el año", "Toute l'année", "Ganzjährig",
                      "Tot anul"),
    "season_winter": ("Winter", "Invernale", "Invierno", "Hiver", "Winter", "Iarnă"),
    "season_summer": ("Summer", "Estiva", "Verano", "Été", "Sommer", "Vară"),
})

S.update({
    "action_sign_out": ("Sign out", "Esci", "Salir", "Se déconnecter", "Abmelden", "Deconectare"),
    "action_nearby": ("Nearby", "Vicine", "Cercanas", "Proches", "In der Nähe", "Apropiate"),
})

S.update({
    "map_pick_line": ("Pick a line to see its route.", "Scegli una linea per vederne il percorso.",
                      "Elige una línea para ver su recorrido.", "Choisissez une ligne pour voir son tracé.",
                      "Linie wählen, um den Verlauf zu sehen.", "Alege o linie pentru a-i vedea traseul."),
})

S.update({
    "google_sign_in": ("Continue with Google", "Continua con Google", "Continuar con Google",
                       "Continuer avec Google", "Weiter mit Google", "Continuă cu Google"),
    "google_signed_in_as": ("Signed in with Google as %1$s", "Accesso effettuato con Google come %1$s",
                            "Sesión iniciada con Google como %1$s", "Connecté avec Google en tant que %1$s",
                            "Mit Google angemeldet als %1$s", "Autentificat cu Google ca %1$s"),
    "google_not_configured": (
        "Google sign-in is not configured in this build. It needs the web client ID of a Google Cloud "
        "project (GOOGLE_WEB_CLIENT_ID).",
        "L'accesso con Google non è configurato in questa build. Serve il web client ID di un progetto "
        "Google Cloud (GOOGLE_WEB_CLIENT_ID).",
        "El inicio de sesión con Google no está configurado en esta compilación. Necesita el ID de cliente "
        "web de un proyecto de Google Cloud (GOOGLE_WEB_CLIENT_ID).",
        "La connexion Google n'est pas configurée dans cette version. Il faut l'ID client web d'un projet "
        "Google Cloud (GOOGLE_WEB_CLIENT_ID).",
        "Die Google-Anmeldung ist in diesem Build nicht konfiguriert. Dafür ist die Web-Client-ID eines "
        "Google-Cloud-Projekts nötig (GOOGLE_WEB_CLIENT_ID).",
        "Autentificarea cu Google nu este configurată în această versiune. Este nevoie de ID-ul de client "
        "web al unui proiect Google Cloud (GOOGLE_WEB_CLIENT_ID)."),
    "google_no_account": ("No Google account is available on this device.",
                          "Nessun account Google disponibile su questo dispositivo.",
                          "No hay ninguna cuenta de Google en este dispositivo.",
                          "Aucun compte Google n'est disponible sur cet appareil.",
                          "Auf diesem Gerät ist kein Google-Konto verfügbar.",
                          "Nu există niciun cont Google pe acest dispozitiv."),
    "google_sign_in_failed": ("Google sign-in failed. Try again.", "Accesso con Google non riuscito. Riprova.",
                              "No se pudo iniciar sesión con Google. Inténtalo de nuevo.",
                              "La connexion Google a échoué. Réessayez.",
                              "Google-Anmeldung fehlgeschlagen. Erneut versuchen.",
                              "Autentificarea cu Google a eșuat. Încearcă din nou."),
    "msg_google_signed_in": ("Signed in with Google as %1$s.", "Accesso con Google effettuato come %1$s.",
                             "Sesión iniciada con Google como %1$s.", "Connecté avec Google en tant que %1$s.",
                             "Mit Google angemeldet als %1$s.", "Autentificat cu Google ca %1$s."),
    "msg_google_signed_out": ("Google account disconnected.", "Account Google disconnesso.",
                              "Cuenta de Google desconectada.", "Compte Google déconnecté.",
                              "Google-Konto getrennt.", "Cont Google deconectat."),
    "about_google_title": ("Google account", "Account Google", "Cuenta de Google", "Compte Google",
                           "Google-Konto", "Cont Google"),
    "about_google_body": (
        "Signing in with Google is optional and only used to show who is using the app on this device: "
        "there is no server of ours to send the token to. The ticket account on ticketonbus.trotta.it "
        "stays separate.",
        "L'accesso con Google è facoltativo e serve solo a mostrare chi usa l'app su questo dispositivo: "
        "non esiste un nostro server a cui inviare il token. L'account ticket su ticketonbus.trotta.it "
        "resta separato.",
        "Iniciar sesión con Google es opcional y solo sirve para mostrar quién usa la app en este "
        "dispositivo: no hay ningún servidor nuestro al que enviar el token. La cuenta de billetes en "
        "ticketonbus.trotta.it es independiente.",
        "La connexion Google est facultative et sert seulement à indiquer qui utilise l'app sur cet "
        "appareil : nous n'avons aucun serveur où envoyer le jeton. Le compte de billets sur "
        "ticketonbus.trotta.it reste séparé.",
        "Die Google-Anmeldung ist optional und zeigt nur, wer die App auf diesem Gerät nutzt: es gibt "
        "keinen Server von uns, an den das Token ginge. Das Ticket-Konto auf ticketonbus.trotta.it "
        "bleibt getrennt.",
        "Autentificarea cu Google este opțională și arată doar cine folosește aplicația pe acest "
        "dispozitiv: nu avem un server căruia să trimitem tokenul. Contul de bilete de pe "
        "ticketonbus.trotta.it rămâne separat."),
})

P = {
    "n_tickets": (
        ("%1$d ticket", "%1$d tickets"), ("%1$d ticket", "%1$d ticket"), ("%1$d billete", "%1$d billetes"),
        ("%1$d billet", "%1$d billets"), ("%1$d Ticket", "%1$d Tickets"),
        ("%1$d bilet", "%1$d bilete", "%1$d bilete")),
    "n_stops": (
        ("%1$d stop", "%1$d stops"), ("%1$d fermata", "%1$d fermate"), ("%1$d parada", "%1$d paradas"),
        ("%1$d arrêt", "%1$d arrêts"), ("%1$d Haltestelle", "%1$d Haltestellen"),
        ("%1$d stație", "%1$d stații", "%1$d stații")),
    "n_runs": (
        ("%1$d run", "%1$d runs"), ("%1$d corsa", "%1$d corse"), ("%1$d servicio", "%1$d servicios"),
        ("%1$d course", "%1$d courses"), ("%1$d Fahrt", "%1$d Fahrten"),
        ("%1$d cursă", "%1$d curse", "%1$d curse")),
    "walk_minutes": (
        ("%1$d min on foot", "%1$d min on foot"), ("%1$d min a piedi", "%1$d min a piedi"),
        ("%1$d min a pie", "%1$d min a pie"), ("%1$d min à pied", "%1$d min à pied"),
        ("%1$d Min. zu Fuß", "%1$d Min. zu Fuß"), ("%1$d min pe jos", "%1$d min pe jos", "%1$d min pe jos")),
}

LANGS = ["", "-it", "-es", "-fr", "-de", "-ro"]

def esc(text):
    text = text.replace("'", "\\'")
    return sx.escape(text)

def main():
    for idx, suffix in enumerate(LANGS):
        folder = os.path.join(RES, "values" + suffix)
        os.makedirs(folder, exist_ok=True)
        lines = ['<?xml version="1.0" encoding="utf-8"?>',
                 "<resources>"]
        for key, values in S.items():
            lines.append(f'    <string name="{key}">{esc(values[idx])}</string>')
        for key, values in P.items():
            forms = values[idx]
            lines.append(f'    <plurals name="{key}">')
            lines.append(f'        <item quantity="one">{esc(forms[0])}</item>')
            if len(forms) == 3:
                lines.append(f'        <item quantity="few">{esc(forms[1])}</item>')
                lines.append(f'        <item quantity="other">{esc(forms[2])}</item>')
            else:
                lines.append(f'        <item quantity="other">{esc(forms[1])}</item>')
            lines.append("    </plurals>")
        lines.append("</resources>")
        with open(os.path.join(folder, "strings.xml"), "w", encoding="utf-8") as fh:
            fh.write("\n".join(lines) + "\n")
        print(f"values{suffix or ' (default)'}: {len(S)} strings, {len(P)} plurals")

if __name__ == "__main__":
    main()

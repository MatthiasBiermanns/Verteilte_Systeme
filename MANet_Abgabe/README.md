# Verteilte_Systeme

Simulation eines MANets als Prüfungsleistung des Modules Verteilte Systeme im 4.
Semester WWI2020SEA

## Realisierung des MANets-Szenario

- Die generelle Kommunikation wird über die Verwendung des UDP-Protokolls umgesetzt (Verwendung von Ports statt IP-Adressen)
- Jedes physische Gerät (Handy, Tablet, etc) wird im Code durch 2 Threads realisiert: Dem Router und dem EndDevice
  - Der Router übernimmt den am MANet beteiligten Routing Teil
  - Das EndDevice realisiert die "normalen" - hier abgespeckten - Funktionalitäten eines physischen Gerätes
  - Versenden von Nachrichten wird ausgelöst durch ein EndDevice
  - Jedes EndDevice kann nur mit einem, sich zugeordneten Router direkt kommunizieren
  - Router und Device besitzen jeweils einen eigenen Port (EndDevice-Port = Router-Port + 1)
  
---

- Field-Klasse als Verwaltungsklasse
- Stellt auch soz. Gottklasse dar, die physikalische Gegebenheiten realisiert
  (bspw. 10m Funkradius)

## Synchronisation

---

- Zur Synchronisation des betrachteten Feldes werden Semaphoren verwendet

## Routingverfahren

---

### MANet Komplikationen

- Pfade funktionieren in MANets nicht (Zu Instabil)
- Verbindungen sind Redundant
- Periodische Updates zu Kostenintensiv
- sehr dynamische Topologie

### Distance Vector Routing

- Dynamisches Routingverfahren
- Kann Überlastung, Beschädigung o. Ä. umgehen und auslgleichen
- Selbstorganisierend
- Funktionieren mit wenig bis keiner Wartung
- Count-To-Infinity Problem --> Adaption durch Split Horizon und Flooding Updates für MANets sinnvoll

#### Destination-Sequenced Distance-Vector Routing

- Tabellenbasiert
- Updates der Tabellen Periodisch und Inkrementell

### Link-State Protokolle

- Aufbau des gesamten Netzes bei jeden Router
- Austausch über Flooding, nicht nur Nachbarn
- Gut bei viel Veränderung (wie z. B. fragile MANets)

#### [Link Reversal Routing](https://courses.engr.illinois.edu/ece428/sp2018/link-reversal.pdf)

- Hohe Maintaining Kosten
- Adaptiv und Selbststabilisierend
- Routingnetz muss zuvor wie bei Link-State aufgebaut werden
- Jeder Router hat eindeutige ID
- Pro mögliches Ziel muss ein Graph gehalten werden:
  - Knoten Stellen Router dar, Routen werden (wenn auch nicht richtig) als einseitig angesehen
  - Graph muss Azyklisch sein
  - Nur Zielknoten hat keine ausgehenden Routen
- Route Maintaining (full reversal):
  - Bei Ausfall einer Route oder eines Routers wird Graph durch LRA überarbeitet
  - Überprüfung, ob jeder Knoten noch Ausgangskanten besitzt
  - Für alle Knoten ohne Ausgangskanten, werden alle Kanten umgedreht
  - Wiederholung, bis Anforderungen an Graphen wieder erfüllt
  - Gedanke: Algorithmus müsste noch überarbeitet werden, sonst könnten Schleifen entstehen?
- [Route Maintaining (partial reversal)](https://disco.ethz.ch/courses/ws0405/seminar/materials/born_slides.pdf):
  - Wenn Knoten nur noch ausgehende Kanten hat, werden 1 - alle Kanten umgedreht
  - Knoten dreht die Kanten um, welche nicht von ihrem Nachbarn zu ihm umgedreht wurden
  - Wurden alle Kanten zu ihm umgedreht, so werden alle Kanten umgedreht
  - Weniger Umkehrungen notwendig, jedoch größerer Verwaltungsaufwand
- [Routing](https://disco.ethz.ch/courses/ws0405/seminar/materials/born_slides.pdf):
  - Slide 8ff.
  - Router Sendet Packet über alle ausgehenden Routen
  - eventually wird das Packet bei dem Ziel ankommen

### OnDemand Protokolle

#### [Dynamic Source Routing](https://www.vs.inf.ethz.ch/edu/SS2001/MC/slides/02-routing.pdf)

- Suche nach Pfaden nur, wenn gebraucht und nicht vorhanden
- Nur während gebrauch wird Pfad gepflegt / aufrecht gehalten
- Protokoll:
  - Route Discovery anfragender Router
    - _Route Request_ Broadcasten
    - _Request_ hat eindeutige ID
  - Router Discovery passiver Router
    - Erhalt einer _Route Request_
    - Wenn nachricht an Router --> Pfad an Initiator mittel _Route Reply_
    - Wenn _Route Request_ schon gesehen --> Nichts tun
    - Sonst Anhänden der eigenen Adresse an den Pfad in _Route Request_
    - Weiter Broadcasten
  - Route Maintainance
    - Packet wird mit Bit für Hop-Acknoledgement verschickt
    - Jeder Router wartet auf per-Hop Acknoledgement
    - --> Überprüfung ob gegenstation das Packet weiterleitet
    - Bei Problem Senden eines _Route Error_ an ursprünglichen Sender
    - Sender entfernt Link aus Cache
    - Senden über anderen Pfad, oder neue _Route Request_

#### [Terminode Routing](https://www.vs.inf.ethz.ch/edu/SS2001/MC/slides/02-routing.pdf)

- Permanenter End-System Unique Identifier
- Temporärer Location-Dependent Address (LDA)
- Bestehend aus 2 Protokollen:
  - Terminode Local Routing (TLR):
    - Verwendung für Nachbarschaftskommunikation (In lokalem Radius)
    - lokaler Radius definiert durch Hops (Bspw. 2 / für uns evtl. 10m Range)
    - Benötigt nur EUI
    - Nachbarerkennung mittels HELLO-Protokoll /-Nachrichten
  - Terminode Remote Routing (TRR):
    - Verwendung wenn TLR nicht möglich
    - Nutzt 3 Verfahren
    - Bei interesse [Link](https://www.vs.inf.ethz.ch/edu/SS2001/MC/slides/02-routing.pdf) folgen (Slide 14 ff.)

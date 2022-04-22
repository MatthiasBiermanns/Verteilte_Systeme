# Funktion des Dynamic-Source-Routing Programms

## Interne Strukturen

---

- Im Sinne des MANets stellen normale Endgeräte wir u. a. Tablets und Handys die Router dar
- In unserer Umsetzung wird jedes Endgerät durch zwei Objekte simuliert, dem Router und dem EndDevice
  - Der Router übernimmt die Routing-Aufgaben
  - Das EndDevice simuliert die eigentliche Funktion eines Endgerätes (Bei uns nur das verschicken von Nachrichten)
- Router und entsprechendes EndDevice haben immer aufeinanderfolgende Ports <br> --> Router gerade; EndDevice ungerade
- Es wird nur eine Kommunikation zwischen EndDevices beachtet 
  - Router filtern nach Nachrichten, die zu ihrem EndDevice gesendet werden sollen
  - Zu versendende Nachrichten, die keinen EndDevice-Port als Destination Port haben können nicht ankommen
- Eine Retry Message enthält im Content die ID der vorlorenen Message

### RouteError

- Im Content eines routeErrors sind die MessageId der verlorenen Message und der Zielport dieser enthalten
- Die Trennung erfolgt durch ein Blanc
- Aufbau "{destPort} {Message Id}"

### RouteReply Message

- Bei der RouteReply ist der Path nicht in der Reihenfolge in der Nachricht enthalten, in der der Pfad durchlaufen wird <br> --> er entspricht dem Pfad der von der RouteRequest zusammengebaut wurde
- Im Vergleich zur RouteRequest wurden lediglich source- und destination-Router gewechselt

## Setup

---

- Erstellen eines Ordners "DSR_Logs" auf dem Desktop

## Logging

---

- Für jeden Router wird eine Log-Datei im Vereichnis "Desktop/DSR_Logs" erstellt
- Aktuell wird geloggt (Bitte bei Veränderungen erweitern):
  - Jede einkommende Nachricht
  - Routerstatus nach jedem Durchlauf
- Eigene Log-Einträge in diese File können durch folgenden Befehl im Code des Routers hinzugefügt werden:

```java
    this.logger.info("Logging String");
```

## GUI-Simulation

---

- Erstelle eine Testvariation in test.java
- Lass GuiWorker.java in der run()-Methode diese Simulation aufrufen
- Zur Visualisierung starte Gui.java

### Farblegende im GUI
<style>
  c { color : Cyan }
  g { color : Green }
  m { color : Magenta }
  r { color : Red }
  w { color : White }
  b { color : Black}
</style>

Router hat erhalten:  
<c>O</c> Send  
<g>O</g> Forward  
<m>O</m> RouteRequest  
<r>O</r> RouteError  
<w>O</w> Retry  
<b>O</b> Idle  

## Eigene Simulationen erstellen

---

### Field creation:
  - Feldobject mit new Field(Router Anzahl, Länge, Breite)
  - Router hinzufügen mit: 
    - myField.createNewDevice() --> An zufälliger Stelle
    - myField.createNewDevice(x, y) --> An Stelle x, y
  - Router bewegen mit:
    - myField.moveDevice(oldX, oldY) --> Device an Stelle oldX, oldY an zufällige neue
    - myField.moveDevice(oldX, oldY, x, y) --> Bewegt Device an Stelle oldX, oldY an stelle x, y
  - Router löschen mit:
    - myField.deleteDevice(x, y) --> Löscht Device an Stelle x, y

### Nachrichten Senden:
  - Device bzw. EndDevice (Immer ungerader Port) aus Map oder 3-Dim Array herausziehen
  - EndDevice.sendMessage(destPort, message) --> Sendet den String message an das EndDevice mit destPort <br> --> destPort muss auch ein EndDevice(ungerader Port) sein

### Ereknnung von Netzzusammengehörigkeit:
  1. ohne Bewegung
    - Ein Router ist nicht teil des Netzes, wenn eine zu seinem EndDevice abgeschickte Nachricht nicht ankommt
    - d. h. in unserer Simulation kann kein Pfad gefunden werden
    - Erkennbar daran, dass sourceRouter nie eine RouteReply für dieses Router erhält (Einzusehen in Log)
  2. mit Bewegung
    - Netz kann ggf. nicht aufgebaut werden 
    - Erkennbar an:
      - Ständige RouteError im SourceRouter --> Nachricht kommt nie ganz durch <br> --> gehen ggf. auch durch viele Bewegung verloren
      - Keine Route kann zum Router gefunden werden <br> --> erkennbar, dass nie eine Route dafür geloggt wird
      - Sicher Erkennbar durch Zielrouter <br> --> Nachricht kommt nie beim Router an (siehe Logging)
# Funktion des Dynamic-Source-Routing Programms

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
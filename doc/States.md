Das ist ein hervorragender Ansatz! Eine solche Zustandstabelle (State-Transition-Table) ist genau das richtige Werkzeug, um die Interaktionslogik deines FMC-Editors präzise abzubilden.

Aus deinem Quellcode lässt sich entnehmen, dass die Übergänge in zwei Bereichen definiert sind:

1. **Zustandsebene (`EditorState`):** Gesteuert über Mausklicks und Drag&Drop im Canvas (definiert in den Klassen im `state`-Paket).
2. **Globale Ebene (`MainController`):** Gesteuert über Tastatur-Shortcuts (Accelerators).

Hier ist die detaillierte Zustandstabelle basierend auf deiner Software-Architektur:

### Zustandstabelle (State Transition Table)

| Startzustand (`startState`) | Tastatur (Shortcuts / Keys) | Maus (Klicks / Aktionen) | Endzustand (`endState`) | Beschreibung / Ausgelöste Aktion |
| --- | --- | --- | --- | --- |
| **IdleState** | *Keine* | Primäre Taste, Doppel-Klick, auf einem Objekt | **ResizeState** | Wechselt in den Modus zur Größenänderung des Objekts. |
| **IdleState** | STRG (gedrückt halten) | Primäre Taste, Klick (1x), ins Leere | **BoxSelectionState** | Startet die Rahmenauswahl (Gummiband-Selektion). |
| **IdleState** | *Keine* | Primäre Taste, Klick (1x), auf einem Objekt | **DragObjectsState** | Wählt das Objekt aus (bzw. erweitert/ändert Selektion) und bereitet das Verschieben vor. |
| **IdleState** | *Keine* | Primäre Taste, Klick (1x), ins Leere | **PanningState** | Leert die Selektion und startet das Verschieben der gesamten Canvas-Ansicht. |
| **IdleState** | *Keine* | Primäre Taste, Doppel-Klick, auf einer Verbindungslinie | **IdleState** *(bleibt)* | Fügt an der geklickten Stelle einen neuen Wegpunkt (`WEGPUNKT`) in die Verbindung ein. |
| **IdleState** | *Keine* | Primäre Taste, Klick (1x), auf einer Verbindungslinie | **IdleState** *(bleibt)* | Blendet die Wegpunkte-Ebene ein. |
| **CreateState** *(Circle/Rect)* | *Keine* | Primäre Taste, Klick (1x), ins Leere | **CreateState** *(bleibt)* | Platziert ein neues Objekt (Kreis/Quadrat) im Canvas und reaktiviert das Tool. |
| **DragObjectsState** | *Keine* | Primäre Taste, Maus bewegt (Dragged) | **DragObjectsState** *(bleibt)* | Verschiebt alle ausgewählten Objekte (ggf. mit Raster-Einrastung / Snap-to-Grid). |
| **DragObjectsState** | *Keine* | Primäre Taste losgelassen (Released) | **CreateState** / **CreateConnectionState** / **IdleState** *(Aktives Tool)* | Schließt das Verschieben ab, fügt den `MoveMultipleObjectsCommand` zur History hinzu und stellt das zuvor aktive Werkzeug wieder her. |
| **ResizeState** | *Keine* | Primäre Taste, Maus bewegt (Dragged) auf Handle | **ResizeState** *(bleibt)* | Ändert die Breite/Höhe des Objekts basierend auf dem aktiven Anfasspunkt (Handle). |
| **ResizeState** | *Keine* | Primäre Taste losgelassen (Released) | **ResizeState** *(bleibt)* | Beendet das Vergrößern/Verkleinern und speichert die Änderung über ein Command. |
| **ResizeState** | *Keine* | Primäre Taste, Klick, auf ein anderes Objekt | **ResizeState** *(neues Ziel)* | Wechselt den Fokus der Größenänderung direkt auf das andere Objekt. |
| **ResizeState** | *Keine* | Primäre Taste, Klick, ins Leere / gleiches Objekt | **CreateState** / **CreateConnectionState** / **IdleState** *(Aktives Tool)* | Bricht den Resize-Modus ab und stellt das Standard-Tool wieder her. |
| **BoxSelectionState** | *Keine* | Primäre Taste, Maus bewegt (Dragged) | **BoxSelectionState** *(bleibt)* | Aktualisiert die Dimensionen des gezeichneten Auswahlrechtecks im UI. |
| **BoxSelectionState** | *Keine* | Primäre Taste losgelassen (Released) | **IdleState** | Fügt alle Objekte innerhalb des Rahmens zur Selektion hinzu und kehrt zu Idle zurück. |
| **PanningState** | *Keine* | Primäre Taste, Maus bewegt (Dragged) | **PanningState** *(bleibt)* | Verschiebt die Kamera (`world`-Gruppe) anhand der Mausbewegung (Szenen-Koordinaten). |
| **PanningState** | *Keine* | Primäre Taste losgelassen (Released) | **CreateState** / **CreateConnectionState** / **IdleState** *(Aktives Tool)* | Beendet das Kameraschwenken und stellt das aktive Werkzeug wieder her. |
| **CreateConnectionState** | *Keine* | Primäre Taste, Klick (1x), auf Objekt (wenn noch kein Startobjekt gewählt) | **CreateConnectionState** *(Start fixiert)* | Registriert das getroffene Objekt als `sourceObjectId` (Verbindungsausgang). |
| **CreateConnectionState** | *Keine* | Primäre Taste, Klick (1x), ins Leere (wenn Startobjekt bereits gewählt) | **CreateConnectionState** *(Wegpunkt hinzugefügt)* | Erstellt dynamisch einen neuen Zwischen-Wegpunkt im Raum. |
| **CreateConnectionState** | *Keine* | Primäre Taste, Klick (1x), auf ein anderes Objekt (wenn Startobjekt gewählt) | **CreateState** / **CreateConnectionState** / **IdleState** *(Aktives Tool)* | Erstellt die finale Verbindung via `CreateConnectionCommand`, leert temporäre Daten und reaktiviert das Tool. |

---

### Globale Tastatur-Zustandsänderungen (Shortcuts)

Diese Übergänge sind im `MainController` registriert und können aus *jedem* Zustand heraus getriggert werden, da sie als globale Accelerator-Shortcuts auf der JavaFX-Scene liegen:

| Zustand | Tastatur (Shortcut) | Maus | Folge-Aktion / Zustandsauswirkung |
| --- | --- | --- | --- |
| **Jeder Zustand** | `STRG + Z` | *Keine* | Führt im `CommandHistory` ein **Undo** (Rückgängig) aus. |
| **Jeder Zustand** | `STRG + SHIFT + Z` | *Keine* | Führt im `CommandHistory` ein **Redo** (Wiederholen) aus. |
| **Jeder Zustand** | `ENTF` (DELETE) | *Keine* | Löscht alle aktuell ausgewählten Objekte über ein Command. |
| **Jeder Zustand** | `RÜCKTASTE` (BACKSPACE) | *Keine* | Gleiche Funktion wie `DELETE` (Löschen der Auswahl). |
| **Jeder Zustand** | *Keine* | Mittleres Mausrad (Scrollen) | Löst das Zoom-Event auf dem Canvas aus (`drawingPane.handleZoom`). |

### Besonderheit deines Codes (Das "Reactivate"-Muster):

Wie man im Code sieht (z.B. in `CanvasController.reactivateCurrentTool()`), merkt sich die Applikation das in der Toolbar ausgewählte Werkzeug (`SELECT`, `CIRCLE_CREATE`, etc.). Sobald temporäre Zustände wie `DragObjectsState`, `ResizeState` oder `PanningState` durch das Loslassen der Maustaste beendet werden, springt das System automatisch wieder in den Zustand zurück, der dem aktuellen Toolbar-Werkzeug entspricht (entweder `IdleState`, `CreateState` oder `CreateConnectionState`).

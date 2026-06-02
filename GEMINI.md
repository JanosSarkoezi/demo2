Das ist ein extrem kluger Ansatz. Wenn man ein solches Projekt in Etappen baut,
neigt man oft dazu, in frühen Phasen Abkürzungen zu nehmen, die einem später
das Genick brechen. Oder man baut zu früh zu viel Abstraktion ein
(*Overengineering*), was den Code unleserlich macht.

Um das zu verhindern, können wir das Dokument in eine **evolutionäre Roadmap**
umschreiben. Jede Etappe liefert ein lauffähiges Produkt (*Increment*) und
führt Patterns erst dann ein, wenn sie wirklich gebraucht werden.

Zusätzlich bringen wir hier noch zwei weitere Patterns ins Spiel, die dir
helfen, den Code sauber zu halten:

1. **Factory Pattern (Etappe 1):** Kapselt die Erstellung von Objekten, damit
   die Registry nicht wissen muss, wie ein `FmcObject` initialisiert wird.
2. **Observer Pattern (Etappe 2):** Löst das Problem der UI-Synchronisation,
   ohne dass die Registry JavaFX-Klassen kennen muss.

Hier ist der Entwurf für deine überarbeitete `gemini.md`, aufgeteilt in **4
klare, aufeinander aufbauende Etappen**.

---

# FMC Editor Projekt-Richtlinien & Evolutions-Roadmap

Dieses Dokument beschreibt die Kernarchitektur und den schrittweisen
(etappenweisen) Aufbau des FMC-Editors. Um Code-Verkrautung zu verhindern, wird
das System inkrementell entwickelt. Jede Etappe fügt Komplexität erst dann
hinzu, wenn das Fundament stabil und getestet ist.

---

## Etappe 1: Das nackte Datenmodell & CRUD (Klarheit zuerst)

**Ziel:** Ein rein im Speicher testbares Datenmodell ohne UI, ohne Undo/Redo
und ohne Layer. Fokus liegt auf der Kern-Logik.

### Architektur-Komponenten
- **FmcObject**: Datenklasse mit `UUID id`, `FmcType type` (KREIS, QUADRAT),
  `double x`, `double y`. Keine JavaFX-Imports.
- **Connection**: Hält `sourceId` und `targetId`.
- **CoreRegistry**: Hält einfache Maps (`Map<UUID, FmcObject>`, `Map<UUID,
  Connection>`).
- **FmcFactory (Neu)**: Eine Fabrik-Klasse zur Erstellung von Objekten. Das
  verhindert, dass Erstellungs-Logik (wie das Generieren von UUIDs oder
  Standard-Größen) die Registry oder spätere Controller verschmutzt.

### Konventionen in Etappe 1
- Modifikationen an der Registry geschehen hier noch über direkte Methoden
  (`addObject()`, `removeObject()`).
- **Vorteil:** Du kannst die komplette Geschäftslogik und die *Bipartite
  Validierung* in JUnit-Tests schreiben, ohne dich mit UI oder Command-Stacks
  herumschlagen zu müssen.

---

## Etappe 2: Die visuelle Darstellung & Entkopplung (Observer Pattern)

**Ziel:** Das Datenmodell auf den Bildschirm bringen, ohne die strikte Trennung
zu verletzen.

### Das Observer Pattern für UI-Synchronisation
Um zu verhindern, dass die `CoreRegistry` JavaFX-Klassen kennt, führt sie ein
einfaches Event-System ein:
- **RegistryListener Interface**:
  ```java
  public interface RegistryListener {
      void onObjectAdded(FmcObject obj);
      void onObjectRemoved(UUID id);
      void onObjectMoved(UUID id, double newX, double newY);
  }

  ```

* Die `CoreRegistry` erlaubt es, Listener zu registrieren und benachrichtigt
  diese bei Änderungen.

### Der ViewMapper (UI-Schicht)

* Der `ViewMapper` implementiert diesen `RegistryListener`.
* Sobald `onObjectAdded` feuert, erzeugt der `ViewMapper` die entsprechende
  JavaFX-Node (z.B. ein `Circle`- oder `Rectangle`-Objekt) und fügt sie der
  JavaFX-Scene hinzu.
* Die JavaFX-Nodes speichern in ihren `properties` **ausschließlich** die UUID
  des Datenmodells.

---

## Etappe 3: Interaktion & Zustandsschutz (Command & State Pattern)

**Ziel:** Benutzerinteraktion (Mausklicks, Drag&Drop) und die Absicherung der
Registry gegen direkte Zugriffe. Einführung von Undo/Redo.

### Umstellung auf das Command Pattern

Direkte Aufrufe an der `CoreRegistry` werden für die UI nun **verboten**. Jede
Aktion wird ein Befehl.

* UI-Controller fangen Maus-Events ab und übergeben sie an den aktuellen
  `EditorState` (z.B. `SelectState`, `CreateConnectionState`).
* Der State erzeugt ein `Command` (z.B. `MoveObjectCommand`) und übergibt es an
  die `CommandHistory`.

### Schutz vor Verkrautung beim Drag&Drop (Zusammenfassung)

* Während des Ziehens mit der Maus (`MouseDragged`) wird kein finalen Command
  auf den Stack gelegt.
* Das `MoveObjectCommand` wird beim `MousePressed` initialisiert, updatet die
  Position kontinuierlich im Speicher/UI, und wird erst beim `MouseReleased`
  (wenn der Drag fertig ist) fest in die `CommandHistory` eingetragen. Das hält
  den Undo-Stack sauber.

---

## Etappe 4: Strukturierung & Layout (Layer-System & Strategy Pattern)

**Ziel:** Erweiterung des Editors um komplexe Features, nachdem die Basis
(Modell -> Command -> UI-Event) felsenfest steht.

### Das Layer-System

* `FmcObject` wird um eine `layerId` erweitert.
* Die `CoreRegistry` verwaltet nun eine Liste von Layern.
* Der `ViewMapper` (aus Etappe 2) lauscht nun auch auf Layer-Events. Wenn ein
  Layer auf `visible = false` gesetzt wird, blendet der `ViewMapper` die
  zugehörigen JavaFX-Nodes aus.

### Das Strategy Pattern für Verbindungen

* Da das Zeichnen von Linien (direkt vs. orthogonal) nun komplexer wird, wird
  die Zeichenlogik aus dem `ViewMapper` in eigene Klassen ausgelagert.
* **RoutingStrategy Interface**:
```java
public interface RoutingStrategy {
    Path calculatePath(FmcObject source, FmcObject target, List<Point2D> waypoints);
}

```


* Der `ViewMapper` nutzt die gewählte Strategie, um die Linien flexibel zu rendern.

---

## Goldene Regeln gegen Code-Verkrautung

1. **Einbahnstraßen-Kommunikation**: Datenmodell -> (über Listener) -> UI.
   Niemals umgekehrt. Die UI greift niemals direkt in die Daten, sondern
   schickt Commands.
2. **Keine Gott-Klassen**: Wenn eine Klasse (z.B. der `ViewMapper`) zu groß
   wird, wird sie aufgeteilt (z.B. in `NodeBinder` für Objekte und `EdgeBinder`
   für Verbindungen).
3. **Erst Test, dann UI**: Jedes Feature in Etappe 1 und 3 muss sich ohne
   gestartete JavaFX-Anwendung in einem Unit-Test ausführen lassen. Wenn das
   klappt, ist die Architektur sauber.

```

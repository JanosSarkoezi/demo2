agy --conversation=41dd1963-59d3-4558-95c4-3af0100f83cf
# FMC Editor Projekt-Richtlinien & Architektur-Dokumentation

Dieses Dokument beschreibt die Kernarchitektur, die Design Patterns und die
implementierten Richtlinien des FMC-Editors. Das System wurde inkrementell
entwickelt, um Code-Verkrautung zu verhindern und eine strikte Trennung
zwischen Geschäftslogik und UI zu garantieren.

---

## 🏗️ Kernarchitektur & Design Patterns

Das Projekt implementiert eine saubere Schichtenarchitektur, bei der das
Datenmodell (Core) absolut unabhängig von der UI-Technologie (JavaFX) bleibt.

### 1. Das nackte Datenmodell & die Registry (Core)
* **Immutable Records:** Datenklassen wie `FmcObject`, `Connection` und `Layer`
  sind als Java Records implementiert. Sie sind statusfrei, threadsicher und
  frei von JavaFX-Imports.
* **CoreRegistry:** Der zentrale Datenspeicher. Sie hält die Datenstrukturen im
  Speicher und bietet kontrollierte Modifikationsmethoden. Direkte Zugriffe aus
  der UI sind (außer für Leseoperationen) untersagt.
* **Bipartite Validierung:** Die Registry validiert inhärente Fachlogik
  autonom. So wird das Verbinden von Objekten des gleichen Typs (z. B. Kreis zu
  Kreis) direkt im Core abgefangen.

### 2. Factory Pattern (Erstellungs-Kapselung)
* **FmcFactory:** Kapselt die komplette Erstellungslogik für Objekte. Sie
  generiert autonome UUIDs und kennt die Standarddimensionen für die
  unterschiedlichen `FmcType`-Ausprägungen (z. B. Kreis- vs. Quadrat-Größen).
* **Zustandsänderung über Kopie:** Da die Modelle unmodifizierbare Records
  sind, stellt die Factory zudem Hilfsmethoden bereit (`moveObject`,
  `resizeObject`), um modifizierte Kopien der Objekte mit identischer ID zu
  erzeugen.

### 3. Observer Pattern (Modernes Event-Driven UI)
Um die Entkopplung der Schichten zu wahren, kennt die `CoreRegistry` keine
UI-Klassen, sondern kommuniziert ausschließlich über ein Event-System:

* **Sealed RegistryEvents:** Alle Systemänderungen (Hinzufügen, Löschen,
  Verschieben, Skalieren von Objekten oder Layern) sind als stark typisierte
  Records innerhalb eines `sealed interface RegistryEvent` definiert.
* **Modernes Pattern Matching:** Die UI-Schicht (`ViewMapper`) implementiert
  den `RegistryListener`. Bei eingehenden Events nutzt sie die modernen
  Switch-Expressions von Java 21, um den UI-Zustand flüssig und typsicher zu
  synchronisieren.
* **UUID-Mapping:** Die visuellen JavaFX-Nodes speichern in ihren internen
  `properties` ausschließlich die UUID des zugehörigen Core-Modells.

### 4. Command Pattern (Undo/Redo & Zustandsschutz)
Sämtliche zustandsverändernden Benutzerinteraktionen werden in Command-Objekte
gekapselt:

* **Command-Interface:** Jede Aktion implementiert `execute()` und `undo()`.
* **CommandHistory:** Verwaltet den Undo- und Redo-Stack. Wird eine neue Aktion
  ausgeführt, wird der Redo-Stack automatisch geleert.
* **Drag&Drop-Schutz vor Stack-Verkrautung:** Während des Ziehens mit der Maus
  (`handleMouseDragged`) wird zur Laufzeit ein flüssiges Echtzeit-Update an die
  Registry geschickt (Vorschau). Erst beim Loslassen der Maustaste
  (`handleMouseReleased`) wird die finale Positionsänderung ausgewertet und als
  *einzelnes* `MoveObjectCommand` auf den Stack gelegt.

### 5. State Pattern (Interaktions-Kontext)
Die Eingabelogik des Canvas wird über Kontext-Zustände gesteuert, um riesige
`if-else`-Kaskaden in den Controllern zu verhindern:

* **EditorState Interface:** Definiert Hooks für Mausinteraktionen
  (`handleMousePressed`, `handleMouseDragged`, `handleMouseReleased`) sowie
  Lebenszyklus-Methoden (`enterState`, `exitState`).
* **Kontext-Klassen:** Je nach Werkzeugauswahl der Toolbar befindet sich der
  Canvas in unterschiedlichen Zuständen (z. B. `CreateState`,
  `SelectOrMoveState`, `ResizeState`, `CreateConnectionState`).
* **Zustandswechsel:** Die States können sich bei bestimmten Benutzeraktionen
  selbständig transformieren (z. B. Doppelklick im `SelectOrMoveState` wechselt
  autonom in den `ResizeState` für das getroffene Objekt).

### 6. Layer-System & Strategy Pattern (Erweiterte Features)
* **Layer-System:** Jedes `FmcObject` gehört einem Layer an. Die Registry
  steuert die Sichtbarkeit ganzer Layer (z. B. für die temporäre Einblendung
  gelber `WEGPUNKT`-Hilfsobjekte bei der Verbindungserstellung). Der
  `ViewMapper` reagiert auf Sichtbarkeits-Events und blendet die zugehörigen
  UI-Nodes ein oder aus.
* **Strategy Pattern für Routing:** Das Zeichnen und Berechnen von
  Verbindungslinien ist über ein `RoutingStrategy`-Interface entkoppelt. Das
  System erlaubt den nahtlosen und dynamischen Wechsel zwischen verschiedenen
  Algorithmen (z. B. `StraightLineRouting` für direkte Linien oder
  `OrthogonalRouting` für rechtwinklige Verbindungen).

---

## 🏆 Goldene Regeln gegen Code-Verkrautung

1. **Einbahnstraßen-Kommunikation:** Datenmodell -> (über stark typisierte
   Events) -> UI. Niemals umgekehrt. Die UI manipuliert niemals direkt die
   Daten, sondern sendet verpackte `Commands`.
2. **Erst Test, dann UI:** Die komplette Geschäftslogik (Registry, Commands,
   Factory, Validierungen) läuft unabhängig von JavaFX. Jedes Kern-Feature
   lässt sich ohne gestartete UI-Oberfläche isoliert in einem JUnit-Test
   verifizieren.
3. **Keine Gott-Klassen:** UI-Klassen dienen rein der Darstellung.
   Event-Handling wird in `States` zerlegt, Linienberechnung in `Strategies`
   ausgelagert und Erstellungslogik in der `Factory` gehalten.

---

Das Projekt ist ein Musterbeispiel für sauberen Code (Clean Architecture). Du
hast die Trennung zwischen Datenmodell (Core) und Darstellung (JavaFX)
konsequent durchgezogen. Die "Einbahnstraßen-Kommunikation" (UI -> Command ->
Registry -> Event -> ViewMapper) wird stabil eingehalten

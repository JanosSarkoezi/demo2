  Hier ist der detaillierte Überblick über das Zusammenspiel zwischen der Toolbar, dem CanvasController und den verschiedenen EditorStates bei Benutzerinteraktionen wie Klicks,
  Doppelklicks und Drag-and-Drop.
  ──────
  ### 1. Das Zusammenspiel zwischen Toolbar und Zustand

  Die Buttons der Toolbar steuern primär den Einstiegszustand des Editors, indem sie das Feld  currentState  im CanvasController.java setzen.

  Wenn in der Toolbar kein spezifisches Erstellungswerkzeug aktiv ist, befindet sich der Editor im  IdleState  (Standardmodus).
  ──────
  ### 2. Die Zustände und ihre Übergänge im Detail

  #### A. IdleState (Standard-Zustand)

  Der  IdleState  ist der Ausgangspunkt für Selektion, Verschieben, Skalieren und Panning.

  • Einfachklick auf ein Objekt:
      • Wählt das Objekt aus (mit gedrückter  STRG/CTRL -Taste wird die Auswahl erweitert/reduziert, andernfalls wird die vorherige Auswahl geleert).
      • Wechselt bei gedrückter Maustaste sofort in den  DragObjectsState , um ein Verschieben vorzubereiten.
  • Doppelklick auf ein Objekt:
      • Wechselt in den  ResizeState  für dieses Objekt. Die UI zeichnet nun Anfasser (Handles) um die Box/den Kreis.
  • Einfachklick auf eine Verbindungslinie:
      • Blendet temporär den Wegpunkt-Layer (Layer ID  1 ) ein, damit eventuell vorhandene Wegpunkte sichtbar und editierbar werden.
  • Doppelklick auf eine Verbindungslinie:
      • Erstellt an der Klick-Position sofort einen neuen Wegpunkt (Typ  FmcType.WEGPUNKT ) auf der Verbindung via  AddWaypointCommand . Dieser Wegpunkt wird direkt selektiert.
  • Klick ins Leere:
      • Ohne STRG: Löscht alle aktiven Selektionen und wechselt in den  PanningState  (Verschieben des sichtbaren Canvas-Ausschnitts beim Ziehen).
      • Mit STRG: Startet eine Rechteck-Auswahl und wechselt in den  BoxSelectionState  (Gummiband-Rahmen).

  ──────
  #### B. CreateState (Objekt-Erstellung)

  Aktiviert, wenn in der Toolbar der Kreis- oder Rechteck-Button gedrückt ist.

  • Mausklick auf dem Canvas:
      • Erzeugt an der Klickstelle ein Objekt des ausgewählten Typs ( CreateObjectCommand ).
      • Sticky-Logik der Toolbar:
          • Ist die Checkbox "Sticky" in der Toolbar aktiv, bleibt der Editor im  CreateState  – du kannst direkt weitere Objekte platzieren.
          • Ist "Sticky" deaktiviert, wechselt der Editor nach der Erstellung des ersten Objekts automatisch zurück in den  IdleState  und der Toolbar-Button wird wieder abgewählt.


  ──────
  #### C. CreateConnectionState (Verbindung erstellen)

  Aktiviert, wenn in der Toolbar der Verbindungs-Button ("Connect") gedrückt ist.

  • Maus gedrückt halten auf Objekt A, ziehen zu Objekt B und loslassen (Drag and Drop):
      • Während des Ziehens wird eine temporäre Linie gezeichnet.
      • Beim Loslassen über Objekt B wird geprüft, ob die Verbindung zulässig ist (z. B. keine Kreis-zu-Kreis-Verbindung im Kern-Modell).
      • Ist sie zulässig, wird das  CreateConnectionCommand  ausgeführt.
      • Je nach "Sticky"-Checkbox verbleibt das System im Verbindungs-Modus oder springt zurück in den  IdleState .

  ──────
  #### D. DragObjectsState (Verschieben per Drag & Drop)

  Wird aktiviert, wenn im  IdleState  auf ein selektiertes Objekt gedrückt und die Maus gezogen wird.

  • Mouse Dragged (Ziehen):
      • Aktualisiert die Position der verschobenen Objekte in Echtzeit in der  CoreRegistry .
      • Toolbar-Einfluss: Hierbei wird geprüft, ob die Checkbox "Snap to Grid" aktiv ist. Falls ja, werden die Koordinaten an das Gitternetz angepasst.
      • Wichtig gegen Stack-Verstopfung: Während des Ziehens werden keine Commands auf den Undo-Stack gelegt (reines Preview-Update).
  • Mouse Released (Loslassen):
      • Führt das finale  MoveObjectCommand  (oder bei mehreren Objekten  MoveMultipleObjectsCommand ) aus, wodurch die Bewegung finalisiert und ein Undo/Redo-Punkt erzeugt wird.
      • Wechselt zurück in den  IdleState .

  ──────
  #### E. ResizeState (Skalieren / Größenänderung)

  Wird durch einen Doppelklick auf ein Objekt im  IdleState  betreten.

  • Ziehen an einem Handle (Anfasser):
      • Verändert temporär die Ausdehnung des Objekts in der Vorschau.
  • Maus loslassen:
      • Führt das finale  ResizeObjectCommand  aus und speichert es in der History.
  • Klick ins Leere:
      • Verlässt den Skalierungsmodus und wechselt zurück in den  IdleState  (die Skalierungs-Handles werden ausgeblendet).

  ──────
  ### Zusammenfassung der Übergänge (Zustandsautomat)

    graph TD
        IdleState -- "Toolbar Click (Circle/Rect)" --> CreateState
        IdleState -- "Toolbar Click (Connect)" --> CreateConnectionState
        IdleState -- "Double Click Object" --> ResizeState
        IdleState -- "Mouse Drag Object" --> DragObjectsState
        IdleState -- "Mouse Drag Canvas (no Ctrl)" --> PanningState
        IdleState -- "Mouse Drag Canvas (Ctrl)" --> BoxSelectionState

        CreateState -- "Click & !Sticky" --> IdleState
        CreateConnectionState -- "Release on target & !Sticky" --> IdleState
        DragObjectsState -- "Mouse Released" --> IdleState
        ResizeState -- "Click Workspace" --> IdleState
        PanningState -- "Mouse Released" --> IdleState
        BoxSelectionState -- "Mouse Released" --> IdleState

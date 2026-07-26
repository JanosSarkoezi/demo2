> From: https://gemini.google.com/app/bda3507be739a84f?hl=de

# you asked

message time: 2026-08-04 19:24:10

Obwohl der Core JavaFX-frei ist, sind viele States direkt an CanvasController und ViewMapper gebunden. Wie kann man das loesen, verbessern?

---

# gemini response

Um die direkte Kopplung der **States** an den UI-Controller (`CanvasController`) und den Renderer/Mapper (`ViewMapper`) aufzulösen und eine saubere Trennung der Belange (Separation of Concerns) im Sinne von MVC/MVVM zu erreichen, bieten sich folgende architektonische Lösungsansätze an:

---

## 1. Abstraktion des UI-Kontextes (Context Interface)

**Problem:** Die States greifen aktuell direkt auf UI-spezifische Methoden des `CanvasController` zu (z.B. `context.getDrawingPane()`, `context.getToolbarController()`).

**Lösung:** Definiere ein abstraktes Interface `EditorContext`, das nur die fachlich relevanten Methoden für die States bereitstellt.

```java
public interface EditorContext {
    CoreRegistry getRegistry();
    CommandHistory getCommandHistory();
    SelectionModel getSelectionModel();
    
    // Fachliche Hilfsmethoden/Abfragen statt UI-Aktionen:
    boolean isSnapToGrid();
    boolean isWaypointsVisible();
    
    void setCurrentState(EditorState state);
    void reactivateCurrentTool();
    void resetToIdleState();
    
    // Hit-Testing-Methoden abstrahiert:
    FmcObject findObjectAt(double worldX, double worldY);
    FmcText findTextAt(double worldX, double worldY);
    UUID findConnectionNear(double worldX, double worldY, double tolerance);
    List<UUID> findObjectsInBounds(double x1, double y1, double x2, double y2);
}
```

* **Vorteil:** Die State-Klassen verlangen im `enterState`, `handleInput` etc. nur noch das Interface `EditorContext` anstelle von `CanvasController`. Dadurch lässt sich die Domain-Logik der States ohne JavaFX im Headless-Modus/Unit-Test testen.

---

## 2. Entkopplung von `ViewMapper` durch UI-Services oder Event-System

**Problem:** In States wie `EditTextState` oder `ResizeState` wird der `ViewMapper` direkt verwendet (z.B. `context.getViewMapper().getTextMapper().getTextNode(...)` oder `ViewMapper.getHandles(...)`). Das bringt JavaFX-Klassen wie `Text`, `TextArea` oder `Rectangle` direkt in die State-Logik.

### A) Für Text-Editing (`EditTextState`):
Statt die `TextArea` im State selbst zu erzeugen und auf den FX-Szenengraph der `drawingPane` zu legen, kapsle diese Operation in einen `TextEditorService`:

```java
public interface TextEditorService {
    void startInlineEditing(UUID textId, String initialText, double x, double y, Consumer<String> onCommit, Runnable onCancel);
}
```
Der State teilt dem Service nur noch mit: *"Starte die Bearbeitung für Text X"* und reagiert auf die Callbacks `onCommit` / `onCancel`.

### B) Für Resize-Handles (`ResizeState`):
Anstatt dass der State die Handles von der View abfragt und Szenengraph-Operationen veranlasst, sollte der **Model-Zustand** bestimmen, welches Objekt gerade im "Resize-Modus" ist (z.B. im `SelectionModel` abspeichern). Der `ViewMapper` reagiert als Observer auf Änderungen im `SelectionModel` und rendert die Handles eigenständig.

---

## 3. UI-freie Event-Daten & Unabhängige Input-Abstraktion

`InteractionEventData` ist als `record` bereits weitgehend frei von JavaFX, enthält aber z.B. noch Bezüge zu `javafx.scene.input.KeyCode`.

* **Verbesserung:**
  1. Ergänze ein eigenes Key-Enum (z.B. `EditorKey.ESCAPE`, `EditorKey.ENTER`) oder abstrahiere Shortcuts in logische Kommandos / Intentions (z.B. `CANCEL`, `CONFIRM`, `PRIMARY_CLICK`).
  2. Die Konvertierung von FX-`MouseEvent`/`KeyEvent` zu `InteractionEventData` erfolgt ausschließlich in der UI-Schicht (`CanvasController`) vor dem Weiterleiten an die States.

---

## Zusammenfassung der Zielarchitektur

```
┌─────────────────────────────────────────────────────────┐
│                     JavaFX UI Layer                     │
│  (CanvasController, GraphView, ViewMapper, UI-Controls) │
└───────────────────────────┬─────────────────────────────┘
                            │ implementiert / ruft auf
                            ▼
┌─────────────────────────────────────────────────────────┐
│                     Core State Layer                    │
│   - EditorContext (Interface)                           │
│   - States (IdleState, CreateObjectState, etc.)         │
│   - InteractionMap / InteractionEventData               │
└───────────────────────────┬─────────────────────────────┘
                            │ gesteuert über
                            ▼
┌─────────────────────────────────────────────────────────┐
│                     Core Domain Model                   │
│   (CoreRegistry, SelectionModel, Commands, Model-Data)  │
└─────────────────────────────────────────────────────────┘
```

Durch diese Schritte entfernst du sämmtliche direkten UI-Referenzen aus dem `state`-Paket, machst das System modularer und vereinfachst automatisierte Tests für Geschäftslogik und State-Übergänge drastisch.

---

# you asked

message time: 2026-08-04 19:29:36

## Sehr gute Frage! Die Kopplung der States an `CanvasController` und `ViewMapper` ist tatsächlich der größte Schwachpunkt deiner aktuellen Architektur.

Du hast völlig recht: Obwohl der **Core** JavaFX-frei ist, sind die **States** (die ja eigentlich zur Logik gehören) stark an die UI-Komponenten gebunden. Das erschwert Unit-Tests und macht Änderungen an der View aufwändig.

Hier zeige ich dir **drei Ansätze** – von **einfach** bis **fortgeschritten** – wie wir diese Kopplung lösen können.

---

## Das Problem im Detail

### Aktuell (starke Kopplung):
```java
public class IdleState implements EditorState {
    @Override
    public void handleInput(InteractionEventData event, CanvasController context) {
        // Direkte Abhängigkeit von CanvasController
        FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
        context.getSelectionModel().selectObject(hit.id());
        context.getViewMapper().setSelectedObjects(...); // ViewMapper!
        context.getRegistry().addObject(...);
    }
}
```

**Warum ist das problematisch?**
1. **Testbarkeit:** Du brauchst für jeden Test einen vollständigen `CanvasController` mit ViewMapper.
2. **Wiederverwendbarkeit:** Die States können nur im Kontext einer JavaFX-Anwendung verwendet werden.
3. **Änderbarkeit:** Jede Änderung am `CanvasController` zieht Änderungen in allen States nach sich.
4. **Single Responsibility:** Der State kennt zu viele Details der Controller- und View-Schicht.

---

## Lösung 1: **Interface-Extraktion** (Einfach & Pragmatisch)

Wir definieren ein **Interface `EditorContext`**, das alle Methoden enthält, die ein State benötigt. Der `CanvasController` implementiert dieses Interface.

### Schritt 1: Interface definieren

```java
package de.fmc.editor.state;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcText;
import de.fmc.editor.core.model.SelectionModel;
import de.fmc.editor.core.command.CommandHistory;
import de.fmc.editor.view.ViewMapper;

import java.util.List;
import java.util.UUID;

/**
 * Schnittstelle zwischen States und der Editor-Umgebung.
 * Alle Methoden, die ein State benötigt, werden hier deklariert.
 */
public interface EditorContext {

    // --- Datenzugriff ---
    CoreRegistry getRegistry();
    SelectionModel getSelectionModel();
    CommandHistory getCommandHistory();

    // --- View-Operationen ---
    void updateSelectionInView();
    ViewMapper getViewMapper();

    // --- Hit-Tests (für Interaktion) ---
    FmcObject findObjectAt(double x, double y);
    FmcText findTextAt(double x, double y);
    UUID findConnectionNear(double x, double y, double tolerance);
    List<UUID> findObjectsInBounds(double x1, double y1, double x2, double y2);

    // --- State-Management ---
    void setCurrentState(EditorState state);
    void resetToIdleState();
    void reactivateCurrentTool();

    // --- Toolbar-Informationen ---
    boolean isWaypointsVisible();
    boolean isSnapToGrid();

    // --- Zugriff auf die DrawingPane (für UI-Overlays) ---
    // Optional – nur für States, die UI-Elemente hinzufügen (wie BoxSelection)
    GraphView getDrawingPane(); // Achtung: GraphView ist JavaFX!
}
```

### Schritt 2: CanvasController implementiert das Interface

```java
public class CanvasController implements EditorContext {
    // Alle Methoden aus dem Interface werden implementiert
    // (die meisten existieren bereits!)
    
    @Override
    public CoreRegistry getRegistry() { return registry; }
    
    @Override
    public SelectionModel getSelectionModel() { return selectionModel; }
    
    @Override
    public void updateSelectionInView() {
        if (viewMapper != null) {
            viewMapper.setSelectedObjects(selectionModel.getSelectedObjectIds());
            viewMapper.setSelectedTexts(selectionModel.getSelectedTextIds());
        }
    }
    
    // ... alle anderen Methoden
}
```

### Schritt 3: States verwenden das Interface

```java
public class IdleState implements EditorState {
    @Override
    public void handleInput(InteractionEventData event, EditorContext context) {
        // Statt CanvasController jetzt EditorContext
        FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
        context.getSelectionModel().selectObject(hit.id());
        // View-Update erfolgt automatisch über SelectionModel-Listener
    }
}
```

**Vorteile:**
- ✅ States sind jetzt **nur noch vom Interface** abhängig.
- ✅ Du kannst den State mit einem **Mock-EditorContext** testen.
- ✅ Die View-Kopplung ist reduziert (immer noch ein `GraphView` im Interface).
- ✅ Minimaler Aufwand – die meisten Methoden existieren bereits.

**Nachteile:**
- ❌ Das Interface ist **groß** (viele Methoden).
- ❌ `GraphView` ist immer noch JavaFX – die State bleiben JavaFX-abhängig.

---

## Lösung 2: **Abhängigkeitsinjektion mit Services** (Fortgeschrittener)

Wir teilen die Verantwortlichkeiten in **Services** auf und injizieren sie in die States.

### Schritt 1: Services definieren

```java
// Service für Datenzugriff
public interface DataService {
    CoreRegistry getRegistry();
    SelectionModel getSelectionModel();
    CommandHistory getCommandHistory();
}

// Service für View-Operationen (abstrahiert)
public interface ViewService {
    void updateSelection();
    void showHandles(UUID objectId, List<Handle> handles);
    void clearHandles();
    void showSelectionBox(double x, double y, double width, double height);
    void hideSelectionBox();
}

// Service für Hit-Tests
public interface HitTestService {
    FmcObject findObjectAt(double x, double y);
    FmcText findTextAt(double x, double y);
    UUID findConnectionNear(double x, double y, double tolerance);
    List<UUID> findObjectsInBounds(double x1, double y1, double x2, double y2);
}

// Service für State-Management
public interface StateService {
    void setCurrentState(EditorState state);
    void resetToIdleState();
    void reactivateCurrentTool();
}
```

### Schritt 2: Konkrete Implementierungen im CanvasController

```java
public class CanvasController 
        implements DataService, ViewService, HitTestService, StateService {
    
    // Implementiert alle Service-Methoden
    // (delegiert an vorhandene Methoden)
}
```

### Schritt 3: State erhält nur die benötigten Services

```java
public class IdleState implements EditorState {
    private final DataService data;
    private final ViewService view;
    private final HitTestService hitTest;
    private final StateService state;
    private final ToolbarService toolbar; // für Snapping etc.
    
    public IdleState(DataService data, ViewService view, HitTestService hitTest, 
                     StateService state, ToolbarService toolbar) {
        this.data = data;
        this.view = view;
        this.hitTest = hitTest;
        this.state = state;
        this.toolbar = toolbar;
    }
    
    @Override
    public void handleInput(InteractionEventData event) {
        FmcObject hit = hitTest.findObjectAt(event.worldX(), event.worldY());
        if (hit != null) {
            data.getSelectionModel().selectObject(hit.id());
            view.updateSelection(); // oder automatisch über Listener
        }
    }
}
```

**Vorteile:**
- ✅ **Single Responsibility:** Jeder Service hat eine klare Aufgabe.
- ✅ **Hohe Testbarkeit:** Du kannst jeden Service mocken.
- ✅ **Lose Kopplung:** Die States kennen nur die Service-Interfaces.
- ✅ **JavaFX-frei:** Keine `GraphView`- oder `CanvasController`-Referenzen mehr.

**Nachteile:**
- ❌ Mehr Boilerplate-Code (Services + Implementierungen).
- ❌ Die Services müssen im `CanvasController` implementiert werden.

---

## Lösung 3: **Event-Bus / Messaging** (Fortgeschritten & Clean)

Statt Methodenaufrufen verwenden wir ein **Event-Bus-System**. Die States senden Events, und der `CanvasController` (oder andere Komponenten) reagieren darauf.

### Schritt 1: Events definieren

```java
// Basis-Event
public interface EditorEvent {}

// Spezifische Events
public record SelectObjectEvent(UUID objectId) implements EditorEvent {}
public record SelectTextEvent(UUID textId) implements EditorEvent {}
public record ClearSelectionEvent() implements EditorEvent {}
public record AddWaypointEvent(UUID connectionId, double x, double y) implements EditorEvent {}
public record SwitchStateEvent(EditorState newState) implements EditorEvent {}
public record UpdateViewEvent() implements EditorEvent {}
```

### Schritt 2: Event-Bus (einfache Implementierung)

```java
public class EventBus {
    private final Map<Class<?>, List<Consumer<?>>> listeners = new HashMap<>();
    
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>())
                 .add(listener);
    }
    
    public void publish(EditorEvent event) {
        List<Consumer<?>> consumers = listeners.get(event.getClass());
        if (consumers != null) {
            for (Consumer<?> consumer : consumers) {
                ((Consumer<EditorEvent>) consumer).accept(event);
            }
        }
    }
}
```

### Schritt 3: State sendet Events

```java
public class IdleState implements EditorState {
    private final EventBus eventBus;
    
    public IdleState(EventBus eventBus) {
        this.eventBus = eventBus;
    }
    
    @Override
    public void handleInput(InteractionEventData event) {
        FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
        if (hit != null) {
            // Statt direkter Methodenaufrufe: Events senden!
            eventBus.publish(new ClearSelectionEvent());
            eventBus.publish(new SelectObjectEvent(hit.id()));
            eventBus.publish(new UpdateViewEvent());
        }
    }
}
```

### Schritt 4: CanvasController lauscht auf Events

```java
public class CanvasController {
    private final EventBus eventBus;
    
    public CanvasController() {
        this.eventBus = new EventBus();
        subscribeToEvents();
    }
    
    private void subscribeToEvents() {
        eventBus.subscribe(SelectObjectEvent.class, this::onSelectObject);
        eventBus.subscribe(ClearSelectionEvent.class, this::onClearSelection);
        eventBus.subscribe(UpdateViewEvent.class, this::updateSelectionInView);
        // ...
    }
    
    private void onSelectObject(SelectObjectEvent e) {
        selectionModel.selectObject(e.objectId());
    }
}
```

**Vorteile:**
- ✅ **Extreme Entkopplung:** States kennen den `CanvasController` nicht mehr.
- ✅ **Erweiterbar:** Neue Listener können einfach hinzugefügt werden.
- ✅ **Testbarkeit:** Du kannst testen, ob der State die richtigen Events sendet.

**Nachteile:**
- ❌ **Schwerer zu debuggen** (Wer hat welches Event ausgelöst?).
- ❌ **Mehr Boilerplate** (Events + Subscriptions).
- ❌ **Keine direkte Rückgabe** (asynchron).

---

## Meine Empfehlung für dein Projekt

| Kriterium | Lösung 1 (Interface) | Lösung 2 (Services) | Lösung 3 (Event-Bus) |
|-----------|---------------------|---------------------|----------------------|
| **Aufwand** | ✅ Gering | ⚠️ Mittel | ⚠️ Mittel |
| **JavaFX-frei** | ⚠️ Teilweise | ✅ Ja | ✅ Ja |
| **Testbarkeit** | ✅ Gut | ✅ Sehr gut | ✅ Gut |
| **Verständlichkeit** | ✅ Sehr gut | ⚠️ Gut | ⚠️ Gewöhnungsbedürftig |
| **Dokumentation** | ✅ Einfach | ✅ Gut | ⚠️ Muss erklärt werden |

**Für dein Projekt empfehle ich:**

1. **Kurzfristig:** Lösung 1 (Interface) – minimaler Aufwand, sofort umsetzbar.
2. **Mittelfristig:** Lösung 2 (Services) – sauberste Trennung, wenn du mehr Zeit investieren möchtest.
3. **Für große Teams:** Lösung 3 (Event-Bus) – wenn viele Komponenten unabhängig voneinander sein sollen.

---

## Beispiel für die kurzfristige Umsetzung (Lösung 1)

Ich zeige dir, wie du den `IdleState` mit dem `EditorContext`-Interface umschreiben kannst:

### 1. `EditorContext`-Interface (kompakt)

```java
package de.fmc.editor.state;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcText;
import de.fmc.editor.core.model.SelectionModel;
import de.fmc.editor.core.command.CommandHistory;
import de.fmc.editor.view.GraphView;
import de.fmc.editor.view.ViewMapper;

import java.util.List;
import java.util.UUID;

public interface EditorContext {
    // Daten
    CoreRegistry getRegistry();
    SelectionModel getSelectionModel();
    CommandHistory getCommandHistory();
    
    // View
    ViewMapper getViewMapper();
    GraphView getDrawingPane();
    
    // Hit-Tests
    FmcObject findObjectAt(double x, double y);
    FmcText findTextAt(double x, double y);
    UUID findConnectionNear(double x, double y, double tolerance);
    List<UUID> findObjectsInBounds(double x1, double y1, double x2, double y2);
    
    // State-Management
    void setCurrentState(EditorState state);
    void resetToIdleState();
    void reactivateCurrentTool();
    
    // Toolbar
    boolean isSnapToGrid();
    boolean isWaypointsVisible();
}
```

### 2. `CanvasController` implementiert das Interface

```java
public class CanvasController implements EditorContext {
    // Alle Methoden des Interfaces werden implementiert
    // (die meisten existieren bereits)
    
    @Override
    public boolean isSnapToGrid() {
        return toolbarController.isSnapToGrid();
    }
    
    @Override
    public boolean isWaypointsVisible() {
        return toolbarController.isWaypointsVisible();
    }
    
    // ... etc.
}
```

### 3. `IdleState` verwendet das Interface

```java
public class IdleState implements EditorState {
    private InteractionMap bindings;
    
    @Override
    public void handleInput(InteractionEventData event, EditorContext context) {
        // Jetzt: EditorContext statt CanvasController
        initBindings(context);
        // ... Rest unverändert
    }
    
    private void handleSelection(InteractionEventData event, EditorContext context, FmcObject hit) {
        SelectionModel sel = context.getSelectionModel();
        // ... Rest unverändert
    }
}
```

---

## Fazit

Die **Interface-Extraktion (Lösung 1)** ist der beste Einstieg:
- Du kannst sie **schrittweise** umsetzen (State für State).
- Sie ist **minimal invasiv** – deine bestehende Architektur bleibt erhalten.
- Sie verbessert sofort die **Testbarkeit** und **Wartbarkeit**.

Mir gefaellt die EventBus Loesung. Scheint aber der schwierigste zu sein. Was meinst du dazu?

---

# gemini response

Die Wahl des **Event-Bus-Musters (Lösung 3)** zeigt eine fantastische Architektur-Vision! Es ist tatsächlich der eleganteste Weg, um ein System **vollständig zu entkoppeln**.

In der Praxis bringt der Event-Bus allerdings ein paar Fallstricke mit sich, die man kennen sollte, bevor man sich entscheidet.

---

## 💡 Warum der Event-Bus extrem charmant ist

* **Absolute UI-Freiheit:** Deine States senden nur noch reine Daten-Events (z. B. `SelectObjectEvent(id)`). Sie wissen weder, wer das Event empfängt, noch ob es eine JavaFX-UI, ein Konsolen-Log oder ein Unit-Test ist.
* **Extrem modular:** Möchtest du später ein Modul hinzufügen (z. B. ein Undo-System, ein Auto-Save-Plugin oder Analytics), lauscht dieses einfach auf dieselben Events – ganz ohne den bestehenden Code der States zu verändern (*Open-Closed Principle*).
* **Traumhafte Testbarkeit:** In Unit-Tests übergibst du dem State einen Mock-Event-Bus und prüfst einfach: *„Hat der State nach Klick X das Event Y gefeuert?“*

---

## ⚠️ Die Tücken beim Event-Bus (und warum er als „schwierig“ gilt)

1. **Keine Rückgabewerte (Fire & Forget):** 
   Ein Event-Bus arbeitet typischerweise einseitig (`void publish(Event)`). Wenn dein State ein Ergebnis *sofort* braucht (z. B. ein Hit-Test: *„Welches Objekt liegt bei Koordinate X/Y?“*), funktioniert der Event-Bus nicht ohne Weiteres. 
   > **Konsequenz:** Hit-Tests oder mathematische Abfragen sollten weiterhin direkt über Domain-Services/Read-Models laufen, nicht über Events.

2. **Der „Erkennungsdienst“-Effekt (Kontrollfluss-Verlust):**
   Bei Methodenaufrufen siehst du im IDE-Stacktrace genau: `IdleState` ➔ `CanvasController` ➔ `ViewMapper`. Beim Event-Bus feuert der State ein Event ab, und „irgendwo“ im Code reagiert eine Methode darauf. Bei vielen Events kann das Debugging knifflig werden, wenn man nicht mehr direkt sieht, wer ein Event ausgelöst hat.

3. **Event-Explosion:**
   Du benötigst für jede Aktion eine eigene `record`- oder `class`-Definition (`SelectObjectEvent`, `ClearSelectionEvent`, `StartResizeEvent`, etc.), was zu vielen kleinen Klassen führt.

---

## 🛠️ Mein Fazit & Empfehlung: Der "Pragmatische Hybrid-Ansatz"

Du musst dich **nicht zu 100 %** für ein einziges Muster entscheiden. Die beste Lösung für Editor-Architekturen ist oft eine **Kombination aus Service/Context & Event-Bus**:

```
 ┌─────────────────────────────────────────────────────────┐
 │                       IdleState                         │
 └─────────────┬─────────────────────────────┬─────────────┘
               │ (1) Lesen / Hit-Tests       │ (2) Aktionen / Intentions
               ▼                             ▼
 ┌───────────────────────────┐ ┌───────────────────────────┐
 │   EditorReadContext       │ │         EventBus          │
 │   - findObjectAt(...)     │ │   - publish(SelectEvent)  │
 │   - isSnapToGrid()        │ │   - publish(MoveEvent)    │
 └───────────────────────────┘ └─────────────┬─────────────┘
                                             │
                                             ▼
                               ┌───────────────────────────┐
                               │ CanvasController / View   │
                               └───────────────────────────┘
```

1. **Events für Aktionen (Zustandsänderungen):** Alles, was der State *verändern* oder *auslösen* möchte, feuert er als Event über den Bus (z. B. `SelectionChangedEvent`, `CreateObjectCommandEvent`).
2. **Abfragen über ein Interface:** Reine Lese-Operationen (wie `findObjectAt(x, y)` oder Grid-Settings) fragt der State über ein einfaches Read-Only-Interface ab, da Events für synchrone Rückfragen unnötig komplex wären.

### Fazit
Wenn dir der Event-Bus zusagt: **Geh das Wagnis ein!** Es ist ein hervorragendes Architektur-Pattern. Starte am besten mit einem kleinen Event-Bus und nutze `record`s in Java (die halten den Code-Overhead extrem gering).

Möchtest du, dass wir für einen konkreten State (z. B. den `IdleState` oder `ResizeState`) ein kurzes Code-Beispiel durchgehen, wie das Entkoppeln mit dem Event-Bus Schritt für Schritt aussieht?

---

# you asked

message time: 2026-08-04 19:32:22

IdleState ist eine super Idee.

---

# gemini response

Hier ist eine konkrete Schritt-für-Schritt-Umsetzung für den **`IdleState`** mit dem hybriden **Event-Bus + Read-Context** Ansatz.

Damit wird der `IdleState` zu **100 % JavaFX-frei** und hat keinerlei Abhängigkeiten mehr zum `CanvasController` oder `ViewMapper`.

---

## 1. Das Read-Interface für Abfragen (`EditorReadContext`)

Da Events synchron keine Werte zurückliefern können, nutzen wir für reine **Lese-Operationen** (Hit-Tests, Tool-Einstellungen) ein einfaches Interface:

```java
package de.fmc.editor.state;

import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcText;
import java.util.List;
import java.util.UUID;

/**
 * Reines Lese-Interface für States.
 * Erlaubt Hit-Tests und Abfragen ohne Seiteneffekte.
 */
public interface EditorReadContext {
    FmcObject findObjectAt(double worldX, double worldY);
    FmcText findTextAt(double worldX, double worldY);
    UUID findConnectionNear(double worldX, double worldY, double tolerance);
    List<UUID> findObjectsInBounds(double x1, double y1, double x2, double y2);

    boolean isSnapToGrid();
    boolean isWaypointsVisible();
}
```

---

## 2. Die Events (Java `record`s)

Alle **Zustandsänderungen und Absichten (Intentions)** werden als kompakte Java Records definiert:

```java
package de.fmc.editor.event;

import de.fmc.editor.state.EditorState;
import java.util.UUID;

public sealed interface EditorEvent {
    // Selection Events
    record SelectObject(UUID id, boolean clearPrevious) implements EditorEvent {}
    record SelectText(UUID id, boolean clearPrevious) implements EditorEvent {}
    record ClearSelection() implements EditorEvent {}

    // State Transitions
    record ChangeState(EditorState newState) implements EditorEvent {}
    record ReactivateTool() implements EditorEvent {}

    // Drag / Box Selection Events
    record StartBoxSelection(double startX, double startY) implements EditorEvent {}
    record StartObjectDrag(UUID objectId, double startX, double startY) implements EditorEvent {}
}
```

---

## 3. Der entkoppelte `IdleState`

Der `IdleState` kennt jetzt **weder JavaFX noch den CanvasController**. Er erhält im Konstruktor den `EventBus` und nutzt beim `handleInput` den `EditorReadContext`:

```java
package de.fmc.editor.state;

import de.fmc.editor.event.EditorEvent.*;
import de.fmc.editor.event.EventBus;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcText;
import java.util.UUID;

public class IdleState implements EditorState {

    private final EventBus eventBus;

    public IdleState(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void handleInput(InteractionEventData event, EditorReadContext context) {
        // Beispiel: Primär-Klick (Linksklick)
        if (event.isPrimaryClick()) {
            handlePrimaryClick(event, context);
        } 
        // Beispiel: Drag gestartet
        else if (event.isDragStart()) {
            handleDragStart(event, context);
        }
    }

    private void handlePrimaryClick(InteractionEventData event, EditorReadContext context) {
        double x = event.worldX();
        double y = event.worldY();

        // 1. Hit-Test über Read-Context
        FmcObject hitObject = context.findObjectAt(x, y);
        if (hitObject != null) {
            // Signalisiere Selektion über EventBus
            boolean keepSelection = event.isShiftDown();
            eventBus.publish(new SelectObject(hitObject.id(), !keepSelection));
            return;
        }

        FmcText hitText = context.findTextAt(x, y);
        if (hitText != null) {
            boolean keepSelection = event.isShiftDown();
            eventBus.publish(new SelectText(hitText.id(), !keepSelection));
            return;
        }

        // Klick ins Leere -> Auswahl aufheben
        eventBus.publish(new ClearSelection());
    }

    private void handleDragStart(InteractionEventData event, EditorReadContext context) {
        double x = event.worldX();
        double y = event.worldY();

        FmcObject hitObject = context.findObjectAt(x, y);
        if (hitObject != null) {
            // Wechsel in den DragState
            eventBus.publish(new StartObjectDrag(hitObject.id(), x, y));
            eventBus.publish(new ChangeState(new MoveObjectState(eventBus, hitObject.id(), x, y)));
        } else {
            // Wechsel in den Box-Selection-State
            eventBus.publish(new StartBoxSelection(x, y));
            eventBus.publish(new ChangeState(new BoxSelectionState(eventBus, x, y)));
        }
    }
}
```

---

## 4. Wie der `CanvasController` die Events verarbeitet

Der `CanvasController` implementiert das `EditorReadContext`-Interface und meldet sich beim Event-Bus als Empfänger für die Events an:

```java
public class CanvasController implements EditorReadContext {

    private final EventBus eventBus;
    private final CoreRegistry registry;
    private final SelectionModel selectionModel;
    private final ViewMapper viewMapper;

    public CanvasController() {
        this.eventBus = new EventBus();
        this.registry = new CoreRegistry();
        this.selectionModel = new SelectionModel();
        this.viewMapper = new ViewMapper(...);

        registerEventHandlers();
    }

    private void registerEventHandlers() {
        // Auf Selektions-Events reagieren
        eventBus.subscribe(SelectObject.class, e -> {
            if (e.clearPrevious()) {
                selectionModel.clear();
            }
            selectionModel.selectObject(e.id());
            // ViewMapper aktualisiert automatisch oder manuell:
            viewMapper.setSelectedObjects(selectionModel.getSelectedObjectIds());
        });

        eventBus.subscribe(ClearSelection.class, e -> {
            selectionModel.clear();
            viewMapper.clearSelection();
        });

        // Auf State-Wechsel reagieren
        eventBus.subscribe(ChangeState.class, e -> {
            this.currentState = e.newState();
        });
    }

    // --- Implementierung von EditorReadContext (Nur Abfragen) ---
    @Override
    public FmcObject findObjectAt(double worldX, double worldY) {
        return registry.findObjectAt(worldX, worldY);
    }

    @Override
    public boolean isSnapToGrid() {
        return toolbarController.isSnapToGrid();
    }
    
    // ... restliche Read-Methoden
}
```

---

## Warum dieser Ansatz für `IdleState` genial ist:

1. **Perfekte Unit-Tests:** Du kannst den `IdleState` testen, ohne JavaFX zu starten!
   ```java
   @Test
   void testClickOnEmptySpaceClearsSelection() {
       TestEventBus bus = new TestEventBus();
       IdleState state = new IdleState(bus);
       EditorReadContext mockContext = mock(EditorReadContext.class);

       state.handleInput(createClickEvent(100, 100), mockContext);

       // Prüfen, ob das passende Event gefeuert wurde:
       assertTrue(bus.hasReceived(ClearSelection.class));
   }
   ```
2. **Klarer Datenfluss:** Der State entscheidet *was* passieren soll und schickt ein Event. Die UI/Controller-Schicht entscheidet *wie* es visuell dargestellt wird.

---

# you asked

message time: 2026-08-04 19:45:20

Brauchen wir bei dieser Architektur die Klasse InteractionMap noch?

---

# gemini response

Das kommt darauf an, wie du deine Tastatur- und Maus-Shortcuts verwalten möchtest. Die kurze Antwort lautet: **Funktional brauchst du sie für die reine Entkopplung nicht mehr unbedingt, aber sie erfüllt jetzt eine neue, sehr wertvolle Aufgabe.**

Hier ist der Überblick, wie sich die Rolle der `InteractionMap` durch den Event-Bus verändert:

---

## 1. Wofür du sie NICHT mehr brauchst
Früher hat die `InteractionMap` vermutlich Lambdas oder Action-Handler ausgeführt, die direkt Methoden auf dem `CanvasController` oder `ViewMapper` aufgerufen haben. 

Diese direkte Kopplung fällt komplett weg, da Handlungen jetzt als **Events** abgefeuert werden.

---

## 2. Warum sie trotzdem extrem nützlich bleibt (als "Shortcut-Decoder")

Wenn du die `InteractionMap` behältst, trennst du **physische Input-Events** (z. B. *`Taste 'DELETE' gedrückt`* oder *`STRG + Z`*) von der **fachlichen Intention** (*`DeleteSelectedObjects`* oder *`Undo`*).

Die `InteractionMap` fungiert dann als **Mapping-Tabelle zwischen Tasten und Events**:

```java
public class InteractionMap {
    private final Map<KeyCombination, Supplier<EditorEvent>> keyBindings = new HashMap<>();

    public InteractionMap() {
        // Mappe Key-Kombinationen direkt auf die passenden Events
        keyBindings.put(new KeyCodeCombination(KeyCode.DELETE), ClearSelection::new);
        keyBindings.put(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN), UndoEvent::new);
    }

    public Optional<EditorEvent> getEventFor(KeyEvent keyEvent) {
        // Findet das passende Event zur gedrückten Taste
        return keyBindings.entrySet().stream()
                .filter(entry -> entry.getKey().match(keyEvent))
                .map(entry -> entry.getValue().get())
                .findFirst();
    }
}
```

---

## 3. Die zwei Wege für dein Projekt

### Weg A: `InteractionMap` behalten (Empfohlen für konfigurierbare Keybindings)
* **Wie es funktioniert:** Der `IdleState` nutzt die `InteractionMap`, um zu prüfen: *"Gibt es für dieses Key-Event ein registriertes `EditorEvent`?"* Wenn ja, schickt er es direkt an den `EventBus`.
* **Vorteil:** Du kannst Shortcuts später spielend leicht anpassbar machen (z. B. Einstellungen für "Custom Keybindings"), ohne eine einzige Zeile Code in den States zu ändern.

### Weg B: `InteractionMap` entfernen (Einfacher, wenn Shortcuts feststehen)
* **Wie es funktioniert:** Du wertest die Tasten/Klicks direkt per `if/switch` im `IdleState` aus (wie im Code-Beispiel oben):
  ```java
  if (event.isDeleteKeyPressed()) {
      eventBus.publish(new DeleteSelectedObjects());
  }
  ```
* **Vorteil:** Weniger Klassen, schmalere Architektur.
* **Nachteil:** Tastenkombinationen sind starr in den States hardcodiert.

---

## Empfehlung

Wenn deine Shortcuts **fest im Code** stehen können und sich nie ändern müssen, kannst du die `InteractionMap` **ersatzlos streichen** und die Auswertung direkt in die `handleInput`-Methoden der States schreiben. 

Möchtest du deinen Benutzern aber ermöglichen, Tastenkombinationen in den Einstellungen anzupassen, ist die `InteractionMap` der perfekte Ort dafür!

---

# you asked

message time: 2026-08-04 19:59:15

und wie wird dann InteractionMap in IdleState benutzt, oder wird sie ueberhaupt dort benutzt? Wenn ja ist die Klasse KeyCombination nicht von JavaFX und damit wieder eine gewisse Abhaengigleit von javaFX in den States?

---

# gemini response

Das ist eine **hervorragende und extrem scharfsinnige Beobachtung!** 

Genau hier liegt der wunde Punkt: Wenn du `javafx.scene.input.KeyCode` oder `KeyCombination` in einer `InteractionMap` innerhalb eines States verwendest, holst du dir **JavaFX wieder durch die Hintertür in dein State-Paket zurück**.

Um den State **100 % JavaFX-frei** zu halten, gibt es zwei saubere Lösungen dafür, ob und wie die `InteractionMap` genutzt wird:

---

## Möglichkeit 1: Die `InteractionMap` wandert in den UI-Layer (Empfohlen)

Die `InteractionMap` gehört eigentlich nicht zur Domain-Logik des States, sondern zur **UI-Schicht (JavaFX)**. Sie übersetzt dort physische Tasteneingaben in **entkoppelte Event-Daten oder Intentions**.

### Wie der Ablauf aussieht:

1. **JavaFX UI Schicht (`CanvasController` / Event Filter):**
   Das rohe JavaFX `KeyEvent` trifft ein. Die UI nutzt eine JavaFX-basierte `InteractionMap`, um zu prüfen, welche *fachliche Taste* oder *Aktion* gemeint ist, und baut daraus dein UI-freies `InteractionEventData`.

2. **Entkoppeltes `InteractionEventData` (Domain):**
   Dein `InteractionEventData` verwendet **keine** JavaFX-Klassen, sondern z.B. eigene Enums oder einfache Properties:

   ```java
   public record InteractionEventData(
       double worldX,
       double worldY,
       EventType type,          // MOUSE_CLICK, DRAG_START, KEY_PRESS
       LogicalKey key,          // Eigenes Enum: DELETE, ENTER, ESCAPE, BACKSPACE...
       boolean isControlDown,
       boolean isShiftDown
   ) {}
   ```

3. **Der `IdleState` (Domain):**
   Der State empfängt nur noch das saubere `InteractionEventData`. Er braucht **keine** `InteractionMap` mehr, sondern wertet direkt das `LogicalKey`-Enum aus:

   ```java
   public class IdleState implements EditorState {
       private final EventBus eventBus;

       public IdleState(EventBus eventBus) {
           this.eventBus = eventBus;
       }

       @Override
       public void handleInput(InteractionEventData event, EditorReadContext context) {
           // Tastatur-Auswertung ohne JavaFX!
           if (event.type() == EventType.KEY_PRESS) {
               if (event.key() == LogicalKey.DELETE) {
                   eventBus.publish(new DeleteSelectedObjects());
                   return;
               }
               if (event.key() == LogicalKey.ESCAPE) {
                   eventBus.publish(new ClearSelection());
                   return;
               }
           }
           
           // Maus-Auswertung...
       }
   }
   ```

---

## Möglichkeit 2: Eine reine Domain-`InteractionMap` im State (Falls dynamisch erwünscht)

Möchtest du, dass die States weiterhin eine konfigurierbare `InteractionMap` besitzen, muss diese auf deinem eigenen `LogicalKey` (oder eigenen `KeyShortcut`-Records) basieren:

```java
// Völlig JavaFX-frei!
public record KeyShortcut(LogicalKey key, boolean ctrl, boolean shift, boolean alt) {}

public class InteractionMap {
    private final Map<KeyShortcut, Supplier<EditorEvent>> bindings = new HashMap<>();

    public void bind(LogicalKey key, boolean ctrl, Supplier<EditorEvent> eventSupplier) {
        bindings.put(new KeyShortcut(key, ctrl, false, false), eventSupplier);
    }

    public Optional<EditorEvent> getEventFor(InteractionEventData event) {
        KeyShortcut shortcut = new KeyShortcut(event.key(), event.isControlDown(), event.isShiftDown(), false);
        return Optional.ofNullable(bindings.get(shortcut)).map(Supplier::get);
    }
}
```

Im `IdleState` würde das so aussehen:

```java
public class IdleState implements EditorState {
    private final EventBus eventBus;
    private final InteractionMap bindings = new InteractionMap();

    public IdleState(EventBus eventBus) {
        this.eventBus = eventBus;
        // Zuordnung von Domain-Shortcuts zu EventBus-Events
        bindings.bind(LogicalKey.DELETE, false, DeleteSelectedObjects::new);
        bindings.bind(LogicalKey.ESCAPE, false, ClearSelection::new);
    }

    @Override
    public void handleInput(InteractionEventData event, EditorReadContext context) {
        // Prüfen, ob für das Tastenevent ein Event hinterlegt ist
        Optional<EditorEvent> mappedEvent = bindings.getEventFor(event);
        if (mappedEvent.isPresent()) {
            eventBus.publish(mappedEvent.get());
            return;
        }

        // Falls kein Shortcut passte: Standard-Mauslogik verarbeiten...
    }
}
```

---

## Zusammenfassung

1. **Wird `InteractionMap` im `IdleState` gebraucht?**
   * **Nein**, wenn deine Shortcuts im `IdleState` überschaubar sind. Dann reicht eine einfache Abfrage über `if (event.key() == LogicalKey.DELETE)` im State.
   * **Ja**, wenn du die Tastenbelegungen dynamisch konfigurieren oder zentral im State verwalten willst.

2. **Ist `KeyCombination` ein JavaFX-Leck?**
   * **Ja, absolut!** `javafx.scene.input.KeyCombination` oder `KeyCode` haben im State nichts zu suchen.
   * **Lösung:** Übersetze JavaFX-Keys in der UI-Schicht (`CanvasController`) in ein eigenes Enum (`LogicalKey`) oder eigene Records (`KeyShortcut`), bevor sie als `InteractionEventData` an den State übergeben werden.

---

# you asked

message time: 2026-08-04 20:14:03

Lass uns ein Proof of Concept erstellen und dabei die genannten Klassen ausarbeiten, sodass ich das als Maven Projekt starten kann. Es soll der Pragmatische Hybrid-Ansatz sein. Danke dir.

---

# gemini response

Das ist eine hervorragende Entscheidung. Mit einem lauffähigen PoC (Proof of Concept) siehst du das Zusammenspiel zwischen dem JavaFX-unabhängigen **Core (State, EventBus, ReadContext)** und dem **JavaFX-UI-Layer** am besten in Aktion.

Hier ist eine vollständige, startbereite Projektstruktur inklusive **`pom.xml`** und den ausgearbeiteten Klassen.

---

## 📁 Projektstruktur

```text
poc-editor
├── pom.xml
└── src
    └── main
        └── java
            └── de
                └── fmc
                    └── editor
                        ├── App.java                   (JavaFX Main Launcher)
                        ├── core
                        │   └── model
                        │       ├── FmcObject.java     (Domain Modell)
                        │       └── SelectionModel.java(Selektions-Zustand)
                        ├── event
                        │   ├── EditorEvent.java       (Sealed Interface + Records)
                        │   └── EventBus.java          (Typisierter Event-Bus)
                        ├── state
                        │   ├── EditorState.java       (State Interface)
                        │   ├── EditorReadContext.java (Lese-Schnittstelle)
                        │   ├── InteractionEventData.java (FX-freies Event Data)
                        │   ├── LogicalKey.java        (FX-freies Key Enum)
                        │   └── IdleState.java         (Der entkoppelte State)
                        └── ui
                            └── CanvasController.java  (FX Controller & UI Bridge)
```

---

## 1. `pom.xml`

Nutzt Java 21+ (`records`, `sealed interfaces`) und JavaFX 21:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>de.fmc</groupId>
    <artifactId>poc-editor</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <javafx.version>21.0.2</javafx.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <version>${javafx.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-maven-plugin</artifactId>
                <version>0.0.8</version>
                <configuration>
                    <mainClass>de.fmc.editor.App</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 2. Core Models (`core/model/`)

### `FmcObject.java`
```java
package de.fmc.editor.core.model;

import java.util.UUID;

public record FmcObject(UUID id, String name, double x, double y, double width, double height) {
    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
}
```

### `SelectionModel.java`
```java
package de.fmc.editor.core.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SelectionModel {
    private final Set<UUID> selectedIds = new HashSet<>();

    public void select(UUID id) {
        selectedIds.add(id);
    }

    public void clear() {
        selectedIds.clear();
    }

    public boolean isSelected(UUID id) {
        return selectedIds.contains(id);
    }

    public Set<UUID> getSelectedIds() {
        return Collections.unmodifiableSet(selectedIds);
    }
}
```

---

## 3. Event-System (`event/`)

### `EditorEvent.java`
```java
package de.fmc.editor.event;

import de.fmc.editor.state.EditorState;
import java.util.UUID;

public sealed interface EditorEvent {
    record SelectObject(UUID id, boolean clearPrevious) implements EditorEvent {}
    record ClearSelection() implements EditorEvent {}
    record DeleteObject(UUID id) implements EditorEvent {}
    record ChangeState(EditorState newState) implements EditorEvent {}
}
```

### `EventBus.java`
```java
package de.fmc.editor.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventBus {
    private final Map<Class<? extends EditorEvent>, List<Consumer<EditorEvent>>> listeners = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T extends EditorEvent> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>())
                 .add(event -> listener.accept((T) event));
    }

    public void publish(EditorEvent event) {
        List<Consumer<EditorEvent>> typeListeners = listeners.get(event.getClass());
        if (typeListeners != null) {
            for (Consumer<EditorEvent> listener : new ArrayList<>(typeListeners)) {
                listener.accept(event);
            }
        }
    }
}
```

---

## 4. State Layer & Enums (`state/`)

### `LogicalKey.java`
```java
package de.fmc.editor.state;

public enum LogicalKey {
    NONE,
    DELETE,
    ESCAPE,
    ENTER
}
```

### `InteractionEventData.java`
```java
package de.fmc.editor.state;

public record InteractionEventData(
    double worldX,
    double worldY,
    boolean isPrimaryClick,
    LogicalKey key,
    boolean isShiftDown
) {}
```

### `EditorReadContext.java`
```java
package de.fmc.editor.state;

import de.fmc.editor.core.model.FmcObject;
import java.util.List;
import java.util.UUID;

public interface EditorReadContext {
    FmcObject findObjectAt(double worldX, double worldY);
    List<UUID> getSelectedObjectIds();
    boolean isSnapToGrid();
}
```

### `EditorState.java`
```java
package de.fmc.editor.state;

public interface EditorState {
    void handleInput(InteractionEventData event, EditorReadContext context);
}
```

### `IdleState.java`
```java
package de.fmc.editor.state;

import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.event.EditorEvent.*;
import de.fmc.editor.event.EventBus;

public class IdleState implements EditorState {
    private final EventBus eventBus;

    public IdleState(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void handleInput(InteractionEventData event, EditorReadContext context) {
        // 1. Keyboard Handling (ohne JavaFX KeyCode!)
        if (event.key() == LogicalKey.DELETE) {
            for (var id : context.getSelectedObjectIds()) {
                eventBus.publish(new DeleteObject(id));
            }
            eventBus.publish(new ClearSelection());
            return;
        }

        if (event.key() == LogicalKey.ESCAPE) {
            eventBus.publish(new ClearSelection());
            return;
        }

        // 2. Mouse Click Handling
        if (event.isPrimaryClick()) {
            FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
            if (hit != null) {
                boolean keepSelection = event.isShiftDown();
                eventBus.publish(new SelectObject(hit.id(), !keepSelection));
            } else {
                eventBus.publish(new ClearSelection());
            }
        }
    }
}
```

---

## 5. UI Controller & Canvas (`ui/`)

Der `CanvasController` hält den Szenengraph auf Stand, übersetzt JavaFX-Inputs in das entkoppelte `InteractionEventData` und bedient die Lese-Anfragen über `EditorReadContext`.

### `CanvasController.java`
```java
package de.fmc.editor.ui;

import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.SelectionModel;
import de.fmc.editor.event.EditorEvent.*;
import de.fmc.editor.event.EventBus;
import de.fmc.editor.state.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CanvasController implements EditorReadContext {
    private final Canvas canvas;
    private final EventBus eventBus;
    private final SelectionModel selectionModel = new SelectionModel();
    private final List<FmcObject> objects = new ArrayList<>();
    
    private EditorState currentState;

    public CanvasController(Canvas canvas) {
        this.canvas = canvas;
        this.eventBus = new EventBus();
        this.currentState = new IdleState(eventBus);

        // Testobjekte anlegen
        objects.add(new FmcObject(UUID.randomUUID(), "Box A", 50, 50, 100, 80));
        objects.add(new FmcObject(UUID.randomUUID(), "Box B", 220, 100, 120, 90));

        setupEventSubscriptions();
        setupFXListeners();
        redraw();
    }

    private void setupEventSubscriptions() {
        eventBus.subscribe(SelectObject.class, e -> {
            if (e.clearPrevious()) selectionModel.clear();
            selectionModel.select(e.id());
            redraw();
        });

        eventBus.subscribe(ClearSelection.class, e -> {
            selectionModel.clear();
            redraw();
        });

        eventBus.subscribe(DeleteObject.class, e -> {
            objects.removeIf(obj -> obj.id().equals(e.id()));
            redraw();
        });

        eventBus.subscribe(ChangeState.class, e -> this.currentState = e.newState());
    }

    private void setupFXListeners() {
        // FX Mouse -> InteractionEventData
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, fxEvent -> {
            canvas.requestFocus(); // Fokus holen für Tastatur-Events
            InteractionEventData data = new InteractionEventData(
                fxEvent.getX(), fxEvent.getY(),
                fxEvent.isPrimaryButtonDown() || fxEvent.getButton() == javafx.scene.input.MouseButton.PRIMARY,
                LogicalKey.NONE,
                fxEvent.isShiftDown()
            );
            currentState.handleInput(data, this);
        });

        // FX Key -> InteractionEventData (Übersetzung FX KeyCode -> LogicalKey)
        canvas.addEventHandler(KeyEvent.KEY_PRESSED, fxEvent -> {
            LogicalKey logicalKey = switch (fxEvent.getCode()) {
                case DELETE -> LogicalKey.DELETE;
                case ESCAPE -> LogicalKey.ESCAPE;
                case ENTER -> LogicalKey.ENTER;
                default -> LogicalKey.NONE;
            };

            if (logicalKey != LogicalKey.NONE) {
                InteractionEventData data = new InteractionEventData(
                    0, 0, false, logicalKey, fxEvent.isShiftDown()
                );
                currentState.handleInput(data, this);
            }
        });
    }

    public void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (FmcObject obj : objects) {
            boolean selected = selectionModel.isSelected(obj.id());
            
            // Objekt füllen
            gc.setFill(selected ? Color.LIGHTBLUE : Color.LIGHTGRAY);
            gc.fillRect(obj.x(), obj.y(), obj.width(), obj.height());

            // Rahmen zeichnen
            gc.setStroke(selected ? Color.BLUE : Color.BLACK);
            gc.setLineWidth(selected ? 3 : 1);
            gc.strokeRect(obj.x(), obj.y(), obj.width(), obj.height());

            // Text zeichnen
            gc.setFill(Color.BLACK);
            gc.fillText(obj.name(), obj.x() + 10, obj.y() + 25);
        }
    }

    // --- Implementierung EditorReadContext ---
    @Override
    public FmcObject findObjectAt(double worldX, double worldY) {
        return objects.stream()
                .filter(obj -> obj.contains(worldX, worldY))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<UUID> getSelectedObjectIds() {
        return new ArrayList<>(selectionModel.getSelectedIds());
    }

    @Override
    public boolean isSnapToGrid() {
        return false;
    }
}
```

---

## 6. Main Application Launcher (`App.java`)

```java
package de.fmc.editor;

import de.fmc.editor.ui.CanvasController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        Canvas canvas = new Canvas(600, 400);
        canvas.setFocusTraversable(true); // Wichtig für Key-Events!

        // Controller initialisieren
        new CanvasController(canvas);

        BorderPane root = new BorderPane(canvas);
        Scene scene = new Scene(root, 600, 400);

        primaryStage.setTitle("PoC Editor - Hybrid Event-Bus Architecture");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

---

## 🚀 Ausführen

Du kannst das Projekt mit folgendem Befehl über das Terminal im Ordner `poc-editor` starten:

```bash
mvn clean javafx:run
```

### Was du im PoC testen kannst:
1. **Linksklick auf eine Box:** Das `IdleState` erkennt den Klick per `findObjectAt`, feuert ein `SelectObject`-Event ab und die Box färbt sich blau.
2. **Klick ins Leere:** Das `IdleState` feuert `ClearSelection` und hebt die Auswahl auf.
3. **Taste `ENTF` (Delete):** Wenn eine Box selektiert ist, schickt der `IdleState` nach dem Tastendruck ein `DeleteObject`-Event und löscht das Objekt vollständig vom Canvas.

Damit hast du die Architektur vollständig **entkoppelt**, ohne auf synchrone Abfragen (`EditorReadContext`) verzichten zu müssen!

---

# you asked

message time: 2026-08-04 20:35:37

koennen wir auch noch ein Drag and Drop mit dieser Technologie erstellen?

---

# gemini response

Ja, das geht hervorragend! Drag & Drop passt perfekt in diese Architektur. 

Um Drag & Drop sauber umzusetzen, führen wir einen **`MoveObjectState`** ein. Der `IdleState` erkennt den Start des Drags und wechselt in den `MoveObjectState`. Dieser verarbeitet die Maus-Bewegungen und schließt die Aktion beim Loslassen der Maustaste ab (oder bricht sie bei `ESC` ab).

Hier ist der erweiterte Code, den du direkt in dein PoC-Projekt einbauen kannst.

---

## 1. Ergänzungen in `EditorEvent.java`

Wir fügen Events für die Objekt-Verschiebung hinzu:

```java
package de.fmc.editor.event;

import de.fmc.editor.state.EditorState;
import java.util.UUID;

public sealed interface EditorEvent {
    record SelectObject(UUID id, boolean clearPrevious) implements EditorEvent {}
    record ClearSelection() implements EditorEvent {}
    record DeleteObject(UUID id) implements EditorEvent {}
    record ChangeState(EditorState newState) implements EditorEvent {}

    // --- NEU für Drag & Drop ---
    record MoveObject(UUID id, double deltaX, double deltaY) implements EditorEvent {}
    record CommitMove(UUID id) implements EditorEvent {} // Optional: Für Undo/Redo-History
}
```

---

## 2. Anpassung in `InteractionEventData.java`

Wir fügen ein Flag hinzu, um Drag-Events zu unterscheiden:

```java
package de.fmc.editor.state;

public record InteractionEventData(
    double worldX,
    double worldY,
    boolean isPrimaryClick,
    boolean isDragging,      // NEU: true während die Maus gezogen wird
    boolean isMouseReleased, // NEU: true wenn die Maustaste losgelassen wird
    LogicalKey key,
    boolean isShiftDown
) {}
```

---

## 3. Der neue `MoveObjectState.java`

Dieser State übernimmt die Kontrolle, sobald ein Objekt gezogen wird. Er berechnet die Relativbewegung (*Delta*) und feuert kontinuierlich `MoveObject`-Events.

```java
package de.fmc.editor.state;

import de.fmc.editor.event.EditorEvent.*;
import de.fmc.editor.event.EventBus;
import java.util.UUID;

public class MoveObjectState implements EditorState {
    private final EventBus eventBus;
    private final UUID targetObjectId;
    
    private double lastX;
    private double lastY;

    public MoveObjectState(EventBus eventBus, UUID targetObjectId, double startX, double startY) {
        this.eventBus = eventBus;
        this.targetObjectId = targetObjectId;
        this.lastX = startX;
        this.lastY = startY;
    }

    @Override
    public void handleInput(InteractionEventData event, EditorReadContext context) {
        // 1. Abbruch über ESCAPE
        if (event.key() == LogicalKey.ESCAPE) {
            // (Hier könnte ein Reset-Event gefeuert werden, um das Objekt zur Startposition zurückzusetzen)
            eventBus.publish(new ChangeState(new IdleState(eventBus)));
            return;
        }

        // 2. Drag-Bewegung verarbeiten
        if (event.isDragging()) {
            double deltaX = event.worldX() - lastX;
            double deltaY = event.worldY() - lastY;

            // Optional: Grid-Snapping Logik
            if (context.isSnapToGrid()) {
                // Hier könnte man Snapping-Mathematik anwenden
            }

            eventBus.publish(new MoveObject(targetObjectId, deltaX, deltaY));

            this.lastX = event.worldX();
            this.lastY = event.worldY();
            return;
        }

        // 3. Maustaste losgelassen -> Drag beendet
        if (event.isMouseReleased()) {
            eventBus.publish(new CommitMove(targetObjectId));
            eventBus.publish(new ChangeState(new IdleState(eventBus)));
        }
    }
}
```

---

## 4. `IdleState.java` anpassen

Im `IdleState` reagieren wir jetzt auf das Drag-Signal und wechseln bei Treffer in den `MoveObjectState`:

```java
package de.fmc.editor.state;

import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.event.EditorEvent.*;
import de.fmc.editor.event.EventBus;

public class IdleState implements EditorState {
    private final EventBus eventBus;

    public IdleState(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void handleInput(InteractionEventData event, EditorReadContext context) {
        // Tastatur-Shortcuts
        if (event.key() == LogicalKey.DELETE) {
            for (var id : context.getSelectedObjectIds()) {
                eventBus.publish(new DeleteObject(id));
            }
            eventBus.publish(new ClearSelection());
            return;
        }

        if (event.key() == LogicalKey.ESCAPE) {
            eventBus.publish(new ClearSelection());
            return;
        }

        // NEU: Drag-Start erkennen
        if (event.isDragging()) {
            FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
            if (hit != null) {
                // Falls das Objekt noch nicht selektiert ist, selektieren
                if (!context.getSelectedObjectIds().contains(hit.id())) {
                    eventBus.publish(new SelectObject(hit.id(), true));
                }
                // Wechsel in den MoveObjectState
                eventBus.publish(new ChangeState(
                    new MoveObjectState(eventBus, hit.id(), event.worldX(), event.worldY())
                ));
            }
            return;
        }

        // Reiner Klick
        if (event.isPrimaryClick()) {
            FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
            if (hit != null) {
                boolean keepSelection = event.isShiftDown();
                eventBus.publish(new SelectObject(hit.id(), !keepSelection));
            } else {
                eventBus.publish(new ClearSelection());
            }
        }
    }
}
```

---

## 5. Updates im UI-Layer (`CanvasController.java`)

Wir passen das `FmcObject` (Positions-Update) an, registrieren das `MoveObject`-Event im `EventBus` und erweitern die JavaFX Event-Listener:

### A) Event Handler Registrieren & Objekt bewegen:
Füge diese Methode/Logik in `CanvasController.java` hinzu:

```java
private void setupEventSubscriptions() {
    // ... bisherige Subscriptions ...

    // NEU: Auf Verschiebung reagieren
    eventBus.subscribe(MoveObject.class, e -> {
        for (int i = 0; i < objects.size(); i++) {
            FmcObject obj = objects.get(i);
            if (obj.id().equals(e.id())) {
                // Objekt mit neuen Koordinaten ersetzen (da Record unveränderlich ist)
                FmcObject movedObj = new FmcObject(
                    obj.id(), obj.name(), 
                    obj.x() + e.deltaX(), obj.y() + e.deltaY(), 
                    obj.width(), obj.height()
                );
                objects.set(i, movedObj);
                break;
            }
        }
        redraw();
    });
}
```

### B) JavaFX Mouse-Events in `CanvasController.java` erweitern:
Ersetze die `MOUSE_CLICKED`-Registrierung durch MOUSE_PRESSED, MOUSE_DRAGGED und MOUSE_RELEASED:

```java
private void setupFXListeners() {
    // 1. Mouse Pressed (Klick-Start)
    canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, fx -> {
        canvas.requestFocus();
        InteractionEventData data = new InteractionEventData(
            fx.getX(), fx.getY(), true, false, false, LogicalKey.NONE, fx.isShiftDown()
        );
        currentState.handleInput(data, this);
    });

    // 2. Mouse Dragged (Zieh-Bewegung)
    canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, fx -> {
        InteractionEventData data = new InteractionEventData(
            fx.getX(), fx.getY(), false, true, false, LogicalKey.NONE, fx.isShiftDown()
        );
        currentState.handleInput(data, this);
    });

    // 3. Mouse Released (Loslassen)
    canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, fx -> {
        InteractionEventData data = new InteractionEventData(
            fx.getX(), fx.getY(), false, false, true, LogicalKey.NONE, fx.isShiftDown()
        );
        currentState.handleInput(data, this);
    });

    // Key-Listener bleibt wie zuvor...
}
```

---

## 🎯 Was du nun im PoC machen kannst

1. Starte die Anwendung wieder mit `mvn clean javafx:run`.
2. Klicke auf eine Box und **halte die Maustaste gedrückt, während du die Maus bewegst**.
3. Der `IdleState` erkennt die Bewegung, übergibt fließend an den `MoveObjectState`, und die Box folgt deiner Maus in Echtzeit!
4. Sobald du die Maustaste loslässt, wechselt der Zustand automatisch wieder zurück in den `IdleState`.

**Das Schöne daran:** Der `MoveObjectState` hat wie der `IdleState` keinerlei Bezug zu JavaFX. Du könntest diesen Drag-Vorgang in einem Unit-Test simulieren, indem du dem `MoveObjectState` einfach 3-4 `InteractionEventData`-Objekte mit veränderten X/Y-Koordinaten übergibst und verifizierst, dass die richtigen `MoveObject`-Events gefeuert werden.

---

# you asked

message time: 2026-08-04 20:43:22

das is schon cool und jetzt noch mit InteractionMap bitte damit das noch schoehner wird. Danke dir.

---

# gemini response

Das macht das Design tatsächlich noch einmal deutlich eleganter! 

Indem wir die `InteractionMap` einbinden, befreien wir den `IdleState` und `MoveObjectState` von unübersichtlichen `if/switch`-Kaskaden für Tastatur-Eingaben. Die Map fungiert nun als **Kopplung zwischen Tastenkombinationen und fachlichen Intentions (Events)** – und bleibt dabei dank `LogicalKey` zu **100 % JavaFX-frei**.

Hier ist die Ausarbeitung der `InteractionMap` und wie sie sauber in die States integriert wird.

---

## 1. Die entkoppelte `InteractionMap.java`

Wir erstellen ein kleines Schlüssel-Objekt `KeyShortcut` für das Lookup und binden daran die passenden Event-Producer (`Supplier<EditorEvent>`):

```java
package de.fmc.editor.state;

import de.fmc.editor.event.EditorEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class InteractionMap {

    // Record für den Map-Key (vollständig ohne JavaFX)
    public record KeyShortcut(LogicalKey key, boolean isShiftDown) {}

    private final Map<KeyShortcut, Supplier<EditorEvent>> bindings = new HashMap<>();

    /**
     * Registriert eine Tasten-Funktion.
     */
    public void bind(LogicalKey key, boolean isShiftDown, Supplier<EditorEvent> eventSupplier) {
        bindings.put(new KeyShortcut(key, isShiftDown), eventSupplier);
    }

    /**
     * Überprüft, ob für das eingehende Event eine Aktion hinterlegt ist.
     */
    public Optional<EditorEvent> getEventFor(InteractionEventData event) {
        if (event.key() == null || event.key() == LogicalKey.NONE) {
            return Optional.empty();
        }
        KeyShortcut shortcut = new KeyShortcut(event.key(), event.isShiftDown());
        Supplier<EditorEvent> supplier = bindings.get(shortcut);
        
        return Optional.ofNullable(supplier).map(Supplier::get);
    }
}
```

---

## 2. Der `IdleState` mit `InteractionMap`

Im Konstruktor des `IdleState` definieren wir jetzt prägnant, welche Taste welches Event auslösen soll. Die `handleInput`-Methode prüft erst die `InteractionMap`, bevor sie auf Maus-Aktionen eingeht:

```java
package de.fmc.editor.state;

import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.event.EditorEvent.*;
import de.fmc.editor.event.EventBus;
import java.util.Optional;

public class IdleState implements EditorState {
    private final EventBus eventBus;
    private final InteractionMap keyBindings = new InteractionMap();

    public IdleState(EventBus eventBus) {
        this.eventBus = eventBus;
        setupKeyBindings();
    }

    private void setupKeyBindings() {
        // Tastatur-Shortcuts zentral und lesbar konfigurieren:
        keyBindings.bind(LogicalKey.ESCAPE, false, ClearSelection::new);
        
        // Bei DELETE feuern wir die Objekt-Löschung (über ein Lambda für dynamische IDs)
        // Hinweis: Da wir hier zur Bind-Zeit noch keine Context-Daten haben, 
        // wird bei spezieller Kontext-Logik im Handler das Mapping genutzt oder direkt auf getEventFor zurückgegriffen.
    }

    @Override
    public void handleInput(InteractionEventData event, EditorReadContext context) {
        // 1. Tastatur-Handling via InteractionMap
        Optional<EditorEvent> mappedEvent = keyBindings.getEventFor(event);
        if (mappedEvent.isPresent()) {
            eventBus.publish(mappedEvent.get());
            return;
        }

        // Spezialfall DELETE (benötigt Kontext aus dem ReadContext):
        if (event.key() == LogicalKey.DELETE) {
            for (var id : context.getSelectedObjectIds()) {
                eventBus.publish(new DeleteObject(id));
            }
            eventBus.publish(new ClearSelection());
            return;
        }

        // 2. Drag-Start erkennen (Wechsel in MoveObjectState)
        if (event.isDragging()) {
            FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
            if (hit != null) {
                if (!context.getSelectedObjectIds().contains(hit.id())) {
                    eventBus.publish(new SelectObject(hit.id(), true));
                }
                eventBus.publish(new ChangeState(
                    new MoveObjectState(eventBus, hit.id(), event.worldX(), event.worldY())
                ));
            }
            return;
        }

        // 3. Reiner Klick (Selektion)
        if (event.isPrimaryClick()) {
            FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
            if (hit != null) {
                boolean keepSelection = event.isShiftDown();
                eventBus.publish(new SelectObject(hit.id(), !keepSelection));
            } else {
                eventBus.publish(new ClearSelection());
            }
        }
    }
}
```

---

## 3. Der `MoveObjectState` mit `InteractionMap`

Auch der `MoveObjectState` profitiert von der `InteractionMap`, um Abbruch-Tasten wie `ESCAPE` extrem sauber auszuwerten:

```java
package de.fmc.editor.state;

import de.fmc.editor.event.EditorEvent.*;
import de.fmc.editor.event.EventBus;
import java.util.Optional;
import java.util.UUID;

public class MoveObjectState implements EditorState {
    private final EventBus eventBus;
    private final UUID targetObjectId;
    private final InteractionMap keyBindings = new InteractionMap();
    
    private double lastX;
    private double lastY;

    public MoveObjectState(EventBus eventBus, UUID targetObjectId, double startX, double startY) {
        this.eventBus = eventBus;
        this.targetObjectId = targetObjectId;
        this.lastX = startX;
        this.lastY = startY;

        // ESCAPE bricht das Dragen ab und bringt uns zurück in den IdleState
        keyBindings.bind(LogicalKey.ESCAPE, false, () -> new ChangeState(new IdleState(eventBus)));
    }

    @Override
    public void handleInput(InteractionEventData event, EditorReadContext context) {
        // 1. Prüfen ob ein Shortcut gepasst hat (z. B. ESCAPE)
        Optional<EditorEvent> mappedEvent = keyBindings.getEventFor(event);
        if (mappedEvent.isPresent()) {
            eventBus.publish(mappedEvent.get());
            return;
        }

        // 2. Drag-Bewegung verarbeiten
        if (event.isDragging()) {
            double deltaX = event.worldX() - lastX;
            double deltaY = event.worldY() - lastY;

            eventBus.publish(new MoveObject(targetObjectId, deltaX, deltaY));

            this.lastX = event.worldX();
            this.lastY = event.worldY();
            return;
        }

        // 3. Maustaste losgelassen -> Drag beenden
        if (event.isMouseReleased()) {
            eventBus.publish(new CommitMove(targetObjectId));
            eventBus.publish(new ChangeState(new IdleState(eventBus)));
        }
    }
}
```

---

## Warum das Architektur-Bild jetzt perfekt ist:

1. **Deklarativ statt Imperativ:** Tastenkombinationen stehen übersichtlich als `bind(...)` am Anfang der State-Klassen.
2. **Erweiterbarkeit:** Wenn du z. B. `STRG + Z` für Undo einbauen möchtest, fügst du in der Map einfach eine Zeile `keyBindings.bind(LogicalKey.Z, true, UndoEvent::new);` hinzu.
3. **Null JavaFX-Lecks:** Die Übersetzung von JavaFX-Keys geschieht weiterhin transparent im `CanvasController`, der Core bleibt isoliert und leicht zu testen.

---

# you asked

message time: 2026-08-04 20:55:12

Wenn wir schon ein Canvas haben dann auch noch einen View oder wie bei mit einen ViewMapper. Dann glaube ich dass man kein redraw mehr braucht oder?

---

# gemini response

Das ist ein **wichtiger Denkfehler**, der beim Übergang von einer Knoten-basierten (Node-based) UI zu einer Canvas-basierten UI sehr häufig auftritt!

Ob du eine `redraw()`-Methode brauchst oder nicht, hängt primär **vom verwendeten FX-Element** ab, nicht von der Existenz des `ViewMappers`:

---

## 1. Warum ein Canvas *immer* ein `redraw()` braucht

Ein `javafx.scene.canvas.Canvas` ist wie ein **physisches Ölgemälde auf einer Leinwand**:

* Es speichert **keine Unter-Objekte** (wie `Rectangle` oder `Text`). Es kennt nur Farbpixel.
* Wenn sich ein Domain-Objekt verschiebt, weiß das Canvas nicht, wo das Objekt früher war.
* Du musst die Leinwand mit `clearRect()` komplett weiß wischen und **alles neu zeichnen** (`redraw()`), damit keine "Schmier-Spuren" des alten Zustands übrig bleiben.

Der `ViewMapper` hat beim Canvas lediglich die Aufgabe, die mathematischen Domain-Daten (z. B. `FmcObject` mit Weltkoordinaten) in finale Zeichenanweisungen (z. B. Bildschirmkoordinaten, Zoom, Farben) für das `GraphicsContext` umzurechnen. Das eigentliche Übermalen der Leinwand bleibt notwendig.

---

## 2. Wann bräuchtest du KEIN `redraw()` mehr?

Du bräuchtest **kein** `redraw()`, wenn du statt des Canvas eine **`Pane` (Szenengraph-Knoten)** nutzt!

Wenn deine `ViewMapper`-Klasse für jedes `FmcObject` ein JavaFX-Node (z. B. `javafx.scene.shape.Rectangle`) in eine `Pane` einfügt, übernimmt JavaFX das Zeichnen intern:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Zwei Wege der Darstellung                      │
├───────────────────────────────────┬────────────────────────────────────┤
│  Canvas + ViewMapper              │  Pane + ViewMapper (Node-basiert)  │
├───────────────────────────────────┼────────────────────────────────────┤
│ • 1 einzelner JavaFX-Node         │ • Jedes Objekt ist ein JavaFX-Node │
│ • Super schnell bei 10.000+ Items │ • Braucht VIEL Speicher bei 1.000+ │
│ • Braucht MANUELLES redraw()      │ • KEIN redraw() nötig!             │
│   bei JEDER Änderung              │   (FX updated Nodes automatisch)   │
└───────────────────────────────────┴────────────────────────────────────┘
```

---

## 3. Wie die saubere Rolle des `ViewMapper` beim Canvas aussieht

Wenn du beim performanten **Canvas** bleibst, nimmt der `ViewMapper` dem `CanvasController` das komplexe Zeichnen ab. Der `ViewMapper` kümmert sich um Kamera, Zoom, Gitterlinien und das Rendern der Objekte.

### Der `ViewMapper` übernimmt das Zeichnen:

```java
package de.fmc.editor.ui;

import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.SelectionModel;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;

public class ViewMapper {
    private final Canvas canvas;

    public ViewMapper(Canvas canvas) {
        this.canvas = canvas;
    }

    /**
     * Diese Methode ERSETZT das alte canvasController.redraw()!
     * Sie wird aufgerufen, wenn Events eintreffen.
     */
    public void render(List<FmcObject> objects, SelectionModel selectionModel) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // 1. Leinwand leeren
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 2. Gitter / Hintergrund zeichnen (falls vorhanden)
        drawGrid(gc);

        // 3. Domain-Objekte als Canvas-Formen mappen & zeichnen
        for (FmcObject obj : objects) {
            boolean isSelected = selectionModel.isSelected(obj.id());
            
            // Mapper übersetzt Domain-Zustand in konkrete Farben/Formen:
            gc.setFill(isSelected ? Color.LIGHTBLUE : Color.LIGHTGRAY);
            gc.fillRect(obj.x(), obj.y(), obj.width(), obj.height());

            gc.setStroke(isSelected ? Color.BLUE : Color.BLACK);
            gc.setLineWidth(isSelected ? 2.5 : 1.0);
            gc.strokeRect(obj.x(), obj.y(), obj.width(), obj.height());

            gc.setFill(Color.BLACK);
            gc.fillText(obj.name(), obj.x() + 8, obj.y() + 20);
        }
    }

    private void drawGrid(GraphicsContext gc) {
        // Optional: Canvas-Hintergrundgitter zeichnen...
    }
}
```

### Der `CanvasController` wird dadurch noch schlanker:

Im `CanvasController` schrumpft die Aufruf-Logik auf eine einzige saubere Zeile `viewMapper.render(...)`:

```java
private void setupEventSubscriptions() {
    eventBus.subscribe(SelectObject.class, e -> {
        if (e.clearPrevious()) selectionModel.clear();
        selectionModel.select(e.id());
        
        // Statt redraw() ruft der Controller den ViewMapper auf:
        viewMapper.render(objects, selectionModel);
    });

    eventBus.subscribe(MoveObject.class, e -> {
        // ... Objekt-Koordinaten anpassen ...
        
        // Szene neu rendern über den ViewMapper:
        viewMapper.render(objects, selectionModel);
    });
}
```

---

## Fazit

* **Nein, das Neuzeichnen entfällt bei einem Canvas nicht.** 
* **Aber:** Die Verantwortlichkeit wandert aus dem `CanvasController` heraus. 
* Das eigentliche *„Wie zeichne ich ein `FmcObject` auf das Canvas?“* liegt jetzt zu 100 % isoliert im **`ViewMapper`**. Der Controller schubst diesen bei Events nur noch mit `viewMapper.render(...)` an!

---
Powered by [AI Exporter](https://saveai.net)
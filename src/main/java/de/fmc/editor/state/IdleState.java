package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.command.AddWaypointCommand;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;

import java.util.UUID;

public class IdleState implements EditorState {

    @Override
    public void enterState(CanvasController context) {
    }

    @Override
    public void exitState(CanvasController context) {
    }

    @Override
    public void handleMousePressed(MouseEventData event, CanvasController context) {
        FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
        var selectedIds = context.getSelectedObjectIds();

        if (hit != null) {
            // Wenn wir ein normales Objekt (kein Wegpunkt) anklicken, blenden wir die Wegpunkte aus
            // (außer der Benutzer hat den Haken in der Toolbar gesetzt)
            if (hit.type() != FmcType.WEGPUNKT && !context.getToolbarController().isWaypointsVisible()) {
                context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, false);
            }

            // --- MULTISELECT LOGIK ---
            if (event.isControlDown()) {
                // CTRL gehalten -> Toggle-Verhalten
                if (selectedIds.contains(hit.id())) {
                    selectedIds.remove(hit.id());
                } else {
                    selectedIds.add(hit.id());
                }
            } else {
                // Kein CTRL -> Normale Einzel-Selektion (vorherige leeren)
                if (!selectedIds.contains(hit.id())) {
                    selectedIds.clear();
                    selectedIds.add(hit.id());
                }
            }
            
            // UI updaten (Schatten/Blur zeichnen)
            context.updateSelectionInView();

            if (event.clickCount() == 2) {
                // Doppel-Klick wechselt in den Resize-Modus
                context.setCurrentState(new ResizeState(hit.id()));
            } else {
                // In den Drag-Zustand wechseln
                context.setCurrentState(new DragObjectsState(hit.id(), event, context));
            }
        } else {
            // Prüfen, ob eine Verbindung getroffen wurde
            UUID clickedConnectionId = context.findConnectionAt(event.sceneX(), event.sceneY());

            if (clickedConnectionId != null) {
                if (event.clickCount() == 2) {
                    // DOPPELKLICK -> Neuen Wegpunkt hinzufügen
                    var waypoint = FmcFactory.createObject(
                        FmcType.WEGPUNKT,
                        event.worldX(), event.worldY(),
                        CoreRegistry.WAYPOINT_LAYER_ID
                    );

                    var cmd = new AddWaypointCommand(
                        context.getRegistry(), clickedConnectionId, waypoint
                    );
                    context.getCommandHistory().executeCommand(cmd);

                    // Layer sicherheitshalber sichtbar schalten
                    context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, true);
                } else {
                    // Einfacher Klick -> Wegpunkte einblenden
                    context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, true);
                }
            } else {
                // Klick ins Leere
                if (event.isControlDown()) {
                    // Gummiband starten
                    context.setCurrentState(new BoxSelectionState(event, context));
                } else {
                    // Alles abwählen und Panning starten
                    selectedIds.clear();
                    context.updateSelectionInView();
                    
                    if (!context.getToolbarController().isWaypointsVisible()) {
                        context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, false);
                    }
                    context.setCurrentState(new PanningState(event, context));
                }
            }
        }
    }

    @Override
    public void handleMouseDragged(MouseEventData event, CanvasController context) {
    }

    @Override
    public void handleMouseReleased(MouseEventData event, CanvasController context) {
    }
}

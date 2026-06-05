import json
import os
import colorsys

json_file_path = "graph.json"
dot_file_path = "class_focus_graph.dot"

if not os.path.exists(json_file_path):
    print(f"Fehler: '{json_file_path}' nicht gefunden.")
    exit(1)

print("Lese JSON-Datei ein...")
with open(json_file_path, "r", encoding="utf-8") as f:
    data = json.load(f)

nodes = data.get("nodes", [])
edges = data.get("links") or data.get("edges") or []

# 1. Alle verfügbaren Quelldateien (Klassen) sammeln, um sie dem Benutzer anzuzeigen
source_files = sorted(list(set(node.get("source_file") for node in nodes if node.get("source_file"))))

print("\nVerfügbare Klassen/Dateien im Projekt:")
for i, file_path in enumerate(source_files, start=1):
    # Wir zeigen den gekürzten Namen an, damit es übersichtlich bleibt
    short_name = file_path.split("/")[-1]
    print(f" [{i}] {short_name}  ({file_path})")

# 2. Benutzerauswahl abfragen
try:
    auswahl = int(input("\nVon welcher Klasse möchtest du das Umfeld extrahieren? (Nummer eingeben): "))
    if not (1 <= auswahl <= len(source_files)):
        raise ValueError
    target_class_file = source_files[auswahl - 1]
except (ValueError, IndexError):
    print("Ungültige Auswahl. Beende Programm.")
    exit(1)

print(f"\n-> Extrahiere Umgebung für: {target_class_file}")

# 3. Alle IDs der Knoten sammeln, die DIREKT zu dieser Klasse gehören
class_node_ids = set(node.get("id") for node in nodes if node.get("source_file") == target_class_file)

# 4. Kanten filtern: Wir wollen alle Kanten, bei denen Source ODER Target zu unserer Klasse gehören
focused_edges = []
neighbor_node_ids = set()
relations_in_use = set()

for edge in edges:
    source = edge.get("source")
    target = edge.get("target")
    relation = edge.get("relation", "unbekannt")

    # Ist die Kante mit unserer Klasse verbunden?
    source_is_in_class = source in class_node_ids
    target_is_in_class = target in class_node_ids

    if source_is_in_class or target_is_in_class:
        focused_edges.append(edge)
        relations_in_use.add(relation)

        # Merk dir die Nachbarknoten außerhalb der Klasse
        if source: neighbor_node_ids.add(source)
        if target: neighbor_node_ids.add(target)

# Alle Knoten bestimmen, die gezeichnet werden müssen (Klasse selbst + direkte Nachbarn)
all_required_node_ids = class_node_ids.union(neighbor_node_ids)

# 5. Dynamische Farbberechnung für die vorkommenden Relationen (Dark Mode Optimiert)
relations_list = sorted(list(relations_in_use))
relationen_farb_mapping = {}
for idx, rel in enumerate(relations_list):
    if relations_list:
        hue = idx / len(relations_list)
        r, g, b = colorsys.hls_to_rgb(hue, 0.65, 0.85)
        relationen_farb_mapping[rel] = f"#{int(r*255):02x}{int(g*255):02x}{int(b*255):02x}"

# 6. DOT-Struktur aufbauen
dot_lines = [
    "digraph ClassFocusGraph {",
    "    bgcolor=\"#121212\";",
    "    rankdir=LR;",
    "    pad=\"0.5\";",
    "    # Standard-Styling für externe Knoten",
    "    node [shape=box, style=\"filled,rounded\", color=\"#444444\", fillcolor=\"#1e1e1e\", fontcolor=\"#aaaaaa\", fontname=\"Arial\", fontsize=10];",
    "    edge [fontname=\"Arial\", fontsize=9, penwidth=1.5];",
    ""
]

# Legende hinzufügen
dot_lines.append("    # LEGENDE")
dot_lines.append("    subgraph cluster_legend {")
dot_lines.append("        label=\"Legende (Relationen)\";")
dot_lines.append("        fontname=\"Arial\"; fontsize=11; color=\"#444444\"; fontcolor=\"#aaaaaa\"; style=\"dashed\";")
for rel in relations_list:
    farbe = relationen_farb_mapping[rel]
    dot_lines.append(f"        \"leg_{rel}_s\" [label=\"\", style=invis, width=0, height=0];")
    dot_lines.append(f"        \"leg_{rel}_t\" [label=\"\", style=invis, width=0, height=0];")
    dot_lines.append(f"        \"leg_{rel}_s\" -> \"leg_{rel}_t\" [label=\"{rel}\", color=\"{farbe}\", fontcolor=\"{farbe}\"];")
dot_lines.append("    }")
dot_lines.append("")

# Visuelle Trennung: Wir packen alle internen Elemente der Klasse in eine optische Box (Cluster)
dot_lines.append(f"    # Interne Struktur der Klasse")
dot_lines.append(f"    subgraph cluster_target_class {{")
dot_lines.append(f"        label=\"KLASSE: {target_class_file.split('/')[-1]}\";")
dot_lines.append(f"        fontname=\"Arial\"; fontsize=12; color=\"#1f77b4\"; fontcolor=\"#1f77b4\"; style=\"bold\";")
# Styling explizit für INTERNE Knoten schick machen
dot_lines.append(f"        node [fillcolor=\"#252525\", color=\"#1f77b4\", fontcolor=\"#ffffff\", style=\"filled,bold\"];")

for node in nodes:
    node_id = node.get("id")
    if node_id in class_node_ids:
        label = node.get("label", node_id).replace('"', '\\"')
        dot_lines.append(f"        \"{node_id}\" [label=\"{label}\"];")
dot_lines.append("    }")
dot_lines.append("")

# Externe Nachbar-Knoten definieren
dot_lines.append("    # Externe Abhängigkeiten")
for node in nodes:
    node_id = node.get("id")
    if node_id in neighbor_node_ids and node_id not in class_node_ids:
        label = node.get("label", node_id).replace('"', '\\"')
        # Herkunft herausfinden, um sie im Label anzuzeigen
        src_f = node.get("source_file", "").split("/")[-1]
        display_label = f"{label}\\n({src_f})" if src_f else label
        dot_lines.append(f"    \"{node_id}\" [label=\"{display_label}\"];")

dot_lines.append("")

# Kanten zeichnen
for edge in focused_edges:
    source = edge.get("source")
    target = edge.get("target")
    relation = edge.get("relation", "unbekannt")

    farbe = relationen_farb_mapping.get(relation, "#888888")
    edge_style = f" [label=\"{relation}\", color=\"{farbe}\", fontcolor=\"{farbe}\"]"
    dot_lines.append(f"    \"{source}\" -> \"{target}\"{edge_style};")

dot_lines.append("}")

# Speichern
with open(dot_file_path, "w", encoding="utf-8") as f:
    f.write("\n".join(dot_lines))

print(f"\nUmfeld-Graph erfolgreich erstellt unter: {dot_file_path}")
print("Generiere das Bild via:")
print(f"  dot -Tpng {dot_file_path} -o class_focus.png")

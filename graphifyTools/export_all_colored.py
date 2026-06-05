import json
import os
import colorsys

# Konfiguration
json_file_path = "graph.json"
dot_file_path = "call_graph_continuous.dot"
STANDARD_FARBE = "#7f7f7f"

if not os.path.exists(json_file_path):
    print(f"Fehler: Die Datei '{json_file_path}' wurde nicht gefunden.")
    exit(1)

print("Lese JSON-Datei ein...")
with open(json_file_path, "r", encoding="utf-8") as f:
    data = json.load(f)

edges = data.get("links") or data.get("edges") or []

# Relationen analysieren und zählen
relations_count = {}
for edge in edges:
    rel = edge.get("relation", "unbekannt")
    relations_count[rel] = relations_count.get(rel, 0) + 1

# Interaktive Ausgabe der gefundenen Relationen
print("\nFolgende Relationen wurden gefunden:")
available_relations = list(relations_count.keys())
for i, rel in enumerate(available_relations, start=1):
    print(f" [{i}] {rel} ({relations_count[rel]}x)")
print(" [A] ALLE Relationen exportieren")

auswahl_input = input("\nDeine Auswahl (kommagetrennt oder 'A'): ").strip()

erlaubte_relationen = []
if auswahl_input.upper() == 'A' or auswahl_input == '':
    erlaubte_relationen = available_relations
else:
    try:
        indizes = [int(x.strip()) - 1 for x in auswahl_input.split(",")]
        for idx in indizes:
            if 0 <= idx < len(available_relations) and available_relations[idx] not in erlaubte_relationen:
                erlaubte_relationen.append(available_relations[idx])
    except ValueError:
        erlaubte_relationen = available_relations

# ==============================================================================
# ERWEITERUNG: ERRECHNETE FARBSKALA (HSL zu HEX)
# ==============================================================================
relationen_farb_mapping = {}
anzahl_relationen = len(erlaubte_relationen)

for idx, rel in enumerate(erlaubte_relationen):
    if anzahl_relationen > 0:
        # Wir teilen den Farbkreis (0.0 bis 1.0) gleichmäßig auf
        hue = idx / anzahl_relationen
        # Sättigung bei 75% und Helligkeit bei 45% für kräftige, gut lesbare Farben
        saturation = 0.75
        lightness = 0.45

        # Umrechnung in RGB (Werte von 0.0 bis 1.0)
        r, g, b = colorsys.hls_to_rgb(hue, lightness, saturation)

        # Umrechnung in Hex-Code (z.B. #FF0000)
        hex_color = f"#{int(r*255):02x}{int(g*255):02x}{int(b*255):02x}"
        relationen_farb_mapping[rel] = hex_color

# DOT-Header vorbereiten
dot_lines = [
    "digraph CallGraph {",
    "    rankdir=LR;",
    '    node [shape=box, style="filled,rounded", color="#2b2b2b", fillcolor="#f9f9f9", fontname="Arial", fontsize=11];',
    '    edge [fontname="Arial", fontsize=9, penwidth=1.5];',
    ""
]

# Legende generieren
dot_lines.append("    # LEGENDE")
dot_lines.append('    subgraph cluster_legend {')
dot_lines.append('        label="Legende (Relationen)";')
dot_lines.append('        fontname="Arial"; fontsize=12; color="#cccccc"; style="dashed";')
for rel in erlaubte_relationen:
    farbe = relationen_farb_mapping.get(rel, STANDARD_FARBE)
    dot_lines.append(f'        "leg_{rel}_s" [label="", style=invis, width=0, height=0];')
    dot_lines.append(f'        "leg_{rel}_t" [label="", style=invis, width=0, height=0];')
    dot_lines.append(f'        "leg_{rel}_s" -> "leg_{rel}_t" [label="{rel}", color="{farbe}", fontcolor="{farbe}"];')
dot_lines.append('    }')
dot_lines.append("")

# Kanten verarbeiten
genutzte_knoten = set()
kanten_output = []
erlaubte_set = set(erlaubte_relationen)

for edge in edges:
    source = edge.get("source")
    target = edge.get("target")
    relation = edge.get("relation", "unbekannt")

    if relation not in erlaubte_set:
        continue

    if source and target:
        farbe = relationen_farb_mapping.get(relation, STANDARD_FARBE)
        edge_style = f' [label="{relation}", color="{farbe}", fontcolor="{farbe}"]'
        kanten_output.append(f'    "{source}" -> "{target}"{edge_style};')
        genutzte_knoten.add(source)
        genutzte_knoten.add(target)

# Knoten verarbeiten
for node in data.get("nodes", []):
    node_id = node.get("id")
    if node_id in genutzte_knoten:
        label = node.get("label", node_id)
        clean_label = label.replace('"', '\\"')
        dot_lines.append(f'    "{node_id}" [label="{clean_label}"];')

dot_lines.append("")
dot_lines.extend(kanten_output)
dot_lines.append("}")

with open(dot_file_path, "w", encoding="utf-8") as f:
    f.write("\n".join(dot_lines))

print(f"\nDatei erfolgreich generiert: {dot_file_path}")

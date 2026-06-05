import json
import os
import colorsys

# Konfiguration
json_file_path = "graph.json"
dot_file_path = "call_graph_dark.dot"

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

# Interaktive Ausgabe
print("\nFolgende Relationen wurden gefunden:")
available_relations = list(relations_count.keys())
for i, rel in enumerate(available_relations, start=1):
    print(f" [{i}] {rel} ({relations_count[rel]}x)")
print(" [A] ALLE Relationen exportieren")

auswahl_input = input("\nDeine Auswahl für den Dark Mode (kommagetrennt oder 'A'): ").strip()

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
# ERRECHNETE FARBSKALA OPTIMIERT FÜR DARK MODE
# ==============================================================================
relationen_farb_mapping = {}
anzahl_relationen = len(erlaubte_relationen)

for idx, rel in enumerate(erlaubte_relationen):
    if anzahl_relationen > 0:
        hue = idx / anzahl_relationen
        # Höhere Sättigung (0.85) und angehobene Helligkeit (0.65) für dunklen Hintergrund
        saturation = 0.85
        lightness = 0.65

        r, g, b = colorsys.hls_to_rgb(hue, lightness, saturation)
        hex_color = f"#{int(r*255):02x}{int(g*255):02x}{int(b*255):02x}"
        relationen_farb_mapping[rel] = hex_color

# DOT-Header mit Dark-Mode Attributen vorbereiten
dot_lines = [
    "digraph CallGraph {",
    "    # GLOBAL DARK MODE SETTINGS",
    "    bgcolor=\"#121212\";",  # Dunkles Hintergrund-Areal
    "    rankdir=LR;",
    "    pad=\"0.5\";",
    "",
    "    # Augenschonende Knoten",
    "    node [",
    "        shape=box,",
    "        style=\"filled,rounded\",",
    "        color=\"#333333\",",         # Unaufdringlicher Rahmen
    "        fillcolor=\"#1e1e1e\",",     # Box-Hintergrund
    "        fontcolor=\"#e0e0e0\",",     # Sanftes Weiß für Text
    "        fontname=\"Arial\",",
    "        fontsize=11",
    "    ];",
    "",
    "    # Kanten",
    "    edge [",
    "        fontname=\"Arial\",",
    "        fontsize=9,",
    "        penwidth=1.6",
    "    ];",
    ""
]

# Legende generieren (Dark Mode Design)
dot_lines.append("    # LEGENDE (DARK MODE)")
dot_lines.append("    subgraph cluster_legend {")
dot_lines.append("        label=\"Legende (Relationen)\";")
dot_lines.append("        fontname=\"Arial\"; fontsize=12; color=\"#444444\"; fontcolor=\"#aaaaaa\"; style=\"dashed\";")
for rel in erlaubte_relationen:
    farbe = relationen_farb_mapping.get(rel, "#888888")
    dot_lines.append(f"        \"leg_{rel}_s\" [label=\"\", style=invis, width=0, height=0];")
    dot_lines.append(f"        \"leg_{rel}_t\" [label=\"\", style=invis, width=0, height=0];")
    dot_lines.append(f"        \"leg_{rel}_s\" -> \"leg_{rel}_t\" [label=\"{rel}\", color=\"{farbe}\", fontcolor=\"{farbe}\"];")
dot_lines.append("    }")
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
        farbe = relationen_farb_mapping.get(relation, "#888888")
        edge_style = f" [label=\"{relation}\", color=\"{farbe}\", fontcolor=\"{farbe}\"]"
        kanten_output.append(f"    \"{source}\" -> \"{target}\"{edge_style};")
        genutzte_knoten.add(source)
        genutzte_knoten.add(target)

# Knoten verarbeiten
for node in data.get("nodes", []):
    node_id = node.get("id")
    if node_id in genutzte_knoten:
        label = node.get("label", node_id)
        clean_label = label.replace('"', '\\"')
        dot_lines.append(f"    \"{node_id}\" [label=\"{clean_label}\"];")

dot_lines.append("")
dot_lines.extend(kanten_output)
dot_lines.append("}")

with open(dot_file_path, "w", encoding="utf-8") as f:
    f.write("\n".join(dot_lines))

print(f"\nDark-Mode Datei erfolgreich generiert: {dot_file_path}")
print("Befehl für das Bild:")
print(f"  dot -Tpng {dot_file_path} -o call_graph_dark.png")

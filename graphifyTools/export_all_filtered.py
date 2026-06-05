import json
import os

# Konfiguration
json_file_path = "graph.json"
dot_file_path = "call_graph_filtered.dot"

# Farbpalette für die verschiedenen Relationen (Graphviz-kompatible Farben)
# Falls du mehr/andere Relationen hast, kannst du die Palette hier erweitern
FARB_PALETTE = {
    "calls": "#1f77b4",        # Blau
    "method": "#2ca02c",       # Grün
    "references": "#ff7f0e",   # Orange
    "contains": "#9467bd",     # Lila
    "extends": "#d62728",      # Rot
    "implements": "#8c564b",   # Braun
    "variable": "#e377c2",     # Rosa
}
STANDARD_FARBE = "#7f7f7f"     # Grau für unbekannte Relationen

if not os.path.exists(json_file_path):
    print(f"Fehler: Die Datei '{json_file_path}' wurde nicht gefunden.")
    exit(1)

print("Lese JSON-Datei ein (das kann bei großen Dateien einen Moment dauern)...")
with open(json_file_path, "r", encoding="utf-8") as f:
    data = json.load(f)

edges = data.get("links") or data.get("edges") or []

# 1. ERWEITERUNG: Relationen analysieren und zählen
relations_count = {}
for edge in edges:
    rel = edge.get("relation", "unbekannt")
    relations_count[rel] = relations_count.get(rel, 0) + 1

# Ausgabe der gefundenen Relationen
print("\nFolgende Relationen wurden in der Datei gefunden:")
available_relations = list(relations_count.keys())
for i, rel in enumerate(available_relations, start=1):
    print(f" [{i}] {rel} ({relations_count[rel]}x)")

print(" [A] ALLE Relationen exportieren")

# Benutzereingabe abfragen
auswahl_input = input("\nBitte gib die Nummern der gewünschten Relationen ein (kommagetrennt, z.B. 1,2) oder 'A' für alle: ").strip()

erlaubte_relationen = set()
if auswahl_input.upper() == 'A' or auswahl_input == '':
    erlaubte_relationen = set(available_relations)
    print("-> Es werden ALLE Relationen exportiert.")
else:
    try:
        indizes = [int(x.strip()) - 1 for x in auswahl_input.split(",")]
        for idx in indizes:
            if 0 <= idx < len(available_relations):
                erlaubte_relationen.add(available_relations[idx])
        print(f"-> Filter aktiv für: {list(erlaubte_relationen)}")
    except ValueError:
        print("Ungültige Eingabe. Es werden alle Relationen exportiert.")
        erlaubte_relationen = set(available_relations)

# DOT-Header vorbereiten
dot_lines = [
    "digraph CallGraph {",
    "    rankdir=LR;",
    '    node [shape=box, style="filled,rounded", color="#2b2b2b", fillcolor="#f9f9f9", fontname="Arial", fontsize=11];',
    '    edge [fontname="Arial", fontsize=9, penwidth=1.5];',
    ""
]

# 2. ERWEITERUNG: Eine Legende im Graphen erzeugen
dot_lines.append("    # LEGENDE")
dot_lines.append('    subgraph cluster_legend {')
dot_lines.append('        label="Legende (Relationen)";')
dot_lines.append('        fontname="Arial"; fontsize=12; color="#cccccc"; style="dashed";')
for rel in erlaubte_relationen:
    farbe = FARB_PALETTE.get(rel, STANDARD_FARBE)
    # Wir erstellen unsichtbare Knoten, um die Kantenfarben in der Legende zu zeigen
    dot_lines.append(f'        "leg_{rel}_s" [label="", style=invis, width=0, height=0];')
    dot_lines.append(f'        "leg_{rel}_t" [label="", style=invis, width=0, height=0];')
    dot_lines.append(f'        "leg_{rel}_s" -> "leg_{rel}_t" [label="{rel}", color="{farbe}", fontcolor="{farbe}"];')
dot_lines.append('    }')
dot_lines.append("")

# Kanten filtern und Farbkodierung anwenden
genutzte_knoten = set()
kanten_output = []

for edge in edges:
    source = edge.get("source")
    target = edge.get("target")
    relation = edge.get("relation", "unbekannt")

    if relation not in erlaubte_relationen:
        continue

    if source and target:
        farbe = FARB_PALETTE.get(relation, STANDARD_FARBE)
        # Die Kante bekommt die zugewiesene Farbe (sowohl der Pfeil als auch der Text)
        edge_style = f' [label="{relation}", color="{farbe}", fontcolor="{farbe}"]'

        kanten_output.append(f'    "{source}" -> "{target}"{edge_style};')
        genutzte_knoten.add(source)
        genutzte_knoten.add(target)

# Knoten hinzufügen (nur die aktiven)
for node in data.get("nodes", []):
    node_id = node.get("id")
    if node_id in genutzte_knoten:
        label = node.get("label", node_id)
        clean_label = label.replace('"', '\\"')
        dot_lines.append(f'    "{node_id}" [label="{clean_label}"];')

dot_lines.append("")
dot_lines.extend(kanten_output)
dot_lines.append("}")

# In Datei schreiben
with open(dot_file_path, "w", encoding="utf-8") as f:
    f.write("\n".join(dot_lines))

print(f"\nErfolgreich konvertiert! Datei wurde gespeichert unter: {dot_file_path}")
print("Tipp zum Generieren des Bildes:")
print(f"  dot -Tpng {dot_file_path} -o call_graph.png")

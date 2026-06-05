import json

# Pfad zu deiner JSON-Datei und der gewünschten DOT-Ausgabe
json_file_path = "graph.json"
dot_file_path = "call_graph.dot"

with open(json_file_path, "r", encoding="utf-8") as f:
    data = json.load(f)

# Wir erstellen einen gerichteten Graphen (digraph)
dot_lines = ["digraph CallGraph {", "    # Graph-Styling für bessere Lesbarkeit", "    rankdir=LR;", "    node [shape=box, style=filled, color=\"#2b2b2b\", fillcolor=\"#f5f5f5\", fontname=\"Arial\"];", "    edge [fontname=\"Arial\", fontsize=10];", ""]

# 1. Knoten (Nodes) hinzufügen
# Wir nutzen die 'id' als eindeutigen Bezeichner und das 'label' für die Anzeige
node_map = {}
for node in data.get("nodes", []):
    node_id = node.get("id")
    label = node.get("label", node_id)
    # Bereinigung des Labels für DOT (Anführungszeichen maskieren)
    clean_label = label.replace('"', '\\"')

    # Optional: Community-basierte Einfärbung oder Zusatzinfos einbauen
    dot_lines.append(f'    "{node_id}" [label="{clean_label}"];')

dot_lines.append("") # Leerzeile zur Trennung

# 2. Kanten (Edges / Links) hinzufügen
# Je nachdem, ob das Feld in deiner JSON 'links' oder 'edges' heißt (hier prüfen wir beides)
edges = data.get("links") or data.get("edges") or []

for edge in edges:
    source = edge.get("source")
    target = edge.get("target")
    relation = edge.get("relation", "")

    if source and target:
        # Wenn eine Relation (z.B. "calls", "references") existiert, schreiben wir sie als Label an die Kante
        edge_label = f' [label="{relation}"]' if relation else ""
        dot_lines.append(f'    "{source}" -> "{target}"{edge_label};')

dot_lines.append("}")

# In die DOT-Datei schreiben
with open(dot_file_path, "w", encoding="utf-8") as f:
    f.write("\n".join(dot_lines))

print(f"Erfolgreich konvertiert! Die Datei wurde als '{dot_file_path}' gespeichert.")

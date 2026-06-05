import json
import hashlib

def generate_color(text):
    """Generiert eine reproduzierbare Farbe basierend auf dem Text."""
    hash_object = hashlib.md5(text.encode('utf-8'))
    return f"#{hash_object.hexdigest()[:6]}"

# 1. JSON-Datei laden
file_path = 'graph.json'
with open(file_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

# ID-zu-Knoten-Mapping aufbauen
nodes_map = {node['id']: node for node in data['nodes']}

# Vorab-Zuordnung: Welche Methode gehört zu welcher Klasse?
method_to_class_label = {}
for link in data['links']:
    if link.get('relation') == 'method':
        source_id = link['source']
        target_id = link['target']
        if source_id in nodes_map:
            method_to_class_label[target_id] = nodes_map[source_id].get('label', '')

# Ergänzung über ID-Präfixe für Methoden (falls die explizite Kante fehlt)
for node_id, node in nodes_map.items():
    node_label = node.get('label', '')
    if node_label.startswith('.') and node_id not in method_to_class_label:
        for potential_class_id, potential_class_node in nodes_map.items():
            p_label = potential_class_node.get('label', '')
            if not (p_label.endswith('.java') or p_label.endswith('.py') or p_label.startswith('.')):
                if node_id.startswith(potential_class_id):
                    method_to_class_label[node_id] = p_label

# ==========================================
# SCHRITT 1: Auswahl der Fokus-Klassen (MEHRFACHAUSWAHL)
# ==========================================
labels_set = set()
for node in data['nodes']:
    lbl = node.get('label', '')
    if lbl:
        # Säubern für die eindeutige Auswahlliste
        if lbl.endswith('.java'): lbl = lbl[:-5]
        elif lbl.endswith('.py'): lbl = lbl[:-3]

        if not (lbl.startswith('.') or lbl in ['Override', 'FXML', 'Test', 'String', 'UUID']):
            labels_set.add(lbl)

eindeutige_klassen = sorted(list(labels_set))

print("\n=== SCHRITT 1: Klassen / Logische Komponenten auswählen ===")
for index, label in enumerate(eindeutige_klassen, start=1):
    print(f"[{index:2d}] {label}")

print("\nBitte geben Sie die Nummern der Klassen ein, die Sie betrachten möchten.")
print("Beispiel für mehrere: 1, 3, 5 (oder drücken Sie Enter für ALLE)")
auswahl_klasse_input = input("Ihre Auswahl: ").strip()

ziel_labels = []
if not auswahl_klasse_input:
    ziel_labels = eindeutige_klassen
else:
    try:
        indizes = [int(x.strip()) for x in auswahl_klasse_input.split(',')]
        ziel_labels = [eindeutige_klassen[i-1] for i in indizes if 0 < i <= len(eindeutige_klassen)]
    except (ValueError, IndexError):
        print("Ungültige Eingabe, es werden standardmäßig ALLE Klassen ausgewählt.")
        ziel_labels = eindeutige_klassen

print(f"\n=> Ausgewählte Fokus-Komponenten: {ziel_labels}")

# ==========================================
# SCHRITT 2: OPTIMIERTE VERSCHMELZUNGS-LOGIK
# ==========================================
id_to_unified_cluster = {}

for node_id, node in nodes_map.items():
    lbl = node.get('label', '')

    # Dateiendungen sofort entfernen, um Datei und Klasse/Objekt gleichzusetzen
    if lbl.endswith('.java'):
        lbl = lbl[:-5]
    elif lbl.endswith('.py'):
        lbl = lbl[:-3]

    # Fall A: Es ist eine Methode (z.B. .initialize())
    if lbl.startswith('.'):
        parent_class = method_to_class_label.get(node_id, '')
        if parent_class.endswith('.java'): parent_class = parent_class[:-5]
        elif parent_class.endswith('.py'): parent_class = parent_class[:-3]

        if parent_class:
            id_to_unified_cluster[node_id] = f"{parent_class}{lbl}"
        else:
            id_to_unified_cluster[node_id] = lbl

    # Fall B: Es ist die Klasse selbst, die Datei oder eine Objekt-Referenz
    else:
        matched_class = None
        if lbl in eindeutige_klassen:
            matched_class = lbl
        else:
            # WICHTIG: Nach Länge absteigend sortieren, damit "RegistryListener"
            # vor "Registry" gematcht wird!
            for cls in sorted(eindeutige_klassen, key=len, reverse=True):
                if cls.lower() in node_id.lower():
                    matched_class = cls
                    break

        if matched_class:
            id_to_unified_cluster[node_id] = matched_class
        else:
            id_to_unified_cluster[node_id] = lbl

# Alle IDs bestimmen, die zu der Gruppe unserer ausgewählten Fokus-Klassen gehören
fokus_unified_names = set(ziel_labels)
# Auch Methoden der gewählten Fokus-Klassen gehören zum Fokus-Bereich
for node_id, unified_name in id_to_unified_cluster.items():
    for zl in ziel_labels:
        if unified_name.startswith(zl + "."):
            fokus_unified_names.add(unified_name)

# ==========================================
# SCHRITT 3: Verfügbare Relationen filtern
# ==========================================
potenzielle_links = []
existierende_relationen = set()

for link in data['links']:
    source_id = link['source']
    target_id = link['target']
    rel_type = link.get('relation', 'unknown')

    source_unified = id_to_unified_cluster.get(source_id, source_id)
    target_unified = id_to_unified_cluster.get(target_id, target_id)

    # Berührt diese Kante mindestens eine unserer gewählten Fokus-Komponenten?
    if source_unified in fokus_unified_names or target_unified in fokus_unified_names:
        # Interne Verweise einer Komponente auf sich selbst filtern wir für die Übersichtlichkeit aus
        if source_unified != target_unified:
            potenzielle_links.append(link)
            if rel_type:
                existierende_relationen.add(rel_type)

sortierte_relationen = sorted(list(existierende_relationen))

print(f"\n=== SCHRITT 3: Relationen filtern ===")
for index, rel in enumerate(sortierte_relationen, start=1):
    print(f"[{index}] {rel}")

print("\nWelche Relationen möchten Sie sehen? (z.B. 1,2 für mehrere oder Enter für ALLE)")
auswahl_rel_input = input("Ihre Auswahl: ").strip()

erlaubte_relationen = []
if not auswahl_rel_input:
    erlaubte_relationen = sortierte_relationen
else:
    try:
        indizes = [int(x.strip()) for x in auswahl_rel_input.split(',')]
        erlaubte_relationen = [sortierte_relationen[i-1] for i in indizes if 0 < i <= len(sortierte_relationen)]
    except (ValueError, IndexError):
        print("Ungültige Eingabe, es werden ALLE Relationen genommen.")
        erlaubte_relationen = sortierte_relationen

# Finaler Filter der Kanten
relevante_links = [l for l in potenzielle_links if l.get('relation') in erlaubte_relationen]

# Alle im finalen Graphen tatsächlich vorkommenden verschmolzenen Knoten sammeln
relevante_unified_nodes = set()
for link in relevante_links:
    relevante_unified_nodes.add(id_to_unified_cluster.get(link['source'], link['source']))
    relevante_unified_nodes.add(id_to_unified_cluster.get(link['target'], link['target']))

# ==========================================
# SCHRITT 4: DOT-Generierung
# ==========================================
dot_lines = [
    "digraph MultiVerschmolzenerFluss {",
    "    rankdir=LR;",
    "    node [shape=box, style=\"filled,rounded\", fillcolor=\"#F0F4F8\", fontname=\"Arial\", color=\"#333333\"];",
    "    edge [fontname=\"Arial\", fontsize=10, penwidth=1.5];\n"
]

# Knoten formatieren und schreiben
for u_name in relevante_unified_nodes:
    # Prüfen, ob der Knoten zu einer der ausgewählten Fokus-Klassen gehört
    is_fokus_basis = u_name in ziel_labels
    is_fokus_methode = any(u_name.startswith(zl + ".") for zl in ziel_labels)

    if is_fokus_basis:
        # Die ausgewählten zentralen Hauptkomponenten
        dot_lines.append(f'    "{u_name}" [fillcolor=\"#FF6B6B\", fontcolor=\"white\", style=\"filled,bold\", fontsize=14];')
    elif is_fokus_methode:
        # Zugehörige Methoden der ausgewählten Hauptkomponenten
        dot_lines.append(f'    "{u_name}" [fillcolor=\"#FFE066\", style=\"filled,bold\"];')
    else:
        # Externe Interaktionspartner, die nicht im Fokus waren, aber verknüpft sind
        dot_lines.append(f'    "{u_name}" [fillcolor=\"#E9ECEF\"];')

dot_lines.append("")

# Kanten formatieren und schreiben (Verhindert identische doppelte Kanten)
geschriebene_kanten = set()
for link in relevante_links:
    source_unified = id_to_unified_cluster.get(link['source'], link['source'])
    target_unified = id_to_unified_cluster.get(link['target'], link['target'])
    rel_type = link.get('relation', 'unknown')

    kanten_id = f"{source_unified}->{target_unified}[{rel_type}]"

    if kanten_id not in geschriebene_kanten:
        edge_color = generate_color(rel_type)
        dot_lines.append(
            f'    "{source_unified}" -> "{target_unified}" '
            f'[label="{rel_type}", color="{edge_color}", fontcolor="{edge_color}"];'
        )
        geschriebene_kanten.add(kanten_id)

dot_lines.append("}")

# In Datei ausgeben
output_filename = "class_focus.dot"
with open(output_filename, 'w', encoding='utf-8') as f:
    f.write("\n".join(dot_lines))

print(f"\nErfolgreich! Der bereinigte Gruppen-Graph wurde in '{output_filename}' gespeichert.")

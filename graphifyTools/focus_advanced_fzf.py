import json
import hashlib
import subprocess
import sys

def generate_color(text):
    """Generiert eine reproduzierbare Farbe basierend auf dem Text."""
    hash_object = hashlib.md5(text.encode('utf-8'))
    return f"#{hash_object.hexdigest()[:6]}"

def run_fzf(items, prompt, multi=True):
    """Gibt eine Liste von Elementen an fzf weiter und gibt die Auswahl zurück."""
    # Bereite die Elemente als String vor (ein Element pro Zeile)
    input_str = "\n".join(items)

    # fzf-Argumente zusammenbauen
    # -m erlaubt Mehrfachauswahl (mit Tab)
    # --prompt setzt den Text vor der Suche
    args = ["fzf", "--prompt", prompt]
    if multi:
        args.append("-m")

    try:
        process = subprocess.Popen(
            args,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            text=True,
            encoding='utf-8'
        )
        stdout, _ = process.communicate(input=input_str)

        # Falls der Nutzer fzf mit ESC abbricht, ist der Rückgabecode nicht 0
        if process.returncode != 0:
            return []

        # Ergebnisse splitten und leere Zeilen filtern
        return [line.strip() for line in stdout.splitlines() if line.strip()]
    except FileNotFoundError:
        print("\n[FEHLER] 'fzf' wurde nicht auf dem System gefunden!", file=sys.stderr)
        print("Bitte installiere fzf (z.B. 'brew install fzf' oder 'apt install fzf').", file=sys.stderr)
        sys.exit(1)

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

# Ergänzung über ID-Präfixe für Methoden
for node_id, node in nodes_map.items():
    node_label = node.get('label', '')
    if node_label.startswith('.') and node_id not in method_to_class_label:
        for potential_class_id, potential_class_node in nodes_map.items():
            p_label = potential_class_node.get('label', '')
            if not (p_label.endswith('.java') or p_label.endswith('.py') or p_label.startswith('.')):
                if node_id.startswith(potential_class_id):
                    method_to_class_label[node_id] = p_label

# ==========================================
# SCHRITT 1: Auswahl der Fokus-Klassen via FZF
# ==========================================
labels_set = set()
for node in data['nodes']:
    lbl = node.get('label', '')
    if lbl:
        if lbl.endswith('.java'): lbl = lbl[:-5]
        elif lbl.endswith('.py'): lbl = lbl[:-3]

        if not (lbl.startswith('.') or lbl in ['Override', 'FXML', 'Test', 'String', 'UUID']):
            labels_set.add(lbl)

eindeutige_klassen = sorted(list(labels_set))

print("\n=== SCHRITT 1: Klassen auswählen ===")
print("-> Nutze [TAB] um mehrere Klassen zu markieren, [ENTER] zum Bestätigen.")
print("-> Einfach tippen, um die Liste live zu filtern.")

# FZF für Klassen aufrufen
ziel_labels = run_fzf(eindeutige_klassen, "Fokus-Klassen auswählen> ")

if not ziel_labels:
    print("Keine Auswahl getroffen oder abgebrochen. Es werden standardmäßig ALLE Klassen ausgewählt.")
    ziel_labels = eindeutige_klassen
else:
    print(f"\n=> Ausgewählte Fokus-Komponenten: {ziel_labels}")

# ==========================================
# SCHRITT 2: OPTIMIERTE VERSCHMELZUNGS-LOGIK
# ==========================================
id_to_unified_cluster = {}

for node_id, node in nodes_map.items():
    lbl = node.get('label', '')

    if lbl.endswith('.java'): lbl = lbl[:-5]
    elif lbl.endswith('.py'): lbl = lbl[:-3]

    if lbl.startswith('.'):
        parent_class = method_to_class_label.get(node_id, '')
        if parent_class.endswith('.java'): parent_class = parent_class[:-5]
        elif parent_class.endswith('.py'): parent_class = parent_class[:-3]

        if parent_class:
            id_to_unified_cluster[node_id] = f"{parent_class}{lbl}"
        else:
            id_to_unified_cluster[node_id] = lbl
    else:
        matched_class = None
        if lbl in eindeutige_klassen:
            matched_class = lbl
        else:
            for cls in sorted(eindeutige_klassen, key=len, reverse=True):
                if cls.lower() in node_id.lower():
                    matched_class = cls
                    break

        if matched_class:
            id_to_unified_cluster[node_id] = matched_class
        else:
            id_to_unified_cluster[node_id] = lbl

fokus_unified_names = set(ziel_labels)
for node_id, unified_name in id_to_unified_cluster.items():
    for zl in ziel_labels:
        if unified_name.startswith(zl + "."):
            fokus_unified_names.add(unified_name)

# ==========================================
# SCHRITT 3: Verfügbare Relationen filtern via FZF
# ==========================================
potenzielle_links = []
existierende_relationen = set()

for link in data['links']:
    source_id = link['source']
    target_id = link['target']
    rel_type = link.get('relation', 'unknown')

    source_unified = id_to_unified_cluster.get(source_id, source_id)
    target_unified = id_to_unified_cluster.get(target_id, target_id)

    if source_unified in fokus_unified_names or target_unified in fokus_unified_names:
        if source_unified != target_unified:
            potenzielle_links.append(link)
            if rel_type:
                existierende_relationen.add(rel_type)

sortierte_relationen = sorted(list(existierende_relationen))

print(f"\n=== SCHRITT 3: Relationen filtern ===")
print("-> Nutze [TAB] für Mehrfachauswahl, [ENTER] zum Bestätigen.")

# FZF für Relationen aufrufen
erlaubte_relationen = run_fzf(sortierte_relationen, "Relationen filtern> ")

if not erlaubte_relationen:
    print("Keine Auswahl getroffen. Es werden ALLE Relationen genommen.")
    erlaubte_relationen = sortierte_relationen
else:
    print(f"=> Sichtbare Relationen: {erlaubte_relationen}")

# Finaler Filter der Kanten
relevante_links = [l for l in potenzielle_links if l.get('relation') in erlaubte_relationen]

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

for u_name in relevante_unified_nodes:
    is_fokus_basis = u_name in ziel_labels
    is_fokus_methode = any(u_name.startswith(zl + ".") for zl in ziel_labels)

    if is_fokus_basis:
        dot_lines.append(f'    "{u_name}" [fillcolor=\"#FF6B6B\", fontcolor=\"white\", style=\"filled,bold\", fontsize=14];')
    elif is_fokus_methode:
        dot_lines.append(f'    "{u_name}" [fillcolor=\"#FFE066\", style=\"filled,bold\"];')
    else:
        dot_lines.append(f'    "{u_name}" [fillcolor=\"#E9ECEF\"];')

dot_lines.append("")

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

output_filename = "class_focus.dot"
with open(output_filename, 'w', encoding='utf-8') as f:
    f.write("\n".join(dot_lines))

print(f"\nErfolgreich! Der bereinigte Gruppen-Graph wurde in '{output_filename}' gespeichert.")

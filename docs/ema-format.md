# `.ema` – EMScript Motion Asset

Schema version: **1**

## Zweck

Eine `.ema`-Datei ist das kanonische, portable Austauschformat für ein M3ShapeMaker-`VisualAsset`. Sie enthält Design, benannte visuelle Zustände und Animationen. Sie definiert keine Workflow-Ausführung und setzt keine EMScript-Runtime voraus.

## Wurzel

```json
{
  "format": "emscript_motion_asset",
  "schemaVersion": 1,
  "asset": {}
}
```

## Asset

| Feld | Bedeutung |
|---|---|
| `assetId` | stabile Asset-ID |
| `name` | Anzeigename |
| `type` | `SHAPE`, `ANIMATED_SHAPE`, `BLOCK_SHAPE`, `NODE_SHAPE`, `ICON`, `VISUAL_COMPONENT` oder `ACTOR` |
| `version` | Asset-Version |
| `tags` | geordnete Schlagwörter |
| `design` | aktuelles kanonisches `VectorDocument` |
| `states` | benannte Dokumentzustände, darunter `start` und `end` |
| `transitions` | explizite Zustandsübergänge mit Timing, Motion und Property-Ranges |
| `animations` | Motion-Sequenzen und Schritte |
| `sequences` | geordnete Referenzen auf Transitions und Animationen |
| `metadata` | erweiterbare String-Metadaten |

## Animation

Ein Motion-Schritt speichert:

- aktive Channels: Morph, Rotation, Skalierung, Alpha, Farbe und Position
- `startMs` und `endMs`
- kubisches Bézier-Profil
- Interpolation `TWEEN`, `SPRING` oder `BOUNCE`
- Wertebereiche pro Channel
- Spring-/Bounce-Parameter
- Reverse- und Hold-Informationen

Transitions und Sequenzen sind erstklassige Asset-Felder. Sie werden nicht als eingebettete
JSON-Strings in `metadata` gespeichert. Ältere Schema-1-Dateien ohne diese optionalen Felder
bleiben lesbar.

## Morph-Kompatibilität

`start`, optional `mid` und `end` müssen jeweils ein nutzbares sichtbares Pfadobjekt enthalten.
Unterschiedliche Pfadtopologien werden für die Vorschau deterministisch normalisiert und auf
eine gemeinsame Punktanzahl resampelt. Die persistierte Geometrie selbst bleibt dabei
unverändert; fehlende nutzbare Pfade erzeugen eine sichtbare Diagnose.

## Nicht-Autorität

`.ema` beschreibt, wie ein visuelles Asset aussieht und animiert werden kann. Wann oder warum ein Host eine Animation abspielt, entscheidet ausschließlich der Host.

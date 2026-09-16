# M3ShapeMaker

M3ShapeMaker ist eine eigenständige Android-Anwendung zum Erstellen, Bearbeiten und Animieren wiederverwendbarer visueller Assets mit Kotlin, Jetpack Compose und Material 3.

## Aktueller vertikaler Slice

- gemeinsamer `DESIGN`-Workspace auf Basis von ComposeCanvas
- neutrales `VectorDocument` mit Canvas, Layern, Gruppen, Geometrien, Appearance und Semantik
- Auswahl, Transform, Snap, Path-/Knotenbearbeitung sowie Undo/Redo
- SVG-Import/-Export, Compose-Path-, Kotlin-Shape- und Metadatenexport
- parametrische Material-3-Expressive-Presets mit stabiler Preset-Identität
- benannte Zustände `start`, optional `mid` und `end` pro `VisualAsset`
- erstklassige Transitions und Sequenzen mit Dauer, Easing, Motion-Preset, Wiederholung und Property-Ranges
- topologie-tolerantes Pfad-Morphing durch deterministisches Resampling
- Animation Studio mit Sequencer, Tween, Spring und Bounce
- kanonisches `.ema`-Format für Design, Zustände und Animationen
- lokale Sicherung sowie Storage-Access-Framework-Import/-Export
- persistente Asset Library mit Suche, Tags und versionierten Einträgen
- Property-Tracks, Keyframes, Events und Resize-Regeln im neutralen Modell
- hostunabhängige Runtime-Steuerung für States, Transitions, Sequenzen und Properties
- zusätzliche Exporte für ImageVector, VectorDrawable und Visual-Metadaten

## Architektur

```text
ShapeMakerDocument
    ├── Metadata
    ├── Canvas
    ├── Assets (Geometry, Layers, Appearance, Content)
    ├── States
    ├── Transitions
    ├── Animations
    ├── Sequences
    └── InteractionMetadata
            │
            ├── Renderer  -> Live Preview
            ├── Serializer -> .json (ShapeMakerDocument) / .ema (Bridge)
            └── Exporter   -> Compose Path / Kotlin Shape / SVG
```

`VisualAsset` ist die kanonische neutrale Authoring-Repräsentation der App. `ShapeMakerDocument`
bildet dieselben Daten für Renderer, Serializer und austauschbare Exporter strukturiert ab;
Transitions und Sequenzen bleiben beim Bridge-/`.ema`-Roundtrip erstklassige Felder. Compose-
und SVG-Formate sind ausschließlich Render-, Interchange- oder Exportziele.

Das Projekt besteht aus:

- `:app` – App-Shell, M3-Preset-Katalog und Animation Studio
- `:design-editor` – kanonisches Asset-/Dokumentmodell, Editor, Renderer, Interchange und `.ema`-Codec
- `:runtime-sample` – unabhängige Consumer-App für die öffentliche Runtime-/Renderer-Grenze

## `.ema`

`.ema` steht für **EMScript Motion Asset**. Das JSON-basierte Format beschreibt VisualAsset, benannte visuelle Zustände und Animationen. Es enthält keine Workflow-, Host- oder EMScript-Runtime-Logik.

Formatkennung: `emscript_motion_asset`

Schema: `1`

MIME: `application/vnd.emscript.motion+json`

Siehe [docs/ema-format.md](docs/ema-format.md).

## Morph-Regel

Der aktuelle Slice verwendet aus `start`, optional `mid` und `end` jeweils ein sichtbares
Pfadobjekt. Unterschiedliche Pfadtopologien werden deterministisch auf eine gemeinsame
Punktanzahl normalisiert und resampelt. Nur Zustände ohne nutzbare Einzelpfad-Geometrie werden
diagnostiziert und von der Morph-Vorschau ausgeschlossen.

## Bewusste Grenze

- Workflow-Ausführung
- EMScript-Runtime
- direkte VisualTasker-Abhängigkeiten; Integration erfolgt ausschließlich über portable Assets und Runtime-API
- Accessibility-Automation, OCR oder YOLO
- Host-Entscheidungslogik

## Build

Voraussetzungen: Android SDK und JDK 17.

```bash
./gradlew :design-editor:testDebugUnitTest :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Die Debug-APK liegt anschließend unter `app/build/outputs/apk/debug/`.

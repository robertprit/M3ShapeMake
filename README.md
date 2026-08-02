# M3ShapeMake

A native Android shape and animation asset maker for Material 3 Expressive and custom polygon geometry.

## Overview

M3ShapeMake is a standalone native Android app written in Kotlin with Jetpack Compose and Material 3. It uses AndroidX Graphics Shapes for polygon geometry and path morphing. There is no WebView, no JavaScript, and no embedded runtime engine.

The app creates reusable shape and animation assets that other tools can import later. Export formats include Compose/Kotlin snippets, SVG, Android Vector Drawable (AVD), and versioned `.m3shape.json` asset bundles.

## Features

- 35 Material 3 Expressive shape presets
- Parametric rounded polygon editor
- Drawn polygon editor with freehand mode
- Zoom and pan in the shape editor
- Vertex list panel
- Single- and multi-select vertex editing
- Multi-drag for selected vertices
- Per-vertex rounding and smoothing
- Vertex insert after the last selected vertex
- Locked vertices
- Two-tab layout: **Shape Editor** and **Animation Studio**
- Animation studio with morph, rotate, scale, alpha, colour, and position channels
- Morph step UI for staged morph progress
- **Sequencer** for multi-step animation sequences
- Save, apply, duplicate, delete, reorder, and play sequence steps
- Compose/Kotlin export
- SVG export
- AVD (Animated Vector Drawable) export
- `.m3shape.json` asset bundle export
- SAF file export/import (Android Storage Access Framework)
- Local save/load of the last bundle
- Bundle validation with diagnostics

## `.m3shape.json` Asset Bundles

`.m3shape.json` files use the `m3shapemaker_asset_bundle` format (schema version 1). A bundle groups:

- shape geometry (`pathData`, optional per-vertex data)
- animation sequence metadata
- placeholder slots for future block and flow-node visual definitions

Bundles are meant for exchange between tools. They describe visual geometry and animation parameters, not runtime behavior or script semantics.

See [docs/m3shape-format.md](docs/m3shape-format.md) and [docs/examples/sample_bundle.m3shape.json](docs/examples/sample_bundle.m3shape.json) for structure and a minimal valid example.

## Non-Goals

M3ShapeMake is intentionally not:

- a full vector graphics editor
- a Blockly clone
- a Flowchart editor
- a VisualTasker Studio plugin
- a runtime automation engine
- an EMScript generator
- a cloud service

## Architecture Boundary

```text
M3ShapeMake creates visual geometry and animation metadata.
Other systems may import the exported files.
The exported files do not define runtime behavior.
```

M3ShapeMake remains a standalone app. VisualTasker Studio or other consumers may import exported files in the future, but M3ShapeMake is not embedded as a plugin. `.m3shape.json` transports geometry and animation metadata only — no semantic execution model.

## Build

Requirements: Android SDK, JDK 17.

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

## Project Structure

```text
app/src/main/java/com/m3shapes/editor/
├── MainActivity.kt
├── M3ShapeMakeApp.kt
├── ShapeEditorTab.kt
├── AnimationStudioTab.kt
├── AnimationSequencer.kt
├── AnimationSequenceModels.kt
├── EditableVertexModels.kt
├── ShapeEditorState.kt
├── VertexListPanel.kt
├── M3ShapeAssetModels.kt
├── M3ShapeAssetCodec.kt
├── M3ShapeAssetValidation.kt
├── M3ShapeAssetMapping.kt
├── M3ShapeAssetRepository.kt
├── M3ShapeFileIo.kt
├── M3ShapeExportPanel.kt
├── ShapeModels.kt
├── ShapeResolver.kt
├── PolygonEditor.kt
├── Exporters.kt
├── UiHelpers.kt
└── ExpressiveShapeCatalog.kt

docs/
├── m3shape-format.md
└── examples/
    └── sample_bundle.m3shape.json
```

## Status

Functional prototype.

The app supports shape editing, animation sequencing, code/vector export, and a versioned `.m3shape.json` asset bundle format.

Known limitations:

- no persistable URI permissions or document library
- local save/load currently uses internal app storage (`filesDir/last_m3shape_bundle.json`)
- BlockVisual and FlowNodeVisual data exist as exchange placeholders only
- no VisualTasker Studio importer is implemented in this repository
- Compose export covers the current animation config, not full sequencer export yet

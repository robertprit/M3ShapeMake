# `.m3shape.json` Asset Bundle Format

Schema version: **1**

## Purpose

The `.m3shape.json` format (MIME-friendly JSON, often named `*.m3shape.json`) is a portable container for shape geometry and animation metadata produced by M3ShapeMake. Importers can read bundles without coupling to the Android app.

Bundles are **descriptive, not authoritative** for runtime behavior. They do not define scripts, block semantics, flow execution, or automation rules.

## Top-Level Bundle

| Field | Type | Description |
|-------|------|-------------|
| `format` | string | Must be `m3shapemaker_asset_bundle` |
| `schemaVersion` | int | Must be `1` |
| `producer` | object | `{ "name", "version" }` — defaults to M3ShapeMaker / 1.0.0 |
| `bundleId` | string | Non-blank bundle identifier |
| `namespace` | string | Non-blank namespace prefix for assets |
| `shapes` | array | Shape assets |
| `animations` | array | Animation sequence assets |
| `blockVisuals` | array | Placeholder block visual definitions |
| `flowNodeVisuals` | array | Placeholder flow-node visual definitions |

## ShapeAsset

Each shape entry (`type: "shape_asset"`) includes:

- `namespace`, `id`, `name`
- `kind`: `MaterialPreset`, `ParametricPolygon`, `DrawnPolygon`, or reserved kinds for future use
- `viewport`: `{ "width", "height" }` in logical units (default 256×256)
- `pathData`: SVG-style path string in viewport coordinates
- `vertices`: optional per-vertex list (`id`, `x`, `y`, `rounding`, `smoothing`, `locked`)
- `metadata`: string map (e.g. material preset id, parametric config)

## AnimationSequence

Each animation entry (`type: "animation_sequence"`) includes:

- `namespace`, `id`, `name`
- `steps`: ordered list of animation steps

Each step (`M3AnimationStepAsset`) stores:

- timing: `durationMs`, `holdMs`, `easing` (enum name such as `EaseInOut`)
- enabled channels: `animations` (names such as `Morph`, `Rotate`, `Scale`, `Alpha`, `Colour`, `Position`)
- spring parameters: `stiffness`, `damping`, `mass`, `reverse`
- range endpoints: `morphStart`/`morphEnd`, `rotateStart`/`rotateEnd`, `scaleStart`/`scaleEnd`, etc.
- legacy display fields: `morphProgress`, `rotation`, `scale`, `alpha`, `wobble`, `bounce` (kept for schema compatibility)

## Placeholder Visual Definitions

`blockVisuals` and `flowNodeVisuals` reserve structure for future Blockly- or flowchart-style visual overlays. They reference shapes by `shapeAssetId` and may define docking zones, sockets, connection points, and text anchors.

M3ShapeMake does not edit these sections in the UI yet. Arrays are typically empty.

## Validation Rules

The in-app validator (`M3ShapeAssetValidator`) checks:

- required format and schema version
- non-blank `bundleId` and root `namespace`
- unique shape ids within the bundle
- non-blank shape `pathData` and valid viewport dimensions
- finite vertex coordinates and rounding/smoothing values
- animation step `durationMs > 0` and `holdMs >= 0`
- finite numeric animation fields
- warnings for unknown animation or easing strings
- block/flow visual shape references when those arrays are non-empty

Decoding via `M3ShapeAssetCodec` additionally rejects unsupported `format` or `schemaVersion` values.

## Non-Authority Rule

```text
.m3shape.json describes what shapes look like and how they may animate.
It does not tell a host application what to execute, when, or why.
```

Consumers must treat bundles as import input and apply their own runtime semantics.

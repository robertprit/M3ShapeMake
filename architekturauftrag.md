# Prompt: ShapeMaker – Smartphone-first Visual Asset, Shape & Motion Editor für Jetpack Compose

Entwirf und implementiere **ShapeMaker** als eigenständige Android-App für die visuelle Erstellung programmierbarer Shapes, Vektorgrafiken, Material-3-Expressive-Formen und Animationen direkt auf einem Smartphone oder Tablet.

ShapeMaker soll **kein allgemeiner Figma-, Android-Studio- oder Compose-UI-Builder** werden. Der Schwerpunkt liegt auf **Visual Asset Authoring, Geometry, States, Transitions und Motion**. Jetpack Compose ist dabei primäres Rendering- und Export-Target, aber nicht das interne Dokumentformat.

## 1. Grundidee

ShapeMaker soll eine Lücke zwischen klassischem Vektoreditor, Compose-Shape-Designer, Animationseditor und programmierbarem Visual-Asset-System schließen.

Der Workflow soll vollständig auf Android funktionieren:

**Zeichnen → Shape bearbeiten → Zustände definieren → Übergänge animieren → testen → exportieren**

Die Bedienung muss für Touch und insbesondere präzise Stiftbedienung geeignet sein.

Die App soll später ihre erzeugten Visual Assets auch anderen Anwendungen als Service bzw. Plugin zur Verfügung stellen können.

---

# 2. Zentrale Architektur

Das interne Modell darf nicht einfach aus generiertem Kotlin-Code bestehen.

Verwende ein neutrales, serialisierbares Dokumentmodell:

```text
ShapeMakerDocument
 ├── Metadata
 ├── Canvas
 ├── Assets
 │    ├── Geometry
 │    ├── Layers
 │    ├── Appearance
 │    └── Content
 ├── States
 ├── Transitions
 ├── Animations
 ├── Sequences
 └── InteractionMetadata
```

Daraus werden verschiedene Targets erzeugt:

```text
                    ShapeMakerDocument
                           │
             ┌─────────────┼─────────────┐
             │             │             │
          Renderer      Serializer    Exporter
             │                           │
             │             ┌─────────────┼──────────────┐
             ▼             ▼             ▼              ▼
        Live Preview    Compose         SVG       VectorDrawable

                                      + weitere Targets
```

Die interne Repräsentation muss deshalb unabhängig von Compose bleiben.

---

# 3. Geometry Editor

Unterstütze mindestens:

* Point
* Line
* Rectangle
* Rounded Rectangle
* Circle
* Ellipse
* Polygon
* Star
* Arc
* Bézier Path
* Spline
* freie Pfade
* Compose `Path`
* Material-3-/Material-3-Expressive-Shapes
* kombinierte Shapes

Punkte und Kontrollpunkte müssen direkt auf dem Canvas verschiebbar sein.

Erforderlich sind:

* Zoom
* Pan
* Grid
* Snap
* Guides
* Auswahl
* Multi-Selection
* Move
* Rotate
* Scale
* Mirror
* Alignment
* Boolean Path Operations
* Undo/Redo

Die Darstellung muss unabhängig von Bildschirmauflösung und Display-Dichte funktionieren.

---

# 4. Layer-System

Visual Assets sollen aus mehreren Ebenen bestehen können.

Beispiel:

```text
Watchdog
 ├── Body
 ├── Eyes
 ├── StatusRing
 ├── Indicator
 └── Shadow
```

Jeder Layer benötigt mindestens:

* Sichtbarkeit
* Position
* Rotation
* Scale
* Alpha
* Fill
* Stroke
* Stroke Width
* Gradient
* Z-Order
* optional eigene Animation

Layer sollen gruppierbar und verschachtelbar sein.

---

# 5. Visual States

Animation darf nicht nur als Timeline betrachtet werden.

Ein Visual Asset besitzt benannte Zustände:

```text
IDLE
LISTENING
THINKING
WORKING
SUCCESS
WARNING
ERROR
```

Jeder State kann unterschiedliche Werte besitzen für:

* Geometry
* Shape
* Position
* Rotation
* Scale
* Alpha
* Farbe
* Gradient
* Stroke
* Sichtbarkeit
* Layer-Zustände
* weitere animierbare Eigenschaften

Beispiel:

```text
IDLE
  shape = circle
  scale = 1.0
  alpha = 0.7

WORKING
  shape = softStar
  scale = 1.1
  alpha = 1.0

ERROR
  shape = diamond
  scale = 1.0
  alpha = 1.0
```

---

# 6. Transitions

States werden über explizite Transitions verbunden:

```text
IDLE ───────→ LISTENING
                 │
                 ▼
              THINKING
                 │
                 ▼
              WORKING
              /      \
             ▼        ▼
         SUCCESS    ERROR
```

Eine Transition beschreibt nicht nur ihre Dauer, sondern die komplette Bewegungscharakteristik.

Beispiel:

```text
Transition
 ├── fromState
 ├── toState
 ├── duration
 ├── delay
 ├── easing
 ├── motionPreset
 ├── repeatMode
 └── propertyOverrides
```

Transitions müssen bidirektional oder unidirektional definierbar sein.

---

# 7. Material 3 Motion

Integriere die Konzepte von Material 3 Motion bzw. Material 3 Expressive.

Unterscheide insbesondere zwischen:

```text
Spatial Motion
 ├── Position
 ├── Größe
 ├── Rotation
 └── Shape/Geometry

Effects Motion
 ├── Alpha
 ├── Farbe
 ├── Stroke
 └── visuelle Effekte
```

ShapeMaker soll Motion-Presets anbieten, beispielsweise:

```text
Standard
Expressive
Linear
Spring
Bounce
Custom
```

Die Architektur soll später die Abbildung auf Compose `MotionScheme` und `FiniteAnimationSpec` erlauben.

Material Motion ist dabei **ein Preset-/Export-System**, nicht die interne Datenstruktur von ShapeMaker.

---

# 8. Shape Morphing

Ein zentraler Bestandteil ist das animierte Morphen zwischen kompatiblen Formen.

Beispiele:

```text
Circle → RoundedSquare
Play → Pause
Circle → SoftStar
SoftStar → Burst
IdleShape → ErrorShape
```

Prüfe vor einer Morph-Animation die geometrische Kompatibilität.

Falls zwei Shapes nicht direkt interpolierbar sind, soll ShapeMaker:

1. die Ursache anzeigen,
2. wenn möglich eine kompatible Normalisierung anbieten,
3. andernfalls auf eine alternative Transition zurückfallen können.

Die Morphing-Logik muss von der UI getrennt und testbar implementiert werden.

---

# 9. Timeline / Animation Sequencer

Zusätzlich zum State-System soll ein visueller Sequencer vorhanden sein.

Beispiel:

```text
TIME     0ms       300       600       900      1200

Body     ●──────────●───────────────────●
Scale    ●────●────────────●
Rotate              ●────────────●
Alpha    ●──────────────────────────────●
Eyes               OPEN       CLOSE
```

Unterstütze:

* Keyframes
* Property Tracks
* Dragging von Keyframes
* Duration
* Delay
* Easing
* Loop
* Repeat
* Reverse
* Ping-Pong
* Sequenzen
* Live Scrubbing
* Play/Pause
* Frame-/Zeitposition

State Transitions und Timeline-Sequenzen müssen miteinander kombinierbar sein.

---

# 10. Live Preview

Jede Änderung soll möglichst unmittelbar sichtbar sein.

Preview-Modi:

```text
Static
State
Transition
Sequence
Loop
Interaction Preview
```

Der Benutzer soll beispielsweise `IDLE → WORKING → SUCCESS` direkt abspielen können.

Die Preview darf nicht vom Export abhängig sein.

---

# 11. Smartphone-first UX

ShapeMaker ist ausdrücklich eine **native Android-App**.

Die Oberfläche darf nicht lediglich einen Desktopeditor auf einen kleinen Bildschirm quetschen.

Berücksichtige:

* Touch
* Stylus/S-Pen
* Long Press
* Drag
* Pinch Zoom
* Zwei-Finger-Pan
* Kontextaktionen
* Floating Tool Panels
* Bottom Sheets
* einklappbare Inspector-Panels
* große Touch Targets
* präzise Point-/Handle-Manipulation

Für Stifteingabe sollen später zusätzlich Druck, Hover und weitere verfügbare Stylus-Eigenschaften berücksichtigt werden können.

Der Canvas erhält möglichst viel Bildschirmfläche; Werkzeuge erscheinen kontextabhängig.

---

# 12. Compose Export

ShapeMaker soll Visual Assets als idiomatischen Kotlin-/Jetpack-Compose-Code exportieren können.

Targets sollen unter anderem umfassen:

```text
Shape
GenericShape
Path
Canvas DrawScope
Composable
ImageVector
Animated Visual
```

Wo sinnvoll, sollen Animationen auf Compose-APIs abgebildet werden:

```text
animate*AsState
Transition
Animatable
graphicsLayer
FiniteAnimationSpec
Material MotionScheme
```

Generierter Code muss lesbar und möglichst unabhängig vom ShapeMaker-Runtime-Modul verwendbar sein.

---

# 13. Weitere Exportformate

Die Exportarchitektur muss erweiterbar sein.

Mindestens vorbereiten für:

* Kotlin/Compose
* SVG
* Android VectorDrawable
* JSON/ShapeMakerDocument
* Compose Path
* Shape-Metadaten
* Animations-/Sequenz-Metadaten

Exporter sollen über klar definierte Interfaces austauschbar sein.

---

# 14. Programmierbare Visual Assets

Ein ShapeMaker Asset soll nicht nur eine Grafikdatei sein.

Es kann ein zustandsbehaftetes visuelles Objekt darstellen:

```text
VisualAsset
 ├── Geometry
 ├── Appearance
 ├── Layers
 ├── States
 ├── Transitions
 ├── Motion
 └── Sequences
```

Eine externe Anwendung könnte dadurch beispielsweise nur noch anfordern:

```text
setState("working")
```

statt einzelne Rotations-, Scale-, Alpha- und Morph-Animationen selbst kontrollieren zu müssen.

---

# 15. Externe Runtime-/Plugin-Nutzung

ShapeMaker bleibt eine eigenständige App.

Andere Anwendungen sollen ShapeMaker-Assets später über eine klar definierte Schnittstelle verwenden können.

Insbesondere soll die Architektur ermöglichen:

```text
External App
     │
     ▼
ShapeMaker Runtime / Service
     │
     ▼
VisualAsset
     │
     ├── setState()
     ├── playTransition()
     ├── playSequence()
     └── setProperty()
```

Die Editor-App und eine mögliche schlanke Runtime-Bibliothek müssen deshalb voneinander getrennt sein.

---

# 16. VisualTasker-Kompatibilität

ShapeMaker soll später als Plugin/Service für VisualTasker Studio dienen können, ohne Bestandteil dessen Kernarchitektur zu werden.

VisualTasker bzw. EMScript könnten beispielsweise ein ShapeMaker-Asset laden und dessen Zustände steuern.

Konzeptionelles Beispiel:

```text
visual("watchdog").state("working")
visual("watchdog").transition("success")
visual("watchdog").sequence("celebrate")
```

Ebenso sollen primitive Transformationen möglich bleiben:

```text
move
rotate
scale
alpha
show
hide
```

Die ShapeMaker-Architektur darf jedoch **nicht von EMScript abhängig sein**.

---

# 17. Bewusste Abgrenzung zu Composer/Figma

ShapeMaker soll **kein allgemeiner Compose Layout Designer** werden.

Nicht das primäre Ziel:

```text
Row
Column
Scaffold
Button
TextField
NavigationBar
LazyColumn
```

visuell zu kompletten Apps zusammenzubauen.

Dafür existieren bzw. entstehen bereits Werkzeuge wie Composer und andere visuelle Compose-Builder.

ShapeMaker konzentriert sich stattdessen auf:

```text
Geometry
      +
Visual Assets
      +
States
      +
Transitions
      +
Motion
      +
Interaction
      +
Export
```

Ein in ShapeMaker erzeugtes Asset soll später problemlos als normales Composable innerhalb anderer Compose-Projekte oder visueller Compose-Editoren verwendet werden können.

---

# 18. Technische Prinzipien

Bevorzuge:

* Kotlin
* Jetpack Compose
* native Android-Komponenten
* immutable Models
* klare Trennung zwischen Model, Renderer, Editor und Exporter
* StateFlow/Compose State für Editorzustände
* Commands/Operations für Undo/Redo
* serialisierbares neutrales Dokumentmodell
* deterministische Geometrie
* Unit Tests für Geometry/Morph/Serialization
* möglichst geringe Kopplung zwischen Editor und Runtime

Vermeide:

* WebView als Kerneditor
* DOM-basierte Canvas-Strukturen
* direkte Speicherung des Editors als Kotlin-Quellcode
* UI-State als Dokumentmodell
* Compose-spezifische Klassen tief im neutralen Asset-Modell
* monolithische Renderer
* unkontrollierte gegenseitige Abhängigkeiten zwischen ShapeMaker und VisualTasker

---

# 19. Performance

Der Editor muss auch bei komplexeren Assets flüssig bleiben.

Insbesondere vermeiden:

* ein eigenständiges schwergewichtiges UI-Element pro Canvas-Punkt,
* unnötige vollständige Recompositions,
* permanente Neuberechnung unveränderter Paths,
* Allokationen innerhalb kritischer Draw-Loops.

Bevorzuge einen Canvas-orientierten Renderer mit separaten Hit-Test- und Selection-Strukturen.

Die Architektur soll später Dirty Regions, Geometry Caching und selektive Invalidierung erlauben.

---

# 20. Erstes Entwicklungsziel

Nicht sofort das vollständige System implementieren.

Erstelle zunächst einen vertikalen Slice:

```text
Canvas
  ↓
Shape erstellen
  ↓
Shape auswählen
  ↓
Control Points bearbeiten
  ↓
Appearance verändern
  ↓
zweiten State anlegen
  ↓
Transition zwischen beiden States
  ↓
Live Preview
  ↓
Speichern/Laden
  ↓
Compose-Export
```

Minimaler Demonstrator:

1. Circle erzeugen.
2. Circle bearbeiten.
3. State `idle` definieren.
4. State `active` erzeugen.
5. In `active` Shape/Scale/Rotation verändern.
6. Transition `idle → active` definieren.
7. Transition auf dem Smartphone abspielen.
8. Dokument speichern und erneut laden.
9. Ergebnis als Compose-Code exportieren.
10. Exportierten Code in einem separaten Compose-Projekt rendern.

Erst wenn dieser komplette Roundtrip stabil funktioniert, sollen Layer-System, komplexe Sequenzen, SVG-Import, VisualTasker-Service und weitere Exportformate ausgebaut werden.

## Leitprinzip

ShapeMaker ist kein weiterer UI-Builder.

**ShapeMaker ist ein Smartphone-first Authoring-System für programmierbare visuelle Erscheinungsformen.**

Die zentrale Einheit ist daher nicht `Composable`, `SVG` oder `Animation`, sondern:

```text
VisualAsset
     =
Geometry
+ Appearance
+ State
+ Transition
+ Motion
```

Compose ist eines der wichtigsten Rendering- und Exportziele. Das interne Modell bleibt jedoch neutral, damit dieselben Assets später in Android-Apps, Overlays, VisualTasker, EMScript und anderen Laufzeitumgebungen verwendet werden können.

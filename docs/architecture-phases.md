# Architekturphasen – Umsetzungsstand

## Phase 5 – Import, Export und Kompatibilität

Abgeschlossen:

- kanonische `.ema`- und `ShapeMakerDocument`-Roundtrips
- SVG, Compose Path, Kotlin Shape und Shape-Metadaten
- Compose `ImageVector`, Android `VectorDrawable` und Visual-Metadaten
- zielbezogene Verlustwarnungen für statische Exportformate
- Strukturtests für Geometrie, Zustände, Transitions, Sequenzen und Animationen

## Phase 6 – Erweiterungen

Abgeschlossen als getrennte vertikale Slices:

1. Ports, Anchors, Content Areas und serialisierbare Resize-Regeln.
2. States, Property Tracks, Keyframes und Events im neutralen Motion-Modell.
3. Persistente Asset Library mit Suche, Tags und Versionen sowie Library-Tab.
4. Hostunabhängige Runtime-API und separate `:runtime-sample`-Consumer-App.
5. Erweiterte Exportziele einschließlich hostneutraler Visual-Metadaten.

Die Runtime enthält bewusst keine EMScript-, Workflow- oder VisualTasker-Abhängigkeit.

## Phase 7 – Abschlussvalidierung

Abnahmebefehle:

```bash
./gradlew :design-editor:testDebugUnitTest :app:testDebugUnitTest
./gradlew :app:assembleDebug :runtime-sample:assembleDebug
./gradlew :app:installDebug :runtime-sample:installDebug
```

Gerätestart und Vordergrundaktivität werden für beide Apps separat geprüft. Automatisierte
Touch-/Screenshot-Bedienung ist eine eigenständige visuelle Abnahme und kein Ersatz für Modell-
und Roundtrip-Tests.

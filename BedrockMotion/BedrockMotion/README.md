# BedrockMotion

Fabric-Mod für **Minecraft Java 26.3**, die Bedrock-Ressourcenpacks mit Animationen auf Java abspielt:
**Molang**, Animationen, Animation Controller, Render Controller, Client Entities und Bedrock-Geometrie.
Ziel: Packs wie *Actions & Stuff* sollen in Java laufen, ohne dass man sie von Hand umbauen muss.

## Ist das technisch machbar?

Ja, mit Grenzen. Der Kern (Molang, Animationen, Controller, Render Controller, Modelle) lässt sich
vollständig nach Java übertragen, weil Bedrock hier nur Datenformate und eine Skriptsprache liefert.
Nicht 1:1 übertragbar sind alles, was an die Bedrock-Engine gebunden ist: Material-Shader (`.material`),
Bedrock-Partikel, Sound-Events, PBR/Raytracing und der Bedrock-Spieler. Die Mod bildet Materialien
deshalb auf Java-Render-Typen ab (Cutout, Translucent, Emissive) und lässt den Rest weg.

## Was getestet ist und was nicht

Hier im Entwicklungsumfeld gab es **kein Minecraft 26.3 und keinen Netzzugang**. Deshalb gilt:

| Teil | Status |
|---|---|
| Molang-Engine, JSON, Geometrie-Parser, Animationen, Controller, Render Controller, Pack-Lader, Konverter, TGA/PNG | **Getestet** gegen die echten Packs (siehe unten) |
| Minecraft-Schicht (`dev.bedrockmotion.client`: Mixins, Renderer, Texturen, Reload-Listener) | **Nicht kompiliert und nicht gestartet.** Geprüft nur gegen selbstgeschriebene API-Stubs und gegen die Quelltexte von AzureLib 26.3 / EMF / ETF |

Der erste `./gradlew build` ist also der echte Test der Minecraft-Schicht. Wenn etwas nicht kompiliert,
steht unten unter *Fehlersuche*, wo die Annahmen liegen.

Testergebnisse (Kern, gegen *Actions & Stuff 1.13* und *Smooth Moves*):

- 17.645 verschiedene Molang-Ausdrücke aus *Actions & Stuff* kompilieren fehlerfrei und laufen ohne Ausnahme
  (inklusive `loop`, `for_each`, `break`/`continue`/`return`, `->`, `??`, Strings, Structs, Arrays).
- Beide Packs laden mit 0 Problemen. *Actions & Stuff*: 116 Entities, 1.698 Attachables, 3.715 Modelle,
  3.492 Animationen, 948 Controller, 1.080 Render Controller.
- 1.814 Definitionen laufen je 90 simulierte Frames (Animator + Render Controller) ohne Fehler, rund 0,02 ms pro Frame.
- Konverter: Rundlauf Bedrock-Pack → Java-Pack → Laden liefert identische Inhalte.
- 409 `.tga`-Texturen (einige davon sind in Wahrheit PNG) werden korrekt zu PNG.

## Installation

1. Java 25, Minecraft 26.3, Fabric Loader ≥ 0.19.5 und Fabric API 0.162.0+26.3.
2. `./gradlew build`, die Jar liegt in `build/libs/`.
3. Bedrock-Packs (`.mcpack`, `.zip` oder entpackter Ordner) nach `.minecraft/bedrockmotion/packs/` legen.
   Mehrere Packs werden alphabetisch geladen, spätere überschreiben frühere.
4. Spiel starten (oder `F3 + T`). Das Log meldet pro Pack die geladenen Definitionen.

Alternativ kann man ein Bedrock-Pack in ein normales Java-Ressourcenpack umwandeln:

```
./gradlew convertPack -Pin="Actions Stuff 1.13 (resources).mcpack" -Pout=ActionsStuff-java.zip
```

Das Ergebnis legt man wie jedes Ressourcenpack in `resourcepacks/`. Dateinamen werden dabei klein
geschrieben und Sonderzeichen durch `_` ersetzt (Java-Pfadregeln); Verweise im Pack werden beim Laden
gleich behandelt. Falls Minecraft das Pack als „inkompatibel“ markiert, in der `pack.mcmeta` die
`pack_format`-Zahl anpassen, es lässt sich trotzdem aktivieren.

## Konfiguration

Optional `config/bedrockmotion.json`:

```json
{
  "enabled": true,
  "subpacks": { "ActionsStuff.mcpack": "SP1" },
  "settings": { "ActionsStuff.mcpack": { "oreville_ans:some_setting": "option_name" } }
}
```

- `subpacks`: Bedrock-Subpacks sind Varianten eines Packs. Standard ist der letzte in der `manifest.json`
  (bei *Actions & Stuff* `SP2`, die volle Variante).
- `settings`: Pack-Einstellungen aus der `manifest.json`, die Molang mit `q.is_pack_setting_selected(...)`
  und `q.is_pack_setting_enabled(...)` abfragt. Ohne Eintrag gelten die Standardwerte des Packs.

## Funktionsumfang (v0.1)

| Bedrock-Feature | Stand |
|---|---|
| Molang: Arithmetik, Vergleiche, `?:`, `??`, Zuweisungen, Variablen `v.`/`t.`/`c.`, Structs, Arrays, Strings, `loop`, `for_each`, `return`, `break`, `continue`, `->`, `this`, alle `math.*` inkl. `ease_*` | ja |
| Queries (`q.*`): Zeit, Bewegung, Rotation, ~50 Zustände, Ausrüstung/Tags, Blöcke, Pack-Einstellungen | rund 170 registriert, davon etwa 100 mit echten Werten; die übrigen liefern 0, bis die Mod sie füllt (`EntitySnapshot.extra`) |
| Animationen: Keyframes (linear, Catmull-Rom, step), `loop`/`hold_on_last_frame`, `anim_time_update`, `blend_weight`, `override_previous_animation`, Timeline-Skripte | ja |
| Animation Controller: States, Transitions, `blend_transition`, `on_entry`/`on_exit`, `any_animation_finished` | ja |
| Client Entity: `scripts` (initialize, pre_animation, animate, scale), Geometrie/Texturen/Materialien | ja |
| Render Controller: Geometrie, Texturen, `part_visibility`, Materialien je Bone, Arrays, Farbe, `uv_anim`, `ignore_lighting` | ja |
| Modelle: Formate 1.8 bis 1.21, Box-UV und Face-UV, Cube-Rotation, Mirror, Inflate, Locators | ja |
| Texturen PNG und TGA | ja |
| Ersetzen von Vanilla-Entities (außer Spieler) und modded Entities mit gleicher ID | ja (ungetestet im Spiel) |
| `relative_to`, mehrere Texturen pro Layer, Partikel, Sounds, Nametags, Hurt-Flash | **noch nicht** |
| Attachables (Items in der Hand, Rüstung) | geladen, **noch nicht gerendert** |
| Spieler (`minecraft:player`) | **noch nicht** |
| Reine Animations-Packs für Vanilla-Modelle (z. B. *Smooth Moves*) | Pack lädt, aber ohne eigene Geometrie gibt es nichts zu zeichnen. Geplant: Pose auf Java-ModelParts anwenden |

## Aufbau

```
src/main/java/dev/bedrockmotion/
  json/      toleranter JSON-Parser (Kommentare, Trailing Commas, BOM)
  molang/    Lexer/Parser, Knoten, Math, Queries, Env (pro Entity), EntitySnapshot
  bedrock/   GeometryDef, AnimationDef, ControllerDef, RenderControllerDef, ClientEntityDef,
             BedrockPack (Lader), EntityAnimator (Laufzeit), RenderPlanner
  pack/      PackFiles (Zip/Ordner/Subpack-Overlay), TGA-Decoder, PNG-Encoder, PathMapper
  tools/     PackConverter
  client/    Minecraft-Schicht: BedrockMotionClient, BmPackLoader, BmRuntime, BmFrame, Mixins
```

Der Kern (alles außer `client/`) kennt Minecraft nicht und läuft mit einem normalen JDK 21+:

```
tools/selftest.sh "/pfad/zu/Actions Stuff 1.13 (resources).mcpack" "/pfad/zu/Smooth Moves"
```

Ablauf pro Frame und Entity: Mixin `EntityRenderer.extractRenderState` füllt einen `EntitySnapshot`,
`EntityAnimator` führt Skripte, Animationen und Controller aus und liefert eine `Pose`, `RenderPlanner`
wertet die Render Controller aus, `BmFrame` berechnet die Bone-Matrizen. Mixin `EntityRenderDispatcher.submit`
ersetzt dann den Vanilla-Aufruf durch `BmFrame.submit` (`submitCustomGeometry`).

### Koordinaten (falls ein Modell gespiegelt oder verdreht aussieht)

Die Umrechnung folgt GeckoLib/AzureLib: Origin- und Pivot-X werden negiert, Rotationen werden als
(−x, −y, +z) in Radiant gespeichert und in der Reihenfolge Z, Y, X angewendet, Animations-Position X wird
negiert. Diese Stellen sind in `GeometryDef.makeBone/makeCube`, `EntityAnimator.play` und `BmFrame.compute`.

## Fehlersuche

Die Mixins stehen mit `defaultRequire: 0`, damit das Spiel auch bei einem falschen Ziel startet.
Dann gibt es aber keine Ersetzung. Wenn nach dem Laden eines Packs nichts passiert, prüfe das Log:

- Warnung „Mixin apply failed“ für `EntityRenderDispatcherMixin`: Die Ziel-Methode `submit` oder der Aufruf
  `EntityRenderer.submit(...)` hat in 26.3 eine andere Form. Anpassen in `EntityRenderDispatcherMixin`.
- Warnung „Mixin apply failed“ für `EntityRendererMixin`: dasselbe für `extractRenderState`.
- Kompilierfehler in `BmTextures`: Konstruktor von `DynamicTexture` oder `TextureManager.register` weicht ab
  (AzureLib 26.3 nutzt `registerAndLoad` mit `ReloadableTexture`).
- Kompilierfehler in `BmProbe`/`BmRuntime`: einzelne Entity- und Item-Methoden (`onGround()`, `getItemBySlot`,
  `isFallFlying`). Die Namen stammen aus Mojang-Mappings, die für 26.x nicht gegen die Jar geprüft werden konnten.
- Die Meldung „Pack(s) geladen, aber noch keine Entity ersetzt“ erscheint, wenn der Extract-Hook läuft, aber
  keine passende Entity gefunden wurde.

## Roadmap

1. Pose auf Vanilla-`ModelPart`s anwenden (Animations-Packs wie *Smooth Moves*), Hook an `Model.setupAnim`.
2. Attachables: Items in der Hand und Rüstung mit Bedrock-Modell.
3. Spieler, Nametags, Hurt-Overlay, mehrere Texturen pro Layer.
4. Partikel (Bedrock-JSON) und Sound-Events aus Animationen.
5. Echte Werte für die Queries mit Standardwert 0 liefern (`q.bone_origin`, `q.is_angry`, `q.is_saddled` und weitere).

many issues like some walking animations not playing etc some eye issues with mobs like bees player animations non existent right now


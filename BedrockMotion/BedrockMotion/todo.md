fix all text from german to eng
add logo

fix the rest of the animations
fix particles (warden death animation)

learn from polytone ?



make new chat 

Erledigt

Kern (ohne Minecraft, gegen Actions & Stuff und Smooth Moves getestet)

Molang: vollständige Engine, alle 17.645 Ausdrücke aus dem Pack laufen.
Pack-Daten:
Lader für .mcpack, .zip und Ordner inklusive Subpacks und Pack-Einstellungen.
Parser für Modelle, Animationen (Keyframes, Delays, relative_to), Controller, Render Controller, Entities und Attachables.
Bei doppelten Definitionen gewinnt die höchste min_engine_version.
Queries: rund 170 registriert, etwa 100 mit echten Werten.
Partikel:
Emitter, Formen, Kurven, Events, Flipbook und Gradient; alle 525 Definitionen laufen.
Partikel laufen nach dem Tod der Entity weiter und verfallen von selbst, wie bei der Warden-Todesanimation.
Konverter (Bedrock → Java-Ressourcenpack): Sounds (477 Events, 955 Dateien), 1.509 Block- und 776 Item-Texturen, 61 Flipbook-Animationen, Colormaps, Environment und Sprachdatei. Die Namenstabelle ist unvollständig.

Minecraft-Schicht (kompiliert bei dir, nur teilweise im Spiel gesehen)

Entities werden mit Bedrock-Modell ersetzt, inklusive Nametag und Hurt-Overlay. Dazu kommen Farben und Texture-Meshes (als flache Quads).
Animations-Packs wirken auf Vanilla-Modelle (Smooth Moves).
Items in der Hand sowie Items auf Bedrock-Trägern werden an Knochen gebunden gezeichnet (Binding).
Es gibt Partikel-Billboards, Sounds, eine Entity-ID-Zuordnung (Bedrock → Java) und Spieler-Skin. Das Ersetzen des Spielers ist per replacePlayers aus.


Fehlt noch
Entities: Bone-reset, extrudierte Texture-Meshes, Pose-Anwendung auf Vanilla-Modelle mit Entity-Rotation, Items auf nicht ersetzten Trägern außer in der Hand, First-Person-Arme, Block-Entities (Kisten, Schilder, Betten, Banner), Rüstungs-Layer.
Partikel: Kollision, expire_if_*, Block-Atlas-Partikel, Vanilla-Bedrock-Partikel. Sie erscheinen nur, solange irgendeine Entity gerendert wird. Die Größenskalierung ist geraten.
Queries: rund 70 liefern noch 0 (Mob-Zustände wie is_stunned).
Bedienung: kein Bildschirm im Spiel für Pack-Einstellungen und Subpacks, nur config/bedrockmotion.json.
Assets: Gemälde-Atlas, GUI und UI, Trims, Font, Map-Icons, Item- und Block-Modelle, vollständige Namenstabellen, Entity-Texturen als Vanilla-Ersatz, Lang-Schlüssel Bedrock → Java.
Nicht Teil der Mod (Vibrant Visuals): PBR, Fogs, Atmosphäre, Lighting, Color Grading, Shadows, Water.



btw particles werent to small btw particles sind zu groß blinking funktioniert nicht animations gibts eh nicht der player hält das schwert komisch hinterm rücken im moment und items sind nach vorne gekippt statt nach oben zu zeigen und toos werden ganz komisch gehalten aber kommt noch oder ONLY XP WAS TO BIG THAT WAS THE ONLY ISSUE THE REST CAN BE FIXED WHEN WE IMPLEMENTED everything THE REST
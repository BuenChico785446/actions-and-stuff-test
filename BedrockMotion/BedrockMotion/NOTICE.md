# Hinweise zu Drittcode und Quellen

BedrockMotion ist eine eigenstaendige Implementierung. Folgende Projekte dienten als Referenz:

- **GeckoLib / AzureLib** (MIT, Copyright Bernie-G. / AzureDoom): Die Konventionen fuer die Umrechnung von
  Bedrock-Geometrie in Java-Raum (X-Spiegelung von Origin und Pivot, Rotationsvorzeichen -x/-y/+z, Reihenfolge
  Z-Y-X, Flaechen- und Vertex-Reihenfolge der Quads, Box-UV-Layout) in `GeometryDef` und `BmFrame` folgen dem
  dort erprobten Verfahren. Ebenso die Render-Pipeline-Aufrufe fuer 26.x (`submitCustomGeometry`).
- **Entity Model Features (EMF)** und **Entity Texture Features (ETF)** (LGPL bzw. GPL, Traben): als Referenz fuer
  die Mixin-Ziele in der 26.x-Render-Pipeline. Es wurde kein Code uebernommen.
- **Actions & Stuff** und **Smooth Moves**: dienten ausschliesslich als Testdaten fuer Molang, Animationen,
  Controller und Render Controller. Die Packs gehoeren ihren Autoren und sind nicht Teil dieses Projekts.

# Zeichnen von Planeten
═══════════════════════════════════════════════════════════════
Phase 1: Canvas-Analyse & Grenzen
═══════════════════════════════════════════════════════════════
1. canvasRadius = min(canvasWidth, canvasHeight) / 2
2. zentrum = (canvasWidth/2, canvasHeight/2)

3. minOffset = star.radius + star.deadZone
4. maxOffset = canvasRadius - globalPadding
5. verfügbarerRadius = maxOffset - minOffset

═══════════════════════════════════════════════════════════════
Phase 2: Dynamische Größenberechnung (aus Canvas)
═══════════════════════════════════════════════════════════════
6. anzahlPlaneten = orbitalBodies.size
7. radialSlotGröße = verfügbarerRadius / anzahlPlaneten
8. maxPlanetenRadius = (radialSlotGröße - 2*planetPadding) / 2

9. Größen-Lookup berechnen:
   - LARGE  = maxPlanetenRadius * 1.0
   - MEDIUM = maxPlanetenRadius * 0.75
   - SMALL  = maxPlanetenRadius * 0.5

═══════════════════════════════════════════════════════════════
Phase 3: Planeten zeichnen (Loop über orbitalBodies)
═══════════════════════════════════════════════════════════════
Für jeden Planet (index i):

  10. Tatsächliche Größe auflösen:
      planetRadius = größenLookup[orbitalBody.sizeCategory]
      
  11. Orbit-Distance berechnen:
      orbitDistance = minOffset + (i * radialSlotGröße) + radialSlotGröße/2
      
  12. Orbit-Pfad berechnen & cachen:
      - Ellipse mit: center=zentrum, radiusX=orbitDistance, 
        radiusY=orbitDistance*ellipseRatio
      - Speichere Pfad für Wiederverwendung
      
  13. Orbit zeichnen (gestrichelte Linie, halbtransparent)
  
  14. Planet-Position berechnen:
      - effectiveOrbitDuration = customOrbitSpeed ?: orbitDuration
      - winkel = (currentTime / effectiveOrbitDuration * 360°) + startAngle
      - x = zentrum.x + orbitDistance * cos(winkel) * ellipseRatio
      - y = zentrum.y + orbitDistance * sin(winkel)
      
  15. Planet zeichnen

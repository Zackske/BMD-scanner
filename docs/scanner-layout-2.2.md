# Scanner 2.2 — goedgekeurde BMD-indeling

- Bovenaan: BMD-naam en snelle Flits-, Camera- en Instellingen-knoppen.
- Midden: echt CameraX-livebeeld met cyaan scanvak, teller en genummerde herkenningsmarkeringen.
- Rechts: compacte zoom met directe standen 1×, 2× en 3×, begrensd door de camera. De schuifknop beweegt en licht kort op.
- Zwevend boven het ondermenu: Scan uit galerij. Bij annuleren keert de app terug naar de scanner.
- Onder: Scan in het midden om zelf de foto vast te leggen. Kleine codelijst- en resetknoppen staan links en rechts; Historie en Maak zijn verwijderd.
- De codelijst blijft per fysieke scan bewerkbaar. Reset draait rechtsom en laat opgeslagen bestanden staan.
- Na vastleggen blijft de foto stilstaan en verschijnt Opslaan/Delen. Geannoteerde markeringen verdwijnen niet doordat de gebruiker wacht met exporteren.
- Camera wisselt tussen voor- en achtercamera wanneer de telefoon beide ondersteunt. Fotomirroring en export houden rekening met de frontcamera.
- QR + barcodes / QR-only / barcodes-only staan in het kleine tandwiel tijdens scannen. Resolutie, gevoeligheid en trilling staan in het grote instellingenpaneel.
- De bestaande tien talen, documenttypen CSV/XLSX/DOCX/PDF, bestandsnamen en het Over de app-paneel blijven behouden.

De bovenbalk, onderste Scan-vormgeving en Nederlandse galerijknop gebruiken uitsneden van het goedgekeurde voorbeeld, zodat lettervormen en BMD-glans behouden blijven. De productfoto uit het voorbeeld wordt **niet** als camerabeeld getoond. De camera blijft een echte live preview met echte bedienbare knoppen.

De homepage uit versie 2.1 blijft ongewijzigd. De nieuwe teksten zijn toegevoegd voor alle tien talen.

Deze versie wordt gebouwd met Java 17 en Gradle 8.9. Lint, JVM-tests, pakketidentiteit en APK-handtekening worden gecontroleerd. Op verzoek test de eigenaar het gedrag en de exacte weergave zelf op de telefoon. Voor deze release wordt geen emulator-/telefoontest geclaimd.

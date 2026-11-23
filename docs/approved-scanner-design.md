# Goedgekeurd scannerontwerp — 16 november 2026

Status: geïmplementeerd in versie 2.0.0. De concrete verificatie en resterende hardwarecontroles staan in [het testrapport](verification-2.0.md).

Home: vier verhoogde 3D-tegels met indruk-animatie, dezelfde afmetingen en positie. BMD ScanTovenaar met vrijstaand scannerbeeld en subtiele bewegende rook bovenaan. Het originele aangeleverde Blue Medical-webplogo staat kleiner en gecentreerd onderaan, op een witte achtergrond met een dunne zwarte opstaande rand. Afbeeldingskleuren en bronbestand zijn behouden. Eerdere B.V./Devices-naamvarianten vervallen. ‘Batch’ is verwijderd en Scannen staat op dezelfde tekstlijn als de andere tegels.

## Talen

Bestaand: Nederlands, Engels, Oekraïens, Turks.
Toegevoegd: Italiaans (eerder afgesproken naam BMD Mago dello Scan), Duits, Frans, Spaans, Hindi, Portugees. Totaal tien talen. Alle schermteksten, instellingen, keuzemenu's, meldingen, toegankelijkheidslabels en appnamen lokaliseren; scanwaarden en productcodes nooit vertalen. Alle tien talen zijn in de 2.0.0-broncode opgenomen.
Algemene instellingen: taalkeuze toont alle tien talen met eigen taalnamen, in deze volgorde: Nederlands, English, Українська, Türkçe, Italiano, Deutsch, Français, Español, Português, हिन्दी. Portugees staat direct boven Hindi. Na selectie gehele interface vertalen, ook scannerinstellingen, detectieformaten, opslaan/delen en correctiemenu. Taalnamen in de kiezer blijven herkenbaar in hun eigen taal.
Kleine vlag naast elke taalnaam in de taalkeuze: Nederland, Verenigd Koninkrijk, Oekraïne, Turkije, Italië, Duitsland, Frankrijk, Spanje, India, Portugal. Taalnaam blijft zichtbaar; vlag is aanvullende visuele herkenning.

## Documenttype-instellingen

Vervang CSV-instellingen in algemene instellingen door Documenttype (documenttype-instellingen), met keuzes CSV (.csv), Excel (.xlsx), Word (.docx), PDF (.pdf). Bewaar bestandsnaaminstelling en pas juiste extensie toe. Exporteer echte bestanden van het gekozen formaat, geen hernoemde CSV. Neem individuele scanregels inclusief identieke inhoud per product mee. Alle teksten in tien talen. Opslaan/delen gebruikt gekozen documenttype; bestaande foto-opslag/delen blijft beschikbaar. Nog ontwerp, niet ingebouwd of geüpload.

Laatste menu-aanpassing: verwijder de tegel Opslaan en delen uit de algemene instellingen. Overblijvende tegels: Taal, Scanner, Documenttype, Over de app. De acties lokaal opslaan en delen na foto vastleggen/resultaatexport blijven behouden.
Algemene instellingentegels iets hoger en ruimer verdelen, zodat vier tegels het scherm evenwichtiger vullen. Houd titel en onderste golf vrij.
Over de app opent met dezelfde vloeiende paneelanimatie als taalkeuze. Paneel/canvas bevat uitsluitend 'Made by Jack with' plus hartsymbool (❤️), zonder versiegegevens, extra informatie of knoppeninhoud. Sluiten via buiten tikken/terug/klein sluiticoon mogelijk. Deze makersregel behouden zoals opgegeven.

- Video opent live detectie automatisch voor QR én lineaire barcodes.
- Tel fysieke code-exemplaren afzonderlijk, ook wanneer de inhoud identiek is. Elk gevolgd exemplaar één keer per sessie tellen. Rustige genummerde kaders/vinkjes blijven bij de code; camera-beweging volgen. Heridentificatie na verdwijnen uit beeld is nog een technische ontwerpuitdaging, geen bewezen garantie.
- Teller '6 codes gescand', uitsplitsing QR/barcodes, zichtbare resultatenlijst met behoud van dubbele waarden.
- Gebruiker legt met ronde sluiter zelf de foto vast. Daarna korte bevestigingsanimatie, keuze lokaal opslaan of Android delen (e-mail/social apps), foto en scanresultaten.
- Verticale zoom rechts op camera: boven plus/inzoomen, onder min/uitzoomen, waarde bij schuifknop, vloeiende animatie en zachte lichtpuls.
- Lampknop direct boven de verticale zoombalk aan de rechterkant (niet meer in bovenste toolbar). Uit: rustig wit/grijs icoon met UIT-status; aan: geel icoon en AAN-status met korte gouden activatiepuls. Lamp blijft continu aan tijdens ingeschakelde toestand; geen stroboscoop.
- Linker ronde knop laat codelijst omhoog uitklappen. Individuele scan corrigeren/verwijderen; identieke waarden op andere producten blijven ongewijzigd. Wijzigingen opslaan.
- Rechter ronde knop reset huidige scansessie; pijl draait met de klok mee. Teller/markeringen/lijst terug naar nul; opgeslagen bestanden blijven behouden.
- Laatste wijziging: alleen live detectie; Video/Foto-scanmodus en de tegel Scanmodus vervallen. De ronde sluiter legt op gebruikerscommando een foto vast. Headerlabel kan 'LIVE DETECTIE' worden zonder 'VIDEO'.
- Gear opent scannerinstellingen: resolutie (eerdere volledige opties behouden met native camera fallback), gevoeligheid, trillen na elke individuele geslaagde scan. Zoom en flits blijven op camera.
- Nieuw klein tandwiel naast LIVE DETECTIE opent tijdens scannen een compact geanimeerd dropdownmenu: QR + barcodes (standaard), Alleen QR, Alleen barcodes. Keuze wijzigt welke codeformaten worden herkend. Bestaande sessieresultaten behouden; telleruitsplitsing blijft correct. Het grote tandwiel voor camera-instellingen blijft apart. Nog ontwerp, niet geïmplementeerd.
- Voorlopig alleen visuele voorstellen; geen GitHub-upload zonder nieuwe gebruikersopdracht.

Laatste overzicht scanner: exec-a5788544-747f-4401-9ce6-a795f23ea27d.png.
Lijst/reset: exec-a944bcf9-f1eb-48ac-91ef-1493ba635356.png.
Foto opslaan/delen: exec-e94a1dd8-fa75-4dc1-8bd4-2153fb8f4b81.png.



Implemented for 2.0.0: approved 3D home tiles; original supplied blue-medical-logo-1.webp replaces the earlier company banner. Tracking associates payload plus physical position within a continuously visible scene. Identical products that leave the view completely cannot be reliably distinguished on re-entry; keep the batch visible until capture. No claim that optical tracking can determine hidden physical identity.

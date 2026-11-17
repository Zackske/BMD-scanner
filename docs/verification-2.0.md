# Verificatie BMD ScanTovenaar 2.0.0

Geteste broncode: `43ec827a0de5c7102875e02768de0a6767229203`.

[GitHub Actions, build 55](https://github.com/Zackske/BMD-scanner/actions/runs/44038630808)

## Geautomatiseerde controles

- Java 17 en Gradle 8.9, APK-bouw, Android lint en 18 JVM-tests.
- 11 tests op een actieve Android 35-emulator, verdeeld over menu-/bestandstests (7), cameracontroles (3) en een echte schermopname (1).
- Live camera: zes fysieke symbolen: drie Code 128-barcodes en drie QR-labels met exact dezelfde inhoud. Herhaalde beelden voegen geen nieuwe exemplaren toe.
- QR-only en barcode-only herkennen elk drie fysieke labels. De gebruiker bepaalt het moment van fotograferen; vastleggen verhoogt de teller niet opnieuw.
- De gemaakte foto blijft stilstaan. Foto, markeringen en teller blijven behouden na opnieuw aanmaken van de activiteit.
- Cameraresolutie: alle dertien voorkeuren binden met een werkelijk ondersteunde terugvalmaat. Gevoeligheid bevestigt opeenvolgende verwerkte beelden, ook wanneer frames langzaam aankomen.
- Zoom wordt begrensd door de cameramogelijkheden. Een ontbrekende zoom- of lampmogelijkheid wordt uitgeschakeld. De teller blijft stabiel bij de ondersteunde zoomaanvraag.
- Correcties, verwijderde fysieke tracks, kort uit beeld verdwijnen en sessieherstel zijn gecontroleerd. Reset wist de huidige sessie en negeert nog lopende beelden van vóór de reset.
- Foto-import decodeert een werkelijk gegenereerde afbeelding met drie identieke QR-codes en drie barcodes.
- CSV behoudt duplicaten, Unicode en voorloopnullen; formuleachtige waarden worden veilig voor spreadsheets geschreven. XLSX en DOCX zijn echte OOXML-pakketten met geldige XML, geen hernoemde CSV. PDF opent via Android PdfRenderer, inclusief meerdere pagina’s en lange codes.
- Androids bestandskiezer en documentvenster openen en kunnen worden geannuleerd. Een gekozen lokale map ontvangt werkelijk een foto en document.
- Delen gebruikt leesbare FileProvider-URI’s, ClipData en tijdelijke leestoegang voor de ontvangende app.
- Alle tien talen hebben vertaalde menuteksten en de afgesproken taalvolgorde met vlaggetjes. De code-inhoud blijft onveranderd.

## Visuele controle

Echte Android-schermen zijn gebruikt, inclusief geopende dialoogvensters. Het startscherm heeft vier verhoogde tegels, Scannen zonder Batch, de aangepaste titel, subtiele bewegende rook en het originele aangeleverde Blue Medical-webplogo met dunne donkere rand.

De bronafbeelding van het bedrijfslogo is ongewijzigd opgenomen in `app/src/main/res/drawable-nodpi/blue_medical_logo.webp`.

Actuele voorbeelden:

- [Startscherm](screenshots/home-2-0-approved.png)
- [Live teller](screenshots/camera-live-six-codes.png)
- [Camera-instellingen](screenshots/camera-settings-no-mode.png)
- [Tien talen](screenshots/language-picker-ten-flags.png)
- [Vastgelegde foto na schermherstel](screenshots/photo-survives-recreation.png)
- [Echte schermopname](demo/BMD-ScanTovenaar-demo.mp4)

## Nog op een echte telefoon te controleren

Dit is geen uitgevoerde Galaxy S26-hardwaretest. Camera-autofocus en beeldkwaliteit bij weinig licht, werkelijke vergroting en lensovergangen, lampsterkte en de voelbare trilling vereisen een fysieke telefoon. E-mail/sociale bezorging en het openen in geïnstalleerde Microsoft Word/Excel-apps zijn niet uitgevoerd.

De camera krijgt verzoekmaten; een niet ondersteunde sensorresolutie wordt niet kunstmatig als ondersteund gerapporteerd. Geannoteerde exportfoto’s worden voor geheugengebruik verkleind; camera-invoer en geëxporteerde fotomaat kunnen verschillen.

Identieke codes zijn afzonderlijke exemplaren op basis van zichtbare posities. Volledig verdwenen of verwisselde identieke producten zijn zonder unieke productidentificatie niet betrouwbaar van elkaar te onderscheiden. Houd de batch in beeld tot het fotograferen en controleer de bewerkbare lijst.

## Download

[APK 2.0.0 build 55](https://github.com/Zackske/BMD-scanner/releases/download/v2.0.0-build.55/BMD-ScanTovenaar.apk)

Bestand: 6.747.667 bytes. SHA-256: `b0858e721e6131d583eda94454310c3508c87f4bc982f7ddd048df99eb0af9c4`.

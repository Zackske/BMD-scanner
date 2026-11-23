# Homepage 2.1 — aangeleverd referentiebeeld

De Nederlandse homepage gebruikt het door de eigenaar aangeleverde PNG-bestand rechtstreeks. Er worden geen benaderende systeemfonts of nieuwe kleurverlopen over de Nederlandse titel en tegelteksten gezet. Daarmee blijven de oorspronkelijke 3D-randen, letters, rookillustratie, scanner en het Blue Medical-logo behouden.

Het originele bestand staat ongewijzigd in `app/src/main/assets/home-reference-2-1.png` (Git-blob `bb70ff54f29e24d5ae9367846e17c740a27436d1`).

## Echte bediening

De afbeelding is de vormgeving. Daarboven liggen vier native Android-knoppen op de exacte tegelposities: Scannen, Foto’s uploaden, Resultaten en Instellingen. Ze hebben klikacties, toegankelijkheidsnamen, toetsenbordfocus en aanraakfeedback. De Nederlandse tekst staat in het originele beeld; andere talen krijgen vertaalde tekst op dezelfde tegelposities. De bestaande scanner, import, resultaten en instellingen worden via dezelfde acties geopend.

## Schaal en systeemranden

Van de afbeelding worden alleen de eigen apppixels gebruikt: van y=48 tot y=1613 in de originele afbeelding van 942 × 1670. De afgebeelde klok en navigatiestreep worden dus niet als nepbediening getoond. Android tekent zijn eigen actuele status- en navigatiebalken.

De afbeelding schaalt proportioneel, zonder uitrekken of afsnijden van tegels. Bij een andere schermverhouding blijft de originele compositie behouden met donkere ruimte waar nodig.

## Verificatie

`HomeReferenceTest` vergelijkt de echt weergegeven Nederlandse Android-windowpixels met het originele referentiebeeld op dezelfde schaal. De toegestane gemiddelde afwijking is minder dan 1,1 per RGB-kanaal op een bereik van 0–255, om alleen afronding bij schermschaling toe te laten.

Daarnaast worden de klikgebieden, gelijke tegelmaten, logo-uitlijning, tien vertaalde toegankelijkheidsnamen en de echte navigatie naar resultaten, instellingen, bestandskiezer en camera gecontroleerd. De APK wordt gebouwd met Java 17 / Gradle 8.9, lint en JVM-tests. Het pakket en de APK-handtekening worden ook gecontroleerd.

Deze homepagecontrole is geen nieuwe fysieke camerakwaliteits-, zoom-, flits- of trillingstest op de Galaxy S26.

## Geslaagde build 57

- Geteste broncode: `85b465b5bcbbdd33438fe84e3548bc079b04aa97`.
- [GitHub Actions](https://github.com/Zackske/BMD-scanner/actions/runs/44123161100): bouwen, lint, JVM-tests, pakket/handtekening en alle drie Android-homepagetests geslaagd.
- De gemeten gemiddelde RGB-afwijking was **0,0** bij een echte appinhoud van **1080 × 1794** pixels. De vergelijking bemonstert om de drie pixels, buiten de eigen systeemranden.
- [Echte APK-screenshot](screenshots/home-2-1-pixel-verified.png).
- [APK 2.1.0, build 57](https://github.com/Zackske/BMD-scanner/releases/download/v2.1.0-build.57/BMD-ScanTovenaar.apk), versiecode 8.
- APK: 8.667.490 bytes; SHA-256 `9ab25e3a55a11becdee372f6dba4ace0d1577dd9621cc6c2d7247805a5bcbab8`.

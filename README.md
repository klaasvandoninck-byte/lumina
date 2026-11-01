# Lumina (Fabric 1.21.11, client-side)

Client-side mod met ClickGUI, HUD, Spotify-kaartje, speler-ESP en een paar simpele modules.

## Bouwen

Genereer eerst een leeg project op https://fabricmc.net/develop (Minecraft 1.21.11,
Mojang mappings). Dat geeft je de juiste Gradle-wrapper en versienummers. Kopieer
daarna `src/` in dat project (en eventueel `build.gradle` / `gradle.properties`), en draai:

    gradlew build

De jar staat in `build/libs/`. Zet hem in je mods-map, samen met Fabric API voor 1.21.11.

## Gebruik

- RIGHT SHIFT: ClickGUI. Linksklik module = aan/uit, rechtsklik = instellingen.
- Titelbalk slepen = paneel verplaatsen, rechtsklik erop = inklappen.
- Elke module heeft een toggle-toets onder Opties > Besturing > Lumina.
- Config: `config/lumina.json`.

## Spotify instellen

1. Ga naar https://developer.spotify.com/dashboard en maak een app.
2. Voeg bij "Redirect URIs" exact toe: `http://127.0.0.1:8888/callback`
3. Kopieer het Client ID (geen secret nodig).
4. Start Minecraft eenmaal, sluit af en open `config/lumina-spotify.json`. Vul `clientId` in.
5. Zet in de ClickGUI de module "Spotify" aan. Rechtsklik op het kaartje (of gebruik de
   toets "Spotify: Login") en plak de gekopieerde link in je browser. Log in en keur goed.
6. Het kaartje is te slepen in de ClickGUI. Play/pauze/volgende/vorige zijn toetsen in
   Opties > Besturing (vereist Spotify Premium en een actief Spotify-apparaat).

`lumina-spotify.json` bevat je tokens. Deel het bestand nooit.

## Niet gebouwd (niet veilig mogelijk zonder de 1.21.11-broncode te controleren)

Custom lucht, wereldtijd, mist uitzetten, FreeCam, Skeleton-ESP, tracers en nametags.

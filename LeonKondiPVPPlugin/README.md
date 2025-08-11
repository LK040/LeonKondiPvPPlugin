# LeonKondiPVPMod

Spigot 1.20.x PvP-Mod mit Teams, Border-Shrink, Respawn-Queue, Elimination, Sieger-/Top-Killer-Ansage und deaktiviertem Nether/End.

## Build
- Java 17
- Maven

```sh
mvn -q -DskipTests package
```

Jar: `target/LeonKondiPVPMod-1.0.0.jar`

## Installation
- Jar nach `plugins/` auf deinem Spigot-Server kopieren
- Server starten
- `config.yml` bei Bedarf anpassen

## Kommandos
- `/join teamRot|teamGelb|teamGrün|teamBlau`
- `/teamreset`
- `/startgame` (OP)

Konfiguration (`config.yml`): Weltgrenze startet standardmäßig bei 4000, shrinkt in 2h auf 100. Teamspawns bei ±1900. Passe Werte an, falls du initial 2000 möchtest (dann Spawns z. B. auf ±900 setzen).


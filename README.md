# All-Stars Pixel Dungeon

A mod of [Shattered Pixel Dungeon](https://github.com/00-Evan/shattered-pixel-dungeon) 4.0.2. It adds online co-op for 2 to 8 players, new heroes, alternate versions of the five regions and, past the usual ending, an open world to survive in.

It has its own app id and save folder, so it installs next to Shattered without touching your runs there.

## What's different from Shattered

**Co-op.** Up to 8 players, either on the same Wi-Fi (games show up on their own) or over the internet with a 5-letter room code. Everyone plays in the same turn order; if two devices ever disagree, the host sends its game to the others and play goes on. The party takes the stairs together, a fallen hero comes back on the next floor, and a run only ends when everybody is down. Strength potions and upgrade scrolls drop once per player. Solo runs work as usual.

**Heroes.** 15 new heroes. Each carries an item that opens an ability window with four abilities (the last one from level 6), and has its own talents, subclasses and armor ability.

**Alternate worlds.** Any region can roll as the Forest, the Kingdom, the Starship, the Undersea or the Forgotten Land instead, with its own terrain, enemies and boss. How often that happens, and how hard they are, is set in "Difficulty and worlds" when you start a run. Each region also has a quest with a mini-boss; finishing it or failing it changes the boss fight at the end.

**The Rift.** In co-op runs, a guest from the Rift joins some boss fights: the Rift Crawler with Tengu, the Crimson Ancestor with DM-300, the Primeval Dragon on floor 20 and the Pit Worm on floor 25.

**Thal-Vekhorr.** If all five regions of a run were alternate worlds, a secret final boss waits on the way back to the surface.

**The Dark Continent.** Beating Thal-Vekhorr opens a portal (in co-op the party votes on it). Behind it is an open world of six biomes: the Ash Plains, the Flesh Forest, the Devoured City, the Frozen Moor, the Graveyard of Colossi and the Black Isles, each with its depths and its abyss. Most of what lives there is far bigger than you, up to colossi that fill a 15×15 area. You can climb them, knock them down, tame one per party, eat their parts to mutate, and forge their remains into gear. Darkness, corruption, campfires and a sacred torch decide how long you last, and gathering seven drops of essence leads to the true ending.

**Smaller things.** An optional damage cap per hit ("Anti-OP") for groups that find a build too strong, a menu with saved co-op runs and run options, friendly fire, map marks and an end-of-run scoreboard.

## Known gaps

- The splash art of the new heroes and worlds is still a placeholder (a question mark).
- New text is in English and Spanish only. With another language selected, the new content shows in English.
- The iOS project is untouched from Shattered and has not been built or tested.

## Download

Android and Windows builds are on the [releases page](../../releases). Everyone in a co-op game needs the same version.

Internet games go through a public MQTT broker (HiveMQ, over TLS). Anyone with your room code can join, so don't share it around.

## Building

Same as Shattered: see the guides for [desktop](docs/getting-started-desktop.md) and [Android](docs/getting-started-android.md). I build with JDK 21.

- `./gradlew desktop:debug` runs the game on PC.
- `./gradlew desktop:jpackageImage` makes the Windows folder with its own Java.
- `./gradlew android:assembleRelease` makes the APK.

## Credits and license

All-Stars Pixel Dungeon is made by tomascomenta. Shattered Pixel Dungeon is made by Evan Debenham, based on Pixel Dungeon by Oleg Dolya (Watabou). This is an unofficial mod; it isn't made or supported by them, so please don't send them bug reports about it.

GPLv3 like the original, see [LICENSE.txt](LICENSE.txt).

## En español

Mod de Shattered Pixel Dungeon 4.0.2 con cooperativo en línea de 2 a 8 jugadores (misma Wi-Fi o por internet con un código de 5 letras), 15 héroes nuevos, versiones alternativas de las cinco regiones con sus propios enemigos y jefes, una misión con minijefe en cada región, los invitados de la Grieta en las partidas cooperativas, un jefe final secreto y, después, el Continente oscuro: un mundo abierto de seis biomas lleno de colosos.

Se instala aparte de Shattered, con sus propias partidas guardadas. Las descargas para Android y Windows están en la página de releases; todos los jugadores de una partida necesitan la misma versión. Los textos nuevos están en inglés y en español.

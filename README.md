# MediaPlayerApp

A modern, offline Android music player built with Jetpack Compose and Media3. It scans the music
on your device and plays it with a polished Material 3 interface, full background playback, and the
kind of features you expect from a commercial player.

## Features

- **Library** — automatic MediaStore scan; browse by **Songs, Albums, Artists, Folders**; search and
  sort (title / artist / album / duration / recently added), with an A-Z fast-scroll index.
- **Playback** — single Media3 `MediaSessionService` engine with a notification, **lockscreen /
  Bluetooth / headset** controls and Android Auto support; shuffle, repeat, gapless, and a
  scrubbable seek bar.
- **Now Playing** — large artwork with a palette-tinted background, a reorderable **queue**
  (drag-and-drop), **sleep timer**, **playback speed**, and a favourite toggle.
- **Playlists** — user playlists (create / rename / delete / duplicate / reorder) with **`.m3u`
  import & export**, plus smart playlists: Favorites, Recently played, Most played, Recently added.
- **Equalizer** — a real, persistent equalizer with presets, per-band sliders, **bass boost** and
  **virtualizer** (attached to the player session, so it keeps working app-wide).
- **Design** — Material You dynamic colour (Android 12+), light / dark / system themes,
  edge-to-edge, album artwork everywhere, swipe-to-favourite / swipe-to-queue gestures with haptics
  and undo, context menus, and a mini-player.

## Tech stack

Kotlin · Jetpack Compose · Material 3 · MVVM + Repository · StateFlow / Coroutines ·
Media3 (ExoPlayer + MediaSession) · Room · DataStore · Coil · Navigation Compose.

## Requirements

- Android Studio (recent stable), Android SDK, JDK 11+
- `minSdk` 26 · `targetSdk` 36

## Build & run

```bash
./gradlew :app:assembleDebug        # build the debug APK
./gradlew :app:testDebugUnitTest    # run unit tests
```

Or open the project in Android Studio, let Gradle sync, pick a device/emulator and press **Run**.
On first launch, grant the audio (and notification) permission so the library can load.

## Project layout

- `playback/` — `MusicService` (single ExoPlayer + MediaSession) and `AudioEffects`
- `data/` — repositories, MediaStore loading, Room (`db/`), DataStore, playlist / m3u logic
- `ui/` — Compose screens (`HomeScreen`, `LibraryScreen`, `NowPlayingScreen`, …) and reusable
  `components/`
- `PlayerViewModel` — the single source of UI state, driving playback through a `MediaController`

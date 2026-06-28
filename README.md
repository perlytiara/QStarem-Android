# QStarem

<p align="center">
  <img src="docs/assets/app-icon.png" alt="QStarem app icon (Q Play)" width="128">
</p>

<p align="center">
  A native Android browser shell for <a href="https://zstream.mov">Z-Stream</a> with bundled P-Stream and ad blocking.
</p>

<p align="center">
  <a href="https://github.com/perlytiara/QStarem-Android/actions/workflows/ci.yml"><img src="https://github.com/perlytiara/QStarem-Android/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
  <a href="https://github.com/perlytiara/QStarem-Android/releases/latest"><img src="https://img.shields.io/github/v/release/perlytiara/QStarem-Android?label=release" alt="Latest release"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-brightgreen" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/minSdk-26-blue" alt="minSdk 26">
</p>

<p align="center">
  <a href="https://github.com/perlytiara/QStarem-Universal">Desktop (Rust)</a>
  ·
  <a href="https://github.com/perlytiara/QStarem-Android/releases/latest">Download APK</a>
</p>

## Why QStarem for Z-Stream

Z-Stream is a movie-web-style streaming frontend. QStarem wraps it in a focused mobile browser with everything pre-configured:

- **P-Stream** — extra sources and quality options (no manual XPI install on Android)
- **uBlock Origin** (default) or **AdGuard** — ad and popup blocking
- **GeckoView** — real Firefox engine with WebExtension support
- **Picture-in-Picture**, media notification controls, and a minimal dark UI
- **Six app icons** — default **Q Play**, switchable in App settings

## Install from Releases

1. Open [Releases](https://github.com/perlytiara/QStarem-Android/releases/latest) and download `QStarem-*-release.apk`.
2. On your phone, allow installs from your browser or file manager when prompted.
3. If needed: **Settings → Security → Install unknown apps** and enable your download app.
4. Open **QStarem** and start browsing your configured Z-Stream instance (default: [zstream.mov](https://zstream.mov)).

## App icon

The default launcher icon is **Q Play** (purple ring + play mark). You can pick a different icon anytime:

1. Open Z-Stream inside QStarem.
2. Open the site **hamburger menu** (Account preferences / Appearance / Subtitles area).
3. Tap **App settings**.
4. Choose one of six icons and tap **Save**.

| Icon | Name |
|------|------|
| 1 | Q Play (default) |
| 2 | Film Reel |
| 3 | Z Waves |
| 4 | Viewfinder |
| 5 | Orbital |
| 6 | Clapper |

The home-screen icon updates after save (may take a moment on some launchers).

## Auto-updates

QStarem checks [GitHub Releases](https://github.com/perlytiara/QStarem-Android/releases/latest) on launch for a newer APK.

1. If an update is available, the app downloads it in the background (~550 MB).
2. On mobile data, you are asked before the download starts.
3. When the download finishes, a banner appears with **Install now** — Android always shows a system confirmation before installing.

You can also open **Hamburger menu → App settings → Check for updates**, or tap **Install update** when a download is ready.

**Notes:**

- Updates must be signed with the same release key; otherwise uninstall the old build first.
- Allow **Install unknown apps** for QStarem if Android prompts you.

## Settings

App settings live **inside Z-Stream**, not in Android system settings:

- **Hamburger menu → App settings** — home URL, ad blocker, P-Stream toggle, app icon, updates, clear browsing data
- While watching: **Return to browse** appears in the same menu

## Features

| Feature | Description |
|---------|-------------|
| Z-Stream frontend | Default home URL `https://zstream.mov`, configurable in App settings |
| P-Stream | Toggle bundled extension for enhanced sources |
| Ad blocking | uBlock Origin, AdGuard, or off |
| PiP | Auto on home press (Android 12+), swipe-up gesture while playing |
| Media controls | Lock-screen and notification controls with poster art when backgrounded |
| App icons | Six launcher icons, switchable in App settings |
| Auto-updates | GitHub Releases check on launch; background APK download and install prompt |

## Disclaimer

QStarem is a browser shell for user-configured streaming frontends. It does not host, index, or distribute content. Bundled extensions are subject to their own licenses:

- P-Stream — MPL 2.0
- uBlock Origin — GPLv3
- AdGuard — open source (see extension package)

## Build from source

### Requirements

- Android SDK API 36
- Java 17 (`brew install openjdk@17`)
- `curl`, `unzip`, optional `jq`

### Steps

```bash
git clone https://github.com/perlytiara/QStarem-Android.git
cd QStarem-Android
./scripts/fetch-extensions.sh
./gradlew installDebug
```

Enable USB debugging, connect your device, and accept the install prompt.

### Release build

```bash
./scripts/fetch-extensions.sh
./gradlew assembleRelease
```

Release signing uses environment variables in CI (`ANDROID_KEYSTORE_*`). See `.github/workflows/release.yml`.

## Project structure

```text
app/src/main/kotlin/com/qstarem/   App code (GeckoView + Compose)
app/src/main/assets/extensions/  Bundled WebExtensions (fetched at build time)
scripts/fetch-extensions.sh        Download latest XPIs from Mozilla Add-ons
```

## Related projects

- **[QStarem Universal](https://github.com/perlytiara/QStarem-Universal)** — Rust/Tauri desktop counterpart for macOS, Linux, and Windows.

## License

QStarem app shell: [MIT](LICENSE). Third-party extensions retain their original licenses.

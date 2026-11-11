# roozTrans

A simple, free, **ad-free** English ⇄ Chinese translator for Android, built for travel, including in mainland China **without a VPN**.

- **Type** text and get a translation, offline or online
- **Camera / photo**: translate text in a picture; the translation is drawn over the original
- **Screen translator**: a floating bubble, a Quick Settings tile, or a **Volume-Down double-press** captures the screen and overlays the translation
- **Works offline**: on-device text recognition and translation (Google ML Kit). Download the language packs once, before you travel
- **Optional online AI** with **your own** API key: Claude, OpenAI or DeepSeek. In auto mode it picks a provider by your country (DeepSeek in mainland China, where the others are blocked) and falls back to offline
- 8 colour themes; no accounts, no analytics, no tracking

> ⚠️ Machine translation can be wrong. Don't rely on it for medical, legal, safety or other important decisions. See [TERMS.md](TERMS.md).

## Requirements
Android 11+ (API 30). The screen translator needs the Accessibility service, which you turn on in the app's Settings.

## Build
Requires JDK 17+ and the Android SDK (compileSdk 36).

```sh
./gradlew assembleDirectRelease   # APK to share yourself
./gradlew bundlePlayRelease       # AAB for Google Play
```

There are two **flavours**:

| Flavour | For | Updates |
|---|---|---|
| `direct` | APK you share yourself | Built-in self-updater from your own server (optional) |
| `play` | Google Play | Google Play. No self-updater or install permission |

### Optional private config (never committed)
- `deploy.properties`: update URL and server for `./deploy.sh` (see `deploy.properties.example`). Without it, the direct build has no updater.
- `update.token`: shared secret between the app and `server/update.php`.
- `keystore.properties`: Play upload key (see `keystore.properties.example`).

`server/update.php` is a small PHP endpoint that serves the latest APK with resume (HTTP Range) support. Put the APK and `version.json` in a folder outside the web root.

## Privacy
Text is translated on the phone unless you turn on online translation and add your own key. Screenshots never leave the phone. Full details: [PRIVACY.md](PRIVACY.md).

## License
[MIT](LICENSE). Provided as is, without warranty.

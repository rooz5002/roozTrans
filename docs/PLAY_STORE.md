# Publishing roozTrans on Google Play

## 1. Build
```sh
./gradlew bundlePlayRelease
# → app/build/outputs/bundle/playRelease/app-play-release.aab
```
Signed with the **upload key** (`keystore.properties`). Enrol in **Play App Signing** (the default): Google holds the real signing key. If the upload key is ever lost, it can be reset through Play Console support.
Bump `version.properties` before each upload. Play rejects a versionCode it has already seen.

## 2. App setup in Play Console
- **App name:** roozTrans: English ⇄ Chinese
- **Default language:** English (UK)
- **App or game:** App · **Free**
- **Category:** Travel & Local (or Tools)
- **Contains ads:** No
- **Target audience:** 18+ (avoids the children's policy requirements)
- **Privacy policy URL:** https://github.com/rooz5002/roozTrans/blob/main/PRIVACY.md

## 3. Store listing
**Short description (≤80):**
Ad-free English ⇄ Chinese translator: text, photos & screen. Works offline.

**Full description:**
roozTrans is a simple, free English ⇄ Chinese translator with no ads, no accounts and no tracking. It was made for travel, including mainland China.

• Type text and translate instantly, even offline
• Translate photos: the translation is drawn over the original text
• Screen translator: tap the floating bubble, use the Quick Settings tile, or double-press Volume Down to translate whatever is on screen
• Works offline with on-device recognition and translation. Download the language packs once before you travel
• Optional online AI translation with your own Claude, OpenAI or DeepSeek API key. Auto mode picks a provider that works in the country you're in
• 8 colour themes

Privacy: screenshots never leave your phone. Only text is sent online, and only if you turn online translation on and add your own key.

Machine translation can make mistakes. Don't rely on it for medical, legal or safety decisions.
Open source (MIT): https://github.com/rooz5002/roozTrans

**Graphics needed:** 512×512 icon, 1024×500 feature graphic, 2 to 8 phone screenshots.

## 4. App content declarations
### Accessibility API (Permissions declaration form)
- **Is the app an accessibility tool (isAccessibilityTool)?** No
- **Core feature that uses it:** Screen translation. The user triggers a capture (floating bubble, Quick Settings tile or Volume-Down double-press). The app takes a screenshot with `AccessibilityService.takeScreenshot()`, recognises the text on the device and draws the translation over the screen with an accessibility overlay. It does not read window content (`canRetrieveWindowContent=false`), does not perform actions in other apps, and does not run in the background without the user's trigger. Key-event filtering only acts on Volume Down.
- **Why it's needed:** No other API lets the app capture and overlay any app's screen from a one-tap trigger without a screen-recording prompt on every use.
- **Prominent disclosure:** shown in-app before the user is sent to Accessibility settings, with Agree / No thanks (Settings → "Turn on screen translator", or the Quick Settings tile).
- **Video:** record a short screen capture showing the disclosure → Agree → enabling it in Settings → tapping the bubble on another app → translation overlay. Upload it to YouTube (unlisted) and paste the link.

### Data safety (suggested answers; check them against the form)
- **Does the app collect or share user data?** Yes, for these two:
  - *App activity → Other user-generated content* (text to translate): **shared** with the AI provider the user chose, **optional**, **for app functionality**, not stored by the developer
  - *App info and performance → Diagnostics* and *Device or other IDs*: **collected** by Google ML Kit, for analytics and diagnostics (see the ML Kit Play data-disclosure page)
- **Encrypted in transit:** Yes (HTTPS)
- **Can users request deletion:** the developer stores nothing. Uninstalling the app deletes everything on the phone

### Other forms
- Ads: No · Content rating questionnaire: Reference/Utility, no objectionable content
- News app: No · Government app: No · Financial features: None · Health: No

## 5. Testing track (new personal accounts)
New personal developer accounts must run a **closed test with at least 12 testers opted in for 14 consecutive days** before applying for production. Add testers' Google account emails to a closed-testing track, share the opt-in link, and keep them opted in for the full 14 days.

## 6. Installing the Play version over the direct version
They have the same package name but different signing keys, so Android won't update one into the other. Uninstall the direct version first. Settings and API keys need to be entered again.

# Accessibility API declaration (Play Console → App content → Sensitive permissions and APIs)

Policy: <https://support.google.com/googleplay/android-developer/answer/10964491>
Most recent clarification: [policy announcement of 29 Oct 2026](https://support.google.com/googleplay/android-developer/answer/18100766), with at least 30 days to comply. It covers the `isAccessibilityTool` flag and disclosure by app type.

## Answers
**Is roozTrans an accessibility tool (`isAccessibilityTool="true"`)?** No. It is a translator. The flag is not set, so it defaults to false.

**Which core feature uses the AccessibilityService API?**
Screen translation. When the user taps the floating bubble, taps the "Translate screen" Quick Settings tile, or double-presses Volume Down, the app takes a screenshot with `AccessibilityService.takeScreenshot()`. It recognises the text on the device (ML Kit OCR) and draws the translation over the screen with a `TYPE_ACCESSIBILITY_OVERLAY` window.

**What it does NOT do:**
- `canRetrieveWindowContent="false"`: it does not read other apps' view hierarchy or text.
- It performs no gestures or actions in other apps.
- It never captures without a user trigger.
- Key filtering acts only on Volume Down. Every other key is passed through untouched and not recorded.
- Screenshots are processed in memory and discarded. They are never stored or uploaded.

**Why no alternative works:** MediaProjection shows a system "start recording?" prompt on every capture and needs a foreground service, which defeats a one-tap translate-what-I'm-looking-at feature. No other API provides an overlay plus a capture over any app from a hardware-key trigger.

**Config (`app/src/main/res/xml/accessibility_config.xml`):** `canTakeScreenshot`, `canRequestFilterKeyEvents`, `typeWindowStateChanged` (used only to re-show the bubble after the screen wakes), `canRetrieveWindowContent=false`.

## Prominent disclosure (already in the app)
- **Where:** Settings → "Turn on screen translator", the first-run Welcome screen, and the Quick Settings tile when the service is off. All three go through the same dialog.
- **Content:** what the service is used for, what it does not do, where the data goes, and how to turn it off (`R.string.a11y_disclosure`).
- **Consent:** "Agree" / "No thanks" buttons, not cancellable by tapping outside. Only "Agree" leads to the system Accessibility settings.
- **Store listing:** the full description has a "Use of the Accessibility service" section, as the October 2026 rules require for apps that aren't accessibility tools.

## Video (required)
Record about 60 to 90 seconds on a phone (screen recorder), upload it to YouTube as **Unlisted**, and paste the link in the form. Show:
1. A fresh install opening on the Welcome screen.
2. Tapping "Turn on screen translator", then the disclosure, then **Agree**.
3. Android Accessibility settings, turning roozTrans on and confirming.
4. Opening another app with Chinese text, then tapping the bubble.
5. The translation overlay appearing, then tapping to close it.
6. Optionally, a Volume Down double-press doing the same.

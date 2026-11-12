# Accessibility API declaration (Play Console → App content → Sensitive permissions and APIs)

Policy: <https://support.google.com/googleplay/android-developer/answer/10964491>
Most recent clarification: [policy announcement of 29 Oct 2026](https://support.google.com/googleplay/android-developer/answer/18100766), with at least 30 days to comply. It covers the `isAccessibilityTool` flag and disclosure by app type.

## Answers
**Is roozTrans an accessibility tool (`isAccessibilityTool="true"`)?** No. It is a translator. The flag is not set, so it defaults to false.

**Which core feature uses the AccessibilityService API?**
Screen translation. When the user taps the floating bubble, taps the "Translate screen" Quick Settings tile, or double-presses Volume Down, the app takes a screenshot with `AccessibilityService.takeScreenshot()`. It recognises the text on the device (ML Kit OCR) and draws the translation over the screen with a `TYPE_ACCESSIBILITY_OVERLAY` window.

**What it does NOT do:**
- `canRetrieveWindowContent="false"`: it does not read other apps' view hierarchy or text.
- It performs no gestures, taps or text input in other apps. The only system actions are both user-triggered: (a) after a tap on the Quick Settings tile it closes the notification panel (`GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE`) so the screen underneath can be captured; (b) after a Volume Down double-press it raises the volume one step to undo the first press.
- It never captures without a user trigger.
- Key filtering acts only on Volume Down. Every other key is passed through untouched and not recorded.
- Screenshots are processed in memory and discarded. They are never stored or uploaded.

**Why no alternative works:** MediaProjection needs a new system "start recording/casting?" consent each session, a foreground service and a persistent screen-sharing indicator, which defeats a one-tap translate-what-I'm-looking-at feature. No other API provides an overlay plus a capture over any app from a hardware-key trigger.

**Config (`app/src/main/res/xml/accessibility_config.xml`):** `canTakeScreenshot`, `canRequestFilterKeyEvents`, `typeWindowStateChanged` (used only to re-show the bubble after the screen wakes), `canRetrieveWindowContent=false`.

## Declaration form answers
1. **Purpose:** App functionality (only).
2. **Does the app collect or share personal or sensitive data through accessibility capabilities?** **Yes**, to stay consistent with the Data safety form: *App activity → Other user-generated content*. The screenshot itself never leaves the phone, but the text recognised from it is sent to the user's chosen AI service when they have turned on online translation with their own key. Optional, ephemeral, not stored by the developer.
3. **Video link:** see below.

If the way the app uses the service ever changes, submit this form again.

## Prominent disclosure (already in the app)
- **Where:** Settings → "Turn on screen translator", the first-run Welcome screen, and the Quick Settings tile when the service is off. All three go through the same dialog.
- **Content:** what the service is used for, what it does not do, where the data goes, and how to turn it off (`R.string.a11y_disclosure`).
- **Consent:** "Agree" / "No thanks" buttons, not cancellable by tapping outside. Only "Agree" leads to the system Accessibility settings.
- **Store listing:** the full description also has a "Use of the Accessibility service" section. The policy page does not strictly require this, but it helps reviewers. The in-app dialog is what counts; the listing text cannot replace it.

## Video (required)
Record about 60 to 90 seconds on a phone (screen recorder), upload it to YouTube as **Unlisted**, and paste the link in the form. Google's checklist requires the app opening, the **whole** disclosure (scroll slowly if it scrolls), **both** the decline and the consent flow, and a core feature. Show:
1. A fresh install: open roozTrans from the launcher, landing on the Welcome screen.
2. Tap "Turn on screen translator". Let the full disclosure stay on screen for a few seconds, then tap **No thanks**. Show that nothing is turned on.
3. Trigger the disclosure again (tap "Turn on screen translator" again, or Settings → Turn on screen translator), then tap **Agree**.
4. Android Accessibility settings, turning roozTrans on and confirming.
5. Opening another app with Chinese text, then tapping the bubble.
6. The translation overlay appearing, then tapping to close it.
7. Optionally, a Volume Down double-press doing the same.

Add short captions (or a voice-over) naming each step, e.g. "Declining consent", "Granting consent", "Core feature: screen translation". Remove any personal notifications from the recording.

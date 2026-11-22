# Foreground service declaration (Play Console → App content → Foreground service permissions)

Policy: <https://support.google.com/googleplay/android-developer/answer/13392821>

roozTrans 1.8.2 declares `FOREGROUND_SERVICE_SPECIAL_USE` for `KeepAliveService`. The manifest carries the subtype text in `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`.

## Form answers
**Foreground service type:** Special use.

**Feature that uses it:** "Keep running while asleep" for the screen translator. Users turn it on and off in Settings → Screen translator. It is on by default after the user has enabled the screen translator.

**Description (paste):**
> roozTrans has a user-enabled screen translator: the user taps a floating bubble, a Quick Settings tile, or the "Translate screen" button in this notification, and the app translates the text on screen. On many phones (Samsung, Huawei, Xiaomi) battery savers kill the app while the phone sleeps, and Android then treats the translator as crashed, so it stops working until the user turns it off and on in the Accessibility settings. The foreground service shows a minimal "Screen translator ready" notification with a "Translate screen" action, and keeps the process alive so the translator is available when the user needs it. It does no background work: no network, location, sensors or recording. The user can turn it off in Settings at any time, and it stops when the screen translator is turned off.

**Why no other foreground service type fits:** it is not media playback, location, data sync, camera, microphone, phone call, health, connected device or media projection. It exists only to keep a user-enabled feature available. WorkManager or alarms can't help, because the problem is the process being killed while idle, not a deferred task.

**Impact on the user if the system defers or interrupts it:** the screen translator stops working without warning until the user re-enables it by hand in Android's Accessibility settings.

**Video link:** the same unlisted YouTube video as the Accessibility declaration, step 8 (the notification, its Translate screen action, and switching it off in Settings).

## Risk
"Keeping a process alive" is a grey area for `specialUse`. If Google rejects it:
- ship a Play build with the keep-alive off by default (or removed),
- keep it in the self-distributed build,
- in the Play build, rely on the "Keep it on" battery guide and the "stopped — tap to fix" bar.

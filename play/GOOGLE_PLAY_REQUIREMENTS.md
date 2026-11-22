# Google Play terms and requirements: roozTrans compliance

_Checked 11 November 2026. Links marked with a policy name go to Google's [Developer Program Policy centre](https://play.google.com/about/developer-content-policy/); search there for the named policy._
_ Google changes these often. Re-check the linked official pages before each submission._

## Agreements you accept when registering
| Document | What it means for you |
|---|---|
| **Developer Distribution Agreement** (shown at sign-up) | The contract with Google: you're responsible for the app, its legality and its support; Google may remove apps that break the policies; one-time US$25 registration fee |
| [Developer Program Policies](https://play.google.com/about/developer-content-policy/) | All content and behaviour rules (below) |
| **Play Console Terms of Service** (shown at sign-up) | Use of the Console itself |
| **Google Play Terms of Service** | Terms between Google and Play users |

## Account
| Requirement | Status |
|---|---|
| **Identity verification** (government ID; personal accounts show limited details publicly) | ⏳ You're doing this now |
| **Public developer contact email** | ✅ support@rooz.com in the Console (Play requires an email). Terms and privacy pages point to the web form https://rooz.com/?contact=roozTrans |
| **Testing requirement for new personal accounts:** a closed test with **≥ 12 testers opted in for 14 consecutive days** before you can apply for production ([official page](https://support.google.com/googleplay/android-developer/answer/14151465)) | ⏳ After the first upload. See README step 6. Organisation accounts (registered with a D-U-N-S number) are exempt |
| **Android developer verification** ([blog](https://developer.android.com/blog/posts/android-developer-verification-rolling-out-to-all-developers-on-play-console-and-android-developer-console)): apps on certified devices must come from verified developers. Enforcement began 30 Sep 2026 in Brazil, Indonesia, Singapore and Thailand, and goes global in 2027, **including sideloaded APKs** | ⚠️ Play builds are covered by your Play registration. The **direct APK** you share yourself must also be registered (package + signing key) before global enforcement. It's currently signed with a debug key, so plan to move it to a proper release key (one reinstall on existing phones) |

## Technical requirements
| Requirement | Status |
|---|---|
| **Android App Bundle (.aab)** for new apps | ✅ `./gradlew bundlePlayRelease` |
| **Target API level:** new apps and updates must target **API 36** from 31 Aug 2026 ([official](https://support.google.com/googleplay/android-developer/answer/11926878)) | ✅ targetSdk 36 |
| **16 KB memory page size** for native libraries (apps targeting Android 15+) | ✅ arm64 `.so` files are 16 KB-aligned (0x4000), checked with `readelf` |
| **Play App Signing** (Google holds the app-signing key, you sign uploads with an upload key) | ✅ Upload key at `~/keys/rooztrans-upload.jks`. **Back it up.** |
| **Package name is permanent:** `com.rooz.trans` | ✅ Note: the Play and direct versions share it but are signed with different keys, so users must uninstall one before installing the other |
| **No self-updating outside Play** ([Device and Network Abuse](https://play.google.com/about/developer-content-policy/)) | ✅ The Play flavour has no updater, `REQUEST_INSTALL_PACKAGES` or `UPDATE_PACKAGES_WITHOUT_USER_ACTION` (checked in the merged manifest) |
| **`REQUEST_INSTALL_PACKAGES` restricted permission** | ✅ Not in the Play build |

## Policies that apply to roozTrans
| Policy | Status |
|---|---|
| **[AccessibilityService API](https://support.google.com/googleplay/android-developer/answer/10964491).** Apps that aren't accessibility tools must: (1) explain the use in the store listing, (2) show an in-app prominent disclosure with affirmative consent, separate from the privacy policy, (3) file the declaration form, (4) provide a video of the flow. The API may not be used to change user settings without consent, bypass privacy controls, or act autonomously | ✅ (1) listing section, (2) Agree/No thanks dialog, (3)+(4) see `declarations/accessibility.md`. ⏳ Video to record on a phone |
| **[User Data](https://play.google.com/about/developer-content-policy/):** privacy policy linked in the Console **and** inside the app; disclosure and consent for sensitive data; secure transmission | ✅ https://rooz.com/rooztrans/privacy.html, in-app Settings → About and the Welcome screen; HTTPS only. ⏳ Upload the pages (`server/publish-legal.sh`) |
| **Data safety section** must be accurate and match the privacy policy | ✅ Drafted in `declarations/data-safety.md` |
| **[Permissions](https://support.google.com/googleplay/android-developer/answer/16558241):** request only what you need | ✅ Internet, network state, notifications and the special-use foreground service. Camera and photos go through system intents and the photo picker, with no permission. `WRITE_SECURE_SETTINGS` (auto-repair) and `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` are only in the direct build |
| **[Foreground service types](https://support.google.com/googleplay/android-developer/answer/13392821):** each declared type needs a Console declaration with a description and a video | ⏳ `specialUse` for the keep-running notification: see `declarations/foreground-service.md` |
| **[Impersonation / Intellectual property](https://play.google.com/about/developer-content-policy/):** don't imply endorsement by another brand | ✅ Claude, OpenAI and DeepSeek are named only as optional services the user connects with their own key, with no logos. The icon is the generic open-licence Material "translate" symbol, not Google Translate's logo |
| **[Metadata](https://play.google.com/about/developer-content-policy/):** no misleading claims, keyword stuffing, or "best/#1" claims; screenshots must show the real app | ✅ Screenshots are real emulator captures of the Play build; the listing states the limitations |
| **Content rating** questionnaire required | ⏳ See `declarations/other-app-content.md` |
| **Target audience** (Families policy if any part targets children) | ✅ 18+, not for children |
| **Ads** declaration | ✅ No ads |
| **Store graphics:** 512×512 icon (32-bit PNG), 1024×500 feature graphic (no alpha), 2–8 phone screenshots (min 320 px, max 3840 px, longest side ≤ 2× shortest) | ✅ `graphics/`: screenshots 1080×2160 (exactly 2:1) |

## Things that commonly get first submissions rejected (and how roozTrans avoids them)
- Accessibility used without a separate in-app disclosure, or with the disclosure only in the privacy policy → separate dialog ✅
- Privacy policy URL that's a PDF, editable doc, or unreachable → static HTML page on your site ✅ (once uploaded)
- Data safety answers that don't match the actual network traffic → table matches the code; country lookup only runs once a key is set ✅
- App self-updating or downloading code → removed in the Play flavour ✅
- Broken or crashing first run on Google's test devices → test on the closed track first ⏳

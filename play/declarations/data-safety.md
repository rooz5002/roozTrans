# Data safety form (Play Console → App content → Data safety)

These are suggested answers. Check each against the live form, because Google's questions and definitions change.
Guide: Play Console Help → "Provide information for Google Play's Data safety section".
ML Kit's own disclosure: ML Kit documentation (developers.google.com/ml-kit) → "Data disclosure".

## Overview
| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **Yes** (HTTPS only) |
| Do you provide a way for users to request that their data is deleted? | Nothing is stored by the developer. Uninstalling deletes all local data. Answer as the form allows ("data is not retained" / no account). |

## Data types
| Data type | Collected | Shared | Ephemeral | Required? | Purpose | Why |
|---|---|---|---|---|---|---|
| **App activity → Other user-generated content** (the text being translated) | Yes | Yes, but only at the user's direction to the AI provider they chose with their own key. That may count as a user-initiated transfer, which Google exempts from "sharing". | Yes | Optional | App functionality | Online translation via Claude, OpenAI or DeepSeek. Off until the user adds a key. Images are never sent |
| **Location → Approximate location** (country derived from IP) | Yes | Yes (sent to the IP-lookup services) | Yes | Optional | App functionality | Choosing a provider reachable in the user's country. Runs only once an API key is set |
| **App info and performance → Diagnostics** | Yes (by ML Kit) | No | No | Required | Analytics | Collected by the Google ML Kit library, as described in its disclosure |
| **Device or other IDs** | Check the ML Kit disclosure page | | | | | Declare it if ML Kit's page lists it at the time you fill in the form |

**Not collected:** name, email, contacts, photos/videos (processed on the device only), audio, files, messages, precise location, financial, health, web history, installed apps.

**Security practices:** encrypted in transit. No account. No ads SDK. Independent security review: No.

The privacy policy (<https://rooz.com/rooztrans/privacy.html>) must match these answers. If you change one, change both.

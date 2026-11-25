# roozTrans on Google Play

Everything needed to publish the **Play flavour** of roozTrans. The app code is shared with the direct-download version; only the build flavour differs (see the main README).

```
play/
├── README.md                      ← this checklist
├── GOOGLE_PLAY_REQUIREMENTS.md    ← Google's terms and policies, and how roozTrans complies
├── build-bundle.sh                ← builds the signed .aab into play/release/
├── listing/en-GB/                 ← title, short and full description (paste into the Console)
├── graphics/                      ← icon-512.png, feature-1024x500.png, screenshots/ (1080×2160)
├── declarations/                  ← Accessibility, Data safety, other App-content answers
└── tools/make-logo.py             ← the logo (R + 译): writes the app icons, icon-512 and feature graphic
```

## Checklist
1. **Register** a developer account at <https://play.google.com/console/signup> (personal, US$25, ID verification). Read and accept the agreements listed in `GOOGLE_PLAY_REQUIREMENTS.md`.
2. **Before submitting:**
   - Upload the privacy and terms pages: `server/publish-legal.sh` (from the repo root). Check that https://rooz.com/rooztrans/privacy.html opens.
   - Contact: Console email = support@rooz.com, website = https://rooz.com/?contact=roozTrans (already in `TERMS.md` / `PRIVACY.md`; re-run `server/publish-legal.sh` after editing them).
   - Back up `~/keys/` (upload key) somewhere off this laptop.
3. **Create app** in the Console: name `roozTrans: English ⇄ Chinese`, default language English (UK), App, Free. Accept the declarations.
4. **Store listing:** paste from `listing/en-GB/` and upload `graphics/icon-512.png`, `graphics/feature-1024x500.png` and `graphics/screenshots/*`. Category: Travel & Local. Add the contact email.
5. **App content:** fill in each form from `declarations/`. For Accessibility, record the video described in `declarations/accessibility.md` first.
6. **Closed test (required for new personal accounts):**
   - `play/build-bundle.sh` builds `play/release/roozTrans-<ver>.aab`.
   - Testing → Closed testing → create a track, upload the .aab, and add at least 12 testers' Google-account emails (an email list or Google Group).
   - Send them the opt-in link. They must stay opted in for **14 consecutive days**. Ask them to open the app a few times and leave feedback.
   - Testers who already have the direct version must **uninstall it first**, because the signing keys differ.
7. **Apply for production** (Dashboard), answering the questions about your test. Review usually takes a few days.
8. **Release:** for each update, bump the version (run `./deploy.sh` for the direct build, which also bumps it), then `play/build-bundle.sh`, then upload to Production.

## Keeping the two versions in step
- `./deploy.sh <ver>` raises the shared version number and publishes the **direct** APK to your server.
- `play/build-bundle.sh` builds the **Play** bundle with that same version number. Upload it to the Console yourself.
- Play rejects a versionCode it has already seen, so always deploy (or bump) before building a new bundle.

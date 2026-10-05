# Play Store launch kit

Everything needed to get Peek-a-Boo from this repo onto Google Play, in order.

## 1. One-time setup

1. **Developer account**: https://play.google.com/console (one-time $25). A new *personal* account
   has to run a **closed test with at least 12 testers for 14 days in a row** before it can publish
   to production, so start that early. Organisation accounts skip this.
2. **Payments profile**: Play Console › Settings › Payments profile. Needed before you can sell subscriptions.
3. **Upload key**: don't upload with `app/peekaboo-sideload.jks`; its password is public in this repo.
   Make a private one and keep it out of git:
   ```sh
   keytool -genkeypair -v -keystore ~/peekaboo-upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000
   export PEEKABOO_KEYSTORE=~/peekaboo-upload.jks PEEKABOO_KEY_ALIAS=upload
   export PEEKABOO_KEYSTORE_PASSWORD=... PEEKABOO_KEY_PASSWORD=...
   ```
   Turn on **Play App Signing** when you create the app (the default). Google then holds the real
   signing key and this one is only for uploads.

## 2. Build the bundle

```sh
./gradlew :app:bundleRelease        # -> app/build/outputs/bundle/release/app-release.aab
```

Don't pass `-PunlockPro=true` for Play builds; that flag is only for the sideload APK.

Already done in the code:
- targets Android 16 (API 36), which Play has required for new apps and updates since 31 Aug 2026
- Play Billing Library 8 (Play requires 8+ for new apps and updates from 31 Aug 2026)
- 16 KB page-size compatible native libraries (required for apps targeting Android 15+)
- no `INTERNET` permission, no `QUERY_ALL_PACKAGES`, no accessibility service

## 3. Create the subscription

Play Console › Monetise › Products › **Subscriptions** › Create:

| Field | Value |
|---|---|
| Product ID | `peekaboo_pro` (must match `ProStore.PRODUCT_ID`) |
| Name | Peek-a-Boo Pro |
| Base plan 1 | ID `monthly`, auto-renewing, 1 month |
| Base plan 2 | ID `yearly`, auto-renewing, 1 year |
| Offer | On the yearly plan (and optionally monthly): **free trial, 7 days, new customers only** |

Suggested launch prices (set the US price, let Play convert the rest, then lower India and other
price-sensitive markets by hand):

| | US | India | UK / EU |
|---|---|---|---|
| Monthly | $2.99 | ₹99 | £2.49 / €2.99 |
| Yearly | $14.99 (≈58% off) | ₹499 | £12.99 / €14.99 |

The paywall reads names, prices, trial lengths and the "Save N%" badge straight from Play, so
changing prices needs no app update. Add your Google account under **Settings › License testing**
to test purchases without being charged.

## 4. App content (Policy › App content)

**Privacy policy URL**: `https://github.com/shashank03-dev/peek-a-boo/blob/main/docs/privacy-policy.md`
(the repo has to be public for this link to work. Otherwise host the file anywhere public.)

**Ads**: No ads.

**Data safety**:
- Does the app collect or share user data? **No.** Everything is processed and stored on the device
  only, and the app has no network access. On-device-only processing doesn't count as "collection"
  under Play's definition.
- Encryption in transit: not applicable.
- Data deletion: users can delete everything in the app (Settings › Hold to clear history).

**Foreground service declaration** (`camera` type):
> Peek-a-Boo's core feature is detecting people looking at the user's screen over their shoulder.
> The user turns the guard on explicitly, and it keeps the front camera running in a foreground
> service while the phone is unlocked so it can warn them in real time while they use other apps.
> It stops automatically when the screen locks. A persistent notification shows while it runs, with a
> Stop button. If this were deferred or interrupted, the user would not be warned while someone was
> reading their screen.

Attach a short screen recording: turn on the guard › open another app › a second person looks › the
notch appears.

**Usage access (`PACKAGE_USAGE_STATS`)**: not a Play-restricted permission, but explain it in the
listing. It's only used, after the user grants it, to apply the Privacy Shield in the apps they
pick.

**Display over other apps**: not Play-restricted. The listing already explains the notch and shield.

**Target audience**: 18+ (or 13+). Not for children. Keep it out of the Families programme because of
camera snapshots.

**Content rating**: fill in the IARC questionnaire. Answers are all "no", so it comes out as Everyone/3+.

**Health/financial/government**: none.

## 5. Store listing

See `listing.md` in this folder for the title, short and full description. Screenshots:
`docs/screenshots/` has the current ones. Add two new ones: the Privacy Shield on (Settings › Try it)
and the Pro paywall. Feature graphic: 1024×500, eye logo plus "Catch shoulder surfers."

## 6. Release

1. Internal testing track › upload the `.aab` › add yourself › install from the opt-in link › buy
   Pro with a license-tester account › check the shield, blackout and protected apps.
2. Closed testing with 12+ testers for 14 days (personal accounts).
3. Production, with a staged rollout at 20%.

After launch, each update means: bump `versionCode`/`versionName`, run `bundleRelease`, upload.

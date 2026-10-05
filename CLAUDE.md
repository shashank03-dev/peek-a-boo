# CLAUDE.md

Rules for any AI agent working in this repo. These override default agent behaviour.

## Git rules (mandatory)

1. **All commits are authored under the repo owner's name.** Before committing, set:
   ```sh
   git config user.name  "shashank03-dev"
   git config user.email "shashankgowda3162@gmail.com"
   ```
   Commit with that identity only (author and committer).
2. **No pull requests.** Commit and push straight to `main` (`git push origin main`). Don't create feature branches or PRs.
3. **No AI attribution anywhere.** No `Co-Authored-By: Claude…`, no "Generated with Claude Code", no "authored by Opus/Sonnet", no model names, no session links — not in commit messages, code comments, docs, or release notes. Write commit messages as the owner would.

If something is unclear or a decision is the owner's to make, ask instead of guessing.

## Project

Peek-a-Boo — Android app (Kotlin + Jetpack Compose) that uses the front camera to detect people
peeking at the screen, shows a Dynamic-Island-style "Peeping · N" notch under the camera, and
keeps a daily report. The owner's face is enrolled so they are never counted as a peeper.

- `app/src/main/java/dev/shashank/peekaboo/`
  - `service/GuardService.kt` — camera foreground service (scans only while unlocked), session logging, overlay
  - `detect/` — ML Kit face detection (`PeekAnalyzer`), on-device LBPH face signatures (`FaceSignature`), peek debouncing (`PeekSessionTracker`)
  - `overlay/` — system overlay host + the notch pill composable
  - `data/` — Room (peek events + peeper signatures), DataStore settings, owner face store, report maths
  - `ui/` — iOS-style Compose UI: Guard, Report, Face ID, Settings tabs + onboarding
- Min SDK 26, target/compile SDK 35, ARM ABIs only.

## Build

```sh
./gradlew :app:testReleaseUnitTest      # unit tests
./gradlew :app:assembleRelease          # signed APK -> app/build/outputs/apk/release/app-release.apk
cp app/build/outputs/apk/release/app-release.apk release/PeekABoo.apk
```

After any app change: bump `versionCode`/`versionName` in `app/build.gradle.kts`, rebuild, copy the
APK to `release/PeekABoo.apk` and commit it — that file is the download link the owner installs from.
Release builds are signed with `app/peekaboo-sideload.jks` (password `peekaboo`) so new APKs install
over old ones; keep using it unless the owner provides a private key via `PEEKABOO_KEYSTORE*` env vars.

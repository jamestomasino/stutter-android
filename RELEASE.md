# Release Process (All Channels)

This doc describes the single release path used for GitHub, F-Droid, Obtanium, and Play Store.

## 0) Preconditions
- Timing regressions are release-blocking (see `TESTPLAN.md`).
- Update `versionCode` / `versionName` in `app/build.gradle.kts`
  (`versionCode` +1, `versionName` semver).
- Ensure metadata is up to date (title/short/full description, screenshots, feature graphic).
- Prepare release notes (GitHub Release notes and Play Store text if applicable).

## Release checklist (quick pass)
- [ ] `versionCode` / `versionName` updated
- [ ] Tests pass (`make test`)
- [ ] Lint passes (`make lint`)
- [ ] Tag created and pushed (`vX.Y.Z`)
- [ ] GitHub Release includes `app-release-signed.apk` and `app-release.aab`
- [ ] Optional: verify signing cert (`apksigner verify --print-certs app-release-signed.apk`)
- [ ] F-Droid sync completed (`scripts/fdroid-sync.sh`)
- [ ] Obtanium configured to use `app-release-signed.apk`
- [ ] Play Store upload completed (if applicable)

## 1) Local checks (recommended)
- `make test`
- `make lint`
- Optional: `make bundle` to verify `app-release.aab` locally.

## 2) Tag a release
- Commit your changes.
- Create an annotated tag `vX.Y.Z` and push it.
  Example:
  `git tag -a v1.2.3 -m "v1.2.3"`
  `git push origin v1.2.3`

## 3) GitHub Actions release (single source of truth)
On tag push, the `Android Release` workflow:
- Builds the release APK and AAB.
- Signs the APK with the release key.
- Uploads `app-release-signed.apk` and `app-release.aab` to the GitHub Release.
- Extracts signature files for F-Droid into a workflow artifact.

Note: Android launcher icons can be cached. If you change icons or other
launcher resources, do a clean uninstall/reinstall to verify the update.

## 4) F-Droid
- Run `scripts/fdroid-sync.sh` to sync `fdroiddata` metadata/signatures from the tag.
- Ensure the signatures artifact is uploaded and `metadata/org.tomasino.stutter.yml` is updated.

## 5) Obtanium
- Obtanium can track GitHub Releases. Point it at the repo and select
  `app-release-signed.apk` as the install asset.
  Suggested settings:
  - Source: GitHub (repo URL)
  - Preferred release asset: `app-release-signed.apk`
  - Update method: GitHub release tags

## 6) Play Store
- Upload `app-release.aab` from the GitHub Release to Play Console.
- Follow `PLAYSTORE.md` for App Signing + store listing + compliance steps.

## 7) Post-release
- Monitor issues/reviews and plan the next update.
- Add/update the Play/F-Droid changelog entry under `metadata/android/en-US/changelogs/`.

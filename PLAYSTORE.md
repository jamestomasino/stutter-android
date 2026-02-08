# Play Store Publishing Checklist

Date created: 2026-02-04
Repo status snapshot: 2026-02-04

This checklist tracks what is required to publish the app to Google Play. Mark items as complete as they are finished.

## 1) Play Console & Account
- [x] Google Play Developer account created (one-time $25 fee paid)
- [ ] Developer identity verification completed (individual or organization)
- [ ] App created in Play Console with correct package name (org.tomasino.stutter)
- [ ] App ownership, contact email, and support details set

## 2) Signing & Build Artifacts
- [ ] Release signing key created and securely stored
- [ ] Play App Signing enabled (upload key registered)
- [x] Play App Signing decision made: use existing signing key as app signing key
- [ ] Release AAB build configured and verified locally/CI
      (local: `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`)
      (CI: tag push runs Android Release workflow and uploads `app-release.aab`)
- [ ] Signing config wired for Play upload key (local or CI)
- [ ] Upload key / keystore storage documented (location, access, rotation plan)
- [x] versionCode and versionName updated for release (15 / 1.1.3)
- [x] Target SDK meets current Play requirements (targetSdk 34)
- [x] Min SDK and device support confirmed (minSdk 24)

### Play App Signing (use existing key)
When the app is created in Play Console:
1) Go to Play Console → your app → Release → Setup → App integrity.
2) Under Play App Signing, choose to use your own key and upload the existing
   signing keystore/certificate.
3) Keep the same app signing key used for F-Droid/Obtanium to preserve
   cross-store update compatibility.
4) Optionally, create a separate upload key for Play Console uploads.

## 3) Store Listing Assets
- [x] App name finalized (title)
- [x] Short description written
- [x] Full description written
- [x] Feature graphic (1024x500)
- [x] App icon (512x512) and adaptive icon verified
- [x] Phone screenshots (at least 2)
- [ ] Tablet screenshots (mark N/A if not supported)
- [ ] TV screenshots (mark N/A if not supported)
- [ ] Wear OS screenshots (mark N/A if not supported)
- [ ] Promo video (optional)

## 4) Policy, Privacy, and Compliance
- [x] Privacy policy URL published and accessible
- [ ] Data Safety form completed in Play Console
- [x] Content rating questionnaire completed
- [x] Ads declaration completed (if ads are used)
- [ ] App access details filled (if login required)
- [ ] Intellectual property and trademark review done
- [ ] Encryption / export compliance answered

## 5) Device Support Decisions
- [ ] Supported form factors decided (phone / tablet / TV / Wear)
- [ ] N/A marked for non-supported form factors in the store listing

## 6) Release Tracks & Testing
- [ ] Internal test track configured (recommended)
- [ ] Testers added and invited
- [ ] Pre-launch report reviewed (crashes/ANRs/compat)
- [ ] Closed testing (if needed for policy or QA)
- [ ] Production release created and staged
- [ ] Staged rollout percentage decided

## 7) Operational Readiness
- [ ] Customer support channel ready (email/website)
- [ ] Crash/ANR monitoring configured
- [ ] Release notes prepared
- [ ] Versioning and tagging strategy documented

## 8) Post-Launch
- [ ] Monitor reviews and ratings
- [ ] Track metrics (installs, retention, crashes)
- [ ] Plan for next update cadence

# Screenshot Workflow

This project uses a consistent, repeatable screenshot process. Capture four phone screenshots:

1) Light theme, homepage loaded (input shelf visible)
2) Light theme, during playback
3) Dark theme, homepage loaded (input shelf visible)
4) Dark theme, during playback

## Device + Environment
- Device: Pixel 7 (or similar 1080x2400 class)
- Font size: default
- Display size: default
- System theme: Light/Dark (set per capture)
- Notifications: disabled / Do Not Disturb
- App state: reset settings to defaults before each capture

## Steps (manual)
1) Launch Stutter.
2) Open Settings and tap:
   - Reset Visual Settings
   - Reset Timing & Features
   - Reset All
3) Return to Stutter and confirm the default sample text is loaded.
4) For the "homepage loaded" screenshot:
   - Input shelf expanded
   - No playback running
5) For the "during playback" screenshot:
   - Tap Play, wait for words to advance
   - Capture mid-playback
6) Repeat in Dark theme.

## Save locations
Save into:
- `metadata/android/en-US/images/phoneScreenshots/`

Suggested naming:
- `1.jpg` light theme, homepage loaded
- `2.jpg` light theme, during playback
- `3.jpg` dark theme, homepage loaded
- `4.jpg` dark theme, during playback

## Optional adb capture
If you want consistent captures via adb:
- `adb shell screencap -p /sdcard/stutter.png`
- `adb pull /sdcard/stutter.png <dest>`

Tip: uninstall/reinstall after launcher icon changes to avoid icon cache issues.

## Automated capture (instrumented)

1) Connect a device or emulator.
2) Run:
   `make screenshots`
3) The script pulls PNGs, converts to JPG, and writes:
   `metadata/android/en-US/images/phoneScreenshots/1.jpg..4.jpg`

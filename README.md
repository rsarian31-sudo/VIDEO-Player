# WAJID VIDEO PLAYER

Personal Android video player starter project for Samsung A55 5G.

## Included
- Local video library
- AndroidX Media3 / ExoPlayer playback
- Playback speed controls from 0.25x to 3x
- PIN-gated private folder that copies selected videos into app-private storage
- GitHub Actions workflow to build a debug APK

## Build APK from your phone
1. Open the **Actions** tab in this repository.
2. Select **Build Android APK** and tap **Run workflow** (or wait for the automatic build after a commit).
3. Open the completed workflow run.
4. Under **Artifacts**, download `WajidVideoPlayer-debug-apk`.
5. Extract the ZIP and install `app-debug.apk` on your Android device.

## Important limitations
This is an early starter build, not yet a full MX Player replacement. Gesture controls, subtitle selection, thumbnail previews, resume playback, and device testing remain to be added. The private folder is PIN-gated, but its video files are **not encrypted**; do not treat it as a secure encrypted vault.

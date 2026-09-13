# Android release checklist

## Before signing

- [ ] Pass `video_capture` and `audio_capture` consent tests on a physical Android 14+ device.
- [ ] Configure `-PoneApiBaseUrl=https://.../api/v1`; never ship the emulator URL.
- [ ] Deploy and verify LiveKit URL/API credentials and publisher pairing.
- [ ] Verify CameraX sampling, denial/revocation of permissions, and foreground notification.
- [ ] Verify medication alarms after reboot, timezone change and consent revocation.
- [ ] Run `./gradlew lintDebug testDebugUnitTest assembleDebug` in CI.
- [ ] Run the instrumented suite on a Pixel API 35+ emulator or physical device.
- [ ] Configure Play App Signing; keep the keystore outside Git/Gradle files.

## Privacy acceptance criteria

Frames are compressed in memory and sent to `/vision/frames`; the Android
client does not persist raw video. The backend may retain only derived,
approximate observations and consented short clips according to its retention
policy. LiveKit publisher sessions must be paired with the publisher role; a
caregiver session is subscribe-only.

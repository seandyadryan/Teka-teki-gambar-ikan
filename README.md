# Fish Riddle Club

An offline, English-language Android picture quiz with 16 illustrated fish, four expeditions, spoken clues and facts, and a collectible field guide. Built with Kotlin and Jetpack Compose for phones, tablets, and ChromeOS. Android 8.0 or later is required.

## Build locally

Use JDK 17 or a compatible newer JDK and the Android SDK with API 36 and Build Tools 36.0.0. Open the repo in Android Studio or set `ANDROID_HOME` to the SDK location.

```sh
./gradlew :app:lintRelease :app:testDebugUnitTest :app:assembleDebug
```

On Windows, use `gradlew.bat`.

## Sign the Play bundle

Use your existing upload key for subsequent releases. This project reads signing values from the environment. From PowerShell, enter the keystore password when prompted. The key password matches the store password for the original key.

```powershell
$env:FISH_UPLOAD_KEYSTORE = 'D:\KEYSTORE\tekatekigambarikan.jks'
$env:FISH_UPLOAD_ALIAS = 'tekatekigambarikan'
$env:FISH_UPLOAD_PASSWORD = Read-Host 'Upload key password' -MaskInput
try {
    .\gradlew.bat :app:bundleRelease :app:assembleRelease
} finally {
    Remove-Item Env:FISH_UPLOAD_PASSWORD
}
```

Find the signed Android App Bundle under `app/build/outputs/bundle/release`. The Play Console uses the upload certificate from this key with Play App Signing.

## GitHub Actions and privacy policy

Open **Actions → Android build → Run workflow** to run lint and package a debug APK and unsigned release AAB on an Ubuntu runner. Pushes do not trigger the Android workflow. The signed store bundle is built locally with the upload key.

The privacy policy is published from [docs/index.html](docs/index.html) by GitHub Pages. Store copy and artwork are in [play-store](play-store), along with a separate preview MP4 and the asset credits.

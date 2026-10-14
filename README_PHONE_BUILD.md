# Phone-only build and install

1. Download and extract `Satyam_Offline_Android_Project.zip` on your Android phone using the Files app or ZArchiver.
2. Open your GitHub repository in Chrome. Use **Desktop site** if needed.
3. Upload the *contents* of `satyam_offline_android` to the repository root, not the outer folder itself. Keep `.github/workflows/android-apk.yml`, `app/`, `build.gradle`, and `settings.gradle` in place.
4. Open the repository's **Actions** tab and run **Build Satyam Android APK**, or push a commit to `main`.
5. When the run finishes with a green check, open it and download artifact `satyam-reports-debug-apk`.
6. Extract the artifact ZIP and install the APK. Android may ask you to allow installation from your browser or file manager.
7. On first launch, create a local password. From the dashboard, choose **Import workbook (.xlsx/.xlsm or JSON backup)** and select your original `.xlsm` workbook from the phone.

The source ZIP deliberately contains only sheet headers and no order records. Import the original workbook directly on the device to avoid exposing business data in a public repository. Make the repository private after the build setup is complete.

This workflow produces a debug APK. It does not prove that VBA-driven calculations have been reproduced. The app reads cached cell values; Excel macros and formula recalculation are not executed on Android.

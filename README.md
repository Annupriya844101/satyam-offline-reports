# Satyam Agarbatti Offline Reports

Android-first, local-only workbook browser/editor for Satyam Agarbatti reports.

## Included features
- First-run local password lock; no account or server required.
- Offline dashboard with all 18 workbook sheet names and schema.
- Search worksheet rows, browse table view, add/edit/delete rows.
- Local persistent storage inside the app sandbox.
- Import `.xlsx` and `.xlsm` workbooks or restore a JSON backup.
- Export a JSON backup and export the current worksheet to CSV.
- GitHub Actions workflow builds a debug APK and publishes it as a workflow artifact.
- No Android `INTERNET` permission; workbook data stays on the device.

## First launch and data
The bundled seed contains worksheet names and headers only. It is intentionally schema-only so order IDs, SKU/customer data, and business records are not exposed when source code is temporarily hosted in a public repository. After installing the APK, import your original `Satyam Agarbatti Automation V6 DAily Report.xlsm` from your phone using **Import workbook**. The app parses worksheet cell values and cached formula results; it does not execute VBA macros or recalculate Excel formulas.

## Build using a phone
1. Upload the contents of this folder to the root of a GitHub repository, preserving `.github/workflows/android-apk.yml` and the `app/` folder.
2. In GitHub, open **Actions** and run **Build Satyam Android APK** (or push a commit to `main`).
3. Wait for a successful run, then download the `satyam-reports-debug-apk` artifact from that run and extract the APK.

## Important limitations
- This is a debug APK intended for initial device testing, not a signed Play Store release.
- Excel VBA macros are not executed. Macro-driven report totals, validation, date shifting, and formulas need a separate tested native implementation if you need identical Excel results.
- Imported workbooks are converted to editable cell-value tables. Formatting, charts, named ranges, formulas themselves, and VBA project code are not preserved in app storage.
- The local password is an app lock, not enterprise-grade encryption. Keep the phone locked and export backups to a private location.
- Test import/export, calculations, and restore with a copy of your workbook before relying on the app for business operations.

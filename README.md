# Call — a calling app for grandparents

Kotlin · Jetpack Compose · Material 3 · MVVM · Contacts Provider · Telecom (InCallService) · min SDK 26

## Build and install the APK
1. Install **Android Studio** (Koala 2024.1 or newer; it bundles JDK 17 and the Android SDK).
2. **File → Open** and choose this `CallApp` folder. Let Gradle sync finish (Android Studio downloads Gradle 8.9 by itself).
3. Connect an Android phone (Settings → About → tap *Build number* 7 times → Developer options → enable *USB debugging*), then press **Run ▶**.
4. To get an APK file: **Build → Build Bundle(s)/APK(s) → Build APK(s)**. The file is `app/build/outputs/apk/debug/app-debug.apk`. Copy it to the phone and open it (allow "install unknown apps" when asked).
Command line alternative (needs ANDROID_HOME set and a Gradle install): `gradle assembleDebug`.

## First run on the phone
1. Allow the permission prompts (Contacts, Phone, Call history, Notifications).
2. Tap **Set up** on the Home banner (or Settings → *Make Call my phone app*) and choose **Call**.
   Android only lets the *default phone app* answer/decline calls and show its own incoming-call screen.
   Until this is done, outgoing calls, contacts, dialer and call logs work, but incoming calls are shown by the system phone app.
3. Android 14+: in Settings, allow **full-screen calls** if it says they are off (needed to show the big screen when the phone is locked).

## What is real
- Home grid, Contacts, Add/Edit/Delete: read and written through `ContactsContract`; photos are the contacts' own photo URIs.
- Call logs: `CallLog.Calls` (All / Incoming / Outgoing / Missed, duration, one-tap callback).
- Outgoing calls: `TelecomManager.placeCall`.
- Incoming calls: `InCallService` + `Call.answer()` / `Call.reject()`, full-screen `InCallActivity` with the caller's contact photo as background, separate green **Answer** and red **Decline** buttons (no swipe), plus a CallStyle notification with full-screen intent.

## Layout
- `data/` repositories (contacts, call log, DataStore settings) and models
- `telecom/` InCallService, CallManager, notifications, in-call screen
- `system/` permissions, default-dialer role, call placement
- `ui/` theme, reusable components, screens, ViewModels, navigation

## Extra features
- **Pin contacts:** hold a contact (Home or Contacts) -> *Pin to top* / *Unpin*. Pinned contacts show a pin badge and stay first.
- **During a call:** Mute, Keypad (sends tones for menus), Speaker, **Add call** (holds the current call and opens the dialer), and **Swap** between two calls.

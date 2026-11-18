# Kitagawh!

Kitagawh! is a Jarvis-style AI assistant for Android that controls the phone
with voice instructions. Say the word **Jarvis**, the panel opens with
"What's up bruh!!", you speak a command, Kitagawh does it, speaks the reply,
and closes itself.

Brand: a simple white **-_-** on black. No emoji anywhere in the UI.

## What it does

- **Voice control (Jarvis mode)** - a foreground service listens for the wake
  word "Jarvis" all the time while powered on. Commands include:
  - "Open YouTube" / "Open <any app name>"
  - "Turn Wi-Fi on / off", "Forget <network name>"
  - "Torch on / off" (flashlight)
  - "Call <number>"
  - "Take a screenshot", "What time is it", "Battery"
  - "Go home", "Go back", "Recent apps"
  - Anything else goes to the Groq chat model (`llama-3.3-70b-versatile`)
    when a Groq API key is saved in Settings.
- **Easy Touch** - a floating "-_-" icon that keeps running after the app is
  closed. Tap it (only a tap) and a pentagon menu opens with five assignable
  slots: Screen Search, Torch, Call Jarvis, Settings, Design.
  - **Screen Search** screenshots the screen through the accessibility
    service and explains it in an overlay, using the Groq vision model
    (`meta-llama/llama-4-scout-17b-16e-instruct`).
  - **Design** lets you change icon size, colour, and one of five icon
    animations: Rotate, Pulse, Bounce, Fade, Shake.
  - **Settings** lets you assign any feature to any pentagon slot.
- **Permissions page** - every permission row shows a live status (re-checked
  each time you return) and opens the real system settings screen.
- **About page** - a humanized story of Kitagawh, its emotions and its work,
  with a legal warning and an Accept button.
- **Simple account** - create account with name, mobile number and age group;
  sign in with just the number. The number is checked against the SIM in the
  phone whenever Android allows reading it.
- **Settings** - Restart Kitagawh, Clear Cache, Clear API Keys, an API
  section with named boxes for Groq / OpenAI / Gemini (each with an info
  button showing how to get that key, plus Save and Edit buttons), and
  Contact Admin (opens email to csharan2122011@gmail.com).
- **Homepage** - a continuously rotating animated sphere with a glowing aura,
  Start AI -> Power On Jarvis with a powering-on animation (the button then
  becomes Power Off), picture-in-picture support, and slide/fade transitions
  between every screen. Every screen has a back arrow at the top-left that
  goes back without reloading.

## Which AI, and which is better for free?

This version uses **Groq** for both chat and vision, because Groq has a
generous free tier and is very fast. Get a free key at console.groq.com and
paste it in **Settings -> API Section -> Groq**. OpenAI and Gemini keys can
also be stored in Settings for future use.

## Building the APK with GitHub Actions

The repo ships with `.github/workflows/build.yml`. On every push, GitHub
Actions:

1. Checks out the repo on `ubuntu-latest`.
2. Installs JDK 17 (Temurin) and Gradle 8.7.
3. Runs `gradle assembleDebug --no-daemon`.
4. Uploads the APK as an artifact named **Kitagawh-debug-apk**.

To get the APK: open the repo on GitHub -> **Actions** -> latest run ->
download **Kitagawh-debug-apk**, unzip it, and install `app-debug.apk` on
your phone (you may need to allow "install from unknown sources" for your
browser or Files app).

## Tech

- Kotlin, classic Views + XML layouts (no Compose).
- minSdk 26, targetSdk / compileSdk 34.
- Only four dependencies: core-ktx, appcompat, material, constraintlayout.
  Networking is plain `HttpURLConnection`, JSON is `org.json`.
- All data (account, API keys, design, slots, power state) lives in
  SharedPreferences on the device.

## Honest Android limits

Kitagawh is powerful, but Android sets hard rules that no normal app can
break, and the app handles them openly instead of pretending:

- **Accessibility cannot be forced to stay on.** Only the user can enable an
  accessibility service, and Android gives apps no way to stop it being
  turned off. Kitagawh runs a watchdog instead: if the service disconnects,
  it posts a notification asking you to re-enable it.
- **Wi-Fi toggle:** below Android 10, Kitagawh toggles Wi-Fi directly with
  `WifiManager`. On Android 10+, apps are forbidden from toggling Wi-Fi, so
  Kitagawh opens the system Wi-Fi panel and tells you to tap the switch.
- **Forgetting a Wi-Fi network** requires privileged system permissions, so
  Kitagawh opens Wi-Fi settings and tells you to long-press the network and
  tap Forget.
- **SIM number:** many carriers do not store a number on the SIM, and Android
  may refuse to read it. When the SIM number cannot be read, the sign-in
  check falls back to the account number saved on the phone and the app says
  so.
- **Screenshots** through accessibility need Android 11+; on older versions
  Screen Search and the screenshot command explain that instead of failing
  silently.

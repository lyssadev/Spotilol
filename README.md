<p align="center">
  <img src="art/spotilolicon.png" alt="Spotilol Logo" width="128" height="128">
</p>

<h1 align="center">Spotilol</h1>

<p align="center">
  <a href="https://github.com/lyssadev/Spotilol/stargazers">
    <img src="https://img.shields.io/github/stars/lyssadev/Spotilol?style=for-the-badge&logo=starship&labelColor=0d0d0d&color=1DB954" alt="stars"/>
  </a>
  &nbsp;
  <a href="https://github.com/lyssadev/Spotilol/releases">
    <img src="https://img.shields.io/github/downloads/lyssadev/Spotilol/total?style=for-the-badge&logo=download&labelColor=0d0d0d&color=1DB954" alt="downloads"/>
  </a>
  &nbsp;
  <a href="https://github.com/lyssadev/Spotilol/releases/latest">
    <img src="https://img.shields.io/github/v/release/lyssadev/Spotilol?style=for-the-badge&logo=github&labelColor=0d0d0d&color=1DB954" alt="version"/>
  </a>
  &nbsp;
  <a href="https://github.com/lyssadev/Spotilol/forks">
    <img src="https://img.shields.io/github/forks/lyssadev/Spotilol?style=for-the-badge&logo=git&labelColor=0d0d0d&color=1DB954" alt="forks"/>
  </a>
  &nbsp;
  <a href="https://github.com/lyssadev/Spotilol/commits/main">
    <img src="https://img.shields.io/github/last-commit/lyssadev/Spotilol?style=for-the-badge&logo=git&labelColor=0d0d0d&color=1DB954" alt="last commit"/>
  </a>
</p>

<p align="center">
  a lil Android app that wraps Spotify's web player with built-in adblocking.
</p>

it's a fork of **Spotifuck** by **deviato**, ported from smali to clean Kotlin. all free, all open-source.

runs a local MITM proxy with a custom CA cert so Spotify doesn't clock you're on a WebView. that's the magic trick. everything else passes through untouched.

---

## Preview

<div align="center">
  <img src="art/spotilol_ss1.webp" alt="screenshot 1" width="30%" style="max-width: 250px; margin: 4px; border-radius: 12px;" />
  <img src="art/spotilol_ss2.webp" alt="screenshot 2" width="30%" style="max-width: 250px; margin: 4px; border-radius: 12px;" />
  <img src="art/spotilol_ss3.webp" alt="screenshot 3" width="30%" style="max-width: 250px; margin: 4px; border-radius: 12px;" />
</div>

---

## Download

grab the latest APK from the [releases page](https://github.com/lyssadev/spotilol/releases/latest).

download the `.apk` file and install it on your device. you may need to toggle **"Install from unknown sources"** in your Settings.

---

## Features

- blocks audio ads 🚫
- media notification with play/pause, skip, seek, like/unlike
- works with lock screen, Bluetooth, Wear OS
- autoplay modes: off, once at start, or permanent
- mobile-friendly CSS/JS layout tweaks
- AMOLED dark mode (pure black)
- keeps screen on while you're vibing
- browse your library through Spotify's API
- update checker (auto & manual)

---

## Requirements

- Android 8.0+ (API 26)
- a Spotify account (free or premium)
- Google Chrome / WebView (comes with your phone)

---

## Quick Start: The Certificate Thing

Spotilol generates a local CA cert so Spotify doesn't know you're in a WebView. it lives on your device, stays on your device.

1. open Spotilol — you'll see the **"Certificate Required"** screen
2. tap **"Export .pem"** to save it to your Downloads
3. go to **Settings > Security > Encryption & Credentials > Install a certificate > CA certificate**
4. find `spotilol_ca.pem` in your Downloads and tap it
5. it'll warn you about network monitoring — tap **"Install anyway"**
6. come back to Spotilol and tap **"Check"**. if it worked, you're in.

> **Note:** if you ever clear your device's credential storage (like after a factory reset), you'll have to do this again.

---

## Build It Yourself

```bash
git clone https://github.com/lyssadev/Spotilol
cd Spotilol
./gradlew assembleDebug
```

APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Contributing

contributions are welcome. open issues, throw PRs, suggest stuff — free for all.

---

## Credits

Spotilol exists because deviato did the reverse-engineering work on Spotifuck. this ports the core logic from smali to Kotlin with extra features and maintenance.

**Open-sourced by lyssadev <3 deviato**
<div align="center">
  <img src="app/src/main/res/drawable/carpet_banner.jpg" alt="Carpet Map Reader" width="100%" style="max-width: 800px; border-radius: 12px;" />
  <br/><br/>
  <h1 dir="rtl">نقشه‌خوان فرش</h1>
  <h3 dir="rtl">Carpet Map Reader</h3>
  <p dir="rtl">
    <strong>یک ابزار هوشمند برای قالی‌بافان — تبدیل نقشه‌های فرش به صدای گویا</strong>
    <br/>
    <em>An intelligent tool for carpet weavers — turn carpet maps into spoken audio</em>
  </p>
  <p>
    <img src="https://img.shields.io/badge/Kotlin-2.2.10-purple?logo=kotlin" alt="Kotlin" />
    <img src="https://img.shields.io/badge/Compose-2024.09-4285F4?logo=jetpackcompose" alt="Compose" />
    <img src="https://img.shields.io/badge/AGP-9.1.1-3DDC84?logo=android" alt="AGP" />
    <img src="https://img.shields.io/badge/MinSDK-24-10b981" alt="Min SDK" />
    <img src="https://img.shields.io/badge/Room-2.7.0-FF6F00" alt="Room" />
    <img src="https://img.shields.io/badge/Gemini_AI-Firebase-FFCA28?logo=firebase" alt="Gemini AI" />
  </p>
</div>

---

**Carpet Map Reader** helps carpet weavers read traditional Persian maps using OCR (ML Kit), Gemini AI, and Persian TTS — scanning grid patterns and speaking each cell aloud.

> **نقشه‌خوان فرش** با استفاده از OCR و گفتار متنی، نقشه‌های فرش را می‌خواند و هر خانه را به فارسی گویا می‌کند.

---

## Features

- **📸 Scan** — Take/import a carpet map photo
- **🧠 AI OCR** — Number recognition via ML Kit & Gemini AI
- **🎤 Persian TTS** — Natural spoken output with 65+ MP3 audio files
- **🎨 Color Detection** — Identifies dominant carpet colors per cell
- **🗂️ Projects** — Save/load multiple maps with auto-resume
- **🧭 Directions** — RTL, LTR, ZIGZAG traversal patterns
- **📐 Map Types** — GRID (standard) / NUMERICAL (color-code with sections)

## Tech Stack

Kotlin · Jetpack Compose + Material 3 · MVVM · Room · ML Kit · Firebase Gemini AI · Retrofit · Moshi · Coil · Gradle 9.3 / AGP 9.1

## Project Structure

```
app/src/main/java/com/farsh/carpetmapreader/
├── MainActivity.kt
├── ui/          CarpetApp.kt, CarpetViewModel.kt, theme/
├── processor/   ReaderEngine, OCR, GridDetector, NumericalParser, TTS, AudioPlayer
└── data/        AppDatabase, MapDao, MapProject, MapCell, MapRepository
```

## License

```
MIT License — Copyright (c) 2026 Farshid
```

---

<div align="center" dir="rtl">
  <sub>ساخته شده با ❤️ برای قالی‌بافان ایران</sub>
  <br/>
  <sub>Made with ❤️ for Iranian carpet weavers</sub>
</div>

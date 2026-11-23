# SkyWay — Route-Synced Weather Companion

<p align="center">
  <img src="app/src/main/res/mipmap-xxhdpi/ic_launcher.webp" width="128" height="128" alt="SkyWay Logo"/>
</p>

**SkyWay** is an immersive, tactile, skeuomorphic Android road-trip and route-weather companion inspired by *Weather on the Way*. It calculates precise route corridors between any origin and destination, samples hourly checkpoints along your journey, and correlates real-time forecasts, solar glare angles, sunrise/sunset horizons, and live Doppler radar overlays to your exact pass-by hour.

---

## 📥 Download & Install

You can download the latest production APK directly from this repository:

👉 **[Download SkyWay v1.0 APK](release/skyway-v1.0.apk)** *(Android 7.0+, ARM64/Universal)*

---

## ✨ Key Features

- **Route Weather Timeline**: Search any origin and destination (or use current GPS) to view a mile-by-mile forecast breakdown timed to your estimated arrival at each waypoint.
- **Interactive Departure Time Scrubber**: Scrub forward or backward up to 72 hours with haptic detents to find the optimal weather window, avoiding storms and heavy rain.
- **Solar Glare & Sunrise/Sunset Telemetry**: Real-time celestial calculations tracking low-sun blinding glare and horizon transitions along your exact driving bearing.
- **Live RainViewer Doppler Radar**: Animate regional precipitation radar directly over your travel corridor with 1-tap playback controls.
- **Offline Map Resiliency**: Powered by osmdroid with a high-capacity 500 MB `FileSystemCache` (`TileWriter`) and automated corridor pre-caching (`CacheManager`), ensuring smooth map navigation even in remote mountain passes with zero cellular connectivity.
- **Material 3 Expressive Tactile Cockpit**: Skeuomorphic glass instruments, high-contrast dark cockpit map styling, and fluid spring motion designed for drivers.
- **Smart Update Checker**: Built-in OTA version checker that prompts users whenever a newer release is available.

---

## 🛠️ Tech Stack

- **UI**: Jetpack Compose & Material 3 Expressive motion
- **Architecture**: MVVM with Kotlin Coroutines & StateFlow
- **Persistence**: Room Database & DataStore Preferences
- **Mapping**: osmdroid with offline tile caching & Multi-Router Fallback (FOSSGIS OSRM / Geodesic corridor)
- **Networking**: Retrofit, Moshi, & OkHttp with resilient retry interceptors

---

## 🚀 Building From Source

```bash
git clone https://github.com/your-username/skyway.git
cd skyway
gradle :app:assembleDebug
```

# AirWear

Wear OS AQI app for Galaxy Watch7. Material 3 UI, GPS-first location, IQAir AirVisual data, watch-face complication.

## Setup

1. Open this folder in **Android Studio**.
2. Confirm `local.properties` has:
   - `sdk.dir=...` (your Android SDK)
   - `IQAIR_API_KEY=...` (your AirVisual key)
3. Sync Gradle.
4. Connect Watch7 (Developer options + ADB debugging / Wi-Fi debug) and Run **:wear**.

### Gradle / Java note

Project uses **Gradle 9.1** + **AGP 9.0** for Android Studio’s bundled **JBR 25**.

If sync still complains about JVM:

**Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**

Pick **jbr-25** (embedded) or any JDK **17–25**. Do not use system Java 8.

## Screens

Swipe horizontally:

| Page | Content |
|---|---|
| 1 | AQI hero (number, category, location, weather) |
| 2 | Details (dominant pollutant, weather, attribution) |
| 3 | Settings (GPS/IP, refresh interval) |

## Complication

Long-press watch face → **Customize** → **Complication** → choose **AirWear / AQI**.

Supported types: `SHORT_TEXT`, `RANGED_VALUE`, `LONG_TEXT`.

## API notes

- IQAir Community plan: **5 req/min**, **500/day**.
- Default refresh: **30 min** (WorkManager).
- Complication reads **cache only**.
- Attribution shown in Details & Settings.

## Project layout

```
wear/src/main/java/com/airwear/aqi/
  data/          IQAir client, DataStore, location, repository
  domain/        AqiSnapshot, categories
  ui/            Home, Details, Settings, Material 3 theme
  complication/  Complication data source
  work/          Periodic refresh worker
```

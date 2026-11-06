# Air Level

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

Wear Material 3 (MD3 Expressive). Opens on Home (middle page):

| Page | Gesture from Home | Content |
|---|---|---|
| World cities | swipe right | Your location + ranked world cities |
| **Home** | — | Bezel AQI gauge, category, data source + measured time, last-fetched chip (tap to refresh) |
| Details | swipe left | Summary, health tips, main pollutant, weather, data source, Settings |

Settings (from Details): location (GPS / approximate / chosen city), auto-refresh interval, about.

Design & roadmap: [design.md](design.md), [plan.md](plan.md).

## Build & run on the watch

```bash
./gradlew :wear:installRelease
```

Release is signed with the local debug key for sideloading. Judge performance on release builds only — debug builds are several times slower.

## Tile

Swipe left from the watch face → long-press a tile → **+** → **Air Level**. Shows the gauge, AQI, source and last update; tap the pill to refresh, anywhere else to open the app.

## Complication

Long-press watch face → **Customize** → **Complication** → choose **Air Level**.

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

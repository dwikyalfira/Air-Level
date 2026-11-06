# AirWear — Rencana Perbaikan

> Pendamping [`design.md`](design.md). Berdasarkan audit kode + uji langsung di Galaxy Watch7 (6 Nov 2026).
> Build debug: **BUILD SUCCESSFUL**, berjalan di watch tanpa crash.

---

## 0. Persiapan

- [ ] `git init` + commit awal (proyek belum di bawah version control). `local.properties` sudah di-`.gitignore`.
- [ ] Simpan JAVA_HOME ke JBR Android Studio (`%LOCALAPPDATA%\Programs\Android Studio\jbr`) untuk build CLI.

---

## 1. Bug & masalah teknis (temuan audit)

### P0 — salah / menyesatkan

| ID | Masalah | Lokasi | Perbaikan |
|---|---|---|---|
| B1 | **Complication tidak pernah diperbarui** — `forceUpdateAll()` adalah no-op. | `complication/AqiComplicationDataSourceService.kt:116` | `ComplicationDataSourceUpdateRequester.create(ctx, ComponentName(...)).requestUpdateAll()` |
| B2 | **Complication menampilkan AQI palsu "54"** saat belum ada data. | idem `:48` | Kembalikan `NoDataComplicationData` |
| B3 | **Interval refresh di Settings tidak berpengaruh** — worker di-hardcode 30 mnt, `KEEP` policy. | `AqiApp.kt:44`, `AqiViewModel.setRefreshMinutes` | Jadwal ulang dengan `UPDATE` policy saat setting berubah |
| B4 | **Cek cache "fresh" memakai waktu ukur stasiun** (`updatedAtEpochMs`, biasanya ±1 jam lalu) → cache tidak pernah dianggap fresh, tiap buka app = request API. | `AqiRepository.kt:58-61`, `AqiDataStore` | Persist `sourceFetchedAtMs`, pakai untuk TTL |
| B5 | **Error & status refresh tidak pernah tampil** bila ada cache (`Ready(fromCache=true)` selalu). | `AqiViewModel.kt:55-67` | State gabungan: `snapshot`, `isRefreshing`, `softError`, `isStale` |
| B6 | **Ranking menampilkan kota basi** (10 kota dari cache lama, watch list cuma 5) dan "Updated just now" palsu. | `AqiRepository.loadCityRank`, `AqiViewModel.kt:133` | Filter cache ke watch list; `updatedAt` = `city_rank_at` sebenarnya |
| B7 | Ganti mode lokasi → `refresh(force)` tapi repository tetap pakai cache lama untuk mode non-MANUAL (TTL), dan tiap tap siklus Settings memicu fetch. | `AqiViewModel.setLocationMode`, Settings | Invalidasi cache saat mode berubah; UI radio (tanpa siklus) |
| B8 | Snapshot hanya di-*merge* (`?.let`) → field lama (state/temp) bocor ke lokasi baru bila respons baru null. | `AqiDataStore.saveSnapshot` | Hapus key yang null (`prefs.remove`) |

### P1 — robustness

| ID | Masalah | Perbaikan |
|---|---|---|
| B17 ✅ | **Complication tidak pernah muncul di picker watch face**: manifest memakai action `...ACTION_COMPLICATION_DATA_SOURCE` dan permission `android.permission.BIND_COMPLICATION_DATA_SOURCE` yang tidak ada. Diganti ke `ACTION_COMPLICATION_UPDATE_REQUEST` + `com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER` (diverifikasi dari konstanta library dan `cmd package query-services` di watch). | `AndroidManifest.xml` | Selesai 6 Nov 2026 |
| B18 ✅ | Refresh di background (worker/tile) mode GPS jatuh ke lokasi IP → kota berbeda (Tabing → Lubukbergalung). | `AqiRepository` | Simpan fix GPS terakhir dari foreground, pakai ≤ 24 jam |
| B9 | Pager kembali ke Home setelah config change (teramati). | `rememberSaveable` posisi halaman / tangani recreate |
| B10 | Loop ranking (5 × 14 s) jalan di `viewModelScope` walau user sudah pindah halaman; `rankStarted` tak pernah reset → tidak bisa refresh ranking. | Pindah ke WorkManager one-time / cancel saat tak terlihat; tombol refresh |
| B11 | `hasLocationPermission()` dihitung sekali di komposisi, `vmRef` disimpan di Activity. | Flow izin di VM; `rememberLauncherForActivityResult` di Compose |
| B12 | Izin ditolak → tidak ada feedback; mode tetap GPS tapi diam-diam pakai IP. | Tampilkan status + fallback eksplisit |
| B13 | Kuota IQAir (5/min, 500/hari) tidak dilindungi secara global. | Rate limiter sederhana di repository (token bucket + counter harian di DataStore) |
| B14 | `RANGED_VALUE` max 500 vs ring app 300. Tipe `MONOCHROMATIC_IMAGE`/`SMALL_IMAGE` dideklarasikan di XML tapi tidak ditangani; file `complication_data_source.xml` tidak dirujuk manifest. | Seragamkan 0–300; hapus XML yatim atau dukung tipe image |
| B15 | `loadCityRank` baris 170: kondisi skip tidak pernah benar (kode mati). | Hapus |

### P2 — build & kebersihan

| ID | Masalah | Perbaikan |
|---|---|---|
| T1 | Versi plugin Kotlin tak seragam (compose 2.2.10, serialization 2.0.21); plugin serialization tidak dipakai (Moshi). | Hapus plugin serialization; pindah ke `gradle/libs.versions.toml` |
| T2 | `compose-bom:2024.12.01` (lama) dicampur Wear Compose 1.6.2. | Naikkan BOM sesuai Wear Compose |
| T3 | `ui-tooling` di `implementation` (masuk APK release). | `debugImplementation`; tambahkan `@Preview` untuk Wear |
| T4 | Moshi reflection (`KotlinJsonAdapterFactory`) + minify → rawan & berat. | Moshi codegen (KSP) atau kotlinx.serialization |
| T5 | `compileSdk/targetSdk 35`, device API 36. | Naikkan ke 36 |
| T6 | API key IQAir tertanam di `BuildConfig` (bisa diekstrak dari APK). | Oke untuk pemakaian pribadi; jika dirilis → proxy kecil |
| T7 | `allowBackup="true"` membackup cache. | Batasi via `dataExtractionRules` |
| T8 | Tidak ada test. | Unit test `AqiCategory`, mapper, TTL/rate limiter; screenshot test Compose |
| T9 | Kode tak terpakai: `AqiScaleRing`, `AqiProgressRing`, `IconLabel`, `CardPadding`, `currentAccent()`, `cycleLocationMode()`, `isStale`, `SurfaceDark`, `OnSurfaceLight`. | Pakai (ring) atau hapus |

---

## 2. Roadmap

### Fase 1 — Fondasi & bug kritis ✅ selesai 6 Nov 2026

Dikerjakan: T1–T5 (version catalog, SDK 36, BOM 2025.08.00, Moshi → kotlinx.serialization, tooling hanya debug), B1–B8, B13–B15, plus **B16** (baru ditemukan): `WorkManager.initialize()` manual melempar `IllegalStateException` sehingga refresh periodik **tidak pernah terjadwal**. 16 unit test (`wear/src/test`).
Terverifikasi di watch: relaunch dalam 10 mnt = 0 request; job WorkManager terdaftar dan ikut setting (60 → 15 mnt menjadwal ulang); Ranking hanya 5 kota watch list.
Belum terverifikasi: worker periodik berjalan sendiri (WorkManager menolak force-run sebelum jadwal); tampilan complication di watch face.

<details><summary>Rencana awal Fase 1</summary>


1. git init, version catalog, bump SDK/BOM, hapus plugin tak terpakai (T1–T5).
2. Perbaiki data layer: B4, B5, B7, B8, B13 + unit test.
3. Complication: B1, B2, B14.
4. WorkManager mengikuti setting (B3).

**Selesai bila:** complication berubah setelah worker jalan; buka app 2× dalam 5 menit = 1 request (cek logcat OkHttp); Settings refresh mengubah jadwal (`adb shell dumpsys jobscheduler`).

</details>

### Fase 2 — UI/UX redesign MD3 ✅ selesai 6 Nov 2026

Dirombak total dengan Wear Compose Material 3 (lihat design.md §9): `AppScaffold` + `SwipeDismissableNavHost`, `HorizontalPagerScaffold` (Home · Detail · Kota) dengan page indicator, `TransformingLazyColumn` + morph, `RadioButton` di Settings, tema tonal per kategori AQI, `strings.xml` + `values-in`, B9 (`configChanges`).
Performa: release build R8 bisa di-sideload (`installRelease`, ditandatangani debug key); **perbaikan crash release** (R8 membuang konstruktor `WorkDatabase_Impl`) dan pin Fragment 1.8.5 (lint). Hasil ukur di design.md §9.6.
Belum: baseline profile (Fase 4), pesan error repository masih bahasa Inggris, B10–B12.

<details><summary>Rencana awal Fase 2</summary>


1. `AppScaffold` + `ScreenScaffold` + `HorizontalPageIndicator`; urutan pager **Home → Detail → Kota**; Settings ke `SwipeDismissableNavHost` (B9).
2. Home baru: ring skala, angka besar, metadata satu baris, `EdgeButton` Refresh, state stale/offline (design.md §5.1).
3. Detail baru: `TransformingLazyColumn`, kartu non-klik, saran kesehatan, cuaca + ikon (§5.2).
4. Settings baru: radio button, pemilih kota terpisah, info "Tentang" (§5.4).
5. Token warna/tipografi tunggal (§6), hapus emoji, rotary di semua layar.
6. Lokalisasi `strings.xml` + `values-in/`.

**Selesai bila:** angka, kategori, dan umur data terlihat tanpa scroll di 432×432; tidak ada teks bertabrakan pada font scale 1.3×; swipe kanan dari Home menutup app.

</details>

### Fase 3 — Fitur baru (±3–5 hari)

| Fitur | Nilai | Catatan teknis |
|---|---|---|
| **Tile AQI** ✅ | Glance tanpa buka app | Selesai 6 Nov 2026 — `AqiTileService` (tiles 1.5 / protolayout 1.3), refresh dari tile, lihat design.md §9.4c |
| **Konsentrasi polutan & prakiraan 24 jam** | Mengisi section Polutan yang sekarang kosong | IQAir Community tidak menyediakan konsentrasi/forecast → tambah **Open-Meteo Air Quality API** (gratis, tanpa key, hourly PM2.5/PM10/O₃/NO₂/SO₂/CO + `us_aqi`) sebagai sumber sekunder per koordinat |
| **Riwayat 24 jam** | Tren naik/turun | Simpan ring buffer pembacaan di DataStore/Room; sparkline di Detail |
| **Notifikasi ambang AQI** | Peringatan proaktif | Worker membandingkan dengan ambang; channel notifikasi; izin `POST_NOTIFICATIONS`; histeresis agar tidak spam |
| **Kota favorit** (ganti ranking global) | Lebih relevan | Pencarian via `v2/states` & `v2/cities` IQAir (di-cache), maks 5 kota, refresh bergiliran sesuai kuota |
| **Saran kesehatan kontekstual** | Actionable | Mode "kelompok sensitif" di Settings |

### Fase 4 — Polish (±1 hari)
- Animasi angka & ring, haptic saat refresh selesai.
- Ikon launcher adaptif + monochrome.
- **Baseline profile** (modul `:baselineprofile` + Macrobenchmark): uji di watch menunjukkan AOT menurunkan jank scroll 1.9 % → 0.5 % dan p99 swipe pager 69 → 29 ms. Juga mempercepat cold start (±2,2 s).
- Uji: ambient / layar mati, tanpa internet, izin ditolak, kuota habis (429), font besar.

---

## 3. Cara uji di watch

```bash
adb connect 192.168.1.6:38267
```

```bash
./gradlew :wear:installRelease
```

Gunakan **release** untuk menilai performa — debug build jauh lebih lambat (jank 23.7 % vs 3.2 %).

```bash
adb shell am start -n com.airwear.aqi/.MainActivity
```

Port wireless debugging berubah setiap kali diaktifkan ulang; cari dengan `adb mdns services`.

---

## 4. Keputusan yang perlu Anda ambil

1. **Sumber data sekunder Open-Meteo** untuk polutan & prakiraan — setuju? (alternatif: upgrade paket IQAir berbayar)
2. **Ranking global** dihapus dan diganti **kota favorit**, atau dipertahankan sebagai halaman tambahan?
3. **Bahasa default**: ikuti locale jam (disarankan), atau paksa Bahasa Indonesia?
4. Urutan eksekusi: mulai **Fase 1** dulu (disarankan), atau langsung redesign UI (Fase 2)?

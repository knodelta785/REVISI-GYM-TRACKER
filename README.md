# Ongarand Gym Tracker

Aplikasi Android (Kotlin + Jetpack Compose) untuk melacak latihan gym: progressive overload,
berat badan & ukuran tubuh, jenis latihan, serta video tutorial offline. Semua data tersimpan lokal
(Room/SQLite), tanpa akun dan tanpa internet.

## Cara menjalankan
1. Install **Android Studio** (Ladybug 2024.2 atau lebih baru).
2. `File → Open` → pilih folder `OngarandGymTracker`. Tunggu Gradle sync selesai
   (Android Studio otomatis membuat Gradle wrapper & mengunduh dependensi, butuh internet sekali).
3. Colok HP (USB debugging aktif) atau buat emulator (API 26+), lalu tekan **Run ▶**.
4. Untuk APK: `Build → Build Bundle(s) / APK(s) → Build APK(s)`.

minSdk 26 (Android 8.0), targetSdk 35.

## Fitur
- **Beranda**: target mingguan (ring), streak hari & minggu, ringkasan mingguan, deteksi plateau, lencana, kutipan harian.
- **Latihan**: logger set × rep × beban cepat, terisi otomatis dari saran overload, timer istirahat, penanda PR, ringkasan sesi.
- **Pustaka**: 29 latihan bawaan + latihan sendiri, program (Push/Pull/Legs, dll.) dengan jadwal hari, **impor video offline**
  (satu per latihan atau banyak sekaligus), pemutar dengan kecepatan 0.5×–1.5× dan loop.
- **Progres**: grafik estimasi 1RM & volume per latihan, volume mingguan, set per kelompok otot.
- **Tubuh**: berat harian + rata-rata 7 hari, ukuran tubuh, tren vs tujuan (cut/maintain/bulk), target kalori & protein.
- **Pengaturan**: pengingat harian, profil, ekspor CSV.

## Catatan
- Revisi: file duplikat dari dua versi kode yang tercampur sudah dibersihkan, dan Gradle wrapper (`gradlew`) ditambahkan.
  Proyek belum pernah dikompilasi di lingkungan pembuatnya (tidak ada Android SDK); bila build di GitHub gagal,
  kirimkan log error-nya agar bisa diperbaiki.
- Belum ada: foto progres, backup/restore database penuh, picture-in-picture video.
- Target kalori memakai rumus Mifflin-St Jeor; hanya perkiraan, bukan saran medis.

## Build otomatis di GitHub
Pastikan **isi folder ini** (bukan folder pembungkusnya) menjadi root repositori, supaya `.github/` dan `gradlew` ada di root.
Setiap push ke branch `main`/`master` menjalankan workflow **Build APK** (`.github/workflows/build.yml`).
Setelah selesai, buka tab **Actions → run terbaru → Artifacts → OngarandGymTracker-debug-apk**
untuk mengunduh APK dan memasangnya di HP. Bisa juga dijalankan manual lewat **Run workflow**.

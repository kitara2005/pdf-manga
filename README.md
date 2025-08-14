## Reading PDF (Android)

Ứng dụng Android đọc PDF với hiệu ứng lật trang như đọc sách, hỗ trợ pinch-to-zoom, nhớ zoom theo từng trang, chọn PDF từ hệ thống, cache bitmap hiệu năng cao, chuyển trang nhanh, “Tới trang…”, và lưu cấu hình chất lượng render/giới hạn bitmap.

### Tính năng
- Lật trang như sách (ViewPager2 + PageTransformer)
- Mở PDF qua hệ thống (ACTION_OPEN_DOCUMENT), nhớ lại tài liệu gần nhất
- Pinch-to-zoom, double-tap zoom, pan; nhớ zoom theo từng trang và từng tài liệu
- Cache bitmap bằng LruCache, preload trang lân cận
- Nút Next/Prev, “Tới trang…”, đổi chất lượng render (1.0x/1.5x/2.0x), giới hạn kích thước bitmap
- Tùy chọn “Quên tài liệu gần nhất”, “Reset cấu hình”

### Yêu cầu
- JDK 17 (Temurin 17 / Microsoft Build of OpenJDK 17)
- Android SDK (compileSdk 34)
- Gradle 8.7+ (nếu cần tạo Gradle Wrapper)
- Android Studio (khuyến nghị)

Đường dẫn SDK mặc định:
- macOS: `~/Library/Android/sdk`
- Windows: `%LOCALAPPDATA%\\Android\\Sdk`

---

## macOS

1) Cài đặt
- Cài JDK 17 và Android Studio
- Android Studio → SDK Manager → cài Android SDK Platform (API 34), Build-Tools, Platform-Tools

2) Mở dự án
```bash
cd "/Users/long-nguyen/Documents/learning/reading-pdf"
```
- Android Studio → File → Open → chọn thư mục dự án → Sync Gradle

3) (Tuỳ chọn) Tạo Gradle Wrapper nếu chưa có
```bash
brew install gradle # nếu cần gradle CLI
gradle wrapper --gradle-version 8.7
```

4) Chỉ định đường dẫn SDK (nếu cần)
```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
```

5) Chấp nhận license (nếu build CLI lần đầu)
```bash
yes | "$ANDROID_SDK_ROOT/tools/bin/sdkmanager" --licenses || true
```
Lưu ý: nếu dùng command-line tools mới, `sdkmanager` nằm ở `cmdline-tools/latest/bin`.

6) Build APK Debug
```bash
./gradlew assembleDebug
```
- APK: `app/build/outputs/apk/debug/app-debug.apk`

7) Build APK Release (ký phát hành)
- Tạo keystore:
```bash
keytool -genkeypair -v -keystore release.keystore -alias readingpdf \
 -keyalg RSA -keysize 2048 -validity 3650
mv release.keystore "/Users/long-nguyen/Documents/learning/reading-pdf/"
```
- Thêm vào `gradle.properties` (project root):
```properties
RELEASE_STORE_FILE=release.keystore
RELEASE_STORE_PASSWORD=your-store-password
RELEASE_KEY_ALIAS=readingpdf
RELEASE_KEY_PASSWORD=your-key-password
```
- Cập nhật `app/build.gradle` (trong `android { }`):
```gradle
signingConfigs {
    release {
        storeFile file(RELEASE_STORE_FILE)
        storePassword RELEASE_STORE_PASSWORD
        keyAlias RELEASE_KEY_ALIAS
        keyPassword RELEASE_KEY_PASSWORD
    }
}
buildTypes {
    release {
        minifyEnabled false
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        signingConfig signingConfigs.release
    }
}
```
- Build release:
```bash
./gradlew assembleRelease
```
- APK: `app/build/outputs/apk/release/app-release.apk`

8) (Tuỳ chọn) Build App Bundle
```bash
./gradlew bundleRelease
```
- AAB: `app/build/outputs/bundle/release/app-release.aab`

9) Cài APK lên thiết bị/emulator
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Windows

1) Cài đặt
- Cài JDK 17 và Android Studio
- Android Studio → SDK Manager → cài Android SDK Platform (API 34), Build-Tools, Platform-Tools

2) Mở dự án
- Android Studio → File → Open → chọn thư mục dự án → Sync Gradle

3) (Tuỳ chọn) Tạo Gradle Wrapper nếu chưa có
```bat
gradle wrapper --gradle-version 8.7
```

4) Chỉ định đường dẫn SDK (nếu cần)
Tạo file `local.properties` ở project root:
```properties
sdk.dir=%LOCALAPPDATA%\\Android\\Sdk
```
Hoặc đặt biến môi trường trước khi build:
```powershell
$env:ANDROID_SDK_ROOT="$env:LOCALAPPDATA\\Android\\Sdk"
```

5) Chấp nhận license (nếu build CLI lần đầu)
```bat
"%ANDROID_SDK_ROOT%\\cmdline-tools\\latest\\bin\\sdkmanager.bat" --licenses
```
Xác nhận Yes cho các license.

6) Build APK Debug
```bat
.\\u0067radlew.bat assembleDebug
```
- APK: `app\\build\\outputs\\apk\\debug\\app-debug.apk`

7) Build APK Release (ký phát hành)
- Tạo keystore:
```bat
keytool -genkeypair -v -keystore release.keystore -alias readingpdf -keyalg RSA -keysize 2048 -validity 3650
```
Di chuyển `release.keystore` vào thư mục dự án.

- Thêm vào `gradle.properties` (project root):
```properties
RELEASE_STORE_FILE=release.keystore
RELEASE_STORE_PASSWORD=your-store-password
RELEASE_KEY_ALIAS=readingpdf
RELEASE_KEY_PASSWORD=your-key-password
```
- Cập nhật `app/build.gradle` (trong `android { }`) với `signingConfigs` và `buildTypes` như phần macOS.

- Build release:
```bat
.\\u0067radlew.bat assembleRelease
```
- APK: `app\\build\\outputs\\apk\\release\\app-release.apk`

8) (Tuỳ chọn) Build App Bundle
```bat
.\\u0067radlew.bat bundleRelease
```
- AAB: `app\\build\\outputs\\bundle\\release\\app-release.aab`

9) Cài APK lên thiết bị/emulator
```bat
adb install -r app\\build\\outputs\\apk\\debug\\app-debug.apk
```

---

### Gỡ lỗi nhanh
- Thiếu compileSdk/Build-Tools: Mở Android Studio → SDK Manager → cài đúng phiên bản (API 34)
- Lỗi JDK: đảm bảo đang dùng JDK 17
- Thiếu Gradle Wrapper: tạo bằng `gradle wrapper --gradle-version 8.7`
- Không tìm thấy SDK: tạo `local.properties` trỏ tới folder SDK đúng
- License SDK: chấp nhận bằng `sdkmanager --licenses`

### Chạy nhanh trong Android Studio
- File → Open → chọn thư mục dự án → Sync Gradle → Run trên thiết bị/emulator

### Đầu ra build
- APK Debug: `app/build/outputs/apk/debug/app-debug.apk`
- APK Release: `app/build/outputs/apk/release/app-release.apk`
- AAB Release: `app/build/outputs/bundle/release/app-release.aab`



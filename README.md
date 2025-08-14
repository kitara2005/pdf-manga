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


---

## Cấu hình kết nối Google Drive (danh sách và folder)

Ứng dụng hỗ trợ 2 cách nạp danh sách PDF để đọc tuần tự:
- Từ một file văn bản (mỗi dòng là 1 URL PDF) đặt trên Drive hoặc bất kỳ URL công khai
- Từ một folder Google Drive (liệt kê toàn bộ file PDF bên trong, có phân trang)

### 1) Chuẩn bị trên Google Cloud (cho chế độ duyệt folder)
1. Mở Google Cloud Console → tạo Project mới (hoặc chọn project có sẵn)
2. Trong “APIs & Services” → “Library” → bật “Google Drive API” cho project
3. Vào “APIs & Services” → “Credentials” → “Create credentials” → chọn “API key”
4. Sao chép API key vừa tạo
   - Khuyến nghị: “Restrict key” để giới hạn chỉ dùng với Google Drive API

### 2) Chia sẻ quyền thư mục/tệp trên Drive
- Với folder: bấm Share → đặt “Anyone with the link” ở quyền Viewer (xem)
- Với file danh sách (text) hoặc file PDF: cũng cần quyền “Anyone with the link - Viewer” để ứng dụng tải được

### 3) Thiết lập trực tiếp trong ứng dụng
- Mở ứng dụng → menu (ba chấm trên thanh trên cùng):
  - “Thiết lập Drive API key”: dán API key vừa tạo ở bước (1)
  - “Thiết lập link folder Drive”: dán link dạng `https://drive.google.com/drive/folders/<FOLDER_ID>`
  - “Tải danh sách từ folder Drive”: ứng dụng sẽ gọi Drive API để liệt kê các file PDF công khai trong folder và lưu thành danh sách đọc
  - “Mở truyện kế tiếp”: tải về và mở lần lượt từng PDF từ danh sách đã lưu

### 4) Thiết lập danh sách bằng file văn bản (tuỳ chọn, không cần API key)
- Tạo 1 file `.txt` trên Drive, mỗi dòng chứa 1 URL PDF (có thể là link Drive dạng file hoặc URL trực tiếp)
- Chia sẻ file ở chế độ công khai như trên
- Trong ứng dụng:
  - “Thiết lập link danh sách”: dán link file `.txt` công khai (hoặc URL bất kỳ trả về text)
  - “Tải danh sách”: đọc nội dung text và lưu thành danh sách URL PDF
  - “Mở truyện kế tiếp”: tải và mở PDF theo thứ tự

### 5) Ghi chú
- Ứng dụng chỉ liệt kê/tải được nội dung công khai; nếu folder/file riêng tư, hãy cấp quyền hoặc chuyển sang “Anyone with the link – Viewer”
- Duyệt folder sử dụng Google Drive API v3; lưu ý hạn ngạch (quota) và rate limit của API key
- Trình phân tích JSON trong mã được tối giản; nếu bạn muốn độ tin cậy cao hơn, có thể thay bằng thư viện JSON như Gson/Moshi
- Link Drive dạng file được chuyển thành URL tải trực tiếp: `https://drive.google.com/uc?export=download&id=<FILE_ID>`

---

## Cấu hình OAuth (Google Sign‑In) để duyệt folder Drive (không cần API key)

Ứng dụng hỗ trợ đăng nhập Google và gọi Drive API bằng access token người dùng (scope chỉ đọc). Bạn cần cấu hình OAuth trên Google Cloud và cung cấp SHA‑1 cho Android Client.

### 1) Bật OAuth consent screen và Drive API
- Vào Google Cloud Console → chọn Project
- “APIs & Services” → “OAuth consent screen”:
  - Chọn User Type (External cho thử nghiệm), điền thông tin bắt buộc
  - Thêm scope: `.../auth/drive.readonly`
  - Click “Publish app” (nếu là External) hoặc thêm tester vào danh sách
- “APIs & Services” → “Library” → bật “Google Drive API”

### 2) Tạo OAuth Client ID cho Android
- “APIs & Services” → “Credentials” → “Create credentials” → “OAuth client ID” → “Android”
- Package name: `com.example.readingpdf`
- SHA‑1:
  - Debug (Android Studio): View → Tool Windows → Gradle → <project> → Tasks → android → `signingReport`
    - Hoặc lệnh:
      ```bash
      keytool -list -v \
        -alias androiddebugkey \
        -keystore "$HOME/.android/debug.keystore" \
        -storepass android -keypass android
      ```
  - Release: dùng keystore phát hành của bạn
    ```bash
    keytool -list -v -alias <RELEASE_ALIAS> -keystore <PATH_TO_RELEASE_KEYSTORE>
    ```
- Tạo xong, lưu Client ID/Client name để tham khảo

### 3) (Tuỳ chọn) Tạo OAuth Client ID loại “Web application”
- Nếu muốn yêu cầu `idToken/serverAuthCode` (phục vụ các luồng nâng cao), tạo thêm Client ID loại Web và lấy `Client ID`
- Thêm vào `app/src/main/res/values/strings.xml` khóa sau nếu cần:
  ```xml
  <string name="default_web_client_id">YOUR_WEB_CLIENT_ID.apps.googleusercontent.com</string>
  ```

### 4) Cấu hình trong ứng dụng
- Mở app → Menu:
  - “Đăng nhập Google”: đăng nhập và cấp quyền Drive Readonly
  - “Thiết lập link folder Drive”: dán link dạng `https://drive.google.com/drive/folders/<FOLDER_ID>`
  - “Tải danh sách từ folder Drive”: lấy access token (Bearer) và gọi Drive API để liệt kê file PDF trong folder
  - “Mở truyện kế tiếp”: tải PDF và mở theo thứ tự

### 5) Ghi chú bảo mật và hạn chế
- Access token là ngắn hạn; app sẽ xin lại khi cần
- Chỉ yêu cầu scope Drive Readonly; bạn có thể thu hẹp thêm nếu cần
- Ở chế độ External và chưa publish, chỉ tester được chỉ định mới đăng nhập được
- Khi phát hành, thay `packageName`/SHA‑1 theo bản release của bạn



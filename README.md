# Lucky Tide — App Android (APK)

App Android bọc trang người chơi (lobby + game) trong một cửa sổ toàn màn hình, nằm ngang, giữ màn hình luôn sáng.
Lần đầu mở app sẽ hỏi **địa chỉ server**:
- link VPS thật, ví dụ `https://game.tenmien.com`
- hoặc link Cloudflare của VPS giả lập, ví dụ `https://xxxx.trycloudflare.com`
- hoặc trong cùng Wi-Fi: `http://192.168.1.212:3000`

Đổi server: ở lobby bấm nút **Back** → **Change server** (app cũng tự hỏi lại khi không kết nối được).
Vì app chỉ là "khung" mở web, **cập nhật game / lobby trên server là app tự có bản mới**, không cần build lại APK.

## Cách 1 — Build trên GitHub (không cần cài gì)
1. Tạo một repo GitHub mới (để Private cũng được), tải toàn bộ thư mục này lên.
2. Vào tab **Actions** → workflow **Build APK** tự chạy (khoảng 3–5 phút).
   Muốn điền sẵn địa chỉ server: bấm **Run workflow** và nhập `server_url`.
3. Mở lần chạy vừa xong → mục **Artifacts** → tải **LuckyTide-apk** → giải nén ra file `app-release.apk`.

## Cách 2 — Android Studio
1. Cài Android Studio → **Open** thư mục này → đợi Gradle tải xong.
2. Menu **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
3. File APK nằm ở `app/build/outputs/apk/`.

## Cài lên điện thoại
Chép file `.apk` vào điện thoại → mở file → cho phép "Cài ứng dụng không rõ nguồn gốc" → Cài đặt.

## Ký APK bằng khóa riêng (khi phát hành chính thức)
Mặc định APK được ký bằng khóa debug — cài thử thoải mái, nhưng mỗi máy build sẽ ra khóa khác nhau
(muốn cập nhật đè lên bản cũ thì phải cùng khóa). Tạo khóa riêng một lần:
```bash
keytool -genkeypair -v -keystore luckytide.jks -alias luckytide -keyalg RSA -keysize 2048 -validity 10000
```
rồi build với biến môi trường `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
**Giữ file .jks và mật khẩu cẩn thận**, mất là không phát hành bản cập nhật được nữa.

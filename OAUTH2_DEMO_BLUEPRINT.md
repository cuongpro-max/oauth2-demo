# 📘 TÀI LIỆU TỔNG HỢP TOÀN DIỆN DỰ ÁN OAUTH2-DEMO (KEYCLOAK)
> **Dự án:** `oauth2-demo` | **Package chính:** `com.keycloak.oauth2_demo` | **Build tool:** Gradle (`build.gradle`)

---

## 📑 MỤC LỤC
1. [Cấu Trúc Thư Mục Thực Tế Dự Án `oauth2-demo`](#1-cấu-trúc-thư-mục-thực-tế-dự-án-oauth2-demo)
2. [Bảng Tổng Hợp Toàn Bộ Công Cụ & Thư Viện Đã Sử Dụng](#2-bảng-tổng-hợp-toàn-bộ-công-cụ--thư-viện-đã-sử-dụng)
3. [Chi Tiết Từng Tính Năng & Cách Thức Hoạt Động](#3-chi-tiết-từng-tính-năng--cách-thức-hoạt-động)
   - 3.1 [Xác Thực Tập Trung Qua Keycloak (OAuth2 / OIDC SSO)](#31-xác-thực-tập-trung-qua-keycloak-oauth2--oidc-sso)
   - 3.2 [Bộ Lọc HTTP Logging & Mặt Nạ Dữ Liệu Nhạy Cảm (SensitiveMasker)](#32-bộ-lọc-http-logging--mặt-nạ-dữ-liệu-nhạy-cảm-sensitivemasker)
   - 3.3 [Cảnh Báo Tức Thời Qua Telegram Bot (@BotFather)](#33-cảnh-báo-tức-thời-qua-telegram-bot-botfather)
   - 3.4 [Đa Ngôn Ngữ i18n (Internationalization)](#34-đa-ngôn-ngữ-i18n-internationalization)
   - 3.5 [Xử Lý Ngoại Lệ Tập Trung (GlobalExceptionHandler)](#35-xử-lý-ngoại-lệ-tập-trung-globalexceptionhandler)
   - 3.6 [Giao Diện Người Dùng Thymeleaf SSR](#36-giao-diện-người-dùng-thymeleaf-ssr)
   - 3.7 [Tối Ưu Mã Nguồn Với Lombok](#37-tối-ưu-mã-nguồn-với-lombok)
4. [Bảng Cheat-Sheet Các Lưu Ý & Lỗi Cần Tránh Khi Vận Hành](#4-bảng-cheat-sheet-các-lưu-ý--lỗi-cần-tránh-khi-vận-hành)

---

# 1. CẤU TRÚC THƯ MỤC THỰC TẾ DỰ ÁN `oauth2-demo`

```text
oauth2-demo/
│
├── 📁 .gradle/ , 📁 gradle/            # Gradle Wrapper & cache
├── 📄 build.gradle                    # Quản lý Dependencies & Plugins bằng Gradle
├── 📄 settings.gradle                 # Cấu hình tên project
├── 📄 .env                            # [BẢO MẬT] Biến môi trường (Chứa Keycloak Secret, Telegram Token...)
├── 📄 .gitignore                      # Loại bỏ .env, build/, .gradle, .idea khỏi Git
│
└── 📁 src/
    └── 📁 main/
        ├── 📁 java/com/keycloak/oauth2_demo/
        │   │
        │   ├── 📁 config/                       # TẦNG CẤU HÌNH HỆ THỐNG
        │   │   ├── AppConfig.java               # Khởi tạo các Bean dùng chung (RestTemplate, WebClient...)
        │   │   ├── I18nConfig.java              # Cấu hình đa ngôn ngữ (LocaleResolver, LocaleChangeInterceptor)
        │   │   ├── GlobalExceptionHandler.java  # Bắt và xử lý toàn bộ Exception trong dự án
        │   │   │
        │   │   ├── 📁 logging/                  # GIÁM SÁT HTTP & BẢO VỆ DỮ LIỆU
        │   │   │   ├── HttpLoggingFilter.java   # Filter bắt mọi HTTP Request/Response & đo thời gian (ms)
        │   │   │   └── SensitiveMasker.java     # Tìm và che giấu password, token thành ****** trong Log
        │   │   │
        │   │   └── 📁 telegram/                 # THÔNG BÁO TỨC THỜI QUA TELEGRAM
        │   │       └── TelegramNotifier.java    # Gửi tin nhắn cảnh báo lỗi 500 / đăng nhập bằng @Async
        │   │
        │   ├── 📁 user/                         # MODULE QUẢN LÝ USER & AUTHENTICATION
        │   │   ├── 📁 controller/
        │   │   │   ├── AuthController.java      # Điều hướng Login, Register, Logout
        │   │   │   └── UserController.java      # Hiển thị thông tin Profile người dùng sau khi Login
        │   │   ├── 📁 dto/                      # DATA TRANSFER OBJECTS
        │   │   │   ├── LoginDto.java            # Nhận username/password đăng nhập
        │   │   │   ├── RegisterDto.java         # Nhận thông tin đăng ký tài khoản mới
        │   │   │   └── AuthResponseDto.java     # Trả về kết quả token / message
        │   │   └── 📁 service/
        │   │       └── UserService.java         # Xử lý logic người dùng & giao tiếp Keycloak
        │   │
        │   └── Oauth2DemoApplication.java       # File chạy chính (@SpringBootApplication)
        │
        └── 📁 resources/
            ├── application.properties           # Cấu hình cổng port, issuer-uri Keycloak, database...
            ├── messages.properties              # File dịch mặc định (Tiếng Anh)
            ├── messages_vi.properties           # File dịch Tiếng Việt
            ├── messages_en.properties           # File dịch Tiếng Anh
            │
            ├── 📁 templates/                    # GIAO DIỆN HTML (Thymeleaf SSR)
            │   ├── login.html                   # Giao diện trang Đăng nhập
            │   ├── register.html                # Giao diện trang Đăng ký
            │   └── access-denied.html           # Trang báo lỗi 403 khi không đủ quyền
            │
            └── 📁 static/                       # Chứa CSS, JavaScript, Logo, hình ảnh
```

---

# 2. BẢNG TỔNG HỢP TOÀN BỘ CÔNG CỤ & THƯ VIỆN ĐÃ SỬ DỤNG

| Nhóm chức năng | Công nghệ / Thư viện | Vai trò cụ thể trong dự án `oauth2-demo` |
| :--- | :--- | :--- |
| **Xác thực & Phân quyền** | **Keycloak Server** | Máy chủ IAM độc lập xử lý xác thực, cấp phát Token chuẩn OAuth2/OIDC, quản lý User tập trung. |
| | **Spring Security OAuth2 Client** | Tự động kết nối, redirect người dùng sang Keycloak và nhận diện `OidcUser` / `Jwt`. |
| **Giám sát & Logging** | **`HttpLoggingFilter`** | Ghi nhận chi tiết mọi Request/Response (URL, Method, IP, Payload, Execution Time ms). |
| | **`SensitiveMasker`** | Tự động lọc và che giấu các trường nhạy cảm (`password`, `token`, `secret`) thành `******`. |
| | **SLF4J + Logback (Lombok `@Slf4j`)** | Động cơ ghi log ra Console / File log của hệ thống. |
| **Cảnh báo tức thời** | **Telegram Bot (`@BotFather`)** | Gửi tin nhắn thông báo đăng nhập hoặc cảnh báo lỗi hệ thống 500 về điện thoại qua `TelegramNotifier` (`@Async`). |
| **Đa ngôn ngữ** | **Spring i18n (`I18nConfig`)** | Đọc các tệp `messages_*.properties` để chuyển đổi Tiếng Việt / Tiếng Anh linh hoạt qua `?lang=vi`. |
| **Xử lý lỗi tập trung** | **`GlobalExceptionHandler`** | Bắt toàn bộ lỗi `AccessDeniedException`, `401`, `500` và chuyển hướng sang trang lỗi hoặc bắn tin nhắn Telegram. |
| **Giao diện Web** | **Thymeleaf SSR** | Render giao diện HTML trực tiếp từ Server (`login.html`, `register.html`, `access-denied.html`). |
| **Tối ưu code** | **Lombok** | Tự động sinh Getter, Setter, Builder, Constructor, Logger để làm sạch code DTO & Service. |
| **Build & Dependencies** | **Gradle (`build.gradle`)** | Quản lý vòng đời build và thư viện của dự án. |

---

# 3. CHI TIẾT TỪNG TÍNH NĂNG & CÁCH THỨC HOẠT ĐỘNG

### 3.1. Xác Thực Tập Trung Qua Keycloak (OAuth2 / OIDC SSO)
- **Cơ chế:** Khi người dùng truy cập trang được bảo vệ, Spring Security sẽ chuyển hướng người dùng sang trang đăng nhập của **Keycloak Server**.
- **Sau khi đăng nhập thành công:** Keycloak cấp Authorization Code, Spring Boot tự động đổi lấy Token và gán thông tin vào `SecurityContextHolder`.
- **Trong Controller:** Lấy trực tiếp thông tin người dùng đã đăng nhập:
  ```java
  @GetMapping("/user/profile")
  public String getProfile(@AuthenticationPrincipal OidcUser principal, Model model) {
      model.addAttribute("username", principal.getPreferredUsername());
      model.addAttribute("email", principal.getEmail());
      model.addAttribute("roles", principal.getAuthorities());
      return "profile";
  }
  ```

---

### 3.2. Bộ Lọc HTTP Logging & Mặt Nạ Dữ Liệu Nhạy Cảm (SensitiveMasker)
- **`HttpLoggingFilter`:**
  - Bọc `HttpServletRequest` vào `ContentCachingRequestWrapper` và `HttpServletResponse` vào `ContentCachingResponseWrapper` để tránh lỗi đóng Stream khi đọc Body.
  - Đo thời gian xử lý: `long duration = System.currentTimeMillis() - startTime;`
  - Đảm bảo luôn gọi `responseWrapper.copyBodyToResponse()` để client nhận được dữ liệu.
- **`SensitiveMasker`:**
  - Dùng Regex tìm các key: `password`, `pass`, `token`, `accessToken`, `refreshToken`, `client_secret`.
  - Thay thế giá trị bằng `******` trước khi in ra log để bảo vệ an toàn thông tin theo chuẩn OWASP.

---

### 3.3. Cảnh Báo Tức Thời Qua Telegram Bot (@BotFather)
- **`TelegramNotifier`:**
  - Đọc `TELEGRAM_BOT_TOKEN` và `TELEGRAM_CHAT_ID` từ biến môi trường/`.env`.
  - Gửi HTTP POST JSON tới: `https://api.telegram.org/bot<TOKEN>/sendMessage`.
  - **Phương thức `@Async`:** Chạy bất đồng bộ trên luồng riêng, đảm bảo việc gửi tin nhắn Telegram không làm chậm tốc độ của ứng dụng web.
  - **Ứng dụng:** Gửi tin khi có user đăng nhập thành công hoặc khi `GlobalExceptionHandler` bắt được lỗi nghiêm trọng 500.

---

### 3.4. Đa Ngôn Ngữ i18n (Internationalization)
- **File cấu hình:** [`I18nConfig.java`](file:///d:/baitapday2/oauth2-demo/src/main/java/com/keycloak/oauth2_demo/config/I18nConfig.java)
  - `CookieLocaleResolver` hoặc `SessionLocaleResolver`: Lưu ngôn ngữ người dùng đã chọn.
  - `LocaleChangeInterceptor`: Lắng nghe tham số URL `?lang=vi` hoặc `?lang=en`.
- **Trong HTML (Thymeleaf):**
  - Dùng `th:text="#{login.title}"` để hiển thị tiêu đề tự động đổi theo ngôn ngữ.
  - Dùng thẻ chuyển đổi: `<a href="?lang=vi">Tiếng Việt</a> | <a href="?lang=en">English</a>`.

---

### 3.5. Xử Lý Ngoại Lệ Tập Trung (GlobalExceptionHandler)
- Sử dụng `@ControllerAdvice` / `@RestControllerAdvice` trong [`GlobalExceptionHandler.java`](file:///d:/baitapday2/oauth2-demo/src/main/java/com/keycloak/oauth2_demo/config/GlobalExceptionHandler.java).
- Bắt các lỗi cụ thể:
  - `@ExceptionHandler(AccessDeniedException.class)` ➔ Trả về giao diện `access-denied.html` (Mã 403 Forbidden).
  - `@ExceptionHandler(MethodArgumentNotValidException.class)` ➔ Trả về thông báo lỗi Validate dữ liệu đầu vào (Mã 400).
  - `@ExceptionHandler(Exception.class)` ➔ Bắt mọi lỗi 500 chưa lường trước, tự động gọi `telegramNotifier.sendErrorAlert(e)` để báo động cho lập trình viên.

---

### 3.6. Giao Diện Người Dùng Thymeleaf SSR
- Các file HTML nằm trong `src/main/resources/templates/`:
  - `login.html`: Giao diện trang đăng nhập hỗ trợ đa ngôn ngữ i18n.
  - `register.html`: Giao diện đăng ký tài khoản.
  - `access-denied.html`: Trang báo lỗi khi người dùng không đủ quyền truy cập tài nguyên.

---

### 3.7. Tối Ưu Mã Nguồn Với Lombok
- Áp dụng trên các DTO ([`LoginDto.java`](file:///d:/baitapday2/oauth2-demo/src/main/java/com/keycloak/oauth2_demo/user/dto/LoginDto.java), [`RegisterDto.java`](file:///d:/baitapday2/oauth2-demo/src/main/java/com/keycloak/oauth2_demo/user/dto/RegisterDto.java), [`AuthResponseDto.java`](file:///d:/baitapday2/oauth2-demo/src/main/java/com/keycloak/oauth2_demo/user/dto/AuthResponseDto.java)):
  - `@Data`: Tự sinh Getter, Setter, `equals()`, `hashCode()`, `toString()`.
  - `@Builder`: Hỗ trợ tạo object nhanh chóng.
  - `@Slf4j`: Tự sinh đối tượng `log` trong Service và Filter để ghi log.

---

# 4. BẢNG CHEAT-SHEET CÁC LƯU Ý & LỖI CẦN TRÁNH KHI VẬN HÀNH

| Vấn đề kỹ thuật | Nguyên nhân | Cách xử lý chuẩn xác |
| :--- | :--- | :--- |
| **Lỗi `Invalid parameter: redirect_uri` trên Keycloak** | Cấu hình Valid Redirect URIs trong Keycloak Client chưa khớp với URL của Spring Boot. | Vào Keycloak Admin ➔ Clients ➔ Chọn Client ➔ Thêm `http://localhost:8080/*` vào mục **Valid Redirect URIs**. |
| **Mất Request Body khi qua `HttpLoggingFilter`** | `InputStream` của `HttpServletRequest` bị đọc hết trước khi vào Controller. | Bắt buộc bọc Request bằng `ContentCachingRequestWrapper` và gọi `responseWrapper.copyBodyToResponse()`. |
| **Lộ mật khẩu/Token trong file Log** | Ghi log trực tiếp JSON của client gửi lên. | Luôn bọc chuỗi qua `SensitiveMasker.mask(...)` trước khi gọi `log.info(...)`. |
| **Gửi Telegram làm đơ/chậm giao diện web** | Gọi API Telegram theo kiểu đồng bộ (Sync) tốn thời gian chờ mạng. | Thêm `@Async` vào phương thức gửi tin nhắn trong `TelegramNotifier` và bật `@EnableAsync` trong cấu hình. |
| **Lỗi font chữ tiếng Việt trong `messages_vi.properties`** | File properties bị lưu ở chuẩn mã hóa ISO-8859-1. | Cấu hình encoding của file Resource Bundle trong IDE là **UTF-8**. |
| **Lỗi 403 Forbidden khi truy cập trang** | Người dùng chưa được cấp Role phù hợp trong Keycloak. | Gán Role tương ứng cho User trên Keycloak Admin Console hoặc hiển thị trang `access-denied.html`. |

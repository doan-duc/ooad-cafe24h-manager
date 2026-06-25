# ☕ Cafe24h Manager

<div align="center">

**Study Coffee Shop Management System 24/7**  
*Hệ thống quản lý quán cà phê học tập 24/24*

---

[![English Version](https://img.shields.io/badge/Language-English-red?style=for-the-badge&logo=github)](#english-version)
[![Bản Tiếng Việt](https://img.shields.io/badge/Ng%C3%B4n_ng%E1%BB%AF-Ti%E1%BA%BFng_Vi%E1%BB%87t-blue?style=for-the-badge&logo=vietnam)](#vietnamese-version)

</div>

---

## <span id="english-version">🇬🇧 English Version</span>

Java Swing desktop application connected to SQL Server — managing tables, orders, kitchen/bar operations, inventory, memberships, and billing in a single application.

<div align="center">
  <img src="https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk&logoColor=white" alt="Java" />
  <img src="https://img.shields.io/badge/SQL%20Server-2019%2B-CC2927?style=flat-square&logo=microsoftsqlserver&logoColor=white" alt="SQL Server" />
  <img src="https://img.shields.io/badge/UI-Java%20Swing-orange?style=flat-square" alt="Swing" />
  <img src="https://img.shields.io/badge/Theme-FlatLaf%203.7.1-5C6BC0?style=flat-square" alt="FlatLaf" />
</div>

---

### 📑 Table of Contents

1. [Overview](#overview-en)
2. [Technologies Used](#technologies-en)
3. [Database Setup](#database-setup-en)
4. [Running on Eclipse](#run-eclipse-en)
5. [Running on VS Code](#run-vscode-en)
6. [Manual Build & Run](#build-run-en)
7. [First-time SQL Server Connection](#sql-connect-en)
8. [Create First Shop Owner](#create-owner-en)
9. [Store Configuration Sequence](#store-config-en)
10. [Access Control & Roles](#permissions-en)
11. [Runtime Options](#runtime-options-en)
12. [Project Structure](#project-structure-en)

---

### 🎯 <span id="overview-en">Overview</span>

Cafe24h Manager is built according to the data structures and business logic described in **Chapters 1, 2, and 3** of the OOAD report. The system supports a 24/7 study cafe model with a complete suite of business operations:

- 🪑 **Table & Space Management** — visual layout, check-in/check-out, hourly billing
- 🧾 **Order & Bar/Kitchen Operations** — send items to the kitchen, track preparation status
- 📦 **Inventory Management** — recipe definition, automatic stock deduction, low stock alerts, stocktaking
- 👥 **Membership** — top up hourly packages, point accumulation, member tiering
- 💳 **Billing & Payment** — cash / QR / card, promotional vouchers
- 📊 **Reports & Shift Closing** — revenue by shift/day/month, cash drawer reconciliation

---

### 🛠 <span id="technologies-en">Technologies Used</span>

| Component | Version | Role |
|---|---|---|
| **Java** | JDK 17 (JavaSE-17) | Language & runtime |
| **Java Swing** | — | Desktop user interface |
| **SQL Server** | 2019+ | Database |
| **Microsoft JDBC Driver** | 13.4.0 (jre11) | Database connection & Windows Authentication |
| **FlatLaf** | 3.7.1 | Modern interface, Light/Dark mode support |
| **jBCrypt** | 0.4 | Password hashing |

> 📁 All libraries are already included in the `lib/` directory — no additional downloads needed.

---

### 1. <span id="database-setup-en">Database Setup</span>

1. Open **SQL Server Management Studio** and connect to `localhost`.
2. *(If a previous version exists)* open and **`Execute`** the script `SQL\Reset_Cafe24hDB.sql` to completely remove the database `Cafe24hDB`.
3. Open `SQL\Cafe24hDB.sql`, click **`Execute`** and wait for the message `Query executed successfully`.
4. The output at the end of the script must look exactly like this:

```text
TenDatabase      = Cafe24hDB
SoBang           = 25
SoNhanVienBanDau = 0
SoVaiTro         = 4
SoKhungCa        = 3
```

> ⚠️ No sample employee accounts are created. The first **Shop Owner** account must be created from the Java application interface in [step 6](#create-owner-en).

---

### 2. <span id="run-eclipse-en">Running on Eclipse</span>

1. Open Eclipse → `File` → `Import…`
2. Select `General` → `Existing Projects into Workspace` → **Next**.
3. Select the project root folder:

   ```text
   D:\OOAD\BTL_OOAD
   ```

4. Keep the JRE as **`JavaSE-17`** and wait for Eclipse to build the project.
5. In the **Project Explorer**, right-click on `Cafe24hManager.launch`.
6. Choose `Run As` → **`Cafe24hManager`**.

> The `Cafe24hManager.launch` file is pre-configured with UTF-8 encoding and the native DLL directory (`lib/native`) for Windows Authentication.

---

### 3. <span id="run-vscode-en">Running on VS Code</span>

1. Install the **Extension Pack for Java** if you haven't already.
2. Open the project folder:

   ```text
   D:\OOAD\BTL_OOAD
   ```

3. *(If you see red error markers)* press `Ctrl+Shift+P` → `Java: Clean Java Language Server Workspace` → **Restart and delete**, then wait for the import to complete.
4. Open the **Run and Debug** tab (`Ctrl+Shift+D`), select the **`Cafe24hManager`** configuration, and press **`F5`**.

> The `.vscode/launch.json` configuration is pre-set with `mainClass = ui.App`, source path `src`, and automatically loads all `.jar` files in the `lib/` folder.

---

### 4. <span id="build-run-en">Manual Build & Run</span>

Open **PowerShell** in the project root directory:

```powershell
cd D:\OOAD\BTL_OOAD

# Compile all source code into the bin directory
javac -encoding UTF-8 -d bin -cp "lib/*" (Get-ChildItem -Recurse -Path src -Filter *.java).FullName

# Run the application
java --enable-native-access=ALL-UNNAMED "-Djava.library.path=lib/native" -Dfile.encoding=UTF-8 -cp "bin;lib/*" ui.App
```

---

### 5. <span id="sql-connect-en">First-time SQL Server Connection</span>

In the connection screen, enter:

```text
Server         : localhost
Port           : 1433
Database       : Cafe24hDB
Authentication : Windows
```

Click **`Kiểm tra và lưu kết nối`** (Test and Save Connection). The configuration will be saved in `config/database.properties`.

> If SQL Server uses a separate account, select **`SQL Server`** authentication and enter the username/password.

---

### 6. <span id="create-owner-en">Create First Shop Owner</span>

Since the `NhanVien` (Employee) table is initially empty, the application will automatically open the initialization screen:

1. Enter the employee ID, e.g., `NV001`.
2. Enter the full name and phone number.
3. Enter a password of **at least 6 characters**.
4. Click **`Khởi tạo hệ thống`** (Initialize System).
5. Log in using the **employee ID** or **phone number**.

---

### 7. <span id="store-config-en">Store Configuration Sequence</span>

After logging in as the Shop Owner, perform configurations in this exact order:

| # | Module | Action |
|---|---|---|
| 1 | **Store Setup** | Create areas → create tables |
| 2 | **Menu & Recipe** | Create categories |
| 3 | **Inventory** | Create ingredients and make stock-in receipts |
| 4 | **Menu & Recipe** | Create items → configure ingredient recipes |
| 5 | **Menu & Recipe** | Create items of type `Gói giờ` (Hourly package, if applicable) |
| 6 | **Employees** | Create accounts for Manager, Cashier, Bartender |
| 7 | **Store Setup** | Create vouchers (if needed) |

---

### 8. <span id="permissions-en">Access Control & Roles</span>

| Role | Functions |
|---|---|
| 👑 **Shop Owner** | Full system access |
| 🧑‍💼 **Operations Manager** | View tables, stock levels, approve inventory, view employees, reports, and shift history |
| 💁 **Cashier / Receptionist** | Table management, check-in, orders, customers, booking, hourly package top-up, payment, and shifts |
| ☕ **Bartender / Inventory** | Bar/Kitchen prep board, ingredients, warehouse entry, inventory auditing, and shifts |

> The application **hides** modules that do not belong to the user's role, and the controllers **re-verify permissions** before performing any write operations.

---

### ⚙️ <span id="runtime-options-en">Runtime Options</span>

The application features transition effects, hover animations, and subtle ambient effects. Light/Dark theme preferences are saved locally. These can be overridden using system properties at startup:

**Disable animations:**

```powershell
java -Dcafe24h.motion=false "-Djava.library.path=lib/native" -cp "bin;lib/*" ui.App
```

**Force Dark Theme:**

```powershell
java -Dcafe24h.theme=dark "-Djava.library.path=lib/native" -cp "bin;lib/*" ui.App
```

| Property | Values | Default | Effect |
|---|---|---|---|
| `cafe24h.motion` | `true` / `false` | `true` | Enable/disable transition and hover effects |
| `cafe24h.theme` | `dark` / `light` | *(saved locally)* | Force theme on startup |
| `cafe24h.config` | file path | `config/database.properties` | Change the path of the database connection file |

---

### 📂 <span id="project-structure-en">Project Structure</span>

```text
BTL_OOAD/
├── src/
│   ├── ui/             # Swing UI (App.java is the entry point)
│   ├── controller/     # Business logic coordination & permission verification
│   ├── dao/            # Data access objects (JDBC)
│   ├── db/             # SQL Server configurations & connections
│   ├── model/          # Data model entities
│   ├── security/       # Authorization & user session management
│   └── tools/          # Testing & screen capture utilities
├── lib/                # JAR libraries + native DLLs
├── config/             # database.properties
├── SQL/                # Cafe24hDB.sql, Reset_Cafe24hDB.sql, data.sql
├── bin/                # Compiled code (.class files)
└── Cafe24hManager.launch
```

<div align="center">

---

**24H Cafe Management System** · Object-Oriented Analysis & Design Assignment

---

[Back to top](#cafe24h-manager)

</div>

---
---

## <span id="vietnamese-version">🇻🇳 Bản Tiếng Việt</span>

Ứng dụng desktop Java Swing kết nối SQL Server — quản lý bàn, order, pha chế, kho nguyên liệu, hội viên và doanh thu trong một ứng dụng duy nhất.

<div align="center">
  <img src="https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk&logoColor=white" alt="Java" />
  <img src="https://img.shields.io/badge/SQL%20Server-2019%2B-CC2927?style=flat-square&logo=microsoftsqlserver&logoColor=white" alt="SQL Server" />
  <img src="https://img.shields.io/badge/UI-Java%20Swing-orange?style=flat-square" alt="Swing" />
  <img src="https://img.shields.io/badge/Theme-FlatLaf%203.7.1-5C6BC0?style=flat-square" alt="FlatLaf" />
</div>

---

### 📑 Mục lục

1. [Tổng quan](#tong-quan-vi)
2. [Công nghệ sử dụng](#cong-nghe-vi)
3. [Tạo cơ sở dữ liệu](#database-setup-vi)
4. [Chạy trên Eclipse](#run-eclipse-vi)
5. [Chạy trên VS Code](#run-vscode-vi)
6. [Build & chạy thủ công](#build-run-vi)
7. [Kết nối SQL Server lần đầu](#sql-connect-vi)
8. [Tạo Chủ quán đầu tiên](#create-owner-vi)
9. [Thứ tự cấu hình cửa hàng](#store-config-vi)
10. [Phân quyền](#permissions-vi)
11. [Tùy chọn khi chạy](#runtime-options-vi)
12. [Cấu trúc dự án](#project-structure-vi)

---

### 🎯 <span id="tong-quan-vi">Tổng quan</span>

Cafe24h Manager được xây dựng theo cấu trúc dữ liệu và nghiệp vụ ở **Chương 1, 2, 3** của báo cáo OOAD. Hệ thống phục vụ mô hình quán cà phê học tập hoạt động 24/24 với đầy đủ nghiệp vụ:

- 🪑 **Quản lý bàn & không gian** — sơ đồ trực quan, check-in/check-out, tính tiền giờ
- 🧾 **Order & pha chế** — gửi món xuống bếp, theo dõi trạng thái pha chế
- 📦 **Kho nguyên liệu** — định mức công thức, tự trừ kho, cảnh báo hết hàng, kiểm kê
- 👥 **Hội viên** — nạp gói giờ, tích điểm, phân hạng thành viên
- 💳 **Thanh toán** — tiền mặt / QR / thẻ, voucher khuyến mãi
- 📊 **Báo cáo & chốt ca** — doanh thu theo ca/ngày/tháng, đối chiếu két tiền

---

### 🛠 <span id="cong-nghe-vi">Công nghệ sử dụng</span>

| Thành phần | Phiên bản | Vai trò |
|---|---|---|
| **Java** | JDK 17 (JavaSE-17) | Ngôn ngữ & runtime |
| **Java Swing** | — | Giao diện desktop |
| **SQL Server** | 2019+ | Cơ sở dữ liệu |
| **Microsoft JDBC Driver** | 13.4.0 (jre11) | Kết nối CSDL + Windows Authentication |
| **FlatLaf** | 3.7.1 | Giao diện hiện đại, hỗ trợ Sáng/Tối |
| **jBCrypt** | 0.4 | Mã hóa mật khẩu |

> 📁 Toàn bộ thư viện đã có sẵn trong thư mục `lib/` — không cần tải thêm.

---

### 1. <span id="database-setup-vi">Tạo cơ sở dữ liệu</span>

1. Mở **SQL Server Management Studio** và kết nối tới `localhost`.
2. *(Nếu đã chạy bản SQL cũ)* mở và `Execute` file `SQL\Reset_Cafe24hDB.sql` để xóa sạch database `Cafe24hDB`.
3. Mở `SQL\Cafe24hDB.sql`, nhấn **`Execute`** và chờ dòng `Query executed successfully`.
4. Kết quả cuối script phải đúng như sau:

```text
TenDatabase      = Cafe24hDB
SoBang           = 25
SoNhanVienBanDau = 0
SoVaiTro         = 4
SoKhungCa        = 3
```

> ⚠️ Không có nhân viên mẫu. Tài khoản **Chủ quán** đầu tiên được tạo từ giao diện Java ở [bước 6](#create-owner-vi).

---

### 2. <span id="run-eclipse-vi">Chạy trên Eclipse</span>

1. Mở Eclipse → `File` → `Import…`
2. Chọn `General` → `Existing Projects into Workspace` → **Next**.
3. Chọn thư mục gốc dự án:

   ```text
   D:\OOAD\BTL_OOAD
   ```

4. Giữ nguyên JRE **`JavaSE-17`** và chờ Eclipse build xong.
5. Trong **Project Explorer**, nhấp phải `Cafe24hManager.launch`.
6. Chọn `Run As` → **`Cafe24hManager`**.

> File `Cafe24hManager.launch` đã cấu hình sẵn UTF-8 và thư mục native DLL (`lib/native`) cho Windows Authentication.

---

### 3. <span id="run-vscode-vi">Chạy trên VS Code</span>

1. Cài **Extension Pack for Java** nếu chưa có.
2. Mở thư mục dự án:

   ```text
   D:\OOAD\BTL_OOAD
   ```

3. *(Nếu gặp dấu đỏ)* nhấn `Ctrl+Shift+P` → `Java: Clean Java Language Server Workspace` → **Restart and delete**, rồi chờ import xong.
4. Mở tab **Run and Debug** (`Ctrl+Shift+D`), chọn cấu hình **`Cafe24hManager`** và nhấn **`F5`**.

> Cấu hình `.vscode/launch.json` đã đặt sẵn `mainClass = ui.App`, source path `src` và tự nạp mọi `.jar` trong `lib/`.

---

### 4. <span id="build-run-vi">Build & chạy thủ công</span>

Mở **PowerShell** tại thư mục gốc dự án:

```powershell
cd D:\OOAD\BTL_OOAD

# Biên dịch toàn bộ mã nguồn vào thư mục bin
javac -encoding UTF-8 -d bin -cp "lib/*" (Get-ChildItem -Recurse -Path src -Filter *.java).FullName

# Chạy ứng dụng
java --enable-native-access=ALL-UNNAMED "-Djava.library.path=lib/native" -Dfile.encoding=UTF-8 -cp "bin;lib/*" ui.App
```

---

### 5. <span id="sql-connect-vi">Kết nối SQL Server lần đầu</span>

Trong màn hình kết nối, nhập:

```text
Máy chủ      : localhost
Cổng         : 1433
Cơ sở dữ liệu: Cafe24hDB
Xác thực     : Windows
```

Nhấn **`Kiểm tra và lưu kết nối`**. Cấu hình được lưu tại `config/database.properties`.

> Nếu SQL Server dùng tài khoản riêng, chọn **`SQL Server`** rồi nhập username/password.

---

### 6. <span id="create-owner-vi">Tạo Chủ quán đầu tiên</span>

Vì bảng `NhanVien` ban đầu rỗng, ứng dụng tự mở màn hình khởi tạo:

1. Nhập mã nhân viên, ví dụ `NV001`.
2. Nhập họ tên và số điện thoại.
3. Nhập mật khẩu **tối thiểu 6 ký tự**.
4. Chọn **`Khởi tạo hệ thống`**.
5. Đăng nhập bằng **mã nhân viên** hoặc **số điện thoại**.

---

### 7. <span id="store-config-vi">Thứ tự cấu hình cửa hàng</span>

Sau khi đăng nhập bằng Chủ quán, thực hiện theo đúng thứ tự:

| # | Module | Việc cần làm |
|---|---|---|
| 1 | **Thiết lập cửa hàng** | Tạo khu vực → tạo bàn |
| 2 | **Menu và định mức** | Tạo danh mục |
| 3 | **Kho nguyên liệu** | Tạo nguyên liệu và lập phiếu nhập |
| 4 | **Menu và định mức** | Tạo món → khai báo định mức nguyên liệu |
| 5 | **Menu và định mức** | Tạo món loại `Gói giờ` (nếu có bán) |
| 6 | **Nhân viên** | Tạo tài khoản Quản lý, Thu ngân, Pha chế |
| 7 | **Thiết lập cửa hàng** | Tạo voucher (nếu cần) |

---

### 8. <span id="permissions-vi">Phân quyền</span>

| Vai trò | Chức năng |
|---|---|
| 👑 **Chủ quán** | Toàn bộ hệ thống |
| 🧑‍💼 **Quản lý vận hành** | Xem bàn, tồn kho, duyệt kiểm kê, xem nhân viên, báo cáo và lịch sử ca |
| 💁 **Thu ngân / Lễ tân** | Bàn, check-in, order, khách hàng, booking, nạp giờ, thanh toán và ca |
| ☕ **Pha chế / Kho** | Bảng pha chế, nguyên liệu, nhập kho, kiểm kê và ca |

> Ứng dụng **ẩn** module không thuộc vai trò, và controller **kiểm tra lại quyền** trước mọi thao tác ghi dữ liệu.

---

### ⚙️ <span id="runtime-options-vi">Tùy chọn khi chạy</span>

Ứng dụng có hiệu ứng chuyển trang, hover và ambient nhẹ; lựa chọn theme Sáng/Tối được lưu trên máy. Có thể ghi đè bằng system property khi chạy:

**Tắt animation:**

```powershell
java -Dcafe24h.motion=false "-Djava.library.path=lib/native" -cp "bin;lib/*" ui.App
```

**Ép theme Tối:**

```powershell
java -Dcafe24h.theme=dark "-Djava.library.path=lib/native" -cp "bin;lib/*" ui.App
```

| Property | Giá trị | Mặc định | Tác dụng |
|---|---|---|---|
| `cafe24h.motion` | `true` / `false` | `true` | Bật/tắt hiệu ứng chuyển động |
| `cafe24h.theme` | `dark` / `light` | *(lưu trên máy)* | Ép theme khi khởi động |
| `cafe24h.config` | đường dẫn | `config/database.properties` | Đổi file cấu hình kết nối |

---

### 📂 <span id="project-structure-vi">Cấu trúc dự án</span>

```text
BTL_OOAD/
├── src/
│   ├── ui/             # Giao diện Swing (App.java là điểm khởi chạy)
│   ├── controller/     # Điều phối nghiệp vụ & kiểm tra quyền
│   ├── dao/            # Truy xuất dữ liệu (JDBC)
│   ├── db/             # Cấu hình & kết nối SQL Server
│   ├── model/          # Lớp thực thể dữ liệu
│   ├── security/       # Phân quyền, phiên đăng nhập
│   └── tools/          # Tiện ích kiểm thử & chụp màn hình
├── lib/                # Thư viện JAR + native DLL
├── config/             # database.properties
├── SQL/                # Cafe24hDB.sql, Reset_Cafe24hDB.sql, data.sql
├── bin/                # Mã đã biên dịch (.class)
└── Cafe24hManager.launch
```

<div align="center">

---

**Hệ thống Quản lý Quán Cà phê 24H** · Bài tập lớn môn OOAD

---

[Quay lại đầu trang](#cafe24h-manager)

</div>

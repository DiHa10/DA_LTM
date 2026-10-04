# HỆ THỐNG ĐẶT PHÒNG HỌP CÔNG TY (MEETING ROOM BOOKING SYSTEM)
> **Đồ án môn:** Lập trình mạng (DA_LTM)  
> **Kiến trúc:** Client - Server qua giao thức **TCP Socket**  
> **Xử lý đồng thời:** Đa luồng (**Multi-threading**) kết hợp **`synchronized`** chặn đặt trùng lịch (Race Condition)  
> **Giao diện:** Java Swing phẳng hiện đại (**FlatLaf**)  
> **Môi trường:** Apache NetBeans IDE 24, Java 17+, Maven, CSDL nhúng SQLite (tự động tạo file `meeting_room.db`)

---

## 🎯 CÁC TÍNH NĂNG NỔI BẬT ĐÁP ỨNG ĐỀ TÀI

1. **Giao thức mạng TCP Socket:**
   - Server mở `ServerSocket` tại cổng `8888`.
   - Mỗi Client kết nối tạo một `ClientHandler` chạy trên Thread độc lập.
   - Dữ liệu trao đổi chuẩn hóa dạng JSON (Gson).
2. **Cơ chế đồng bộ `synchronized` chống đặt trùng lịch & Gia hạn phòng:**
   - Khi có nhiều nhân viên gửi yêu cầu đặt cùng 1 phòng vào cùng 1 khung giờ cùng một lúc (Race Condition), khối `synchronized` trong [BookingService.java](file:///d:/NetBeansProjects/DA_LTM/src/main/java/com/meeting/server/service/BookingService.java) sẽ khóa critical section.
   - **Chỉ 1 nhân viên đầu tiên đặt thành công**, các yêu cầu sau sẽ bị chặn lại kèm thông báo xung đột lịch chi tiết.
   - **Gia hạn giờ họp (+30 phút)**: Kiểm tra xung đột khung giờ nới rộng theo thời gian thực dưới khối `synchronized`.
3. **Mời đồng nghiệp tham gia họp qua mạng TCP (Real-time TCP Push Notification):**
   - Khi tạo lịch họp, người đặt có thể tích chọn các đồng nghiệp cần mời.
   - Server phân tích danh sách và gửi thông báo trực tiếp qua socket (`INVITATION_NOTIFICATION`) tới máy trạm của đồng nghiệp kèm âm thanh báo động.
4. **Kênh Chat nhanh nội bộ TCP (In-App Quick Chat Broadcast):**
   - Kênh trao đổi nội bộ thời gian thực giữa các máy trạm nhân viên để phối hợp công tác chuẩn bị phòng họp, trao đổi tài liệu và thiết bị trực tiếp qua TCP Socket mà không cần dùng ứng dụng bên ngoài.
5. **Trả phòng sớm & Giải phóng lịch họp (Early Release):**
   - Nút trả phòng sớm giúp hoàn thành cuộc họp ngay khi xong việc và mở lại khung giờ cho các nhân viên khác đặt ngay lập tức.
6. **Nhắc nhở tự động trước giờ họp 60 phút và 15 phút (Background Worker):**
   - Luồng chạy ngầm trên Server [MeetingReminderService.java](file:///d:/NetBeansProjects/DA_LTM/src/main/java/com/meeting/server/service/MeetingReminderService.java) quét lịch định kỳ và chủ động gửi thông báo TCP Push tới máy khách.
7. **Cơ chế chống đăng nhập trùng lặp (Anti-duplicate Login):**
   - Khóa đồng bộ `synchronized` trên Server kiểm tra danh sách phiên đang hoạt động.
   - Khi 1 tài khoản đã online ở Client A, nếu ai đó cố tình đăng nhập tài khoản này ở Client B thì Server sẽ lập tức từ chối.

---

## 🚀 HƯỚNG DẪN MỞ VÀ CHẠY DỰ ÁN TRÊN APACHE NETBEANS IDE 24

### Bước 1: Mở dự án trong NetBeans 24
1. Khởi động **Apache NetBeans IDE 24**.
2. Trên thanh menu, chọn **File** -> **Open Project...** (hoặc phím tắt `Ctrl + Shift + O`).
3. Điều hướng đến thư mục: `D:\NetBeansProjects\DA_LTM`.
4. Nhấn **Open Project**. NetBeans sẽ nhận diện đây là một Maven Project và tự động tải thư viện (FlatLaf, SQLite, Gson) trong vài giây.

### Bước 2: Chạy Server (Máy chủ TCP)
1. Trong cửa sổ **Projects** bên trái NetBeans, mở cây thư mục:  
   `Source Packages` -> `com.meeting.server` -> [ServerApp.java](file:///d:/NetBeansProjects/DA_LTM/src/main/java/com/meeting/server/ServerApp.java).
2. **Click chuột phải** vào [ServerApp.java](file:///d:/NetBeansProjects/DA_LTM/src/main/java/com/meeting/server/ServerApp.java) -> chọn **Run File** (hoặc nhấn `Shift + F6`).
3. Cửa sổ **MÁY CHỦ TCP ĐẶT PHÒNG HỌP** sẽ hiển thị:
   - Server tự động khởi động tại cổng `8888`.
   - CSDL SQLite (`meeting_room.db`) tự động được tạo và chèn dữ liệu mẫu phòng họp & tài khoản.
   - Giao diện có tab theo dõi log trực tiếp và danh sách các máy khách đang kết nối.

### Bước 3: Chạy Client (Máy trạm Nhân viên)
1. Trong cây thư mục:  
   `Source Packages` -> `com.meeting.client` -> [ClientApp.java](file:///d:/NetBeansProjects/DA_LTM/src/main/java/com/meeting/client/ClientApp.java).
2. **Click chuột phải** vào [ClientApp.java](file:///d:/NetBeansProjects/DA_LTM/src/main/java/com/meeting/client/ClientApp.java) -> chọn **Run File** (hoặc nhấn `Shift + F6`).
3. Cửa sổ **Đăng nhập** sẽ xuất hiện:
   - Bạn có thể mở đồng thời 2 hoặc 3 cửa sổ Client cùng lúc (lặp lại thao tác Run File) để thử nghiệm nhiều người dùng khác nhau!

---

## 🔑 DANH SÁCH TÀI KHOẢN THỬ NGHIỆM SẴN CÓ

Hệ thống có menu chọn nhanh tài khoản tại màn hình đăng nhập:

| Tên đăng nhập | Mật khẩu | Họ và tên | Phòng ban | Quyền hạn |
| :--- | :--- | :--- | :--- | :--- |
| `admin` | `admin123` | Quản Trị Viên (Admin) | Ban Giám Đốc | Toàn quyền (Thêm/Sửa/Xóa phòng, Hủy mọi lịch) |
| `nhanvien1` | `123456` | An | Phòng Kỹ Thuật IT | Đặt phòng, Hủy lịch cá nhân |
| `nhanvien2` | `123456` | Vũ | Phòng Marketing | Đặt phòng, Hủy lịch cá nhân |
| `nhanvien3` | `123456` | Kha | Phòng Nhân Sự | Đặt phòng, Hủy lịch cá nhân |

---

## 🧪 KỊCH BẢN DEMO ĐẠT ĐIỂM TỐI ĐA TRƯỚC GIẢNG VIÊN

### Kịch bản 1: Đặt phòng & Broadcast thời gian thực
1. Chạy 1 Server và 2 Client:
   - Client 1 đăng nhập bằng `nhanvien1`.
   - Client 2 đăng nhập bằng `nhanvien2`.
2. Trên Client 1, bấm nút **"➕ ĐẶT PHÒNG HỌP"**:
   - Chọn *Phòng Họp Sáng Tạo A1*, ngày hôm nay, giờ `09:00 - 10:30`.
   - Bấm **Xác nhận Đặt phòng**.
3. **Quan sát:**
   - Client 1 báo "Đặt phòng thành công".
   - Ngay lập tức trên Client 2, bảng lịch tự động cập nhật cuộc họp mới mà **không cần bấm F5** (nhờ tính năng Real-time Broadcast của Socket).
   - Trên màn hình Server Monitor, hiển thị dòng log giao dịch thành công.

### Kịch bản 2: Kiểm thử thực tế cơ chế `synchronized` chặn trùng lịch (Race Condition)
1. Mở song song 2 cửa sổ Client trên màn hình:
   - Cửa sổ Client 1: đăng nhập tài khoản **An** (`nhanvien1`).
   - Cửa sổ Client 2: đăng nhập tài khoản **Vũ** (`nhanvien2`).
2. Trên cả 2 máy, cùng mở hộp thoại **"➕ ĐẶT PHÒNG HỌP"** và chọn:
   - Cùng 1 phòng: *Phòng Họp Sáng Tạo A1*.
   - Cùng 1 ngày: hôm nay.
   - Cùng 1 khung giờ: ví dụ `14:00 - 15:30`.
3. Bấm nút **"Xác nhận Đặt phòng"** ở cả 2 bên:
   - Phía người bấm trước (dù chỉ nhanh hơn vài mili-giây): Server chấp nhận và thông báo **"Đặt phòng thành công"**.
   - Phía người bấm sau: Server dùng `synchronized` kiểm tra thấy phòng đã bị chiếm và lập tức hiện cảnh báo: **"CẢNH BÁO: TRÙNG LỊCH PHÒNG HỌP: Khung giờ này đã được đặt bởi An..."**.
   - Chứng minh rõ ràng trước giảng viên tính đúng đắn của cơ chế `synchronized` bảo vệ tài nguyên chia sẻ!

---

## 📁 CẤU TRÚC MÃ NGUỒN DỰ ÁN

```text
DA_LTM/
├── pom.xml                                 # File cấu hình Maven (FlatLaf, SQLite, Gson)
├── schema.sql                              # Kịch bản SQL tạo bảng và seed dữ liệu
├── meeting_room.db                         # File CSDL SQLite (tự sinh khi chạy Server)
└── src/main/java/com/meeting/
    ├── common/                             # Các thành phần dùng chung Client - Server
    │   ├── model/                          # User.java, Room.java, Booking.java
    │   └── protocol/                       # ActionType.java, Request.java, Response.java, JsonUtil.java
    │
    ├── server/                             # Máy chủ TCP Socket Backend
    │   ├── ServerApp.java                  # Main entry point Server
    │   ├── core/                           # ServerManager.java, ClientHandler.java
    │   ├── dao/                            # UserDao.java, RoomDao.java, BookingDao.java
    │   ├── db/                             # DatabaseManager.java (SQLite auto-init)
    │   ├── service/                        # BookingService.java (Chứa logic synchronized cốt lõi)
    │   └── ui/                             # ServerMonitorFrame.java (Giao diện giám sát Server)
    │
    ├── client/                             # Máy trạm giao diện người dùng
    │   ├── ClientApp.java                  # Main entry point Client
    │   ├── net/                            # SocketClient.java (Quản lý TCP connection & listener)
    │   └── ui/                             # LoginForm.java, MainDashboard.java, BookingDialog.java
    │
    └── test/                               # Kiểm thử độc lập
        └── ConcurrencySimulationTest.java  # Script giả lập tải 10 thread cùng lúc
```

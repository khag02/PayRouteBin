# ISO8583 NAPAS Simulator - Routing & Transaction Logging

## 🧩 Mục tiêu dự án

Dự án mô phỏng một hệ thống xử lý giao dịch thanh toán theo chuẩn **ISO 8583**, phù hợp với mô hình hoạt động của **NAPAS** tại Việt Nam. Hệ thống phục vụ mục đích học tập và thử nghiệm trong các nội dung:

- Gửi và nhận thông điệp ISO8583 qua TCP
- Định tuyến giao dịch dựa trên **BIN code** của thẻ
- Lưu thông tin giao dịch vào hệ thống (cơ sở dữ liệu)
- Áp dụng các cấu trúc cấu hình trong jPOS (deploy files, packager, transaction manager)

---

## 🔧 Thành phần chính

### 1. Server

- Sử dụng jPOS để khởi tạo `ServerChannel` lắng nghe và nhận bản tin
- Phân tích và định tuyến giao dịch dựa theo:
- **BIN code (Bank Identification Number)**
- Các cấu hình `<endpoint>` và `<regexp>` trong `SelectDestination` participant
- Áp dụng kiểm tra số thẻ bằng thuật toán **Luhn** file generate_card.py để tạo ra pan hợp lệ với thật toán Luhn
- Ghi log đầy đủ toàn bộ thông điệp, header, và kết quả xử lý

### 2. Transaction Manager (jPOS)

- Quản lý luồng giao dịch bằng các participant như:
- `CheckFields`
- `SelectDestination`
- `TransactionLogger`
- `SendResponse`
- Lưu lại thông tin giao dịch hợp lệ (trong database)

---

## 📦 Cấu trúc file quan trọng

| File / Thư mục                      | Mô tả |
|------------------------------------|-------|
| `deploy/10_server.xml`             | Cấu hình server channel kết nối với host bank |
| `deploy/30_txnmgr.xml`             | TransactionManager định nghĩa các participant xử lý |
| `cfg/napas.xml`                    | ISO8583 packager định nghĩa cấu trúc các trường theo chuẩn iso8583 của napas|
| `log/`                             | Log giao dịch |
| `README.md`                        | Tài liệu hướng dẫn |

---

## 📍 Routing theo BIN

Trong participant `SelectDestination`, bạn có thể cấu hình routing như sau:

```xml
<participant class="org.jpos.transaction.participant.SelectDestination">
  <property name="request" value="REQUEST" />
  <property name="destination" value="DESTINATION" />
  <endpoint destination="VCB">970426 411130</endpoint>
  <endpoint destination="VIB">970441</endpoint>
  <endpoint destination="TPB">970423</endpoint>
  <regexp destination="NAPAS_SPECIAL">^4111[0-9]{2}.*</regexp>
</participant>

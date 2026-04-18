# Tài Liệu Yêu Cầu - Hệ Thống Quản Lý Phí Sàn (Commission Fee)

## Giới Thiệu

Hệ thống quản lý phí sàn cho phép Admin cấu hình và tự động thu phí từ các giao dịch của nghệ nhân. Phí sàn được trừ tự động khi xử lý callback thanh toán, đảm bảo nghệ nhân chỉ nhận số tiền sau khi đã trừ phí.

## Thuật Ngữ

- **System**: Hệ thống Catholic Souvenir Custom Order Platform
- **Admin**: Quản trị viên hệ thống có quyền cấu hình phí sàn
- **Artisan**: Nghệ nhân bán sản phẩm và nhận thanh toán qua nền tảng
- **Customer**: Khách hàng mua sản phẩm
- **Commission_Rate**: Tỷ lệ phí sàn (%) mà Admin thu từ mỗi giao dịch
- **Commission_Amount**: Số tiền phí sàn thực tế được trừ từ giao dịch
- **Net_Amount**: Số tiền nghệ nhân thực nhận sau khi trừ phí sàn
- **Wallet**: Ví điện tử của nghệ nhân lưu trữ số dư
- **Wallet_Transaction**: Giao dịch ví (nạp tiền, thanh toán, hoàn tiền)
- **Payment**: Thanh toán từ khách hàng qua VNPay/ZaloPay
- **Payment_Callback**: Callback từ cổng thanh toán xác nhận giao dịch thành công
- **Order**: Đơn hàng sản phẩm thông thường
- **Custom_Order**: Đơn hàng tùy chỉnh
- **Stage_Payment**: Thanh toán theo giai đoạn cho custom order
- **System_Config**: Bảng lưu cấu hình hệ thống

## Yêu Cầu

### Yêu Cầu 1

**User Story:** Là Admin, tôi muốn cấu hình tỷ lệ phí sàn toàn hệ thống, để thu phí từ các giao dịch của nghệ nhân.

#### Tiêu Chí Chấp Nhận

1. THE System SHALL lưu trữ commission_rate dưới dạng BigDecimal trong bảng system_config với key "PLATFORM_COMMISSION_RATE"
2. WHEN Admin cập nhật commission_rate, THE System SHALL validate giá trị từ 0 đến 100
3. WHILE thời gian hiện tại KHÔNG PHẢI từ 00:00 đến 00:59 (12 giờ đêm), THE System SHALL từ chối cập nhật commission_rate với lỗi "Chỉ có thể cập nhật phí sàn từ 00:00-00:59"
4. WHEN Admin xem cấu hình hệ thống, THE System SHALL hiển thị commission_rate hiện tại và thời gian cho phép cập nhật
5. THE System SHALL ghi log mỗi lần Admin thay đổi commission_rate với timestamp và giá trị cũ/mới
6. THE System SHALL cache commission_rate trong Redis với TTL 24 giờ để tránh query database liên tục

### Yêu Cầu 2

**User Story:** Là Admin, tôi muốn xem API endpoint để cấu hình commission rate, để dễ dàng quản lý phí sàn.

#### Tiêu Chí Chấp Nhận

1. THE System SHALL cung cấp endpoint GET /api/admin/commission/config để xem cấu hình
2. THE System SHALL cung cấp endpoint PUT /api/admin/commission/config để cập nhật commission_rate
3. WHEN Admin gọi PUT endpoint ngoài khung giờ 00:00-00:59, THE System SHALL trả về lỗi 400 với message rõ ràng
4. WHEN Admin gọi PUT endpoint với commission_rate không hợp lệ, THE System SHALL trả về lỗi 400
5. THE System SHALL yêu cầu role ADMIN để truy cập các endpoint commission config
6. WHEN cập nhật thành công, THE System SHALL trả về response chứa commission_rate mới và clear Redis cache
7. WHEN Admin cập nhật commission_rate thành công, THE System SHALL gửi notification cho TẤT CẢ Artisan trong hệ thống
8. THE System SHALL bao gồm thông tin old_rate, new_rate, effective_date trong notification
9. THE System SHALL sử dụng notification_type = COMMISSION_RATE_UPDATED
10. THE System SHALL gửi notification trong vòng 10 giây sau khi cập nhật thành công

### Yêu Cầu 3

**User Story:** Là System, tôi muốn TỰ ĐỘNG lấy và lưu commission_rate vào Payment khi tạo để đảm bảo tính nhất quán, đảm bảo payment đang PENDING không bị ảnh hưởng khi Admin thay đổi phí.

#### Tiêu Chí Chấp Nhận

1. WHEN System tạo Payment entity (trước khi redirect đến VNPay/ZaloPay), THE System SHALL TỰ ĐỘNG lấy commission_rate hiện tại từ SystemConfigService
2. THE System SHALL TỰ ĐỘNG lưu commission_rate vào field commission_rate của Payment entity mà KHÔNG yêu cầu input từ Customer hoặc Artisan
3. WHEN System tạo Stage_Payment entity, THE System SHALL TỰ ĐỘNG lấy và lưu commission_rate vào field commission_rate của Stage_Payment entity
4. THE System SHALL đảm bảo commission_rate KHÔNG thay đổi dù Admin cập nhật config sau khi payment được tạo (snapshot tại thời điểm tạo)
5. WHEN Payment có status PENDING và Admin cập nhật commission_rate, THE System SHALL VẪN sử dụng commission_rate cũ đã lưu trong Payment entity
6. WHEN Payment callback được xử lý, THE System SHALL sử dụng commission_rate từ Payment entity (không lấy từ config mới)
7. IF commission_rate không tồn tại trong system_config, THE System SHALL sử dụng giá trị mặc định 0
8. THE System SHALL KHÔNG cho phép Customer, Artisan hoặc bất kỳ user nào nhập tay commission_rate
9. THE System SHALL cache commission_rate trong Redis để tối ưu performance khi tạo nhiều payment đồng thời

### Yêu Cầu 4

**User Story:** Là System, tôi muốn tự động tính và trừ phí sàn khi xử lý Payment callback thành công, để nghệ nhân chỉ nhận số tiền sau khi trừ phí.

#### Tiêu Chí Chấp Nhận

1. WHEN Payment callback được xử lý và Payment có status COMPLETED, THE System SHALL lấy commission_rate từ Payment entity (đã lưu trước đó)
2. THE System SHALL tính commission_amount = payment_amount × commission_rate / 100
3. THE System SHALL tính net_amount = payment_amount - commission_amount (số tiền nghệ nhân nhận)
4. WHEN cộng tiền vào wallet của Artisan, THE System SHALL CHỈ cộng net_amount
5. THE System SHALL tạo Wallet_Transaction với amount = net_amount, commission_fee = commission_amount, commission_rate = commission_rate
6. THE System SHALL KHÔNG tạo transaction riêng cho commission, chỉ ghi vào field commission_fee
7. WHEN tính toán commission, THE System SHALL làm tròn đến 2 chữ số thập phân
8. THE System SHALL đảm bảo logic này được thực hiện TRONG cùng transaction với việc cập nhật Payment status

### Yêu Cầu 5

**User Story:** Là System, tôi muốn tự động tính và trừ phí sàn khi xử lý Stage_Payment callback thành công.

#### Tiêu Chí Chấp Nhận

1. WHEN Stage_Payment callback được xử lý và có status COMPLETED, THE System SHALL lấy commission_rate từ Stage_Payment entity
2. THE System SHALL tính commission_amount = stage_amount × commission_rate / 100
3. THE System SHALL tính net_amount = stage_amount - commission_amount
4. WHEN cộng tiền vào wallet của Artisan, THE System SHALL CHỈ cộng net_amount
5. THE System SHALL tạo Wallet_Transaction với amount = net_amount, commission_fee = commission_amount, commission_rate = commission_rate
6. THE System SHALL liên kết Wallet_Transaction với stage_payment_id
7. THE System SHALL đảm bảo logic này được thực hiện TRONG cùng transaction với việc cập nhật Stage_Payment status

### Yêu Cầu 6

**User Story:** Là Artisan, tôi muốn xem chi tiết phí sàn bị trừ trong mỗi giao dịch, để hiểu rõ thu nhập thực tế của mình.

#### Tiêu Chí Chấp Nhận

1. WHEN Artisan xem wallet transaction history, THE System SHALL hiển thị commission_fee cho mỗi giao dịch
2. THE System SHALL hiển thị original_amount (số tiền gốc trước khi trừ phí) và net_amount (số tiền sau trừ phí)
3. WHEN Artisan xem chi tiết một transaction, THE System SHALL hiển thị commission_rate được áp dụng
4. THE System SHALL hiển thị commission_fee = 0 cho các giao dịch không bị trừ phí (nạp tiền, hoàn tiền)
5. THE System SHALL format commission_fee với 2 chữ số thập phân và ký hiệu tiền tệ VND

### Yêu Cầu 7

**User Story:** Là Artisan, tôi muốn nhận thông báo khi phí sàn được trừ từ giao dịch, để theo dõi thu nhập của mình.

#### Tiêu Chí Chấp Nhận

1. WHEN System trừ commission từ payment, THE System SHALL gửi notification cho Artisan
2. THE System SHALL bao gồm thông tin: order_id, original_amount, commission_amount, net_amount trong notification
3. THE System SHALL sử dụng notification_type = COMMISSION_DEDUCTED
4. WHEN Artisan click vào notification, THE System SHALL điều hướng đến wallet transaction detail
5. THE System SHALL gửi notification trong vòng 5 giây sau khi xử lý payment callback

### Yêu Cầu 8

**User Story:** Là Admin, tôi muốn xem báo cáo tổng hợp phí sàn thu được, để theo dõi doanh thu của nền tảng.

#### Tiêu Chí Chấp Nhận

1. THE System SHALL cung cấp endpoint GET /api/admin/commission/report với query params: startDate, endDate
2. WHEN Admin gọi endpoint, THE System SHALL tính tổng commission_amount từ wallet_transactions trong khoảng thời gian
3. THE System SHALL trả về: total_commission, total_transactions, average_commission_per_transaction
4. THE System SHALL group commission theo ngày/tuần/tháng dựa trên query param groupBy
5. THE System SHALL chỉ tính commission từ các transaction có commission_fee > 0

### Yêu Cầu 9

**User Story:** Là Developer, tôi muốn đảm bảo commission được tính chính xác trong mọi trường hợp, để tránh lỗi tài chính.

#### Tiêu Chí Chấp Nhận

1. WHEN commission_rate = 0, THE System SHALL không trừ commission và commission_amount = 0
2. WHEN payment bị refund, THE System SHALL hoàn lại TOÀN BỘ original_amount (bao gồm cả commission) cho Artisan
3. IF tính toán commission gặp lỗi, THE System SHALL rollback toàn bộ payment callback transaction
4. THE System SHALL log tất cả các lần tính commission với đầy đủ thông tin: payment_id, original_amount, rate, commission_amount, net_amount
5. THE System SHALL validate commission_amount >= 0 trước khi lưu vào database
6. THE System SHALL đảm bảo net_amount > 0 sau khi trừ commission

### Yêu Cầu 10

**User Story:** Là Artisan, tôi muốn xem commission rate hiện tại của hệ thống, để biết trước phí sẽ bị trừ.

#### Tiêu Chí Chấp Nhận

1. THE System SHALL cung cấp endpoint GET /api/commission/rate cho tất cả user đã đăng nhập
2. WHEN Artisan gọi endpoint, THE System SHALL trả về commission_rate hiện tại
3. THE System SHALL hiển thị commission_rate trên trang profile của Artisan
4. WHEN Admin thay đổi commission_rate, THE System SHALL gửi notification cho tất cả Artisan
5. THE System SHALL hiển thị commission_rate khi Artisan tạo product hoặc custom order

### Yêu Cầu 11

**User Story:** Là System, tôi muốn thêm các field cần thiết vào database để lưu trữ commission, giữ thiết kế đơn giản.

#### Tiêu Chí Chấp Nhận

1. THE System SHALL thêm field commission_rate (BigDecimal, nullable) vào bảng payments
2. THE System SHALL thêm field commission_rate (BigDecimal, nullable) vào bảng stage_payments
3. THE System SHALL thêm field commission_fee (BigDecimal, nullable, default 0) vào bảng wallet_transactions
4. THE System SHALL thêm field commission_rate (BigDecimal, nullable) vào bảng wallet_transactions
5. THE System SHALL tạo bảng system_config với columns: config_key (VARCHAR, PK), config_value (TEXT), updated_at, updated_by
6. THE System SHALL index commission_fee trong wallet_transactions để tối ưu query báo cáo
7. THE System SHALL KHÔNG tạo bảng commission_transactions riêng để giữ thiết kế đơn giản

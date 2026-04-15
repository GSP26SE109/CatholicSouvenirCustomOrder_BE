# Requirements Document - Artisan Withdrawal Feature

## Introduction

Tính năng rút tiền cho phép Artisan rút số dư từ ví của họ về tài khoản ngân hàng. Hệ thống yêu cầu Admin phê duyệt mỗi yêu cầu rút tiền để đảm bảo tính bảo mật và kiểm soát. Artisan có thể hủy yêu cầu rút tiền nếu chưa được xử lý.

## Glossary

- **Withdrawal System**: Hệ thống quản lý yêu cầu rút tiền của Artisan
- **Artisan**: Người thợ thủ công có tài khoản trên sàn và có số dư trong ví
- **Admin**: Quản trị viên có quyền phê duyệt hoặc từ chối yêu cầu rút tiền
- **Wallet**: Ví điện tử lưu trữ số dư của Artisan
- **Withdrawal Request**: Yêu cầu rút tiền được tạo bởi Artisan
- **Bank Account**: Tài khoản ngân hàng đích để nhận tiền
- **Minimum Withdrawal Amount**: Số tiền rút tối thiểu (50,000 VND)
- **Processing Fee**: Phí xử lý giao dịch rút tiền

## Requirements

### Requirement 1: Tạo yêu cầu rút tiền

**User Story:** Là một Artisan, tôi muốn tạo yêu cầu rút tiền từ ví của mình về tài khoản ngân hàng, để tôi có thể sử dụng số tiền đã kiếm được.

#### Acceptance Criteria

1. WHEN Artisan gửi yêu cầu rút tiền, THE Withdrawal_System SHALL xác thực số dư ví của Artisan lớn hơn hoặc bằng số tiền yêu cầu rút
2. WHEN Artisan gửi yêu cầu rút tiền, THE Withdrawal_System SHALL xác thực số tiền rút lớn hơn hoặc bằng 50,000 VND
3. WHEN Artisan gửi yêu cầu rút tiền, THE Withdrawal_System SHALL yêu cầu thông tin tài khoản ngân hàng bao gồm tên ngân hàng, số tài khoản, và tên chủ tài khoản
4. WHEN yêu cầu rút tiền được tạo thành công, THE Withdrawal_System SHALL tạo bản ghi với trạng thái PENDING
5. WHEN yêu cầu rút tiền được tạo thành công, THE Withdrawal_System SHALL gửi thông báo cho Admin để xem xét

### Requirement 2: Xem danh sách yêu cầu rút tiền

**User Story:** Là một Artisan, tôi muốn xem lịch sử các yêu cầu rút tiền của mình, để theo dõi trạng thái và lịch sử giao dịch.

#### Acceptance Criteria

1. WHEN Artisan truy cập danh sách yêu cầu rút tiền, THE Withdrawal_System SHALL hiển thị tất cả yêu cầu của Artisan đó theo thứ tự thời gian giảm dần
2. WHEN hiển thị danh sách yêu cầu, THE Withdrawal_System SHALL bao gồm thông tin số tiền, trạng thái, ngày tạo, và ngày xử lý
3. WHEN hiển thị danh sách yêu cầu, THE Withdrawal_System SHALL hỗ trợ lọc theo trạng thái (PENDING, APPROVED, REJECTED, CANCELLED)
4. WHEN hiển thị danh sách yêu cầu, THE Withdrawal_System SHALL hỗ trợ phân trang với tối đa 20 bản ghi mỗi trang

### Requirement 3: Hủy yêu cầu rút tiền

**User Story:** Là một Artisan, tôi muốn hủy yêu cầu rút tiền đang chờ xử lý, để tôi có thể thay đổi quyết định hoặc tạo yêu cầu mới với thông tin khác.

#### Acceptance Criteria

1. WHEN Artisan yêu cầu hủy một withdrawal request, THE Withdrawal_System SHALL xác thực yêu cầu đó thuộc về Artisan
2. WHEN Artisan yêu cầu hủy một withdrawal request, THE Withdrawal_System SHALL xác thực trạng thái của yêu cầu là PENDING
3. IF trạng thái không phải PENDING, THEN THE Withdrawal_System SHALL từ chối yêu cầu hủy với thông báo lỗi rõ ràng
4. WHEN yêu cầu hủy hợp lệ, THE Withdrawal_System SHALL cập nhật trạng thái thành CANCELLED
5. WHEN yêu cầu được hủy thành công, THE Withdrawal_System SHALL ghi log thời gian hủy và lý do

### Requirement 4: Admin xem danh sách yêu cầu rút tiền

**User Story:** Là một Admin, tôi muốn xem tất cả yêu cầu rút tiền từ các Artisan, để quản lý và xử lý các yêu cầu một cách hiệu quả.

#### Acceptance Criteria

1. WHEN Admin truy cập danh sách yêu cầu rút tiền, THE Withdrawal_System SHALL hiển thị tất cả yêu cầu từ mọi Artisan
2. WHEN hiển thị danh sách cho Admin, THE Withdrawal_System SHALL bao gồm thông tin Artisan, số tiền, thông tin ngân hàng, và trạng thái
3. WHEN hiển thị danh sách cho Admin, THE Withdrawal_System SHALL hỗ trợ lọc theo trạng thái và tìm kiếm theo tên Artisan
4. WHEN hiển thị danh sách cho Admin, THE Withdrawal_System SHALL ưu tiên hiển thị các yêu cầu PENDING ở đầu danh sách
5. WHEN hiển thị danh sách cho Admin, THE Withdrawal_System SHALL hỗ trợ phân trang với tối đa 50 bản ghi mỗi trang

### Requirement 5: Admin phê duyệt yêu cầu rút tiền

**User Story:** Là một Admin, tôi muốn phê duyệt yêu cầu rút tiền hợp lệ, để Artisan có thể nhận được tiền trong tài khoản ngân hàng của họ.

#### Acceptance Criteria

1. WHEN Admin phê duyệt một withdrawal request, THE Withdrawal_System SHALL xác thực trạng thái của yêu cầu là PENDING
2. WHEN Admin phê duyệt yêu cầu, THE Withdrawal_System SHALL xác thực lại số dư ví của Artisan vẫn đủ để rút
3. WHEN phê duyệt hợp lệ, THE Withdrawal_System SHALL trừ số tiền từ ví của Artisan
4. WHEN phê duyệt hợp lệ, THE Withdrawal_System SHALL tạo bản ghi WalletTransaction với type WITHDRAW
5. WHEN phê duyệt hợp lệ, THE Withdrawal_System SHALL cập nhật trạng thái yêu cầu thành APPROVED
6. WHEN phê duyệt hợp lệ, THE Withdrawal_System SHALL ghi nhận thời gian phê duyệt và Admin đã phê duyệt
7. WHEN phê duyệt thành công, THE Withdrawal_System SHALL gửi thông báo cho Artisan về việc yêu cầu đã được chấp nhận
8. IF số dư không đủ, THEN THE Withdrawal_System SHALL từ chối thao tác và thông báo lỗi cho Admin

### Requirement 6: Admin từ chối yêu cầu rút tiền

**User Story:** Là một Admin, tôi muốn từ chối yêu cầu rút tiền không hợp lệ hoặc đáng ngờ, để bảo vệ hệ thống khỏi gian lận.

#### Acceptance Criteria

1. WHEN Admin từ chối một withdrawal request, THE Withdrawal_System SHALL xác thực trạng thái của yêu cầu là PENDING
2. WHEN Admin từ chối yêu cầu, THE Withdrawal_System SHALL yêu cầu Admin nhập lý do từ chối
3. WHEN từ chối hợp lệ, THE Withdrawal_System SHALL cập nhật trạng thái yêu cầu thành REJECTED
4. WHEN từ chối hợp lệ, THE Withdrawal_System SHALL lưu lý do từ chối, thời gian và Admin đã từ chối
5. WHEN từ chối thành công, THE Withdrawal_System SHALL gửi thông báo cho Artisan kèm theo lý do từ chối
6. WHEN yêu cầu bị từ chối, THE Withdrawal_System SHALL không thay đổi số dư ví của Artisan

### Requirement 7: Xem chi tiết yêu cầu rút tiền

**User Story:** Là một Artisan hoặc Admin, tôi muốn xem chi tiết một yêu cầu rút tiền cụ thể, để kiểm tra thông tin đầy đủ và trạng thái xử lý.

#### Acceptance Criteria

1. WHEN người dùng yêu cầu xem chi tiết withdrawal request, THE Withdrawal_System SHALL xác thực quyền truy cập (Artisan chỉ xem của mình, Admin xem tất cả)
2. WHEN hiển thị chi tiết, THE Withdrawal_System SHALL bao gồm tất cả thông tin: số tiền, thông tin ngân hàng, trạng thái, thời gian tạo
3. WHERE yêu cầu đã được xử lý, THE Withdrawal_System SHALL hiển thị thêm thông tin người xử lý, thời gian xử lý, và lý do (nếu bị từ chối)
4. WHERE yêu cầu đã được phê duyệt, THE Withdrawal_System SHALL hiển thị thông tin giao dịch ví liên quan

### Requirement 8: Ràng buộc nghiệp vụ và bảo mật

**User Story:** Là hệ thống, tôi cần đảm bảo tính toàn vẹn dữ liệu và bảo mật trong quá trình rút tiền, để bảo vệ cả Artisan và nền tảng.

#### Acceptance Criteria

1. THE Withdrawal_System SHALL không cho phép Artisan tạo yêu cầu rút tiền mới khi đã có yêu cầu PENDING
2. THE Withdrawal_System SHALL sử dụng transaction database để đảm bảo tính nhất quán khi trừ tiền từ ví
3. THE Withdrawal_System SHALL ghi log đầy đủ mọi thao tác liên quan đến rút tiền (tạo, hủy, phê duyệt, từ chối)
4. THE Withdrawal_System SHALL mã hóa thông tin nhạy cảm như số tài khoản ngân hàng trong database
5. THE Withdrawal_System SHALL xác thực Artisan phải có role ARTISAN và tài khoản đã được xác minh
6. THE Withdrawal_System SHALL giới hạn số tiền rút tối đa là 50,000,000 VND mỗi lần
7. THE Withdrawal_System SHALL không cho phép số dư ví âm sau khi trừ tiền
8. WHILE một withdrawal request đang được xử lý bởi Admin, THE Withdrawal_System SHALL khóa bản ghi để tránh xử lý đồng thời

### Requirement 9: Thông báo và giao tiếp

**User Story:** Là người dùng hệ thống, tôi muốn nhận thông báo kịp thời về các thay đổi trạng thái yêu cầu rút tiền, để theo dõi tiến trình xử lý.

#### Acceptance Criteria

1. WHEN yêu cầu rút tiền được tạo, THE Withdrawal_System SHALL gửi thông báo cho Admin với priority HIGH
2. WHEN yêu cầu được phê duyệt, THE Withdrawal_System SHALL gửi thông báo cho Artisan với thông tin chi tiết
3. WHEN yêu cầu bị từ chối, THE Withdrawal_System SHALL gửi thông báo cho Artisan kèm lý do từ chối
4. WHEN Artisan hủy yêu cầu, THE Withdrawal_System SHALL gửi thông báo cho Admin để cập nhật
5. THE Withdrawal_System SHALL lưu trữ tất cả thông báo trong hệ thống notification hiện có

### Requirement 10: Báo cáo và thống kê

**User Story:** Là một Admin, tôi muốn xem thống kê về các giao dịch rút tiền, để giám sát hoạt động tài chính của nền tảng.

#### Acceptance Criteria

1. WHEN Admin truy cập báo cáo rút tiền, THE Withdrawal_System SHALL hiển thị tổng số tiền đã rút trong khoảng thời gian
2. WHEN Admin truy cập báo cáo, THE Withdrawal_System SHALL hiển thị số lượng yêu cầu theo từng trạng thái
3. WHEN Admin truy cập báo cáo, THE Withdrawal_System SHALL hỗ trợ lọc theo khoảng thời gian (ngày, tuần, tháng)
4. WHEN Admin truy cập báo cáo, THE Withdrawal_System SHALL hiển thị top Artisan có số tiền rút nhiều nhất

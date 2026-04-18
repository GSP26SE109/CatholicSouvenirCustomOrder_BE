# Requirements Document - Hệ Thống Hoàn Tiền

## Introduction

Hệ thống hoàn tiền cho phép khách hàng (Customer) gửi đơn khiếu nại (Complaint) khi nhận hàng không đúng yêu cầu hoặc có vấn đề về chất lượng. Admin sẽ xem xét và quyết định phê duyệt hoặc từ chối đơn khiếu nại. Khi được phê duyệt, hệ thống tự động hoàn tiền từ ví của Artisan về ví của Customer. Hệ thống này áp dụng cho cả 3 luồng đặt hàng: request-based, template-based và product-based.

## Business Rules

### BR1: Quy Tắc Về Trả Hàng và Phản Hồi Của Artisan

**Nguyên tắc:** Artisan có quyền xem đơn khiếu nại, phản hồi và quyết định có yêu cầu Customer trả hàng hay không. Admin sẽ xem xét cả hai phía trước khi đưa ra quyết định cuối cùng.

**Lý do nghiệp vụ:**
- Đảm bảo công bằng cho cả Customer và Artisan
- Artisan có cơ hội giải thích và cung cấp bằng chứng phản bác
- Admin đóng vai trò trọng tài, xem xét cả hai phía trước khi quyết định
- Với sản phẩm giá trị cao hoặc còn sử dụng được: Artisan có thể yêu cầu trả hàng để tái chế, sửa chữa hoặc bán lại
- Với sản phẩm giá trị thấp hoặc hỏng hoàn toàn: Artisan có thể không yêu cầu trả hàng để tiết kiệm chi phí vận chuyển

**Quy trình:**
1. Customer tạo đơn khiếu nại với lý do và hình ảnh bằng chứng → Artisan nhận thông báo
2. Artisan xem bằng chứng và có 2 lựa chọn:
   - **Phản hồi giải thích:** Cung cấp lý do tại sao sản phẩm không có vấn đề (ví dụ: xước nhỏ là do vận chuyển, sản phẩm thủ công nên có sai số nhỏ, v.v.)
   - **Quyết định trả hàng:** Chọn "Yêu cầu trả hàng" hoặc "Không cần trả hàng"
3. Admin xem xét:
   - Bằng chứng của Customer (hình ảnh, lý do)
   - Phản hồi của Artisan (giải thích, bằng chứng phản bác nếu có)
   - Quyết định trả hàng của Artisan
4. Admin đưa ra quyết định:
   - **Từ chối (REJECTED):** Nếu thấy khiếu nại không hợp lý (ví dụ: xước nhỏ là bình thường, Customer kỳ vọng quá cao)
   - **Phê duyệt (APPROVED):** Nếu thấy khiếu nại hợp lý
5. Nếu Admin phê duyệt:
   - **Không cần trả hàng (requireReturn = false):** Hoàn tiền ngay lập tức
   - **Yêu cầu trả hàng (requireReturn = true):** Chuyển sang trạng thái WAITING_RETURN, Customer phải gửi hàng về, Artisan xác nhận nhận hàng, sau đó mới hoàn tiền

**Bảo vệ quyền lợi Artisan:**
- Artisan được xem đơn khiếu nại và phản hồi trước khi Admin quyết định
- Admin phải xem xét cả hai phía, không tự động tin Customer
- Nếu khiếu nại không hợp lý (xước nhỏ, sai số chấp nhận được), Admin sẽ từ chối
- Artisan có thể cung cấp bằng chứng như: ảnh sản phẩm trước khi gửi, tiêu chuẩn chất lượng thủ công, chính sách về sai số cho phép

### BR2: Quy Tắc Về Thời Gian Khiếu Nại

**Nguyên tắc:** Customer chỉ có thể khiếu nại trong vòng 7 ngày kể từ khi đơn hàng được giao thành công.

**Lý do nghiệp vụ:**
- Đảm bảo Customer có đủ thời gian kiểm tra sản phẩm
- Tránh trường hợp khiếu nại quá muộn khi sản phẩm đã bị hư hỏng do sử dụng
- Phù hợp với thông lệ thương mại điện tử tại Việt Nam

**Điều kiện:**
- Đơn hàng phải có trạng thái DELIVERED
- Thời gian tính từ deliveredDate đến thời điểm tạo complaint không quá 7 ngày

### BR3: Quy Tắc Về Số Tiền Hoàn

**Nguyên tắc:** Số tiền hoàn không bao gồm phí platform (10%) đã thu khi thanh toán ban đầu.

**Lý do nghiệp vụ:**
- Phí platform là chi phí dịch vụ cho việc kết nối Customer và Artisan
- Dù có hoàn tiền, platform vẫn đã cung cấp dịch vụ (hệ thống, thanh toán, vận chuyển)
- Đây là thông lệ phổ biến của các sàn thương mại điện tử

**Công thức:**
- Số tiền Customer đã trả = 100%
- Số tiền Artisan nhận được = 90% (đã trừ 10% phí platform)
- Số tiền hoàn tối đa = 90% (trừ từ ví Artisan)
- Platform giữ lại 10% phí dịch vụ

**Ví dụ:**
- Customer trả: 1,000,000 VND
- Artisan nhận: 900,000 VND (platform giữ 100,000 VND)
- Khi hoàn tiền: Customer nhận tối đa 900,000 VND (từ ví Artisan)
- Platform không hoàn lại 100,000 VND phí dịch vụ

### BR4: Quy Tắc Về Xử Lý Ví Không Đủ Số Dư

**Nguyên tắc:** Nếu ví Artisan không đủ số dư để hoàn tiền, hệ thống sẽ đánh dấu giao dịch FAILED và thông báo Admin xử lý thủ công.

**Lý do nghiệp vụ:**
- Artisan có thể đã rút tiền hoặc chi tiêu hết số dư trong ví
- Không thể ép buộc hoàn tiền tự động nếu không có tiền
- Cần sự can thiệp của Admin để liên hệ Artisan hoặc xử lý theo chính sách công ty

**Quy trình xử lý:**
1. Hệ thống kiểm tra số dư ví Artisan
2. Nếu không đủ: Đánh dấu RefundTransaction là FAILED
3. Gửi thông báo khẩn cấp đến Admin
4. Admin liên hệ Artisan để yêu cầu nạp tiền hoặc chuyển khoản trực tiếp
5. Admin có thể retry hoàn tiền sau khi Artisan nạp tiền vào ví

### BR5: Quy Tắc Về Một Đơn Hàng Một Khiếu Nại

**Nguyên tắc:** Mỗi đơn hàng chỉ được phép tạo một đơn khiếu nại duy nhất.

**Lý do nghiệp vụ:**
- Tránh spam và lạm dụng hệ thống
- Đảm bảo quy trình xử lý rõ ràng, không bị trùng lặp
- Customer cần cân nhắc kỹ trước khi gửi khiếu nại

**Xử lý:**
- Hệ thống kiểm tra xem đơn hàng đã có complaint chưa
- Nếu đã có (bất kể trạng thái): Từ chối tạo mới và thông báo lỗi
- Customer có thể xem lại complaint cũ và theo dõi tiến trình

### BR6: Quy Tắc Về Tính Bất Biến Của Quyết Định

**Nguyên tắc:** Sau khi Admin đã phê duyệt (APPROVED) hoặc từ chối (REJECTED) đơn khiếu nại, không thể thay đổi quyết định.

**Lý do nghiệp vụ:**
- Đảm bảo tính minh bạch và công bằng
- Tránh thao túng kết quả sau khi đã xử lý
- Nếu có sai sót, cần tạo quy trình khiếu nại mới hoặc xử lý ngoại lệ riêng

**Xử lý:**
- Hệ thống khóa không cho phép cập nhật status nếu đã là APPROVED hoặc REJECTED
- Nếu cần thay đổi: Admin phải có quyền đặc biệt và ghi log đầy đủ lý do

### BR7: Quy Tắc Về Xác Định Artisan Cho Các Loại Đơn Hàng

**Nguyên tắc:** Hệ thống tự động xác định Artisan liên quan dựa trên loại đơn hàng.

**Lý do nghiệp vụ:**
- Đảm bảo hoàn tiền đúng người chịu trách nhiệm
- Hỗ trợ cả 3 luồng đặt hàng khác nhau

**Quy tắc xác định:**
- **CustomOrder (request-based):** Lấy từ trường `customOrder.artisan`
- **Order với OrderDetail (product-based):** Lấy từ `orderDetail.product.artisan`
- **Order với OrderTemplateDetail (template-based):** Lấy từ `orderTemplateDetail.productTemplate.artisan`

**Lưu ý:** Một Order có thể có nhiều OrderDetail từ nhiều Artisan khác nhau. Trong trường hợp này, Customer phải chỉ định rõ sản phẩm nào bị khiếu nại, và hệ thống chỉ hoàn tiền cho Artisan của sản phẩm đó.

## Glossary

- **System**: Hệ thống quản lý đơn hàng và hoàn tiền Catholic Souvenir Custom Order
- **Customer**: Khách hàng đặt mua sản phẩm
- **Artisan**: Nghệ nhân thực hiện đơn hàng
- **Admin**: Quản trị viên hệ thống
- **Complaint**: Đơn khiếu nại của khách hàng về sản phẩm
- **Refund**: Giao dịch hoàn tiền
- **Wallet**: Ví điện tử của người dùng trong hệ thống
- **Order**: Đơn hàng thông thường (product/template-based)
- **CustomOrder**: Đơn hàng tùy chỉnh (request-based)
- **ComplaintStatus**: Trạng thái của đơn khiếu nại (PENDING, APPROVED, REJECTED, WAITING_RETURN)
- **RefundStatus**: Trạng thái của giao dịch hoàn tiền (PENDING, COMPLETED, FAILED)
- **Shipment**: Đơn vận chuyển (có thể là giao hàng thường hoặc trả hàng, phân biệt bằng field isReturn)
- **RequireReturn**: Yêu cầu trả hàng (true/false) do Artisan quyết định

## Requirements

### Requirement 1: Tạo Đơn Khiếu Nại

**User Story:** Là một Customer, tôi muốn gửi đơn khiếu nại khi nhận hàng không đúng yêu cầu, để có thể được xem xét hoàn tiền

#### Acceptance Criteria

1. WHEN Customer nhận hàng và phát hiện vấn đề, THE System SHALL cho phép Customer tạo đơn khiếu nại cho đơn hàng đã giao
2. THE System SHALL yêu cầu Customer cung cấp lý do khiếu nại với độ dài từ 20 đến 1000 ký tự
3. THE System SHALL cho phép Customer tải lên tối đa 5 hình ảnh bằng chứng với kích thước mỗi file không vượt quá 5MB
4. THE System SHALL chỉ cho phép tạo đơn khiếu nại cho đơn hàng có trạng thái DELIVERED
5. THE System SHALL chỉ cho phép tạo đơn khiếu nại trong vòng 7 ngày kể từ ngày giao hàng thành công
6. WHEN đơn khiếu nại được tạo thành công, THE System SHALL đặt trạng thái ban đầu là PENDING
7. WHEN đơn khiếu nại được tạo thành công, THE System SHALL gửi thông báo đến Artisan liên quan

### Requirement 2: Artisan Phản Hồi và Quyết Định Trả Hàng

**User Story:** Là một Artisan, tôi muốn được thông báo khi có đơn khiếu nại về sản phẩm của mình và có cơ hội giải thích/phản bác, để bảo vệ quyền lợi của mình trước khi Admin quyết định

#### Acceptance Criteria

1. WHEN đơn khiếu nại được tạo, THE System SHALL gửi thông báo đến Artisan qua hệ thống notification hiện có
2. THE System SHALL cho phép Artisan xem chi tiết đơn khiếu nại bao gồm lý do và hình ảnh bằng chứng của Customer
3. THE System SHALL cho phép Artisan gửi phản hồi giải thích với độ dài từ 20 đến 1000 ký tự
4. THE System SHALL cho phép Artisan chọn "Yêu cầu trả hàng" hoặc "Không cần trả hàng"
5. WHEN Artisan chọn "Yêu cầu trả hàng", THE System SHALL lưu trường requireReturn là true
6. WHEN Artisan chọn "Không cần trả hàng", THE System SHALL lưu trường requireReturn là false
7. THE System SHALL lưu trữ thời gian phản hồi của Artisan để Admin tham khảo
8. THE System SHALL cho phép Admin xem phản hồi của Artisan trước khi đưa ra quyết định

**Ghi chú:** Artisan có thể giải thích các trường hợp như: xước nhỏ do vận chuyển, sai số chấp nhận được của sản phẩm thủ công, Customer kỳ vọng không thực tế, v.v.

### Requirement 3: Xem Xét và Phê Duyệt Đơn Khiếu Nại

**User Story:** Là một Admin, tôi muốn xem xét đơn khiếu nại và quyết định phê duyệt hoặc từ chối, để đảm bảo công bằng cho cả Customer và Artisan

#### Acceptance Criteria

1. THE System SHALL hiển thị danh sách tất cả đơn khiếu nại với khả năng lọc theo trạng thái và ngày tạo
2. THE System SHALL cho phép Admin xem chi tiết đơn khiếu nại bao gồm thông tin Customer, Artisan, đơn hàng, lý do và hình ảnh
3. THE System SHALL hiển thị phản hồi của Artisan và quyết định requireReturn (có/không) trong chi tiết đơn khiếu nại
4. WHEN Admin phê duyệt đơn khiếu nại, THE System SHALL yêu cầu Admin nhập số tiền hoàn với giá trị từ 1 đến tổng giá trị đơn hàng
5. WHEN Admin phê duyệt đơn khiếu nại, THE System SHALL yêu cầu Admin nhập ghi chú với độ dài từ 10 đến 500 ký tự
6. WHEN Admin từ chối đơn khiếu nại, THE System SHALL yêu cầu Admin nhập lý do từ chối với độ dài từ 20 đến 500 ký tự
7. THE System SHALL chỉ cho phép Admin xử lý đơn khiếu nại có trạng thái PENDING

### Requirement 4: Xử Lý Hoàn Tiền Theo Yêu Cầu Trả Hàng

**User Story:** Là một Customer, tôi muốn nhận tiền hoàn tự động vào ví khi đơn khiếu nại được phê duyệt, để không phải thực hiện thêm thao tác nào

#### Acceptance Criteria

1. WHEN Admin phê duyệt đơn khiếu nại AND requireReturn là false, THE System SHALL tự động tạo giao dịch hoàn tiền với trạng thái PENDING
2. WHEN Admin phê duyệt đơn khiếu nại AND requireReturn là true, THE System SHALL cập nhật trạng thái đơn khiếu nại thành WAITING_RETURN
3. WHEN giao dịch hoàn tiền được tạo, THE System SHALL kiểm tra số dư ví của Artisan có đủ để hoàn tiền
4. IF số dư ví Artisan đủ, THEN THE System SHALL trừ số tiền hoàn từ ví Artisan
5. WHEN trừ tiền thành công từ ví Artisan, THE System SHALL cộng số tiền hoàn vào ví Customer
6. WHEN cả hai giao dịch hoàn tất, THE System SHALL cập nhật trạng thái giao dịch hoàn tiền thành COMPLETED
7. IF số dư ví Artisan không đủ, THEN THE System SHALL đặt trạng thái giao dịch hoàn tiền là FAILED và ghi lại lý do
8. WHEN giao dịch hoàn tiền hoàn tất, THE System SHALL cập nhật trạng thái đơn khiếu nại thành APPROVED

### Requirement 5: Xử Lý Trả Hàng (Optional - Có thể đơn giản hóa)

**User Story:** Là một Customer, tôi muốn gửi hàng trả lại cho Artisan khi được yêu cầu, để hoàn tất quy trình hoàn tiền

#### Acceptance Criteria

1. WHEN đơn khiếu nại có trạng thái WAITING_RETURN, THE System SHALL hiển thị thông tin địa chỉ trả hàng của Artisan cho Customer
2. THE System SHALL cho phép Customer tạo đơn trả hàng với thông tin vận chuyển
3. WHEN Customer tạo đơn trả hàng, THE System SHALL lưu mã vận đơn và thông tin vận chuyển
4. THE System SHALL cho phép Artisan xác nhận đã nhận hàng trả về
5. WHEN Artisan xác nhận nhận hàng, THE System SHALL tự động kích hoạt quy trình hoàn tiền như Requirement 4
6. THE System SHALL cập nhật trạng thái đơn khiếu nại từ WAITING_RETURN sang PROCESSING_REFUND khi Artisan xác nhận nhận hàng

**Ghi chú:** Tính năng này có thể đơn giản hóa bằng cách chỉ lưu mã vận đơn và để Admin/Artisan tự theo dõi offline, sau đó Admin confirm để trigger hoàn tiền.

### Requirement 6: Thông Báo Kết Quả Xử Lý

**User Story:** Là một Customer hoặc Artisan, tôi muốn được thông báo về kết quả xử lý đơn khiếu nại, để biết được tình trạng của đơn khiếu nại

#### Acceptance Criteria

1. WHEN đơn khiếu nại được phê duyệt, THE System SHALL gửi thông báo đến Customer qua hệ thống notification hiện có
2. WHEN đơn khiếu nại được phê duyệt, THE System SHALL gửi thông báo đến Artisan qua hệ thống notification hiện có
3. WHEN đơn khiếu nại bị từ chối, THE System SHALL gửi thông báo đến Customer qua hệ thống notification hiện có
4. THE System SHALL bao gồm thông tin số tiền hoàn (nếu có) và ghi chú của Admin trong thông báo

**Ghi chú:** Sử dụng hệ thống notification hiện có thay vì WebSocket và email riêng để tiết kiệm thời gian implement.

### Requirement 7: Xem Chi Tiết Đơn Khiếu Nại

**User Story:** Là một Customer, tôi muốn xem chi tiết đơn khiếu nại của mình, để theo dõi tiến trình xử lý và kết quả

#### Acceptance Criteria

1. THE System SHALL cho phép Customer xem danh sách tất cả đơn khiếu nại của mình
2. THE System SHALL hiển thị chi tiết đơn khiếu nại bao gồm thông tin đơn hàng, lý do, hình ảnh bằng chứng, trạng thái và thời gian tạo
3. WHEN Artisan đã phản hồi, THE System SHALL hiển thị phản hồi của Artisan cho Customer
4. WHEN đơn khiếu nại được xử lý, THE System SHALL hiển thị kết quả (phê duyệt hoặc từ chối), số tiền hoàn (nếu có) và ghi chú của Admin
5. THE System SHALL hiển thị thời gian xử lý của Admin trong chi tiết đơn khiếu nại

### Requirement 8: Quản Lý Đơn Khiếu Nại Của Artisan

**User Story:** Là một Artisan, tôi muốn xem danh sách và chi tiết các đơn khiếu nại liên quan đến sản phẩm của mình, để theo dõi và phản hồi kịp thời

#### Acceptance Criteria

1. THE System SHALL cho phép Artisan xem danh sách tất cả đơn khiếu nại liên quan đến sản phẩm của mình
2. THE System SHALL hiển thị chi tiết đơn khiếu nại bao gồm thông tin đơn hàng, Customer, lý do và hình ảnh bằng chứng của Customer
3. WHEN đơn khiếu nại được xử lý, THE System SHALL hiển thị kết quả và ghi chú của Admin cho Artisan

**Ghi chú:** Artisan cần có tính năng này để phản hồi và quyết định có yêu cầu trả hàng hay không.

### Requirement 9: Lịch Sử và Báo Cáo (Optional - Có thể bỏ qua trong MVP)

**User Story:** Là một Admin, tôi muốn xem báo cáo về các đơn khiếu nại và hoàn tiền, để theo dõi tình hình và phát hiện vấn đề

#### Acceptance Criteria

1. THE System SHALL cho phép Admin xem danh sách tất cả giao dịch hoàn tiền
2. THE System SHALL lưu trữ đầy đủ audit trail cho mọi thay đổi trạng thái của đơn khiếu nại

**Ghi chú:** Các tính năng báo cáo phức tạp (thống kê, xuất file) có thể bỏ qua trong MVP. Chỉ cần list view cơ bản.

### Requirement 10: Xử Lý Cho Các Loại Đơn Hàng

**User Story:** Là một Customer, tôi muốn có thể khiếu nại cho bất kỳ loại đơn hàng nào, để được bảo vệ quyền lợi trong mọi trường hợp

#### Acceptance Criteria

1. THE System SHALL hỗ trợ tạo đơn khiếu nại cho đơn hàng thông thường (Order) từ product-based hoặc template-based
2. THE System SHALL hỗ trợ tạo đơn khiếu nại cho đơn hàng tùy chỉnh (CustomOrder) từ request-based
3. WHEN hoàn tiền cho Order, THE System SHALL xác định Artisan dựa trên OrderDetail hoặc OrderTemplateDetail
4. WHEN hoàn tiền cho CustomOrder, THE System SHALL xác định Artisan từ trường artisan của CustomOrder
5. THE System SHALL tính toán số tiền hoàn dựa trên giá trị thực tế mà Customer đã thanh toán
6. THE System SHALL không hoàn lại phí platform (10%) đã thu khi thanh toán ban đầu

### Requirement 11: Xử Lý Trường Hợp Đặc Biệt

**User Story:** Là một Admin, tôi muốn hệ thống xử lý các trường hợp đặc biệt một cách an toàn, để tránh lỗi và mất mát dữ liệu

#### Acceptance Criteria

1. IF giao dịch hoàn tiền thất bại do lỗi hệ thống, THEN THE System SHALL ghi log chi tiết và giữ nguyên trạng thái đơn khiếu nại là PENDING
2. THE System SHALL không cho phép tạo nhiều đơn khiếu nại cho cùng một đơn hàng
3. IF đơn khiếu nại đã được xử lý (APPROVED hoặc REJECTED), THEN THE System SHALL không cho phép thay đổi trạng thái
4. WHEN xử lý giao dịch hoàn tiền, THE System SHALL sử dụng transaction để đảm bảo tính toàn vẹn dữ liệu
5. IF ví Artisan không đủ số dư, THEN THE System SHALL gửi thông báo đến Admin để xử lý thủ công
6. THE System SHALL ghi log đầy đủ cho mọi thao tác liên quan đến hoàn tiền để audit

# Tài liệu Yêu cầu

## Giới thiệu

Tài liệu này mô tả các yêu cầu cho tính năng Thống kê Dashboard dành cho Artisan trong hệ thống Catholic Souvenir Custom Order. Tính năng này cung cấp cho artisan các phân tích và thông tin chi tiết về hiệu suất kinh doanh của họ, bao gồm chi tiết tài chính, thống kê đơn hàng custom, chỉ số hiệu suất, phân tích khách hàng và hiệu suất template. Dashboard này giúp artisan theo dõi thu nhập, đánh giá sự hài lòng của khách hàng, phân tích tỷ lệ chuyển đổi đơn hàng custom và đưa ra quyết định kinh doanh dựa trên dữ liệu.

## Thuật ngữ

- **Dashboard_System**: Dịch vụ backend tổng hợp và cung cấp dữ liệu thống kê cho người dùng artisan
- **Artisan**: Người dùng đã xác minh với vai trò ARTISAN, tạo và bán sản phẩm trên nền tảng
- **Custom_Request**: Yêu cầu của khách hàng cho sản phẩm custom có thể được chuyển thành Custom_Order
- **Custom_Order**: Đơn hàng đã xác nhận cho sản phẩm custom với các giai đoạn và thanh toán đã định
- **Wallet**: Hệ thống số dư tài khoản theo dõi thu nhập và giao dịch của artisan
- **Commission**: Phí nền tảng được khấu trừ từ thu nhập của artisan trên mỗi giao dịch
- **Feedback**: Đánh giá và nhận xét của khách hàng cho đơn hàng đã hoàn thành
- **Template**: Mẫu sản phẩm được thiết kế sẵn do artisan tạo ra mà khách hàng có thể tùy chỉnh
- **Conversion_Rate**: Tỷ lệ phần trăm Custom_Request được chuyển đổi thành Custom_Order
- **Gross_Earnings**: Tổng doanh thu trước khi khấu trừ hoa hồng
- **Net_Earnings**: Doanh thu sau khi khấu trừ hoa hồng
- **Withdrawal_Request**: Yêu cầu của artisan chuyển tiền từ ví sang tài khoản ngân hàng

## Yêu cầu

### Yêu cầu 1: Thống kê Chi tiết Tài chính

**User Story:** Là một artisan, tôi muốn xem thống kê tài chính bao gồm tổng thu nhập, hoa hồng đã trả, thu nhập ròng, số tiền rút đang chờ và xu hướng số dư ví, để tôi có thể hiểu rõ doanh thu và quản lý tài chính hiệu quả.

#### Tiêu chí chấp nhận

1. WHEN một artisan đã xác thực yêu cầu chi tiết tài chính cho một khoảng thời gian, THE Dashboard_System SHALL tính toán và trả về tổng thu nhập gộp từ tất cả đơn hàng và custom order đã hoàn thành trong khoảng thời gian đó
2. WHEN tính toán thu nhập gộp, THE Dashboard_System SHALL tổng hợp tổng giá từ các Order entity mà artisan sở hữu sản phẩm và trạng thái đơn hàng là DELIVERED, cộng với tổng giá từ các Custom_Order entity mà artisan được chỉ định và trạng thái là COMPLETED
3. WHEN một artisan đã xác thực yêu cầu chi tiết tài chính, THE Dashboard_System SHALL tính tổng hoa hồng đã trả bằng cách tổng hợp trường commission_fee từ tất cả WalletTransaction entity liên kết với ví của artisan trong khoảng thời gian chỉ định
4. WHEN tính toán thu nhập ròng, THE Dashboard_System SHALL trừ tổng hoa hồng đã trả từ thu nhập gộp
5. WHEN một artisan đã xác thực yêu cầu chi tiết tài chính, THE Dashboard_System SHALL lấy số tiền rút đang chờ bằng cách tổng hợp trường amount từ tất cả WithdrawalRequest entity mà artisan là người yêu cầu và trạng thái là PENDING
6. WHEN một artisan đã xác thực yêu cầu xu hướng số dư ví, THE Dashboard_System SHALL trả về tập dữ liệu chuỗi thời gian của ảnh chụp số dư ví hàng ngày cho khoảng thời gian chỉ định
7. WHEN tính toán xu hướng số dư ví, THE Dashboard_System SHALL tổng hợp các WalletTransaction entity theo ngày và tính số dư tích lũy cho mỗi ngày
8. IF artisan không có giao dịch nào trong khoảng thời gian chỉ định, THEN THE Dashboard_System SHALL trả về giá trị zero cho tất cả các chỉ số tài chính

### Yêu cầu 2: Thống kê Custom Order

**User Story:** Là một artisan, tôi muốn xem thống kê về custom order bao gồm tổng số yêu cầu nhận được, tỷ lệ chấp nhận báo giá, giá trị trung bình custom order, thời gian hoàn thành và yêu cầu đang chờ, để tôi có thể đánh giá hiệu suất kinh doanh custom order của mình.

#### Tiêu chí chấp nhận

1. WHEN một artisan đã xác thực yêu cầu thống kê custom order cho một khoảng thời gian, THE Dashboard_System SHALL đếm tổng số Custom_Request entity mà artisan là selected_artisan và created_at nằm trong khoảng thời gian chỉ định
2. WHEN tính toán tỷ lệ chấp nhận báo giá, THE Dashboard_System SHALL chia số lượng Custom_Request entity có Custom_Order liên kết cho tổng số Custom_Request entity mà artisan được chọn, sau đó nhân với 100 để có phần trăm
3. WHEN một artisan đã xác thực yêu cầu giá trị trung bình custom order, THE Dashboard_System SHALL tính trung bình của trường total_price từ tất cả Custom_Order entity mà artisan được chỉ định và created_at nằm trong khoảng thời gian chỉ định
4. WHEN tính toán thời gian hoàn thành trung bình, THE Dashboard_System SHALL tính thời lượng trung bình tính bằng ngày giữa timestamp created_at và completed_at cho tất cả Custom_Order entity mà artisan được chỉ định và trạng thái là COMPLETED
5. WHEN một artisan đã xác thực yêu cầu số lượng custom request đang chờ, THE Dashboard_System SHALL đếm các Custom_Request entity mà artisan là selected_artisan và trạng thái là PENDING hoặc QUOTED
6. IF artisan không có custom request nào trong khoảng thời gian chỉ định, THEN THE Dashboard_System SHALL trả về zero cho tổng số yêu cầu và yêu cầu đang chờ, và null cho tỷ lệ chấp nhận, giá trị trung bình và thời gian hoàn thành

### Yêu cầu 3: Chỉ số Hiệu suất

**User Story:** Là một artisan, tôi muốn xem các chỉ số hiệu suất bao gồm đánh giá trung bình, tổng số đánh giá, thời gian phản hồi, tỷ lệ hoàn thành đơn hàng, tỷ lệ khiếu nại và tỷ lệ giao hàng đúng hạn, để tôi có thể đánh giá và cải thiện chất lượng dịch vụ của mình.

#### Tiêu chí chấp nhận

1. WHEN một artisan đã xác thực yêu cầu chỉ số hiệu suất, THE Dashboard_System SHALL tính đánh giá trung bình bằng cách tính trung bình của trường rating từ tất cả Feedback entity mà artisan là người nhận
2. WHEN một artisan đã xác thực yêu cầu tổng số đánh giá, THE Dashboard_System SHALL đếm tất cả Feedback entity mà artisan là người nhận
3. WHEN tính toán thời gian phản hồi, THE Dashboard_System SHALL tính thời lượng trung bình tính bằng giờ giữa Custom_Request created_at và ChatMessage đầu tiên được gửi bởi artisan trong Conversation liên kết cho tất cả Custom_Request entity mà artisan được chọn
4. WHEN một artisan đã xác thực yêu cầu tỷ lệ hoàn thành đơn hàng, THE Dashboard_System SHALL chia số lượng Order entity với trạng thái DELIVERED cho tổng số Order entity (loại trừ trạng thái CANCELLED) mà artisan sở hữu sản phẩm, sau đó nhân với 100
5. WHEN tính toán tỷ lệ khiếu nại, THE Dashboard_System SHALL chia số lượng Complaint entity liên quan đến đơn hàng của artisan cho tổng số đơn hàng đã hoàn thành, sau đó nhân với 100
6. WHEN tính toán tỷ lệ giao hàng đúng hạn, THE Dashboard_System SHALL chia số lượng Order entity mà actual_delivery_date nhỏ hơn hoặc bằng expected_delivery_date cho tổng số đơn hàng đã giao, sau đó nhân với 100
7. IF artisan không có bản ghi feedback nào, THEN THE Dashboard_System SHALL trả về null cho đánh giá trung bình và zero cho tổng số đánh giá
8. IF artisan không có custom request nào với phản hồi, THEN THE Dashboard_System SHALL trả về null cho thời gian phản hồi

### Yêu cầu 4: Phân tích Khách hàng

**User Story:** Là một artisan, tôi muốn xem phân tích về khách hàng bao gồm tổng số khách hàng đã phục vụ, tỷ lệ khách hàng quay lại, điểm hài lòng của khách hàng và khách hàng hàng đầu, để tôi có thể hiểu rõ cơ sở khách hàng và xác định khách hàng trung thành.

#### Tiêu chí chấp nhận

1. WHEN một artisan đã xác thực yêu cầu phân tích khách hàng cho một khoảng thời gian, THE Dashboard_System SHALL đếm số lượng Account entity khách hàng riêng biệt đã đặt Order entity hoặc Custom_Order entity với artisan trong khoảng thời gian chỉ định
2. WHEN tính toán tỷ lệ khách hàng quay lại, THE Dashboard_System SHALL chia số lượng khách hàng đã đặt nhiều hơn một đơn hàng với artisan cho tổng số khách hàng đã phục vụ, sau đó nhân với 100
3. WHEN một artisan đã xác thực yêu cầu điểm hài lòng của khách hàng, THE Dashboard_System SHALL tính trung bình của trường rating từ tất cả Feedback entity mà artisan là người nhận, biểu thị dưới dạng phần trăm của đánh giá tối đa (5 sao)
4. WHEN một artisan đã xác thực yêu cầu danh sách khách hàng hàng đầu, THE Dashboard_System SHALL trả về top 10 khách hàng được xếp hạng theo tổng chi tiêu cho sản phẩm và custom order của artisan
5. WHEN lấy khách hàng hàng đầu, THE Dashboard_System SHALL bao gồm tên khách hàng, email, tổng số đơn hàng và tổng số tiền đã chi tiêu cho mỗi khách hàng
6. IF artisan không phục vụ khách hàng nào trong khoảng thời gian chỉ định, THEN THE Dashboard_System SHALL trả về zero cho tổng số khách hàng và null cho tỷ lệ khách hàng quay lại và điểm hài lòng

### Yêu cầu 5: Hiệu suất Template

**User Story:** Là một artisan, tôi muốn xem thống kê về product template bao gồm tổng số template đã tạo, template phổ biến nhất, tỷ lệ chuyển đổi template và doanh thu theo template, để tôi có thể tối ưu hóa các template của mình.

#### Tiêu chí chấp nhận

1. WHEN một artisan đã xác thực yêu cầu thống kê hiệu suất template, THE Dashboard_System SHALL đếm tổng số ProductTemplate entity mà artisan là người tạo
2. WHEN một artisan đã xác thực yêu cầu template phổ biến nhất, THE Dashboard_System SHALL trả về top 10 ProductTemplate entity được xếp hạng theo số lượng OrderTemplateDetail entity liên kết
3. WHEN lấy template phổ biến nhất, THE Dashboard_System SHALL bao gồm tên template, tổng số đơn hàng và tổng doanh thu cho mỗi template
4. WHEN tính toán tỷ lệ chuyển đổi template, THE Dashboard_System SHALL chia số lượng ProductTemplate entity có ít nhất một OrderTemplateDetail liên kết cho tổng số ProductTemplate entity được tạo bởi artisan, sau đó nhân với 100
5. WHEN một artisan đã xác thực yêu cầu doanh thu theo template cho một khoảng thời gian, THE Dashboard_System SHALL tổng hợp tổng giá từ tất cả OrderTemplateDetail entity được nhóm theo ProductTemplate mà artisan là người tạo template và order created_at nằm trong khoảng thời gian chỉ định
6. IF artisan không có template nào, THEN THE Dashboard_System SHALL trả về zero cho tổng số template và danh sách rỗng cho template phổ biến và phân tích doanh thu

### Yêu cầu 6: API Endpoint Dashboard

**User Story:** Là một artisan, tôi muốn truy cập thống kê dashboard của mình thông qua một API endpoint duy nhất với khoảng thời gian có thể cấu hình, để tôi có thể lấy tất cả các chỉ số liên quan một cách hiệu quả.

#### Tiêu chí chấp nhận

1. THE Dashboard_System SHALL cung cấp REST API endpoint tại GET /api/artisan/dashboard chấp nhận tham số query "days" để chỉ định khoảng thời gian
2. WHEN một artisan thực hiện yêu cầu đến dashboard endpoint, THE Dashboard_System SHALL xác minh rằng người dùng đã xác thực có vai trò ARTISAN
3. IF người dùng đã xác thực không có vai trò ARTISAN, THEN THE Dashboard_System SHALL trả về HTTP status 403 với thông báo lỗi "Bạn không có quyền truy cập"
4. WHEN một artisan hợp lệ yêu cầu dashboard với tham số "days", THE Dashboard_System SHALL tính ngày bắt đầu là ngày hiện tại trừ đi số ngày chỉ định
5. WHEN dashboard endpoint được gọi, THE Dashboard_System SHALL tổng hợp và trả về tất cả thống kê bao gồm chi tiết tài chính, thống kê custom order, chỉ số hiệu suất, phân tích khách hàng và hiệu suất template trong một response duy nhất
6. THE Dashboard_System SHALL trả về response ở định dạng JSON với cấu trúc chứa tất cả dữ liệu thống kê được tổ chức theo danh mục
7. WHEN tham số "days" không được cung cấp, THE Dashboard_System SHALL mặc định là 30 ngày
8. THE Dashboard_System SHALL hoàn thành yêu cầu dashboard và trả về response trong vòng 5 giây trong điều kiện tải bình thường

### Yêu cầu 7: Độ chính xác và Tính nhất quán của Dữ liệu

**User Story:** Là một artisan, tôi muốn thống kê dashboard chính xác và nhất quán với dữ liệu thực tế trong hệ thống, để tôi có thể tin tưởng các chỉ số khi đưa ra quyết định kinh doanh.

#### Tiêu chí chấp nhận

1. WHEN tính toán bất kỳ chỉ số tài chính nào, THE Dashboard_System SHALL sử dụng kiểu dữ liệu BigDecimal với độ chính xác 18 chữ số và scale 2 chữ số thập phân để tránh lỗi làm tròn
2. WHEN tổng hợp dữ liệu qua nhiều entity, THE Dashboard_System SHALL sử dụng các query cấp database với các thao tác JOIN phù hợp để đảm bảo tính nhất quán của dữ liệu
3. WHEN tính toán các chỉ số dựa trên thời gian, THE Dashboard_System SHALL sử dụng múi giờ hệ thống của server một cách nhất quán trong tất cả các phép tính
4. THE Dashboard_System SHALL loại trừ các bản ghi đã xóa mềm hoặc đã hủy khỏi tất cả các phép tính thống kê
5. WHEN tính toán các chỉ số liên quan đến hoa hồng, THE Dashboard_System SHALL lấy tỷ lệ hoa hồng từ SystemConfig entity để đảm bảo tính nhất quán với chính sách hoa hồng hiện tại của nền tảng
6. THE Dashboard_System SHALL xử lý giá trị null trong các trường tùy chọn bằng cách coi chúng là zero hoặc loại trừ chúng khỏi các phép tính tùy theo từng chỉ số
7. WHEN tính toán các chỉ số phần trăm, THE Dashboard_System SHALL trả về null nếu mẫu số bằng zero để tránh lỗi chia cho zero
8. THE Dashboard_System SHALL làm tròn tất cả giá trị phần trăm đến 2 chữ số thập phân cho mục đích hiển thị

# Requirements Document

## Introduction

Tích hợp lại tính năng AI gen ảnh vào luồng request-based để cho phép khách hàng tạo ảnh concept AI trước khi lưu custom request, với giới hạn regenerate sau khi lưu.

## Glossary

- **System**: Hệ thống Catholic Souvenir Custom Order
- **Customer**: Khách hàng sử dụng hệ thống để đặt hàng tùy chỉnh
- **Artisan**: Nghệ nhân nhận và thực hiện đơn hàng tùy chỉnh
- **CustomRequest**: Yêu cầu tùy chỉnh do khách hàng tạo ra
- **AI Concept Image**: Hình ảnh concept được tạo bởi AI dựa trên mô tả
- **Image Generation Count**: Số lần đã regenerate ảnh AI sau khi lưu request (giới hạn 3 lần)
- **Request Status**: Trạng thái của custom request (DRAFT, OPEN, ARTISAN_SELECTED, IN_PROGRESS, COMPLETED)

## Requirements

### Requirement 1

**User Story:** Là một Customer, tôi muốn có thể tạo ảnh AI concept trên UI trước khi lưu custom request, để có thể xem và điều chỉnh trước khi lưu.

#### Acceptance Criteria

1. THE System SHALL cung cấp endpoint riêng để tạo ảnh AI từ description mà không cần lưu CustomRequest
2. WHEN Customer gọi endpoint tạo ảnh AI với description, THE System SHALL trả về imageUrl và prompt đã sử dụng
3. THE System SHALL không giới hạn số lần Customer gọi endpoint tạo ảnh trước khi lưu request
4. THE System SHALL validate description length tối thiểu 50 ký tự và tối đa 1000 ký tự

### Requirement 2

**User Story:** Là một Customer, tôi muốn lưu custom request với đầy đủ thông tin bao gồm ảnh AI đã tạo, để có request hoàn chỉnh.

#### Acceptance Criteria

1. WHEN Customer gọi POST /api/custom-requests, THE System SHALL yêu cầu title, description và aiConceptImageUrl
2. IF aiConceptImageUrl là null hoặc empty, THEN THE System SHALL trả về lỗi "Vui lòng tạo ảnh AI trước khi lưu yêu cầu"
3. WHEN request được lưu với aiConceptImageUrl, THE System SHALL set imageGenCount=0 và status=DRAFT
4. THE System SHALL lưu aiImagePrompt kèm theo aiConceptImageUrl
5. THE System SHALL cho phép description từ AI generation được tự động điền vào field description của form

### Requirement 3

**User Story:** Là một Customer, tôi muốn có thể regenerate ảnh AI cho request đã lưu ở trạng thái DRAFT, để tinh chỉnh trước khi publish.

#### Acceptance Criteria

1. WHILE CustomRequest ở trạng thái DRAFT, THE System SHALL cho phép Customer regenerate ảnh AI
2. WHEN Customer regenerate ảnh cho DRAFT request, THE System SHALL kiểm tra quyền sở hữu
3. IF imageGenCount đã đạt giới hạn 3, THEN THE System SHALL từ chối và trả về lỗi "Bạn đã hết lượt tạo lại ảnh (tối đa 3 lần)"
4. WHEN regenerate thành công, THE System SHALL cập nhật aiConceptImageUrl, aiImagePrompt và tăng imageGenCount

### Requirement 4

**User Story:** Là một Customer, tôi muốn publish request để nghệ nhân có thể xem và làm việc với yêu cầu của tôi.

#### Acceptance Criteria

1. WHEN Customer publish CustomRequest từ DRAFT sang OPEN, THE System SHALL kiểm tra aiConceptImageUrl không null
2. IF aiConceptImageUrl là null, THEN THE System SHALL từ chối publish và trả về lỗi "Yêu cầu phải có ảnh concept"
3. WHEN publish thành công, THE System SHALL gửi notification cho tất cả Artisan kèm ảnh concept
4. THE System SHALL đảm bảo ảnh concept hiển thị trong danh sách request cho Artisan

### Requirement 5

**User Story:** Là một Customer, tôi muốn xem lịch sử các ảnh AI đã tạo cho request, để có thể so sánh và chọn phiên bản phù hợp nhất.

#### Acceptance Criteria

1. THE System SHALL lưu trữ tất cả các ảnh AI đã regenerate cho mỗi CustomRequest trong bảng riêng
2. WHEN Customer xem chi tiết request, THE System SHALL hiển thị danh sách tất cả ảnh AI đã regenerate kèm timestamp
3. THE System SHALL hiển thị prompt đã sử dụng cho mỗi ảnh
4. THE System SHALL cho phép Customer chọn một ảnh từ lịch sử làm ảnh concept chính (aiConceptImageUrl)

### Requirement 6

**User Story:** Là một Customer, tôi muốn có thể cập nhật description và title cho DRAFT request, để tinh chỉnh ý tưởng trước khi publish.

#### Acceptance Criteria

1. WHILE CustomRequest ở trạng thái DRAFT, THE System SHALL cho phép Customer cập nhật description và title
2. WHEN description hoặc title được cập nhật, THE System SHALL không đặt lại imageGenCount
3. THE System SHALL cho phép Customer regenerate ảnh AI với description đã cập nhật
4. IF CustomRequest không ở trạng thái DRAFT, THEN THE System SHALL từ chối việc cập nhật description và title

### Requirement 7

**User Story:** Là một Developer, tôi muốn hệ thống xử lý lỗi AI service một cách graceful, để không ảnh hưởng đến trải nghiệm người dùng.

#### Acceptance Criteria

1. WHEN AI service không khả dụng, THE System SHALL trả về thông báo lỗi rõ ràng cho người dùng
2. THE System SHALL retry tối đa 3 lần với backoff delay khi gọi AI service
3. IF tất cả retry đều thất bại, THEN THE System SHALL ghi log error và trả về response với success=false
4. THE System SHALL không block các chức năng khác của CustomRequest khi AI service lỗi

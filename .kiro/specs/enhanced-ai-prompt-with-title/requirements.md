# Tài Liệu Yêu Cầu

## Giới Thiệu

Tài liệu này mô tả các yêu cầu cải tiến cho hệ thống Custom Request hiện tại. Hệ thống đã có đầy đủ chức năng cơ bản bao gồm:
- Title field để customer quản lý các request
- AI image generation sử dụng description
- Workflow: Draft → Publish → Artisan Selection

Các cải tiến cần thực hiện tập trung vào:
1. Tối ưu hóa prompt engineering cho AI
2. Cải thiện UX cho việc preview và regenerate AI image
3. Tăng cường logging và monitoring

## Thuật Ngữ

- **AI_Image_Service**: Dịch vụ backend chịu trách nhiệm tạo ảnh sử dụng API AI bên ngoài (Hugging Face Stable Diffusion)
- **Custom_Request**: Yêu cầu do khách hàng khởi tạo mô tả sản phẩm đồ lưu niệm tùy chỉnh mong muốn
- **Prompt**: Văn bản đầu vào gửi đến API tạo ảnh AI - CHỈ sử dụng description, KHÔNG dùng title
- **Title**: Tên ngắn gọn (15-1000 ký tự) để customer quản lý các custom request - KHÔNG dùng cho AI prompt
- **Description**: Mô tả chi tiết (50-5000 ký tự) - được sử dụng ĐỘC LẬP để tạo AI prompt
- **Frontend_Client**: Ứng dụng React/Vue mà khách hàng sử dụng để tạo yêu cầu tùy chỉnh
- **Generate_Design_Endpoint**: REST API endpoint `/api/ai/generate-design` nhận yêu cầu tạo ảnh AI (hiện tại)
- **Custom_Request_Workflow**: DRAFT → OPEN (published) → ARTISAN_SELECTED → IN_PROGRESS → COMPLETED
- **Image_Gen_Limit**: Giới hạn 3 lần tạo/tạo lại ảnh AI cho mỗi custom request

## Yêu Cầu

### Yêu Cầu 1

**User Story:** Là developer, tôi muốn cải thiện prompt engineering cho AI image generation, để ảnh được tạo ra có chất lượng cao hơn và phù hợp với ngữ cảnh đồ lưu niệm Công giáo.

#### Tiêu Chí Chấp Nhận

1. KHI AI_Image_Service xây dựng prompt từ description, THÌ AI_Image_Service PHẢI sử dụng phương thức `buildEnhancedPrompt` chuyên dụng
2. KHI phương thức `buildEnhancedPrompt` thực thi, THÌ phương thức PHẢI tuân theo cấu trúc: "Catholic religious souvenir, [description], high quality, detailed craftsmanship, religious art style, sacred atmosphere, professional product photography, clean background"
3. KHI AI_Image_Service nhận description, THÌ AI_Image_Service KHÔNG ĐƯỢC sử dụng title field trong prompt
4. KHI prompt được xây dựng, THÌ AI_Image_Service PHẢI ghi log prompt cuối cùng ở mức INFO để theo dõi chất lượng

### Yêu Cầu 2

**User Story:** Là khách hàng, tôi muốn xem rõ số lần tạo ảnh còn lại, để tôi biết cần cân nhắc kỹ trước khi regenerate.

#### Tiêu Chí Chấp Nhận

1. KHI Frontend_Client hiển thị custom request detail, THÌ Frontend_Client PHẢI hiển thị số lần đã tạo ảnh và giới hạn tối đa (ví dụ: "2/3 lần")
2. KHI customer đã sử dụng hết 3 lần tạo ảnh, THÌ Frontend_Client PHẢI ẩn hoặc disable nút "Tạo lại ảnh AI"
3. KHI API trả về custom request response, THÌ API PHẢI bao gồm cả `imageGenCount` và `maxImageGenCount` trong response
4. NẾU customer cố gắng regenerate khi đã hết lượt, THÌ API PHẢI trả về lỗi 400 với message "Bạn đã hết lượt tạo ảnh (tối đa 3 lần)"

### Yêu Cầu 3

**User Story:** Là khách hàng, tôi muốn có trải nghiệm tốt hơn khi tạo và xem ảnh AI, để tôi có thể đánh giá chất lượng ảnh trước khi publish request.

#### Tiêu Chí Chấp Nhận

1. KHI Frontend_Client gọi API tạo custom request với `generateAiImage=true`, THÌ Frontend_Client PHẢI hiển thị loading indicator trong quá trình tạo ảnh
2. KHI AI image được tạo thành công, THÌ Frontend_Client PHẢI hiển thị ảnh preview với kích thước phù hợp
3. KHI customer xem ảnh preview, THÌ Frontend_Client PHẢI cung cấp nút "Tạo lại ảnh" nếu còn lượt
4. KHI customer ở trang danh sách custom requests, THÌ Frontend_Client PHẢI hiển thị thumbnail của AI image (nếu có) cùng với title

### Yêu Cầu 4

**User Story:** Là developer, tôi muốn có logging chi tiết cho AI image generation process, để tôi có thể troubleshoot và optimize performance.

#### Tiêu Chí Chấp Nhận

1. KHI AI_Image_Service bắt đầu generate image, THÌ AI_Image_Service PHẢI log request ID và description length ở mức INFO
2. KHI AI_Image_Service gọi Hugging Face API, THÌ AI_Image_Service PHẢI log API endpoint, model name, và prompt ở mức DEBUG
3. NẾU Hugging Face API trả về lỗi, THÌ AI_Image_Service PHẢI log status code, error message, và retry attempt ở mức ERROR
4. KHI image được upload lên Supabase thành công, THÌ AI_Image_Service PHẢI log Supabase URL và file size ở mức INFO
5. KHI toàn bộ quá trình hoàn tất, THÌ AI_Image_Service PHẢI log tổng thời gian xử lý ở mức INFO

### Yêu Cầu 5

**User Story:** Là khách hàng, tôi muốn có thể tạo custom request mà không bắt buộc phải generate AI image, để tôi có thể linh hoạt sử dụng reference images của riêng mình.

#### Tiêu Chí Chấp Nhận

1. KHI Frontend_Client hiển thị form tạo custom request, THÌ Frontend_Client PHẢI cung cấp checkbox "Tạo ảnh concept bằng AI" với giá trị mặc định là false
2. KHI customer không chọn generate AI image, THÌ API PHẢI tạo custom request thành công mà không có `aiConceptImageUrl`
3. KHI customer chọn generate AI image nhưng AI service thất bại, THÌ API PHẢI vẫn tạo custom request thành công và log warning
4. KHI custom request không có AI image, THÌ Frontend_Client PHẢI hiển thị placeholder image hoặc icon thay thế

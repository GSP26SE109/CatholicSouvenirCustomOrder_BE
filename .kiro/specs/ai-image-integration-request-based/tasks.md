# Implementation Plan

- [x] 1. Tạo endpoint generate AI image (pre-save)





  - Tạo DTO `GenerateConceptImageRequest` với validation description 50-1000 chars
  - Tạo endpoint POST `/api/ai/generate-concept` trong `AIController`
  - Sử dụng lại `AIImageService.generateConceptImage()` đã có
  - Trả về `AIImageResponse` với imageUrl và prompt
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [x] 2. Sửa logic tạo CustomRequest để bắt buộc có ảnh AI





  - Sửa `CreateFreeFormRequestDTO`: thêm `aiConceptImageUrl`, `aiImagePrompt`, loại bỏ `generateAiImage`
  - Sửa `CustomRequestServiceImp.createFreeFormRequest()`: validate `aiConceptImageUrl` not null
  - Throw `BadRequestException` nếu không có `aiConceptImageUrl`
  - Set `imageGenCount = 0` khi tạo mới
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 3. Sửa logic publish request để validate có ảnh





  - Sửa `CustomRequestServiceImp.publishRequest()`: kiểm tra `aiConceptImageUrl` not null
  - Throw `BadRequestException` với message "Yêu cầu phải có ảnh concept" nếu null
  - Đảm bảo notification gửi cho Artisan có kèm ảnh concept
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 4. Tạo endpoint update DRAFT request





  - Tạo DTO `UpdateDraftRequestDTO` với optional title và description
  - Tạo method `updateDraftRequest()` trong `CustomRequestService` interface
  - Implement `updateDraftRequest()` trong `CustomRequestServiceImp`
  - Kiểm tra status = DRAFT, throw exception nếu không phải DRAFT
  - Kiểm tra quyền sở hữu request
  - Không reset `imageGenCount` khi update
  - Tạo endpoint PUT `/api/custom-requests/{id}` trong `CustomRequestController`
  - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [ ] 5. Kiểm tra và test toàn bộ flow



  - Test POST `/api/ai/generate-concept` với description hợp lệ
  - Test POST `/api/ai/generate-concept` với description < 50 chars → 400 error
  - Test POST `/api/custom-requests` without `aiConceptImageUrl` → 400 error
  - Test POST `/api/custom-requests` with `aiConceptImageUrl` → success, imageGenCount=0
  - Test POST `/api/custom-requests/{id}/regenerate-image` 3 lần → success
  - Test POST `/api/custom-requests/{id}/regenerate-image` lần 4 → 400 error
  - Test POST `/api/custom-requests/{id}/publish` without image → 400 error
  - Test POST `/api/custom-requests/{id}/publish` with image → success
  - Test PUT `/api/custom-requests/{id}` with DRAFT status → success
  - Test PUT `/api/custom-requests/{id}` with OPEN status → 400 error
  - _Requirements: 1.1-7.4_

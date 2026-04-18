# Design Document

## Overview

Thiết kế tích hợp AI image generation vào luồng request-based với flow: tạo ảnh trên UI trước → lưu request với ảnh → có thể regenerate sau khi lưu (giới hạn 3 lần).

## Architecture

### High-Level Flow

```
UI (Customer)
    ↓
1. Nhập description → POST /api/ai/generate-concept (không giới hạn)
    ↓
2. Nhận imageUrl → Điền vào form (title + description + imageUrl)
    ↓
3. POST /api/custom-requests (bắt buộc có imageUrl)
    ↓
4. Request lưu với status=DRAFT, imageGenCount=0
    ↓
5. (Optional) POST /api/custom-requests/{id}/regenerate-image (max 3 lần)
    ↓
6. POST /api/custom-requests/{id}/publish (kiểm tra có ảnh)
```

## Components and Interfaces

### 1. New Endpoint: Generate AI Image (Pre-Save)

**Controller**: `AIController`

```java
@PostMapping("/generate-concept")
@PreAuthorize("hasAuthority('CUSTOMER')")
public ResponseEntity<BaseResponse<AIImageResponse>> generateConceptImage(
        @Valid @RequestBody GenerateConceptImageRequest request)
```

**Request DTO**: `GenerateConceptImageRequest`
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateConceptImageRequest {
    @NotBlank(message = "Mô tả không được để trống")
    @Size(min = 50, max = 1000, message = "Mô tả phải từ 50 đến 1000 ký tự")
    private String description;
}
```

**Service**: `AIImageService.generateConceptImage()`
- Đã có sẵn, sử dụng lại
- Input: `AIPromptRequest` với `additionalDescription`
- Output: `AIImageResponse` với `imageUrl`, `prompt`, `success`

### 2. Modified Endpoint: Create Custom Request

**Controller**: `CustomRequestController.createCustomRequest()`

**Modified Request DTO**: `CreateFreeFormRequestDTO`
```java
// Thêm fields mới:
private String aiConceptImageUrl;  // Bắt buộc
private String aiImagePrompt;      // Optional, lưu prompt đã dùng

// Loại bỏ:
// private Boolean generateAiImage;  // Không cần nữa vì tạo trước
```

**Validation Logic** (trong `CustomRequestServiceImp.createFreeFormRequest()`):
```java
// Kiểm tra aiConceptImageUrl bắt buộc
if (request.getAiConceptImageUrl() == null || request.getAiConceptImageUrl().isEmpty()) {
    throw new BadRequestException("Vui lòng tạo ảnh AI trước khi lưu yêu cầu");
}

// Set imageGenCount = 0 khi tạo mới
customRequest.setImageGenCount(0);
```

### 3. Existing Endpoint: Regenerate AI Image (Post-Save)

**Controller**: `CustomRequestController.regenerateAIImage()`
- Đã có sẵn, giữ nguyên logic
- Kiểm tra `imageGenCount < 3`
- Tăng `imageGenCount` sau mỗi lần regenerate

### 4. Modified Endpoint: Publish Request

**Controller**: `CustomRequestController.publishRequest()`

**Modified Logic** (trong `CustomRequestServiceImp.publishRequest()`):
```java
// Thêm validation
if (customRequest.getAiConceptImageUrl() == null || customRequest.getAiConceptImageUrl().isEmpty()) {
    throw new BadRequestException("Yêu cầu phải có ảnh concept");
}
```

### 5. New Feature: Update DRAFT Request

**New Endpoint**:
```java
@PutMapping("/{id}")
@PreAuthorize("hasAuthority('CUSTOMER')")
public ResponseEntity<BaseResponse<CustomRequestResponse>> updateDraftRequest(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateDraftRequestDTO request,
        Authentication authentication)
```

**New Request DTO**: `UpdateDraftRequestDTO`
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDraftRequestDTO {
    @Size(min = 15, max = 1000, message = "Tiêu đề phải từ 15 đến 1000 ký tự")
    private String title;
    
    @Size(min = 50, max = 5000, message = "Mô tả phải từ 50 đến 5000 ký tự")
    private String description;
}
```

**Service Method**: `CustomRequestService.updateDraftRequest()`
```java
CustomRequestResponse updateDraftRequest(UUID requestId, UpdateDraftRequestDTO request, UUID customerId);
```

## Data Models

### Modified: CustomRequest
```java
// Các fields đã có - không cần thay đổi:
private String aiConceptImageUrl;  // URL ảnh AI trên Supabase
private String aiImagePrompt;      // Prompt đã sử dụng
private Integer imageGenCount = 0; // Số lần regenerate (max 3)
```

## Error Handling

### Custom Exceptions

**Existing**: `ImageGenerationLimitExceededException`
- Sử dụng khi `imageGenCount >= 3`

**Existing**: `BadRequestException`
- Sử dụng cho validation errors

### Error Responses

```java
// AI service unavailable
{
    "success": false,
    "errorMessage": "Dịch vụ AI tạm thời không khả dụng. Vui lòng thử lại sau."
}

// Missing AI image when creating request
{
    "success": false,
    "errorMessage": "Vui lòng tạo ảnh AI trước khi lưu yêu cầu"
}

// Regenerate limit exceeded
{
    "success": false,
    "errorMessage": "Bạn đã hết lượt tạo lại ảnh (tối đa 3 lần)"
}

// Publish without image
{
    "success": false,
    "errorMessage": "Yêu cầu phải có ảnh concept"
}

// Update non-DRAFT request
{
    "success": false,
    "errorMessage": "Chỉ có thể cập nhật yêu cầu ở trạng thái nháp"
}
```

## Testing Strategy

### Unit Tests

1. **AIImageServiceImp**
   - Test `generateConceptImage()` với description hợp lệ
   - Test validation description length (50-1000 chars)
   - Test retry mechanism khi AI service fail
   - Test graceful failure handling

2. **CustomRequestServiceImp**
   - Test `createFreeFormRequest()` với aiConceptImageUrl null → throw exception
   - Test `createFreeFormRequest()` với aiConceptImageUrl hợp lệ → imageGenCount=0
   - Test `regenerateAIImage()` khi imageGenCount < 3 → success
   - Test `regenerateAIImage()` khi imageGenCount >= 3 → throw exception
   - Test `publishRequest()` với aiConceptImageUrl null → throw exception
   - Test `updateDraftRequest()` với status=DRAFT → success
   - Test `updateDraftRequest()` với status!=DRAFT → throw exception

### Integration Tests

1. **AI Image Generation Flow**
   - POST `/api/ai/generate-concept` → verify imageUrl returned
   - POST `/api/ai/generate-concept` multiple times → no limit
   - POST `/api/ai/generate-concept` với description < 50 chars → 400 error

2. **Create Request Flow**
   - POST `/api/custom-requests` without aiConceptImageUrl → 400 error
   - POST `/api/custom-requests` with aiConceptImageUrl → 200 success, imageGenCount=0

3. **Regenerate Flow**
   - POST `/api/custom-requests/{id}/regenerate-image` 3 times → success
   - POST `/api/custom-requests/{id}/regenerate-image` 4th time → 400 error
   - Verify imageGenCount increments correctly

4. **Publish Flow**
   - POST `/api/custom-requests/{id}/publish` without aiConceptImageUrl → 400 error
   - POST `/api/custom-requests/{id}/publish` with aiConceptImageUrl → 200 success

5. **Update DRAFT Flow**
   - PUT `/api/custom-requests/{id}` with status=DRAFT → 200 success
   - PUT `/api/custom-requests/{id}` with status=OPEN → 400 error
   - Verify imageGenCount not reset after update

## API Endpoints Summary

### New Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/ai/generate-concept` | Tạo ảnh AI từ description (pre-save) | CUSTOMER |
| PUT | `/api/custom-requests/{id}` | Cập nhật title/description cho DRAFT request | CUSTOMER |

### Modified Endpoints

| Method | Endpoint | Changes |
|--------|----------|---------|
| POST | `/api/custom-requests` | Bắt buộc `aiConceptImageUrl`, loại bỏ `generateAiImage` |
| POST | `/api/custom-requests/{id}/publish` | Thêm validation `aiConceptImageUrl` not null |
| POST | `/api/custom-requests/{id}/regenerate-image` | Giữ nguyên logic, đã có sẵn |

## Implementation Notes

### Convention to Follow

1. **DTO Naming**: `{Action}{Entity}{Request/Response}DTO`
   - Example: `GenerateConceptImageRequest`, `UpdateDraftRequestDTO`

2. **Service Interface**: Define in `service/` package
   - Example: `CustomRequestService`, `AIImageService`

3. **Service Implementation**: Implement in `service/imp/` package
   - Example: `CustomRequestServiceImp`, `AIImageServiceImp`

4. **Repository**: Extend `JpaRepository`
   - Example: `CustomRequestAIImageRepository`

5. **Exception Handling**: Use `@ControllerAdvice` in `GlobalExceptionHandler`

6. **Validation**: Use Jakarta validation annotations
   - `@NotBlank`, `@Size`, `@NotNull`, etc.

7. **Response Format**: Use `BaseResponse<T>`
   - Success: `BaseResponse.success(message, data)`
   - Error: `BaseResponse.error(code, message)`

### Existing Code to Reuse

1. **AIImageService.generateConceptImage()** - Đã có sẵn, sử dụng lại
2. **CustomRequestController.regenerateAIImage()** - Đã có sẵn, giữ nguyên
3. **AIImageServiceImp retry mechanism** - Đã có `@Retryable` annotation
4. **SupabaseStorageService** - Đã có sẵn để upload ảnh

### New Code to Implement

1. **GenerateConceptImageRequest** DTO
2. **UpdateDraftRequestDTO** DTO
3. **AIController.generateConceptImage()** endpoint
4. **CustomRequestController.updateDraftRequest()** endpoint
5. Modify **CreateFreeFormRequestDTO** (add fields, remove generateAiImage)
6. Modify **CustomRequestServiceImp.createFreeFormRequest()** (add validation)
7. Modify **CustomRequestServiceImp.publishRequest()** (add validation)
8. Add **CustomRequestService.updateDraftRequest()** method
9. Add **CustomRequestServiceImp.updateDraftRequest()** implementation

## Database Changes

### Modified Table: custom_requests

No schema changes needed. Existing fields are sufficient:
- `ai_concept_image_url` TEXT - URL ảnh AI trên Supabase
- `ai_image_prompt` TEXT - Prompt đã sử dụng
- `image_gen_count` INTEGER DEFAULT 0 - Số lần regenerate (max 3)

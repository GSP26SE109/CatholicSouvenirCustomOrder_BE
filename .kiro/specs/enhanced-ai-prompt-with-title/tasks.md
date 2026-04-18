# Implementation Plan - Enhanced AI Prompt Engineering

## Overview

Implementation plan để cải tiến AI image generation trong Custom Request system với focus vào prompt engineering, image generation limits, và enhanced logging.

---

## Tasks

- [ ] 1. Implement enhanced prompt building in AIImageService
  - Create `buildEnhancedPrompt()` method với Catholic souvenir context
  - Method nhận ONLY description parameter (không dùng title)
  - Implement prompt structure: "Catholic religious souvenir, [description], high quality, detailed craftsmanship, religious art style, sacred atmosphere, professional product photography, clean background"
  - Add INFO level logging cho final prompt
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [ ] 2. Add comprehensive logging to AI image generation process
  - [ ] 2.1 Add request ID tracking throughout generation flow
    - Generate UUID cho mỗi generation request
    - Pass requestId through all methods
    - Include requestId trong tất cả log messages
    - _Requirements: 4.1_

  - [ ] 2.2 Implement start/completion logging
    - Log INFO khi bắt đầu generation với description length
    - Log INFO khi hoàn thành với total processing time
    - Track start time và calculate duration
    - _Requirements: 4.1, 4.5_

  - [ ] 2.3 Add detailed API call logging
    - Log DEBUG cho Hugging Face API endpoint
    - Log DEBUG cho model name
    - Log DEBUG cho enhanced prompt
    - _Requirements: 4.2_

  - [ ] 2.4 Implement error logging with retry tracking
    - Log ERROR khi Hugging Face API fails
    - Include HTTP status code trong error log
    - Include error message và retry attempt number
    - Log full stack trace cho debugging
    - _Requirements: 4.3_

  - [ ] 2.5 Add Supabase upload logging
    - Log INFO khi upload thành công
    - Include Supabase URL trong log
    - Include file size (bytes) trong log
    - _Requirements: 4.4_

- [ ] 3. Update CustomRequestService to manage image generation limits
  - [ ] 3.1 Add MAX_IMAGE_GEN_COUNT constant (value = 3)
    - Define constant trong CustomRequestServiceImp
    - Document purpose của limit
    - _Requirements: 2.1, 2.2_

  - [ ] 3.2 Implement limit validation in regenerateAIImage method
    - Check imageGenCount < MAX_IMAGE_GEN_COUNT trước khi generate
    - Throw BadRequestException với message "Bạn đã hết lượt tạo ảnh (tối đa 3 lần)" nếu exceeded
    - Return appropriate HTTP 400 error response
    - _Requirements: 2.4_

  - [ ] 3.3 Increment imageGenCount after successful generation
    - Update imageGenCount field sau mỗi lần generate thành công
    - Save updated CustomRequest entity
    - _Requirements: 2.1_

  - [ ] 3.4 Add imageGenCount initialization in createCustomRequest
    - Set imageGenCount = 0 khi tạo request mới (nếu không generate)
    - Set imageGenCount = 1 nếu generate AI image lúc tạo
    - _Requirements: 2.1_

- [ ] 4. Update CustomRequestResponse DTO with generation count fields
  - Add `imageGenCount` field (Integer)
  - Add `maxImageGenCount` field (Integer) 
  - Add `canRegenerateImage` field (Boolean) - calculated từ imageGenCount < maxImageGenCount
  - Update mapToResponse() method để populate new fields
  - _Requirements: 2.3_

- [ ] 5. Implement optional AI image generation
  - [ ] 5.1 Update createCustomRequest to handle generateAiImage flag
    - Check `generateAiImage` boolean trong request DTO
    - Only call aiImageService.generateImage() nếu flag = true
    - Default value của flag = false
    - _Requirements: 5.1, 5.2_

  - [ ] 5.2 Add graceful error handling for AI service failures
    - Wrap AI generation trong try-catch block
    - Log WARNING nếu generation fails
    - Continue creating CustomRequest thành công (without image)
    - Set aiConceptImageUrl = null nếu generation fails
    - _Requirements: 5.3_

  - [ ] 5.3 Ensure CustomRequest creation succeeds without AI image
    - Verify request được save thành công ngay cả khi aiConceptImageUrl = null
    - Return valid CustomRequestResponse
    - _Requirements: 5.2, 5.3_

- [ ] 6. Update AIImageServiceImp to use enhanced prompt
  - [ ] 6.1 Refactor generateImage() method
    - Call buildEnhancedPrompt() thay vì enhancePromptForReligiousArt()
    - Pass enhanced prompt to generateWithHuggingFace()
    - _Requirements: 1.1_

  - [ ] 6.2 Remove or deprecate old enhancePromptForReligiousArt() method
    - Mark method as @Deprecated nếu cần backward compatibility
    - Hoặc remove completely nếu không còn sử dụng
    - _Requirements: 1.1_

  - [ ] 6.3 Update generateWithHuggingFace() signature
    - Add requestId parameter cho logging
    - Update all log statements để include requestId
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [ ] 7. Add regenerateAIImage endpoint to CustomRequestController
  - Create POST endpoint `/api/custom-requests/{requestId}/regenerate-image`
  - Validate request ownership (customerId matches authenticated user)
  - Call customRequestService.regenerateAIImage()
  - Return updated CustomRequestResponse
  - Handle BadRequestException cho limit exceeded case
  - _Requirements: 2.4_

- [ ] 8. Update existing endpoints to return new response fields
  - Update GET `/api/custom-requests/{id}` response
  - Update GET `/api/custom-requests` list response
  - Update POST `/api/custom-requests` create response
  - Ensure all responses include imageGenCount, maxImageGenCount, canRegenerateImage
  - _Requirements: 2.3_

- [ ]* 9. Add integration tests for new functionality
  - [ ]* 9.1 Test enhanced prompt building
    - Verify prompt structure matches requirements
    - Verify title is NOT included in prompt
    - Verify description is included correctly
    - _Requirements: 1.2, 1.3_

  - [ ]* 9.2 Test image generation limit enforcement
    - Create request và regenerate 3 times
    - Verify 4th attempt throws BadRequestException
    - Verify error message matches requirement
    - _Requirements: 2.4_

  - [ ]* 9.3 Test optional AI generation
    - Test creating request với generateAiImage=false
    - Test creating request với generateAiImage=true
    - Test graceful handling khi AI service fails
    - _Requirements: 5.1, 5.2, 5.3_

  - [ ]* 9.4 Test response includes generation count fields
    - Verify imageGenCount trong response
    - Verify maxImageGenCount = 3
    - Verify canRegenerateImage calculation
    - _Requirements: 2.3_

- [ ]* 10. Add logging verification tests
  - [ ]* 10.1 Verify INFO logs are generated
    - Check prompt logging
    - Check start/completion logging
    - Check Supabase upload logging
    - _Requirements: 1.4, 4.1, 4.4, 4.5_

  - [ ]* 10.2 Verify DEBUG logs are generated
    - Check API endpoint logging
    - Check model name logging
    - _Requirements: 4.2_

  - [ ]* 10.3 Verify ERROR logs on failures
    - Simulate API failure
    - Check error logging với status code
    - Check retry attempt logging
    - _Requirements: 4.3_


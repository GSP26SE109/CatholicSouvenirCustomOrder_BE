# AI Features Guide - Catholic Souvenir Platform

Tất cả AI features được tập trung trong `/api/ai` controller để dễ quản lý và kiểm soát.

## 📋 Table of Contents
1. [AI Image Validation](#ai-image-validation)
2. [AI Product Description](#ai-product-description)
3. [AI Scripture Recommendation](#ai-scripture-recommendation)
4. [AI Image Generation](#ai-image-generation)

---

## 1. AI Image Validation

### Tổng quan
Kiểm tra xem hình ảnh có phải là vật phẩm Công Giáo hay không.

### Confidence Levels
- **High (≥0.8)**: Tự động chấp nhận
- **Medium (0.5-0.8)**: Chấp nhận nhưng cần review
- **Low (<0.5)**: Yêu cầu admin review

### Detected Categories
`statue`, `rosary`, `cross`, `religious_art`, `medal`, `candle`, `religious_item`

### API Endpoints

#### 1.1. Validate Image File
**Use case**: Product upload file trực tiếp (Cloudinary)

```http
POST /api/ai/validate-image
Content-Type: multipart/form-data
```

```bash
curl -X POST http://localhost:8080/api/ai/validate-image \
  -F "image=@statue-mary.jpg"
```

**Response**:
```json
{
  "success": true,
  "message": "Image validated successfully - High confidence",
  "data": {
    "isValid": true,
    "confidenceScore": 0.92,
    "category": "statue",
    "detectedItems": ["mary", "statue", "holy"],
    "warning": null,
    "requiresManualReview": false
  }
}
```

#### 1.2. Validate Image URL
**Use case**: ProductTemplate - Frontend upload lên Supabase trước, Backend validate URL

```http
POST /api/ai/validate-image-url
```

```bash
curl -X POST http://localhost:8080/api/ai/validate-image-url \
  -d "imageUrl=https://supabase.co/storage/v1/object/public/images/statue.jpg"
```

#### 1.3. Batch Validate URLs
**Use case**: ProductTemplate có nhiều baseImages

```http
POST /api/ai/validate-images-batch
Content-Type: application/json
```

```bash
curl -X POST http://localhost:8080/api/ai/validate-images-batch \
  -H "Content-Type: application/json" \
  -d '{
    "imageUrls": [
      "https://supabase.co/.../statue1.jpg",
      "https://supabase.co/.../statue2.jpg"
    ]
  }'
```

**Response**:
```json
{
  "success": true,
  "message": "All images validated successfully",
  "data": {
    "allValid": true,
    "totalImages": 2,
    "validImages": 2,
    "results": [...]
  }
}
```

---

## 2. AI Product Description

### Tổng quan
Tự động tạo mô tả sản phẩm có tính tôn giáo bằng tiếng Việt.

### API Endpoint

```http
POST /api/ai/generate-description
Content-Type: application/json
```

**Request**:
```json
{
  "productName": "Tượng Đức Mẹ Maria",
  "category": "Tượng Thánh",
  "tags": "Đức Mẹ, Maria, Tượng",
  "existingDescription": "Tượng cao 30cm" // optional
}
```

**Response**:
```json
{
  "success": true,
  "message": "Mô tả được tạo bởi AI",
  "data": {
    "description": "Tượng Đức Mẹ Maria là một sản phẩm lưu niệm Công Giáo...",
    "aiGenerated": true,
    "message": "Mô tả được tạo bởi AI"
  }
}
```

**Example**:
```bash
curl -X POST http://localhost:8080/api/ai/generate-description \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "Chuỗi Mân Côi",
    "category": "Đồ Cầu Nguyện",
    "tags": "Mân Côi, Rosary"
  }'
```

---

## 3. AI Scripture Recommendation

### Tổng quan
Gợi ý câu Kinh Thánh phù hợp để khắc lên sản phẩm.

### API Endpoints

#### 3.1. Recommend Scripture
```http
POST /api/ai/recommend-scripture
Content-Type: application/json
```

**Request**:
```json
{
  "purpose": "baptism",
  "productName": "Rosary",
  "theme": "faith",
  "language": "vi",
  "maxResults": 3
}
```

**Response**:
```json
{
  "success": true,
  "message": "Gợi ý câu Kinh Thánh thành công",
  "data": {
    "recommendations": [
      {
        "verse": "Matthew 28:19",
        "text": "Go therefore and make disciples...",
        "translation": "Vậy anh em hãy đi...",
        "reason": "The Great Commission",
        "occasion": "Baptism, Confirmation"
      }
    ]
  }
}
```

#### 3.2. Get Popular Scriptures
```http
GET /api/ai/popular-scriptures?occasion=baptism&language=vi
```

---

## 4. AI Image Generation

### Tổng quan
Tạo hình ảnh concept cho custom order.

### API Endpoints

#### 4.1. Generate Design
```http
POST /api/ai/generate-design
Content-Type: application/json
```

**Request**:
```json
{
  "description": "Catholic statue of Virgin Mary",
  "size": "30cm",
  "material": "ceramic",
  "style": "traditional"
}
```

#### 4.2. Generate Concept Image
```http
POST /api/ai/generate-concept
Content-Type: application/json
```

**Request**:
```json
{
  "description": "A rosary with blue beads and silver cross"
}
```

---

## Frontend Integration Examples

### ProductTemplate Flow (Supabase Storage)

```typescript
// Step 1: Upload to Supabase
const uploadToSupabase = async (file: File) => {
  const { data, error } = await supabase.storage
    .from('product-images')
    .upload(`templates/${Date.now()}_${file.name}`, file);
  
  if (error) throw error;
  
  const { data: { publicUrl } } = supabase.storage
    .from('product-images')
    .getPublicUrl(data.path);
  
  return publicUrl;
};

// Step 2: Validate URL with AI
const validateImageUrl = async (imageUrl: string) => {
  const response = await fetch('/api/ai/validate-image-url', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: `imageUrl=${encodeURIComponent(imageUrl)}`
  });
  
  const result = await response.json();
  return result.data;
};

// Step 3: Create ProductTemplate
const createTemplate = async (formData) => {
  // Upload all images first
  const uploadedUrls = await Promise.all(
    formData.images.map(file => uploadToSupabase(file))
  );
  
  // Validate all URLs
  const validations = await fetch('/api/ai/validate-images-batch', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ imageUrls: uploadedUrls })
  }).then(r => r.json());
  
  if (!validations.data.allValid) {
    alert('Some images failed validation!');
    return;
  }
  
  // Create template with validated URLs
  await fetch('/api/templates', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      ...formData,
      baseImages: uploadedUrls
    })
  });
};
```

### Product Flow (Direct Upload)

```typescript
const createProduct = async (formData) => {
  // Validate images before upload
  for (const image of formData.images) {
    const validation = await validateImageFile(image);
    
    if (!validation.isValid) {
      alert(`Image ${image.name} is not a Catholic item!`);
      return;
    }
    
    if (validation.requiresManualReview) {
      showWarning('This product will require admin review');
    }
  }
  
  // Upload to backend (Cloudinary)
  const formDataToSend = new FormData();
  formDataToSend.append('productName', formData.productName);
  formData.images.forEach(img => formDataToSend.append('images', img));
  
  await fetch('/api/product', {
    method: 'POST',
    body: formDataToSend
  });
};

const validateImageFile = async (file: File) => {
  const formData = new FormData();
  formData.append('image', file);
  
  const response = await fetch('/api/ai/validate-image', {
    method: 'POST',
    body: formData
  });
  
  const result = await response.json();
  return result.data;
};
```

---

## Configuration

### application.yml
```yaml
huggingface:
  api:
    key: ${HUGGINGFACE_API_KEY:}
    url: ${HUGGINGFACE_API_URL:https://router.huggingface.co/v1/chat/completions}
  text:
    model: ${HUGGINGFACE_TEXT_MODEL:meta-llama/Llama-3.1-8B-Instruct:novita}
  vision:
    model: ${HUGGINGFACE_VISION_MODEL:meta-llama/Llama-3.2-11B-Vision-Instruct}
```

### Environment Variables
```bash
HUGGINGFACE_API_KEY=your_api_key_here
HUGGINGFACE_VISION_MODEL=meta-llama/Llama-3.2-11B-Vision-Instruct  # Optional, có default
```

### Vision Models được hỗ trợ

**Qwen2-VL (Default - Recommended)**
- Model: `Qwen/Qwen2-VL-7B-Instruct`
- Ưu điểm: Nhẹ (7B), nhanh, hỗ trợ tốt tiếng Việt
- Miễn phí qua Hugging Face Inference API
- Hiểu hình ảnh tốt, phù hợp cho Catholic items

**Llama 3.2 Vision (Alternative)**
- Model: `meta-llama/Llama-3.2-11B-Vision-Instruct`
- Ưu điểm: Hiểu hình ảnh tốt, hỗ trợ tiếng Việt
- Nặng hơn (11B) nhưng chính xác hơn

**LLaVA (Alternative)**
- Model: `llava-hf/llava-v1.6-mistral-7b-hf`
- Ưu điểm: Open source, dễ fine-tune

### Cách đổi model

Chỉ cần set environment variable:
```bash
HUGGINGFACE_VISION_MODEL=Qwen/Qwen2-VL-7B-Instruct
# hoặc
HUGGINGFACE_VISION_MODEL=meta-llama/Llama-3.2-11B-Vision-Instruct
```

---

## Error Handling

### Common Errors

**400 Bad Request**
```json
{
  "success": false,
  "message": "Image URL is required",
  "errorCode": 400
}
```

**503 Service Unavailable** (AI không khả dụng)
```json
{
  "success": true,
  "message": "Mô tả được tạo từ template (AI không khả dụng)",
  "data": {
    "description": "...",
    "aiGenerated": false
  }
}
```

---

## Best Practices

### Image Validation
1. ✅ Validate trước khi upload (tiết kiệm bandwidth)
2. ✅ Show confidence score cho user
3. ✅ Batch validate khi có nhiều ảnh
4. ✅ Cache validation results
5. ✅ Allow admin override

### Product Description
1. ✅ Luôn cung cấp productName
2. ✅ Thêm category và tags để AI hiểu context
3. ✅ Cho phép user chỉnh sửa sau khi generate
4. ✅ Show loading state (2-5s)

### General
1. ✅ Handle fallback khi AI không khả dụng
2. ✅ Log all AI interactions
3. ✅ Monitor confidence scores
4. ✅ Collect user feedback
5. ✅ Set reasonable timeouts

---

## Monitoring

### Metrics to Track
- Validation success rate
- Average confidence score
- Manual review rate
- API response time
- Fallback usage rate

### Logs
```
INFO: Validating Catholic image: statue-mary.jpg
INFO: AI validation successful, confidence: 0.92
WARN: Image requires manual review, confidence: 0.65
ERROR: Image validation failed: not a Catholic item
```

---

## Roadmap

- [ ] Support video validation
- [ ] Custom confidence thresholds per category
- [ ] Fine-tune model for Catholic items
- [ ] Multi-language support
- [ ] Automated feedback collection
- [ ] A/B testing different thresholds

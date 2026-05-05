# AI Product Description Generator - Hướng dẫn sử dụng

## Tổng quan
Tính năng AI Product Description tự động tạo mô tả sản phẩm có tính tôn giáo, phù hợp với các sản phẩm lưu niệm Công Giáo.

## Cách hoạt động

### User Flow (Frontend)
1. Artisan điền form tạo sản phẩm
2. Tại field "Product Description", có 2 lựa chọn:
   - **Option 1**: Tự nhập mô tả thủ công
   - **Option 2**: Nhấn button "Generate AI Description" → Gọi API generate → Điền tự động vào field
3. User có thể chỉnh sửa mô tả sau khi AI generate
4. Submit form để tạo sản phẩm

### Backend Behavior
- **KHÔNG tự động generate** khi tạo sản phẩm
- Chỉ generate khi Frontend gọi API riêng: `POST /api/product/generate-description`
- User có toàn quyền kiểm soát việc sử dụng AI hay không

### API để generate description

#### Endpoint
```
POST /api/product/generate-description
```

#### Request Body
```json
{
  "productName": "Tượng Đức Mẹ Maria",
  "category": "Tượng Thánh",
  "tags": "Đức Mẹ, Maria, Tượng",
  "existingDescription": "Tượng Đức Mẹ Maria cao 30cm"
}
```

**Lưu ý**: 
- `productName` là bắt buộc
- `category`, `tags`, `existingDescription` là optional
- Nếu có `existingDescription`, AI sẽ cải thiện/mở rộng nó
- Nếu không có, AI sẽ tạo mô tả hoàn toàn mới

#### Response
```json
{
  "success": true,
  "message": "Mô tả được tạo bởi AI",
  "data": {
    "description": "Tượng Đức Mẹ Maria là một sản phẩm lưu niệm Công Giáo được chế tác tỉ mỉ với tâm huyết và lòng sùng kính. Sản phẩm này không chỉ là một vật dụng trang trí mà còn là một công cụ giúp bạn gần gũi hơn với Chúa trong cuộc sống hàng ngày...",
    "aiGenerated": true,
    "message": "Mô tả được tạo bởi AI"
  }
}
```

**Response khi AI không khả dụng**:
```json
{
  "success": true,
  "message": "Mô tả được tạo từ template (AI không khả dụng)",
  "data": {
    "description": "Tượng Đức Mẹ Maria là một sản phẩm lưu niệm Công Giáo...",
    "aiGenerated": false,
    "message": "Mô tả được tạo từ template (AI không khả dụng)"
  }
}
```

## Frontend Implementation Guide

### Recommended UI/UX

```jsx
// Example React component
function ProductDescriptionField() {
  const [description, setDescription] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);
  
  const handleGenerateAI = async () => {
    setIsGenerating(true);
    try {
      const response = await fetch('/api/product/generate-description', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          productName: formData.productName,
          category: formData.categoryName,
          tags: formData.tags.join(', '),
          existingDescription: description
        })
      });
      
      const result = await response.json();
      if (result.success) {
        setDescription(result.data.description);
        // Show notification: result.data.message
      }
    } catch (error) {
      console.error('Failed to generate description', error);
    } finally {
      setIsGenerating(false);
    }
  };
  
  return (
    <div className="form-group">
      <label>Product Description</label>
      <div className="description-field-wrapper">
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Nhập mô tả sản phẩm hoặc nhấn nút Generate AI"
          rows={6}
        />
        <button 
          type="button"
          onClick={handleGenerateAI}
          disabled={isGenerating || !formData.productName}
          className="btn-generate-ai"
        >
          {isGenerating ? (
            <>
              <Spinner size="sm" /> Đang tạo...
            </>
          ) : (
            <>
              <AIIcon /> Generate AI Description
            </>
          )}
        </button>
      </div>
      <small className="text-muted">
        Bạn có thể tự nhập hoặc sử dụng AI để tạo mô tả tự động
      </small>
    </div>
  );
}
```

### Button Placement Options

**Option 1: Button bên trong textarea (recommended)**
```
┌─────────────────────────────────────┐
│ Product Description                 │
├─────────────────────────────────────┤
│                                     │
│  [Textarea content here...]         │
│                                     │
│                                     │
│  ┌──────────────────────────────┐  │
│  │ 🤖 Generate AI Description  │  │
│  └──────────────────────────────┘  │
└─────────────────────────────────────┘
```

**Option 2: Button bên cạnh label**
```
Product Description  [🤖 Generate AI]
┌─────────────────────────────────────┐
│                                     │
│  [Textarea content here...]         │
│                                     │
└─────────────────────────────────────┘
```

**Option 3: Button dưới textarea**
```
Product Description
┌─────────────────────────────────────┐
│                                     │
│  [Textarea content here...]         │
│                                     │
└─────────────────────────────────────┘
[🤖 Generate AI Description]
```

## Cấu hình

### Trong application.yml
```yaml
huggingface:
  api:
    key: ${HUGGINGFACE_API_KEY:}
    url: ${HUGGINGFACE_API_URL:https://router.huggingface.co/v1/chat/completions}
    text:
      model: ${HUGGINGFACE_TEXT_MODEL:meta-llama/Llama-3.1-8B-Instruct:novita}
```

### Biến môi trường
```bash
HUGGINGFACE_API_KEY=your_api_key_here
```

## Đặc điểm của AI Description

### Nội dung được tạo bao gồm:
- ✅ Mô tả chi tiết về sản phẩm
- ✅ Ý nghĩa tôn giáo và tâm linh
- ✅ Cách sản phẩm giúp tăng cường đức tin
- ✅ Ngôn ngữ ấm áp, tôn trọng và truyền cảm hứng
- ✅ Độ dài 100-200 từ
- ✅ Hoàn toàn bằng tiếng Việt

### Fallback mechanism
Nếu AI không khả dụng, hệ thống sẽ:
1. Sử dụng mô tả có sẵn (nếu có)
2. Tạo mô tả từ template dựa trên tên, category, tags
3. Đảm bảo luôn có mô tả cho sản phẩm

## Ví dụ sử dụng

### Test API với curl
```bash
curl -X POST http://localhost:8080/api/product/generate-description \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "Chuỗi Mân Côi",
    "category": "Đồ Cầu Nguyện",
    "tags": "Mân Côi, Cầu Nguyện, Rosary"
  }'
```

### Tạo sản phẩm (description đã được generate trước)
```bash
# Step 1: Generate description first
DESCRIPTION=$(curl -X POST http://localhost:8080/api/product/generate-description \
  -H "Content-Type: application/json" \
  -d '{"productName":"Tượng Thánh Giuse","category":"Tượng Thánh"}' \
  | jq -r '.data.description')

# Step 2: Create product with generated description
curl -X POST http://localhost:8080/api/product \
  -H "Content-Type: multipart/form-data" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "productName=Tượng Thánh Giuse" \
  -F "productPrice=500000" \
  -F "quantity=10" \
  -F "categoryId=uuid-here" \
  -F "productDescription=$DESCRIPTION" \
  -F "images=@image1.jpg"
```

## Lưu ý kỹ thuật

### Performance
- AI generation có thể mất 2-5 giây
- Có cơ chế fallback nhanh nếu AI timeout
- Generate trước khi submit form (không block việc tạo sản phẩm)
- Frontend nên show loading state khi đang generate

### Error Handling
- Nếu AI fail → Trả về template-based description
- Frontend nên check `aiGenerated` flag để thông báo cho user
- Log tất cả errors để debug
- Không throw exception ra ngoài

### Best Practices
1. **Luôn cung cấp `productName`** - Đây là field bắt buộc
2. **Thêm `category` và `tags`** - Giúp AI hiểu context tốt hơn
3. **Có thể cung cấp `existingDescription`** - AI sẽ cải thiện thay vì tạo mới
4. **Test với endpoint riêng** trước khi tích hợp vào form
5. **Cho phép user chỉnh sửa** sau khi AI generate
6. **Disable button** khi productName chưa được điền
7. **Show loading state** khi đang generate (2-5s)

## Monitoring

### Logs để theo dõi
```
INFO: Generating AI description for product: Tượng Đức Mẹ Maria
INFO: AI description generated successfully
WARN: AI service unavailable, using fallback
ERROR: Error generating description: [error details]
```

## Roadmap
- [ ] Hỗ trợ đa ngôn ngữ (English, French)
- [ ] Cache descriptions phổ biến
- [ ] Fine-tune model cho Catholic products
- [ ] A/B testing AI vs template descriptions

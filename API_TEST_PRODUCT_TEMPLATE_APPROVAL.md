# KỊCH BẢN TEST API - PRODUCT TEMPLATE APPROVAL

## Base URL
```
http://localhost:8080/api
```

## Authentication
Tất cả API đều cần JWT token trong header:
```
Authorization: Bearer {token}
```

---

## 1. ARTISAN - TẠO TEMPLATE MỚI

### Endpoint
```http
POST /api/templates
```

### Headers
```
Authorization: Bearer {artisan_token}
Content-Type: application/json
```

### Request Body
```json
{
  "name": "Tượng Đức Mẹ Maria Tùy Chỉnh",
  "description": "Tượng Đức Mẹ Maria có thể tùy chỉnh tên, lời cầu nguyện",
  "categoryId": "category-uuid-here",
  "basePrice": 500000,
  "material": "Gỗ sồi",
  "style": "Cổ điển",
  "baseImages": [
    "https://example.com/image1.jpg",
    "https://example.com/image2.jpg"
  ],
  "customZones": [
    {
      "zoneName": "Tên người nhận",
      "zoneDescription": "Khắc tên lên đế tượng",
      "inputType": "TEXT",
      "inputConstraints": {
        "maxLength": 50,
        "minLength": 1
      },
      "extraPrice": 50000,
      "isRequired": true,
      "sortOrder": 1
    },
    {
      "zoneName": "Lời cầu nguyện",
      "zoneDescription": "Khắc lời cầu nguyện phía sau",
      "inputType": "TEXT",
      "inputConstraints": {
        "maxLength": 200
      },
      "extraPrice": 100000,
      "isRequired": false,
      "sortOrder": 2
    }
  ]
}
```

### Expected Response (200 OK)
```json
{
  "status": "success",
  "message": "Tạo mẫu sản phẩm thành công",
  "data": {
    "templateId": "template-uuid",
    "artisanId": "artisan-uuid",
    "artisanName": "Nghệ nhân ABC",
    "categoryId": "category-uuid",
    "name": "Tượng Đức Mẹ Maria Tùy Chỉnh",
    "description": "Tượng Đức Mẹ Maria có thể tùy chỉnh tên, lời cầu nguyện",
    "basePrice": 500000,
    "material": "Gỗ sồi",
    "style": "Cổ điển",
    "baseImages": ["https://example.com/image1.jpg"],
    "isActive": false,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00",
    "zoneCount": 2
  }
}
```

### ✅ Kiểm tra
- Template được tạo với `isActive = false` (chờ duyệt)
- Response trả về đầy đủ thông tin

---

## 2. ARTISAN - XEM TEMPLATE CỦA MÌNH

### Endpoint
```http
GET /api/templates/my-templates?page=0&size=10
```

### Headers
```
Authorization: Bearer {artisan_token}
```

### Expected Response (200 OK)
```json
{
  "status": "success",
  "message": "Lấy danh sách mẫu của tôi thành công",
  "data": {
    "content": [
      {
        "templateId": "template-uuid",
        "name": "Tượng Đức Mẹ Maria Tùy Chỉnh",
        "isActive": false,
        "basePrice": 500000,
        "zoneCount": 2,
        "createdAt": "2024-01-15T10:30:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 10,
    "number": 0
  }
}
```

### ✅ Kiểm tra
- Artisan thấy được cả template chờ duyệt (isActive = false)
- Có phân trang

---

## 3. PUBLIC - XEM DANH SÁCH TEMPLATE (CHỈ THẤY ĐÃ DUYỆT)

### Endpoint
```http
GET /api/templates?page=0&size=10
```

### Headers
```
(Không cần authentication)
```

### Expected Response (200 OK)
```json
{
  "status": "success",
  "message": "Lấy danh sách mẫu thành công",
  "data": {
    "content": [],
    "totalElements": 0,
    "totalPages": 0,
    "size": 10,
    "number": 0
  }
}
```

### ✅ Kiểm tra
- Public KHÔNG thấy template chưa duyệt
- Chỉ trả về template có `isActive = true`

---

## 4. ADMIN - XEM DANH SÁCH TEMPLATE CHỜ DUYỆT

### Endpoint
```http
GET /api/templates/admin/pending?page=0&size=10
```

### Headers
```
Authorization: Bearer {admin_token}
```

### Expected Response (200 OK)
```json
{
  "status": "success",
  "message": "Lấy danh sách template chờ duyệt thành công",
  "data": {
    "content": [
      {
        "templateId": "template-uuid",
        "artisanId": "artisan-uuid",
        "artisanName": "Nghệ nhân ABC",
        "name": "Tượng Đức Mẹ Maria Tùy Chỉnh",
        "description": "Tượng Đức Mẹ Maria có thể tùy chỉnh tên, lời cầu nguyện",
        "basePrice": 500000,
        "material": "Gỗ sồi",
        "style": "Cổ điển",
        "isActive": false,
        "zoneCount": 2,
        "createdAt": "2024-01-15T10:30:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 10,
    "number": 0
  }
}
```

### ✅ Kiểm tra
- Chỉ ADMIN mới truy cập được
- Hiển thị tất cả template có `isActive = false`
- Có thông tin artisan để admin biết ai tạo

---

## 5. ADMIN - DUYỆT TEMPLATE

### Endpoint
```http
PUT /api/templates/admin/{templateId}/approve
```

### Headers
```
Authorization: Bearer {admin_token}
```

### Example
```http
PUT /api/templates/admin/550e8400-e29b-41d4-a716-446655440000/approve
```

### Expected Response (200 OK)
```json
{
  "status": "success",
  "message": "Duyệt template thành công",
  "data": {
    "templateId": "550e8400-e29b-41d4-a716-446655440000",
    "artisanId": "artisan-uuid",
    "artisanName": "Nghệ nhân ABC",
    "name": "Tượng Đức Mẹ Maria Tùy Chỉnh",
    "isActive": true,
    "basePrice": 500000,
    "zoneCount": 2,
    "updatedAt": "2024-01-15T11:00:00"
  }
}
```

### ✅ Kiểm tra
- Template được set `isActive = true`
- `updatedAt` được cập nhật
- Chỉ ADMIN mới thực hiện được

---

## 6. ADMIN - TỪ CHỐI TEMPLATE

### Endpoint
```http
PUT /api/templates/admin/{templateId}/reject
```

### Headers
```
Authorization: Bearer {admin_token}
```

### Example
```http
PUT /api/templates/admin/550e8400-e29b-41d4-a716-446655440000/reject
```

### Expected Response (200 OK)
```json
{
  "status": "success",
  "message": "Từ chối template thành công",
  "data": {
    "templateId": "550e8400-e29b-41d4-a716-446655440000",
    "artisanId": "artisan-uuid",
    "artisanName": "Nghệ nhân ABC",
    "name": "Tượng Đức Mẹ Maria Tùy Chỉnh",
    "isActive": false,
    "basePrice": 500000,
    "zoneCount": 2,
    "updatedAt": "2024-01-15T11:05:00"
  }
}
```

### ✅ Kiểm tra
- Template được set `isActive = false`
- Template bị ẩn khỏi public

---

## 7. ADMIN - XEM DANH SÁCH TEMPLATE ĐÃ DUYỆT

### Endpoint
```http
GET /api/templates/admin/approved?page=0&size=10
```

### Headers
```
Authorization: Bearer {admin_token}
```

### Expected Response (200 OK)
```json
{
  "status": "success",
  "message": "Lấy danh sách template đã duyệt thành công",
  "data": {
    "content": [
      {
        "templateId": "template-uuid",
        "artisanId": "artisan-uuid",
        "artisanName": "Nghệ nhân ABC",
        "name": "Tượng Đức Mẹ Maria Tùy Chỉnh",
        "isActive": true,
        "basePrice": 500000,
        "zoneCount": 2,
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-15T11:00:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 10,
    "number": 0
  }
}
```

### ✅ Kiểm tra
- Chỉ hiển thị template có `isActive = true`
- Admin có thể quản lý tất cả template đã duyệt

---

## 8. PUBLIC - XEM TEMPLATE SAU KHI DUYỆT

### Endpoint
```http
GET /api/templates?page=0&size=10
```

### Headers
```
(Không cần authentication)
```

### Expected Response (200 OK)
```json
{
  "status": "success",
  "message": "Lấy danh sách mẫu thành công",
  "data": {
    "content": [
      {
        "templateId": "template-uuid",
        "artisanId": "artisan-uuid",
        "artisanName": "Nghệ nhân ABC",
        "name": "Tượng Đức Mẹ Maria Tùy Chỉnh",
        "isActive": true,
        "basePrice": 500000,
        "zoneCount": 2,
        "createdAt": "2024-01-15T10:30:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 10,
    "number": 0
  }
}
```

### ✅ Kiểm tra
- Sau khi admin duyệt, public đã thấy được template
- Chỉ hiển thị template có `isActive = true`

---

## 9. PUBLIC - XEM CHI TIẾT TEMPLATE

### Endpoint
```http
GET /api/templates/{templateId}
```

### Example
```http
GET /api/templates/550e8400-e29b-41d4-a716-446655440000
```

### Expected Response (200 OK)
```json
{
  "status": "success",
  "message": "Lấy chi tiết mẫu thành công",
  "data": {
    "templateId": "550e8400-e29b-41d4-a716-446655440000",
    "artisanId": "artisan-uuid",
    "artisanName": "Nghệ nhân ABC",
    "categoryId": "category-uuid",
    "name": "Tượng Đức Mẹ Maria Tùy Chỉnh",
    "description": "Tượng Đức Mẹ Maria có thể tùy chỉnh tên, lời cầu nguyện",
    "basePrice": 500000,
    "material": "Gỗ sồi",
    "style": "Cổ điển",
    "baseImages": ["https://example.com/image1.jpg"],
    "isActive": true,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T11:00:00",
    "customZones": [
      {
        "zoneId": "zone-uuid-1",
        "zoneName": "Tên người nhận",
        "zoneDescription": "Khắc tên lên đế tượng",
        "inputType": "TEXT",
        "inputConstraints": {
          "maxLength": 50,
          "minLength": 1
        },
        "extraPrice": 50000,
        "isRequired": true,
        "sortOrder": 1
      },
      {
        "zoneId": "zone-uuid-2",
        "zoneName": "Lời cầu nguyện",
        "zoneDescription": "Khắc lời cầu nguyện phía sau",
        "inputType": "TEXT",
        "inputConstraints": {
          "maxLength": 200
        },
        "extraPrice": 100000,
        "isRequired": false,
        "sortOrder": 2
      }
    ]
  }
}
```

### ✅ Kiểm tra
- Hiển thị đầy đủ thông tin template và custom zones
- Chỉ template đã duyệt mới truy cập được

---

## ERROR CASES

### 1. Non-Admin cố truy cập Admin API
```http
GET /api/templates/admin/pending
Authorization: Bearer {artisan_token}
```

**Expected Response (403 Forbidden)**
```json
{
  "status": "error",
  "message": "Access Denied"
}
```

### 2. Template không tồn tại
```http
PUT /api/templates/admin/invalid-uuid/approve
```

**Expected Response (404 Not Found)**
```json
{
  "status": "error",
  "message": "Template không tồn tại"
}
```

### 3. Public truy cập template chưa duyệt
```http
GET /api/templates/{pending-template-id}
```

**Expected Response (404 Not Found hoặc 403 Forbidden)**
```json
{
  "status": "error",
  "message": "Template không tồn tại hoặc chưa được duyệt"
}
```

---

## FLOW TEST HOÀN CHỈNH

### Bước 1: Artisan tạo template
```bash
POST /api/templates
→ isActive = false (chờ duyệt)
```

### Bước 2: Artisan xem template của mình
```bash
GET /api/templates/my-templates
→ Thấy template với isActive = false
```

### Bước 3: Public không thấy template
```bash
GET /api/templates
→ Không có template nào (vì chưa duyệt)
```

### Bước 4: Admin xem danh sách chờ duyệt
```bash
GET /api/templates/admin/pending
→ Thấy template mới tạo
```

### Bước 5: Admin duyệt template
```bash
PUT /api/templates/admin/{id}/approve
→ isActive = true
```

### Bước 6: Public đã thấy template
```bash
GET /api/templates
→ Thấy template đã duyệt
```

### Bước 7: Admin có thể từ chối lại
```bash
PUT /api/templates/admin/{id}/reject
→ isActive = false (ẩn khỏi public)
```

---

## POSTMAN COLLECTION

Để test nhanh, import collection này vào Postman:

```json
{
  "info": {
    "name": "Product Template Approval API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "1. Artisan - Create Template",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{artisan_token}}"
          }
        ],
        "url": "{{base_url}}/api/templates",
        "body": {
          "mode": "raw",
          "raw": "{\n  \"name\": \"Test Template\",\n  \"categoryId\": \"{{category_id}}\",\n  \"basePrice\": 500000,\n  \"customZones\": []\n}"
        }
      }
    },
    {
      "name": "2. Admin - Get Pending Templates",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{admin_token}}"
          }
        ],
        "url": "{{base_url}}/api/templates/admin/pending"
      }
    },
    {
      "name": "3. Admin - Approve Template",
      "request": {
        "method": "PUT",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{admin_token}}"
          }
        ],
        "url": "{{base_url}}/api/templates/admin/{{template_id}}/approve"
      }
    },
    {
      "name": "4. Public - Get Templates",
      "request": {
        "method": "GET",
        "url": "{{base_url}}/api/templates"
      }
    }
  ]
}
```

---

## CHECKLIST TESTING

- [ ] Artisan tạo template → isActive = false
- [ ] Artisan thấy template của mình (cả pending)
- [ ] Public KHÔNG thấy template pending
- [ ] Admin thấy danh sách pending
- [ ] Admin approve → isActive = true
- [ ] Public thấy template sau khi approve
- [ ] Admin reject → isActive = false
- [ ] Public không thấy template sau khi reject
- [ ] Non-admin không truy cập được admin API
- [ ] Pagination hoạt động đúng
- [ ] Error handling đúng (404, 403)

---

## LƯU Ý CHO FE

1. **Artisan Dashboard**: Hiển thị cả template pending và approved, có badge để phân biệt
2. **Admin Dashboard**: Cần 2 tab riêng cho "Chờ duyệt" và "Đã duyệt"
3. **Public**: Chỉ hiển thị template đã duyệt
4. **Notification**: Nên thông báo cho artisan khi template được duyệt/từ chối
5. **Badge/Status**: Hiển thị trạng thái rõ ràng (Chờ duyệt / Đã duyệt / Bị từ chối)

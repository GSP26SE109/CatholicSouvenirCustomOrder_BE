# 🚀 Quick Start Guide

## Chạy ứng dụng chỉ với 1 lệnh!

### Windows:
```bash
docker-compose up -d
```

Hoặc double-click file `start.bat`

### Linux/Mac:
```bash
chmod +x start.sh
./start.sh
```

Hoặc:
```bash
docker-compose up -d
```

---

## ✅ Kiểm tra ứng dụng đã chạy

Sau khoảng 30 giây, truy cập:

- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health (nếu có)

---

## 📝 Test API ngay

### 1. Register (Đăng ký tài khoản)

```bash
curl -X POST http://localhost:8080/api/authen/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Nguyen",
    "lastName": "Van A",
    "email": "test@example.com",
    "password": "password123",
    "confirmPassword": "password123",
    "phoneNumber": "0123456789",
    "gender": "Nam",
    "dateOfBirth": "1990-01-01"
  }'
```

### 2. Login (Đăng nhập)

```bash
curl -X POST http://localhost:8080/api/authen/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

Response sẽ trả về token:
```json
{
  "code": 200,
  "message": "Đăng nhập thành công",
  "data": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 3. Test Protected Endpoint

```bash
curl -X GET http://localhost:8080/api/test/protected \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 4. Logout

```bash
curl -X POST http://localhost:8080/api/authen/logout \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## 🛠️ Các lệnh hữu ích

### Xem logs
```bash
# Tất cả logs
docker-compose logs -f

# Chỉ logs của app
docker-compose logs -f app

# Chỉ logs của database
docker-compose logs -f postgres
```

### Dừng ứng dụng
```bash
docker-compose down
```

### Dừng và xóa database
```bash
docker-compose down -v
```

### Restart ứng dụng
```bash
docker-compose restart app
```

### Rebuild sau khi sửa code
```bash
docker-compose up -d --build
```

---

## 🗄️ Kết nối Database

### Thông tin kết nối:
- **Host**: localhost
- **Port**: 5432
- **Database**: catholic_souvenir
- **Username**: admin
- **Password**: admin123

### Sử dụng psql:
```bash
psql -h localhost -p 5432 -U admin -d catholic_souvenir
# Password: admin123
```

### Hoặc sử dụng GUI tools:
- DBeaver
- pgAdmin
- DataGrip
- TablePlus

---

## ❓ Troubleshooting

### Port đã được sử dụng?

Sửa port trong `docker-compose.yml`:
```yaml
app:
  ports:
    - "8081:8080"  # Đổi 8080 thành 8081

postgres:
  ports:
    - "5433:5432"  # Đổi 5432 thành 5433
```

### Database chưa sẵn sàng?

Đợi thêm 10-20 giây, sau đó restart:
```bash
docker-compose restart app
```

### Xem chi tiết lỗi?

```bash
docker-compose logs app
```

---

## 📚 Tài liệu đầy đủ

- [API Documentation](API_AUTHENTICATION.md)
- [Docker Setup Guide](README_DOCKER.md)

---

## 🎉 Xong!

Bây giờ bạn có thể:
1. ✅ Đăng ký tài khoản
2. ✅ Đăng nhập và nhận JWT token
3. ✅ Truy cập các protected endpoints
4. ✅ Đăng xuất

Chúc bạn code vui vẻ! 🚀

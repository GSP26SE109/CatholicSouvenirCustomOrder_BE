# Hướng Dẫn Reset Database

## 📋 Tổng Quan

Repository này cung cấp nhiều cách để reset database PostgreSQL trên server Azure VM với Docker Compose.

---

## 🎯 Phương Pháp 1: Sử Dụng DBeaver (KHUYẾN NGHỊ - ĐƠN GIẢN NHẤT)

### Bước 1: Kết nối DBeaver đến Database

Trong DBeaver, tạo kết nối mới với thông tin:
- **Host**: IP Azure VM của bạn
- **Port**: 5432
- **Database**: catholic_souvenir (hoặc tên trong file .env)
- **Username**: postgres (hoặc giá trị DB_USER trong .env)
- **Password**: Lấy từ file .env trên server

### Bước 2: Chạy Script Reset

1. Mở file `scripts/reset-for-dbeaver.sql` trong DBeaver
2. Chọn toàn bộ script (Ctrl+A)
3. Execute script (Ctrl+Enter hoặc Alt+X)
4. Đợi script chạy xong

### Bước 3: Restart Backend

SSH vào Azure VM và chạy:
```bash
cd /path/to/your/project
docker-compose restart backend
```

### Bước 4: Kiểm Tra

```bash
# Xem logs để đảm bảo Hibernate tạo lại bảng
docker-compose logs -f backend
```

---

## 🐳 Phương Pháp 2: Sử Dụng Script Bash trên Server

### Cách 1: Reset Schema (Nhanh - An toàn)

SSH vào Azure VM:

```bash
cd /path/to/your/project

# Cấp quyền thực thi
chmod +x scripts/reset-docker-db.sh

# Chạy script
./scripts/reset-docker-db.sh
```

Chọn option **1** (Drop và tạo lại schema)

### Cách 2: Reset Database Hoàn Toàn

Chạy script và chọn option **2** (Drop và tạo lại database)

### Cách 3: Reset Volume (Xóa Tất Cả)

Chạy script và chọn option **3** (Drop volume và tạo mới)

---

## 🔧 Phương Pháp 3: Manual Commands

### Option A: Sử dụng Docker Exec

```bash
# SSH vào Azure VM
cd /path/to/your/project

# Chạy SQL command trực tiếp
docker exec -i catholic-souvenir-postgres psql -U postgres -d catholic_souvenir << 'EOF'
DROP SCHEMA IF EXISTS public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
EOF

# Restart backend
docker-compose restart backend
```

### Option B: Drop và Recreate Volume

```bash
# Dừng containers
docker-compose stop backend postgres

# Xóa volume
docker-compose down
docker volume rm $(docker volume ls -q | grep postgres_data)

# Khởi động lại
docker-compose up -d
```

---

## 📝 So Sánh Các Phương Pháp

| Phương Pháp | Tốc Độ | Độ An Toàn | Dễ Sử Dụng | Khuyến Nghị |
|-------------|--------|------------|------------|-------------|
| DBeaver SQL | ⚡⚡⚡ Nhanh | ✅ An toàn | ⭐⭐⭐ Dễ | ✅ Khuyến nghị |
| Bash Script | ⚡⚡ Trung bình | ✅ An toàn | ⭐⭐ Trung bình | ✅ Tốt |
| Docker Volume | ⚡ Chậm | ⚠️ Xóa hết | ⭐ Khó | ⚠️ Cẩn thận |

---

## ⚠️ Lưu Ý Quan Trọng

### Trước Khi Reset:

1. **Backup dữ liệu** nếu cần:
   ```bash
   docker exec catholic-souvenir-postgres pg_dump -U postgres catholic_souvenir > backup.sql
   ```

2. **Thông báo team** nếu đang làm việc chung

3. **Kiểm tra môi trường**: Đảm bảo đang reset đúng database (dev/staging)

### Sau Khi Reset:

1. **Restart backend container**:
   ```bash
   docker-compose restart backend
   ```

2. **Kiểm tra logs**:
   ```bash
   docker-compose logs -f backend
   ```

3. **Verify tables được tạo**:
   - Mở DBeaver
   - Refresh connection
   - Kiểm tra danh sách tables

4. **Test API**: Gọi một vài endpoints để đảm bảo hoạt động bình thường

---

## 🔍 Troubleshooting

### Lỗi: "permission denied"

```bash
chmod +x scripts/reset-docker-db.sh
```

### Lỗi: "container not found"

```bash
# Kiểm tra container đang chạy
docker ps

# Khởi động lại nếu cần
docker-compose up -d postgres
```

### Lỗi: "database is being accessed by other users"

```bash
# Stop backend trước
docker-compose stop backend

# Sau đó chạy lại script reset
```

### Backend không tạo lại bảng

Kiểm tra `application.yml` hoặc environment variable:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # Phải là 'update' hoặc 'create'
```

---

## 📞 Hỗ Trợ

Nếu gặp vấn đề, kiểm tra:
1. Logs của backend: `docker-compose logs backend`
2. Logs của postgres: `docker-compose logs postgres`
3. File .env có đúng thông tin không
4. Network connection từ backend đến postgres

---

## 🎓 Best Practices

1. **Development**: Dùng DBeaver SQL script (nhanh, dễ)
2. **Staging**: Dùng bash script với option 1 (an toàn)
3. **Production**: ⚠️ KHÔNG BAO GIỜ reset! Dùng migration tools như Flyway/Liquibase

---

## 📚 Files Trong Thư Mục Scripts

- `reset-for-dbeaver.sql` - Script SQL cho DBeaver (KHUYẾN NGHỊ)
- `reset-docker-db.sh` - Script bash tự động cho Docker
- `reset-schema.sql` - Script SQL reset schema
- `reset-database.sql` - Script SQL reset database
- `reset-db.sh` - Script bash cho PostgreSQL thông thường
- `README.md` - File này

---

**Lưu ý**: Luôn đảm bảo bạn đang làm việc trên môi trường development/staging, KHÔNG BAO GIỜ chạy trên production!

# Docker Setup Guide

## Yêu cầu
- Docker Desktop đã cài đặt
- Docker Compose đã cài đặt

## Cách chạy

### 1. Chạy toàn bộ ứng dụng (Database + Backend)

```bash
docker-compose up -d
```

Lệnh này sẽ:
- Tạo PostgreSQL database container
- Build và chạy Spring Boot application
- Tự động tạo các bảng và insert default roles

### 2. Kiểm tra trạng thái containers

```bash
docker-compose ps
```

### 3. Xem logs

```bash
# Xem logs của tất cả services
docker-compose logs -f

# Xem logs của app
docker-compose logs -f app

# Xem logs của database
docker-compose logs -f postgres
```

### 4. Dừng ứng dụng

```bash
docker-compose down
```

### 5. Dừng và xóa volumes (xóa cả database)

```bash
docker-compose down -v
```

---

## Thông tin kết nối

### Application
- **URL**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html

### PostgreSQL Database
- **Host**: localhost
- **Port**: 5432
- **Database**: catholic_souvenir
- **Username**: admin
- **Password**: admin123

### Kết nối database từ máy local

```bash
psql -h localhost -p 5432 -U admin -d catholic_souvenir
# Password: admin123
```

Hoặc sử dụng DBeaver, pgAdmin, DataGrip với thông tin trên.

---

## Rebuild application

Nếu có thay đổi code, rebuild lại:

```bash
docker-compose up -d --build
```

---

## Troubleshooting

### Port đã được sử dụng

Nếu port 8080 hoặc 5432 đã được sử dụng, sửa trong `docker-compose.yml`:

```yaml
ports:
  - "8081:8080"  # Đổi port 8080 thành 8081
```

### Database không khởi động

Kiểm tra logs:
```bash
docker-compose logs postgres
```

### Application không kết nối được database

Đợi database khởi động hoàn toàn (khoảng 10-20 giây), sau đó restart app:
```bash
docker-compose restart app
```

---

## Development Mode

Để chạy chỉ database (develop app trên máy local):

```bash
# Chỉ chạy postgres
docker-compose up -d postgres

# Chạy Spring Boot app trên máy local
mvn spring-boot:run
```

---

## Production Notes

Để deploy production, nên:

1. Thay đổi password database trong `docker-compose.yml`
2. Thay đổi JWT secret key
3. Set `SPRING_JPA_SHOW_SQL=false`
4. Sử dụng `ddl-auto: validate` thay vì `update`
5. Thêm volume backup cho database
6. Sử dụng environment file (`.env`) thay vì hardcode

### Ví dụ sử dụng .env file:

Tạo file `.env`:
```env
POSTGRES_DB=catholic_souvenir
POSTGRES_USER=admin
POSTGRES_PASSWORD=your-secure-password
JWT_KEY=your-secure-jwt-key
```

Sửa `docker-compose.yml`:
```yaml
environment:
  POSTGRES_DB: ${POSTGRES_DB}
  POSTGRES_USER: ${POSTGRES_USER}
  POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
```

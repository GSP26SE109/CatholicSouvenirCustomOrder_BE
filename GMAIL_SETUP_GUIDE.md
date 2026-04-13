# Hướng dẫn Setup Gmail cho Email Verification

## Bước 1: Tạo App Password cho Gmail

### 1.1. Bật 2-Step Verification
1. Vào https://myaccount.google.com/security
2. Tìm "2-Step Verification"
3. Click "Get started" và làm theo hướng dẫn
4. Xác thực bằng số điện thoại

### 1.2. Tạo App Password
1. Sau khi bật 2-Step Verification
2. Vào https://myaccount.google.com/apppasswords
3. Hoặc search "App passwords" trong Google Account
4. Chọn "Mail" và "Other (Custom name)"
5. Nhập tên: "Catholic Souvenir App"
6. Click "Generate"
7. Copy password 16 ký tự (dạng: xxxx xxxx xxxx xxxx)

## Bước 2: Set Environment Variables

### Windows (PowerShell)
```powershell
$env:MAIL_USERNAME="your-email@gmail.com"
$env:MAIL_PASSWORD="your-16-char-app-password"
$env:APP_BASE_URL="http://localhost:8080"
```

### Windows (CMD)
```cmd
set MAIL_USERNAME=your-email@gmail.com
set MAIL_PASSWORD=your-16-char-app-password
set APP_BASE_URL=http://localhost:8080
```

### Linux/Mac
```bash
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="your-16-char-app-password"
export APP_BASE_URL="http://localhost:8080"
```

### IntelliJ IDEA
1. Run → Edit Configurations
2. Chọn Spring Boot application
3. Environment variables:
```
MAIL_USERNAME=your-email@gmail.com;MAIL_PASSWORD=your-16-char-app-password;APP_BASE_URL=http://localhost:8080
```

## Bước 3: Test Email

### 3.1. Start Application
```bash
mvn spring-boot:run
```

### 3.2. Register Account
```bash
POST http://localhost:8080/api/authen/register
Content-Type: application/json

{
  "firstName": "Test",
  "lastName": "User",
  "email": "test@example.com",
  "password": "password123",
  "confirmPassword": "password123",
  "phoneNumber": "0123456789",
  "gender": "Male",
  "dateOfBirth": "1990-01-01"
}
```

### 3.3. Check Email
- Kiểm tra inbox của test@example.com
- Tìm email từ your-email@gmail.com
- Click link verify

### 3.4. Login
```bash
POST http://localhost:8080/api/authen/login
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "password123"
}
```

## Troubleshooting

### Lỗi: Authentication failed
- Kiểm tra lại email và app password
- Đảm bảo đã bật 2-Step Verification
- Tạo lại app password mới

### Lỗi: Connection timeout
- Kiểm tra firewall/antivirus
- Thử đổi port từ 587 sang 465 (SSL)
- Kiểm tra internet connection

### Email không gửi được
- Check logs trong console
- Verify MAIL_USERNAME và MAIL_PASSWORD đã set đúng
- Thử gửi email test bằng Gmail web để đảm bảo account hoạt động

## Production Deployment

Khi deploy lên server, set environment variables:

```bash
# Heroku
heroku config:set MAIL_USERNAME=your-email@gmail.com
heroku config:set MAIL_PASSWORD=your-app-password
heroku config:set APP_BASE_URL=https://your-app.herokuapp.com

# Docker
docker run -e MAIL_USERNAME=your-email@gmail.com \
           -e MAIL_PASSWORD=your-app-password \
           -e APP_BASE_URL=https://your-domain.com \
           your-image

# AWS/Azure
# Set trong Environment Variables của service
```

## Security Notes

⚠️ QUAN TRỌNG:
- KHÔNG commit app password vào Git
- KHÔNG share app password với ai
- Dùng environment variables cho production
- Revoke app password nếu bị lộ
- Tạo app password riêng cho mỗi môi trường (dev/staging/prod)

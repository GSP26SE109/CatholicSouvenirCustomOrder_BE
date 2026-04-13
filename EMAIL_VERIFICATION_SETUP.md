# Email Verification Setup Guide

## 1. Thêm dependency vào pom.xml

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

## 2. Thêm config vào application.yml

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME:your-email@gmail.com}
    password: ${MAIL_PASSWORD:your-app-password}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000

app:
  base-url: ${APP_BASE_URL:http://localhost:8080}
```

## 3. Tạo App Password cho Gmail

1. Vào https://myaccount.google.com/security
2. Bật "2-Step Verification"
3. Vào "App passwords"
4. Tạo password mới cho "Mail"
5. Copy password và dùng làm `MAIL_PASSWORD`

## 4. API Endpoints

### Register
```
POST /api/authen/register
Body: {
  "firstName": "John",
  "lastName": "Doe",
  "email": "user@example.com",
  "password": "password123",
  "confirmPassword": "password123",
  "phoneNumber": "0123456789",
  "gender": "Male",
  "dateOfBirth": "1990-01-01"
}

Response: {
  "code": 200,
  "message": "Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản.",
  "data": {
    "accountId": "uuid",
    "fullName": "John Doe",
    "email": "user@example.com",
    "phone": "0123456789"
  }
}
```

### Verify Email
```
GET /api/authen/verify?token=verification-token

Response: {
  "code": 200,
  "message": "Xác thực email thành công. Bạn có thể đăng nhập ngay bây giờ."
}
```

### Resend Verification Email
```
POST /api/authen/resend-verification?email=user@example.com

Response: {
  "code": 200,
  "message": "Email xác thực đã được gửi lại. Vui lòng kiểm tra hộp thư."
}
```

### Login (requires verified email)
```
POST /api/authen/login
Body: {
  "email": "user@example.com",
  "password": "password123"
}

Response (if not verified): {
  "code": 400,
  "message": "Tài khoản chưa được xác thực. Vui lòng kiểm tra email."
}

Response (if verified): {
  "code": 200,
  "message": "Đăng nhập thành công",
  "data": {
    "accountId": "uuid",
    "email": "user@example.com",
    "roleName": "CUSTOMER",
    "token": "jwt-token"
  }
}
```

## 5. Testing Flow

1. Register account → Email sent
2. Check email → Click verification link
3. Account verified → Can login
4. If email not received → Use resend endpoint

## 6. Production Deployment

Đặt biến môi trường:
```bash
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
APP_BASE_URL=https://your-domain.com
```

## 7. Email Template

Email sẽ có dạng:
```
Subject: Xác thực tài khoản - Catholic Souvenir

Chào bạn,

Cảm ơn bạn đã đăng ký tài khoản tại Catholic Souvenir.

Vui lòng click vào link dưới đây để xác thực tài khoản:
http://localhost:8080/api/authen/verify?token=xxx-xxx-xxx

Link này sẽ hết hạn sau 24 giờ.

Nếu bạn không đăng ký tài khoản này, vui lòng bỏ qua email này.

Trân trọng,
Catholic Souvenir Team
```

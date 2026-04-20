#!/bin/bash

# =====================================================
# Script để reset PostgreSQL database trong Docker
# Dành cho môi trường Docker Compose trên Azure VM
# =====================================================

set -e

echo "=========================================="
echo "RESET DATABASE - DOCKER COMPOSE"
echo "=========================================="
echo ""
echo "CẢNH BÁO: Script này sẽ XÓA TOÀN BỘ DỮ LIỆU!"
echo ""
read -p "Bạn có chắc chắn muốn tiếp tục? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Đã hủy thao tác."
    exit 0
fi

# Load environment variables từ .env file
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo "✓ Đã load environment variables từ .env"
else
    echo "✗ Không tìm thấy file .env"
    exit 1
fi

CONTAINER_NAME="catholic-souvenir-postgres"

echo ""
echo "Đang kiểm tra container PostgreSQL..."

# Kiểm tra container có đang chạy không
if ! docker ps | grep -q $CONTAINER_NAME; then
    echo "✗ Container $CONTAINER_NAME không chạy"
    echo "Khởi động lại containers..."
    docker-compose up -d postgres
    sleep 5
fi

echo "✓ Container đang chạy"
echo ""
echo "Chọn phương thức reset:"
echo "1. Drop và tạo lại schema (Khuyến nghị - Nhanh, an toàn)"
echo "2. Drop và tạo lại database (Chậm hơn)"
echo "3. Drop volume và tạo mới hoàn toàn (Xóa tất cả, bao gồm cả backup)"
read -p "Nhập lựa chọn (1/2/3): " method

case $method in
    1)
        echo ""
        echo "Đang reset schema..."
        docker exec -i $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME << 'EOF'
-- Drop schema và tạo lại
DROP SCHEMA IF EXISTS public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
SELECT 'Schema reset successfully!' as status;
EOF
        ;;
    2)
        echo ""
        echo "Đang reset database..."
        docker exec -i $CONTAINER_NAME psql -U $DB_USER -d postgres << EOF
-- Ngắt kết nối
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = '$DB_NAME'
  AND pid <> pg_backend_pid();

-- Drop và tạo lại database
DROP DATABASE IF EXISTS $DB_NAME;
CREATE DATABASE $DB_NAME WITH OWNER = $DB_USER ENCODING = 'UTF8';
SELECT 'Database reset successfully!' as status;
EOF
        ;;
    3)
        echo ""
        echo "Đang dừng containers..."
        docker-compose stop backend postgres
        
        echo "Đang xóa volume PostgreSQL..."
        docker-compose down
        docker volume rm $(docker volume ls -q | grep postgres_data) 2>/dev/null || true
        
        echo "Đang khởi động lại containers..."
        docker-compose up -d postgres redis
        
        echo "Chờ PostgreSQL khởi động..."
        sleep 10
        ;;
    *)
        echo "Lựa chọn không hợp lệ."
        exit 1
        ;;
esac

if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "✓ Reset database thành công!"
    echo "=========================================="
    echo ""
    echo "Bước tiếp theo:"
    echo "1. Restart backend container:"
    echo "   docker-compose restart backend"
    echo ""
    echo "2. Kiểm tra logs:"
    echo "   docker-compose logs -f backend"
    echo ""
    echo "3. Hibernate sẽ tự động tạo lại các bảng"
    echo ""
else
    echo ""
    echo "✗ Có lỗi xảy ra khi reset database"
    exit 1
fi

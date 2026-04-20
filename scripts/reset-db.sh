#!/bin/bash

# =====================================================
# Script để reset database trên server
# =====================================================

echo "=========================================="
echo "CẢNH BÁO: Script này sẽ XÓA TOÀN BỘ DỮ LIỆU"
echo "=========================================="
echo ""
read -p "Bạn có chắc chắn muốn tiếp tục? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Đã hủy thao tác."
    exit 0
fi

# Lấy thông tin từ environment variables hoặc sử dụng giá trị mặc định
DB_HOST="${POSTGRES_HOST:-localhost}"
DB_PORT="${POSTGRES_PORT:-5432}"
DB_NAME="${POSTGRES_DB:-catholic_souvenir}"
DB_USER="${POSTGRES_USER:-postgres}"

echo ""
echo "Đang kết nối đến database..."
echo "Host: $DB_HOST"
echo "Port: $DB_PORT"
echo "Database: $DB_NAME"
echo "User: $DB_USER"
echo ""

# Chọn phương thức reset
echo "Chọn phương thức reset:"
echo "1. Drop và tạo lại schema (Khuyến nghị - An toàn hơn)"
echo "2. Drop và tạo lại database (Cần quyền superuser)"
read -p "Nhập lựa chọn (1 hoặc 2): " method

if [ "$method" == "1" ]; then
    echo "Đang thực thi reset schema..."
    PGPASSWORD=$POSTGRES_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f reset-schema.sql
elif [ "$method" == "2" ]; then
    echo "Đang thực thi reset database..."
    PGPASSWORD=$POSTGRES_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d postgres -f reset-database.sql
else
    echo "Lựa chọn không hợp lệ."
    exit 1
fi

if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "✓ Reset database thành công!"
    echo "=========================================="
    echo ""
    echo "Bước tiếp theo:"
    echo "1. Restart ứng dụng Spring Boot"
    echo "2. Hibernate sẽ tự động tạo lại các bảng"
    echo ""
else
    echo ""
    echo "✗ Có lỗi xảy ra khi reset database"
    exit 1
fi

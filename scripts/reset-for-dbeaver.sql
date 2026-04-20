-- =====================================================
-- SCRIPT RESET DATABASE CHO DBEAVER
-- Chạy trực tiếp trong DBeaver SQL Editor
-- =====================================================
-- 
-- CẢNH BÁO: Script này sẽ XÓA TOÀN BỘ DỮ LIỆU!
--
-- HƯỚNG DẪN SỬ DỤNG:
-- 1. Mở DBeaver và kết nối đến database
-- 2. Mở file này trong SQL Editor (Ctrl+O)
-- 3. Chọn toàn bộ script (Ctrl+A)
-- 4. Execute (Ctrl+Enter hoặc Alt+X)
-- 5. Restart backend container sau khi chạy xong
-- =====================================================

-- Bước 1: Drop tất cả schema và objects
DROP SCHEMA IF EXISTS public CASCADE;

-- Bước 2: Tạo lại schema public
CREATE SCHEMA public;

-- Bước 3: Grant quyền
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;

-- Bước 4: Tạo extension cần thiết
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Bước 5: Verify
SELECT 
    'Database schema has been reset successfully!' as status,
    current_database() as database_name,
    current_user as current_user,
    now() as reset_time;

-- =====================================================
-- KẾT QUẢ MONG ĐỢI:
-- - Tất cả bảng đã bị xóa
-- - Schema public đã được tạo lại
-- - Extension uuid-ossp đã được cài đặt
-- 
-- BƯỚC TIẾP THEO:
-- 1. Restart backend container:
--    docker-compose restart backend
-- 
-- 2. Kiểm tra logs:
--    docker-compose logs -f backend
-- 
-- 3. Hibernate sẽ tự động tạo lại các bảng
-- =====================================================

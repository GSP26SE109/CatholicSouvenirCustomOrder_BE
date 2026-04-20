-- =====================================================
-- Script để DROP và TẠO LẠI schema public
-- An toàn hơn vì không cần drop database
-- CẢNH BÁO: Script này sẽ XÓA TOÀN BỘ DỮ LIỆU
-- =====================================================

-- Drop schema public và tất cả objects bên trong
DROP SCHEMA IF EXISTS public CASCADE;

-- Tạo lại schema public
CREATE SCHEMA public;

-- Grant quyền
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;

-- Tạo extension nếu cần
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Hiển thị thông báo
SELECT 'Database schema has been reset successfully!' as status;

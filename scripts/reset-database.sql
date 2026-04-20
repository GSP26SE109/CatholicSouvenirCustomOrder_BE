-- =====================================================
-- Script để DROP và TẠO LẠI toàn bộ database
-- Sử dụng cho môi trường DEVELOPMENT/STAGING
-- CẢNH BÁO: Script này sẽ XÓA TOÀN BỘ DỮ LIỆU
-- =====================================================

-- Ngắt kết nối tất cả các session đang kết nối đến database
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = 'catholic_souvenir'
  AND pid <> pg_backend_pid();

-- Drop database nếu tồn tại
DROP DATABASE IF EXISTS catholic_souvenir;

-- Tạo lại database
CREATE DATABASE catholic_souvenir
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- Kết nối vào database mới
\c catholic_souvenir

-- Tạo extension nếu cần
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

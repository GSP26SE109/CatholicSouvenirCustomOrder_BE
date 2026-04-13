# Database Migration Guide - Notification Actions Update

## Mục đích
Update database constraints để phù hợp với luồng mới (không còn QUOTATION).

## Thay đổi

### NotificationAction
- ❌ Xóa: `VIEW_QUOTATION` (luồng cũ)
- ✅ Thêm: `APPROVE_STAGE`, `VIEW_NOTIFICATION`

### RelatedEntityType  
- ❌ Xóa: `QUOTATION` (luồng cũ)
- ✅ Thêm: `NOTIFICATION`

## Cách chạy Migration trên Azure VM

### Bước 1: Backup Database
```bash
# SSH vào Azure VM
ssh [your-vm-user]@[your-vm-ip]

# Backup database
pg_dump -U [db-user] -d [db-name] > backup_before_migration_$(date +%Y%m%d_%H%M%S).sql
```

### Bước 2: Upload Migration Script
```bash
# Từ local machine, upload file lên VM
scp database_migration_notification_actions.sql [your-vm-user]@[your-vm-ip]:~/
```

### Bước 3: Chạy Migration
```bash
# Trên Azure VM, connect vào PostgreSQL
psql -U [db-user] -d [db-name] -f database_migration_notification_actions.sql

# Hoặc chạy trực tiếp
psql -U [db-user] -d [db-name] << EOF
\i database_migration_notification_actions.sql
EOF
```

### Bước 4: Verify
```sql
-- Check constraints
SELECT conname, pg_get_constraintdef(oid) 
FROM pg_constraint 
WHERE conrelid = 'notifications'::regclass 
AND conname LIKE '%check%';

-- Check data
SELECT action_type, COUNT(*) FROM notifications GROUP BY action_type;
SELECT related_entity_type, COUNT(*) FROM notifications GROUP BY related_entity_type;
```

### Bước 5: Restart Application
```bash
# Restart Spring Boot app
sudo systemctl restart [your-app-service]

# Hoặc nếu dùng Docker
docker restart [container-name]
```

## Rollback (nếu cần)

Nếu có vấn đề, chạy rollback script:
```bash
psql -U [db-user] -d [db-name] -f database_rollback_notification_actions.sql
```

## Lưu ý

1. **Backup trước khi chạy migration!**
2. Migration sẽ tự động convert data cũ:
   - `VIEW_QUOTATION` → `VIEW_REQUEST`
   - `QUOTATION` entity type → `CUSTOM_REQUEST`
3. Sau khi chạy migration, phải restart app để áp dụng thay đổi
4. Test kỹ trên staging trước khi chạy production

## Troubleshooting

### Lỗi: constraint violation
```sql
-- Kiểm tra records có giá trị không hợp lệ
SELECT * FROM notifications WHERE action_type NOT IN (
    'NONE', 'ACCEPT_REQUEST', 'REJECT_REQUEST', 'VIEW_REQUEST',
    'CONFIRM_ARTISAN', 'VIEW_ORDER', 'PAY_STAGE', 'COMPLETE_STAGE',
    'APPROVE_STAGE', 'VIEW_CONVERSATION', 'VIEW_NOTIFICATION'
);
```

### Lỗi: permission denied
```bash
# Đảm bảo user có quyền ALTER TABLE
GRANT ALL PRIVILEGES ON TABLE notifications TO [db-user];
```

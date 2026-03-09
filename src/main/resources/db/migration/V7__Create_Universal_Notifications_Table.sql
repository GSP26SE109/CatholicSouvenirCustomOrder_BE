-- Create universal notifications table
CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Who
    recipient_id UUID NOT NULL,
    
    -- What
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    
    -- Where (Related Entity)
    related_entity_id UUID,
    related_entity_type VARCHAR(50),
    
    -- Status
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    
    -- Action
    action_type VARCHAR(50),
    action_required BOOLEAN NOT NULL DEFAULT FALSE,
    action_completed BOOLEAN NOT NULL DEFAULT FALSE,
    action_completed_at TIMESTAMP,
    
    -- Metadata
    metadata TEXT,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) 
        REFERENCES accounts(account_id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_notifications_recipient ON notifications(recipient_id);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_priority ON notifications(priority);

-- Composite indexes for common queries
CREATE INDEX idx_notifications_recipient_unread 
    ON notifications(recipient_id, is_read) 
    WHERE is_read = FALSE;

CREATE INDEX idx_notifications_actionable 
    ON notifications(recipient_id, action_required, action_completed) 
    WHERE action_required = TRUE AND action_completed = FALSE;

CREATE INDEX idx_notifications_recipient_type 
    ON notifications(recipient_id, type, created_at DESC);

-- Add comments
COMMENT ON TABLE notifications IS 'Universal notification system for all users and events';
COMMENT ON COLUMN notifications.type IS 'Type of notification: NEW_QUOTATION, ORDER_CREATED, NEW_CUSTOM_REQUEST, etc.';
COMMENT ON COLUMN notifications.action_type IS 'Required action: ACCEPT_REQUEST, PAY_STAGE, VIEW_QUOTATION, etc.';
COMMENT ON COLUMN notifications.action_required IS 'Does this notification require user action?';
COMMENT ON COLUMN notifications.action_completed IS 'Has the user completed the required action?';
COMMENT ON COLUMN notifications.metadata IS 'JSON data for additional notification information';
COMMENT ON COLUMN notifications.priority IS 'Notification priority: LOW, NORMAL, HIGH, URGENT';

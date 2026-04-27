-- Add order_detail_id column to feedbacks table
ALTER TABLE feedbacks ADD COLUMN order_detail_id UUID;

-- Add foreign key constraint
ALTER TABLE feedbacks ADD CONSTRAINT fk_feedback_order_detail 
    FOREIGN KEY (order_detail_id) REFERENCES order_detail(id) ON DELETE CASCADE;

-- Add index for better query performance
CREATE INDEX idx_feedback_order_detail ON feedbacks(order_detail_id);

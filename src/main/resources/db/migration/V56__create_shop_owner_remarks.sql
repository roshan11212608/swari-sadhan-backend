CREATE TABLE shop_owner_remarks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_owner_id BIGINT NOT NULL,
    remark TEXT NOT NULL,
    admin_user_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_remarks_shop_owner FOREIGN KEY (shop_owner_id) REFERENCES shop_owners(id) ON DELETE CASCADE
);

CREATE INDEX idx_remarks_shop_owner_id ON shop_owner_remarks(shop_owner_id);

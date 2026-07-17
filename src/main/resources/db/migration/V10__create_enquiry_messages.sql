-- Conversation messages for enquiries.
CREATE TABLE IF NOT EXISTS enquiry_messages (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    enquiry_id   BIGINT       NOT NULL,
    sender       VARCHAR(20)  NOT NULL,
    sender_name  VARCHAR(255),
    message      TEXT         NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_em_enquiry_id (enquiry_id),
    CONSTRAINT fk_em_enquiry FOREIGN KEY (enquiry_id) REFERENCES enquiries (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

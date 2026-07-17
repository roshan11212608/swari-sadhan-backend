-- Sync enquiries.status enum with EnquiryStatus Java enum values
ALTER TABLE enquiries MODIFY COLUMN status ENUM('PENDING', 'IN_PROGRESS', 'RESPONDED', 'CONTACTED', 'CLOSED', 'RESOLVED') DEFAULT 'PENDING';

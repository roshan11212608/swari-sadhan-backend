
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `advance_payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `advance_payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint NOT NULL,
  `employee_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `employee_photo_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `designation` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `department` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `advance_amount` decimal(38,2) NOT NULL,
  `reason` text COLLATE utf8mb4_unicode_ci,
  `date` date NOT NULL,
  `recovery_method` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `monthly_deduction` decimal(38,2) DEFAULT NULL,
  `recovered_amount` decimal(38,2) DEFAULT NULL,
  `remaining_balance` decimal(38,2) NOT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Pending',
  `approved_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approved_date` date DEFAULT NULL,
  `rejection_reason` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL DEFAULT NULL,
  `version` bigint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_status` (`status`),
  KEY `idx_date` (`date`),
  KEY `idx_advance_payments_deleted_at` (`deleted_at`),
  KEY `idx_advance_payments_version` (`version`),
  CONSTRAINT `fk_advance_payments_employee` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_advance_amount_positive` CHECK ((`advance_amount` > 0)),
  CONSTRAINT `chk_advance_recovered_non_negative` CHECK ((`recovered_amount` >= 0)),
  CONSTRAINT `chk_advance_remaining_non_negative` CHECK ((`remaining_balance` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `attendance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint NOT NULL,
  `employee_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `date` date NOT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `clock_in` time DEFAULT NULL,
  `clock_out` time DEFAULT NULL,
  `working_hours` decimal(38,2) DEFAULT NULL,
  `overtime` decimal(38,2) DEFAULT NULL,
  `notes` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL DEFAULT NULL,
  `reason` text COLLATE utf8mb4_unicode_ci,
  `version` bigint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_employee_date` (`employee_id`,`date`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_date` (`date`),
  KEY `idx_status` (`status`),
  KEY `idx_attendance_deleted_at` (`deleted_at`),
  KEY `idx_attendance_version` (`version`),
  CONSTRAINT `fk_attendance_employee` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=103 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `image_url` varchar(255) DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKt8o6pivur7nn124jehx7cygw5` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `employees`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employees` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_number` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `full_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `gender` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `date_of_birth` date NOT NULL,
  `father_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `marital_status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `mobile_number` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `current_address` text COLLATE utf8mb4_unicode_ci,
  `joining_date` date NOT NULL,
  `department` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `designation` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `employment_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `basic_salary` decimal(38,2) NOT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Active',
  `bank_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `account_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ifsc_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `emergency_contact` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `emergency_contact_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `profile_photo_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shop_id` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `employee_number` (`employee_number`),
  KEY `idx_employee_number` (`employee_number`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_status` (`status`),
  KEY `idx_department` (`department`),
  CONSTRAINT `fk_employees_shop` FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_employee_basic_salary_positive` CHECK ((`basic_salary` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `enquiries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enquiries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_notes` text,
  `budget_range` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `customer_email` varchar(255) NOT NULL,
  `customer_name` varchar(255) NOT NULL,
  `customer_phone` varchar(255) DEFAULT NULL,
  `expected_purchase_time` varchar(255) DEFAULT NULL,
  `financing_required` bit(1) DEFAULT NULL,
  `message` text,
  `preferred_contact_method` varchar(255) DEFAULT NULL,
  `responded_at` datetime(6) DEFAULT NULL,
  `response` text,
  `status` enum('PENDING','IN_PROGRESS','RESPONDED','CONTACTED','CLOSED','RESOLVED') DEFAULT 'PENDING',
  `test_drive_requested` bit(1) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `customer_id` bigint DEFAULT NULL,
  `shop_id` bigint NOT NULL,
  `vehicle_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK966p4d9ami88jmpxtfthabhn2` (`customer_id`),
  KEY `FKhxsobykbskme558l3rpa6cusr` (`shop_id`),
  KEY `FKgg9p1ofx69crcqhr5et2akeh1` (`vehicle_id`),
  CONSTRAINT `FK966p4d9ami88jmpxtfthabhn2` FOREIGN KEY (`customer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKgg9p1ofx69crcqhr5et2akeh1` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicles` (`id`),
  CONSTRAINT `FKhxsobykbskme558l3rpa6cusr` FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `enquiry_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enquiry_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `message` text NOT NULL,
  `sender` enum('CUSTOMER','SHOP_OWNER') NOT NULL,
  `sender_name` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `enquiry_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKldugteeg5gcsgp4w9lgittvju` (`enquiry_id`),
  CONSTRAINT `FKldugteeg5gcsgp4w9lgittvju` FOREIGN KEY (`enquiry_id`) REFERENCES `enquiries` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `expense_attachments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `expense_attachments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `expense_id` bigint NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_path` varchar(255) NOT NULL,
  `file_size` bigint NOT NULL,
  `file_type` varchar(255) NOT NULL,
  `uploaded_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_attachment_expense` (`expense_id`),
  CONSTRAINT `fk_attachment_expense` FOREIGN KEY (`expense_id`) REFERENCES `expenses` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `expense_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `expense_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `color` varchar(255) NOT NULL,
  `icon` varchar(255) NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `idx_ec_name` (`name`),
  KEY `idx_ec_active` (`is_active`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `expenses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `expenses` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `expense_number` varchar(255) NOT NULL,
  `shop_id` bigint NOT NULL,
  `title` varchar(255) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `expense_date` date NOT NULL,
  `description` text,
  `notes` text,
  `vendor_paid_to` varchar(255) DEFAULT NULL,
  `payment_method` enum('CASH','BANK_TRANSFER','UPI','CARD','CHEQUE') NOT NULL,
  `payment_status` enum('PENDING','PAID','PARTIALLY_PAID') NOT NULL,
  `reference_number` varchar(255) DEFAULT NULL,
  `due_date` date DEFAULT NULL,
  `attachment_path` varchar(255) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` varchar(255) NOT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  `category_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `expense_number` (`expense_number`),
  UNIQUE KEY `idx_expense_number` (`expense_number`),
  KEY `idx_expense_shop` (`shop_id`),
  KEY `idx_expense_status` (`payment_status`),
  KEY `idx_expense_date` (`expense_date`),
  KEY `idx_expense_active` (`is_active`),
  KEY `FKg7aulw52en8nct0mjq8uut03q` (`category_id`),
  CONSTRAINT `fk_expense_shop` FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKg7aulw52en8nct0mjq8uut03q` FOREIGN KEY (`category_id`) REFERENCES `expense_categories` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `leave_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `leave_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint NOT NULL,
  `employee_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `employee_photo_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `designation` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `department` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `leave_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `duration` int NOT NULL,
  `reason` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Pending',
  `applied_date` date NOT NULL,
  `approved_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approved_date` date DEFAULT NULL,
  `rejection_reason` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL DEFAULT NULL,
  `version` bigint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_status` (`status`),
  KEY `idx_dates` (`start_date`,`end_date`),
  KEY `idx_leave_requests_deleted_at` (`deleted_at`),
  KEY `idx_leave_requests_version` (`version`),
  CONSTRAINT `fk_leave_requests_employee` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `password_reset_otp`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `password_reset_otp` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `last_sent_at` datetime(6) DEFAULT NULL,
  `otp_hash` varchar(255) NOT NULL,
  `resend_count` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  `verification_attempts` int NOT NULL,
  `verified` bit(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `transaction_uuid` varchar(100) NOT NULL,
  `gateway` varchar(50) NOT NULL,
  `gateway_transaction_id` varchar(100) DEFAULT NULL,
  `gateway_ref_id` varchar(100) DEFAULT NULL,
  `shop_owner_id` bigint NOT NULL,
  `subscription_plan_id` bigint NOT NULL,
  `subscription_id` bigint DEFAULT NULL,
  `billing_cycle` varchar(30) DEFAULT NULL,
  `amount` decimal(12,2) NOT NULL,
  `tax_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `total_amount` decimal(12,2) NOT NULL,
  `currency` varchar(10) NOT NULL DEFAULT 'NPR',
  `status` enum('PENDING','SUCCESS','FAILED','CANCELLED','VERIFICATION_FAILED') NOT NULL,
  `payment_method` varchar(50) DEFAULT NULL,
  `invoice_number` varchar(100) DEFAULT NULL,
  `failure_reason` varchar(500) DEFAULT NULL,
  `product_code` varchar(50) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `paid_at` datetime DEFAULT NULL,
  `discount_amount` decimal(12,2) DEFAULT '0.00',
  `coupon_id` bigint DEFAULT NULL,
  `coupon_code_snapshot` varchar(50) DEFAULT NULL,
  `coupon_discount_type_snapshot` varchar(20) DEFAULT NULL,
  `coupon_discount_value_snapshot` varchar(50) DEFAULT NULL,
  `plan_name_snapshot` varchar(100) DEFAULT NULL,
  `subscription_start_date_snapshot` datetime DEFAULT NULL,
  `subscription_end_date_snapshot` datetime DEFAULT NULL,
  `vehicle_limit_snapshot` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payments_transaction_uuid` (`transaction_uuid`),
  KEY `idx_payments_shop_owner_id` (`shop_owner_id`),
  KEY `idx_payments_status` (`status`),
  KEY `idx_payments_subscription_plan_id` (`subscription_plan_id`),
  KEY `idx_payments_subscription_id` (`subscription_id`)
) ENGINE=InnoDB AUTO_INCREMENT=63 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `public_vehicle_listing_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `public_vehicle_listing_files` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `display_order` int DEFAULT NULL,
  `document_type` varchar(255) DEFAULT NULL,
  `file_type` enum('VEHICLE_PHOTO','VEHICLE_VIDEO','BLUEBOOK','INSURANCE','CITIZENSHIP','AUTHORIZATION_DOCUMENT','OTHER') NOT NULL,
  `file_url` text NOT NULL,
  `is_cover` bit(1) NOT NULL,
  `is_public` bit(1) NOT NULL,
  `original_filename` varchar(255) DEFAULT NULL,
  `listing_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_pvlf_listing` (`listing_id`),
  KEY `idx_pvlf_file_type` (`file_type`),
  KEY `idx_pvlf_is_public` (`is_public`),
  CONSTRAINT `FKp1pupku3u53umadhe7uatsqe0` FOREIGN KEY (`listing_id`) REFERENCES `public_vehicle_listings` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=75 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `public_vehicle_listing_review_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `public_vehicle_listing_review_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(30) NOT NULL,
  `actor` varchar(20) NOT NULL,
  `listing_id` bigint NOT NULL,
  `notes` text,
  `performed_at` datetime(6) NOT NULL,
  `reason` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `public_vehicle_listing_sequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `public_vehicle_listing_sequence` (
  `period_key` varchar(6) COLLATE utf8mb4_unicode_ci NOT NULL,
  `next_value` bigint NOT NULL,
  PRIMARY KEY (`period_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `public_vehicle_listings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `public_vehicle_listings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `listing_number` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `seller_user_id` bigint DEFAULT NULL,
  `seller_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `seller_phone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `seller_address` text COLLATE utf8mb4_unicode_ci,
  `owner_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `owner_address` text COLLATE utf8mb4_unicode_ci,
  `vehicle_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `brand` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `model` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `variant` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `manufacturing_year` int NOT NULL,
  `kilometers_driven` int NOT NULL,
  `fuel_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `engine_cc` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `color` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `price` decimal(19,2) NOT NULL,
  `price_in_words` text COLLATE utf8mb4_unicode_ci,
  `negotiable` tinyint(1) DEFAULT '0',
  `status` enum('DRAFT','SUBMITTED','UNDER_REVIEW','CHANGES_REQUESTED','APPROVED','PUBLISHED','SOLD','REJECTED','CANCELLED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `admin_notes` text COLLATE utf8mb4_unicode_ci,
  `rejection_reason` text COLLATE utf8mb4_unicode_ci,
  `declaration_accepted` tinyint(1) DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `submitted_at` timestamp NULL DEFAULT NULL,
  `reviewed_at` timestamp NULL DEFAULT NULL,
  `approved_at` timestamp NULL DEFAULT NULL,
  `published_at` timestamp NULL DEFAULT NULL,
  `sold_at` timestamp NULL DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `owner_phone` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `seller_updated_at` datetime DEFAULT NULL,
  `lot_number` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `listing_number` (`listing_number`),
  UNIQUE KEY `idx_pvl_listing_number` (`listing_number`),
  KEY `idx_pvl_status` (`status`),
  KEY `idx_pvl_seller_user` (`seller_user_id`),
  KEY `idx_pvl_vehicle_number` (`vehicle_number`),
  KEY `idx_pvl_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `refresh_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refresh_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `token_hash` varchar(64) NOT NULL,
  `user_email` varchar(150) NOT NULL,
  `expires_at` datetime NOT NULL,
  `revoked` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_rt_token_hash` (`token_hash`),
  KEY `idx_rt_user_email` (`user_email`)
) ENGINE=InnoDB AUTO_INCREMENT=388 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `salary_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `salary_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint NOT NULL,
  `employee_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `month` int NOT NULL,
  `year` int NOT NULL,
  `month_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `basic_salary` decimal(38,2) NOT NULL,
  `house_allowance` decimal(38,2) DEFAULT NULL,
  `travel_allowance` decimal(38,2) DEFAULT NULL,
  `medical_allowance` decimal(38,2) DEFAULT NULL,
  `food_allowance` decimal(38,2) DEFAULT NULL,
  `bonus` decimal(38,2) DEFAULT NULL,
  `commission` decimal(38,2) DEFAULT NULL,
  `overtime` decimal(38,2) DEFAULT NULL,
  `total_earnings` decimal(38,2) NOT NULL,
  `pf` decimal(38,2) DEFAULT NULL,
  `esi` decimal(38,2) DEFAULT NULL,
  `professional_tax` decimal(38,2) DEFAULT NULL,
  `income_tax` decimal(38,2) DEFAULT NULL,
  `other_deductions` decimal(38,2) DEFAULT NULL,
  `total_deductions` decimal(38,2) NOT NULL,
  `net_salary` decimal(38,2) NOT NULL,
  `payment_status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Pending',
  `payment_date` date DEFAULT NULL,
  `generated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL DEFAULT NULL,
  `employee_id_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shop_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shop_location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `available_days` int DEFAULT NULL,
  `paid_days` int DEFAULT NULL,
  `loss_of_pay_days` int DEFAULT NULL,
  `loss_of_pay_amount` decimal(38,2) DEFAULT NULL,
  `employee_photo_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `advance_deduction` decimal(38,2) DEFAULT NULL,
  `amount_paid` decimal(38,2) DEFAULT NULL,
  `balance_due` decimal(38,2) DEFAULT NULL,
  `previous_balance` decimal(38,2) DEFAULT NULL,
  `total_payable` decimal(38,2) DEFAULT NULL,
  `version` bigint DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_employee_month_year` (`employee_id`,`month`,`year`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_month_year` (`month`,`year`),
  KEY `idx_payment_status` (`payment_status`),
  KEY `idx_salary_records_deleted_at` (`deleted_at`),
  KEY `idx_salary_records_version` (`version`),
  CONSTRAINT `fk_salary_records_employee` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_salary_amount_paid_non_negative` CHECK ((`amount_paid` >= 0)),
  CONSTRAINT `chk_salary_net_salary_non_negative` CHECK ((`net_salary` >= 0)),
  CONSTRAINT `chk_salary_total_payable_non_negative` CHECK ((`total_payable` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=68 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sell_applications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sell_applications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `application_date` datetime(6) DEFAULT NULL,
  `citizenship_back_photo` varchar(255) DEFAULT NULL,
  `citizenship_front_photo` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `customer_address` varchar(255) DEFAULT NULL,
  `customer_citizenship_number` varchar(255) DEFAULT NULL,
  `customer_email` varchar(255) DEFAULT NULL,
  `customer_name` varchar(255) DEFAULT NULL,
  `customer_parent_name` varchar(255) DEFAULT NULL,
  `customer_phone` varchar(255) DEFAULT NULL,
  `customer_photo` varchar(255) DEFAULT NULL,
  `down_payment` decimal(38,2) DEFAULT NULL,
  `financing_bank` varchar(255) DEFAULT NULL,
  `financing_required` bit(1) DEFAULT NULL,
  `offered_price` decimal(38,2) DEFAULT NULL,
  `offered_price_in_words` varchar(255) DEFAULT NULL,
  `payment_method` enum('CASH','FINANCING','MIXED') DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `vehicle_id` bigint DEFAULT NULL,
  `sales_man_name` varchar(255) DEFAULT NULL,
  `notes` varchar(255) DEFAULT NULL,
  `customer_income` decimal(38,2) DEFAULT NULL,
  `customer_occupation` varchar(255) DEFAULT NULL,
  `reference_name` varchar(255) DEFAULT NULL,
  `reference_phone` varchar(255) DEFAULT NULL,
  `reference_relation` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKc92p6nkm9al2oes5w4lx1wa2o` (`vehicle_id`),
  CONSTRAINT `FKc92p6nkm9al2oes5w4lx1wa2o` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicles` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sell_vehicle_applications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sell_vehicle_applications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address_proof_provided` bit(1) DEFAULT NULL,
  `application_date` datetime(6) NOT NULL,
  `background_check_consent` bit(1) DEFAULT NULL,
  `citizenship_copy_provided` bit(1) DEFAULT NULL,
  `customer_address` text NOT NULL,
  `customer_citizenship_number` varchar(255) NOT NULL,
  `customer_email` varchar(255) NOT NULL,
  `customer_income` decimal(19,2) DEFAULT NULL,
  `customer_name` varchar(255) NOT NULL,
  `customer_occupation` varchar(255) DEFAULT NULL,
  `customer_phone` varchar(255) NOT NULL,
  `down_payment` decimal(19,2) DEFAULT NULL,
  `financing_amount` decimal(19,2) DEFAULT NULL,
  `sales_man_name` varchar(255) DEFAULT NULL,
  `financing_bank` varchar(255) DEFAULT NULL,
  `financing_required` bit(1) DEFAULT NULL,
  `income_proof_provided` bit(1) DEFAULT NULL,
  `notes` text,
  `offered_price` decimal(19,2) NOT NULL,
  `offered_price_in_words` text,
  `payment_method` enum('CASH','FINANCING','MIXED') NOT NULL,
  `photo_provided` bit(1) DEFAULT NULL,
  `reference_name` varchar(255) DEFAULT NULL,
  `reference_phone` varchar(255) DEFAULT NULL,
  `reference_relation` varchar(255) DEFAULT NULL,
  `status` enum('APPROVED','CANCELLED','COMPLETED','PENDING','REJECTED','UNDER_REVIEW') NOT NULL,
  `submitted_at` datetime(6) NOT NULL,
  `terms_accepted` bit(1) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `shop_id` bigint NOT NULL,
  `vehicle_id` bigint NOT NULL,
  `citizenship_back_photo` text,
  `citizenship_front_photo` text,
  `customer_parent_name` varchar(255) DEFAULT NULL,
  `customer_photo` text,
  PRIMARY KEY (`id`),
  KEY `FKipahv77fce3xiyvewjl0s1hoy` (`shop_id`),
  KEY `FKq1btt6qw9l0rb91w3f24gwq76` (`vehicle_id`),
  CONSTRAINT `FKipahv77fce3xiyvewjl0s1hoy` FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`),
  CONSTRAINT `FKq1btt6qw9l0rb91w3f24gwq76` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicles` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `shop_owner_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shop_owner_permissions` (
  `shop_owner_id` bigint NOT NULL,
  `permission` varchar(255) DEFAULT NULL,
  KEY `FK6w39ajw3kx1s1pfp7mjxx7o6u` (`shop_owner_id`),
  CONSTRAINT `FK6w39ajw3kx1s1pfp7mjxx7o6u` FOREIGN KEY (`shop_owner_id`) REFERENCES `shop_owners` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `shop_owners`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shop_owners` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `address` varchar(255) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `company_name` varchar(255) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `email_verified` bit(1) DEFAULT NULL,
  `first_name` varchar(255) NOT NULL,
  `kyc_verified` bit(1) DEFAULT NULL,
  `last_name` varchar(255) NOT NULL,
  `license_number` varchar(255) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `phone` varchar(255) NOT NULL,
  `postal_code` varchar(255) DEFAULT NULL,
  `role` enum('PUBLIC','SHOP_OWNER','SUPERADMIN') NOT NULL,
  `state` varchar(255) DEFAULT NULL,
  `subscription_active` bit(1) DEFAULT NULL,
  `subscription_expires_at` datetime(6) DEFAULT NULL,
  `subscription_plan` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  `auto_generate_password` bit(1) DEFAULT NULL,
  `citizenship_no` varchar(255) DEFAULT NULL,
  `citizenship_pic` varchar(255) DEFAULT NULL,
  `citizenship_upload` varchar(255) DEFAULT NULL,
  `closing_time` varchar(255) DEFAULT NULL,
  `confirm_password` varchar(255) DEFAULT NULL,
  `district` varchar(255) DEFAULT NULL,
  `subscription_expiry_date` varchar(255) DEFAULT NULL,
  `facebook_page` varchar(255) DEFAULT NULL,
  `father_name` varchar(255) DEFAULT NULL,
  `full_name` varchar(255) DEFAULT NULL,
  `google_map_link` varchar(255) DEFAULT NULL,
  `municipality` varchar(255) DEFAULT NULL,
  `notes` varchar(255) DEFAULT NULL,
  `off_days` varchar(255) DEFAULT NULL,
  `opening_time` varchar(255) DEFAULT NULL,
  `pan_no` varchar(255) DEFAULT NULL,
  `profile_photo` varchar(255) DEFAULT NULL,
  `province` varchar(255) DEFAULT NULL,
  `reg_cert_no` varchar(255) DEFAULT NULL,
  `send_via_email` bit(1) DEFAULT NULL,
  `send_via_sms` bit(1) DEFAULT NULL,
  `shop_email` varchar(255) DEFAULT NULL,
  `shop_logo` varchar(255) DEFAULT NULL,
  `shop_name` varchar(255) DEFAULT NULL,
  `shop_phone` varchar(255) DEFAULT NULL,
  `shop_registration_upload` varchar(255) DEFAULT NULL,
  `shop_type` varchar(255) DEFAULT NULL,
  `staff_limit` int DEFAULT NULL,
  `subscription_start_date` varchar(255) DEFAULT NULL,
  `account_status` varchar(255) DEFAULT NULL,
  `tole` varchar(255) DEFAULT NULL,
  `vat_registration` varchar(255) DEFAULT NULL,
  `vehicle_limit` int DEFAULT NULL,
  `ward` varchar(255) DEFAULT NULL,
  `whatsapp_no` varchar(255) DEFAULT NULL,
  `pan` varchar(255) DEFAULT NULL,
  `reg_cert` varchar(255) DEFAULT NULL,
  `shop_reg_upload` varchar(255) DEFAULT NULL,
  `vat` varchar(255) DEFAULT NULL,
  `citizenship_pic_back` varchar(255) DEFAULT NULL,
  `citizenship_pic_front` varchar(255) DEFAULT NULL,
  `approval_status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `password_changed` tinyint(1) NOT NULL DEFAULT '0',
  `rejection_reason` text,
  `approved_at` datetime DEFAULT NULL,
  `approved_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKe8jxc2xnwj76ubyyleaxl8g3a` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=48 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `shop_reg_otp`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shop_reg_otp` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `email_otp_hash` varchar(255) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `last_sent_at` datetime(6) DEFAULT NULL,
  `mobile_number` varchar(20) NOT NULL,
  `mobile_otp_hash` varchar(255) NOT NULL,
  `resend_count` int NOT NULL,
  `token_expires_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  `verification_attempts` int NOT NULL,
  `verification_token_hash` varchar(255) DEFAULT NULL,
  `verified` bit(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `shop_reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shop_reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `shop_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `reviewer_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rating` int NOT NULL,
  `comment` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_shop_reviews_shop_id` (`shop_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `shops`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shops` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address_line_1` varchar(255) DEFAULT NULL,
  `address_line_2` varchar(255) DEFAULT NULL,
  `city` varchar(255) NOT NULL,
  `country` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `email_address` varchar(255) DEFAULT NULL,
  `is_featured` bit(1) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `license_number` varchar(255) NOT NULL,
  `logo_url` varchar(255) DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `opening_hours` varchar(255) DEFAULT NULL,
  `phone_number` varchar(255) DEFAULT NULL,
  `postal_code` varchar(255) DEFAULT NULL,
  `state` varchar(255) NOT NULL,
  `status` enum('ACTIVE','INACTIVE','PENDING_APPROVAL','REJECTED','SUSPENDED') NOT NULL,
  `subscription_expiry` datetime(6) DEFAULT NULL,
  `subscription_plan` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `website_url` varchar(255) DEFAULT NULL,
  `shop_owner_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKsfk72pn2pnt5mbjvw1ubdn85p` (`license_number`),
  KEY `FK5p9oog17doynu4v85d3g71rma` (`shop_owner_id`),
  KEY `FK34po7mmli7wotimo70r6640ap` (`user_id`),
  CONSTRAINT `FK34po7mmli7wotimo70r6640ap` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK5p9oog17doynu4v85d3g71rma` FOREIGN KEY (`shop_owner_id`) REFERENCES `shop_owners` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `signup_otp`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `signup_otp` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `mobile_number` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `otp_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expires_at` datetime NOT NULL,
  `verification_attempts` int NOT NULL DEFAULT '0',
  `resend_count` int NOT NULL DEFAULT '0',
  `last_sent_at` datetime DEFAULT NULL,
  `verified` tinyint(1) NOT NULL DEFAULT '0',
  `verification_token_hash` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `token_expires_at` datetime DEFAULT NULL,
  `used_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_signup_otp_mobile` (`mobile_number`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `subscription_activities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subscription_activities` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` enum('PLAN_CREATED','PLAN_UPDATED','PLAN_PUBLISHED','PLAN_DISABLED','PLAN_ARCHIVED','PLAN_DUPLICATED','PLAN_DELETED','SUBSCRIPTION_CREATED','SUBSCRIPTION_UPGRADED','SUBSCRIPTION_DOWNGRADED','SUBSCRIPTION_SUSPENDED','SUBSCRIPTION_CANCELLED','PAYMENT_COMPLETED','PAYMENT_FAILED','COUPON_CREATED','COUPON_UPDATED','COUPON_DELETED','TRIAL_UPDATED','SETTINGS_UPDATED') NOT NULL,
  `entity_type` varchar(50) DEFAULT NULL,
  `entity_id` bigint DEFAULT NULL,
  `admin_user_id` bigint DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'COMPLETED',
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_activities_entity` (`entity_type`,`entity_id`),
  KEY `idx_activities_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=71 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `subscription_coupon_usages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subscription_coupon_usages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `coupon_id` bigint NOT NULL,
  `transaction_id` bigint NOT NULL,
  `shop_owner_id` bigint NOT NULL,
  `discount_amount` decimal(12,2) NOT NULL,
  `used_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_usage_coupon_transaction` (`coupon_id`,`transaction_id`),
  KEY `idx_coupon_usage_coupon_id` (`coupon_id`),
  KEY `idx_coupon_usage_txn_id` (`transaction_id`),
  CONSTRAINT `fk_coupon_usage_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `subscription_coupons` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_coupon_usage_txn` FOREIGN KEY (`transaction_id`) REFERENCES `subscription_transactions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `subscription_coupons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subscription_coupons` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) NOT NULL,
  `discount_type` enum('PERCENTAGE','FLAT') NOT NULL,
  `percentage` int DEFAULT NULL,
  `flat_discount` decimal(12,2) DEFAULT NULL,
  `maximum_discount` decimal(12,2) DEFAULT NULL,
  `minimum_purchase` decimal(12,2) DEFAULT NULL,
  `usage_limit` int NOT NULL DEFAULT '100',
  `expiry_date` date DEFAULT NULL,
  `active` tinyint(1) DEFAULT '1',
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_code` (`code`),
  KEY `idx_coupon_expiry_date` (`expiry_date`),
  KEY `idx_coupon_active` (`active`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `subscription_invoice_sequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subscription_invoice_sequence` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `year` int NOT NULL,
  `next_val` int NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invoice_seq_year` (`year`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `subscription_plan_features`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subscription_plan_features` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL,
  `name` varchar(100) NOT NULL,
  `icon` varchar(50) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `included` tinyint(1) DEFAULT '0',
  `limit` int DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_features_plan_id` (`plan_id`),
  CONSTRAINT `fk_features_plan` FOREIGN KEY (`plan_id`) REFERENCES `subscription_plans` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `subscription_plan_pricing`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subscription_plan_pricing` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL,
  `monthly` decimal(12,2) DEFAULT NULL,
  `quarterly` decimal(12,2) DEFAULT NULL,
  `half_yearly` decimal(12,2) DEFAULT NULL,
  `yearly` decimal(12,2) DEFAULT NULL,
  `currency` varchar(10) NOT NULL DEFAULT 'INR',
  `gst_included` tinyint(1) DEFAULT '1',
  `discount_percentage` int DEFAULT '0',
  `strike_price` decimal(12,2) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_pricing_plan_id` (`plan_id`),
  CONSTRAINT `fk_pricing_plan` FOREIGN KEY (`plan_id`) REFERENCES `subscription_plans` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `subscription_plan_restrictions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subscription_plan_restrictions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL,
  `max_vehicles` int DEFAULT NULL,
  `max_employees` int DEFAULT NULL,
  `max_storage` varchar(20) DEFAULT NULL,
  `max_branches` int DEFAULT NULL,
  `api_calls` int DEFAULT NULL,
  `support_level` varchar(50) DEFAULT NULL,
  `daily_upload_limit` int DEFAULT NULL,
  `backup_frequency` varchar(30) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_restrictions_plan_id` (`plan_id`),
  CONSTRAINT `fk_restrictions_plan` FOREIGN KEY (`plan_id`) REFERENCES `subscription_plans` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `subscription_plans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subscription_plans` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `slug` varchar(120) NOT NULL,
  `description` text,
  `short_description` varchar(255) DEFAULT NULL,
  `category` enum('BASIC','STANDARD','PREMIUM','ULTIMATE','CUSTOM') NOT NULL,
  `icon` varchar(50) DEFAULT NULL,
  `theme_color` varchar(20) DEFAULT '#f97316',
  `sort_order` int DEFAULT '0',
  `is_popular` tinyint(1) DEFAULT '0',
  `is_recommended` tinyint(1) DEFAULT '0',
  `visibility` enum('PUBLIC','PRIVATE') NOT NULL,
  `status` enum('DRAFT','PUBLISHED','ARCHIVED','DISABLED') NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subscription_plans_slug` (`slug`),
  KEY `idx_subscription_plans_status` (`status`),
  KEY `idx_subscription_plans_visibility` (`visibility`),
  KEY `idx_subscription_plans_category` (`category`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `subscription_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subscription_settings` (
  `id` bigint NOT NULL DEFAULT '1',
  `default_trial_days` int NOT NULL DEFAULT '14',
  `tax_percentage` int NOT NULL DEFAULT '18',
  `currency` varchar(10) NOT NULL DEFAULT 'INR',
  `invoice_prefix` varchar(20) NOT NULL DEFAULT 'INV',
  `payment_reminder_days` int NOT NULL DEFAULT '7',
  `renewal_reminder` int NOT NULL DEFAULT '3',
  `grace_period` int NOT NULL DEFAULT '5',
  `cancellation_policy` text,
  `refund_policy` text,
  `enable_auto_renewal` tinyint(1) DEFAULT '1',
  `enable_free_trial` tinyint(1) DEFAULT '1',
  `enable_coupons` tinyint(1) DEFAULT '1',
  `enable_lifetime_plans` tinyint(1) DEFAULT '1',
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `enable_vat` bit(1) DEFAULT b'1',
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_settings_single_row` CHECK ((`id` = 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `subscription_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subscription_transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `transaction_id` varchar(50) NOT NULL,
  `subscription_id` bigint DEFAULT NULL,
  `shop_owner_id` bigint NOT NULL,
  `plan_id` bigint NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `tax` decimal(12,2) DEFAULT '0.00',
  `coupon_id` bigint DEFAULT NULL,
  `discount` decimal(12,2) DEFAULT '0.00',
  `final_amount` decimal(12,2) NOT NULL,
  `payment_method` varchar(30) DEFAULT NULL,
  `gateway` varchar(30) DEFAULT NULL,
  `status` enum('COMPLETED','PENDING','FAILED','REFUNDED') NOT NULL,
  `invoice_number` varchar(50) NOT NULL,
  `transaction_date` datetime NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sub_txn_id` (`transaction_id`),
  UNIQUE KEY `uk_sub_invoice_number` (`invoice_number`),
  KEY `fk_subtxn_subscription` (`subscription_id`),
  KEY `fk_subtxn_coupon` (`coupon_id`),
  KEY `idx_subtxn_transaction_date` (`transaction_date`),
  KEY `idx_subtxn_status` (`status`),
  KEY `idx_subtxn_shop_owner_id` (`shop_owner_id`),
  KEY `idx_subtxn_plan_id` (`plan_id`),
  CONSTRAINT `fk_subtxn_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `subscription_coupons` (`id`),
  CONSTRAINT `fk_subtxn_plan` FOREIGN KEY (`plan_id`) REFERENCES `subscription_plans` (`id`),
  CONSTRAINT `fk_subtxn_subscription` FOREIGN KEY (`subscription_id`) REFERENCES `subscriptions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `subscription_trial_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subscription_trial_config` (
  `id` bigint NOT NULL DEFAULT '1',
  `name` varchar(100) NOT NULL,
  `description` text,
  `duration` int NOT NULL DEFAULT '14',
  `eligibility_rules` varchar(500) DEFAULT NULL,
  `maximum_uses` int NOT NULL DEFAULT '100',
  `trial_plan_id` bigint DEFAULT NULL,
  `active` tinyint(1) DEFAULT '1',
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `vehicle_limit` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_trial_config_plan` (`trial_plan_id`),
  CONSTRAINT `fk_trial_config_plan` FOREIGN KEY (`trial_plan_id`) REFERENCES `subscription_plans` (`id`),
  CONSTRAINT `chk_trial_single_row` CHECK ((`id` = 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `subscriptions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subscriptions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `shop_owner_id` bigint NOT NULL,
  `shop_id` bigint DEFAULT NULL,
  `plan_id` bigint NOT NULL,
  `trial_id` bigint DEFAULT NULL,
  `start_date` datetime NOT NULL,
  `current_period_start` datetime NOT NULL,
  `end_date` datetime NOT NULL,
  `auto_renewal` tinyint(1) DEFAULT '0',
  `status` enum('ACTIVE','TRIAL','SUSPENDED','CANCELLED','EXPIRED') NOT NULL,
  `renewal_date` datetime DEFAULT NULL,
  `cancelled_date` datetime DEFAULT NULL,
  `suspended_date` datetime DEFAULT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `active_owner_key` bigint GENERATED ALWAYS AS ((case when (`status` in (_utf8mb4'ACTIVE',_utf8mb4'TRIAL')) then `shop_owner_id` else NULL end)) STORED,
  `plan_name_snapshot` varchar(255) DEFAULT NULL,
  `plan_description_snapshot` text,
  `plan_icon_snapshot` varchar(255) DEFAULT NULL,
  `plan_theme_color_snapshot` varchar(50) DEFAULT NULL,
  `vehicle_limit_snapshot` int DEFAULT NULL,
  `price_paid` decimal(10,2) DEFAULT NULL,
  `billing_cycle_snapshot` varchar(50) DEFAULT NULL,
  `new_plan_vehicle_limit` int DEFAULT NULL,
  `carried_forward_vehicle_limit` int DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subscriptions_active_owner` (`active_owner_key`),
  KEY `fk_subscriptions_trial` (`trial_id`),
  KEY `idx_subscriptions_shop_owner_id` (`shop_owner_id`),
  KEY `idx_subscriptions_plan_id` (`plan_id`),
  KEY `idx_subscriptions_status` (`status`),
  KEY `idx_subscriptions_end_date` (`end_date`),
  CONSTRAINT `fk_subscriptions_plan` FOREIGN KEY (`plan_id`) REFERENCES `subscription_plans` (`id`),
  CONSTRAINT `fk_subscriptions_trial` FOREIGN KEY (`trial_id`) REFERENCES `subscription_trial_config` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `first_name` varchar(255) NOT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `is_email_verified` bit(1) DEFAULT NULL,
  `last_name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `phone_number` varchar(255) DEFAULT NULL,
  `role` enum('PUBLIC','SHOP_OWNER','SUPERADMIN') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `customer_code` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  KEY `idx_users_customer_code` (`customer_code`)
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vehicle_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vehicle_images` (
  `vehicle_id` bigint NOT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  KEY `FKp6gw8mt61ktmsk5nuc4qid7i8` (`vehicle_id`),
  CONSTRAINT `FKp6gw8mt61ktmsk5nuc4qid7i8` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vehicles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vehicles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `body_type` varchar(255) DEFAULT NULL,
  `brand_name` varchar(255) DEFAULT NULL,
  `color` varchar(255) DEFAULT NULL,
  `vehicle_condition` varchar(255) DEFAULT NULL,
  `contact_count` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `engine_capacity` varchar(255) DEFAULT NULL,
  `features` text,
  `fuel_type` varchar(255) DEFAULT NULL,
  `insurance_valid` datetime(6) DEFAULT NULL,
  `is_featured` bit(1) DEFAULT NULL,
  `is_negotiable` bit(1) DEFAULT NULL,
  `kilometers_driven` int DEFAULT NULL,
  `last_service_date` datetime(6) DEFAULT NULL,
  `main_image_url` varchar(255) DEFAULT NULL,
  `manufacturing_year` int DEFAULT NULL,
  `model_name` varchar(255) DEFAULT NULL,
  `ownership_type` varchar(255) DEFAULT NULL,
  `price` decimal(12,2) NOT NULL,
  `registration_number` varchar(255) DEFAULT NULL,
  `rejection_reason` varchar(255) DEFAULT NULL,
  `sold_at` datetime(6) DEFAULT NULL,
  `specifications` text,
  `status` enum('ACTIVE','INACTIVE','SOLD','PENDING_APPROVAL','REJECTED','SUSPENDED','FLAGGED','PENDING_SALE') DEFAULT 'PENDING_APPROVAL',
  `title` varchar(255) NOT NULL,
  `transmission_type` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `vehicle_type` enum('BIKE','BUS','CAR','SCOOTER','TRUCK') NOT NULL,
  `video_url` varchar(255) DEFAULT NULL,
  `view_count` bigint DEFAULT NULL,
  `category_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `seller_citizenship_back` varchar(255) DEFAULT NULL,
  `seller_citizenship_front` varchar(255) DEFAULT NULL,
  `seller_passport_photo` varchar(255) DEFAULT NULL,
  `lots_number` varchar(255) DEFAULT NULL,
  `bought_date` date DEFAULT NULL,
  `purchase_price` decimal(12,2) DEFAULT '0.00' COMMENT 'Purchase cost of the vehicle',
  `repair_cost` decimal(12,2) DEFAULT '0.00' COMMENT 'Cost of repairs/refurbishment',
  `additional_expenses` decimal(12,2) DEFAULT '0.00' COMMENT 'Additional vehicle expenses (transport, documentation, etc.)',
  PRIMARY KEY (`id`),
  KEY `FKdjuwisect6diyjv2bk0j4wlcv` (`category_id`),
  KEY `idx_vehicle_shop_status` (`shop_id`,`status`),
  KEY `idx_vehicle_sold_at` (`sold_at`),
  KEY `idx_vehicle_bought_date` (`bought_date`),
  CONSTRAINT `FK60uo474m70v8b6iybyfne41x7` FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`),
  CONSTRAINT `FKdjuwisect6diyjv2bk0j4wlcv` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `wishlists`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wishlists` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `customer_id` bigint NOT NULL,
  `vehicle_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKko1h7j8hmfh24ht4mpp5gjm8h` (`customer_id`),
  KEY `FKn560xsy34rmsnvrvhyqgrm89h` (`vehicle_id`),
  CONSTRAINT `FKko1h7j8hmfh24ht4mpp5gjm8h` FOREIGN KEY (`customer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKn560xsy34rmsnvrvhyqgrm89h` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicles` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


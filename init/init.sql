-- ------------------------------------------------------------
-- hhplus 초기화 스키마 (MySQL 8.0 / UTC 저장)
-- ------------------------------------------------------------

CREATE DATABASE IF NOT EXISTS hhplus
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE hhplus;
SET NAMES utf8mb4;

-- ============================================================
-- 1) concerts (공연 마스터)
-- ============================================================
CREATE TABLE `concerts`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `title`          VARCHAR(255) NOT NULL COMMENT '공연 제목',
    `description`    TEXT COMMENT '공연 상세 설명',
    `artist_name`    VARCHAR(255) DEFAULT NULL COMMENT '아티스트명',
    `organizer_name` VARCHAR(255) DEFAULT NULL COMMENT '주최/주관사명',
    `is_open`        TINYINT(1) NOT NULL DEFAULT 1 COMMENT '공개 여부(1=활성)',
    `created_at`     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각(UTC)',
    `updated_at`     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각(UTC)',
    `deleted_at`     DATETIME(6) DEFAULT NULL COMMENT '삭제 시각(UTC)',
    PRIMARY KEY (`id`),
    KEY `idx_concerts_title` (`title`),
    KEY `idx_concerts_open` (`is_open`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='공연 마스터';


-- ============================================================
-- 2) users (회원)
-- ============================================================
CREATE TABLE `users`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `user_uuid`  CHAR(36) NOT NULL COMMENT '외부 노출용 UUID',
    `email`      VARCHAR(255) NOT NULL COMMENT '로그인 이메일(유니크)',
    `password`   VARCHAR(255) NOT NULL COMMENT '암호 해시',
    `name`       VARCHAR(50)  NOT NULL COMMENT '사용자 이름',
    `created_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각(UTC)',
    `updated_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각(UTC)',
    `deleted_at` DATETIME(6)  DEFAULT NULL COMMENT '삭제 시각(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_uuid` (`user_uuid`),
    UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='회원';


-- ============================================================
-- 3) concert_dates (공연 일자)
-- ============================================================
CREATE TABLE `concert_dates`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `concert_id` BIGINT UNSIGNED NOT NULL COMMENT 'FK: concerts.id',
    `event_date` DATE NOT NULL COMMENT '공연 날짜(YYYY-MM-DD)',
    `venue_name` VARCHAR(200) NOT NULL COMMENT '공연장 이름',
    `is_open`    TINYINT(1) NOT NULL DEFAULT 0 COMMENT '예매 오픈 여부(1=오픈)',
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각(UTC)',
    `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각(UTC)',
    `deleted_at` DATETIME(6) DEFAULT NULL COMMENT '삭제 시각(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_concert_date` (`concert_id`,`event_date`),
    KEY `idx_concert_dates_open` (`is_open`),
    CONSTRAINT `fk_concert_dates_concert`
        FOREIGN KEY (`concert_id`) REFERENCES `concerts` (`id`)
            ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='공연 일자';


-- ============================================================
-- 4) concert_seats (일자별 좌석)
-- ============================================================
CREATE TABLE `concert_seats`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `concert_date_id` BIGINT UNSIGNED NOT NULL COMMENT 'FK: concert_dates.id',
    `seat_no`         INT UNSIGNED NOT NULL COMMENT '좌석 번호',
    `section`         VARCHAR(50) NOT NULL DEFAULT '' COMMENT '구역/층/블록(공백=없음)',
    `price`           BIGINT UNSIGNED NOT NULL COMMENT '좌석 가격(원)',
    `created_at`      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각(UTC)',
    `updated_at`      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각(UTC)',
    `deleted_at`      DATETIME(6) DEFAULT NULL COMMENT '삭제 시각(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_concert_seat` (`concert_date_id`,`section`,`seat_no`),
    KEY `idx_concert_seats_price` (`price`),
    CONSTRAINT `fk_concert_seats_date`
        FOREIGN KEY (`concert_date_id`) REFERENCES `concert_dates` (`id`)
            ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `chk_concert_seats_price_nonneg`
        CHECK (`price` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='공연 좌석(일자별)';


-- ============================================================
-- 5) reservations (예약) + concert_id 포함
-- ============================================================
CREATE TABLE `reservations`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT 'FK: users.id',
    `concert_id`      BIGINT UNSIGNED NOT NULL COMMENT 'FK: concerts.id (공연)',
    `concert_date_id` BIGINT UNSIGNED NOT NULL COMMENT 'FK: concert_dates.id',
    `seat_id`         BIGINT UNSIGNED NOT NULL COMMENT 'FK: concert_seats.id',
    `status`          ENUM('PENDING','CONFIRMED','CANCELED','EXPIRED') NOT NULL COMMENT '예약 상태',
    `amount`          BIGINT UNSIGNED NOT NULL COMMENT '결제 예정/실제 금액(원)',
    `hold_expires_at` DATETIME(6) DEFAULT NULL COMMENT '홀드 만료 시각(UTC)',
    `confirmed_at`    DATETIME(6) DEFAULT NULL COMMENT '확정 시각(UTC)',
    `canceled_at`     DATETIME(6) DEFAULT NULL COMMENT '취소 시각(UTC)',
    `expired_at`      DATETIME(6) DEFAULT NULL COMMENT '만료 처리 시각(UTC)',
    `version`         BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '낙관적 락 버전',
    `created_at`      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각(UTC)',
    `updated_at`      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각(UTC)',
    `deleted_at`      DATETIME(6) DEFAULT NULL COMMENT '삭제 시각(UTC)',
    PRIMARY KEY (`id`),
    KEY `idx_resv_user` (`user_id`),
    KEY `idx_resv_concert` (`concert_id`),
    KEY `idx_resv_date` (`concert_date_id`),
    KEY `idx_resv_status` (`status`),
    KEY `idx_resv_hold_exp` (`hold_expires_at`),
    KEY `idx_resv_confirmed_at` (`confirmed_at`),
    KEY `idx_resv_canceled_at` (`canceled_at`),
    KEY `idx_resv_expired_at` (`expired_at`),
    CONSTRAINT `fk_resv_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_resv_concert` FOREIGN KEY (`concert_id`) REFERENCES `concerts` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_resv_date` FOREIGN KEY (`concert_date_id`) REFERENCES `concert_dates` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_resv_seat` FOREIGN KEY (`seat_id`) REFERENCES `concert_seats` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `chk_reservations_amount_nonneg` CHECK (`amount` >= 0),
    CONSTRAINT `chk_reservations_pending_hold` CHECK (`status` <> 'PENDING' OR `hold_expires_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='예약(홀드/확정/취소/만료)';


-- ============================================================
-- 6) payments (결제)
-- ============================================================
CREATE TABLE `payments`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `reservation_id`  BIGINT UNSIGNED NOT NULL COMMENT 'FK: reservations.id',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT 'FK: users.id',
    `amount`          BIGINT UNSIGNED NOT NULL COMMENT '결제 금액(원)',
    `provider`        VARCHAR(50) DEFAULT NULL COMMENT '결제 수단/PG사',
    `provider_txn_id` VARCHAR(100) DEFAULT NULL COMMENT '외부 거래 ID',
    `status`          ENUM('SUCCEEDED','FAILED','PENDING') NOT NULL COMMENT '결제 상태',
    `idempotency_key` CHAR(36) NOT NULL COMMENT '멱등 키(UUID)',
    `created_at`      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각(UTC)',
    `updated_at`      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각(UTC)',
    `deleted_at`      DATETIME(6) DEFAULT NULL COMMENT '삭제 시각(UTC)',
    `is_success`      TINYINT(1) GENERATED ALWAYS AS (CASE WHEN status='SUCCEEDED' THEN 1 ELSE 0 END) STORED COMMENT '성공 결제 플래그',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payments_idempo` (`idempotency_key`),
    UNIQUE KEY `uk_payments_one_success_per_resv` (`reservation_id`,`is_success`),
    KEY `idx_payments_user` (`user_id`),
    CONSTRAINT `fk_payments_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservations` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_payments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `chk_payments_amount_nonneg` CHECK (`amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='결제(가상)';


-- ============================================================
-- 7) queue_audit_logs (대기열 로그)
-- ============================================================
CREATE TABLE `queue_audit_logs`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT 'FK: users.id',
    `concert_id`    BIGINT UNSIGNED NOT NULL COMMENT 'FK: concerts.id',
    `status`        ENUM('ISSUED','ACTIVE','EXPIRED') NOT NULL COMMENT '토큰 상태',
    `rank_position` INT UNSIGNED DEFAULT NULL COMMENT '대기 순번(0이 맨 앞)',
    `note`          VARCHAR(255) DEFAULT NULL COMMENT '비고',
    `created_at`    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각(UTC)',
    PRIMARY KEY (`id`),
    KEY `idx_queue_logs_user` (`user_id`),
    KEY `idx_queue_logs_concert` (`concert_id`),
    KEY `idx_queue_logs_status` (`status`),
    CONSTRAINT `fk_queue_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_queue_logs_concert` FOREIGN KEY (`concert_id`) REFERENCES `concerts` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `chk_queue_logs_rank_nonneg` CHECK (`rank_position` IS NULL OR `rank_position` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='대기열 감사 로그';


-- ============================================================
-- 8) user_tokens (리프레시 토큰)
-- ============================================================
CREATE TABLE `user_tokens`
(
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `user_id`            BIGINT UNSIGNED NOT NULL COMMENT 'FK: users.id',
    `refresh_token_hash` VARCHAR(255) NOT NULL COMMENT 'Refresh 토큰 해시',
    `expires_at`         DATETIME(6) NOT NULL COMMENT '만료 시각(UTC)',
    `created_at`         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_tokens_rth` (`refresh_token_hash`),
    KEY `idx_user_tokens_user` (`user_id`),
    KEY `idx_user_tokens_expires_at` (`expires_at`),
    CONSTRAINT `fk_user_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='리프레시 토큰 저장소';


-- ============================================================
-- 9) wallet_accounts (지갑 계정)
-- ============================================================
CREATE TABLE `wallet_accounts`
(
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT 'PK/FK: users.id',
    `balance`    BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '잔액(원)',
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각(UTC)',
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_wallet_accounts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `chk_wallet_accounts_balance_nonneg` CHECK (`balance` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='지갑 계정';


-- ============================================================
-- 10) wallet_transactions (지갑 거래 원장)
-- ============================================================
CREATE TABLE `wallet_transactions`
(
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `user_id`            BIGINT UNSIGNED NOT NULL COMMENT 'FK: users.id',
    `related_payment_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '연관 결제 ID',
    `amount`             BIGINT UNSIGNED NOT NULL COMMENT '거래 금액(원)',
    `type`               ENUM('CHARGE','DEBIT','REFUND') NOT NULL COMMENT '거래 유형',
    `idempotency_key`    CHAR(36) DEFAULT NULL COMMENT '멱등 키',
    `created_at`         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wallet_txn_user_idempo` (`user_id`,`idempotency_key`),
    KEY `idx_wallet_txn_user` (`user_id`),
    KEY `idx_wallet_txn_payment` (`related_payment_id`),
    CONSTRAINT `fk_wallet_txn_payment` FOREIGN KEY (`related_payment_id`) REFERENCES `payments` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT `fk_wallet_txn_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `chk_wallet_txn_amount_nonneg` CHECK (`amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='지갑 거래 원장(불변)';


-- ============================================================
-- 11) idempotency_keys (멱등 키 저장)
-- ============================================================
CREATE TABLE `idempotency_keys`
(
    `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `request_key` VARCHAR(128) NOT NULL COMMENT '요청 멱등 키(엔드포인트+사용자)',
    `created_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각(UTC)',
    `updated_at`  DATETIME(6)  NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각(UTC)',
    `deleted_at`  DATETIME(6)  NULL DEFAULT NULL COMMENT '삭제 시각(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_idempotency_request_key` (`request_key`),
    KEY `idx_idempotency_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='멱등 키 저장소';
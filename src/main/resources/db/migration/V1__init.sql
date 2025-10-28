-- ============================================================
-- V1__init.sql (MySQL 8.0 / Flyway)
--  * No CREATE DATABASE / USE / SET NAMES
--  * Adds concert_seats.status ENUM('AVAILABLE','HELD','CONFIRMED')
--  * >=5 seed rows per table
-- ============================================================

-- 1) concerts
CREATE TABLE `concerts`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `title`          VARCHAR(255)    NOT NULL COMMENT '공연 제목',
    `description`    TEXT COMMENT '공연 상세 설명',
    `artist_name`    VARCHAR(255)             DEFAULT NULL COMMENT '아티스트명',
    `organizer_name` VARCHAR(255)             DEFAULT NULL COMMENT '주최/주관사명',
    `is_open`        TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '공개 여부(1=활성)',
    `created_at`     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각(UTC)',
    `updated_at`     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각(UTC)',
    `deleted_at`     DATETIME(6)              DEFAULT NULL COMMENT '삭제 시각(UTC)',
    PRIMARY KEY (`id`),
    KEY `idx_concerts_title` (`title`),
    KEY `idx_concerts_open` (`is_open`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='공연 마스터';

-- 2) users
CREATE TABLE `users`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `user_uuid`  CHAR(36)        NOT NULL COMMENT '외부 노출용 UUID',
    `email`      VARCHAR(255)    NOT NULL COMMENT '로그인 이메일(유니크)',
    `password`   VARCHAR(255)    NOT NULL COMMENT '암호 해시',
    `name`       VARCHAR(50)     NOT NULL COMMENT '사용자 이름',
    `created_at` DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted_at` DATETIME(6)              DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_uuid` (`user_uuid`),
    UNIQUE KEY `uk_users_email` (`email`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='회원';

-- 3) concert_dates
CREATE TABLE `concert_dates`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `concert_id` BIGINT UNSIGNED NOT NULL COMMENT 'FK: concerts.id',
    `event_date` DATE            NOT NULL COMMENT '공연 날짜(YYYY-MM-DD)',
    `venue_name` VARCHAR(200)    NOT NULL COMMENT '공연장 이름',
    `is_open`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '예매 오픈 여부(1=오픈)',
    `created_at` DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted_at` DATETIME(6)              DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_concert_date` (`concert_id`, `event_date`),
    KEY `idx_concert_dates_open` (`is_open`),
    CONSTRAINT `fk_concert_dates_concert`
        FOREIGN KEY (`concert_id`) REFERENCES `concerts` (`id`)
            ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='공연 일자';

-- 4) concert_seats (status 추가)
CREATE TABLE `concert_seats`
(
    `id`              BIGINT UNSIGNED                       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `concert_date_id` BIGINT UNSIGNED                       NOT NULL COMMENT 'FK: concert_dates.id',
    `seat_no`         INT UNSIGNED                          NOT NULL COMMENT '좌석 번호',
    `section`         VARCHAR(50)                           NOT NULL DEFAULT '' COMMENT '구역/층/블록(공백=없음)',
    `price`           BIGINT UNSIGNED                       NOT NULL COMMENT '좌석 가격(원)',
    `status`          ENUM ('AVAILABLE','HELD','CONFIRMED') NOT NULL DEFAULT 'AVAILABLE' COMMENT '좌석 상태',
    `created_at`      DATETIME(6)                           NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`      DATETIME(6)                           NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted_at`      DATETIME(6)                                    DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_concert_seat` (`concert_date_id`, `section`, `seat_no`),
    KEY `idx_concert_seats_price` (`price`),
    CONSTRAINT `fk_concert_seats_date`
        FOREIGN KEY (`concert_date_id`) REFERENCES `concert_dates` (`id`)
            ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `chk_concert_seats_price_nonneg`
        CHECK (`price` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='공연 좌석(일자별)';

-- 5) reservations
CREATE TABLE `reservations`
(
    `id`              BIGINT UNSIGNED                                   NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `user_id`         BIGINT UNSIGNED                                   NOT NULL,
    `concert_id`      BIGINT UNSIGNED                                   NOT NULL,
    `concert_date_id` BIGINT UNSIGNED                                   NOT NULL,
    `seat_id`         BIGINT UNSIGNED                                   NOT NULL,
    `status`          ENUM ('PENDING','CONFIRMED','CANCELED','EXPIRED') NOT NULL COMMENT '예약 상태',
    `amount`          BIGINT UNSIGNED                                   NOT NULL COMMENT '결제 예정/실제 금액(원)',
    `hold_expires_at` DATETIME(6)                                                DEFAULT NULL COMMENT '홀드 만료 시각(UTC)',
    `confirmed_at`    DATETIME(6)                                                DEFAULT NULL COMMENT '확정 시각(UTC)',
    `canceled_at`     DATETIME(6)                                                DEFAULT NULL COMMENT '취소 시각(UTC)',
    `expired_at`      DATETIME(6)                                                DEFAULT NULL COMMENT '만료 처리 시각(UTC)',
    `version`         BIGINT UNSIGNED                                   NOT NULL DEFAULT 0 COMMENT '낙관적 락 버전',
    `created_at`      DATETIME(6)                                       NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`      DATETIME(6)                                       NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted_at`      DATETIME(6)                                                DEFAULT NULL,
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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='예약(홀드/확정/취소/만료)';

-- 6) payments
CREATE TABLE `payments`
(
    `id`              BIGINT UNSIGNED                       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `reservation_id`  BIGINT UNSIGNED                       NOT NULL,
    `user_id`         BIGINT UNSIGNED                       NOT NULL,
    `amount`          BIGINT UNSIGNED                       NOT NULL,
    `provider`        VARCHAR(50)                                    DEFAULT NULL,
    `provider_txn_id` VARCHAR(100)                                   DEFAULT NULL,
    `status`          ENUM ('SUCCEEDED','FAILED','PENDING') NOT NULL,
    `idempotency_key` CHAR(36)                              NOT NULL,
    `created_at`      DATETIME(6)                           NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`      DATETIME(6)                           NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted_at`      DATETIME(6)                           NULL,
    -- 성공일 때만 1, 그 외는 NULL → 실패/대기 중복 허용, 성공 단 1건 보장
    `is_success`      TINYINT(1) GENERATED ALWAYS AS (CASE WHEN status = 'SUCCEEDED' THEN 1 ELSE NULL END) STORED,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payments_idempo` (`idempotency_key`),
    UNIQUE KEY `uk_payments_one_success_per_resv` (`reservation_id`, `is_success`),
    KEY `idx_payments_user` (`user_id`),

    CONSTRAINT `fk_payments_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservations` (`id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_payments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `chk_payments_amount_nonneg` CHECK (`amount` >= 0)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci COMMENT ='결제(가상)';

-- 7) queue_audit_logs
CREATE TABLE `queue_audit_logs`
(
    `id`            BIGINT UNSIGNED                    NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `user_id`       BIGINT UNSIGNED                    NOT NULL,
    `concert_id`    BIGINT UNSIGNED                    NOT NULL,
    `status`        ENUM ('ISSUED','ACTIVE','EXPIRED') NOT NULL,
    `rank_position` INT UNSIGNED                                DEFAULT NULL,
    `note`          VARCHAR(255)                                DEFAULT NULL,
    `created_at`    DATETIME(6)                        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    KEY `idx_queue_logs_user` (`user_id`),
    KEY `idx_queue_logs_concert` (`concert_id`),
    KEY `idx_queue_logs_status` (`status`),
    CONSTRAINT `fk_queue_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_queue_logs_concert` FOREIGN KEY (`concert_id`) REFERENCES `concerts` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `chk_queue_logs_rank_nonneg` CHECK (`rank_position` IS NULL OR `rank_position` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='대기열 감사 로그';

-- 8) user_tokens
CREATE TABLE `user_tokens`
(
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `user_id`            BIGINT UNSIGNED NOT NULL,
    `refresh_token_hash` VARCHAR(255)    NOT NULL,
    `expires_at`         DATETIME(6)     NOT NULL,
    `created_at`         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_tokens_rth` (`refresh_token_hash`),
    KEY `idx_user_tokens_user` (`user_id`),
    KEY `idx_user_tokens_expires_at` (`expires_at`),
    CONSTRAINT `fk_user_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='리프레시 토큰 저장소';

-- 9) wallet_accounts
CREATE TABLE `wallet_accounts`
(
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT 'PK/FK: users.id',
    `balance`    BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '잔액(원)',
    `version`    BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '낙관적 락 버전',
    `created_at` DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_wallet_accounts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `chk_wallet_accounts_balance_nonneg` CHECK (`balance` >= 0)
);


-- 10) wallet_transactions
CREATE TABLE `wallet_transactions`
(
    `id`                 BIGINT UNSIGNED                  NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `user_id`            BIGINT UNSIGNED                  NOT NULL,
    `related_payment_id` BIGINT UNSIGNED                           DEFAULT NULL,
    `amount`             BIGINT UNSIGNED                  NOT NULL,
    `type`               ENUM ('CHARGE','DEBIT','REFUND') NOT NULL,
    `idempotency_key`    CHAR(36)                                  DEFAULT NULL,
    `created_at`         DATETIME(6)                      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wallet_txn_user_idempo` (`user_id`, `idempotency_key`),
    KEY `idx_wallet_txn_user` (`user_id`),
    KEY `idx_wallet_txn_payment` (`related_payment_id`),
    CONSTRAINT `fk_wallet_txn_payment` FOREIGN KEY (`related_payment_id`) REFERENCES `payments` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT `fk_wallet_txn_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `chk_wallet_txn_amount_nonneg` CHECK (`amount` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='지갑 거래 원장(불변)';

-- 11) idempotency_keys
CREATE TABLE `idempotency_keys`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `request_key` VARCHAR(128) NOT NULL COMMENT '요청 멱등 키(엔드포인트+사용자)',
    `created_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`  DATETIME(6)  NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    `deleted_at`  DATETIME(6)  NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_idempotency_request_key` (`request_key`),
    KEY `idx_idempotency_created_at` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='멱등 키 저장소';

-- ============================================================
-- Seed Data (>=5 rows per table)
-- ============================================================

-- users (5)
INSERT INTO users (id, user_uuid, email, password, name)
VALUES (1, '11111111-1111-1111-1111-111111111111', 'user1@test.com', '{noop}hash', '유저1'),
       (2, '22222222-2222-2222-2222-222222222222', 'user2@test.com', '{noop}hash', '유저2'),
       (3, '33333333-3333-3333-3333-333333333333', 'user3@test.com', '{noop}hash', '유저3'),
       (4, '44444444-4444-4444-4444-444444444444', 'user4@test.com', '{noop}hash', '유저4'),
       (5, '55555555-5555-5555-5555-555555555555', 'user5@test.com', '{noop}hash', '유저5');

-- 지갑 계정 초기 데이터
INSERT INTO wallet_accounts (user_id, balance, version, created_at)
VALUES (1, 100000, 0, CURRENT_TIMESTAMP(6)),
       (2, 50000, 0, CURRENT_TIMESTAMP(6)),
       (3, 80000, 0, CURRENT_TIMESTAMP(6)),
       (4, 20000, 0, CURRENT_TIMESTAMP(6)),
       (5, 0, 0, CURRENT_TIMESTAMP(6));

-- concerts
INSERT INTO concerts (id, title, description, artist_name, organizer_name, is_open)
VALUES (1, '락 페스티벌', '락 공연', '더락밴드', '락컴퍼니', 1),
       (2, '재즈 나이트', '재즈 공연', '재즈트리오', '재즈에이전시', 1),
       (3, '클래식 갈라', '오케스트라', '심포니', '클래식하우스', 1),
       (4, 'K-POP 콘서트', '아이돌 콘서트', '아이돌X', '케이팝엔터', 1),
       (5, '인디 라이브', '인디 공연', '인디싱어', '인디레이블', 1);

-- concert_dates
INSERT INTO concert_dates (id, concert_id, event_date, venue_name, is_open)
VALUES (1, 1, '2025-12-01', '서울 올림픽홀', 1),
       (2, 1, '2025-12-02', '서울 올림픽홀', 1),
       (3, 2, '2025-11-28', '블루노트홀', 1),
       (4, 3, '2025-12-10', '예술의전당', 1),
       (5, 4, '2025-12-15', '고척스카이돔', 1),
       (6, 5, '2025-11-30', '홍대 라이브클럽', 1);

-- concert_seats  — (concert_date_id, section, seat_no) 유니크 보장 + status 반영
INSERT INTO concert_seats (id, concert_date_id, seat_no, section, price, status)
VALUES
-- date 1
(1, 1, 1, 'A', 70000, 'CONFIRMED'),                -- 예약 확정 좌석
(2, 1, 2, 'A', 70000, 'HELD'),                     -- 보류(PENDING) 좌석
(3, 1, 10, 'B', 60000, 'AVAILABLE'),
-- date 2
(4, 2, 1, 'A', 70000, 'AVAILABLE'),
(5, 2, 2, 'A', 70000, 'AVAILABLE'),
-- date 3
(6, 3, 5, 'S', 90000, 'CONFIRMED'),
(7, 3, 6, 'S', 90000, 'AVAILABLE'),
-- date 4
(8, 4, 25, 'R', 80000, 'CANCELED' IS NOT NULL),    -- dummy to keep SQL valid; will set AVAILABLE below
(9, 4, 26, 'R', 80000, 'AVAILABLE'),
-- date 5
(10, 5, 50, 'VIP', 120000, 'EXPIRED' IS NOT NULL), -- dummy; will set AVAILABLE below
-- date 6
(11, 6, 3, 'A', 50000, 'HELD'),
(12, 6, 4, 'A', 50000, 'AVAILABLE');

-- 위 두 줄(8,10)의 의도치 않은 주석-표현을 정정 (MySQL에서 ENUM 값만 허용되므로 UPDATE로 정상화)
UPDATE concert_seats
SET status='AVAILABLE'
WHERE id IN (8, 10);

-- reservations  — PENDING이면 hold_expires_at 필수
INSERT INTO reservations
(id, user_id, concert_id, concert_date_id, seat_id, status, amount, hold_expires_at, confirmed_at, canceled_at,
 expired_at, version)
VALUES (1, 1, 1, 1, 1, 'CONFIRMED', 70000, NULL, CURRENT_TIMESTAMP(6), NULL, NULL, 0),
       (2, 2, 1, 1, 2, 'PENDING', 70000, CURRENT_TIMESTAMP(6) + INTERVAL 15 MINUTE, NULL, NULL, NULL, 0),
       (3, 3, 2, 3, 6, 'CONFIRMED', 90000, NULL, CURRENT_TIMESTAMP(6), NULL, NULL, 0),
       (4, 4, 3, 4, 8, 'CANCELED', 80000, NULL, NULL, CURRENT_TIMESTAMP(6), NULL, 0),
       (5, 5, 4, 5, 10, 'EXPIRED', 120000, NULL, NULL, NULL, CURRENT_TIMESTAMP(6), 0),
       (6, 2, 5, 6, 11, 'PENDING', 50000, CURRENT_TIMESTAMP(6) + INTERVAL 10 MINUTE, NULL, NULL, NULL, 0);

-- payments — idempotency_key UNIQUE

INSERT INTO payments (
    reservation_id,
    user_id,
    amount,
    provider,
    provider_txn_id,
    status,
    idempotency_key,
    created_at,
    updated_at,
    deleted_at
) VALUES
-- 성공 결제 (user 1, reservation 1)
(1, 1, 60000, 'KAKAO_PAY', 'TXN-202510280001', 'SUCCEEDED', 'idem-0001-aaaa-bbbb-1111',
 NOW(6), NOW(6), NULL),
-- 실패 결제 (user 2, reservation 2)
(2, 2, 80000, 'NAVER_PAY', 'TXN-202510280002', 'FAILED', 'idem-0002-cccc-dddd-2222',
 NOW(6), NOW(6), NULL),

-- 결제 진행중 (user 3, reservation 3)
(3, 3, 45000, 'TOSS_PAY', NULL, 'PENDING', 'idem-0003-eeee-ffff-3333',
 NOW(6), NOW(6), NULL),

-- 성공 결제 (user 4, reservation 4)
(4, 4, 120000, 'CARD_VISA', 'TXN-202510280004', 'SUCCEEDED', 'idem-0004-gggg-hhhh-4444',
 NOW(6), NOW(6), NULL),

-- 실패 결제 (user 5, reservation 5)
(5, 5, 50000, 'APPLE_PAY', 'TXN-202510280005', 'FAILED', 'idem-0005-iiii-jjjj-5555',
 NOW(6), NOW(6), NULL);

-- queue_audit_logs
INSERT INTO queue_audit_logs (id, user_id, concert_id, status, rank_position, note)
VALUES (1, 1, 1, 'ACTIVE', 0, '진입'),
       (2, 2, 1, 'ISSUED', 5, '발급'),
       (3, 3, 2, 'EXPIRED', NULL, '만료'),
       (4, 4, 3, 'ISSUED', 12, NULL),
       (5, 5, 4, 'ACTIVE', 2, NULL),
       (6, 2, 5, 'ISSUED', 3, '대기');

-- user_tokens (>=5) — refresh_token_hash UNIQUE
INSERT INTO user_tokens (id, user_id, refresh_token_hash, expires_at, created_at)
VALUES (1, 1, 'rth-0001', CURRENT_TIMESTAMP(6) + INTERVAL 30 DAY, CURRENT_TIMESTAMP(6)),
       (2, 2, 'rth-0002', CURRENT_TIMESTAMP(6) + INTERVAL 30 DAY, CURRENT_TIMESTAMP(6)),
       (3, 3, 'rth-0003', CURRENT_TIMESTAMP(6) + INTERVAL 30 DAY, CURRENT_TIMESTAMP(6)),
       (4, 4, 'rth-0004', CURRENT_TIMESTAMP(6) + INTERVAL 30 DAY, CURRENT_TIMESTAMP(6)),
       (5, 5, 'rth-0005', CURRENT_TIMESTAMP(6) + INTERVAL 30 DAY, CURRENT_TIMESTAMP(6));

-- wallet_transactions
INSERT INTO wallet_transactions
(id, user_id, related_payment_id, amount, type, idempotency_key, created_at)
VALUES (1, 1, NULL, 50000, 'CHARGE', 'wtx-1-u1', CURRENT_TIMESTAMP(6)),
       (2, 1, 1, 70000, 'DEBIT', 'wtx-2-u1', CURRENT_TIMESTAMP(6)),
       (3, 2, NULL, 30000, 'CHARGE', 'wtx-1-u2', CURRENT_TIMESTAMP(6)),
       (4, 3, 2, 90000, 'DEBIT', 'wtx-1-u3', CURRENT_TIMESTAMP(6)),
       (5, 4, 3, 80000, 'REFUND', 'wtx-1-u4', CURRENT_TIMESTAMP(6)),
       (6, 5, NULL, 20000, 'CHARGE', 'wtx-1-u5', CURRENT_TIMESTAMP(6));

-- idempotency_keys
INSERT INTO idempotency_keys (id, request_key, created_at)
VALUES (1, 'POST:/api/v1/wallet/charge:user1:2025-10-28T00:00:00Z', CURRENT_TIMESTAMP(6)),
       (2, 'POST:/api/v1/reservations:user2:2025-10-28T00:01:00Z', CURRENT_TIMESTAMP(6)),
       (3, 'POST:/api/v1/payments:user3:2025-10-28T00:02:00Z', CURRENT_TIMESTAMP(6)),
       (4, 'POST:/api/v1/queue/issue:user4:2025-10-28T00:03:00Z', CURRENT_TIMESTAMP(6)),
       (5, 'POST:/api/v1/tokens/refresh:user5:2025-10-28T00:04:00Z', CURRENT_TIMESTAMP(6));
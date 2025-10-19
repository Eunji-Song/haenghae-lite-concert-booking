-- ------------------------------------------------------------
-- hhplus 초기화 스크립트 (KST, utf8mb4)
-- ------------------------------------------------------------

-- 데이터베이스 생성 및 선택
CREATE
DATABASE IF NOT EXISTS hhplus
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE
hhplus;

-- 타임존 (컨테이너 부팅 시 --default-time-zone=+09:00 로도 설정됨)
SET
GLOBAL time_zone = '+09:00';

-- 안전을 위해 문자셋 지정
SET NAMES utf8mb4;

-- ============================================================
-- 테이블 스키마
-- ============================================================

-- Create syntax for TABLE 'concert_dates'
CREATE TABLE `concert_dates`
(
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `concert_id` bigint unsigned NOT NULL COMMENT 'FK: concerts.id (공연 참조)',
    `event_date` date         NOT NULL COMMENT '공연 날짜(YYYY-MM-DD, 로컬 KST 기준)',
    `venue_name` varchar(200) NOT NULL COMMENT '공연장 이름(간단 문자열)',
    `is_open`    tinyint(1) NOT NULL DEFAULT '0' COMMENT '해당 날짜 예매 오픈 여부(1=오픈)',
    `created_at` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    `updated_at` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    `deleted_at` timestamp NULL DEFAULT NULL COMMENT '삭제 시각',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_concert_date` (`concert_id`,`event_date`),
    KEY          `idx_concert_dates_open` (`is_open`),
    CONSTRAINT `fk_concert_dates_concert` FOREIGN KEY (`concert_id`) REFERENCES `concerts` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='공연 일자';

-- Create syntax for TABLE 'concert_seats'
CREATE TABLE `concert_seats`
(
    `id`              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `concert_date_id` bigint unsigned NOT NULL COMMENT 'FK: concert_dates.id (해당 일자)',
    `seat_no`         int unsigned NOT NULL COMMENT '좌석 번호(단일 번호 체계)',
    `section`         varchar(50)        DEFAULT NULL COMMENT '구역/층/블록(옵션)',
    `price`           bigint unsigned NOT NULL COMMENT '좌석 가격(원)',
    `created_at`      timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    `updated_at`      timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    `deleted_at`      timestamp NULL DEFAULT NULL COMMENT '삭제 시각',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_concert_seat` (`concert_date_id`,`seat_no`),
    UNIQUE KEY `uk_seat_id_date` (`id`,`concert_date_id`),
    KEY               `idx_concert_seats_price` (`price`),
    CONSTRAINT `fk_concert_seats_date` FOREIGN KEY (`concert_date_id`) REFERENCES `concert_dates` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='공연 좌석(일자별)';

-- Create syntax for TABLE 'concerts'
CREATE TABLE `concerts`
(
    `id`             bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `title`          varchar(255) NOT NULL COMMENT '공연 제목',
    `description`    text COMMENT '공연 상세 설명(소개/주의사항 등)',
    `artist_name`    varchar(255)          DEFAULT NULL COMMENT '주요 아티스트명(초기 버전: 문자열 보관)',
    `organizer_name` varchar(255)          DEFAULT NULL COMMENT '주최/주관사명(초기 버전: 문자열 보관)',
    `is_open`        tinyint(1) NOT NULL DEFAULT '1' COMMENT '공연 공개/활성 여부(1=활성)',
    `created_at`     timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    `updated_at`     timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    `deleted_at`     timestamp NULL DEFAULT NULL COMMENT '삭제 시각',
    PRIMARY KEY (`id`),
    KEY              `idx_concerts_title` (`title`),
    KEY              `idx_concerts_open` (`is_open`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='공연 마스터';

-- Create syntax for TABLE 'payments'
CREATE TABLE `payments`
(
    `id`              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `reservation_id`  bigint unsigned NOT NULL COMMENT 'FK: reservations.id (대상 예약)',
    `amount`          bigint unsigned NOT NULL COMMENT '결제 금액(원)',
    `provider`        varchar(50)        DEFAULT NULL COMMENT '결제 수단/PG(가상: VIRTUAL 등)',
    `provider_txn_id` varchar(100)       DEFAULT NULL COMMENT '외부 거래 ID(가상은 내부 생성 ID)',
    `status`          enum('SUCCEEDED','FAILED','PENDING') NOT NULL COMMENT '결제 상태',
    `idempotency_key` char(36)  NOT NULL COMMENT '멱등 키(UUID, 결제 재시도 보호)',
    `created_at`      timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    `updated_at`      timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    `deleted_at`      timestamp NULL DEFAULT NULL COMMENT '삭제 시각',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payments_idempo` (`idempotency_key`),
    KEY               `idx_payments_reservation` (`reservation_id`),
    KEY               `idx_payments_status` (`status`),
    CONSTRAINT `fk_payments_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservations` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='결제(가상)';

-- Create syntax for TABLE 'queue_audit_logs'
CREATE TABLE `queue_audit_logs`
(
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `user_id`       bigint unsigned NOT NULL COMMENT 'FK: users.id (대기 사용자)',
    `concert_id`    bigint unsigned NOT NULL COMMENT 'FK: concerts.id (해당 콘서트)',
    `status`        enum('ISSUED','ACTIVE','EXPIRED') NOT NULL COMMENT '대기열 토큰 상태 스냅샷',
    `rank_position` int unsigned DEFAULT NULL COMMENT '당시 대기 순번(0이 맨 앞, 없으면 NULL)',
    `note`          varchar(255)       DEFAULT NULL COMMENT '비고/사유(선택)',
    `created_at`    timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '레코드 생성 시각(이벤트 발생 시각)',
    `updated_at`    timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각(일반적으론 미사용)',
    `deleted_at`    timestamp NULL DEFAULT NULL COMMENT '삭제 시각',
    PRIMARY KEY (`id`),
    KEY             `idx_queue_logs_user` (`user_id`),
    KEY             `idx_queue_logs_concert` (`concert_id`),
    KEY             `idx_queue_logs_status` (`status`),
    CONSTRAINT `fk_queue_logs_concert` FOREIGN KEY (`concert_id`) REFERENCES `concerts` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_queue_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='대기열 감사 로그(변경마다 INSERT, append-only)';

-- Create syntax for TABLE 'reservations'
CREATE TABLE `reservations`
(
    `id`              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `user_id`         bigint unsigned NOT NULL COMMENT 'FK: users.id (예약자)',
    `concert_date_id` bigint unsigned NOT NULL COMMENT 'FK: concert_dates.id (공연 일자)',
    `seat_id`         bigint unsigned NOT NULL COMMENT 'FK: concert_seats.id (좌석 참조)',
    `status`          enum('PENDING','CONFIRMED','CANCELED','EXPIRED') NOT NULL COMMENT '예약 상태',
    `amount`          bigint unsigned NOT NULL COMMENT '결제 예정/실제 금액(원)',
    `hold_expires_at` datetime           DEFAULT NULL COMMENT '홀드 만료 시각(KST, PENDING 일 때 설정)',
    `confirmed_at`    datetime           DEFAULT NULL COMMENT '확정 시각(KST)',
    `canceled_at`     datetime           DEFAULT NULL COMMENT '취소 시각(KST)',
    `expired_at`      datetime           DEFAULT NULL COMMENT '만료 처리 시각(KST)',
    `version`         bigint unsigned NOT NULL DEFAULT '0' COMMENT '낙관적 락 버전(애플리케이션 증가)',
    `created_at`      timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    `updated_at`      timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    `deleted_at`      timestamp NULL DEFAULT NULL COMMENT '삭제 시각',
    PRIMARY KEY (`id`),
    KEY               `idx_resv_user` (`user_id`),
    KEY               `idx_resv_date` (`concert_date_id`),
    KEY               `idx_resv_seat` (`seat_id`),
    KEY               `idx_resv_status` (`status`),
    KEY               `idx_resv_hold_exp` (`hold_expires_at`),
    KEY               `idx_resv_seat_date` (`seat_id`,`concert_date_id`),
    CONSTRAINT `fk_resv_date` FOREIGN KEY (`concert_date_id`) REFERENCES `concert_dates` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_resv_seat` FOREIGN KEY (`seat_id`) REFERENCES `concert_seats` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_resv_seat_date_consistency` FOREIGN KEY (`seat_id`, `concert_date_id`) REFERENCES `concert_seats` (`id`, `concert_date_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_resv_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='예약(홀드/확정/취소/만료)';

-- Create syntax for TABLE 'user_tokens'
CREATE TABLE `user_tokens`
(
    `id`                 bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `user_id`            bigint unsigned NOT NULL COMMENT 'FK: users.id (토큰 소유자)',
    `refresh_token_hash` varchar(255) NOT NULL COMMENT 'Refresh 토큰 해시(원문 미보관)',
    `expires_at`         datetime     NOT NULL COMMENT '만료 일시(KST, 비즈니스 시각)',
    `created_at`         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    `updated_at`         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    `deleted_at`         timestamp NULL DEFAULT NULL COMMENT '삭제 시각',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_tokens_rth` (`refresh_token_hash`),
    KEY                  `idx_user_tokens_user` (`user_id`),
    CONSTRAINT `fk_user_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Refresh 토큰 저장소(검증용 해시)';

-- Create syntax for TABLE 'users'
CREATE TABLE `users`
(
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'PK: 내부 식별자',
    `user_uuid`  char(36)     NOT NULL COMMENT '외부 노출용 UUID(불변)',
    `email`      varchar(255) NOT NULL COMMENT '로그인 ID(이메일, 유니크)',
    `password`   varchar(255) NOT NULL COMMENT '해시된 비밀번호(BCrypt/Argon2 권장)',
    `name`       varchar(50)  NOT NULL COMMENT '사용자 이름',
    `created_at` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '레코드 생성 시각',
    `updated_at` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '레코드 수정 시각',
    `deleted_at` timestamp NULL DEFAULT NULL COMMENT '소프트 삭제 시각',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_uuid` (`user_uuid`),
    UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='회원';

-- Create syntax for TABLE 'wallet_accounts'
CREATE TABLE `wallet_accounts`
(
    `user_id`    bigint unsigned NOT NULL COMMENT 'PK/FK: users.id (1:1 지갑)',
    `balance`    bigint unsigned NOT NULL DEFAULT '0' COMMENT '현재 잔액(원, 음수 불가)',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    `deleted_at` timestamp NULL DEFAULT NULL COMMENT '삭제 시각',
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_wallet_accounts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='지갑 계정(현재 잔액)';

-- Create syntax for TABLE 'wallet_transactions'
CREATE TABLE `wallet_transactions`
(
    `id`                 bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `user_id`            bigint unsigned NOT NULL COMMENT 'FK: users.id (지갑 소유자)',
    `related_payment_id` bigint unsigned DEFAULT NULL COMMENT '연관 결제 ID(NULL 가능, 예: 충전은 결제와 무관할 수 있음)',
    `amount`             bigint unsigned NOT NULL COMMENT '거래 금액(원, CHARGE/REFUND=증가, DEBIT=감소)',
    `type`               enum('CHARGE','DEBIT','REFUND') NOT NULL COMMENT '거래 유형',
    `idempotency_key`    char(36)           DEFAULT NULL COMMENT '멱등 키(사용자 범위, 중복 방지)',
    `created_at`         timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    `updated_at`         timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    `deleted_at`         timestamp NULL DEFAULT NULL COMMENT '삭제 시각',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wallet_txn_user_idempo` (`user_id`,`idempotency_key`),
    KEY                  `idx_wallet_txn_user` (`user_id`),
    KEY                  `idx_wallet_txn_payment` (`related_payment_id`),
    CONSTRAINT `fk_wallet_txn_payment` FOREIGN KEY (`related_payment_id`) REFERENCES `payments` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT `fk_wallet_txn_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='지갑 거래 원장(불변 원장)';
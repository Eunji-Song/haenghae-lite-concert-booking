-- 테스트용 사용자 데이터
INSERT INTO users (id, username, email, created_at)
VALUES (1, 'testuser1', 'testuser1@test.com', CURRENT_TIMESTAMP),
       (2, 'testuser2', 'testuser2@test.com', CURRENT_TIMESTAMP);

-- 테스트용 지갑 계정 데이터
INSERT INTO wallet_accounts (id, user_id, balance, created_at)
VALUES (1, 1, 100000, CURRENT_TIMESTAMP),
       (2, 2, 50000, CURRENT_TIMESTAMP);

-- 테스트용 콘서트 데이터
INSERT INTO concerts (id, name, artist, venue, created_at)
VALUES (1, '테스트 콘서트', '테스트 아티스트', '테스트 공연장', CURRENT_TIMESTAMP);

-- 테스트용 콘서트 일자 데이터
INSERT INTO concert_dates (id, concert_id, date, capacity, created_at)
VALUES (1, 1, '2025-09-10', 100, CURRENT_TIMESTAMP);

-- 테스트용 콘서트 좌석 데이터
INSERT INTO concert_seats (id, concert_date_id, seat_no, section, price, created_at)
VALUES (1, 1, 1, 'A구역', 50000, CURRENT_TIMESTAMP),
       (2, 1, 2, 'A구역', 50000, CURRENT_TIMESTAMP),
       (3, 1, 3, 'A구역', 50000, CURRENT_TIMESTAMP),
       (4, 1, 4, 'A구역', 50000, CURRENT_TIMESTAMP),
       (5, 1, 5, 'A구역', 50000, CURRENT_TIMESTAMP),
       (6, 1, 10, 'B구역', 40000, CURRENT_TIMESTAMP),
       (7, 1, 11, 'B구역', 40000, CURRENT_TIMESTAMP),
       (8, 1, 12, 'B구역', 40000, CURRENT_TIMESTAMP),
       (9, 1, 13, 'B구역', 40000, CURRENT_TIMESTAMP),
       (10, 1, 20, 'C구역', 30000, CURRENT_TIMESTAMP);

-- 테스트 코드에서 사용하는 특정 데이터
-- USER_ID = 1L, CONCERT_ID = 1L, DATE = "2025-09-10", SEAT_NO = 12L
-- 이미 위에서 좌석 12번이 생성되어 있음 (id=8)

-- 추가적인 테스트용 데이터 (동시 예약 테스트용)
INSERT INTO concert_seats (id, concert_date_id, seat_no, section, price, created_at)
VALUES (11, 1, 15, 'B구역', 40000, CURRENT_TIMESTAMP),
       (12, 1, 16, 'B구역', 40000, CURRENT_TIMESTAMP),
       (13, 1, 17, 'B구역', 40000, CURRENT_TIMESTAMP);
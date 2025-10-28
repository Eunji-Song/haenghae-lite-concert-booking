-- users
INSERT INTO users (id, user_uuid, email, password, name, created_at, updated_at)
VALUES (1, 'testuser-1', 'testuser1@test.com', '{noop}hash', '테스터1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       (2, 'testuser-2', 'testuser2@test.com', '{noop}hash', '테스터2', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

-- wallet_accounts  (PK = user_id)
INSERT INTO wallet_accounts (user_id, balance, created_at)
VALUES (1, 100000, CURRENT_TIMESTAMP(6)),
       (2,  50000, CURRENT_TIMESTAMP(6));

-- concerts
INSERT INTO concerts (id, title, description, artist_name, organizer_name, is_open, created_at, updated_at)
VALUES (1, '테스트 콘서트', '설명', '테스트 아티스트', '테스트 주최', 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

-- concert_dates  (event_date, venue_name, is_open)
INSERT INTO concert_dates (id, concert_id, event_date, venue_name, is_open, created_at, updated_at)
VALUES (1, 1, '2025-09-10', '테스트 공연장', 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

-- concert_seats (status 컬럼을 쓰지 않는 설계라면 제외, 우리 엔티티는 status 필드 없음)
INSERT INTO concert_seats (id, concert_date_id, seat_no, section, price, created_at, updated_at)
VALUES (1, 1, 1,  'A', 50000, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       (2, 1, 2,  'A', 50000, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       (3, 1, 3,  'A', 50000, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       (4, 1, 4,  'A', 50000, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       (5, 1, 5,  'A', 50000, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       (6, 1, 10, 'B', 40000, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       (7, 1, 11, 'B', 40000, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       (8, 1, 12, 'B', 40000, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       (9, 1, 13, 'B', 40000, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       (10,1, 20, 'C', 30000, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       (11,1, 15, 'B', 40000, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       (12,1, 16, 'B', 40000, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       (13,1, 17, 'B', 40000, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
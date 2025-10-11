-- 테스트용 이벤트 데이터
INSERT INTO events (name, description, event_date, venue, sale_start_at, sale_end_at, status, created_at, updated_at)
VALUES
  ('아이유 콘서트 2025', '아이유 전국 투어 콘서트', '2025-06-15 19:00:00', '올림픽공원 체조경기장', '2025-01-01 10:00:00', '2025-06-14 23:59:59', 'SELLING', NOW(), NOW()),
  ('BTS 월드투어', 'BTS WORLD TOUR', '2025-07-20 19:00:00', '잠실 종합운동장', '2025-02-01 10:00:00', '2025-07-19 23:59:59', 'SCHEDULED', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- 테스트용 상품 데이터
INSERT INTO products (event_id, name, description, price, total_stock, available_stock, sale_start_at, sale_end_at, created_at, updated_at)
VALUES
  (1, 'VIP석', 'VIP 좌석 (특전 포함)', 150000, 100, 100, '2025-01-01 10:00:00', '2025-06-14 23:59:59', NOW(), NOW()),
  (1, '일반석', '일반 좌석', 80000, 500, 500, '2025-01-01 10:00:00', '2025-06-14 23:59:59', NOW(), NOW()),
  (1, '스탠딩', '스탠딩 구역', 50000, 1000, 1000, '2025-01-01 10:00:00', '2025-06-14 23:59:59', NOW(), NOW()),
  (2, 'VIP석', 'VIP 좌석', 200000, 50, 50, '2025-02-01 10:00:00', '2025-07-19 23:59:59', NOW(), NOW())
ON CONFLICT DO NOTHING;

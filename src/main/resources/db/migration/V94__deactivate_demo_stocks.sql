-- 데모용 가짜 종목을 비활성화한다. (실데이터 전환)
UPDATE stocks SET is_active = FALSE WHERE code IN ('BRIFO01', 'BRIFO02', 'BRIFO03');

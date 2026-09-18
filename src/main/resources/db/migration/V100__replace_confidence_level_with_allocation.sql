-- 확신도(1~5) 기반 예측 등록을 배분 금액(allocated_ap) 기반으로 전환한다.
-- 기존 행은 등록 당시 고정 참가비(1,000 AP)를 냈으므로 allocated_ap는 1,000으로,
-- allocation_rate_percent는 과거 confidence_level(1~5)을 1~40% 스케일로 근사 환산해 채운다.
ALTER TABLE decisions ADD COLUMN allocated_ap INT;
ALTER TABLE decisions ADD COLUMN allocation_rate_percent SMALLINT;

UPDATE decisions SET
    allocated_ap = 1000,
    allocation_rate_percent = LEAST(confidence_level * 8, 40);

ALTER TABLE decisions ALTER COLUMN allocated_ap SET NOT NULL;
ALTER TABLE decisions ALTER COLUMN allocation_rate_percent SET NOT NULL;
ALTER TABLE decisions DROP COLUMN confidence_level;

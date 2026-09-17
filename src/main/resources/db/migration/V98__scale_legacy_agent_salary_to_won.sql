-- 포인트를 원화 단위로 올리면서(V96) 신규 가입자의 기본 의뢰비는 UserService의
-- DEFAULT_AGENT_PROFILES 상수로 갱신했지만, 이미 가입해 사원을 보유한 기존 유저의
-- agents.daily_salary는 그대로 남아 "의뢰비 10원" 처럼 과거 스케일로 표시되고 있었다.
-- 온보딩 기본값과 정확히 일치하는 행만 골라 안전하게 새 스케일로 올린다.
UPDATE agents SET daily_salary = 50000 WHERE agent_type = 'ROOKIE' AND daily_salary = 10;
UPDATE agents SET daily_salary = 200000 WHERE agent_type = 'PRO' AND daily_salary = 25;
UPDATE agents SET daily_salary = 150000 WHERE agent_type = 'TANKER' AND daily_salary = 15;

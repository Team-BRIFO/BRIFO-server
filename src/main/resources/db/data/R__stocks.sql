INSERT INTO stocks (code, name, sector)
VALUES ('BRIFO01', '브리포테크', '브리포'),
       ('BRIFO02', '브리포시스템', '브리포'),
       ('BRIFO03', '브리포랩', '브리포')
ON CONFLICT (code) DO NOTHING;

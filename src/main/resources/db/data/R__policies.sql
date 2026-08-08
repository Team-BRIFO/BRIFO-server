INSERT INTO policies (code, title, content, is_required, version)
VALUES ('AGE_14_OR_OLDER', '만 14세 이상입니다.', '본인은 만 14세 이상임을 확인합니다.', TRUE, 1.0),
       ('TERMS_OF_SERVICE', '서비스 이용약관', '이용자는 서비스 이용약관을 준수하며, 제공되는 정보를 참고 목적으로 사용합니다.', TRUE, 1.0),
       ('PRIVACY_POLICY', '개인정보 처리방침', '서비스 제공에 필요한 개인정보를 수집·이용하며, 이용 목적이 달성되면 파기합니다.', TRUE, 1.0),
       ('INVESTMENT_INFORMATION_NOTICE', '투자 정보 유의사항', 'test', TRUE, 1.0),
       ('MARKETING_COMMUNICATION_CONSENT', '마케팅 정보 수신', '서비스 소식과 혜택 정보를 수신하며, 동의는 언제든지 철회할 수 있습니다.', FALSE, 1.0)
ON CONFLICT (code, version) DO NOTHING;

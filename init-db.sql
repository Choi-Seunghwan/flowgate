-- ReserveX 데이터베이스 초기화 스크립트

-- 데이터베이스 생성
CREATE DATABASE reservex_account;
CREATE DATABASE reservex_ticket;
CREATE DATABASE reservex_payment;

-- 권한 부여
GRANT ALL PRIVILEGES ON DATABASE reservex_account TO dev;
GRANT ALL PRIVILEGES ON DATABASE reservex_ticket TO dev;
GRANT ALL PRIVILEGES ON DATABASE reservex_payment TO dev;

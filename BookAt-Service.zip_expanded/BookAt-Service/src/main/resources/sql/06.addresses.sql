CREATE TABLE ADDRESSES (
	addr_id	number	PRIMARY KEY,
	user_id	varchar2(50)	NOT NULL,
	addr_name	varchar2(50)	NOT NULL,
	recipient_name	varchar2(50)	NOT NULL,
	recipient_phone	varchar2(20)	NOT NULL,
	addr	varchar2(2000)	NOT NULL
);
-- 테이블 조인
ALTER TABLE ADDRESSES ADD CONSTRAINT FK_users_TO_addr_1 FOREIGN KEY (user_id) REFERENCES users (user_id);

-- 시퀀스 생성
CREATE SEQUENCE SEQ_ADDR;

-- 테이블 확인
SELECT * FROM ADDRESSES ORDER BY ADDR_ID;
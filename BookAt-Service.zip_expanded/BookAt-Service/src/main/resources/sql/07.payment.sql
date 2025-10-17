CREATE TABLE PAYMENT (
	payment_id	number	PRIMARY KEY,
	total_price	number	NOT NULL,
	payment_price	number	NOT NULL,
	payment_method	varchar2(50)	NOT NULL,
	payment_status	number(2)	NOT NULL,
	payment_date	Date	DEFAULT SYSDATE NOT NULL,
	payment_info	varchar2(100)	NULL
);

-- 시퀀스 생성
CREATE SEQUENCE SEQ_PAYMENT;

-- 제약 조건
ALTER TABLE PAYMENT ADD CONSTRAINT CHK_PAYMENT_METHOD
CHECK (PAYMENT_METHOD IN ('CARD','VBANK','POINT'));


ALTER TABLE PAYMENT ADD CONSTRAINT CHK_PAYMENT_STATUS
CHECK (payment_status IN (0, 1, -1, 2, 3));

-- 결제 식별자/검증용
ALTER TABLE PAYMENT ADD (merchant_uid   VARCHAR2(64)  UNIQUE); -- 우리 서버가 만든 결제번호
ALTER TABLE PAYMENT ADD (imp_uid        VARCHAR2(64));         -- 포트원 결제 고유번호(성공 시)
ALTER TABLE PAYMENT ADD (order_id       NUMBER       NULL);
ALTER TABLE PAYMENT ADD CONSTRAINT FK_PAYMENT_BOOK_ORDER FOREIGN KEY (order_id) REFERENCES BOOK_ORDER(order_id);

-- 상태/오류/영수증/PG메타
ALTER TABLE PAYMENT ADD (fail_reason    VARCHAR2(400) NULL);
ALTER TABLE PAYMENT ADD (receipt_url    VARCHAR2(400) NULL);
ALTER TABLE PAYMENT ADD (pg_tid         VARCHAR2(100) NULL);   -- PG사 승인번호/거래ID

-- 인덱스
CREATE INDEX IDX_PAYMENT_DATE ON PAYMENT(payment_date);

INSERT INTO PAYMENT (payment_id, total_price, payment_price, payment_method, payment_status, payment_date, merchant_uid, payment_info)
VALUES (seq_payment.nextval,35000, 35000, 'CARD', 0, SYSDATE, 111 , '테스트결제');

ALTER TABLE PAYMENT ADD (user_id VARCHAR2(50) NULL);
ALTER TABLE PAYMENT ADD CONSTRAINT FK_PAYMENT_USER FOREIGN KEY (user_id) REFERENCES USERS(user_id);
COMMIT;

-- 테이블 수정
ALTER TABLE PAYMENT MODIFY (payment_info VARCHAR2(400 CHAR));
-- 테이블 확인
SELECT * FROM PAYMENT ORDER BY PAYMENT_ID;


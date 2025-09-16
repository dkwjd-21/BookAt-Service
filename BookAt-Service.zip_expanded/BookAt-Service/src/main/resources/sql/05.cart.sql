CREATE TABLE CART (
    cart_id varchar2(20) PRIMARY KEY,
	user_id	varchar2(50)	NOT NULL,
	book_id	varchar2(20)	NOT NULL,
	cart_quantity	number	NOT NULL,
	cart_regdate	Date	DEFAULT SYSDATE NOT NULL	
);

-- 테이블 조인
ALTER TABLE CART ADD CONSTRAINT FK_users_TO_cart_1 FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE CART ADD CONSTRAINT FK_book_TO_cart_1 FOREIGN KEY (book_id) REFERENCES book (book_id);

-- 시퀀스 생성
CREATE SEQUENCE SEQ_CART;

-- 테이블 확인
SELECT * FROM CART ORDER BY CART_ID;
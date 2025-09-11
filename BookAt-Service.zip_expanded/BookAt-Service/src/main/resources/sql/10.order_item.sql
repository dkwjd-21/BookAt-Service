CREATE TABLE ORDER_ITEM (
    order_item_id   NUMBER PRIMARY KEY,
	book_id	varchar2(20)	NOT NULL,
	order_id	number	NOT NULL,
	order_quantity	number	NOT NULL,
	item_price NUMBER NOT NULL
);

-- 시퀀스 생성
CREATE SEQUENCE SEQ_ORDER_ITEM;

-- 테이블 조인
ALTER TABLE ORDER_ITEM ADD CONSTRAINT FK_book_TO_order_item_1 FOREIGN KEY (book_id) REFERENCES book (book_id);

ALTER TABLE ORDER_ITEM ADD CONSTRAINT FK_BOOK_ORDER_TO_order_item_1 FOREIGN KEY (order_id) REFERENCES BOOK_ORDER (order_id);

-- 제약 조건
-- 한 주문에서 동일 도서 중복 라인 금지
ALTER TABLE ORDER_ITEM ADD CONSTRAINT UQ_ORDER_ITEM_BOOK UNIQUE (order_id, book_id);


-- 테이블 확인
SELECT * FROM ORDER_ITEM ORDER BY order_item_id;
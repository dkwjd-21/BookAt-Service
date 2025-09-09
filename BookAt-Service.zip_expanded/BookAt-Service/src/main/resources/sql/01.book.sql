

---------------------------------------------------------------------------------

-- 도서
CREATE TABLE BOOK (
  BOOK_ID     VARCHAR2(20) PRIMARY KEY,   -- 값은 ISBN
  BOOK_TITLE  VARCHAR2(500)    NOT NULL,  
  BOOK_COVER  VARCHAR2(1000)    NOT NULL,
  AUTHOR      VARCHAR2(200)    NOT NULL,
  BOOK_PRICE  NUMBER           NOT NULL,
  PUBLISHER   VARCHAR2(100)    NOT NULL,
  PUBDATE     DATE             NOT NULL,
  DESCRIPTION VARCHAR2(4000)    NOT NULL
);

---------------------------------------------------------------------------------

-- 도서 재고
CREATE TABLE BOOK_INVENTORY (
  BOOK_ID       VARCHAR2(20) PRIMARY KEY
                REFERENCES BOOK(BOOK_ID),
  BOOK_INVENTORY NUMBER    DEFAULT 10 NOT NULL
);

---------------------------------------------------------------------------------

-- 도서 구매
CREATE TABLE PURCHASE (
	PAYMENT_ID NUMBER PRIMARY KEY,
	TOTAL_PRICE NUMBER NOT NULL,
	PAYMENT_PRICE NUMBER NOT NULL,
	PAYMENT_METHOD VARCHAR2(50) NOT NULL,
	PAYMENT_STATUS NUMBER(2) NOT NULL,
	PAYMENT_DATE DATE NOT NULL,
	PAYMENT_INFO VARCHAR2(100) NULL
);
CREATE SEQUENCE SEQ_PURCHASE;

ALTER TABLE PURCHASE ADD CONSTRAINT CHK_PAYMENT_METHOD
CHECK (PAYMENT_METHOD IN ('CARD', 'VIRTUAL', 'POINT'));
ALTER TABLE PURCHASE ADD CONSTRAINT CHK_PAYMENT_STATUS
CHECK (PAYMENT_STATUS IN (0, 1, -1, 2));

---------------------------------------------------------------------------------

-- JSON 형식의 대용량 데이터를 임시로 저장하기 위한 테이블 
CREATE TABLE stage_book (
  doc CLOB CHECK (doc IS JSON)
);

-- STAGE_BOOK 테이블의 JSON 데이터를 조회하여 ITEMS의 총 개수 확인 
SELECT COUNT(*) AS json_item_cnt
FROM stage_book s,
     JSON_TABLE(
       s.doc,
       '$.books[*].items[*]'
       COLUMNS (
         isbn      VARCHAR2(30)  PATH '$.isbn'
       )
     );

-- JSON 데이터를 가공하여 BOOK 테이블에 삽입/업데이트 (MERGE)
MERGE INTO ADMIN.book b
USING (
  SELECT *
  FROM (
    SELECT
      REGEXP_REPLACE(jt.isbn,'[^0-9Xx]','')                  AS book_id,
      NULLIF(TRIM(jt.title),     '')                         AS book_title,
      NULLIF(TRIM(jt.image),     '')                         AS book_cover,
      NULLIF(TRIM(jt.author),    '')                         AS author,
      TO_NUMBER(NULLIF(TRIM(jt.discount),''))                AS book_price,
      NULLIF(TRIM(jt.publisher), '')                         AS publisher,
      TO_DATE(jt.pubdate,'YYYYMMDD')                         AS pubdate,
      jt.description                                          AS description,
      ROW_NUMBER() OVER (
        PARTITION BY REGEXP_REPLACE(jt.isbn,'[^0-9Xx]','')
        ORDER BY TO_DATE(jt.pubdate,'YYYYMMDD') DESC NULLS LAST
      ) rn
    FROM ADMIN.stage_book s,
         JSON_TABLE(
           s.doc, '$.books[*].items[*]'
           COLUMNS (
             title       VARCHAR2(500)  PATH '$.title',
             image       VARCHAR2(1000) PATH '$.image',
             author      VARCHAR2(200)  PATH '$.author',
             publisher   VARCHAR2(100)  PATH '$.publisher',
             pubdate     VARCHAR2(8)    PATH '$.pubdate',
             isbn        VARCHAR2(30)   PATH '$.isbn',
             discount    VARCHAR2(20)   PATH '$.discount',
             description CLOB           PATH '$.description'
           )
         ) jt
  )
  WHERE rn = 1
    AND book_id      IS NOT NULL
    AND book_title   IS NOT NULL
    AND book_cover   IS NOT NULL
    AND author       IS NOT NULL
    AND book_price   IS NOT NULL
    AND publisher    IS NOT NULL
    AND pubdate      IS NOT NULL
) src
ON (b.book_id = src.book_id)
WHEN MATCHED THEN UPDATE SET
  b.book_title  = src.book_title,
  b.book_cover  = src.book_cover,
  b.author      = src.author,
  b.book_price  = src.book_price,
  b.publisher   = src.publisher,
  b.pubdate     = src.pubdate,
  b.description = src.description
WHEN NOT MATCHED THEN INSERT
  (book_id, book_title, book_cover, author, book_price, publisher, pubdate, description)
VALUES
  (src.book_id, src.book_title, src.book_cover, src.author, src.book_price, src.publisher, src.pubdate, src.description);
COMMIT;

/* ISBN 중복값 + 테이블의 NOT NULL 제약으로 일부 빠지고 315개 테이블에 넣기 성공*/

-- 일부 유효하지 않은 데이터 삭제 
DELETE FROM BOOK
WHERE TO_NUMBER(BOOK_ID) BETWEEN 6000428595 AND 6000731272;

-- 카테고리 테이블 추가 
ALTER TABLE BOOK ADD (category VARCHAR2(50));

-- 생략 
UPDATE book SET category = 'KIDS' WHERE book_id = '9781524770488';



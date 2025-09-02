CREATE TABLE stage_book (
  doc CLOB CHECK (doc IS JSON)
);


CREATE TABLE book (
  book_Id     VARCHAR2(20) PRIMARY KEY,   -- 값은 ISBN
  book_title  VARCHAR2(500)    NOT NULL,  
  book_cover  VARCHAR2(1000)    NOT NULL,
  author      VARCHAR2(200)    NOT NULL,
  book_Price  NUMBER           NOT NULL,
  publisher   VARCHAR2(100)    NOT NULL,
  pubdate     DATE             NOT NULL,
  description VARCHAR2(4000)    NOT NULL
);


SELECT * FROM book;

CREATE TABLE book_inventory (
  book_Id       VARCHAR2(20) PRIMARY KEY
                REFERENCES book(book_Id),
  book_Inventory NUMBER      DEFAULT 10 NOT NULL
);

SELECT * FROM book_inventory;

SELECT COUNT(*) AS json_item_cnt
FROM stage_book s,
     JSON_TABLE(
       s.doc,
       '$.books[*].items[*]'
       COLUMNS (
         isbn      VARCHAR2(30)  PATH '$.isbn'
       )
     );


SELECT COUNT(*) AS c FROM book;



SELECT COUNT(*) AS before_cnt FROM book;

ALTER TABLE book MODIFY (description VARCHAR2(32767 CHAR));

DELETE FROM book;
COMMIT;


SELECT USER AS session_user,
       SYS_CONTEXT('USERENV','CURRENT_SCHEMA') AS current_schema
FROM dual;


SELECT owner, table_name
FROM   all_tables
WHERE  table_name IN ('BOOK','STAGE_BOOK','BOOK_INVENTORY');

ALTER SESSION SET CURRENT_SCHEMA=ADMIN;

DELETE FROM ADMIN.book; COMMIT;

ALTER TABLE ADMIN.book MODIFY (description NULL);

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

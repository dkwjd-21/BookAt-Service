-- REVIEW 테이블 생성
CREATE TABLE review (
	review_id	number	NOT NULL,
	user_id	varchar2(50)	NOT NULL,
	review_type	char(1)	NOT NULL,
	review_title	varchar2(255)	NOT NULL,
	review_content	varchar2(2000)	NOT NULL,
	review_date	date	DEFAULT SYSDATE NOT NULL,
	book_id	varchar2(20)	NULL,
	event_id	number	NULL,
	rating	number	NOT NULL
);

-- 테이블 조인
ALTER TABLE review ADD CONSTRAINT PK_REVIEW PRIMARY KEY (review_id);

ALTER TABLE review ADD CONSTRAINT FK_users_TO_review_1 FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE review ADD CONSTRAINT FK_book_TO_review_1 FOREIGN KEY (book_id) REFERENCES book (book_id);

ALTER TABLE review ADD CONSTRAINT FK_event_TO_review_1 FOREIGN KEY (event_id)REFERENCES event (event_id);

-- 제약 조건
ALTER TABLE REVIEW ADD CONSTRAINT CHK_REVIEW_TYPE 
CHECK (REVIEW_TYPE IN ('B', 'E'));
ALTER TABLE REVIEW ADD CONSTRAINT CHK_RATING 
CHECK (RATING IN (1,2,3,4,5));
ALTER TABLE review
  ADD CONSTRAINT chk_review_target
  CHECK ( (book_id IS NOT NULL) OR (event_id IS NOT NULL) );

-- 시퀀스 생성
CREATE SEQUENCE SEQ_REVIEW;


-- 도서 리뷰, 이벤트 리뷰 예시 데이터 추가
INSERT INTO REVIEW
  (review_id, user_id, review_type, review_title, review_content, review_date, book_id,event_id,rating)
VALUES
  (SEQ_REVIEW.NEXTVAL, 'userA', 'B', '도서 리뷰', '도서 리뷰 예시입니다. 재밌습니다!', SYSDATE, '9791199317017',NULL,5);

INSERT INTO REVIEW
  (review_id, user_id, review_type, review_title, review_content, review_date, book_id,event_id,rating)
VALUES
  (SEQ_REVIEW.NEXTVAL, 'userB', 'E', '이벤트 리뷰', '이벤트 리뷰 예시입니다. 재밌습니다!', SYSDATE,NULL,'70',4);

-- 테이블 확인
SELECT * FROM REVIEW ORDER BY REVIEW_ID;
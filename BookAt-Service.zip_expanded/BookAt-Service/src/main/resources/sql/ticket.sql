-- 추가 X
CREATE TABLE seat_grade (
	seat_grade_id NUMBER PRIMARY KEY,
	seat_grade_type varchar2(20) check(seat_grade_type IN ('STANDARD', 'ROYAL', 'VIP')) NOT NULL,
	seat_rate NUMBER check(seat_rate IN (100, 150, 200) NOT null
);

-- 이벤트 좌석
CREATE TABLE seat_type (
	seat_id NUMBER PRIMARY KEY,
	seat_name varchar2(50) NULL,
	seat_status NUMBER(2) check(seat_status IN (-1, 0, 1)) NOT NULL,
	seat_grade_type varchar2(20) check(seat_grade_type IN ('STANDARD', 'ROYAL', 'VIP')) NOT NULL,
	event_id NUMBER NOT NULL,
	schedule_id NUMBER NOT NULL
);

ALTER TABLE seat_type ADD CONSTRAINT FK_seat_event_id FOREIGN KEY (event_id) REFERENCES event (event_id);
ALTER TABLE seat_type ADD CONSTRAINT FK_seat_schedule_id FOREIGN KEY (schedule_id) REFERENCES event_part (schedule_id);
CREATE SEQUENCE SEQ_SEAT_TYPE;

-- 이벤트 예매
CREATE TABLE reservation (
	reservation_id NUMBER PRIMARY KEY,
	payment_id NUMBER NOT NULL,
	reservation_date DATE NOT NULL,
	reservation_status NUMBER(2) check(reservation_status IN (-1, 0, 1, 2)) NOT NULL,
	schedule_id NUMBER NOT NULL,
	user_id varchar2(50) NOT NULL
);

ALTER TABLE reservation ADD CONSTRAINT FK_reservation_payment_id FOREIGN KEY (payment_id) REFERENCES payment (payment_id);
ALTER TABLE reservation ADD CONSTRAINT FK_reservation_schedule_id FOREIGN KEY (schedule_id) REFERENCES event_part (schedule_id);
ALTER TABLE reservation ADD CONSTRAINT FK_reservation_user_id FOREIGN KEY (user_id) REFERENCES users (user_id);
CREATE SEQUENCE SEQ_RESERVATION;

-- 티켓
CREATE TABLE ticket (
	ticket_id NUMBER PRIMARY KEY,
	ticket_created_date DATE NOT NULL,
	ticket_status number(2) CHECK(ticket_status IN (-1, 0, 1)) NOT NULL,
	ticket_type varchar2(20) CHECK(ticket_type IN ('SEAT_TYPE', 'PERSON_TYPE')) NOT NULL,
	person_type varchar2(10) CHECK(person_type IN ('ADULT', 'YOUTH', 'CHILD')),
	reservation_id NUMBER NOT NULL,
	seat_id NUMBER,
	payment_id NUMBER NOT NULL
);

ALTER TABLE ticket ADD CONSTRAINT FK_ticket_reservation_id FOREIGN KEY (reservation_id) REFERENCES reservation (reservation_id);
ALTER TABLE ticket ADD CONSTRAINT FK_ticket_seat_id FOREIGN KEY (seat_id) REFERENCES seat_type (seat_id);
ALTER TABLE ticket ADD CONSTRAINT FK_ticket_payment_id FOREIGN KEY (payment_id) REFERENCES payment (payment_id);
CREATE SEQUENCE SEQ_TICKET;

SELECT * FROM seat_type;
SELECT * FROM reservation;
SELECT * FROM ticket;

SELECT * FROM payment;
SELECT * FROM event_part;
-- 이벤트 회차 테이블에 잔여수량 컬럼 추가
ALTER TABLE EVENT_PART ADD (REMAINING_SEAT NUMBER NOT NULL);

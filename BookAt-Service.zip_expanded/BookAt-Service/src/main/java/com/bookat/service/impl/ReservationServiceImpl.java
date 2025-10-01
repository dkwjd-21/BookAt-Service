package com.bookat.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookat.domain.ReservationStatus;
import com.bookat.domain.TicketStatus;
import com.bookat.dto.EventSeatDto;
import com.bookat.dto.reservation.PaymentInfoResDto;
import com.bookat.dto.reservation.PersonTypeReqDto;
import com.bookat.dto.reservation.ReservationStartDto;
import com.bookat.dto.reservation.SeatTypeReqDto;
import com.bookat.dto.reservation.UserInfoReqDto;
import com.bookat.entity.Event;
import com.bookat.entity.reservation.EventPart;
import com.bookat.entity.reservation.Reservation;
import com.bookat.entity.reservation.Ticket;
import com.bookat.enums.PersonType;
import com.bookat.mapper.EventPartMapper;
import com.bookat.mapper.ReservationMapper;
import com.bookat.mapper.TicketMapper;
import com.bookat.service.ReservationService;
import com.bookat.util.ReservationRedisUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

	private final ReservationMapper reservationMapper;
	private final TicketMapper ticketMapper;
	private final EventPartMapper eventPartMapper;
	private final StringRedisTemplate redisTemplate;
	private final SeatServiceImpl seatService;
	private final ReservationRedisUtil redisUtil;
	private final QueueServiceImpl queueService;

	// 예매 시작 (초기 진입)
	@Override
	public ReservationStartDto startReservation(int eventId, String userId) {

		// 이벤트 조회
		Event event = reservationMapper.findEventByEventId(eventId);
		// 이벤트 회차 조회
		List<EventPart> eventParts = eventPartMapper.findEventPartsByEventId(eventId);
		// 이벤트 날짜
		LocalDate eventDate = null;
		// 이벤트 회차 조회
		eventParts.sort(Comparator.comparing(EventPart::getScheduleTime));

		// 이벤트의 날짜
		if (eventParts != null) {
			for (EventPart part : eventParts) {
				Date scheduleTime = part.getScheduleTime();
				eventDate = scheduleTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			}
		}

		// person type - 잔여좌석 불러오기
		if ("PERSON_TYPE".equals(event.getTicketType())) {
			for (EventPart eventPart : eventParts) {
				String redisKey = String.format("EVENT:%d:SCHEDULE:%d:AVAILABLE_SEAT", eventId,
						eventPart.getScheduleId());
				String availableSeat = redisTemplate.opsForValue().get(redisKey);

				if (availableSeat != null) {
					eventPart.setRemainingSeat(Integer.parseInt(availableSeat));
				} else {
					eventPart.setRemainingSeat(0);
				}
			}
		}

		// 예약 세션 생성
		String reservationToken = redisUtil.createInit(eventId, event.getEventName(), userId,
				String.valueOf(eventDate));
		return new ReservationStartDto(event, eventParts, reservationToken);
	}

	// step1 : 날짜/회차 선택
	@Override
	public void selectSchedule(String reservationToken, int scheduleId) {
		if (!redisUtil.validateReservationSession(reservationToken)) {
			throw new IllegalStateException("예약 세션이 만료되었습니다. 다시 예약해주세요.");
		}

		Map<Object, Object> existingData = redisUtil.getDataAll(reservationToken);
		String prevEventId = (String) existingData.get("eventId");
		String prevScheduleId = (String) existingData.get("scheduleId");

		// 회차 변경 희망 시 이전 회차와 동일하면 (그냥 변경없이 다시 이후 단계로 진행하고자 할 때) 아무 작업도 하지 않음
		if (prevScheduleId != null && prevScheduleId.equals(String.valueOf(scheduleId))) {
			log.info("같은 회차 재선택: eventId={}, scheduleId={}, 변경 없음", prevEventId, prevScheduleId);
			return;
		}

		// 회차를 변경할 경우 기존 좌석의 복구처리
		if (prevEventId != null && prevScheduleId != null) {
			int result = redisUtil.rollbackOnScheduleChange(reservationToken, prevEventId, prevScheduleId);
			log.info("회차 변경으로 좌석 복구: eventId={}, scheduleId={}, 복구좌석={}", prevEventId, prevScheduleId, result);

		}

		// 회차 저장 & STEP2 세팅
		redisUtil.updateStep1(reservationToken, scheduleId);
	}

	// step2 : 인원등급/인원수 선택
	@Override
	public void selectPersonType(String reservationToken, PersonTypeReqDto personTypeReqDto) {
		if (!redisUtil.validateReservationSession(reservationToken)) {
			throw new IllegalStateException("예약 세션이 만료되었습니다. 다시 예약해주세요.");
		}

		String eventId = redisUtil.getDataField(reservationToken, "eventId");
		String scheduleId = redisUtil.getDataField(reservationToken, "scheduleId");

		if (eventId == null || scheduleId == null) {
			throw new IllegalStateException("예약 토큰에 회차 정보가 없습니다.");
		}

		// 기존에 요청받았던 인원
		int prevTotal = Optional.ofNullable(redisUtil.getDataField(reservationToken, "reservedCount"))
				.map(Integer::parseInt).orElse(0);

		// 새로 요청받은 인원 합계
		int totalPersonCount = personTypeReqDto.getPersonCounts().values().stream().mapToInt(Integer::intValue).sum();

		int diff = totalPersonCount - prevTotal;

		if (diff != 0) {
			int result = redisUtil.adjustSeatsAndUpdateStep2PT(reservationToken, eventId, scheduleId, diff,
					totalPersonCount, personTypeReqDto.getTotalPrice(), personTypeReqDto.getPersonCounts());
			if (result == -1) {
				throw new IllegalArgumentException(String.format("잔여 좌석 부족\n요청 %d석, 기존 인원 %d, 새로 추가된 인원 %d, 현재 잔여좌석 %d",
						totalPersonCount, prevTotal, diff, redisUtil.getAvailableSeats(eventId, scheduleId)));
			}

			log.info("인원수 변경 완료: eventId={}, scheduleId={}, diff={}, 현재 잔여좌석={}", eventId, scheduleId, diff, result);
		}

		// TTL 만료 시 복구용 METADATA 생성
		redisUtil.createMetaDataForSessionExpired(reservationToken, Integer.parseInt(eventId),
				Integer.parseInt(scheduleId), totalPersonCount);
	}

	// step2 : 좌석 선택
	@Override
	public void selectSeatType(String reservationToken, SeatTypeReqDto reqDto) {
		// 좌석 유효성 검증
		boolean allAvailable = seatService.checkAllSeatsAvailable(reqDto.getEventId(), reqDto.getScheduleId(),
				reqDto.getSeatNames());

		if (!allAvailable) {
			throw new IllegalArgumentException("다른 고객님께서 이미 선택한 좌석입니다.");
		}

		// 좌석 홀드
		boolean success = seatService.holdSeats(reqDto.getEventId(), reqDto.getScheduleId(), reqDto.getSeatNames());
		if (!success) {
			throw new IllegalArgumentException("다른 고객님께서 이미 선택한 좌석입니다.");
		}

		// 좌석 갱신
		String seatsCsv = String.join(",", reqDto.getSeatNames());

		// reservedCount 갱신
		int totalPersonCount = reqDto.getSeatNames().size(); // 좌석 수 = 예약 인원 수
		redisUtil.updateStep2SeatType(reservationToken, seatsCsv, reqDto.getTotalPrice(), totalPersonCount);

		// TTL 만료시 복구용 메타데이터 생성
		redisUtil.createSeatMetaDataForSessionExpired(reservationToken, reqDto.getEventId(), reqDto.getScheduleId(),
				seatsCsv);
	}

	// step3 : 주문자 정보 입력
	@Override
	public boolean inputUserInfo(String reservationToken, String userId, UserInfoReqDto userInfoReqDto) {
		if (!redisUtil.validateReservationSession(reservationToken)) {
			throw new IllegalStateException("예약 세션이 만료되었습니다. 다시 예약해주세요.");
		}

		String redisUserId = redisUtil.getDataField(reservationToken, "userId");
		if (redisUserId == null || !redisUserId.equals(userId)) {
			log.info("유저 불일치");
			return false;
		}

		redisUtil.updateStep3(reservationToken, userInfoReqDto.getUserName(), userInfoReqDto.getPhone(),
				userInfoReqDto.getEmail());

		return true;
	}

	// 좌석 확정 로직
	@Override
	public void confirmBooking(String reservationToken, SeatTypeReqDto reqDto) {
		if (!redisUtil.validateReservationSession(reservationToken)) {
			throw new IllegalStateException("예약 세션이 만료되었습니다. 다시 예약해주세요.");
		}

		boolean success = seatService.confirmSeats(reqDto.getEventId(), reqDto.getScheduleId(), reqDto.getSeatNames());

		if (!success) {
			throw new IllegalStateException("좌석 확정 처리 실패");
		}

		// Redis 예약 정보 업데이트
		redisUtil.updateStepStatus(reservationToken, "COMPLETED");
		redisUtil.updateReservationStatus(reservationToken, ReservationStatus.RESERVED);
		redisUtil.deleteMetaData(reservationToken);
	}

	// 좌석 취소 로직
	@Override
	public void cancelReservation(String reservationToken, boolean isPaymentStep, String reason) {
		if (!redisUtil.validateReservationSession(reservationToken)) {
			log.warn("취소 시도: 유효하지 않은 예약 토큰 [{}]", reservationToken);
			return;
		}

		Map<Object, Object> data = redisUtil.getDataAll(reservationToken);
		if (data.isEmpty()) {
			return;
		}

		String eventId = (String) data.get("eventId");
		String userId = (String) data.get("userId");
		String scheduleId = (String) data.get("scheduleId");
		String seatNamesStr = (String) data.get("seatNames");
		String reservationStatus = (String) data.get("reservationStatus");

		// ACTIVE SET에서 제거 (예매 중인 인원 set)
		removeFromActiveSet(eventId, userId);

		if (eventId == null || scheduleId == null) {
			log.warn("STEP1 취소 or eventId 또는 scheduleId 없음 [{}]", reservationToken);
			redisUtil.deleteDataAll(reservationToken);
			removeFromActiveSet(eventId, userId);
			return;
		}

		if (reservationStatus != null && reservationStatus.toString().equals(ReservationStatus.RESERVED.name())) {
			log.info("이미 결제 완료된 예약, 좌석 복구 스킵");
			redisUtil.deleteDataAll(reservationToken);
			removeFromActiveSet(eventId, userId);
			return;
		}

		// step4 에서 이전단계로 이동할 경우 결제세션만 삭제, step4 에서 브라우저를 닫을경우 결제세션 + 예약세션 함께 삭제
		if (isPaymentStep) {
			if (reason != null && reason.equals("from stage 4 to the previous step")) {
				redisUtil.deletePaymentData(reservationToken);
				removeFromActiveSet(eventId, userId);
				return;
			} else if (reason != null && reason.equals("popup close")) {
				redisUtil.deletePaymentData(reservationToken);
				removeFromActiveSet(eventId, userId);
			}
		}

		// 좌석 이름이 존재 -> SEAT_TYPE 처리
		if (seatNamesStr != null && !seatNamesStr.isEmpty()) {
			List<String> seatNames = Arrays.stream(seatNamesStr.split(",")).map(String::trim).filter(s -> !s.isEmpty())
					.collect(Collectors.toList());
			if (!seatNames.isEmpty()) {
				try {
					int evtId = Integer.parseInt(eventId);
					int schId = Integer.parseInt(scheduleId);
					seatService.releaseSeats(evtId, schId, seatNames);
					redisUtil.deleteDataAll(reservationToken); // 예약 세션 삭제
					log.info("예약 취소 시 좌석 초기화 완료");
				} catch (Exception e) {
					log.error("좌석 초기화 중 오류 발생", e);
				}
			}

		} else {
			// 좌석 정보가 없으면 PERSON_TYPE 처리
			int restored = redisUtil.rollbackOnCancel(reservationToken, eventId, scheduleId);
			log.info("예약 취소 완료: eventId={}, scheduleId={}, 복구좌석={}", eventId, scheduleId, restored);
		}
	}

	@Override
	public void validateReservation(String reservationToken) {
		if (!redisUtil.validateReservationSession(reservationToken)) {
			throw new IllegalStateException("예약 세션이 만료되었습니다. 다시 예약해주세요.");
		}
	}

	@Override
	public PaymentInfoResDto getPaymentInfo(String reservationToken) {
		if (!redisUtil.validateReservationSession(reservationToken)) {
			throw new IllegalStateException("예약 세션이 만료되었습니다. 다시 예약해주세요.");
		}

		Map<Object, Object> redisData = redisUtil.getDataAll(reservationToken);
		System.out.println("redisData : " + redisData);

		int eventId = Integer.parseInt(redisData.get("eventId").toString());
		int scheduleId = Integer.parseInt(redisData.get("scheduleId").toString());
		String eventName = (String) redisData.get("eventName");
		int totalPrice = Integer.parseInt(redisData.get("totalPrice").toString());
		int reservedCount = Integer.parseInt(redisData.get("reservedCount").toString());

		PaymentInfoResDto paymentInfoResDto = new PaymentInfoResDto();
		paymentInfoResDto.setEventId(eventId);
		paymentInfoResDto.setScheduleId(scheduleId);
		paymentInfoResDto.setTitle(eventName);
		paymentInfoResDto.setTotalPrice(totalPrice);
		paymentInfoResDto.setReservedCount(reservedCount);

		return paymentInfoResDto;
	}

	// =========================================================================================================

	// 결제 시점 예매 1건 + 티켓 N건 insert, 이벤트회차 잔여좌석 update
	@Transactional
	public void createReservation(String reservationToken, Long paymentId) {
		if (!redisUtil.validateReservationSession(reservationToken)) {
			throw new IllegalStateException("예약 세션이 만료되었습니다. 다시 예약해주세요.");
		}

		Map<Object, Object> reservationData = redisUtil.getDataAll(reservationToken);
		if (reservationData == null || reservationData.isEmpty()) {
			throw new IllegalArgumentException("예약 세션이 존재하지 않습니다.");
		}
		
		int eventId = Integer.parseInt(reservationData.get("eventId").toString());
		int scheduleId = Integer.parseInt(reservationData.get("scheduleId").toString());

		// 예매 1건 디비 저장
		Reservation reservation = new Reservation();
		reservation.setPaymentId(paymentId);
		reservation.setReservationStatus(ReservationStatus.RESERVED.code);
		reservation.setScheduleId(Integer.parseInt(reservationData.get("scheduleId").toString()));
		reservation.setUserId((String) reservationData.get("userId"));
		reservationMapper.insertReservation(reservation);

		redisUtil.updateReservationStatus(reservationToken, ReservationStatus.RESERVED);

		// 티켓 N건 디비 저장
		String seatNamesCsv = (String) reservationData.get("seatNames");
		String personCountsJson = (String) reservationData.get("personCounts");
		Long reservationId = reservation.getReservationId();

		if (personCountsJson != null && !personCountsJson.isEmpty()) {
			// 인원유형 티켓 저장
			var parsed = redisUtil.parsePersonCounts(personCountsJson)
					.orElseThrow(() -> new IllegalArgumentException("예약 인원 정보 파싱 오류"));

			for (Map.Entry<String, Integer> entry : parsed.entrySet()) {
				String personType = entry.getKey();
				int personTypeCount = entry.getValue();
				
				for (int i = 0; i < personTypeCount; i++) {
					Ticket ticket = new Ticket();
					ticket.setTicketStatus(TicketStatus.ACTIVE.code);
					ticket.setTicketType("PERSON_TYPE");
					ticket.setPersonType(PersonType.valueOf(personType).name());
					ticket.setSeatId(null);
					ticket.setReservationId(reservationId);
					ticket.setPaymentId(paymentId);
					ticketMapper.insertTicket(ticket);
				}
			}
		} else if (seatNamesCsv != null && !seatNamesCsv.isEmpty()) {
			// 좌석 유형 티켓 저장
			String[] seatNames = seatNamesCsv.split(",");
			
			for (String seatName : seatNames) {
				EventSeatDto seat = seatService.selectOneBySeatName(seatName, eventId, scheduleId);
				
				Ticket ticket = new Ticket();
				ticket.setTicketStatus(TicketStatus.ACTIVE.code);
				ticket.setTicketType("SEAT_TYPE");
				ticket.setPersonType(null);
				ticket.setSeatId(seat.getSeatId());
				ticket.setReservationId(reservationId);
				ticket.setPaymentId(paymentId);
				ticketMapper.insertTicket(ticket);
			}
		}

		// 이벤트 회차의 잔여좌석 차감
		int reservedCount = Integer.parseInt(reservationData.get("reservedCount").toString());
		int updateResult = eventPartMapper.updateRemainingSeatByReservation(scheduleId, eventId, reservedCount);

		if (updateResult == 0) {
			throw new IllegalStateException("잔여좌석 부족으로 업데이트 실패");
		}

		log.info("결제 후 DB 저장 완료");
	}

	// 예매 완료 버튼 누를 때 호출 - 레디스에 저장된 예약세션, 결제정보 삭제
	// 레디스 잔여좌석 유지 (복구X)
//	@Transactional
	public void completeReservation(String reservationToken, String userId) {

		redisUtil.deleteDataAll(reservationToken);

		log.info("예매 프로세스 완료");
	}

	@Override
	public List<EventPart> selectPartsByEventId(int eventId) {
		return reservationMapper.findPartsByEventId(eventId);
	}

	// Active Set에서 사용자 제거
	private void removeFromActiveSet(String eventId, String userId) {
		// Active Set에서 제거
		if (eventId != null && userId != null) {
			try {
				queueService.leaveActive(eventId, userId);
				log.info("Active Set에서 사용자 제거 완료: eventId={}, userId={}", eventId, userId);
			} catch (Exception e) {
				log.error("Active Set 제거 중 오류 발생: eventId={}, userId={}", eventId, userId, e);
			}
		}
	}
}

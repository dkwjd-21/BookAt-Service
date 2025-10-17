package com.bookat.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookat.dto.EventSeatDto;
import com.bookat.dto.EventSeatInfoDto;
import com.bookat.mapper.SeatMapper;
import com.bookat.service.SeatService;
import com.bookat.util.SeatRedisUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SeatServiceImpl implements SeatService {

	@Autowired
	private SeatRedisUtil seatRedisUtil;

	@Autowired
	private SeatMapper mapper;

	@Override
	// 회차별 좌석 리스트 조회 
	public List<EventSeatInfoDto> getSeatList(int eventId, int scheduleId) {
		String hashKey = "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":SEATS";

		// Redis에서 hashKey에 해당하는 값들 가져오기
		Map<Object, Object> seatMap = seatRedisUtil.getSeatMap(eventId, scheduleId);

		List<EventSeatInfoDto> seatList = new ArrayList<>();

		for (Map.Entry<Object, Object> entry : seatMap.entrySet()) {
			EventSeatInfoDto seat = new EventSeatInfoDto();
			seat.setSeatName((String) entry.getKey());
			seat.setStatus((String) entry.getValue());
			seatList.add(seat);
		}

		seatList.sort((s1, s2) -> {
		    String name1 = s1.getSeatName();
		    String name2 = s2.getSeatName();

		    // 첫 글자는 문자
		    char row1 = name1.charAt(0);
		    char row2 = name2.charAt(0);

		    if (row1 != row2) {
		        return row1 - row2; // 행 기준 정렬
		    }

		    // 숫자 부분 추출
		    int num1 = Integer.parseInt(name1.substring(1));
		    int num2 = Integer.parseInt(name2.substring(1));

		    return num1 - num2; // 좌석 번호 기준 정렬
		});
		return seatList;
	}

	@Override
	// 선택한 모든 좌석이 예약 가능한지 확인 
	public boolean checkAllSeatsAvailable(int eventId, int scheduleId, List<String> seatNames) {
		String availableSetKey = "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":AVAILABLE_SEATS";

		// Redis의 Set에서 각 좌석이 존재하는지 isMember로 확인한다.
		for (String seatName : seatNames) {
			Boolean isAvailable = seatRedisUtil.isSeatAvailable(eventId, scheduleId, seatName);
			log.info("좌석 {}에 대한 Redis 조회 결과: {}", seatName, isAvailable);
			// 하나라도 존재하지 않으면 false 반환
			if (isAvailable == null || !isAvailable) {
				return false;
			}
		}

		// 모든 좌석이 예약가능한 경우 true 반환
		return true;
	}

	@Override
	// 선택한 모든 좌석을 HOLD 처리
	public boolean holdSeats(int eventId, int scheduleId, List<String> seatNames) {
		log.info("좌석 {} 선점 시도. AVAILABLE_SET: {}, HOLD_SET: {}", seatNames);

		Long successCount = seatRedisUtil.holdSeats(eventId, scheduleId, seatNames);

		// 요청한 좌석 수와 성공한 좌석 수가 같아야 성공
		return successCount != null && successCount == seatNames.size();
	}

	@Override
	// 선택한 모든 좌석을 BOOKED 처리 
	public boolean confirmSeats(int eventId, int scheduleId, List<String> seatNames) {
		// Redis 처리 
		Long successCount = seatRedisUtil.bookSeats(eventId, scheduleId, seatNames);
		
		if(successCount == null || successCount != seatNames.size()) {
			log.warn("일부 좌석 BOOKED 처리 실패. 요청: {}, 성공: {}", seatNames.size(), successCount);
	        return false;
		}
		
		// DB 동기화 (예약 완료 상태 = 0)
		for(String seatName : seatNames) {
			EventSeatDto seat = selectOneBySeatName(seatName, eventId, scheduleId);
			if(seat != null && seat.getSeatStatus() != 0) {
				seat.setSeatStatus(0);
				updateSeatStatus(seat);
			}
		}
		
		log.info("좌석 BOOKED 처리 완료: eventId={}, scheduleId={}, seats={}", eventId, scheduleId, seatNames);
	    return true;
	}
	
	@Override
	// 선택한 모든 좌석을 HOLD/BOOKED 해제 처리 (예약 가능 상태로 되돌림)
	public boolean releaseSeats(int eventId, int scheduleId, List<String> seatNames) {
		Long releasedCount = seatRedisUtil.releaseSeats(eventId, scheduleId, seatNames);

	    if (releasedCount == null || !releasedCount.equals((long) seatNames.size())) {
	        log.warn("일부 좌석 해제 실패. 요청: {}, 성공: {}", seatNames.size(), releasedCount);
	        return false;
	    }

	    // DB 동기화 (예약 가능 상태로 맞추기)
	    for (String seatName : seatNames) {
	        EventSeatDto seat = selectOneBySeatName(seatName, eventId, scheduleId);
	        if (seat != null && seat.getSeatStatus() != 1) {
	            seat.setSeatStatus(1); // 예약 가능
	            updateSeatStatus(seat);
	        }
	    }
	    return true;
	}
	
	@Override
	public int insertSeat(EventSeatDto dto) {
		return mapper.insertSeat(dto);
	}

	@Override
	public EventSeatDto selectOneBySeatName(String seatName, int eventId, int scheduleId) {
		return mapper.selectOneBySeatName(seatName, eventId, scheduleId);
	}

	@Override
	public int updateSeatStatus(EventSeatDto dto) {
		return mapper.updateSeatStatus(dto);
	}
}

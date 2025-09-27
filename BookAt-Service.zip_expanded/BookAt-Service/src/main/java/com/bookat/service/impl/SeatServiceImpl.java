package com.bookat.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
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

		seatList.sort((s1, s2) -> s1.getSeatName().compareTo(s2.getSeatName()));
		return seatList;
	}

	@Override
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
	public boolean holdSeats(int eventId, int scheduleId, List<String> seatNames) {
		String hashKey = "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":SEATS";
		String availableSetKey = "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":AVAILABLE_SEATS";
		String holdSetKey = "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":HOLD_SEATS";

		log.info("좌석 {} 선점 시도. AVAILABLE_SET: {}, HOLD_SET: {}", seatNames, availableSetKey, holdSetKey);

		// Redis 파이프라인을 사용해 여러 명령어를 한 번에 실행
		List<Object> results = seatRedisUtil.holdSeats(eventId, scheduleId, seatNames);

		// 트랜잭션 결과 검증
		for (int i = 0; i < results.size(); i += 2) {
			// SMOVE 결과만 확인
			Object moveResultObject = results.get(i);
			Long moveResult = null;

			// SMOVE 결과는 Redis 버전에 따라 Long 또는 Boolean으로 반환될 수 있음
			if (moveResultObject instanceof Long) {
				moveResult = (Long) moveResultObject;
			} else if (moveResultObject instanceof Boolean) {
				moveResult = ((Boolean) moveResultObject) ? 1L : 0L;
			}

			if (moveResult == null || moveResult == 0L) {
				// 좌석 선점에 실패
				return false;
			}
		}

		// 모든 좌석 선점 명령이 성공적으로 실행되었으면 true 반환
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

	@Override
	public boolean releaseSeats(int eventId, int scheduleId, List<String> seatNames) {
		boolean allReleased = true;

		String hashKey = "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":SEATS";
		String availableSetKey = "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":AVAILABLE_SEATS";
		String holdSetKey = "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":HOLD_SEATS";
		String bookedSetKey = "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":BOOKED_SEATS";

		for (String seatName : seatNames) {
			try {
				// Redis에서 좌석 해제
				seatRedisUtil.releaseSeat(eventId, scheduleId, seatName);

				// DB 상태 확인 -> 예약 가능 상태가 아니면 1로 되돌림
				EventSeatDto seat = selectOneBySeatName(seatName, eventId, scheduleId);
				if (seat != null && seat.getSeatStatus() != 1) {
					seat.setSeatStatus(1); // 예약 가능
					updateSeatStatus(seat);
				}

			} catch (Exception e) {
				log.warn("좌석 릴리즈 실패 : {}", seatName, e);
				allReleased = false;
			}
		}
		return allReleased;
	}

}

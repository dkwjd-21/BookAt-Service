package com.bookat.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.bookat.dto.EventSeatInfoDto;
import com.bookat.service.SeatService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SeatServiceImpl implements SeatService {

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Override
	public List<EventSeatInfoDto> getSeatList(int eventId, int scheduleId) {
		String hashKey = "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":SEATS";

		// Redis에서 hashKey에 해당하는 값들 가져오기
		Map<Object, Object> seatMap = redisTemplate.opsForHash().entries(hashKey);

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
			Boolean isAvailable = redisTemplate.opsForSet().isMember(availableSetKey, seatName);
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
		List<Object> results = redisTemplate.executePipelined((RedisCallback<Void>) connection -> {
			for (String seatName : seatNames) {
				// SMOVE 명령어 실행: AVAILABLE Set에서 HOLD Set으로 좌석을 이동
				connection.setCommands().sMove(availableSetKey.getBytes(), holdSetKey.getBytes(), seatName.getBytes());
				// HSET 명령어 실행: Hash의 좌석 상태를 "HOLD"로 변경
				connection.hashCommands().hSet(hashKey.getBytes(), seatName.getBytes(), "HOLD".getBytes());
			}
			// 여기서 return null은 명령어를 모두 작성했음을 의미함
			return null;
		});

		// 트랜잭션 결과 검증
		for(int i=0; i<results.size(); i+= 2) {
			// SMOVE 결과만 확인 
			Object moveResultObject = results.get(i);
			Long moveResult = null;
			
			// SMOVE 결과는 Redis 버전에 따라 Long 또는 Boolean으로 반환될 수 있음
			if(moveResultObject instanceof Long) {
				moveResult = (Long) moveResultObject;
			} else if(moveResultObject instanceof Boolean) {
				moveResult = ((Boolean) moveResultObject)? 1L : 0L;
			}
			
			if(moveResult == null || moveResult == 0L) {
				// 좌석 선점에 실패 
				return false;
			}
		}
		
		// 모든 좌석 선점 명령이 성공적으로 실행되었으면 true 반환 
		return true;
	}

}

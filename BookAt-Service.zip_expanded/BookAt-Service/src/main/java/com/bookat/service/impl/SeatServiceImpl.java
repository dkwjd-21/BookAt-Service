package com.bookat.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.bookat.dto.EventSeatInfoDto;
import com.bookat.service.SeatService;

@Service
public class SeatServiceImpl implements SeatService{

	@Autowired
	private StringRedisTemplate redisTemplate;
	
	@Override
	public List<EventSeatInfoDto> getSeatList(int eventId, int scheduleId) {
		String hashKey = "EVENT:"+eventId+":SCHEDULE:"+scheduleId+":SEATS";
		
		// Redis에서 hashKey에 해당하는 값들 가져오기
		Map<Object, Object> seatMap = redisTemplate.opsForHash().entries(hashKey);
		
		List<EventSeatInfoDto> seatList = new ArrayList<>();
		
		for(Map.Entry<Object, Object> entry : seatMap.entrySet()) {
			EventSeatInfoDto seat = new EventSeatInfoDto();
			seat.setSeatName((String) entry.getKey());
			seat.setStatus((String) entry.getValue());
			seatList.add(seat);
		}
		
		seatList.sort((s1, s2) -> s1.getSeatName().compareTo(s2.getSeatName()));
		return seatList;
	}

}

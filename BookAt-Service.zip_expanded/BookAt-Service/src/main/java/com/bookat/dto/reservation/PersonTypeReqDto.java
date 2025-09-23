package com.bookat.dto.reservation;

import java.util.Map;

import com.bookat.enums.PersonType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonTypeReqDto {
	
	// step2 : 등급 인원별 선택
	
	private Map<PersonType, Integer> personCounts;
	private int totalPrice;

}

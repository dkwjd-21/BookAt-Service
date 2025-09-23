package com.bookat.enums;

public enum PersonType {

    ADULT("성인"),
    YOUTH("청소년"),
    CHILD("유아");

	private final String label;
	PersonType(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	}
	
}

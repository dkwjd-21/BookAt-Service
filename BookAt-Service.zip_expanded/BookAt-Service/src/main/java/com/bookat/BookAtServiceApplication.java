package com.bookat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.bookat.mapper")
public class BookAtServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookAtServiceApplication.class, args);
	}

}

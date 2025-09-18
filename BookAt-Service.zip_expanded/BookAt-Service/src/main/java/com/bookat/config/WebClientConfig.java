package com.bookat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
	  @Bean
	  public WebClient payClient(
	      @Value("${pay.portone.api.base-url}") String baseUrl) { // ✅ 키 이름 통일
	    return WebClient.builder()
	        .baseUrl(baseUrl) 
	        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
	        .build();
	  }

}

package com.bookat.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component                           
@RequiredArgsConstructor
public class PortOneClient {

    private final WebClient webClient; 

    
    @Value("${pay.portone.api.key}")
    private String apiKey;

    @Value("${pay.portone.api.secret}")
    private String apiSecret;

    public Mono<String> getAccessToken() {
        return webClient.post()
                .uri("/users/getToken")                     
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("imp_key", apiKey, "imp_secret", apiSecret))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(m -> (String) ((Map<?, ?>) m.get("response")).get("access_token"))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(300)));
    }

    public Mono<Map<String, Object>> getPaymentByImpUid(String accessToken, String impUid) {
        return webClient.get()
                .uri("/payments/{impUid}", impUid)         
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
    
    public Mono<Map<String, Object>> cancelPayment(String accessToken, String impUid, Integer amount, String reason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("imp_uid", impUid);
        if (amount != null && amount > 0) body.put("amount", amount);
        if (reason != null && !reason.isBlank()) body.put("reason", reason);

        return webClient
            .post()
            .uri("/payments/cancel")
            .header("Authorization", accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .retryWhen(Retry.backoff(2, Duration.ofMillis(300)));
      }
}

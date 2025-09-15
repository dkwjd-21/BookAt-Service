package com.bookat._debug;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j                 // ✅ Lombok이 log 필드 생성
@Component             // ✅ 컴포넌트 스캔 대상
public class EnvCheck {

  private final Environment env;

  public EnvCheck(Environment env) { this.env = env; }

  @EventListener(ContextRefreshedEvent.class)
  public void check() {
    String key = env.getProperty("iamport.api.key");
    String sec = env.getProperty("iamport.api.secret");
    log.info("iamport.api.key present? {}", key != null);
    log.info("iamport.api.secret present? {}", sec != null);
    if (key != null) {
      log.info("iamport.api.key head: {}***", key.substring(0, Math.min(6, key.length())));
    }
  }
}
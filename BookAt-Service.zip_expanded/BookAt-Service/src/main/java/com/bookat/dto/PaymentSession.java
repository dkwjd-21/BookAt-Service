package com.bookat.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public record PaymentSession(
    String bookId,
    int qty,
    String method,
    BigDecimal amount,
    String merchantUid,
    String userId,
    String status,
    String createdAt,
    String title

) {}
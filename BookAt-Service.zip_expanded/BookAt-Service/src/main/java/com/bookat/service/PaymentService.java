package com.bookat.service;

import com.bookat.dto.PaymentRequest;
import com.bookat.dto.PaymentResponse;

public interface PaymentService {
  PaymentResponse.CheckoutView prepareCheckout(Long orderId, String userId);
  PaymentResponse.Result complete(PaymentRequest.Complete req, String userId);
  PaymentResponse.Result cancel(PaymentRequest.Cancel req, String userId);
  void handleWebhook(String impUid);
}
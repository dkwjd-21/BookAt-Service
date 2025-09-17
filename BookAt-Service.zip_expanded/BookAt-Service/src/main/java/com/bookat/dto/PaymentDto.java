package com.bookat.dto;

import java.util.Date;

public class PaymentDto {

    private Long paymentId;
    private Long orderId;
    private String userId;

	private Integer totalPrice;
    private Integer paymentPrice;
    private String paymentMethod;  // CARD/VIRTUAL/POINT
    private Integer paymentStatus; // 0 READY, 1 PAID, -1 FAILED, 2 CANCEL_REQUESTED, 3 CANCELED
    private Date paymentDate;      // DEFAULT SYSDATE
    private String paymentInfo;
    
    private String merchantUid;    // 우리 결제번호 UNIQUE
    private String impUid;         // 포트원 결제번호
    private String pgTid;          // PG TID
    private String receiptUrl;     // 영수증 URL
    private String failReason;     // 실패 사유
    
    public PaymentDto() {}
    public PaymentDto(Long paymentId, Long orderId, String userId, Integer totalPrice, Integer paymentPrice, String paymentMethod,
			Integer paymentStatus, Date paymentDate, String paymentInfo, String merchantUid, String impUid,
			String pgTid, String receiptUrl, String failReason) {
		super();
		this.paymentId = paymentId;
		this.orderId = orderId;
		this.userId = userId;
		this.totalPrice = totalPrice;
		this.paymentPrice = paymentPrice;
		this.paymentMethod = paymentMethod;
		this.paymentStatus = paymentStatus;
		this.paymentDate = paymentDate;
		this.paymentInfo = paymentInfo;
		this.merchantUid = merchantUid;
		this.impUid = impUid;
		this.pgTid = pgTid;
		this.receiptUrl = receiptUrl;
		this.failReason = failReason;
	}
	public Long getPaymentId() {
		return paymentId;
	}
	public void setPaymentId(Long paymentId) {
		this.paymentId = paymentId;
	}
	public Long getOrderId() {
		return orderId;
	}
	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public Integer getTotalPrice() {
		return totalPrice;
	}
	public void setTotalPrice(Integer totalPrice) {
		this.totalPrice = totalPrice;
	}
	public Integer getPaymentPrice() {
		return paymentPrice;
	}
	public void setPaymentPrice(Integer paymentPrice) {
		this.paymentPrice = paymentPrice;
	}
	public String getPaymentMethod() {
		return paymentMethod;
	}
	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}
	public Integer getPaymentStatus() {
		return paymentStatus;
	}
	public void setPaymentStatus(Integer paymentStatus) {
		this.paymentStatus = paymentStatus;
	}
	public Date getPaymentDate() {
		return paymentDate;
	}
	public void setPaymentDate(Date paymentDate) {
		this.paymentDate = paymentDate;
	}
	public String getPaymentInfo() {
		return paymentInfo;
	}
	public void setPaymentInfo(String paymentInfo) {
		this.paymentInfo = paymentInfo;
	}
	public String getMerchantUid() {
		return merchantUid;
	}
	public void setMerchantUid(String merchantUid) {
		this.merchantUid = merchantUid;
	}
	public String getImpUid() {
		return impUid;
	}
	public void setImpUid(String impUid) {
		this.impUid = impUid;
	}
	public String getPgTid() {
		return pgTid;
	}
	public void setPgTid(String pgTid) {
		this.pgTid = pgTid;
	}
	public String getReceiptUrl() {
		return receiptUrl;
	}
	public void setReceiptUrl(String receiptUrl) {
		this.receiptUrl = receiptUrl;
	}
	public String getFailReason() {
		return failReason;
	}
	public void setFailReason(String failReason) {
		this.failReason = failReason;
	}

    
}

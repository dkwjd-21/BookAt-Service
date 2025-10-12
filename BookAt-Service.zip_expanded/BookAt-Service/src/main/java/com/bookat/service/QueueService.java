package com.bookat.service;

public interface QueueService {
	public Long addUserToQueue(String eventId, String userId);
	public Long addUserToQueue(String eventId, String userId, String reservationId);
	public Long getUserRank(String eventId, String userId);
	public boolean leaveQueue(String eventId, String userId);
	public boolean tryEnterActive(String eventId, String userId);
	public void leaveActive(String eventId, String userId);
	public Long getActiveCount(String eventId);
	public boolean canEnterReservation(String eventId, String userId);
	public Long getQueueCount(String eventId);
	public void updateLastActive(String eventId, String userId);
}

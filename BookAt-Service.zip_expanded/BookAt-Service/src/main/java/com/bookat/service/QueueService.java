package com.bookat.service;

public interface QueueService {
	public Long addUserToQueue(String eventId, String userId);
	public Long getUserRank(String eventId, String userId);
	public boolean leaveQueue(String eventId, String userId);
}

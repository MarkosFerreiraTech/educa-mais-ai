package br.com.educamais.api.dto;

public record AttendanceWebhookResponse(String eventId, String status, String attendanceId) {
}

package br.com.educamais.api.dto;

public record AnswerResponse(String classification, String feedback, MissionResponse mission) {
}

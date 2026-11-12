package br.com.educamais.domain.port;

import java.time.Instant;
import java.time.LocalDate;

/** Relogio injetavel para testes deterministicos (DIP). */
public interface ClockPort {

    Instant now();

    LocalDate today();
}

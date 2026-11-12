package br.com.educamais.service;

import br.com.educamais.agent.planner.PromptFactory;
import br.com.educamais.domain.enums.QuestionType;
import br.com.educamais.domain.model.DiagnosticQuestion;
import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.MissionStep;
import br.com.educamais.domain.port.DiagnosticRepository;
import br.com.educamais.domain.port.LlmPort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Factory de exercicios diagnosticos (RF16): reaproveita a questao do passo ou gera via LLM.
 */
@Service
public class DiagnosticQuestionFactory {

    private final DiagnosticRepository diagnosticRepository;
    private final LlmPort llmPort;
    private final PromptFactory promptFactory;

    public DiagnosticQuestionFactory(DiagnosticRepository diagnosticRepository, LlmPort llmPort,
                                     PromptFactory promptFactory) {
        this.diagnosticRepository = diagnosticRepository;
        this.llmPort = llmPort;
        this.promptFactory = promptFactory;
    }

    public DiagnosticQuestion forStep(Mission mission, MissionStep step) {
        return diagnosticRepository.findQuestionsByMissionId(mission.id()).stream()
                .filter(question -> question.stepId().equals(step.id()))
                .findFirst()
                .orElseGet(() -> generate(mission, step));
    }

    private DiagnosticQuestion generate(Mission mission, MissionStep step) {
        int difficulty = Math.min(Math.max(step.attempts() + 1, 1), 5);
        String interestHint = "futebol";
        String prompt = promptFactory.questionPrompt(step.objective(), step.explanation(), difficulty, interestHint);
        Optional<List<LlmPort.LlmQuestion>> generated = llmPort.generateQuestions(
                new LlmPort.QuestionPrompt(prompt, difficulty));

        DiagnosticQuestion question = generated.flatMap(list -> list.stream().findFirst())
                .map(llm -> new DiagnosticQuestion(null, mission.id(), step.id(), llm.prompt(),
                        parseType(llm.type()), llm.options(), llm.correctAnswer(), llm.explanation(),
                        llm.difficulty()))
                .orElseGet(() -> fallbackQuestion(mission, step, difficulty));
        return diagnosticRepository.saveQuestion(question);
    }

    private DiagnosticQuestion fallbackQuestion(Mission mission, MissionStep step, int difficulty) {
        String correct = "1/2";
        return new DiagnosticQuestion(null, mission.id(), step.id(),
                "No exemplo de " + step.objective() + ": se o time aproveitou 2 de 4 chutes, qual fracao "
                        + "representa esse aproveitamento?",
                QuestionType.MULTIPLE_CHOICE, List.of("1/4", "1/2", "2/3", "3/4"), correct,
                "2 de 4 chutes equivalem a 2/4, que simplificado resulta em 1/2.", difficulty);
    }

    private static QuestionType parseType(String type) {
        try {
            return type == null ? QuestionType.MULTIPLE_CHOICE : QuestionType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return QuestionType.MULTIPLE_CHOICE;
        }
    }
}

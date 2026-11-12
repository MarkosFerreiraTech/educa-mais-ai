package br.com.educamais.service;

import br.com.educamais.agent.planner.PromptFactory;
import br.com.educamais.agent.strategy.AdaptiveStrategySelector;
import br.com.educamais.agent.strategy.PedagogicalStrategy;
import br.com.educamais.agent.strategy.PedagogicalStrategyCatalog;
import br.com.educamais.domain.enums.AnswerClassification;
import br.com.educamais.domain.enums.PedagogicalStrategyType;
import br.com.educamais.domain.model.DiagnosticAttempt;
import br.com.educamais.domain.model.DiagnosticQuestion;
import br.com.educamais.domain.model.Mission;
import br.com.educamais.domain.model.MissionStep;
import br.com.educamais.domain.model.Student;
import br.com.educamais.domain.port.DiagnosticRepository;
import br.com.educamais.domain.port.LlmPort;
import br.com.educamais.domain.port.LlmPort.ExplanationSuggestion;
import br.com.educamais.domain.port.StudentRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Avaliacao diagnostica e adaptacao pedagogica (RF16-RF19).
 * O aluno nunca fica sem resposta: se o LLM falhar, entra o template da estrategia.
 */
@Service
public class AssessmentService {

    private final DiagnosticRepository diagnosticRepository;
    private final StudentRepository studentRepository;
    private final DiagnosticQuestionFactory questionFactory;
    private final AdaptiveStrategySelector selector;
    private final PedagogicalStrategyCatalog catalog;
    private final LlmPort llmPort;
    private final PromptFactory promptFactory;
    private final AttendanceMetrics metrics;

    public AssessmentService(DiagnosticRepository diagnosticRepository, StudentRepository studentRepository,
                             DiagnosticQuestionFactory questionFactory, AdaptiveStrategySelector selector,
                             PedagogicalStrategyCatalog catalog, LlmPort llmPort,
                             PromptFactory promptFactory, AttendanceMetrics metrics) {
        this.diagnosticRepository = diagnosticRepository;
        this.studentRepository = studentRepository;
        this.questionFactory = questionFactory;
        this.selector = selector;
        this.catalog = catalog;
        this.llmPort = llmPort;
        this.promptFactory = promptFactory;
        this.metrics = metrics;
    }

    @Transactional
    public DiagnosticQuestion prepareQuestion(Mission mission) {
        MissionStep step = mission.currentStep().orElseThrow();
        return questionFactory.forStep(mission, step);
    }

    /** RF17: avalia a resposta, persiste a tentativa e classifica o desempenho. */
    @Transactional
    public Evaluation evaluate(UUID questionId, UUID studentId, String answer, String interestHint) {
        DiagnosticQuestion question = diagnosticRepository.findQuestionById(questionId)
                .orElseThrow(() -> new br.com.educamais.domain.exception.ResourceNotFoundException(
                        "Exercicio", questionId));
        boolean correct = question.matches(answer);
        AnswerClassification classification = correct ? AnswerClassification.CORRECT
                : AnswerClassification.INCORRECT;
        String feedback = correct
                ? "Isso mesmo! " + safeExplanation(question)
                : "Quase la. " + safeExplanation(question) + " Vamos tentar por outro caminho.";
        DiagnosticAttempt attempt = diagnosticRepository.saveAttempt(
                DiagnosticAttempt.of(questionId, studentId, answer, classification, feedback));
        return new Evaluation(attempt, classification, feedback);
    }

    /**
     * RF18: aplica a proxima estrategia pedagogica no passo, gerando nova explicacao.
     */
    @Transactional
    public Adaptation adapt(Mission mission, Student student, String studentAnswer) {
        MissionStep step = mission.currentStep().orElseThrow();
        PedagogicalStrategyType nextType = selector.nextStrategy(step);
        PedagogicalStrategy strategy = catalog.get(nextType);
        String interestHint = student.primaryInterest()
                .map(code -> code.contextHint())
                .orElse("cotidiano");

        String prompt = promptFactory.reExplainPrompt(step.objective(), step.explanation(), nextType,
                strategy.instruction(), studentAnswer, interestHint);
        Optional<ExplanationSuggestion> suggestion = llmPort.reExplain(
                new LlmPort.ExplanationPrompt(prompt, nextType));
        ExplanationSuggestion resolved = suggestion.orElseGet(() ->
                new ExplanationSuggestion(strategy.templateExplanation(step.objective()),
                        strategy.templateExample(step.objective(), interestHint)));

        mission.adaptCurrentStep();
        mission.applyExplanation(resolved.explanation(), resolved.example());
        boolean reinforcement = selector.isReinforcement(nextType);
        return new Adaptation(nextType, resolved.explanation(), resolved.example(), reinforcement,
                suggestion.isPresent());
    }

    private static String safeExplanation(DiagnosticQuestion question) {
        return question.explanation() == null ? "" : question.explanation();
    }

    /** Resultado da avaliacao de uma resposta. */
    public record Evaluation(DiagnosticAttempt attempt, AnswerClassification classification, String feedback) {
    }

    /** Resultado da adaptacao pedagogica aplicada ao passo. */
    public record Adaptation(PedagogicalStrategyType strategy, String explanation, String example,
                             boolean reinforcement, boolean generatedByLlm) {
    }
}

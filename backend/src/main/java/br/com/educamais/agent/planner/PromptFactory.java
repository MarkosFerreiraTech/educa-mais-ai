package br.com.educamais.agent.planner;

import br.com.educamais.domain.enums.PedagogicalStrategyType;
import br.com.educamais.domain.model.MissionPlan;
import br.com.educamais.domain.model.Syllabus;
import org.springframework.stereotype.Component;

/** Constroi os prompts do agente (RF14, RF15) com grounding explicito na ementa. */
@Component
public class PromptFactory {

    private static final String SYSTEM_ROLE = """
            Voce e o tutor autonomo do Educa Mais, especializado em recuperacao de aprendizagem
            para estudantes brasileiros de 12 a 18 anos. Regras obrigatorias:
            1. Responda sempre em portugues do Brasil, com linguagem simples e acolhedora.
            2. NUNCA saia dos objetivos de aprendizagem informados na ementa.
            3. Personalize explicacoes e exemplos usando o interesse do aluno.
            4. Nao use marcacao Markdown, apenas texto corrido.
            5. Respeite o limite de passos solicitado.
            """;

    public String planPrompt(RecoveryPlanRequest request) {
        Syllabus syllabus = request.lesson().syllabus();
        return """
                %s
                Aluno: %s (primeiro nome)
                Interesse principal: %s
                Disciplina: %s
                Topico da aula perdida: %s
                Objetivos de aprendizagem (ementa): %s
                Topicos da ementa: %s
                Pre-requisitos: %s

                Gere um plano de recuperacao entre %d e %d passos. Cada passo deve conter:
                - objective: reaproveitando um objetivo da ementa;
                - explanation: explicacao curta (max. 60 palavras) com exemplo de %s;
                - example: um exemplo concreto e numerico.
                """.formatted(SYSTEM_ROLE, request.firstName(), request.interestHint(),
                request.lesson().subject(), request.lesson().topic(), syllabus.objectivesAsText(),
                syllabus.topicsAsText(), String.join(", ", syllabus.prerequisites()),
                MissionPlan.MIN_STEPS, MissionPlan.MAX_STEPS,
                request.interestHint());
    }

    public String reExplainPrompt(String objective, String previousExplanation, PedagogicalStrategyType strategy,
                                  String strategyInstruction, String studentAnswer, String interestHint) {
        return """
                %s
                O aluno nao entendeu a explicacao anterior sobre: %s
                Resposta dada pelo aluno: %s
                Explicacao anterior (NAO repetir): %s
                Nova abordagem obrigatoria: %s - %s
                Interesse do aluno: %s
                Gere uma nova explicacao (max. 70 palavras) e um exemplo diferente.
                """.formatted(SYSTEM_ROLE, objective, studentAnswer, previousExplanation, strategy.name(),
                strategyInstruction, interestHint);
    }

    public String questionPrompt(String objective, String explanation, int difficulty, String interestHint) {
        return """
                %s
                Objetivo: %s
                Explicacao dada ao aluno: %s
                Nivel de dificuldade: %d de 5
                Interesse do aluno: %s
                Crie UMA pergunta de multipla escolha com 4 alternativas, exatamente uma correta,
                gabarito explicito e justificativa pedagogica.
                """.formatted(SYSTEM_ROLE, objective, explanation, difficulty, interestHint);
    }

    public String recommendationPrompt(String studentFirstName, String topic, int successRate, int attempts,
                                       java.util.List<String> gaps) {
        return """
                %s
                Aluno: %s | Topico: %s | Acerto: %d%% | Tentativas totais: %d
                Lacunas identificadas: %s
                Escreva uma recomendacao pedagogica objetiva (max. 80 palavras) para o professor,
                indicando a intervencao sugerida em sala.
                """.formatted(SYSTEM_ROLE, studentFirstName, topic, successRate, attempts,
                String.join("; ", gaps));
    }
}

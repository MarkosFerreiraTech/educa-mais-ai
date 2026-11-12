package br.com.educamais.infrastructure.llm;

import br.com.educamais.config.JsonMapperConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Monta o payload do Gemini com Structured Outputs (responseMimeType + responseSchema)
 * e as declaracoes de ferramentas expostas ao modelo (Tool Calling) - RF12, RF21.
 */
@Component
public class GeminiRequestFactory {

    private final ObjectMapper objectMapper;

    public GeminiRequestFactory(@Qualifier(JsonMapperConfig.ADAPTER_OBJECT_MAPPER)
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectNode planRequest(String prompt, int maxSteps) {
        ObjectNode payload = baseRequest(prompt);
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "OBJECT");
        ObjectNode properties = schema.putObject("properties");
        ObjectNode steps = properties.putObject("steps");
        steps.put("type", "ARRAY");
        ObjectNode items = steps.putObject("items");
        items.put("type", "OBJECT");
        ObjectNode itemProperties = items.putObject("properties");
        itemProperties.putObject("objective").put("type", "STRING");
        itemProperties.putObject("explanation").put("type", "STRING");
        itemProperties.putObject("example").put("type", "STRING");
        ArrayNode required = items.putArray("required");
        required.add("objective").add("explanation").add("example");
        schema.putArray("required").add("steps");
        payload.putObject("generationConfig")
                .put("responseMimeType", "application/json")
                .set("responseSchema", schema);
        addTools(payload, maxSteps);
        return payload;
    }

    public ObjectNode explanationRequest(String prompt) {
        return structured(prompt, "explanation", "example");
    }

    public ObjectNode recommendationRequest(String prompt) {
        return structured(prompt, "recommendation");
    }

    public ObjectNode questionRequest(String prompt) {
        ObjectNode payload = baseRequest(prompt);
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "OBJECT");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("prompt").put("type", "STRING");
        properties.putObject("type").put("type", "STRING");
        ObjectNode options = properties.putObject("options");
        options.put("type", "ARRAY");
        options.putObject("items").put("type", "STRING");
        properties.putObject("correctAnswer").put("type", "STRING");
        properties.putObject("explanation").put("type", "STRING");
        ArrayNode required = schema.putArray("required");
        required.add("prompt").add("type").add("options").add("correctAnswer");
        payload.putObject("generationConfig")
                .put("responseMimeType", "application/json")
                .set("responseSchema", schema);
        return payload;
    }

    private ObjectNode structured(String prompt, String... fields) {
        ObjectNode payload = baseRequest(prompt);
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "OBJECT");
        ObjectNode properties = schema.putObject("properties");
        ArrayNode required = schema.putArray("required");
        for (String field : fields) {
            properties.putObject(field).put("type", "STRING");
            required.add(field);
        }
        payload.putObject("generationConfig")
                .put("responseMimeType", "application/json")
                .set("responseSchema", schema);
        return payload;
    }

    private ObjectNode baseRequest(String prompt) {
        ObjectNode payload = objectMapper.createObjectNode();
        ObjectNode content = payload.putArray("contents").addObject();
        content.put("role", "user");
        content.putArray("parts").addObject().put("text", prompt);
        return payload;
    }

    /** Ferramentas internas expostas ao modelo (Tool Calling); o backend executa a consulta real. */
    private void addTools(ObjectNode payload, int maxSteps) {
        ArrayNode functionDeclarations = payload.putArray("tools").addObject()
                .putArray("functionDeclarations");

        ObjectNode syllabus = functionDeclarations.addObject();
        syllabus.put("name", "consultarEmenta");
        syllabus.put("description", "Consulta os objetivos de aprendizagem da aula perdida");
        syllabus.putObject("parameters").put("type", "OBJECT")
                .putObject("properties").putObject("lessonId").put("type", "STRING");

        ObjectNode student = functionDeclarations.addObject();
        student.put("name", "consultarPerfilAluno");
        student.put("description", "Consulta interesses e historico de desempenho do aluno");
        student.putObject("parameters").put("type", "OBJECT")
                .putObject("properties").putObject("studentId").put("type", "STRING");

        ObjectNode limit = functionDeclarations.addObject();
        limit.put("name", "limiteDePassos");
        limit.put("description", "Limite maximo de passos permitidos no plano: " + maxSteps);
        limit.putObject("parameters").put("type", "OBJECT");
    }
}

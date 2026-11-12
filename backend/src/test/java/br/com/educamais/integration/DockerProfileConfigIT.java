package br.com.educamais.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * Guarda de regressao do ambiente Docker (perfil {@code demo}).
 *
 * <p>Historia: o perfil {@code demo} fazia {@code spring.profiles.include: dev} e herdava do
 * {@code dev} o {@code driver-class-name: org.h2.Driver}. Como o Docker Compose injeta uma URL
 * PostgreSQL, a API subia com a URL do Postgres e o driver do H2 e morria com
 * {@code Failed to determine a suitable driver class}, antes de responder ao healthcheck:
 * {@code docker compose up} terminava em {@code container educa-mais-api exited (1)}.</p>
 *
 * <p>O runner do CI nao tem Docker, entao a verificacao aqui e sobre a <strong>configuracao
 * resolvida</strong> e sobre o <strong>comportamento real do driver JDBC</strong> aceitar a URL —
 * exatamente a checagem que o Hikari faz ao subir.</p>
 */
@DisplayName("Perfil demo: configuracao coerente com o Postgres do Docker Compose")
class DockerProfileConfigIT {

    private static final String DEMO = "demo";
    private static final String DEV = "dev";
    private static final String POSTGRES_URL = "jdbc:postgresql://postgres:5432/educamais";

    @Test
    @DisplayName("o perfil demo nao inclui o dev (que fixa o driver do H2)")
    void demoNaoIncluiPerfilDev() {
        String includes = loadYaml("application-" + DEMO + ".yml")
                .getProperty("spring.profiles.include", "");

        assertThat(splitProfiles(includes))
                .as("incluir o dev trazia driver-class-name: org.h2.Driver para dentro do demo")
                .doesNotContain(DEV);
    }

    @Test
    @DisplayName("demo declara explicitamente o driver do Postgres")
    void demoDeclaraDriverDoPostgres() {
        Properties demo = loadYaml("application-" + DEMO + ".yml");

        assertThat(resolve(demo.getProperty("spring.datasource.driver-class-name", "")))
                .isEqualTo("org.postgresql.Driver");
        assertThat(resolve(demo.getProperty("spring.datasource.url", "")))
                .as("a URL padrao do demo precisa ser PostgreSQL")
                .startsWith("jdbc:postgresql://");
    }

    @Test
    @DisplayName("o driver configurado existe no classpath e aceita a URL (mesma checagem do Hikari)")
    void driverConfiguradoAceitaUrl() throws Exception {
        String driverClass = resolve(loadYaml("application-" + DEMO + ".yml")
                .getProperty("spring.datasource.driver-class-name", ""));

        Class<?> type = Class.forName(driverClass);
        Driver driver = (Driver) type.getDeclaredConstructor().newInstance();

        assertThat(driver.acceptsURL(POSTGRES_URL))
                .as("era exatamente aqui que o startup falhava: o driver H2 nao aceita jdbc:postgresql")
                .isTrue();
        assertThat(DriverManager.getDriver(POSTGRES_URL))
                .as("nenhum driver do classpath aceita a URL do Docker Compose")
                .isNotNull();
    }

    @Test
    @DisplayName("demo usa as migrations do PostgreSQL, nunca as do H2")
    void demoUsaMigrationsDoPostgres() {
        Properties demo = loadYaml("application-" + DEMO + ".yml");

        assertThat(resolve(demo.getProperty("spring.flyway.locations", "")))
                .isEqualTo("classpath:db/migration")
                .doesNotContain("h2");
        assertThat(demo.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    }

    @Test
    @DisplayName("o perfil dev continua sendo H2 (uso local sem Docker)")
    void devContinuaH2() {
        Properties dev = loadYaml("application-" + DEV + ".yml");

        assertThat(dev.getProperty("spring.datasource.driver-class-name")).isEqualTo("org.h2.Driver");
        assertThat(dev.getProperty("spring.datasource.url")).contains("jdbc:h2:mem:");
        assertThat(dev.getProperty("spring.flyway.locations")).contains("db/migration/h2");
    }

    // ------------------------------------------------------------------ helpers

    private static List<String> splitProfiles(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(profile -> !profile.isEmpty())
                .toList();
    }

    /** Resolve {@code ${VAR:default}} usando o ambiente do processo (como o Spring faria). */
    private static String resolve(String value) {
        Matcher matcher = Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?}").matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String fromEnvironment = System.getenv(matcher.group(1));
            String fallback = matcher.group(2) == null ? "" : matcher.group(2);
            matcher.appendReplacement(result,
                    Matcher.quoteReplacement(fromEnvironment != null ? fromEnvironment : fallback));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /** Le um YAML de {@code src/main/resources} como propriedades achatadas (chave pontuada). */
    private static Properties loadYaml(String resource) {
        Properties properties = new Properties();
        ClassPathResource classPath = new ClassPathResource(resource);
        assertThat(classPath.exists()).as("recurso ausente no classpath: " + resource).isTrue();
        try (InputStream input = classPath.getInputStream()) {
            input.readAllBytes();
            List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(resource, classPath);
            for (PropertySource<?> source : sources) {
                if (source instanceof EnumerablePropertySource<?> enumerable) {
                    for (String name : enumerable.getPropertyNames()) {
                        Object value = enumerable.getProperty(name);
                        if (value != null) {
                            properties.setProperty(name, String.valueOf(value));
                        }
                    }
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao ler " + resource, exception);
        }
        return properties;
    }
}

# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
COPY config ./config
RUN mvn -B -q package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:21-jre-alpine AS runtime
# curl entra na imagem porque o healthcheck precisa de um cliente HTTP confiavel
# (eclipse-temurin:*-alpine nao garante wget/curl).
RUN apk add --no-cache curl \
    && addgroup -S educamais && adduser -S educamais -G educamais
WORKDIR /app
COPY --from=build /workspace/target/educa-mais-api.jar app.jar
USER educamais
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"
# Prontidao real: /actuator/health so responde 200 quando o contexto e o banco estao de pe.
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

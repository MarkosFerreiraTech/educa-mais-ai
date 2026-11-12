# =============================================================================
# Educa Mais - atalhos de desenvolvimento (make help para a lista)
# =============================================================================
SHELL := /bin/bash
.DEFAULT_GOAL := help

MVN := ./mvnw
API_DIR := backend
WEB_DIR := frontend

.PHONY: help
help: ## Lista os alvos disponiveis
	@grep -hE '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-22s\033[0m %s\n", $$1, $$2}'

# ------------------------------------------------------------------- backend
.PHONY: api-build api-test api-run api-run-demo api-lint api-coverage
api-build: ## Compila o backend empacotando o jar
	cd $(API_DIR) && $(MVN) -B -ntp clean package -DskipTests

api-test: ## Roda testes unitarios + integracao com cobertura
	cd $(API_DIR) && $(MVN) -B -ntp verify

api-coverage: api-test ## Gera e abre o relatorio JaCoCo
	@echo "Relatorio: $(API_DIR)/target/site/jacoco/index.html"

api-lint: ## Checkstyle + SpotBugs
	cd $(API_DIR) && $(MVN) -B -ntp checkstyle:check spotbugs:check

api-run: ## Sobe a API em modo dev (H2 em memoria, Gemini desligado)
	cd $(API_DIR) && SPRING_PROFILES_ACTIVE=dev $(MVN) spring-boot:run

api-run-demo: ## Sobe a API em modo demo contra um Postgres ja em execucao
	cd $(API_DIR) && SPRING_PROFILES_ACTIVE=demo $(MVN) spring-boot:run

# ------------------------------------------------------------------ frontend
.PHONY: web-install web-build web-test web-run
web-install: ## Instala dependencias do frontend (npm ci)
	cd $(WEB_DIR) && npm ci

web-build: ## Build de producao do Angular
	cd $(WEB_DIR) && npm run build

web-test: ## Testes unitarios (vitest, sem watch)
	cd $(WEB_DIR) && npm test -- --watch=false

web-run: ## Dev server em http://localhost:4200 com proxy /api -> 8080
	cd $(WEB_DIR) && npm start

# --------------------------------------------------------------------- infra
.PHONY: up down logs ps clean
up: ## Sobe Postgres + API + Web (docker compose)
	docker compose up -d --build

down: ## Derruba os containers
	docker compose down

logs: ## Acompanha os logs da API
	docker compose logs -f api

ps: ## Status dos containers
	docker compose ps

clean: ## Remove artefatos de build
	rm -rf $(API_DIR)/target $(WEB_DIR)/dist $(WEB_DIR)/.angular

# ------------------------------------------------------------------ qualidade
.PHONY: verify ci-doctor
verify: api-lint api-test web-test web-build ## Pipeline completo local (equivalente ao CI)

ci-doctor: ## Diagnostico da ultima execucao do backend CI
	./scripts/ci-doctor.sh

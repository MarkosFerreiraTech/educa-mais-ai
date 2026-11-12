# ---------- build ----------
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

# ---------- runtime ----------
FROM nginx:1.27-alpine AS runtime

# curl para o healthcheck (a imagem nginx:alpine nao traz curl nem wget)
RUN apk add --no-cache curl \
    && addgroup -S web && adduser -S web -G web

# Configuracao completa (contexto `main`): PID e temporarios em /tmp, porta 8080.
# Copiar para /etc/nginx/nginx.conf substitui a config padrao que exige root.
COPY nginx.conf /etc/nginx/nginx.conf

# O builder @angular/build:application gera dist/<projeto>/browser
# (nao ha outputPath no angular.json, entao o padrao vale).
COPY --from=build /app/dist/educa-mais-web/browser /usr/share/nginx/html

RUN chown -R web:web /usr/share/nginx/html /var/cache/nginx

USER web
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
  CMD curl -fsS http://localhost:8080/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]

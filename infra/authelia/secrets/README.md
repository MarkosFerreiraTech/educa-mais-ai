# Segredos do Authelia

`jwks.key` é a chave **privada RSA de demonstração** usada para assinar os tokens OIDC do
perfil `sso`. Ela é versionada **de propósito**, para que `docker compose --profile sso up -d`
funcione em um clone limpo do repositório do hackathon.

> ⚠️ Esta chave é pública (está no Git). **Nunca** use em produção.

## Em produção

1. Gere um par novo e mantenha fora do Git (o `.gitignore` bloqueia `infra/authelia/secrets/*`
   exceto este arquivo de demo):

   ```bash
   docker run --rm -u "$(id -u):$(id -g)" -v "$PWD/infra/authelia/secrets:/keys" \
     authelia/authelia:4.38 authelia crypto pair rsa generate --directory /keys
   mv infra/authelia/secrets/private.pem infra/authelia/secrets/jwks.key
   rm -f infra/authelia/secrets/public.pem
   ```

2. Troque também `AUTHELIA_STORAGE_KEY`, `AUTHELIA_SESSION_SECRET`, `AUTHELIA_JWT_SECRET` e
   `AUTHELIA_OIDC_HMAC` por valores aleatórios (≥ 32 caracteres):

   ```bash
   openssl rand -base64 48
   ```

3. Em nuvem, prefira AWS Secrets Manager / SSM Parameter Store em vez de arquivos.

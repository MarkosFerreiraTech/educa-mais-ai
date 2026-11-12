# ADR-005 — Cifragem de PII com AES-256-GCM em repouso

- **Status:** Aceito
- **Data:** 2026-11-12
- **Decisores:** Tech Lead, DPO
- **Requisitos relacionados:** RF37, RF38, RF39, RNF12, RNF13

## Contexto
O sistema trata dados de menores (nome, e-mail, telefone, data de nascimento). A LGPD exige proteção
adequada e o hackathon exige AES-256.

## Decisão
Cifrar campos sensíveis na aplicação (antes do JPA) com **AES-256-GCM**, IV aleatório de 12 bytes por
registro e *tag* de autenticação de 128 bits, serializados em Base64 (`v1:iv:tag:ciphertext`).
A chave vem de **AWS Secrets Manager/KMS** (envelope encryption); em dev, de variável de ambiente.
Buscas por e-mail usam **índice cego HMAC-SHA256** (`email_hash`).

## Alternativas consideradas
| Alternativa | Motivo da rejeição |
| --- | --- |
| AES-CBC | Sem autenticação integrada; vulnerável a manipulação |
| Cifrar só no banco (TDE/pgcrypto) | Não protege backup, réplica nem DBA com acesso |
| Tokenização externa | Custo e complexidade desnecessários no MVP |

## Consequências
- ✅ PII ilegível em dumps, réplicas e logs.
- ⚠️ Campos cifrados não permitem `LIKE`/ordenação direta — usar hash cego e atributos derivados.
- ⚠️ Rotação de chave exige re-cifragem versionada (prefixo `v1` já prepara o caminho).

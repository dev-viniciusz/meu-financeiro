# Meu Financeiro

- `frontend/src` — HTML/CSS/JS puro (vai para a Netlify)
- `backend` — API em Java com Spring Boot, em camadas (roda em Docker)
- Banco: PostgreSQL (as tabelas são criadas sozinhas ao iniciar — `backend/src/main/resources/schema.sql`)

## Camadas do backend (`backend/src/main/java/com/meufinanceiro`)

```
controller/   recebe a requisição HTTP e devolve JSON (não tem regra nenhuma)
service/      regras e validações (ex.: valor > 0, e-mail único, gerar despesas fixas do mês)
repository/   SQL (JdbcTemplate) — o único lugar que fala com o banco
model/        o que a API devolve (records)       dto/  o que a API recebe (records)
config/       login por token (AuthInterceptor) e CORS     exception/  erros em JSON
```

Fluxo: `Controller → Service → Repository → PostgreSQL`.

## Rodar local

Precisa de Java 21, Maven e um PostgreSQL com um banco chamado `financeiro`.

```bash
cd backend
mvn spring-boot:run          # porta 8080 (usuário postgres, sem senha, localhost)
# ou com outro banco:
DB_URL="jdbc:postgresql://localhost:5432/financeiro" DB_USER=postgres DB_PASSWORD=senha mvn spring-boot:run

cd ../frontend/src
python3 -m http.server 8000     # abra http://localhost:8000/pages/login.html
```

Em `localhost` o front chama a API em `http://localhost:8080` automaticamente.

## Variáveis do backend

| Variável | Para quê |
|---|---|
| `DB_URL` | `jdbc:postgresql://HOST/BANCO?sslmode=require` |
| `DB_USER` / `DB_PASSWORD` | Usuário e senha do banco |
| `ALLOWED_ORIGIN` | Domínio do front que pode chamar a API (ex.: `https://meu-site.netlify.app`). Padrão `*` |
| `PORT` | Porta (as hospedagens definem sozinhas) |

## Publicar (resumo)

1. **Banco**: crie um Postgres gratuito (ex.: Neon). Do painel, pegue host, banco, usuário e senha e monte o `DB_URL`.
2. **Backend**: suba este repositório numa hospedagem que rode Docker (ex.: Render → *Web Service*, *Root Directory* `backend`, runtime Docker). Defina `DB_URL`, `DB_USER`, `DB_PASSWORD` e `ALLOWED_ORIGIN`.
3. **Front**: em `frontend/src/js/api.js`, troque `PRODUCTION_API_URL` pela URL do backend. Na Netlify, importe o repositório (o `netlify.toml` já aponta `frontend/src`).
4. Volte no backend e ajuste `ALLOWED_ORIGIN` para a URL final da Netlify.

## Rotas da API (todas em `/api`, JSON, `Authorization: Bearer <token>`)

`POST auth/register` · `POST auth/login` · `GET|PUT profile` · `GET dashboard` ·
`GET|POST transactions` (`?type=&month=&year=&limit=`) · `PUT|DELETE transactions/:id` ·
`GET|POST fixed-expenses` · `PUT|DELETE fixed-expenses/:id` · `GET|POST notes` · `PUT|DELETE notes/:id` · `GET health`

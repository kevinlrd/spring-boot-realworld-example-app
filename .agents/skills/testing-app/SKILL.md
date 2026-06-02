---
name: testing-app
description: How to build, run, and end-to-end test the spring-boot-realworld-example-app (RealWorld REST + GraphQL API). Use when running the app locally or verifying its endpoints.
---

# Testing spring-boot-realworld-example-app

Headless Spring Boot + MyBatis + Netflix DGS (GraphQL) backend implementing the RealWorld spec. SQLite file DB (`dev.db`). Serves on `http://localhost:8080`.

## Toolchain
- **Java 11 is required** (CI uses Zulu 11). With SDKMAN: `source "$HOME/.sdkman/bin/sdkman-init.sh"; export JAVA_HOME="$HOME/.sdkman/candidates/java/current"`.
- Build tool: Gradle wrapper (`./gradlew`, Gradle 7.4).

## Common commands
- Build: `./gradlew assemble` (runs DGS GraphQL codegen).
- Test (what CI runs): `./gradlew clean test`.
- Run: `./gradlew bootRun` then `curl http://localhost:8080/tags`.
- Format: check `./gradlew spotlessCheck`, apply `./gradlew spotlessApply` (google-java-format).

## API reference (from WebSecurityConfig)
- Public: `POST /users`, `POST /users/login`, `GET /articles/**`, `GET /tags`, `/graphql`, `/graphiql`.
- All other requests need a JWT header: `Authorization: Token <jwt>`.
- Request bodies are **root-wrapped** (`spring.jackson.deserialization.UNWRAP_ROOT_VALUE=true`).

## Verified end-to-end smoke flow (curl)
```bash
# register -> 201, returns user.token (JWT)
curl -s -X POST localhost:8080/users -H 'Content-Type: application/json' \
  -d '{"user":{"username":"devintester","email":"devintester@example.com","password":"password123"}}'

# login -> 200, returns token
TOKEN=$(curl -s -X POST localhost:8080/users/login -H 'Content-Type: application/json' \
  -d '{"user":{"email":"devintester@example.com","password":"password123"}}' \
  | grep -o '"token":"[^"]*"' | head -1 | sed 's/"token":"//;s/"$//')

# create article (auth) -> 200, slug derived from title
curl -s -X POST localhost:8080/articles -H 'Content-Type: application/json' -H "Authorization: Token $TOKEN" \
  -d '{"article":{"title":"Hello World","description":"d","body":"b","tagList":["demo"]}}'

# GraphQL (public) -> data.tags + data.articles
curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' \
  -d '{"query":"{ tags articles(first:5){ edges { node { title slug tagList } } } }"}'
```

## Notes
- The `/graphiql` interactive UI loads its JS from an external CDN; if the CDN is blocked the page renders blank. The `/graphql` endpoint itself still works — test GraphQL via `/graphql` directly.
- `./gradlew clean` deletes `./dev.db`.

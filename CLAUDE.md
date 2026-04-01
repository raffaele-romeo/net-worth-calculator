# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Net Worth Calculator — a Scala 3 REST API for tracking personal assets, transactions, and net worth with currency conversion. Built with Tagless Final pattern using Cats Effect, Http4s, Doobie (PostgreSQL), and Redis (JWT token storage).

## Build & Run Commands

```bash
# Start dependencies (PostgreSQL + Redis + Adminer)
docker-compose up -d

# Run the application (port 9000)
sbt run

# Compile
sbt compile

# Format and lint
sbt fix              # runs scalafixAll + scalafmtAll + scalafmtSbt
sbt validate         # clean + scalafix check + scalafmt check + missinglink + compile

# Build Docker image
sbt docker:publishLocal

# Run tests
sbt tests/test
```

## Architecture

**Scala 3 / Tagless Final** — all services are parameterized over `F[_]` with Cats Effect typeclasses.

### Module Layout (`modules/core/src/main/scala/networthcalculator/`)

- **`algebras/`** — trait interfaces (e.g., `AssetsService[F]`, `AuthService[F]`)
- **`services/`** — implementations (e.g., `AssetsServiceImpl`, `AuthServiceImpl`)
- **`domain/`** — domain models and error types as case classes/opaque types
- **`config/`** — `Loader` reads `NWC_APP_ENV` env var (`Local`|`Test`) to select config; `data.scala` defines config types using opaque types
- **`http/routes/`** — Http4s routes split into open (`HealthRoutes`), auth (`LoginRoutes`, `LogoutRoutes`, `UserRoutes`), and secured (`AssetRoutes`, `TransactionRoutes`)
- **`http/clients/`** — `CurrencyExchangeRateClient` calls freecurrencyapi.net
- **`middleware/`** — `JWTAuthMiddleware` for secured routes
- **`modules/`** — wiring/composition: `Services`, `Security`, `Programs`, `HttpClients`, `HttpApi`
- **`programs/`** — `CurrencyExchangeRate` orchestrates exchange rate fetching with retry

### Wiring Flow (Main.scala)

`Config → AppResources (PG + Redis + HTTP client) → Services + Security + HttpClients + Programs → HttpApi → BlazeServer`

All routes are under `/v1`. Middleware applies AutoSlash, Timeout (60s), and request/response logging.

### Database

PostgreSQL schema initialized via `init/init.sql`. Three tables: `users`, `assets`, `transactions`. Doobie with HikariCP for connection pooling. Redis stores JWT tokens for session management.

### Environment

- `NWC_APP_ENV=Local` (default): connects to localhost for PG/Redis
- `NWC_APP_ENV=Test`: connects to `postgres`/`redis` hostnames (Docker network)

### Key Libraries

Cats Effect 3, Http4s (Blaze), Doobie, Circe (JSON), Ciris (config), Redis4Cats, cats-retry, Nimbus JOSE+JWT, Squants (quantities)

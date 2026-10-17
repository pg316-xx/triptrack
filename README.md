# TripNest

Plan trips together: a shared day-by-day itinerary that updates live, fair expense
splitting with minimal settle-up payments, group chat, shared documents and photos,
and a private travel wallet.

**Stack:** Java 21 · Spring Boot 4 · PostgreSQL 17 (Flyway) · Redis 7 · S3-compatible
storage · STOMP over WebSocket · React 18 + Vite.

---

## Run it locally

Requirements: Docker Desktop.

```bash
cp .env.example .env        # then set passwords/secrets in .env
docker compose up -d --build
```

| What | URL |
|---|---|
| App | http://localhost:5173 |
| Backend API | http://localhost:8080 (or `BACKEND_PORT`) |
| Mail inbox (Mailpit — every email the app sends lands here) | http://localhost:8026 |
| S3 storage (SeaweedFS) | http://localhost:8333 |
| Postgres | localhost:5432 |

Stop with `docker compose down` (keeps data) or `docker compose down -v` (wipes it).
Host ports can be changed in `.env` (`BACKEND_PORT`, `S3_PORT`, `REDIS_PORT`,
`POSTGRES_PORT`, `MAILPIT_PORT`).

The Flyway migrations in `backend/demo/src/main/resources/db/migration` create the
schema on first start.

---

## Features

**Accounts** — email/password (BCrypt, cost 12) and Google sign-in; 15-minute access
JWTs and 7-day rotating refresh tokens (reuse of a revoked token signs out every
session); forgot/reset password by email; account deletion (anonymized, so shared
expense history stays correct); rate limits on login, sign-up and reset endpoints.

**Trips & roles** — owner / admin / member. Owners manage roles, transfer ownership
and delete trips; admins edit trip details, invite and remove members; members
collaborate. Invite links expire, have a usage limit and can be emailed. Optional
cover image.

**Itinerary** — days and activities; activity order uses LexoRank strings, so a
drag-and-drop move rewrites one row. Edits use optimistic locking (409 on
conflicting edits). Every change is broadcast live to the trip. Activity locations
come from a place search that stores coordinates.

**Weather** — 5-day forecast for the destination, cached for 3 hours, with a
last-known fallback if the provider is down.

**Expenses** — equal splits to the cent or custom amounts that must sum exactly to
the total; balances, a debt-simplification "who pays whom" suggestion, and
settlements. Expense and settlement creation require an `Idempotency-Key` header
(stored in Redis for 24 h), so retries never double-charge.

**Files** — shared trip documents, a photo/video gallery (with captions and author)
and a personal wallet (tickets, confirmations, IDs; private unless shared with a
trip). Browsers upload directly to storage with 15-minute pre-signed URLs; the
backend verifies the upload before saving metadata. Buckets are never public.

**Real time** — STOMP over `/ws-trip` (SockJS fallback at `/ws-trip-sockjs`). Every
subscription is checked: you only receive your own notifications and events for trips
you belong to. Frames go through Redis pub/sub, so several backend instances behind a
load balancer all deliver them.

**Notifications** — saved first, then pushed live: new expenses, payments, documents,
members joining or leaving, role changes, trip updates.

**Audit & errors** — audit log for trip creation, member removal, role changes and
expenses. Every error is an RFC 7807 Problem Detail with a `code` and the request's
`X-Request-ID`, which also appears on every log line.

---

## Configuration

All settings are environment variables (see `.env.example` and
`backend/demo/src/main/resources/application.properties`). The most useful:

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Postgres |
| `REDIS_URL` | Redis (cache, idempotency keys, rate limits, WebSocket fan-out) |
| `JWT_SECRET` | HMAC key, 32+ characters |
| `S3_ENDPOINT`, `S3_PUBLIC_ENDPOINT`, `S3_ACCESS_KEY`, `S3_SECRET_KEY` | Storage. Leave all empty on AWS to use real S3 with the instance's IAM role |
| `S3_BUCKET_DOCUMENTS`, `S3_BUCKET_MEDIA`, `S3_BUCKET_WALLET` | Bucket names |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS`, `MAIL_FROM` | Email. On AWS use the Amazon SES SMTP endpoint |
| `GOOGLE_MAPS_API_KEY` | Optional: Google Places for location search (otherwise OpenStreetMap) |
| `OPENWEATHER_API_KEY` | Optional: OpenWeatherMap (otherwise Open-Meteo) |
| `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS` | Your frontend's address, for links in emails and CORS |
| `RATE_LIMIT_ENABLED`, `RATE_LIMIT_TRUST_FORWARDED_FOR` | Turn limits off for load tests; trust `X-Forwarded-For` behind an ALB |
| `GOOGLE_CLIENT_ID` | Google sign-in |

The frontend's API address is baked in at build time: `VITE_API_URL`
(a build arg in `docker-compose.yml`).

---

## Project layout

```text
backend/demo/            Spring Boot app
  src/main/java/com/tripnext/demo/
    config/              security, JWT, WebSocket, Redis, storage, rate limits, request ids
    controller/          REST + STOMP endpoints
    service/             business logic (trips, itinerary, expenses, files, wallet, auth…)
    integration/         place search and weather providers
    model/ repo/ dto/    JPA entities, repositories, request/response records
  src/main/resources/db/migration/   Flyway migrations V1…V6
frontend/                React + Vite app (served by nginx in Docker)
docker/                  SeaweedFS start script
loadtest/                k6 load-test scripts and a load-test compose override
docker-compose.yml       the full local stack
```

---

## Deploying

The same images run on a single VM with `docker compose`. For AWS: use S3 instead of
SeaweedFS (empty `S3_*` endpoint/keys + an IAM role, and add CORS on the buckets for
your domain), Amazon SES SMTP credentials for email, put HTTPS in front, and expose
only ports 80/443. For more than one backend instance, point them at a shared Redis
(ElastiCache) and Postgres (RDS); WebSocket events already fan out through Redis.
`/actuator/health` is open for load-balancer health checks.

---

## License

MIT License

Copyright (c) 2026

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software.

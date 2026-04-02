- Slugs: random alphanumeric
- Rate limiting: Redis token bucket / sliding window
- Cache: Redis
- Click tracking: log every click as an event
- Async: Spring `@Async`
- Analytics: Postgres only
- Payments: no subscriptions, everything free

---

# 1. Recommended architecture

## Components

- Frontend: Alpine.js
- Backend: Spring Boot
- Primary database: PostgreSQL
- Cache: Redis
- Rate limiting: Redis-based token bucket or sliding window
- Async processing: Spring `@Async`
- Hosting: Azure App Service or Azure Container Apps
- Domain/DNS: Vercel domain pointing to Azure

---

# 2. System overview

## Main user flow
1. User opens your dashboard
2. Creates a short URL
3. Backend generates a random slug
4. Link is stored in Postgres
5. Slug → destination mapping is cached in Redis
6. When someone visits the short URL:
   - check Redis first
   - if miss, fetch from Postgres and cache it
   - redirect to destination
   - record click event asynchronously
7. Analytics page reads from Postgres and shows totals / breakdowns

---

# 3. Core architecture diagram

```text
Browser
  |
  | 1. Create link / view dashboard
  v
Alpine.js frontend
  |
  v
Spring Boot API
  |
  +--> PostgreSQL (source of truth)
  |
  +--> Redis
  |      - cache redirect targets
  |      - rate limiting counters/buckets
  |
  +--> Async click tracking (@Async)
         |
         v
      PostgreSQL click_events table
```

---

# 4. Data model

## users
If you want accounts/login.

- `id`
- `email`
- `password_hash`
- `created_at`

If you want anonymous usage at first, you can skip this and add it later.

## links
Stores short link data.

- `id`
- `slug`
- `original_url`
- `created_at`
- `updated_at`
- `is_active`
- `click_count`

Useful indexes:
- unique index on `slug`
- index on `created_at`

## click_events
Stores each click.

- `id`
- `link_id`
- `clicked_at`
- `ip_address` or masked IP
- `user_agent`
- `referer`
- `country`
- `device_type`
- `browser`
- `os`

Useful indexes:
- `link_id`
- `clicked_at`
- maybe `(link_id, clicked_at)`

---

# 5. Service responsibilities

## A. Link creation service
Responsibilities:
- validate the long URL
- generate random slug
- ensure slug uniqueness
- save to Postgres
- cache slug → URL in Redis

## B. Redirect service
Responsibilities:
- accept `GET /{slug}`
- check Redis
- fallback to Postgres
- redirect user
- log click asynchronously

## C. Analytics service
Responsibilities:
- query click_events table
- aggregate by:
  - country
  - device
  - referrer
  - time period

## D. Rate limiting service
Responsibilities:
- protect creation endpoint
- protect redirect endpoint from abuse
- protect auth endpoints if you add auth later

---

# 6. Request flows

## Link creation flow
```text
POST /api/links
  -> validate URL
  -> generate slug
  -> save link in Postgres
  -> put slug -> originalUrl in Redis
  -> return short URL
```

## Redirect flow
```text
GET /{slug}
  -> check Redis
  -> if found, redirect
  -> if not found, query Postgres
  -> cache result in Redis
  -> redirect
  -> call async click logger
```

## Analytics flow
```text
GET /api/links/{id}/analytics
  -> query click_events
  -> aggregate in SQL
  -> return dashboard data
```

---

# 7. Redis usage

You chose Redis cache and Redis rate limiting, so Redis will do two jobs:

## Cache
Store:
- `slug:{slug} -> originalUrl`
- maybe `link:{slug} -> link metadata`

TTL options:
- long TTL, like 1 day to 7 days
- refresh on access if needed

## Rate limiting
Store:
- IP-based counters
- per-user counters if you later add accounts
- per-endpoint counters

Suggested limits for MVP:
- link creation: 10/minute per IP
- redirect endpoint: usually very generous, but you can still block obvious abuse
- dashboard API: 60/minute per IP

---

# 8. Async click tracking with Spring @Async

You chose Spring `@Async`, so the click tracking path is simple.

## What happens
- redirect returns immediately
- click event is saved in the background thread

## Important caveat
Spring `@Async` is easy, but it is not durable.
If the app crashes, some click events could be lost.

For MVP, that’s acceptable.
Later, if you want stronger guarantees, we can switch to a queue.

## Click data captured
- slug/link id
- timestamp
- IP
- user-agent
- referer
- country
- device info

---

# 9. Analytics approach in Postgres

Since you chose Postgres only, keep analytics simple but useful.

## Raw event storage
Each click becomes one row in `click_events`.

## Dashboard queries
You can compute:
- total clicks per link
- clicks by day
- clicks by country
- clicks by device
- top referrers

## Suggested strategy
- use raw events as source of truth
- compute aggregates on demand with SQL
- if dashboards get slow later, add summary tables

---

# 10. Suggested API endpoints

## Public
- `GET /{slug}` → redirect
- `POST /api/links` → create short link

## Dashboard
- `GET /api/links`
- `GET /api/links/{id}`
- `GET /api/links/{id}/analytics`

## Health
- `GET /actuator/health`

---

# 11. Minimal backend packages

A clean Spring Boot structure:

```text
com.yourapp.shortener
├── config
├── controller
├── dto
├── entity
├── repository
├── service
├── util
└── ShortenerApplication.java
```

## Example responsibilities
- `controller`: HTTP endpoints
- `service`: business logic
- `repository`: JPA queries
- `entity`: database models
- `config`: Redis, async, security, rate limiting

---

# 12. Recommended implementation order

## Phase 1: Core shortener
- create link
- redirect
- random slug generation
- Postgres persistence

## Phase 2: Redis
- cache redirects
- add rate limiting

## Phase 3: Click tracking
- add `click_events`
- log events with `@Async`
- build analytics endpoint

## Phase 4: Dashboard
- Alpine.js UI
- list links
- analytics charts/tables

## Phase 5: Production hardening
- link validation
- anti-abuse checks
- better logging
- monitoring
- backups

---

# 13. Random slug strategy

Since you chose random alphanumeric slugs:

## Recommended format
- 7 to 8 characters
- base62: `a-zA-Z0-9`

Example:
- `b7K2pQx`
- `X3m9Za1`

## Why this works
- short
- hard to guess
- enough combinations for MVP

## Collision handling
- generate slug
- check DB if it exists
- retry if needed

That’s enough for now.

---

# 14. What I would build first

If I were building your version, I’d do this exact MVP:

## Stack
- Spring Boot
- PostgreSQL
- Redis
- Alpine.js
- Bootstrap or Tailwind for simple UI
- Azure App Service

## Features
- create short link
- redirect
- cache redirects
- rate limit creation endpoint
- log click events async
- analytics page with totals, country, device, referrer

---

# 15. Clean architecture summary

## Source of truth
- PostgreSQL

## Fast lookup
- Redis

## Background work
- Spring `@Async`

## UI
- Alpine.js

## Hosting
- Azure

## Domain
- Vercel DNS

---

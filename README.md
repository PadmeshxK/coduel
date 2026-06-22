# Coduel

A real-time **1v1 competitive-coding platform** built around an **asynchronous, queue-based code-judging engine**. Matched players race the same problem in a live duel; their code is judged off the request thread by a pool of workers and the verdict is pushed back over WebSocket.

**▶ Live demo: https://coduel-ui.vercel.app** &nbsp;·&nbsp; Frontend repo: [coduel-ui](https://github.com/PadmeshxK/coduel-ui)


---

## Why it's interesting

- **Asynchronous judging pipeline** — `POST /submission` persists the submission and returns `202` immediately; judging happens on RabbitMQ workers. Dispatch is race-free via a **transactional outbox**: the row is saved with `dispatched=false` in the same transaction, and a scheduled **relay** turns it into a queue message and flips the flag only after the broker confirms. Re-judging is **idempotent** and poison messages **dead-letter** — so it's at-least-once without orphans or duplicates.
- **One execution path for runs *and* submissions** — the editor's "Run" (against sample cases) goes through the *same* broker → worker → executor path as a graded submission; only persistence and result-routing differ.
- **Untrusted code execution** behind a `CodeExecutor` **Strategy interface** — the current P1 impl runs each submission in an **isolated temp workspace** with a **wall-clock timeout**, full **process-tree termination** on timeout, and **output caps**. The interface keeps a container sandbox a drop-in P2 swap, untouched by the rest of the app.
- **Real-time over a single shared WebSocket/STOMP connection** — per-user queues (`/user/queue/…` for notifications, run results, submission results) and broadcast topics (`/topic/match/{id}`, `/topic/room/{id}`) all multiplex over **one** socket per session, with a subscription registry that **re-subscribes on reconnect** and a reconcile that re-reads authoritative state after a drop (no missed `MATCH_OVER`).
- **Matchmaking** — a Redis waiting-queue with **event-driven pairing**: when two players match, both are **pushed** the match (no polling). Presence tracking handles **no-show / forfeit**, and "first ACCEPTED wins" is resolved by **server receipt order + an idempotent finish**, so it's correct under concurrent judging.
- **Generic `Match` with pluggable game modes** (ranked duel, private room) so new modes plug in **without touching the execution core**.
- **Layered architecture** (`Controller → Dto → Flow → Api → Dao`) with **ports/adapters** for every piece of swappable infra — judge dispatcher, matchmaking queue, WebSocket publishers, notification inbox — so RabbitMQ/Redis/STOMP are implementation details, not dependencies of the domain.
- **Auth** — Google **OAuth2** with stateful **Redis-backed sessions**.

## Architecture

```
                      ┌──────────── Spring Boot (coduel-app) ─────────────┐
  Browser  ──REST──▶  │  Controller → Dto → Flow → Api → Dao  ──▶  MySQL/TiDB
 (React,            │        │ persist submission (dispatched=false)      │
  Monaco)           │        ▼                                            │
                      │   SubmissionRelay  ──@Scheduled outbox sweep──▶ RabbitMQ
                      └───────────────────────────────────────────────────┘
                                                                  │  (judge.queue, DLQ)
                                                                  ▼
                      ┌──────────── workers (@RabbitListener) ────────────┐
                      │  JudgeDto / RunDto  ──▶  CodeExecutor (subprocess) │
                      │            │ persist verdict                       │
                      └────────────┼───────────────────────────────────────┘
                                   ▼  verdict
   Browser  ◀──WebSocket (STOMP)── /topic/match/{id}   (duel: both players)
                                   /user/queue/submission-result | run-result (solo)
```

Redis backs sessions, the matchmaking queue, and short-lived invites/challenges.

## Tech stack

- **Language / framework:** Java 21, Spring Boot 4, Spring Web, Spring Security (OAuth2), Spring Data JPA, Spring AMQP, Spring WebSocket (STOMP)
- **Messaging:** RabbitMQ (topic exchange + dead-letter queue)
- **Data:** MySQL / TiDB · Redis
- **Build:** Maven (multi-module)
- **Frontend** ([separate repo](https://github.com/PadmeshxK/coduel-ui)): React, TypeScript, Vite, Tailwind, Monaco
- **Deployed on:** Railway (backend, push-to-deploy) · Vercel (frontend) · TiDB Cloud · Upstash Redis · CloudAMQP

## Modules

| Module | Responsibility |
|---|---|
| `coduel-execution` | Framework-agnostic execution engine — the `CodeExecutor` interface + a `ProcessBuilder` subprocess implementation. No Spring. |
| `coduel-common` | Shared layered-architecture base (`AbstractApi`/`AbstractDto`, the `ApiException` envelope, common constants). |
| `coduel-app` | The Spring Boot application — web/controllers, domain (`flow`/`api`/`dao`), RabbitMQ producers/consumers, WebSocket publishers, schedulers. |

## Running locally

**Prerequisites:** JDK 21, Maven 3.9+, and locally reachable MySQL, Redis, and RabbitMQ. Code execution needs Python 3 and a C++ compiler (`g++`) on the host.

1. Configure `coduel-app/src/main/resources/application.properties` (or environment variables) with your DB / Redis / RabbitMQ connection details and Google OAuth2 client credentials.
2. Build and run:

   ```bash
   mvn -pl coduel-app -am spring-boot:run
   ```

   The API serves under the `/coduel` context path (e.g. `http://localhost:8080/coduel`).
3. Run the [frontend](https://github.com/PadmeshxK/coduel-ui) separately and point it at the backend.

## Deployment

Backend on **Railway** (auto-rebuilds and redeploys on push), frontend on **Vercel**, with managed **TiDB Cloud**, **Upstash Redis**, and **CloudAMQP** (RabbitMQ).

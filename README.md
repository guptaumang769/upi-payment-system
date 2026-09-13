# UPI Payment System — Distributed, Resilient Microservices

A UPI/Razorpay-style money-transfer backend built as **true Spring Cloud microservices**,
showcasing the patterns that keep a payment system correct and available under failure:
**SAGA**, the **Outbox pattern**, **idempotency**, a **double-entry ledger**, and
**Resilience4j** (circuit breaker, retry, bulkhead, time limiter).

Java 21 · Spring Boot 3.3.5 · Spring Cloud 2023.0.3 · PostgreSQL 17 · Kafka · Docker.

![CI](https://github.com/guptaumang769/upi-payment-system/actions/workflows/ci.yml/badge.svg)

---

## Why this project

"Design a payment system" is one of the most-asked interview questions, especially for
fintech in India (PhonePe, Razorpay, Paytm, Cred). This implements the hard parts for
real rather than describing them:

- **Money must never be lost or double-spent** → double-entry ledger + optimistic locking + idempotency.
- **A distributed transaction spans two services** → choreography SAGA over Kafka, with compensation.
- **Events must publish exactly when the DB commits** → transactional Outbox (no dual-write).
- **A dependency failure must not cascade** → Resilience4j on every inter-service call.

---

## Architecture

📐 **Rendered diagrams** (Mermaid — HLD, UML class, ER, the SAGA+Outbox sequence, payment
state machine) → [DIAGRAMS.md](DIAGRAMS.md). Quick ASCII view below:

```
                       ┌──────────────────┐
        client ───────►│   api-gateway    │  :8080  (Spring Cloud Gateway)
                       └────────┬─────────┘  routes via Eureka (lb://), CB filter
              ┌─────────────────┴───────────────────┐
              ▼                                       ▼
     ┌──────────────────┐   OpenFeign + Resilience4j  ┌──────────────────┐
     │ payment-service  │ ───(@CircuitBreaker/@Retry/─►│  wallet-service  │
     │  :8081           │      @Bulkhead/@TimeLimiter) │  :8082           │
     │  • idempotency   │                              │  • double-entry  │
     │  • Outbox → Kafka│                              │    ledger        │
     │  • SAGA orchestr.│◄──── Kafka events ──────────►│  • debit/credit  │
     └────────┬─────────┘                              └────────┬─────────┘
              ▼                                                  ▼
        paymentdb (Postgres)                              walletdb (Postgres)

  Supporting: discovery-server (Eureka :8761) · config-server (:8888) · Kafka (:9092)
```

Modules: `api-gateway`, `payment-service`, `wallet-service`, `discovery-server`,
`config-server`, `common` (shared events/enums/DTOs).

---

## The patterns (interview talking points)

### 1. Outbox pattern — guaranteed event publishing
`payment-service` writes the `PaymentInitiatedEvent` into an `outbox_events` table **in
the same transaction** as the `Payment` insert. A `@Scheduled` `OutboxPoller` then relays
unpublished rows to Kafka. **Why:** publishing to Kafka *inside* the DB transaction is a
dual-write — if the commit succeeds but the Kafka send fails (or vice versa), state and
events diverge. The outbox makes the write atomic (one DB commit) and the publish
at-least-once, eliminating lost/ghost events.

### 2. Choreography SAGA — distributed transaction without 2PC
A transfer spans two services, so there's no single ACID transaction. Instead:
`PaymentInitiated → (wallet debits) → WalletDebited → (payment credits payee, marks
COMPLETED)`. If the debit fails → `WalletDebitFailed → payment marks FAILED`
(**compensation**). Choreography (services react to events) over orchestration keeps the
services decoupled. **Why not 2PC:** two-phase commit locks resources across services and
doesn't scale; SAGA trades atomicity for eventual consistency, which payments tolerate.

### 3. Idempotency — safe retries
`POST /payments` takes an `Idempotency-Key`; a replay returns the original payment instead
of creating a second. The ledger is idempotent per `(payment_id, type)`. **Why:** clients
and networks retry; without idempotency a retry double-charges.

### 4. Double-entry ledger — auditable, correct balances
Every movement is a `LedgerEntry` (DEBIT/CREDIT) with `balance_after`; the wallet balance
is updated under **optimistic locking** (`@Version`) so concurrent debits can't lost-update.
**Why:** a running balance column alone can't be audited or reconciled; double-entry is how
real financial systems stay provably consistent.

### 5. Resilience4j — failure isolation
The `payment → wallet` call (OpenFeign) is wrapped with **@CircuitBreaker** (stop calling a
failing dependency), **@Retry** (ride out transient blips), **@Bulkhead** (cap concurrent
calls so one slow dependency can't exhaust threads), and **@TimeLimiter** (bound latency),
with a **fallback**. The gateway adds a route-level circuit breaker. **Why:** in a
distributed system, a single slow/broken service must degrade gracefully, not cascade.

### 6. Spring Cloud infra
**Eureka** (services find each other by name, `lb://payment-service`), **Spring Cloud
Gateway** (one entry point, cross-cutting concerns), **Config Server** (centralized config).

---

## Run it

**Prerequisites:** JDK 21 (Temurin or Corretto — needed only if you build/test locally;
the containers bring their own) · Maven · Docker Desktop with **~6 GB RAM** free (this runs
7 containers: postgres, kafka, config-server, discovery-server, wallet-service,
payment-service, api-gateway).

See **[RUN-GUIDE.md](RUN-GUIDE.md)** for the full walkthrough. TL;DR:

```bash
git clone https://github.com/guptaumang769/upi-payment-system.git
cd upi-payment-system

docker compose up -d --build
# Eureka UI: http://localhost:8761   ·   gateway: http://localhost:8080

# Give the services ~60–90s to register in Eureka, then verify the gateway is up:
curl localhost:8080/actuator/health   # → {"status":"UP"}

curl -s -XPOST localhost:8080/api/v1/payments \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: k1' \
  -d '{"fromVpa":"alice@upi","toVpa":"bob@upi","amount":250.00}'
```
Then watch the SAGA move money `alice → bob` and the balances update.

---

## Tests

```bash
mvn install -DskipTests     # build the reactor (installs parent + common)
mvn test -pl wallet-service,payment-service
```
- `WalletServiceTest` — debit/credit, idempotency, insufficient-balance (overdraft) guard
- `PaymentServiceTest` — idempotent initiate + outbox write

Integration tests and the live multi-service flow need Docker (see RUN-GUIDE).

---

## What this demonstrates for interviews

Distributed transactions (SAGA), exactly-once-ish delivery (outbox + idempotency),
financial correctness (double-entry + optimistic locking), fault tolerance (Resilience4j),
and microservice infrastructure (Eureka / Gateway / Config) — the full "design a payment
system" answer, implemented and runnable.

Concept deep-dives + interview Q&A → see the project's design notes.

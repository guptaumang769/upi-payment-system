# UPI Payment System — Run Guide

How to stand up the full distributed system and watch a payment flow through it end
to end. Two ways: **all-in-Docker** (simplest) or **hybrid** (infra in Docker, services
from your IDE for debugging).

Prereqs: Docker running, Java 21, Maven.

---

## Ports & topology

| Service | Port | Role |
|---|---|---|
| api-gateway | 8080 | single entry point → routes to services via Eureka (`lb://`) |
| payment-service | 8081 | payments, Outbox, SAGA orchestration, Resilience4j |
| wallet-service | 8082 | double-entry ledger, debit/credit |
| discovery-server (Eureka) | 8761 | service registry — UI at http://localhost:8761 |
| config-server | 8888 | centralized config (native mode) |
| postgres | 5432 | two DBs: `paymentdb`, `walletdb` |
| kafka | 9092 | SAGA event bus |

Seeded wallets: `alice@upi` (₹10,000), `bob@upi` (₹5,000), `carol@upi` (₹0).

---

## Option A — Everything in Docker (recommended first run)

```bash
cd upi-payment-system
docker compose up -d --build          # builds all 5 images + starts infra
docker compose ps                     # wait until all are healthy/up
```

First build downloads Maven deps per service, so it takes a few minutes. Watch progress:
```bash
docker compose logs -f payment-service
```

**Verify the cluster formed:**
- Eureka dashboard: http://localhost:8761 — you should see `PAYMENT-SERVICE`,
  `WALLET-SERVICE`, `API-GATEWAY` registered.
- Health: `curl localhost:8080/actuator/health` (gateway), `:8081`, `:8082`.

Tear down:
```bash
docker compose down          # keep data
docker compose down -v       # also wipe the Postgres volume
```

---

## Option B — Hybrid (infra in Docker, services in IDE)

Good for debugging or stepping through the SAGA.

```bash
# 1. Start only infra
docker compose up -d postgres kafka

# 2. Start platform services in order (separate terminals or IDE run configs),
#    each defaults to localhost for DB/Kafka/Eureka/config:
mvn -pl config-server    spring-boot:run
mvn -pl discovery-server spring-boot:run
mvn -pl wallet-service   spring-boot:run
mvn -pl payment-service  spring-boot:run
mvn -pl api-gateway      spring-boot:run
```

---

## Drive a payment end to end

All calls go through the **gateway** on :8080 (that's the point of the gateway).

**1. Check starting balances**
```bash
curl -s localhost:8080/api/v1/wallets/alice@upi
curl -s localhost:8080/api/v1/wallets/bob@upi
```

**2. Initiate a payment (alice → bob, ₹250).**
The `Idempotency-Key` header makes retries safe — send the same key twice and only one
payment is created.
```bash
curl -s -XPOST localhost:8080/api/v1/payments \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-key-001' \
  -d '{"fromVpa":"alice@upi","toVpa":"bob@upi","amount":250.00}'
```
You get back a payment with a `paymentRef` and status `INITIATED`.

**3. Watch the SAGA complete (async).** Within a moment:
- payment-service writes a `PaymentInitiatedEvent` to its **outbox**, the poller relays
  it to Kafka;
- wallet-service consumes it, **debits alice**, emits `WalletDebitedEvent`;
- payment-service consumes that, **credits bob** (via the Resilience4j-guarded Feign
  call) and marks the payment `COMPLETED`.

Check the result:
```bash
curl -s localhost:8080/api/v1/payments/<paymentRef>     # status → COMPLETED
curl -s localhost:8080/api/v1/wallets/alice@upi          # 10000 → 9750
curl -s localhost:8080/api/v1/wallets/bob@upi            #  5000 → 5250
```

**4. Prove idempotency** — re-run step 2 with the *same* `Idempotency-Key`: you get the
same payment back, balances don't move again.

**5. Trigger a compensation** — pay from `carol@upi` (₹0 balance): the debit fails,
wallet-service emits `WalletDebitFailedEvent`, payment-service marks it `FAILED`. No
money moves. That's the SAGA compensating.

---

## See the resilience patterns

**Circuit breaker state** (payment-service → wallet-service call):
```bash
curl -s localhost:8081/actuator/circuitbreakers | jq
```
Stop wallet-service (`docker compose stop wallet-service`) and fire a few payments: the
breaker **opens**, the Feign call short-circuits to the **fallback**, and the gateway
route breaker returns the fallback response instead of hanging. Restart wallet-service
and the breaker moves `OPEN → HALF_OPEN → CLOSED`.

**Metrics / traces** (if you also run the observability stack from BookMyShow):
`curl localhost:8081/actuator/prometheus | grep resilience4j`.

---

## Common issues

| Symptom | Fix |
|---|---|
| Services not in Eureka | Give it ~30s; check `EUREKA_URI`; confirm discovery-server healthy |
| `Connection refused` to Kafka | Kafka not healthy yet — `docker compose logs kafka`; services retry |
| Flyway validation error | `docker compose down -v` to reset the DB volume, then up again |
| Port already in use | Something else on 8080/5432/9092 — stop it or edit compose ports |
| First build very slow | Maven downloading deps per module; subsequent builds are cached |

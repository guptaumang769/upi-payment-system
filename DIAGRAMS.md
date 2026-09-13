# UPI Payment System — Diagrams

Mermaid diagrams generated from the actual services (`payment-service`, `wallet-service`),
their entities, and the Spring Cloud topology.

- [1. High-Level Design (HLD)](#1-high-level-design-hld)
- [2. UML Class Diagram](#2-uml-class-diagram)
- [3. Entity-Relationship Diagram](#3-entity-relationship-diagram)
- [4. SAGA + Outbox Sequence](#4-saga--outbox-sequence)
- [5. Payment State Machine](#5-payment-state-machine)

---

## 1. High-Level Design (HLD)

```mermaid
flowchart TB
    Client[Client / UPI UI] -->|HTTPS| GW[api-gateway<br/>Spring Cloud Gateway<br/>routes + CB filter]

    subgraph Platform[Spring Cloud platform]
      Eureka[discovery-server<br/>Eureka registry]
      Config[config-server<br/>centralized config]
    end

    GW -. lb:// via .-> Eureka
    Pay -. registers .-> Eureka
    Wal -. registers .-> Eureka
    Pay -. reads config .-> Config
    GW -->|/api/v1/payments/**| Pay[payment-service]
    GW -->|/api/v1/wallets/**| Wal[wallet-service]

    Pay -->|OpenFeign + Resilience4j<br/>CB · Retry · Bulkhead · TimeLimiter| Wal
    Pay --> PayDB[(paymentdb<br/>payments + outbox)]
    Wal --> WalDB[(walletdb<br/>wallets + ledger)]

    Pay -->|Outbox poller| Kafka{{Kafka}}
    Wal -->|consumes / emits| Kafka
    Kafka -. failures .-> DLT{{*.DLT}}

    note1[SAGA choreography over Kafka:<br/>PaymentInitiated → WalletDebited → COMPLETED<br/>or WalletDebitFailed → FAILED compensation]
```

---

## 2. UML Class Diagram

```mermaid
classDiagram
    class PaymentController {
      +initiate(idempotencyKey, req) ApiResponse
      +get(ref) ApiResponse
    }
    class PaymentService {
      +initiatePayment(req, key) Payment
      +onWalletDebited(event) void
      +onWalletDebitFailed(event) void
    }
    class OutboxPoller {
      +publishPending() void
    }
    class WalletClient {
      <<Feign + Resilience4j>>
      +credit(vpa, req) WalletResponse
      +creditFallback(...) WalletResponse
    }
    class Payment {
      +Long id
      +String paymentRef
      +String fromVpa
      +String toVpa
      +BigDecimal amount
      +PaymentStatus status
      +String idempotencyKey
    }
    class OutboxEvent {
      +Long id
      +String aggregateId
      +String eventType
      +String payload
      +boolean published
    }

    class WalletController {
      +balance(vpa) ApiResponse
      +debit(vpa, req) ApiResponse
      +credit(vpa, req) ApiResponse
    }
    class WalletService {
      +debit(vpa, amount, paymentId) Wallet
      +credit(vpa, amount, paymentId) Wallet
    }
    class Wallet {
      +Long id
      +String vpa
      +BigDecimal balance
      +Long version
    }
    class LedgerEntry {
      +Long id
      +Long walletId
      +TransactionType type
      +BigDecimal amount
      +BigDecimal balanceAfter
      +String paymentId
    }

    PaymentController --> PaymentService
    PaymentService --> Payment
    PaymentService --> OutboxEvent : writes in same tx
    PaymentService --> WalletClient : resilient credit
    OutboxPoller --> OutboxEvent : relays to Kafka
    WalletController --> WalletService
    WalletService --> Wallet
    WalletService --> LedgerEntry : double-entry
    Wallet "1" o-- "many" LedgerEntry
```

---

## 3. Entity-Relationship Diagram

Two databases (one per service — the database-per-service pattern). No cross-service FKs;
services are linked only by `payment_id` carried in events.

```mermaid
erDiagram
    PAYMENTS {
      bigint id PK
      varchar payment_ref UK
      varchar from_vpa
      varchar to_vpa
      numeric amount
      varchar status
      varchar idempotency_key UK
    }
    OUTBOX_EVENTS {
      bigint id PK
      varchar aggregate_id "= payment_ref"
      varchar event_type
      text payload
      boolean published
    }

    WALLETS ||--o{ LEDGER_ENTRIES : "has (double-entry)"
    WALLETS {
      bigint id PK
      varchar vpa UK
      numeric balance
      bigint version "optimistic lock"
    }
    LEDGER_ENTRIES {
      bigint id PK
      bigint wallet_id FK
      varchar type "DEBIT / CREDIT"
      numeric amount
      numeric balance_after
      varchar payment_id "idempotency: unique(payment_id,type)"
    }
```

---

## 4. SAGA + Outbox Sequence

The headline flow: a transfer as a choreographed distributed transaction.

```mermaid
sequenceDiagram
    autonumber
    actor U as Client
    participant P as payment-service
    participant PDB as paymentdb
    participant K as Kafka
    participant W as wallet-service
    participant WDB as walletdb

    U->>P: POST /payments (Idempotency-Key)
    P->>PDB: BEGIN TX — INSERT payment(INITIATED)<br/>+ INSERT outbox(PaymentInitiated)
    Note over P,PDB: single atomic commit — no dual-write
    P-->>U: 200 {ref, status: INITIATED}

    P->>K: OutboxPoller relays PaymentInitiated
    K->>W: PaymentInitiatedEvent
    W->>WDB: debit payer (optimistic lock,<br/>idempotent per payment_id)
    alt debit ok
        W->>K: WalletDebitedEvent
        K->>P: consume
        P->>W: credit payee (Feign + Resilience4j)
        P->>PDB: status = COMPLETED
    else insufficient funds
        W->>K: WalletDebitFailedEvent
        K->>P: consume
        P->>PDB: status = FAILED (compensation)
    end
```

---

## 5. Payment State Machine

```mermaid
stateDiagram-v2
    [*] --> INITIATED : POST /payments
    INITIATED --> COMPLETED : WalletDebited + payee credited
    INITIATED --> FAILED : WalletDebitFailed (compensation)
    COMPLETED --> [*]
    FAILED --> [*]
```

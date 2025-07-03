
---

# 💳 ISO8583 NAPAS Simulator — Transaction Processing, Routing, Logging, and Database Persistence

## 🎯 Project Objectives

This project simulates a transaction processing system based on the **ISO 8583** standard, aimed at **learning** and **integration testing** in the following areas:

* Sending and receiving ISO8583 messages over TCP
* Routing transactions based on **card BIN codes**
* Storing transaction data in a database
* Configuring transaction flows using jPOS deploy files

> 🛠️ Built with: **[jPOS 3.0.0-SNAPSHOT](https://github.com/jpos/jPOS)** — the latest development version of jPOS

---

## ⚙️ Core Components

### 🖥️ 1. ISO8583 Server (jPOS)

* Listens for ISO8583 messages over TCP using `ISOServer`
* Routes transactions based on:

  * **BIN codes** (Bank Identification Number)
* Logs all transaction details: timestamps, headers, MTI, and field data
* Supports **Luhn algorithm** validation for PAN generation (`generate_card.py`)

### 🔄 2. Transaction Manager (TXNMGR)

* Manages transaction flows through a participant chain:

  * `CheckFields` – validates required fields
  * `SelectDestination` – routes by BIN
  * `QueryHost` – forwards message to host
  * `Transaction` – persists transaction to database
  * `SendResponse` – sends ISO8583 response
* Connects to the database using **JPA + Hibernate**

---

## 📁 Project Structure

| File / Directory       | Description                                              |
| ---------------------- | -------------------------------------------------------- |
| `cfg/default.yml`      | Configuration for DB, server port, and outbound channels |
| `deploy/10_server.xml` | jPOS `ISOServer` and `ChannelAdaptor` setup              |
| `deploy/30_txnmgr.xml` | TransactionManager and participant flow definition       |
| `cfg/napas.xml`        | ISO8583 packager configuration for NAPAS format          |
| `log/`                 | Transaction logs (rotated by session or date)            |

---

## 🔀 BIN Routing Example in SelectDestination

Sample configuration for routing by BIN:

```xml
<participant class="org.jpos.transaction.participant.SelectDestination">
  <property name="request" value="REQUEST" />
  <property name="destination" value="DESTINATION" />

  <!-- Static routing -->
  <endpoint destination="VCB">970426 411130</endpoint>
  <endpoint destination="VIB">970441</endpoint>
  <endpoint destination="TPB">970423</endpoint>

  <!-- Dynamic routing using regex -->
  <regexp destination="NAPAS_SPECIAL">^4111[0-9]{2}.*</regexp>
</participant>
```

> ✅ If the card's BIN matches any configured route, the message is forwarded to the corresponding host channel (via QMUX)

---

## 💻 Technology Stack

* **Java 23** (with Virtual Threads - Project Loom)
* **jPOS 3.0.0-SNAPSHOT**
* **Spring Framework** (Core, ORM, Data JPA)
* **Hibernate 6** (JPA 3.1 implementation)
* **PostgreSQL** database
* **Lombok** (boilerplate reduction)
* **Apache Commons Lang**
* **Auth0 Java JWT** (optional authentication support)
* **HdrHistogram** (for performance monitoring)

---

## 📄 Sample ISO8583 Logs

```text
2025-07-03 15:22:54.306  INFO  --- ServerChannel      [RCVE] : ================ ISO Message =================
2025-07-03 15:22:54.306  INFO  --- ServerChannel      [RCVE] : Direction = [incoming]
2025-07-03 15:22:54.306  INFO  --- ServerChannel      [RCVE] : Packager  = [napas.xml]
2025-07-03 15:22:54.306  INFO  --- ServerChannel      [RCVE] : Header    = [6000030000]
2025-07-03 15:22:54.306  INFO  --- ServerChannel      [RCVE] : MTI       = [0200]
2025-07-03 15:22:54.306  INFO  --- ServerChannel      [RCVE] : Field 2   = [________________]
2025-07-03 15:22:54.306  INFO  --- ServerChannel      [RCVE] : Field 3   = [000000]
2025-07-03 15:22:54.306  INFO  --- ServerChannel      [RCVE] : Field 4   = [000000010000]
2025-07-03 15:22:54.306  INFO  --- ServerChannel      [RCVE] : Field 7   = [0703222254]
2025-07-03 15:22:54.306  INFO  --- ServerChannel      [RCVE] : Field 11  = [123456]
2025-07-03 15:22:54.306  INFO  --- ServerChannel      [RCVE] : Field 12  = [153045]
2025-07-03 15:22:54.306  INFO  --- ServerChannel      [RCVE] : Field 13  = [0701]
2025-07-03 15:22:54.306  INFO  --- ServerChannel      [RCVE] : Field 14  = [____]
2025-07-03 15:22:54.306  INFO  --- ServerChannel      [RCVE] : Field 32  = [970426]
2025-07-03 15:22:54.306  INFO  --- ServerChannel      [RCVE] : Field 37  = [123456789012]
2025-07-03 15:22:54.306  INFO  --- ServerChannel      [RCVE] : Field 41  = [TERMID01]
2025-07-03 15:22:54.306  INFO  --- ServerChannel      [RCVE] : Field 42  = [MERCHID000001  ]
2025-07-03 15:22:54.306  INFO  --- ServerChannel      [RCVE] : Field 49  = [704]
2025-07-03 15:22:54.306  INFO  --- ServerChannel      [RCVE] : ================ End of ISO Message ==========

2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : ================ ISO Message =================
2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : Direction = [outgoing]
2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : Packager  = [napas.xml]
2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : Header    = [6000030000]
2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : MTI       = [0210]
2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : Field 2   = [________________]
2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : Field 3   = [000000]
2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : Field 4   = [000000010000]
2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : Field 7   = [0703222254]
2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : Field 11  = [123456]
2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : Field 12  = [153045]
2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : Field 13  = [0701]
2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : Field 14  = [____]
2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : Field 32  = [970426]
2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : Field 37  = [123456789012]
2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : Field 39  = [000]
2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : Field 41  = [TERMID01]
2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : Field 42  = [MERCHID000001  ]
2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : Field 49  = [704]
2025-07-03 15:22:54.503  INFO  --- ServerChannel      [SEND] : ================ End of ISO Message ==========
```

---

## 🚀 How to Run

### 1. Configure the system

Create or edit `cfg/default.yml`:

```yaml
# TCP port for ISO8583 server
qserver:
  port: 9999

# Outbound host channels (Acquirer banks)
bank:
  tpb:
    host: localhost
    port: 8000
  tcb:
    host: localhost
    port: 8001
  vcb:
    host: localhost
    port: 8002
  vib:
    host: localhost
    port: 8003

# Host simulators (mock responses)
hostSimulate:
  tpb:
    port: 8000
  tcb:
    port: 8001
  vcb:
    port: 8002
  vib:
    port: 8003

# PostgreSQL database connection
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/demo
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

---

### 2. Start jPOS

```bash
./start.sh
```

---

### 3. Generate Valid Card Numbers (Luhn Check)

To generate valid card PANs (Primary Account Numbers) that pass the **Luhn algorithm**:

```bash
python generate_card.py
```

> ✅ This script input Bincode will output randomly generated PANs that are structurally valid and ready to be used in ISO8583 transaction testing (e.g. field 2).

---

### 4. Send ISO8583 Message

Use any ISO8583 client (e.g., Java or Python) to send a transaction request to `localhost:9999`.

---

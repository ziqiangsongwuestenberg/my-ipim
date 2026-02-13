# my-pim:

[![CI](https://github.com/ziqiangsongwuestenberg/my-ipim/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/ziqiangsongwuestenberg/my-ipim/actions/workflows/ci.yml)

A personal Spring Boot + PostgreSQL (Docker) backend project inspired by Product Information Management (PIM) systems.

This project is intentionally designed as a technical showcase focusing on backend architecture, domain modeling, database design, export/batch processing, and integration testing — rather than a production-ready application.

Current primary functionality:
Supports scheduled and REST-triggered export jobs with chunk-based parallel asynchronous processing,
structured payload handling, XML generation, and S3 upload integration,
and Micrometer-based metrics for monitoring job duration, success/failure rates, and running jobs.


### 1. Project Positioning
* Senior Java Backend interview portfolio
* Backend architecture & data modeling showcase
* Inspired by enterprise-grade PIM  systems
* Not a CRUD demo
* Not a full-featured PIM product


### 2. Tech Stack:
* Java 21,
* Spring Boot 3, 
* Spring Web,
* Spring Data JPA,
* PostgreSQL 16 
  * Dockerized for local development
  * Provisioned via Testcontainers for integration tests
* Flyway 
  * database schema migrations 
  * test data seeding
* Testcontainers (integration testing with real PostgreSQL)
* Docker & Docker Compose,
* Gradle (build tool),
* Hibernate / JPA,
* Lombok
* AWS S3 SDK
* JUnit 5
* GitHub Actions (CI)
* Micrometer (metrics & observability,Timers, Counters, Running job gauges)
* OpenTelemetry (Java Agent) + OTEL Collector + Jaeger (distributed tracing via OTLP)


### 3. Project Structure
(full structure is in project-structure.txt)
```
src/main/java
 └── com/song/my_pim
     ├── common        # Shared utilities
     ├── config        # Configuration
     ├── dto           # Data transfer objects
     ├── entity        # JPA entities
     ├── repository   # Data access layer
     ├── service      # Business logic
     │   └── exportjob   # Export job, chunking, async, writers
     ├── specification# JPA Specifications
     └── web           # REST controllers
```
The structure follows a layered architecture, with additional sub-packages introduced where complexity justifies it (e.g. export jobs).


### 4. Domain Model Overview

Core Domains
* Article / Product
  * Multi-tenant (client)
  * Logical deletion
  * Multiple lifecycle status fields
* Attributes (EAV model)
  * Multiple value types (STRING, NUMBER, BOOLEAN, DATE, ENUM)
  * Multi-value support
* Pricing
  * Price definitions separated from article-price relations
  * Time-based validity
* Job / Scheduler
  * Job definitions and job history tracking
  * Repository support for scheduled execution records
* Category Tree (prepared)
  * Tree schema designed but business logic not yet implemented
* User / Role / Right (prepared)
  * Schema prepared for future security integration



### 5. Database Design

The database schema reflects patterns commonly used in enterprise PIM systems, with a strong focus on auditability, consistency, and export-oriented read performance.

Key Characteristics
* Multi-tenant design  
  Most business tables include a client column for tenant isolation.
* Logical deletion  
  Records are soft-deleted using a deleted flag to preserve historical data.

* Centralized audit fields  
  All domain tables include:
  * creation_time
  * update_time
  * creation_user
  * update_user  
A shared PostgreSQL trigger automatically updates update_time on every update, ensuring consistency even for bulk SQL operations.

####  Modeling Highlights

* Article / Product  
Unified table with article_type differentiation and multiple status fields, reflecting common enterprise lifecycle management patterns.
* Category Tree (prepared)  
Designed using a combination of adjacency list and materialized path (parent_node, hierarchy_path, level_no), supporting multiple independent trees.
* Attributes (EAV)  
Entity–Attribute–Value model with multi-value support and ENUM lookups via a dedicated table.
* Pricing  
Clean separation between price definitions and article-price relations, supporting multiple price types and time validity.

#### Migration & Testing
* Database schema and seed data are fully managed via Flyway
* Integration tests use Testcontainers with a real PostgreSQL instance


### 6. **Export Architecture (Core Focus)**

Export functionality is the primary focus of this project and demonstrates progressive backend design.

#### Export Evolution :
(These are 3 export jobs)
* 1. Advanced async & chunk-based export 
  * Streaming XML via XMLStreamWriter
  * Chunk-based processing
  * Asynchronous writers
  * Temporary file merging
  * Payload abstraction for extensibility
  * Upload to AWS S3  
  * scheduled job execution


* 2. Structured export with prices
  * Articles with attributes and prices
  * Improved XML structure
  * Clear separation of concerns
  * Upload to AWS S3


* 3. Simple paged export
* Articles with attributes
* No pricing
* Page-based processing
  
#### Key Components :

* ExportJobContext – shared runtime context
* ExportJobPayloadHandler – pluggable output targets
* Chunk & async writers
* Streaming XML generation (low memory footprint)
* Job scheduling & execution tracking
* S3 bucket upload and export storage

This architecture mirrors real-world batch/export systems handling large datasets.

#### Observability & Metrics

Export execution is instrumented using Micrometer metrics:

* Timer (duration per phase: export / upload)
* Counter (success / failure tracking with exception tagging)
* Gauge (currently running jobs per client and job type)

Metrics are tagged by:
* phase (export / upload)
* job_type
* client
* result (success / fail)
* exception type

This enables production-style monitoring and operational visibility of export workloads.

#### Tracing (OpenTelemetry + Jaeger)

This project supports distributed tracing for local debugging using OpenTelemetry and Jaeger.

**Components**
- **my-pim-app** runs with the OpenTelemetry **Java Agent** (`-javaagent:/otel/opentelemetry-javaagent.jar`)
- **OpenTelemetry Collector** receives traces via **OTLP** (gRPC/HTTP) on **4317/4318**
- **Jaeger (all-in-one)** stores and visualizes traces (UI on **16686**)

**Trace pipeline**
`my-pim-app (OTel Agent) → OTLP (4317/4318) → otel-collector → Jaeger`

**Jaeger UI**
- http://localhost:16686

**Ports (Docker Compose)**
- OTEL Collector: `4317` (OTLP/gRPC), `4318` (OTLP/HTTP)
- Jaeger UI: `16686`
- Jaeger gRPC ingestion (from collector): `14250`

> Note: This setup is intended for local development only (no auth / no persistence by default).

In production, tracing should be configured via environment variables and proper security controls.

### 7. API Example – Export Articles (XML)
> Note: (This endpoint triggers an export job. The XML file is generated using a
streaming, chunk-based writer and uploaded to S3. The response returns
the final S3 location of the exported file.)
* Endpoint :  
  * POST /api/exports/articlesAsync.xml/s3
* Headers :
  * Accept: application/json
  * Content-Type: application/json
* Request Body :  
  {
  "client": 12,
  "requestedBy": "demo-user"
  }
* Response (JSON)  
  {
  "s3Uri": "s3://my-ipim-exports/exports/articles/client-12/articles-2026-01-24T17:34:04.147871600.xml"
  }  
  * s3Uri – Location of the generated XML export in the S3 bucket


### 8. Testing & CI

* Integration tests run against a real PostgreSQL instance using Testcontainers
* Database schema and test data are applied via Flyway
* GitHub Actions CI
  * Java 21 (Temurin)
  * Full build & test execution
  * Artifact upload

This ensures that test behavior closely matches production-like environments.


### 9. How to Run Locally
* Prerequisites: 
  * Java 21+
  * Docker, Docker Compose
* Start PostgreSQL (Docker): docker compose up -d
* Application Configuration: (Configuration files containing credentials are excluded from version control.) Create a local config file, then "cp src/main/resources/application-example.properties \
  src/main/resources/application.properties"
* Run the Application: "./gradlew bootRun", or directly from IntelliJ via the Spring Boot run configuration.
* Database Initialization: V1__create_tables.sql (core tables), v2__seed_base_data.sql (sample data)
* - “Optional: Start tracing stack (Jaeger + otel-collector)”
 
### 10. Current Status
* Core domain entities
* Export architecture (streaming, async, chunk-based)
* Micrometer-based job monitoring (duration, counters, running gauges)
* PostgreSQL (Docker)
* Flyway migrations
* Integration tests with Testcontainers
* CI pipeline


 Work in progress:
*  Category tree business logic
* Import functionality
* Security / authentication
* Advanced query optimization


### 11. Author :

Ziqiang Song,
Java Developer








































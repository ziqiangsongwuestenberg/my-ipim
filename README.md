1, my-pim:

A personal Spring Boot + PostgreSQL (Docker) backend project inspired by Product Information Management (PIM) systems.

This project is a work-in-progress personal implementation focusing on backend architecture, domain modeling, and data access patterns commonly used in PIM systems.

It is designed primarily as a technical showcase for Java backend development rather than a production-ready application.



2, Tech Stack:
* Java 17,
* Spring Boot, 
* Spring Web,
* Spring Data JPA,
* PostgreSQL 16 (
  - Dockerized for local development
  - Provisioned via Testcontainers for integration tests
  ),
- Flyway (database schema & test data migrations)
- Testcontainers (integration testing with real PostgreSQL)
* Docker & Docker Compose,
* Gradle (build tool),
* Hibernate / JPA,
* Lombok
* CI


3, Project Structure

```
src/main/java
 └── com/song/my_pim
     ├── common        # Shared utilities
     ├── config        # Configuration
     ├── dto           # Data transfer objects
     ├── entity        # JPA entities
     ├── repository   # Data access layer
     ├── service      # Business logic
     ├── specification# JPA Specifications
     └── web           # REST controllers
```


4, How to run:
* Prerequisites: Java 17+, Docker, Docker Compose
* Start PostgreSQL (Docker): docker compose up -d
* Application Configuration: (Configuration files containing credentials are excluded from version control.) Create a local config file, then "cp src/main/resources/application-example.properties \
  src/main/resources/application.properties"
* Run the Application: "./gradlew bootRun", or directly from IntelliJ via the Spring Boot run configuration.
* Database Initialization: V1__create_tables.sql (core tables), v2__seed_base_data.sql (sample data)
 
5, Current Status
* Core domain entities implemented
* Dockerized PostgreSQL setup
* Repository & service layers in place
* CI pipeline
* Extended export functionality

 Work in progress:
* Advanced query optimization
* Integration tests

6, Export Articles (XML) Endpoint

This project provides an API endpoint to export articles as an XML file.

Endpoint:
* POST /api/exports/articles.xml
* Accept: application/xml
* Content-Type: application/json

Request Body: {
  "client": 12,
  "requestedBy": "demo-user"
}

* client (required): client identifier to export data for
* requestedBy (optional): user triggering the export (for logging / audit)

* Content-Type: application/xml
* Content-Disposition: attachment; filename="articles-export.xml"
* Response body contains the exported XML document

Manual Test:
You can manually trigger the export using tools like curl, Insomnia, or Postman.

Example using curl:
curl -X POST http://localhost:8081/api/exports/articles.xml \
  -H "Accept: application/xml" \
  -H "Content-Type: application/json" \
  -d '{"client":12}' \
  -o articles-export.xml


7, Integration Tests

Integration tests are executed inside a VM environment with Docker enabled.
PostgreSQL is provisioned dynamically using Testcontainers.
Database schema and test data are managed via Flyway.

To run tests:

./gradlew test

Author :
Zq Song,
Java Developer








































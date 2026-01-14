1, my-pim:

A personal Spring Boot + PostgreSQL (Docker) backend project inspired by Product Information Management (PIM) systems.

This project is a work-in-progress personal implementation focusing on backend architecture, domain modeling, and data access patterns commonly used in PIM systems.

It is designed primarily as a technical showcase for Java backend development rather than a production-ready application.



2, Tech Stack:
* Java 17,
* Spring Boot, 
* Spring Web,
* Spring Data JPA,
* PostgreSQL 16 (Dockerized),
* Docker & Docker Compose,
* Gradle (build tool),
* Hibernate / JPA,
* Lombok

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
 
5,Current Status
* Core domain entities implemented
* Dockerized PostgreSQL setup
* Repository & service layers in place

 Work in progress:
*  Extended export functionality
* Advanced query optimization
* Integration tests
* CI pipeline





6, Author :
Zq Song,
Java Developer








































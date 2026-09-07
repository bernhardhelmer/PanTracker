# PanTracker

Your personal cosmetics inventory and challenge tracker designed to help users catalog cosmetic inventory and track progress within specific challenge constraints.

## Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot, Spring Data JPA
- **Database:** PostgreSQL (H2 for integration testing)
- **ORM:** Hibernate
- **Templating:** Thymeleaf
- **Testing:** JUnit 5, Mockito

## Highlights

- **RESTful backend** built with Spring Boot and Java 21, using a DTO pattern to isolate database entities from public API contracts
- **Relational PostgreSQL schema** modeled with Spring Data JPA derived queries
- **High code coverage** across isolated Mockito unit tests, controller-slice integration tests, and full transactional integration tests against an in-memory H2 database
- **Centralized global exception handler** mapping business, database, and Jakarta validation constraint violations to standardized, user-friendly JSON payloads and HTTP status codes

## Known Limitations
There is no authentification process at the moment because the focus is on CRUD-logic, DTO-pattern and test coverage. The next steps include user testing, frontend implementation and, when ready, Spring Security with password-hashing.
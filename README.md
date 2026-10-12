# ClubConnect

ClubConnect is a Spring Boot web application for managing college clubs, memberships, events, and campus announcements. It includes separate dashboards for students, club leads, and administrators.

## Features

- Student registration with administrator approval and profile management
- Club directory, membership applications, and club lead administration
- Event publishing and registration, QR-backed passes, and attendance scanning
- Campus and club announcements with image or attachment support
- Role-based access control and email notification support

## Technology

- Java 21 and Spring Boot 3
- Spring MVC, Spring Security, Spring Data JPA, and Thymeleaf
- MySQL for the default application configuration
- Maven for builds

## Requirements

- JDK 21
- Maven 3.9 or newer
- MySQL 8 or a compatible MySQL server

## Run locally

1. Create a MySQL database and a local application user. For example, in the MySQL client:

   ```sql
   CREATE DATABASE vpm_clubconnect CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'clubconnect_app'@'localhost' IDENTIFIED BY 'replace-this-password';
   GRANT ALL PRIVILEGES ON vpm_clubconnect.* TO 'clubconnect_app'@'localhost';
   ```

2. Copy `.env.example` to `.env` and set `MYSQL_PASSWORD` to the password you chose. Keep `.env` private; it is excluded from Git.

   ```powershell
   Copy-Item .env.example .env
   ```

3. Start the application:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   On macOS or Linux, run `./mvnw spring-boot:run` instead.

4. Open [http://localhost:8080](http://localhost:8080).

The first local run seeds demonstration accounts and sample content. Those credentials are for local demonstration only. Do not expose a seeded instance to the public internet. For a production deployment, disable seed data and provision an administrator through a secure process. Before publishing this repository publicly, verify that all sample names and contact details in the seed data are fictional or replace them with synthetic values.

Email is disabled by default. Set the `MAIL_*` values and `MAIL_ENABLED=true` in your local `.env` only if you want to configure a mail provider.

## Build

```powershell
.\mvnw.cmd --batch-mode verify
```

On macOS or Linux, run `./mvnw --batch-mode verify`.

The executable Spring Boot JAR is written to `target/clubconnect-1.0.0.jar`.

## Configuration

Application configuration lives in `src/main/resources/application.properties`. Local overrides are loaded from `.env`. Do not commit `.env`, database files, generated output, or real credentials.

The default database setting is MySQL. H2 is included as a runtime dependency for migration tooling, but the application is not configured to use the H2 console by default.

`H2ToMySqlMigrator` clears the matching tables in its destination MySQL database before copying data. Use it only with a disposable or backed-up destination.

## Project layout

```text
src/main/java/com/vpmcoe/clubconnect/
  config/       Application initialization
  controller/   Web routes and request handling
  migration/    Database and data migration utilities
  model/        JPA entities and domain enums
  repository/   Spring Data repositories
  security/     Authentication and authorization
  service/      Business logic
src/main/resources/
  templates/    Thymeleaf pages
  static/       CSS and image assets
```

## GitHub publishing checklist

- Keep `.env`, database files, IDE metadata, and `target/` out of the repository.
- Review the first commit for credentials, personal data, and local database contents.
- Confirm the sample people and contact details in `DataInitializer.java` are safe to publish.
- Add screenshots of the main pages if you want the repository page to serve as a portfolio entry.
- Choose and add a license before inviting reuse; no license is included yet.
- Configure repository secrets separately if you later add deployment or integration workflows.

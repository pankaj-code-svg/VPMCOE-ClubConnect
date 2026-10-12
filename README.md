# ClubConnect

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/pankaj-code-svg/VPMCOE-ClubConnect)

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

The first local run seeds demonstration accounts and sample content. The seeded names, emails, and phone numbers are synthetic. Demo passwords are intentionally simple and must never be used for a production database.

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

## Free public demo on Render

GitHub stores this repository, but GitHub Pages only serves static files and cannot run this Spring Boot server. For a free hosted demo, this repository includes a Render Blueprint and a `demo` profile that uses an in-memory H2 database instead of a paid MySQL service.

1. Open the **Deploy to Render** button above and sign in to Render if prompted.
2. Review the `clubconnect-demo` service and make sure its plan is **Free**, then apply the `render.yaml` configuration.
3. Wait for the first build to finish. Render will show the public `onrender.com` URL on the service page.

The demo starts with synthetic sample content and publicly documented demo accounts (`admin@example.invalid` / `admin123`, `techlead@example.invalid` / `tech123`, and `student@example.invalid` / `student123`). Anyone can change the demo data; it resets when the service restarts or sleeps. On Render Free, an idle app sleeps after 15 minutes and the first request after that can take about a minute to wake. The free service has no persistent disk, so do not use this setup for real accounts or real data. You do not need a paid MySQL database for this demo.

If Render asks you to add a payment method or select a paid plan, stop rather than confirm; the demo configuration is intended to run on the Free plan. See [Render's free-service limits](https://render.com/docs/free).

### Email on Render Free

Render Free web services block outbound SMTP traffic on ports 25, 465, and 587. The demo therefore uses an HTTPS email API provider instead of SMTP. To enable email through Resend, set these service environment variables in the Render Dashboard (never commit the API key):

- `MAIL_ENABLED=true`
- `MAIL_PROVIDER=resend`
- `RESEND_API_KEY` to your Resend API key
- `MAIL_FROM` to the sender address Resend allows for your account

Resend's `onboarding@resend.dev` sender is for testing. For normal delivery to students, configure and verify a sending domain you control with your email provider. Demo accounts use `.invalid` email addresses and cannot receive messages; use a real recipient only after configuring a verified sender. See [Render Free limitations](https://render.com/docs/free) and [Resend pricing](https://resend.com/pricing).

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

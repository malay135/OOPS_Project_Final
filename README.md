# I-ITSM — Intelligent IT Service Management (backend)

Spring Boot backend for the CS F213 OOP project. The website (React) is in the separate `itsm-frontend` repository.

Live version: https://iitsm-bits.netlify.app (the free server sleeps when unused; the first load can take about 2 minutes).

## What you need

- Java 21 and Maven 3.9 or newer
- PostgreSQL 14 or newer (tested with 16)
- Node.js 18 or newer (for the website)

## Run it on your computer

1. **Create the database once.** In pgAdmin (Query Tool) or `psql`, run:

   ```sql
   CREATE DATABASE itsm_db;
   ```

2. **Database login.** The app connects as user `postgres` with password `malay@sql`. If your PostgreSQL password is different, set yours before starting, in the same window:

   - Windows Command Prompt: `set DB_PASSWORD=yourpassword`
   - Windows PowerShell: `$env:DB_PASSWORD="yourpassword"`
   - Mac / Linux: `export DB_PASSWORD=yourpassword`

   (`DB_USERNAME` and `DB_URL` can be changed the same way.)

3. **Start the backend** in this folder:

   ```
   mvn spring-boot:run
   ```

   Wait for `Started IItsmApplication`. Check http://localhost:8080/api/health shows `{"status":"UP"}`.
   The tables are created automatically.

4. **Start the website** in the `itsm-frontend` folder, in a second window:

   ```
   npm install
   npm run dev
   ```

   Open http://localhost:5173 and use the demo buttons on the login page.

## Accounts and sample data

On an **empty** database the app fills in everything needed to try it straight away:

| Login | Password | Role | Who |
| --- | --- | --- | --- |
| admin | admin123 | Administrator | |
| employee | employee123 | Employee | reported the sample incidents |
| technician | tech123 | Technician | Ravi Kumar (HARDWARE) |
| priya | priya123 | Technician | Priya Sharma (NETWORK, SECURITY) |
| arjun | arjun123 | Technician | Arjun Mehta (SOFTWARE, DATABASE) |

Six sample incidents show every stage: closed (with feedback), resolved, in progress, acknowledged, new, and one marked as a possible duplicate. A new payment incident also shows a suggested fix from the closed one.

A database that already has data is left as it is. To start without sample data, set `SAMPLE_DATA=false` before step 3.

## Tests

```
mvn clean test
```

110 automated tests (JUnit 5 + Mockito). One of them starts the whole application, so PostgreSQL must be running with the database above. Close VS Code first: its Java extension rebuilds the same `target` folder and can make that test fail.

## Where things are

```
src/main/java/i_itsm/
  config/        security, default users, reference data, sample data
  controller/    REST endpoints (/api/...)
  model/         entities (Incident, Technician, User, SLAPolicy, ...)
  repository/    Spring Data repositories (Repository pattern)
  service/       business logic (priority, SLA, assignment, duplicates, suggestions, ...)
  strategy/      assignment strategies (Strategy pattern)
  state/         incident lifecycle states (State pattern)
  notification/  in-app and email notifications (Factory + Adapter patterns)
  exception/     custom exceptions and the error handler
src/test/java/i_itsm/   automated tests
```

`CHANGES_AND_API.md` lists the endpoints and what each change added.

## Cloud

The live version runs on Render (this backend, built with the `Dockerfile`), Neon (PostgreSQL), Netlify (website) and Brevo (emails). Their settings are environment variables on Render, never in this repository.

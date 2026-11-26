# AI Helpdesk

A full-stack IT support ticket system. Employees report problems, and Google Gemini suggests a category, a likely cause and fix steps.

**Live demo:** https://ai-helpdesk-1-jxq7.onrender.com
(Hosted on a free plan. If the app has been idle, the first load can take up to 2 minutes.)

## Features

- Register and log in (passwords are hashed with BCrypt)
- Roles: users see only their own tickets, `admin` sees all tickets
- Create, close, delete and filter tickets, with Low/Medium/High priority
- AI analysis of each ticket (Gemini API), saved with the ticket
- Retry handling when the AI service is overloaded (HTTP 503)
- Ticket counts: total, open, closed

## Tech stack

| Area | Technology |
|---|---|
| Backend | Java, Spring Boot 4, Spring Web, Spring Security, Spring Data JPA / Hibernate |
| Database | MySQL (local), TiDB Cloud Serverless (live) |
| AI | Google Gemini API |
| Frontend | React (Vite), HTML, CSS, JavaScript |
| Testing | JUnit 5, Mockito (17 unit tests) |
| Deployment | Docker, Render |

## Run locally

Requirements: Java, MySQL, Node.js, and a Gemini API key.

1. Create the database:
   ```sql
   CREATE DATABASE ai_helpdesk CHARACTER SET utf8mb4;
   ```
2. Start the backend (PowerShell):
   ```
   $env:DB_PASSWORD="your-mysql-password"
   $env:GEMINI_API_KEY="your-gemini-key"
   $env:ADMIN_PASSWORD="choose-an-admin-password"
   $env:DDL_AUTO="update"
   .\mvnw.cmd spring-boot:run
   ```
3. Start the frontend in a second window:
   ```
   cd frontend
   npm install
   npm run dev
   ```
4. Open http://localhost:5173

## Run the tests

```
.\mvnw.cmd test
```

## Configuration

Secrets are read from environment variables and are never stored in the repository:
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GEMINI_API_KEY`, `ADMIN_PASSWORD`, `DDL_AUTO`.

## Known limitations

- CSRF protection is disabled and there is no password reset (learning project)
- AI answers are suggestions and can be wrong
- The free hosting plan sleeps when idle

## Author

Ajay Kumar
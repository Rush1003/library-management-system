# Library Management System

A full-stack, data-driven Library Management System built with **Spring Boot**,
**MongoDB** (NoSQL), and a **Java Swing** desktop client that consumes a RESTful API.

> Replace the placeholders below (`<GITHUB_REPO_URL>`, `<VIDEO_URL>`, student details)
> before submitting. See `docs/Project_Report.docx` for the full write-up.

- **GitHub Repository:** `<GITHUB_REPO_URL>`
- **Video Demonstration:** `<VIDEO_URL>`

---

## 1. Project Structure

```
library-management-system/
├── backend/                 # Spring Boot REST API (Maven project)
│   └── src/main/java/com/library/lms/
│       ├── model/            # Book, Member, Loan, LoanStatus (MongoDB documents)
│       ├── repository/       # Spring Data MongoDB repositories
│       ├── service/          # Business logic + fallback-wrapped DB access
│       ├── controller/       # REST controllers (CRUD + issue/return)
│       ├── exception/        # Custom exceptions + GlobalExceptionHandler
│       ├── dto/               # ErrorResponse DTO
│       └── config/           # CORS config, sample data seeder
├── gui/                     # Java Swing desktop client (Maven project)
│   └── src/main/java/com/library/gui/
│       ├── net/               # ApiClient (HTTP + retry/error handling), ApiException
│       ├── model/             # DTOs mirroring backend JSON
│       └── panels/            # BooksPanel, MembersPanel, LoansPanel
└── docs/
    ├── Project_Report.docx
    ├── diagrams/
    └── screenshots/
```

## 2. Technology Stack

| Layer          | Technology                                   |
|----------------|-----------------------------------------------|
| Backend        | Java 17, Spring Boot 3.3 (Web, Validation, Actuator) |
| Database       | MongoDB (NoSQL, document-oriented)            |
| API            | RESTful JSON API (`/api/books`, `/api/members`, `/api/loans`) |
| Desktop client | Java Swing (`java.net.http.HttpClient`, Jackson) |
| Build tool     | Maven                                         |
| Testing        | JUnit 5 + Mockito                             |

## 3. Prerequisites

- **JDK 17+**
- **Maven 3.8+**
- **MongoDB** running locally on `mongodb://localhost:27017` (Community Server,
  or a Docker container, or a free MongoDB Atlas cluster — see below)

### Option A — Local MongoDB
Install MongoDB Community Edition and start the service, e.g.:
```bash
# Ubuntu/Debian
sudo systemctl start mongod

# macOS (Homebrew)
brew services start mongodb-community
```

### Option B — Docker
```bash
docker run -d --name lms-mongo -p 27017:27017 mongo:7
```

### Option C — MongoDB Atlas (cloud, no local install)
Create a free cluster at https://www.mongodb.com/atlas, then update
`backend/src/main/resources/application.properties`:
```properties
spring.data.mongodb.uri=mongodb+srv://<username>:<password>@<cluster-url>/library_management_db?retryWrites=true&w=majority
```

## 4. Running the Backend (REST API)

```bash
cd backend
mvn spring-boot:run
```

The API starts on **http://localhost:8080**. On first run, `DataSeeder`
automatically inserts a handful of sample books and members so there is
data to explore immediately (it only seeds empty collections, so it is
safe to leave running).

Quick health check:
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/books
```

## 5. Running the Desktop GUI

Build a runnable jar (bundles Jackson, so no separate classpath setup is needed):
```bash
cd gui
mvn clean package
java -jar target/lms-gui.jar
```

By default the GUI connects to `http://localhost:8080`. To point it at a
different backend URL:
```bash
java -jar target/lms-gui.jar http://your-server:8080
# or
java -DapiUrl=http://your-server:8080 -jar target/lms-gui.jar
```

If the backend is not reachable, the GUI shows a clear error dialog
(rather than freezing or crashing) and lets you retry once the backend is
back up.

## 6. REST API Reference

| Method | Endpoint                              | Description                          |
|--------|----------------------------------------|---------------------------------------|
| GET    | `/api/books`                           | List all books                       |
| GET    | `/api/books/{id}`                      | Get a book by id                     |
| GET    | `/api/books/search?title=&author=&genre=` | Search books                     |
| POST   | `/api/books`                           | Create a book                        |
| PUT    | `/api/books/{id}`                      | Update a book                        |
| DELETE | `/api/books/{id}`                      | Delete a book                        |
| GET    | `/api/members`                         | List all members                     |
| GET    | `/api/members/{id}`                    | Get a member by id                   |
| POST   | `/api/members`                         | Create a member                      |
| PUT    | `/api/members/{id}`                    | Update a member                      |
| DELETE | `/api/members/{id}`                    | Delete a member                      |
| GET    | `/api/loans`                           | List all loans                       |
| GET    | `/api/loans/{id}`                      | Get a loan by id                     |
| GET    | `/api/loans/member/{memberId}`         | List a member's loans                |
| GET    | `/api/loans/overdue`                   | List overdue loans                   |
| POST   | `/api/loans/issue?bookId=&memberId=`   | Issue a book to a member             |
| PUT    | `/api/loans/{id}/return`               | Return a book, computes fine if late |

### Example requests

```bash
# Create a book
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{"isbn":"978-0596009205","title":"Head First Design Patterns","author":"Freeman & Robson","genre":"Programming","publishedYear":2004,"totalCopies":2,"availableCopies":2}'

# Issue a book
curl -X POST "http://localhost:8080/api/loans/issue?bookId=<BOOK_ID>&memberId=<MEMBER_ID>"

# Return a book
curl -X PUT http://localhost:8080/api/loans/<LOAN_ID>/return
```

## 7. Error Handling & Fallback Design (summary)

- All custom exceptions (`ResourceNotFoundException`, `DuplicateResourceException`,
  `BookNotAvailableException`, `InvalidRequestException`, `DatabaseOperationException`)
  are centrally translated by `GlobalExceptionHandler` into a consistent JSON
  `ErrorResponse` with the correct HTTP status code — no raw stack traces are
  ever returned to a client.
- Every service method routes its MongoDB calls through a `safeCall`/`safeRun`
  wrapper that catches `DataAccessException` and rethrows it as a
  `DatabaseOperationException` (HTTP 503), so a database outage degrades
  gracefully instead of crashing the request.
- The Swing GUI's `ApiClient` retries once on a transient connection failure,
  applies request timeouts so the UI never hangs, and always shows the
  server's actual error message to the user via a dialog.
- See `docs/Project_Report.docx`, section "Challenges and Solutions", for a
  detailed discussion with code excerpts.

## 8. Running Tests

```bash
cd backend
mvn test
```

## 9. Author

Fill in before submission:
- **Name:**
- **Student ID:**
- **Course / Module:**
- **Submission date:**

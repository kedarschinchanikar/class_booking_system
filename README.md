# Global Class Offering Booking System

A production-ready Spring Boot backend service for a global live-learning platform where teachers conduct online classes for students across different countries and timezones. Supports course offerings with multiple sessions, timezone-aware scheduling, and conflict-free booking with proper concurrency handling.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Tech Stack](#tech-stack)
3. [Setup Instructions](#setup-instructions)
4. [Environment Variables](#environment-variables)
5. [Database Schema Overview](#database-schema-overview)
6. [API Documentation](#api-documentation)
7. [Timezone Handling Approach](#timezone-handling-approach)
8. [Concurrency Handling Approach](#concurrency-handling-approach)
9. [Assumptions Made](#assumptions-made)
10. [Steps to Run Locally](#steps-to-run-locally)

---

## Project Overview

This system enables:
- **Teachers** to create course offerings and add session schedules in their local timezone
- **Parents/Students** to browse available offerings, view session times in their own timezone, and book offerings
- **Conflict detection** — prevents double-booking when sessions overlap
- **Concurrency safety** — handles simultaneous booking attempts without data corruption

### Core Entities
- **Course** — A subject (e.g., Python Coding, Art Drawing)
- **Offering** — A schedulable section of a course (e.g., Saturday Batch)
- **Session** — An individual meeting time within an offering
- **Booking** — A parent's enrollment in an offering (covers all sessions)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.3.0 |
| ORM | Spring Data JPA / Hibernate 6.5 |
| Database | PostgreSQL 16 |
| Validation | Jakarta Bean Validation |
| Build Tool | Maven 3.8+ |
| Utilities | Lombok |

---

## Setup Instructions

### Prerequisites

- **Java 17+** (JDK)
- **Maven 3.8+** (or use the included Maven wrapper)
- **PostgreSQL 14+** (tested on PostgreSQL 16)

## Database Setup

1. Install and start PostgreSQL.

2. Create the database:
   ```sql
   CREATE DATABASE booking_system;
   ```

3. Update `src/main/resources/application.yml` if your credentials differ from defaults.

### Build & Run

```bash
cd booking-system

# Build the project
mvn clean package -DskipTests

# Run the application
mvn spring-boot:run
```

The server starts on **http://localhost:8080**.

On first startup, the schema and seed data (teachers, parents, courses) are created automatically.

---

## Environment Variables

The application is configured via `src/main/resources/application.yml`:

| Variable | Default | Description |
|----------|---------|-------------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/booking_system` | PostgreSQL JDBC URL |
| `spring.datasource.username` | `postgres` | Database username |
| `spring.datasource.password` | `postgres` | Database password |
| `server.port` | `8080` | Application port |

You can override via environment variables:
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/booking_system
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=yourpassword
export SERVER_PORT=8080
```
---

## Database Schema Overview

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│   teachers   │       │   courses    │       │   parents    │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ id (PK)      │       │ id (PK)      │       │ id (PK)      │
│ name         │       │ name         │       │ name         │
│ email (UQ)   │       │ description  │       │ email (UQ)   │
│ timezone     │       │ created_at   │       │ timezone     │
│ created_at   │       └──────────────┘       │ created_at   │
└──────┬───────┘              │               └──────┬───────┘
       │                      │                      │
       │         ┌────────────┴────────────┐         │
       │         │        offerings        │         │
       │         ├─────────────────────────┤         │
       └────────►│ id (PK)                 │         │
                 │ course_id (FK→courses)   │         │
                 │ teacher_id (FK→teachers) │         │
                 │ name                    │         │
                 │ max_capacity            │         │
                 │ current_enrollment      │         │
                 │ version (optimistic)    │         │
                 │ created_at              │         │
                 └────────────┬────────────┘         │
                              │                      │
              ┌───────────────┼───────────────┐      │
              │                               │      │
   ┌──────────┴──────────┐       ┌───────────┴──────┴───────┐
   │      sessions       │       │        bookings          │
   ├─────────────────────┤       ├──────────────────────────┤
   │ id (PK)             │       │ id (PK)                  │
   │ offering_id (FK)    │       │ parent_id (FK→parents)   │
   │ start_time (UTC)    │       │ offering_id (FK→offerings)│
   │ end_time (UTC)      │       │ booked_at                │
   │ created_at          │       │ UQ(parent_id,offering_id)│
   └─────────────────────┘       └──────────────────────────┘
```

### Key Constraints
- `sessions.end_time > sessions.start_time` — CHECK constraint
- `bookings(parent_id, offering_id)` — UNIQUE constraint prevents duplicate bookings
- All timestamps stored as `TIMESTAMP WITH TIME ZONE` (UTC internally)
- Indexes on `sessions(start_time, end_time)` for efficient conflict queries
- Indexes on `bookings(parent_id)` and `offerings(teacher_id)` for fast lookups

---

## Seed Data (Auto-created on Startup)

| Entity   | ID | Name             | Timezone            |
|----------|----|------------------|---------------------|
| Teacher  | 1  | Alice Teacher    | America/New_York    |
| Teacher  | 2  | Bob Teacher      | Asia/Kolkata        |
| Parent   | 1  | Charlie Parent   | America/Los_Angeles |
| Parent   | 2  | Diana Parent     | Europe/London       |
| Course   | 1  | Minecraft Coding | -                   |
| Course   | 2  | Art Drawing Class| -                   |
| Course   | 3  | Python Programming| -                  |

---

## API Documentation

A Postman collection is included in the repository: `postman/Booking_System_API.postman_collection.json`

Import it into Postman to test all endpoints with pre-configured requests.

### API Reference

### Teacher APIs

#### 1. Create Offering
```
POST /api/teacher/offerings
Content-Type: application/json

{
  "courseId": 1,
  "teacherId": 1,
  "name": "Saturday Batch",
  "maxCapacity": 20
}
```

#### 2. Add Session to Offering
Times are specified in the teacher's local timezone (auto-converted to UTC for storage).
```
POST /api/teacher/sessions
Content-Type: application/json

{
  "offeringId": 1,
  "startTime": "2025-06-07T17:00:00",
  "endTime": "2025-06-07T18:00:00",
  "timezone": "America/New_York"
}
```
If `timezone` is omitted, the teacher's profile timezone is used.

#### 3. Get Teacher's Offerings
```
GET /api/teacher/1/offerings
```
Returns offerings with sessions displayed in the teacher's timezone.

---

### Parent APIs

#### 4. Get Available Offerings
```
GET /api/parent/offerings?timezone=America/Los_Angeles
```
Session times are converted to the specified timezone. Defaults to UTC.

#### 5. Book an Offering
```
POST /api/parent/bookings
Content-Type: application/json

{
  "parentId": 1,
  "offeringId": 1
}
```
**Booking rules enforced:**
- Books all sessions of the offering at once
- Rejects if offering is at max capacity
- Rejects if any session overlaps with the parent's existing bookings
- Handles concurrent booking attempts safely (pessimistic locking)

#### 6. Get Parent's Bookings
```
GET /api/parent/1/bookings
```
Returns all booked offerings with sessions in the parent's timezone.

---

## Example Test Flow (curl)

```bash
# Step 1: Create an offering
curl -X POST http://localhost:8080/api/teacher/offerings \
  -H "Content-Type: application/json" \
  -d '{"courseId":1,"teacherId":1,"name":"Saturday Batch","maxCapacity":20}'

# Step 2: Add sessions (teacher's timezone: America/New_York)
curl -X POST http://localhost:8080/api/teacher/sessions \
  -H "Content-Type: application/json" \
  -d '{"offeringId":1,"startTime":"2025-06-07T17:00:00","endTime":"2025-06-07T18:00:00","timezone":"America/New_York"}'

curl -X POST http://localhost:8080/api/teacher/sessions \
  -H "Content-Type: application/json" \
  -d '{"offeringId":1,"startTime":"2025-06-14T17:00:00","endTime":"2025-06-14T18:00:00","timezone":"America/New_York"}'

curl -X POST http://localhost:8080/api/teacher/sessions \
  -H "Content-Type: application/json" \
  -d '{"offeringId":1,"startTime":"2025-06-21T17:00:00","endTime":"2025-06-21T18:00:00","timezone":"America/New_York"}'

# Step 3: View teacher's offerings
curl http://localhost:8080/api/teacher/1/offerings

# Step 4: Parent views available offerings (in LA timezone)
curl "http://localhost:8080/api/parent/offerings?timezone=America/Los_Angeles"

# Step 5: Parent books the offering
curl -X POST http://localhost:8080/api/parent/bookings \
  -H "Content-Type: application/json" \
  -d '{"parentId":1,"offeringId":1}'

# Step 6: View parent's bookings
curl http://localhost:8080/api/parent/1/bookings

# Step 7: Create a conflicting offering and try to book (should fail)
curl -X POST http://localhost:8080/api/teacher/offerings \
  -H "Content-Type: application/json" \
  -d '{"courseId":2,"teacherId":2,"name":"Art Weekend","maxCapacity":15}'

curl -X POST http://localhost:8080/api/teacher/sessions \
  -H "Content-Type: application/json" \
  -d '{"offeringId":2,"startTime":"2025-06-14T17:30:00","endTime":"2025-06-14T18:30:00","timezone":"America/New_York"}'

# This should return 409 Conflict (overlapping session)
curl -X POST http://localhost:8080/api/parent/bookings \
  -H "Content-Type: application/json" \
  -d '{"parentId":1,"offeringId":2}'
```

---

## Timezone Handling Approach

| Step | Description |
|------|-------------|
| **Storage** | All times stored as UTC (`TIMESTAMP WITH TIME ZONE`) in PostgreSQL |
| **Teacher Input** | Teachers provide session times in their local timezone. The system converts to UTC before storing |
| **Parent View** | Parents pass their timezone as a query parameter. The system converts UTC → parent's timezone in the response |
| **Conflict Detection** | All comparisons happen in UTC — no timezone ambiguity |
| **DST Safety** | Using IANA timezone IDs (e.g., `America/New_York`) with Java's `ZoneId` handles daylight saving transitions correctly |

### Flow Example
```
Teacher (New York) creates session: 2025-06-07T17:00:00 ET
  → Stored as: 2025-06-07T21:00:00Z (UTC)

Parent (Los Angeles) views the same session:
  → Displayed as: 2025-06-07T14:00:00 PT
```

---

## Concurrency Handling Approach

The system handles simultaneous booking attempts using a multi-layered strategy:

### 1. Pessimistic Locking (Primary)
```sql
SELECT * FROM offerings WHERE id = ? FOR UPDATE
```
- Acquires a row-level exclusive lock on the offering during booking
- Serializes all concurrent bookings for the same offering
- Prevents race conditions in capacity checks and enrollment updates

### 2. Database Unique Constraint (Safety Net)
```sql
UNIQUE (parent_id, offering_id)
```
- Even if application-level checks are bypassed due to race conditions, the DB rejects duplicate bookings

### 3. Optimistic Locking (Secondary)
```java
@Version
private Long version;
```
- Hibernate `@Version` field on the Offering entity
- Detects concurrent modifications if pessimistic lock is somehow bypassed

### 4. Transactional Consistency
- The entire booking flow (lock → validate → conflict check → enroll → save) runs in a single `@Transactional` block
- Ensures atomicity — either the full booking succeeds or nothing changes

### Concurrent Scenario Handling
| Scenario | How It's Handled |
|----------|-----------------|
| Two parents booking same offering simultaneously | Pessimistic lock serializes them; second waits for first to commit |
| Same parent attempting overlapping bookings | Time-conflict query prevents it; unique constraint as backup |
| Offering capacity reached during concurrent requests | Capacity checked inside the lock; second request sees updated count |

---

## Conflict Detection Logic

- Before booking, each session of the target offering is checked against all sessions from the parent's existing bookings
- Overlap formula: `existingStart < newEnd AND existingEnd > newStart`
- Runs within the same transaction as the booking for consistency

---

## Assumptions Made

1. **Authentication is out of scope** — Teacher/Parent IDs are passed directly in requests. In production, these would come from JWT tokens.
2. **Booking is at offering level** — Parents book all sessions of an offering together (cannot cherry-pick individual sessions).
3. **No cancellation flow** — Once booked, there's no cancel/refund endpoint (can be added later).
4. **Session times are teacher's responsibility** — The system doesn't validate that a teacher's own sessions don't overlap within an offering.
5. **Capacity is per offering** — All sessions in an offering share the same enrollment count.
6. **Seed data** — Teachers, parents, and courses are pre-seeded for testing convenience.
7. **Single instance deployment** — Pessimistic locking works correctly for a single DB instance. For multi-instance, distributed locks would be needed.
8. **Time format** — All input times follow ISO-8601 local datetime format (`yyyy-MM-ddTHH:mm:ss`) without offset. The timezone is specified separately.

---

## Steps to Run Locally

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd booking-system
   ```

2. **Ensure PostgreSQL is running** on port 5432

3. **Create the database**
   ```sql
   CREATE DATABASE booking_system;
   ```

4. **Update credentials** (if needed) in `src/main/resources/application.yml`

5. **Build and run**
   ```bash
   mvn clean package -DskipTests
   mvn spring-boot:run
   ```

6. **Verify** — Application starts on http://localhost:8080

7. **Test with Postman** — Import `postman/Booking_System_API.postman_collection.json`

---

## Docker Setup (Optional)

Run the entire application with PostgreSQL using Docker Compose — no local DB setup needed.

### Prerequisites
- Docker & Docker Compose installed

### Run with Docker

```bash
# Start both PostgreSQL and the application
docker-compose up --build

# Run in detached mode
docker-compose up --build -d

# View logs
docker-compose logs -f app

# Stop everything
docker-compose down

# Stop and remove data volumes
docker-compose down -v
```

The app will be available at **http://localhost:8080** once both containers are healthy.

### What's Included
- `Dockerfile` — Multi-stage build (build with JDK, run with JRE Alpine for small image)
- `docker-compose.yml` — PostgreSQL 16 + Spring Boot app with health checks
- `.dockerignore` — Excludes build artifacts from Docker context 
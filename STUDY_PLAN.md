# 📅 Smart Campus API — 5-Day Development Plan
**Module:** 5COSC022W — Client-Server Architectures  
**Due:** 24 April 2026, 13:00  
**Stack:** JAX-RS + Jersey + Apache Tomcat (WAR deployment) + Postman testing  
**No DB** — pure `HashMap` / `ArrayList` in-memory storage only.

---

## 🗓️ Overview at a Glance

| Day | Date | Focus | Coursework Parts |
|-----|------|-------|-----------------|
| 1 | Apr 19 (Sat) | Maven Project + Tomcat + Base Wiring | Part 1 |
| 2 | Apr 20 (Sun) | Room + Sensor Resources | Part 2 + Part 3 |
| 3 | Apr 21 (Mon) | Sub-Resources + Readings | Part 4 |
| 4 | Apr 22 (Tue) | Error Handling + Logging Filters | Part 5 |
| 5 | Apr 23 (Wed) | Polish, README, curl examples, Postman video prep | Submission prep |

> **Apr 24 morning** → Final review + BlackBoard submit by 13:00.

---

## 📆 Day 1 — Saturday 19 April
### Goal: Bootstrap the Maven project, configure Jersey on Tomcat, and verify the server starts.

### Tasks
- [ ] Create a **Maven Web Application** project inside `SmartCampusAPI/`
  - Use `maven-archetype-webapp` or set up manually
  - Packaging: `war`
- [ ] Add dependencies to `pom.xml`
  - `jersey-container-servlet` (Jersey 2.x)
  - `jersey-media-json-jackson` (JSON binding)
  - `jakarta.ws.rs-api` (JAX-RS API)
  - Set `<packaging>war</packaging>` and configure `maven-war-plugin`
- [ ] Create `web.xml` (or use `@ApplicationPath` annotation approach)
  - Register Jersey's `ServletContainer` as the servlet
- [ ] Create `SmartCampusApplication.java`
  - Extends `javax.ws.rs.core.Application`
  - Annotated with `@ApplicationPath("/api/v1")`
- [ ] Create **3 POJO model classes** with proper encapsulation:
  - `Room.java` — id, name, capacity, sensorIds
  - `Sensor.java` — id, type, status, currentValue, roomId
  - `SensorReading.java` — id, timestamp, value
- [ ] Create **`DataStore.java`** — a singleton holding:
  - `Map<String, Room> rooms`
  - `Map<String, Sensor> sensors`
  - `Map<String, List<SensorReading>> sensorReadings`
- [ ] Implement the **Discovery Endpoint** `GET /api/v1`
  - Returns JSON with: version, contact email, links map (`rooms`, `sensors`)
- [ ] Deploy to local Tomcat, hit `GET /api/v1` with Postman — confirm JSON response.
- [ ] **Git commit:** `"Day 1: Project bootstrap, POJOs, DataStore, discovery endpoint"`

### Deliverable Check ✅
- Server starts without errors
- `GET http://localhost:8080/SmartCampusAPI/api/v1` returns valid JSON

---

## 📆 Day 2 — Sunday 20 April
### Goal: Full Room CRUD + Sensor collection endpoints.

### Tasks — Part 2: Room Management
- [ ] Create `RoomResource.java` mapped to `/api/v1/rooms`
  - `GET /` → Return all rooms as JSON list
  - `POST /` → Create a new room; return `201 Created` with Location header
  - `GET /{roomId}` → Return a single room or `404`
  - `DELETE /{roomId}` → Delete room; block if sensors exist (throw `RoomNotEmptyException` — implement stub for now)
- [ ] Add seed data to `DataStore` for easy testing

### Tasks — Part 3: Sensor Operations
- [ ] Create `SensorResource.java` mapped to `/api/v1/sensors`
  - `POST /` → Register sensor; validate `roomId` exists → throw `LinkedResourceNotFoundException` if not (stub)
    - On success, add sensor ID to the parent `Room.sensorIds` list
  - `GET /` → Return all sensors; support optional `?type=CO2` query param filtering (`@QueryParam`)
  - `GET /{sensorId}` → Return a single sensor or `404`
  - `DELETE /{sensorId}` → Remove sensor; also remove its ID from the parent Room's `sensorIds` list
- [ ] Test all endpoints in Postman — create a basic Postman collection

### Git Commits (do both during the day)
- `"Day 2a: Room resource - GET, POST, GET by ID, DELETE with guard stub"`
- `"Day 2b: Sensor resource - POST with roomId validation, GET with type filter, DELETE"`

### Deliverable Check ✅
- Create room → `201`
- Get all rooms → `200` with list
- Delete room that has sensors → currently may 500 (fix tomorrow with mapper)
- Create sensor with bad roomId → currently may 500 (fix tomorrow)
- `GET /sensors?type=Temperature` → filtered list

---

## 📆 Day 3 — Monday 21 April
### Goal: Sub-resource locator pattern + SensorReadings history.

### Tasks — Part 4: Sub-Resources
- [ ] Create `SensorReadingResource.java` — NOT registered as a root resource, only used via locator
  - Constructor accepts `sensorId` and the shared `DataStore`
  - `GET /` → Return all readings for this sensor
  - `POST /` → Append a new reading
    - Validate sensor exists
    - Check sensor status — if `"MAINTENANCE"`, throw `SensorUnavailableException` (stub)
    - Generate UUID for reading ID, set timestamp to `System.currentTimeMillis()`
    - **Side effect:** Update `sensor.currentValue` with the new reading's value
    - Return `201 Created`
- [ ] In `SensorResource.java`, add the **Sub-Resource Locator** method:
  ```java
  @Path("{sensorId}/readings")
  public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
      return new SensorReadingResource(sensorId);
  }
  ```
  (No HTTP method annotation — this is the locator pattern)
- [ ] Test in Postman:
  - `POST /api/v1/sensors/{id}/readings` → adds reading, updates currentValue
  - `GET /api/v1/sensors/{id}/readings` → shows history
  - `POST` on a MAINTENANCE sensor → 500 for now (fix tomorrow)

### Git Commit
- `"Day 3: Sub-resource locator pattern, SensorReadingResource, currentValue side-effect"`

### Deliverable Check ✅
- Full nested path works: `/api/v1/sensors/{id}/readings`
- Parent sensor's `currentValue` is updated after POST reading
- Reading history is stored per sensor

---

## 📆 Day 4 — Tuesday 22 April
### Goal: All 5 error handling scenarios + request/response logging filter.

### Tasks — Part 5: Error Handling & Logging

#### Custom Exceptions (create each as a plain Java class)
- [ ] `RoomNotEmptyException.java`
- [ ] `LinkedResourceNotFoundException.java`
- [ ] `SensorUnavailableException.java`

#### Exception Mappers (each implements `ExceptionMapper<T>`)
- [ ] `RoomNotEmptyExceptionMapper` → **409 Conflict** + JSON body
- [ ] `LinkedResourceNotFoundExceptionMapper` → **422 Unprocessable Entity** + JSON body
- [ ] `SensorUnavailableExceptionMapper` → **403 Forbidden** + JSON body
- [ ] `NotFoundExceptionMapper` → **404 Not Found** + JSON body (for `NotFoundException`)
- [ ] `GlobalExceptionMapper` → **500 Internal Server Error** — catches `Throwable`, logs the exception, returns safe generic message

#### Wire Exceptions into Resources
- [ ] `RoomResource.DELETE` → throw `RoomNotEmptyException` when room has sensors
- [ ] `SensorResource.POST` → throw `LinkedResourceNotFoundException` when roomId missing
- [ ] `SensorReadingResource.POST` → throw `SensorUnavailableException` when status is `MAINTENANCE`

#### Logging Filter
- [ ] Create `ApiLoggingFilter.java` implementing both:
  - `ContainerRequestFilter` → log `[REQUEST] METHOD URI`
  - `ContainerResponseFilter` → log `[RESPONSE] STATUS_CODE`
  - Annotate with `@Provider`
  - Use `java.util.logging.Logger`

#### Register everything in `SmartCampusApplication.java`
- [ ] Add all mappers + filter to `getSingletons()` or `getClasses()` set

### Git Commits
- `"Day 4a: Custom exceptions and exception mappers (409, 422, 403, 500)"`
- `"Day 4b: ApiLoggingFilter - request/response logging with java.util.logging"`

### Deliverable Check ✅
| Scenario | Expected HTTP |
|----------|--------------|
| Delete room with sensors | `409 Conflict` |
| Create sensor, bad roomId | `422 Unprocessable Entity` |
| POST reading to MAINTENANCE sensor | `403 Forbidden` |
| Any unexpected runtime error | `500 Internal Server Error` (no stack trace) |
| Every request | Logged in Tomcat console |

---

## 📆 Day 5 — Wednesday 23 April
### Goal: Full polish, README, curl examples, Postman collection, and video prep.

### Tasks — Documentation & Submission
- [ ] **Write `README.md`** (required in the repo) with:
  - API Overview (resource hierarchy diagram in text/ASCII)
  - Build instructions: `mvn clean package` → deploy WAR to Tomcat
  - Server start instructions (Tomcat startup)
  - Base URL: `http://localhost:8080/api/v1` (WAR is named `ROOT.war` — set `<finalName>ROOT</finalName>` in `pom.xml`)
  - **At least 5 `curl` examples** covering:
    1. `GET /api/v1` — Discovery
    2. `POST /api/v1/rooms` — Create room
    3. `POST /api/v1/sensors` — Register sensor
    4. `GET /api/v1/sensors?type=CO2` — Filtered sensor list
    5. `POST /api/v1/sensors/{id}/readings` — Add reading
    6. `DELETE /api/v1/rooms/{id}` (with sensors) — Show 409 error
  - Answers to **all report questions** from each Part (inline in README)

- [ ] **Finalize Postman Collection**
  - Organize requests by folder: Discovery / Rooms / Sensors / Readings / Error Cases
  - Add example responses/assertions
  - Export as JSON and add to repo

- [ ] **Full end-to-end smoke test** — run through every endpoint manually
- [ ] Fix any remaining bugs found during testing
- [ ] **Git commit:** `"Day 5: README, curl examples, Postman collection, report questions"`

### Tasks — Video Preparation
- [ ] Plan your 10-minute video script (roughly 2 min per part):
  - Intro: show repo + README
  - Part 1: Hit discovery endpoint
  - Part 2: Room CRUD (including 409 error)
  - Part 3: Sensor create, filter by type
  - Part 4: Sub-resource readings, show currentValue update
  - Part 5: All error scenarios, show Tomcat logs for filter
- [ ] Record video (camera + mic on, face visible)
- [ ] Upload video to BlackBoard submission link

### Final Git Commit
- `"Final submission: clean build, all endpoints working, README complete"`

---

## 🏗️ Project Structure (Target)

```
SmartCampusAPI/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/smartcampus/
│       │       ├── SmartCampusApplication.java   ← @ApplicationPath
│       │       ├── model/
│       │       │   ├── Room.java
│       │       │   ├── Sensor.java
│       │       │   └── SensorReading.java
│       │       ├── store/
│       │       │   └── DataStore.java            ← singleton, HashMaps
│       │       ├── resource/
│       │       │   ├── DiscoveryResource.java    ← GET /api/v1
│       │       │   ├── RoomResource.java         ← /api/v1/rooms
│       │       │   ├── SensorResource.java       ← /api/v1/sensors
│       │       │   └── SensorReadingResource.java ← sub-resource (no @Path root)
│       │       ├── exception/
│       │       │   ├── RoomNotEmptyException.java
│       │       │   ├── LinkedResourceNotFoundException.java
│       │       │   └── SensorUnavailableException.java
│       │       ├── mapper/
│       │       │   ├── RoomNotEmptyExceptionMapper.java    ← 409
│       │       │   ├── LinkedResourceNotFoundMapper.java   ← 422
│       │       │   ├── SensorUnavailableMapper.java        ← 403
│       │       │   ├── NotFoundExceptionMapper.java        ← 404
│       │       │   └── GlobalExceptionMapper.java          ← 500
│       │       └── filter/
│       │           └── ApiLoggingFilter.java               ← logging
│       ├── resources/
│       └── webapp/
│           └── WEB-INF/
│               └── web.xml
├── pom.xml
├── README.md          ← report questions + curl commands
└── STUDY_PLAN.md      ← this file
```

---

## 📦 Key Dependencies (`pom.xml` snippets)

```xml
<!-- JAX-RS API -->
<dependency>
  <groupId>jakarta.ws.rs</groupId>
  <artifactId>jakarta.ws.rs-api</artifactId>
  <version>2.1.6</version>
</dependency>

<!-- Jersey (JAX-RS Implementation) -->
<dependency>
  <groupId>org.glassfish.jersey.containers</groupId>
  <artifactId>jersey-container-servlet</artifactId>
  <version>2.41</version>
</dependency>

<!-- JSON with Jackson -->
<dependency>
  <groupId>org.glassfish.jersey.media</groupId>
  <artifactId>jersey-media-json-jackson</artifactId>
  <version>2.41</version>
</dependency>

<!-- Jersey DI (required for Jersey 2.26+) -->
<dependency>
  <groupId>org.glassfish.jersey.inject</groupId>
  <artifactId>jersey-hk2</artifactId>
  <version>2.41</version>
</dependency>
```

---

## ⚠️ Rules to Remember
- ❌ No Spring Boot — JAX-RS / Jersey only
- ❌ No database — only `HashMap` and `ArrayList`
- ❌ No ZIP submission — GitHub repo link only
- ✅ `@ApplicationPath("/api/v1")` for versioning
- ✅ JSON responses on ALL endpoints (success and error)
- ✅ Proper HTTP status codes: 200, 201, 400, 403, 404, 409, 422, 500
- ✅ README must contain all report question answers
- ✅ Video max 10 min, face on camera, mic working

---

## 📝 Report Questions Checklist
- [ ] Part 1.1 — JAX-RS Resource lifecycle (request-scoped vs singleton)
- [ ] Part 1.2 — HATEOAS and hypermedia benefits
- [ ] Part 2.1 — Returning IDs vs full objects (bandwidth trade-offs)
- [ ] Part 2.2 — DELETE idempotency justification
- [ ] Part 3.1 — `@Consumes` and format mismatch consequences
- [ ] Part 3.2 — `@QueryParam` vs path segment for filtering
- [ ] Part 4.1 — Sub-Resource Locator architectural benefits
- [ ] Part 5.2 — HTTP 422 vs 404 semantics
- [ ] Part 5.4 — Security risks of exposing stack traces
- [ ] Part 5.5 — Why filters are better than manual logging

---

*Good luck! Commit at the end of each day to show progress history for the assessors.* 🚀

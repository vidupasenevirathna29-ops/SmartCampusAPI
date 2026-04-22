# Smart Campus API

**Module:** 5COSC022W — Client-Server Architectures  
**Student:** [Your Name / Student ID]  
**Due:** 24 April 2026, 13:00  
**Stack:** JAX-RS 2.1 · Jersey 2.41 · Apache Tomcat 9 · Jackson JSON  

---

## Table of Contents

1. [API Overview](#1-api-overview)  
2. [Resource Hierarchy](#2-resource-hierarchy)  
3. [Build & Deploy Instructions](#3-build--deploy-instructions)  
4. [curl Examples](#4-curl-examples)  
5. [Report Question Answers](#5-report-question-answers)  
6. [Error Reference](#6-error-reference)  

---

## 1. API Overview

Smart Campus API is a RESTful web service built with JAX-RS (Jersey) deployed on Apache Tomcat.  
It manages **campus rooms** and the **environmental sensors** installed in them, including a full reading history per sensor.

Key design constraints (per coursework spec):

| Constraint | Decision |
|---|---|
| No database | All data stored in in-memory `HashMap` / `ArrayList` via a singleton `DataStore` |
| No Spring Boot | Pure JAX-RS / Jersey 2.x on Tomcat servlet container |
| Versioned API | All endpoints under `/api/v1` via `@ApplicationPath` |
| JSON everywhere | Jackson via `jersey-media-json-jackson`; all responses (including errors) are `application/json` |

**Base URL:** `http://localhost:8080/api/v1`  
_(WAR is built as `ROOT.war` so Tomcat serves it at the context root `/`)_

---

## 2. Resource Hierarchy

```
GET /api/v1                                ← Discovery / HATEOAS root

/api/v1/rooms
  GET    /api/v1/rooms                     ← List all rooms
  POST   /api/v1/rooms                     ← Create a room  (201)
  GET    /api/v1/rooms/{roomId}            ← Get a room     (200 / 404)
  PUT    /api/v1/rooms/{roomId}            ← Update a room  (200 / 404)
  DELETE /api/v1/rooms/{roomId}            ← Delete a room  (204 / 404 / 409)

/api/v1/sensors
  POST   /api/v1/sensors                   ← Register a sensor (201 / 422)
  GET    /api/v1/sensors                   ← List all sensors; ?type= filter
  GET    /api/v1/sensors/{sensorId}        ← Get a sensor      (200 / 404)
  PUT    /api/v1/sensors/{sensorId}        ← Update a sensor   (200 / 404)
  DELETE /api/v1/sensors/{sensorId}        ← Delete a sensor   (204 / 404)

  /api/v1/sensors/{sensorId}/readings      ← Sub-resource (locator pattern)
    GET  /api/v1/sensors/{sensorId}/readings    ← Fetch reading history
    POST /api/v1/sensors/{sensorId}/readings    ← Append a reading (201 / 403 / 404)
```

### HTTP Status Code Summary

| Code | Meaning | When used |
|------|---------|-----------|
| 200 | OK | Successful GET / PUT |
| 201 | Created | Successful POST (includes `Location` header) |
| 204 | No Content | Successful DELETE |
| 400 | Bad Request | Missing required fields in body |
| 403 | Forbidden | POST reading to MAINTENANCE/OFFLINE sensor |
| 404 | Not Found | Resource does not exist |
| 409 | Conflict | DELETE room that still has sensors |
| 415 | Unsupported Media Type | Request `Content-Type` is not `application/json` |
| 422 | Unprocessable Entity | POST sensor with a `roomId` that doesn't exist |
| 500 | Internal Server Error | Unexpected runtime error |

---

## 3. Build & Deploy Instructions

### Prerequisites

| Tool | Version |
|------|---------|
| Java JDK | 11+ |
| Apache Maven | 3.8+ |
| Apache Tomcat | 9.x |

### Step 1 — Build the WAR

```bash
cd SmartCampusAPI
mvn clean package -DskipTests
```

The output WAR is at: `target/ROOT.war`

### Step 2 — Deploy to Tomcat

1. Stop Tomcat if running:
   ```bash
   # Windows
   %CATALINA_HOME%\bin\shutdown.bat

   # Linux / macOS
   $CATALINA_HOME/bin/shutdown.sh
   ```

2. Remove any existing ROOT deployment from Tomcat's `webapps/` directory:
   ```bash
   rm -rf %CATALINA_HOME%\webapps\ROOT*          # Windows
   rm -rf $CATALINA_HOME/webapps/ROOT*            # Linux / macOS
   ```

3. Copy the new WAR:
   ```bash
   copy target\ROOT.war %CATALINA_HOME%\webapps\  # Windows
   cp target/ROOT.war $CATALINA_HOME/webapps/      # Linux / macOS
   ```

4. Start Tomcat:
   ```bash
   %CATALINA_HOME%\bin\startup.bat   # Windows
   $CATALINA_HOME/bin/startup.sh     # Linux / macOS
   ```

5. Verify the server is up — check Tomcat logs for:
   ```
   INFO: Server startup in [N] ms
   ```

### Step 3 — Smoke-Test

```bash
curl http://localhost:8080/api/v1
```

Expected: HTTP 200 with JSON body containing `"name": "Smart Campus API"`.

---

## 4. curl Examples

> All examples assume `Content-Type: application/json` and the server is running at `http://localhost:8080`.

---

### 4.1 — Discovery Endpoint `GET /api/v1`

```bash
curl -s http://localhost:8080/api/v1
```

**Expected response (200 OK):**

```json
{
  "name": "Smart Campus API",
  "version": "1.0",
  "description": "RESTful API for managing campus rooms and environmental sensors.",
  "contact": "admin@smartcampus.ac.uk",
  "status": "operational",
  "_links": {
    "rooms":   "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

---

### 4.2 — Create a Room `POST /api/v1/rooms`

```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"name": "Lab A101", "capacity": 30}'
```

**Expected response (201 Created):**

```json
{
  "id": "a1b2c3d4-...",
  "name": "Lab A101",
  "capacity": 30,
  "sensorIds": []
}
```

The response also includes a `Location` header:
```
Location: http://localhost:8080/api/v1/rooms/a1b2c3d4-...
```

---

### 4.3 — Register a Sensor `POST /api/v1/sensors`

_(Replace `<ROOM_ID>` with an actual room ID from step 4.2)_

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type": "CO2", "roomId": "<ROOM_ID>"}'
```

**Expected response (201 Created):**

```json
{
  "id": "s1s2s3...",
  "type": "CO2",
  "status": "ACTIVE",
  "currentValue": 0.0,
  "roomId": "<ROOM_ID>"
}
```

---

### 4.4 — Filter Sensors by Type `GET /api/v1/sensors?type=CO2`

```bash
curl -s "http://localhost:8080/api/v1/sensors?type=CO2"
```

**Expected response (200 OK):** An array containing only sensors of type `CO2`.

```json
[
  {
    "id": "s1s2s3...",
    "type": "CO2",
    "status": "ACTIVE",
    "currentValue": 0.0,
    "roomId": "<ROOM_ID>"
  }
]
```

_(If no sensors match, returns an empty array `[]` — not a 404.)_

---

### 4.5 — Add a Sensor Reading `POST /api/v1/sensors/{sensorId}/readings`

_(Replace `<SENSOR_ID>` with an ID from step 4.3)_

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/<SENSOR_ID>/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 412.5}'
```

**Expected response (201 Created):**

```json
{
  "id": "r1r2r3...",
  "value": 412.5,
  "timestamp": 1745352761000
}
```

The parent sensor's `currentValue` is also updated to `412.5`.

---

### 4.6 — Delete Room with Sensors → 409 Conflict

_(Attempt to delete a room that still has sensors linked to it)_

```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/<ROOM_ID>
```

**Expected response (409 Conflict):**

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Room '<ROOM_ID>' cannot be deleted because it still has sensors. Remove all sensors first."
}
```

---

### 4.7 — Register Sensor with Invalid roomId → 422

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type": "Temperature", "roomId": "nonexistent-room-id"}'
```

**Expected response (422 Unprocessable Entity):**

```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Linked resource 'Room' with id 'nonexistent-room-id' was not found."
}
```

---

### 4.8 — Post Reading to MAINTENANCE Sensor → 403

First, set a sensor to MAINTENANCE status:

```bash
curl -s -X PUT http://localhost:8080/api/v1/sensors/<SENSOR_ID> \
  -H "Content-Type: application/json" \
  -d '{"status": "MAINTENANCE"}'
```

Then try to post a reading:

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/<SENSOR_ID>/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 99.9}'
```

**Expected response (403 Forbidden):**

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Sensor '<SENSOR_ID>' is currently MAINTENANCE and cannot accept readings."
}
```

---

## 5. Report Question Answers

---

### Part 1 — Project Bootstrap

#### Q1.1 — JAX-RS Resource Lifecycle: Request-Scoped vs Singleton

By default, JAX-RS creates a **new instance of each resource class for every HTTP request** (request-scoped). This means fields declared inside a resource class (e.g., `RoomResource`) are freshly initialised on every call — they are NOT shared between requests.

This is the correct design for stateless REST services because:

- Each request is fully isolated: no shared mutable state, no threading hazards.
- Resources can be safely annotated with `@Context`-injected objects (e.g., `UriInfo`) that are request-specific.

**Design consequence in this project:** All persistent state (rooms, sensors, readings) lives in the `DataStore` *singleton* (`DataStore.getInstance()`) rather than as fields inside `RoomResource` or `SensorResource`. The resource classes themselves are disposable per-request shells; `DataStore` is the single source of truth that survives across requests.

You can make a resource a true singleton by annotating it with `@Singleton`, but that requires thread-safety for all instance fields — adding unnecessary complexity for most use cases.

---

#### Q1.2 — HATEOAS and Hypermedia Benefits

**HATEOAS** (Hypermedia As The Engine Of Application State) is the REST constraint that API responses should include *links* to related or next-step resources, allowing clients to navigate the API dynamically rather than relying on hardcoded URLs.

**Implementation in this project:**  
The discovery endpoint (`GET /api/v1`) returns a `_links` object:

```json
{
  "_links": {
    "rooms":   "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

**Benefits:**

1. **Discoverability** — A new client only needs to know one URL (the root) and can discover all other endpoints from the response, reducing coupling to documentation.
2. **Evolvability** — If the `/rooms` path changes in v2, clients following links from the discovery response automatically pick up the new path without code changes.
3. **Self-documenting** — The API response itself describes what actions are possible and where, acting as a live contract.
4. **Reduced hardcoding** — Clients don't embed URLs; they follow `href` values from responses, making the API more maintainable and testable.

---

### Part 2 — Room Management

#### Q2.1 — Returning Full Objects vs Returning Just IDs

**Decision:** `GET /rooms` returns **full `Room` objects** (including `sensorIds`, `name`, `capacity`) rather than just a list of IDs.

**Trade-off analysis:**

| Approach | Bandwidth | Round-trips | Best for |
|----------|-----------|-------------|----------|
| Return full objects | Higher (larger payload) | 1 | Dashboard / list views that need all data |
| Return IDs only | Lower | 1 + N (one GET per ID) | Very large collections, lazy-loading UIs |

**Justification for full objects:** A campus management dashboard typically renders a full room list with names and capacities immediately. Returning only IDs would force the client into N additional requests (the "N+1 problem"), multiplying latency linearly with the number of rooms. For the expected scale of a campus system (tens to low hundreds of rooms), returning full objects is the pragmatic choice that minimises roundtrips.

---

#### Q2.2 — DELETE Idempotency

**RFC 9110 definition:** A method is *idempotent* if applying it multiple times produces the same server-side state as applying it once.

**DELETE is idempotent by specification.** Calling `DELETE /rooms/abc` twice should leave the server in the same state: room `abc` does not exist.

**Implementation in this project:**
- First call: room exists → deleted → server returns `204 No Content`.
- Second call: room doesn't exist → server returns `404 Not Found`.

The HTTP *response code* differs, but the **server state** is identical: the room is absent. This satisfies idempotency per the RFC. Returning `404` on the second call is acceptable (and preferable) because it gives the client honest feedback that the resource was already gone, without misrepresenting it as a new deletion.

---

### Part 3 — Sensor Operations

#### Q3.1 — `@Consumes` Annotation and Format Mismatch Consequences

`@Consumes(MediaType.APPLICATION_JSON)` declares the **acceptable `Content-Type`** for incoming request bodies. When Jersey sees this annotation:

1. **Match:** If the client sends `Content-Type: application/json` with a valid JSON body, Jersey deserialises the body via Jackson and passes it to the method.
2. **Mismatch:** If the client sends `Content-Type: text/plain` or `Content-Type: application/xml`, Jersey **rejects the request before the method body executes**, returning `415 Unsupported Media Type` automatically.

**Benefits of declaring `@Consumes` explicitly:**
- Prevents malformed or unexpected data from reaching business logic.
- Provides a clear contract to API consumers.
- Eliminates the need for manual content-type checking inside every method.
- Fails fast — the developer gets an immediate, unambiguous error rather than a mysterious NullPointerException later.

---

#### Q3.2 — `@QueryParam` vs Path Segment for Filtering

**Decision:** Sensor type filtering uses `GET /sensors?type=CO2` rather than `GET /sensors/CO2`.

**Justification:**

| Approach | Semantic meaning | Cacheability |
|----------|-----------------|--------------|
| `?type=CO2` (query param) | "Give me the sensor collection, narrowed to type CO2" | Better: base URI `/sensors` can be cached; params are separate |
| `/sensors/CO2` (path segment) | "CO2 is its own distinct resource" | Worse: conflates filtering with resource identity |

A path segment implies the thing it names is a **distinct, addressable resource** — `sensors/CO2` would suggest CO2 is a resource with its own identity and behaviour. Sensor type is a *filtering criterion*, not a resource. Query parameters are designed precisely for optional, refinement-style operations on a collection.

Additionally, query parameters are conventionally ignored by HTTP caches that key on the path alone, giving better cache-ability for the base collection.

---

### Part 4 — Sub-Resources

#### Q4.1 — Sub-Resource Locator Architectural Benefits

A **sub-resource locator** is a JAX-RS method with a `@Path` annotation but **no HTTP method annotation** (`@GET`, `@POST`, etc.). Instead of handling the request itself, it returns an object — the sub-resource — which Jersey then inspects for the matching HTTP method handler.

**In this project:**

```java
// In SensorResource.java — no @GET, @POST, etc.
@Path("{sensorId}/readings")
public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
    return new SensorReadingResource(sensorId);
}
```

**Architectural benefits:**

1. **Separation of concerns** — `SensorResource` handles sensor CRUD; `SensorReadingResource` handles reading history. Neither class is aware of the other's implementation details.
2. **Constructor-based context injection** — `SensorReadingResource` receives `sensorId` through its constructor, making it a plain Java object with no JAX-RS routing knowledge required.
3. **Lazy instantiation** — Jersey only creates `SensorReadingResource` when the `/readings` path is actually requested, saving resources for the common case.
4. **Testability** — `SensorReadingResource` can be unit-tested without a running JAX-RS container by simply constructing it with a test `sensorId`.
5. **Clean URL hierarchy** — The nested path `/sensors/{id}/readings` naturally expresses the parent-child relationship between sensors and readings.

---

### Part 5 — Error Handling & Logging

#### Q5.2 — HTTP 422 Unprocessable Entity vs 404 Not Found

Both 422 and 404 could, at first glance, be used when a referenced resource is missing. The distinction is semantic:

| Code | Meaning | When to use |
|------|---------|-------------|
| **404 Not Found** | The *requested resource* does not exist | The URL itself points to a non-existent resource (e.g., `GET /rooms/abc` where room abc doesn't exist) |
| **422 Unprocessable Entity** | The request is syntactically valid but semantically invalid | The body is well-formed JSON but references an entity that doesn't exist |

**Applied to `POST /sensors`:**  
The endpoint `/api/v1/sensors` *does* exist (404 would be wrong). The request body is valid JSON. The problem is that the `roomId` field inside the body refers to a room that doesn't exist — the request is semantically invalid. HTTP 422 precisely captures this: "I understand your request, but I cannot process it because the referenced resource is missing."

Using 404 here would be ambiguous — the client might think the `/sensors` endpoint itself was not found.

---

#### Q5.4 — Security Risks of Exposing Stack Traces

Returning raw Java stack traces in HTTP error responses poses several security risks:

1. **Information leakage** — Stack traces reveal internal class names, method signatures, library versions, and file paths. An attacker can use this to:
   - Identify the exact framework and version (enabling targeted exploit searches).
   - Map the internal package structure for further attacks.
   - Discover third-party library vulnerabilities by seeing exact dependency names.

2. **Sensitive data exposure** — Stack traces may inadvertently print variable values or SQL queries that were in the call stack, potentially exposing credentials, tokens, or query logic.

3. **Reconnaissance aid** — Knowing the line number of a crash helps an attacker refine their attack vector without needing source code access.

**Mitigation in this project:**  
`GlobalExceptionMapper` catches `Throwable`, **logs the full stack trace server-side** (visible only to system administrators in Tomcat logs), and returns a **safe, generic JSON response** to the client:

```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred. Please contact support."
}
```

The principle is: *log everything, expose nothing*.

---

#### Q5.5 — Why Filters Are Better Than Manual Logging

**Manual logging** (calling `Logger.info(...)` inside every resource method) has significant drawbacks:

| Problem | Detail |
|---------|--------|
| Code duplication | Every endpoint must repeat the same log statements |
| Incompleteness | Developers forget to add logging to some endpoints |
| Inconsistency | Different developers use different log formats |
| Maintenance burden | Changing the log format requires editing every resource class |
| Cross-cutting concern violation | Logging has nothing to do with business logic |

**Filter-based logging** (`ApiLoggingFilter` implementing `ContainerRequestFilter` + `ContainerResponseFilter`) solves all of these:

1. **Single point of implementation** — Registered once in `SmartCampusApplication`, it intercepts *every* request and response automatically.
2. **Consistent format** — One place controls the log format for the entire API.
3. **Zero boilerplate in resources** — Resource classes stay focused on business logic only.
4. **AOP principle** — Logging is a cross-cutting concern; filters implement it in the correct architectural layer.
5. **Toggleable** — Removing the filter from the `Application` class disables all logging without touching any resource code.

---

## 6. Error Reference

| Exception Class | Mapper | HTTP Code | Trigger |
|----------------|--------|-----------|---------|
| `RoomNotEmptyException` | `RoomNotEmptyExceptionMapper` | 409 Conflict | DELETE room that has sensors |
| `LinkedResourceNotFoundException` | `LinkedResourceNotFoundExceptionMapper` | 422 Unprocessable Entity | POST sensor with unknown `roomId` |
| `SensorUnavailableException` | `SensorUnavailableExceptionMapper` | 403 Forbidden | POST reading to MAINTENANCE/OFFLINE sensor |
| `NotFoundException` (JAX-RS) | `NotFoundExceptionMapper` | 404 Not Found | Any resource not found via JAX-RS routing |
| `Throwable` (catch-all) | `GlobalExceptionMapper` | 500 Internal Server Error | Any unexpected runtime exception |

All error responses follow the same JSON schema:

```json
{
  "status": <http-code>,
  "error": "<reason-phrase>",
  "message": "<human-readable description>"
}
```

---

## Project Structure

```
SmartCampusAPI/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/smartcampus/
│       │       ├── SmartCampusApplication.java   ← @ApplicationPath("/api/v1")
│       │       ├── model/
│       │       │   ├── Room.java
│       │       │   ├── Sensor.java
│       │       │   └── SensorReading.java
│       │       ├── store/
│       │       │   └── DataStore.java            ← Singleton, HashMaps
│       │       ├── resource/
│       │       │   ├── DiscoveryResource.java    ← GET /api/v1
│       │       │   ├── RoomResource.java         ← /api/v1/rooms
│       │       │   ├── SensorResource.java       ← /api/v1/sensors
│       │       │   └── SensorReadingResource.java ← Sub-resource (no @Path root)
│       │       ├── exception/
│       │       │   ├── RoomNotEmptyException.java
│       │       │   ├── LinkedResourceNotFoundException.java
│       │       │   └── SensorUnavailableException.java
│       │       ├── mapper/
│       │       │   ├── RoomNotEmptyExceptionMapper.java    ← 409
│       │       │   ├── LinkedResourceNotFoundExceptionMapper.java ← 422
│       │       │   ├── SensorUnavailableExceptionMapper.java     ← 403
│       │       │   ├── NotFoundExceptionMapper.java        ← 404
│       │       │   └── GlobalExceptionMapper.java          ← 500
│       │       └── filter/
│       │           └── ApiLoggingFilter.java     ← Request/response logging
│       └── webapp/
│           └── WEB-INF/
│               └── web.xml
├── pom.xml
├── README.md          ← This file
└── STUDY_PLAN.md
```

---

*Built for 5COSC022W Client-Server Architectures Coursework — April 2026*

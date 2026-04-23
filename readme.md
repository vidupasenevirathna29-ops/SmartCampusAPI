# Smart Campus API

## API Overview

Smart Campus API is a JAX-RS (Jersey 2.41) REST API deployed on Tomcat 9. It lets you manage campus **rooms** and the **sensors** inside them, and keeps a history of readings for each sensor. All data is kept in memory using `HashMap` and `ArrayList` inside a singleton `DataStore` — there's no database.

**Base URL:** `http://localhost:8080/api/v1`

The WAR is packaged as `ROOT.war` so Tomcat serves it at the context root `/`, making every endpoint available at `/api/v1/...`.

**Resource Hierarchy:**

```
GET /api/v1                                      ← Discovery root

/api/v1/rooms
  GET    /api/v1/rooms                           ← List all rooms
  POST   /api/v1/rooms                           ← Create a room (201)
  GET    /api/v1/rooms/{roomId}                  ← Get one room (200 / 404)
  PUT    /api/v1/rooms/{roomId}                  ← Update a room (200 / 404)
  DELETE /api/v1/rooms/{roomId}                  ← Delete a room (204 / 404 / 409)

/api/v1/sensors
  POST   /api/v1/sensors                         ← Register a sensor (201 / 422)
  GET    /api/v1/sensors                         ← List all; optional ?type= filter
  GET    /api/v1/sensors/{sensorId}              ← Get one sensor (200 / 404)
  PUT    /api/v1/sensors/{sensorId}              ← Update a sensor (200 / 404)
  DELETE /api/v1/sensors/{sensorId}              ← Delete a sensor (204 / 404)

  /api/v1/sensors/{sensorId}/readings            ← Sub-resource (locator pattern)
    GET  /api/v1/sensors/{sensorId}/readings     ← Fetch reading history
    POST /api/v1/sensors/{sensorId}/readings     ← Append a reading (201 / 403 / 404)
```

**HTTP Status Codes used:**

| Code | When used |
|------|-----------|
| 200 | Successful GET / PUT |
| 201 | Successful POST — includes `Location` header |
| 204 | Successful DELETE |
| 400 | Missing required fields in body |
| 403 | POST reading to MAINTENANCE / OFFLINE sensor |
| 404 | Resource does not exist |
| 409 | DELETE room that still has sensors |
| 415 | Request `Content-Type` is not `application/json` |
| 422 | POST sensor with a `roomId` that doesn't exist |
| 500 | Unexpected runtime error |

---

## Build and Launch Instructions

### Prerequisites

| Tool | Version |
|------|---------|
| Java JDK | 11 or higher |
| Apache Maven | 3.8 or higher |
| Apache Tomcat | 9.x (not 10+) |

### Step 1 — Clone the repository

```bash
git clone https://github.com/<your-username>/SmartCampusAPI.git
cd SmartCampusAPI
```

### Step 2 — Build the WAR file

```bash
mvn clean package -DskipTests
```

This compiles all Java source files and packages them into `target/ROOT.war`.

### Step 3 — Stop Tomcat (if already running)

```bash
# Windows
%CATALINA_HOME%\bin\shutdown.bat

# Linux / macOS
$CATALINA_HOME/bin/shutdown.sh
```

### Step 4 — Remove any existing ROOT deployment

```bash
# Windows
del /q %CATALINA_HOME%\webapps\ROOT.war
rmdir /s /q %CATALINA_HOME%\webapps\ROOT

# Linux / macOS
rm -rf $CATALINA_HOME/webapps/ROOT $CATALINA_HOME/webapps/ROOT.war
```

### Step 5 — Copy ROOT.war to Tomcat

```bash
# Windows
copy target\ROOT.war %CATALINA_HOME%\webapps\ROOT.war

# Linux / macOS
cp target/ROOT.war $CATALINA_HOME/webapps/ROOT.war
```

### Step 6 — Start Tomcat

```bash
# Windows
%CATALINA_HOME%\bin\startup.bat

# Linux / macOS
$CATALINA_HOME/bin/startup.sh
```

### Step 7 — Verify the server is running

Open Tomcat's log (`logs/catalina.out`) and look for:

```
INFO: Server startup in [N] ms
```

Then run a quick smoke test:

```bash
curl http://localhost:8080/api/v1
```

Expected: `200 OK` with a JSON body containing `"name": "Smart Campus API"`.

---

## Sample curl Commands

All commands assume the server is running at `http://localhost:8080`.

### 1 — GET discovery endpoint

```bash
curl -s http://localhost:8080/api/v1
```

**Expected 200 OK:**
```json
{
  "name": "Smart Campus API",
  "version": "1.0",
  "status": "operational",
  "_links": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

---

### 2 — POST create a room

```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"name": "Lab A101", "capacity": 30}'
```

**Expected 201 Created:**
```json
{
  "id": "a1b2c3d4-e5f6-...",
  "name": "Lab A101",
  "capacity": 30,
  "sensorIds": []
}
```

The response includes a `Location` header: `http://localhost:8080/api/v1/rooms/a1b2c3d4-...`

---

### 3 — POST register a sensor (linked to an existing room)

Using the seeded room `LIB-301` that exists on startup:

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type": "Humidity", "roomId": "LIB-301"}'
```

**Expected 201 Created:**
```json
{
  "id": "f7a8b9c0-...",
  "type": "Humidity",
  "status": "ACTIVE",
  "currentValue": 0.0,
  "roomId": "LIB-301"
}
```

---

### 4 — GET list sensors filtered by type

```bash
curl -s "http://localhost:8080/api/v1/sensors?type=Temperature"
```

**Expected 200 OK:** An array of all sensors whose type is `Temperature` (case-insensitive match).

```json
[
  {
    "id": "TEMP-001",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 22.5,
    "roomId": "LIB-301"
  },
  {
    "id": "TEMP-002",
    "type": "Temperature",
    "status": "MAINTENANCE",
    "currentValue": 0.0,
    "roomId": "LAB-202"
  }
]
```

---

### 5 — POST add a sensor reading

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 24.1}'
```

**Expected 201 Created:**
```json
{
  "id": "r1r2r3r4-...",
  "value": 24.1,
  "timestamp": 1745352761000
}
```

The parent sensor's `currentValue` is also updated to `24.1`.

---

### 6 — DELETE room with sensors → 409 Conflict

```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

**Expected 409 Conflict:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Room 'LIB-301' cannot be deleted because it still has sensors. Remove all sensors first."
}
```

---

### 7 — POST sensor with invalid roomId → 422

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type": "CO2", "roomId": "does-not-exist"}'
```

**Expected 422 Unprocessable Entity:**
```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Linked resource 'Room' with id 'does-not-exist' was not found."
}
```

---

### 8 — POST reading to MAINTENANCE sensor → 403

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-002/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 99.9}'
```

`TEMP-002` is seeded with status `MAINTENANCE`.

**Expected 403 Forbidden:**
```json
{
  "error": "Forbidden",
  "message": "Sensor 'TEMP-002' is currently MAINTENANCE and cannot accept readings.",
  "sensorId": "TEMP-002",
  "currentStatus": "MAINTENANCE"
}
```

---

## Report Question Answers

### Part 1 — Project Bootstrap

#### Q1.1 — JAX-RS Resource Lifecycle: Request-Scoped vs Singleton

By default, JAX-RS creates a **new instance of each resource class for every HTTP request** (request-scoped). This means any fields inside a class like `RoomResource` are reset on every call, so you can't store data there. It also means JAX-RS can safely inject request-specific things like `UriInfo` via `@Context` without thread-safety issues.

In this project, all the data — rooms, sensors, readings — is kept in the `DataStore` singleton (`DataStore.getInstance()`). The resource classes just handle the HTTP logic and don't hold any state themselves. You could annotate a resource with `@Singleton` to make it shared across requests, but then you'd have to make every field thread-safe, which adds unnecessary complexity for most cases.

---

#### Q1.2 — HATEOAS and Hypermedia Benefits

HATEOAS (Hypermedia As The Engine Of Application State) is a REST principle where responses include links to related resources, so clients can navigate the API without hardcoding URLs.

In this project, the discovery endpoint (`GET /api/v1`) returns a `_links` object with URLs to the rooms and sensors collections. The benefits are:

1. **Discoverability** — A client only needs to know the root URL and can find everything else from the response.
2. **Evolvability** — If a path changes in a future version, clients following the links will pick it up automatically without needing code changes.
3. **Self-documenting** — The response itself shows what's available and where, which makes the API easier to explore.
4. **Less hardcoding** — Clients use the href values from the response instead of building URLs themselves, which makes the integration easier to maintain.

---

### Part 2 — Room Management

#### Q2.1 — Returning Full Objects vs Returning Just IDs

`GET /rooms` returns **full `Room` objects** (including `name`, `capacity`, and `sensorIds`) rather than a list of IDs only.

Returning full objects uses more bandwidth per response, but it saves the client from having to make an extra request for each room just to get its details. If we only returned IDs, the client would need N follow-up requests to load all the room data — that gets expensive quickly. Since a campus system won't have thousands of rooms, returning the full list in one go is more practical. It's a bit more data, but it avoids a lot of unnecessary round-trips. For much larger collections, returning only IDs with pagination would make more sense, but for this use case returning full objects is the better choice.

---

#### Q2.2 — DELETE Idempotency

Idempotency means calling the same method multiple times should leave the server in the same state as calling it once. DELETE is idempotent — calling `DELETE /rooms/abc` twice results in the same outcome both times: room `abc` doesn't exist.

In this implementation, the first DELETE returns `204 No Content`. The second call finds nothing and returns `404 Not Found`. The response code is different, but the server state is the same both times — the room is gone. We return `404` on the second call rather than `204` again because it's more honest — it tells the client the room was already gone, rather than pretending the delete worked when there was nothing to delete.

---

### Part 3 — Sensor Operations

#### Q3.1 — `@Consumes` Annotation and Format Mismatch Consequences

`@Consumes(MediaType.APPLICATION_JSON)` tells Jersey what Content-Type the endpoint accepts. If the client sends the wrong type (e.g. `text/plain`), Jersey automatically returns 415 Unsupported Media Type before our code even runs.

This is useful because it means we don't have to manually check the content type inside every method. It also prevents badly formatted data from reaching the business logic and causing confusing errors later on.

---

#### Q3.2 — `@QueryParam` vs Path Segment for Filtering

We use `GET /sensors?type=CO2` instead of `GET /sensors/CO2` because query params are for optional filtering — they narrow down a collection. A path segment like `/sensors/CO2` would suggest that CO2 is its own separate resource, which it isn't.

Query parameters also make more sense semantically — the same endpoint works with or without them, and the base URL `/sensors` always refers to the full collection.

---

### Part 4 — Sub-Resources

#### Q4.1 — Sub-Resource Locator Architectural Benefits

A sub-resource locator is a method that has a `@Path` annotation but no HTTP method (`@GET`, `@POST`, etc.). Instead of handling the request, it just returns another object (the sub-resource) for Jersey to use.

In this project, `SensorResource` has a locator method that returns a `SensorReadingResource` object. This is a good design because:

1. **Separation of concerns** — `SensorResource` deals with sensors, and `SensorReadingResource` deals with readings. They don't mix logic.
2. **Easy context passing** — We can pass the `sensorId` straight into the `SensorReadingResource` constructor, so it doesn't need to worry about URL parsing.
3. **Efficiency** — Jersey only creates the `SensorReadingResource` object if the user actually goes to the `/readings` path.
4. **Easy to test** — Since `SensorReadingResource` is just a regular Java object, we can test it easily without starting up the whole JAX-RS server.
5. **Clean URLs** — The path `/sensors/{id}/readings` is very clear and shows exactly how the data is connected.

---

### Part 5 — Error Handling and Logging

#### Q5.2 — HTTP 422 Unprocessable Entity vs 404 Not Found

Both codes could apply here but they mean different things. 404 means the URL itself doesn't exist. 422 means the URL is fine but the data in the request body doesn't make sense.

When you `POST /api/v1/sensors` with a `roomId` that doesn't exist, the endpoint is valid and the JSON is valid — the problem is that the `roomId` value refers to something that doesn't exist. That's a semantic issue, so 422 is the right code. Using 404 would be confusing because a client might think the `/sensors` endpoint itself wasn't found.

---

#### Q5.4 — Security Risks of Exposing Stack Traces

Returning stack traces in HTTP responses is risky for a few reasons:

1. **Information leakage** — Stack traces show class names, method names, library versions, and file paths. An attacker can use this to figure out exactly what you're running and look for known exploits.
2. **Sensitive data** — The stack might include variable values like tokens or config data that ended up on the call stack.
3. **Easier attacks** — Knowing the exact line of a crash makes it easier to figure out how to trigger it.

In this project, `GlobalExceptionMapper` catches all exceptions, logs the full details to the server log where only admins can see it, and just returns a generic error message to the client.

---

#### Q5.5 — Why Filters Are Better Than Manual Logging

If we added logging manually inside every resource method, we'd be copying the same code everywhere. It's also easy to forget it on new methods, and changing the format would mean editing every class.

Using a filter is much cleaner. `ApiLoggingFilter` is registered once in `SmartCampusApplication` and automatically logs every request and response without touching the resource classes. The benefits are:

1. **No duplicate code** — One class does all the logging.
2. **Consistent format** — It's all managed in one place.
3. **Cleaner resources** — Resource classes only deal with business logic.
4. **Easy to turn off** — You can disable all logging just by removing the filter from the registration.

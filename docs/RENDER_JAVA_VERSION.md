# Set Java 17 or 21 on Render (api-service)

Render does **not** list Java in the dashboard under "Environment" or "Runtime" the way it does for Node or Python. The Java version is determined by **how** you build and run the service.

---

## 1. If your api-service uses **Docker** on Render

The Java version is set by the **base image** in your **Dockerfile**.

- In the Render Dashboard: open your **Web Service** (e.g. tito-api) → **Settings**.
- Check **Docker** section: if "Dockerfile Path" is set (e.g. `Dockerfile` or `api-service/Dockerfile`), you are using Docker.
- Open that **Dockerfile** in the repo. The **FROM** line defines the JDK/JRE version, for example:
  - `FROM eclipse-temurin:17-jdk-alpine` → Java 17
  - `FROM eclipse-temurin:21-jdk-alpine` → Java 21
- To use Java 17 or 21: change the image to **eclipse-temurin:17** or **eclipse-temurin:21** (with `-jdk` for build, `-jre` for run). Example:
  ```dockerfile
  FROM eclipse-temurin:21-jre-alpine
  ADD api-service/target/*.jar /app.jar
  EXPOSE 8281
  ENTRYPOINT ["java", "-jar", "/app.jar"]
  ```
- Save, commit, push; then **Redeploy** the service on Render.

**No setting in the Render UI** changes the Java version when using Docker — it comes only from the Dockerfile.

---

## 2. If your api-service does **not** use Docker (Build + Start command)

If you use a **Build Command** (e.g. `mvn -pl api-service clean package`) and **Start Command** (e.g. `java -jar api-service/target/api-service-0.1.1.jar`) without a Dockerfile:

- Render may be using a **default build image** that includes an older Java (e.g. 8 or 11). There is **no** "Java version" dropdown in the Render dashboard for this.
- **Recommended:** Switch to **Docker** and add a Dockerfile (see section 1) so you explicitly control Java 21. The repo can include an **api-service/Dockerfile** (or root **Dockerfile** that builds api-service) and you set **Dockerfile Path** in Render to that file.
- **Alternative:** If Render uses a buildpack that reads `system.properties`, add a file **`system.properties`** in the **repository root** (same level as `pom.xml`) with:
  ```properties
  java.runtime.version=21
  ```
  Then redeploy. This only works if Render’s build environment uses a JVM buildpack that respects this file; if nothing changes, use Docker instead.

---

## 3. Quick check: which Java is running?

After deploy, open **Logs** for the service and look for the Java version in the startup output (e.g. "Starting ApiApplication using **Java 21.0.x**"). If you see Java 8 or 11, follow section 1 or 2 to move to 17 or 21.

---

## Why Java 17+ for MongoDB Atlas

Older Java (8 or outdated 11) can cause TLS handshake failures when connecting to MongoDB Atlas from Render (`SSLException: internal_error`). Java 17 or 21 avoids most of these issues. See the frontend doc **LOGIN_ERROR_BACKEND_MONGODB_TIMEOUT.md** (section "Exact error: SSLException: internal_error").

# ---- build stage: compile the coduel-app fat jar (multi-module Maven) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
# -am builds the upstream modules (coduel-common, coduel-execution) that coduel-app needs.
RUN mvn -q -pl coduel-app -am clean package -DskipTests

# ---- run stage: JRE + python3 (the code-execution engine runs `python3 main.py`) ----
FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /src/coduel-app/target/coduel-app-*.jar app.jar
# Keep the JVM heap under the free-tier 512 MB (leaves room for the spawned python subprocesses).
ENV JAVA_TOOL_OPTIONS="-Xmx384m -XX:+UseSerialGC"
# App reads server.port=${PORT:8080}; Render injects PORT. EXPOSE is documentation only.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

# syntax=docker/dockerfile:1

# ─────────────────────────────────────────────────────────────────────────────
# Étape 1 — Build (front Next.js + back Micronaut) → fat jar « all-in-one ».
# Le frontend-maven-plugin télécharge Node tout seul et compile l'export Next dans
# forge-app/src/main/resources/public, embarqué ensuite dans le jar.
# JDK 26 requis (cible release 25).
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:26-jdk AS build
WORKDIR /workspace

# Cache des dépendances : copie d'abord le wrapper + les pom, puis le reste.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY forge-core/pom.xml          forge-core/
COPY forge-security/pom.xml      forge-security/
COPY forge-catalog/pom.xml       forge-catalog/
COPY forge-business/pom.xml      forge-business/
COPY forge-valuation/pom.xml     forge-valuation/
COPY forge-ledger/pom.xml        forge-ledger/
COPY forge-billing/pom.xml       forge-billing/
COPY forge-treasury/pom.xml      forge-treasury/
COPY forge-notifications/pom.xml forge-notifications/
COPY forge-stats/pom.xml         forge-stats/
COPY forge-app/pom.xml           forge-app/
RUN chmod +x mvnw && ./mvnw -B -ntp -pl forge-app -am dependency:go-offline -DskipTests -Denforcer.skip=true || true

# Sources puis build complet (front + back).
COPY . .
RUN ./mvnw -B -ntp -pl forge-app -am clean package -DskipTests -Denforcer.skip=true \
 && cp forge-app/target/forge-app-*.jar /workspace/app.jar

# ─────────────────────────────────────────────────────────────────────────────
# Étape 2 — Runtime (JRE seul, image légère, user non-root).
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:26-jre AS runtime
WORKDIR /app

# curl : requis par le HEALTHCHECK (/health). /logs : créé/possédé par theforge
# pour que le stdout soit écrivable même sans volume (en prod le volume le recouvre).
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl bash \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system theforge \
 && useradd --system --gid theforge --home /app theforge \
 && mkdir -p /logs && chown theforge:theforge /logs
COPY --from=build /workspace/app.jar /app/app.jar
USER theforge

EXPOSE 8080
ENV JAVA_OPTS=""

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fsS http://localhost:8080/health || exit 1

# Tout le stdout/stderr est affiché (docker logs) ET copié dans ${LOG_DIR}/theforge-stdout.log.
ENTRYPOINT ["bash", "-c", "exec java $JAVA_OPTS -jar /app/app.jar > >(tee -a \"${LOG_DIR:-/logs}/theforge-stdout.log\") 2>&1"]

# Railway-ready build: compila el WAR y arranca WildFly con el datasource registrado.
# El docker-compose local sigue usando docker/wildfly/Dockerfile.

# ---- Build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Runtime ----
FROM quay.io/wildfly/wildfly:31.0.1.Final-jdk17

# Driver JDBC de PostgreSQL (misma version que el pom).
# --chown=jboss: jboss-cli corre como usuario jboss y debe poder leer el jar.
ADD --chown=jboss:jboss https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar /tmp/postgresql.jar

# Registra el driver y el datasource java:/jdbc/ChatDS.
# Los valores reales llegan como system properties en runtime (ver entrypoint.sh).
COPY docker/wildfly/register-ds.cli /tmp/register-ds.cli
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/tmp/register-ds.cli

COPY --from=build /app/target/chat-empresarial.war /opt/jboss/wildfly/standalone/deployments/
COPY docker/wildfly/entrypoint.sh /opt/jboss/entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/bin/bash", "/opt/jboss/entrypoint.sh"]

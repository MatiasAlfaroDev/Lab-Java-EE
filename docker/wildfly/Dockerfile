FROM quay.io/wildfly/wildfly:31.0.1.Final-jdk17

# Copiar WAR generado por Maven
COPY target/mensajeria-empresarial.war /opt/jboss/wildfly/standalone/deployments/

EXPOSE 8080 9990

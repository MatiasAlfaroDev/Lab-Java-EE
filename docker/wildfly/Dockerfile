FROM quay.io/wildfly/wildfly:31.0.1.Final-jdk17

# Copiar WAR (lo dejamos comentado por ahora)
COPY target/chat-empresarial.war /opt/jboss/wildfly/standalone/deployments/

# Crear usuario admin
RUN /opt/jboss/wildfly/bin/add-user.sh admin admin123 --silent

# Habilitar acceso externo
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]

EXPOSE 8080 9990
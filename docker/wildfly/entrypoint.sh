#!/bin/bash
# Pasa las credenciales de la DB (env vars de Railway) como system properties
# que resuelven los ${db.*} del datasource ChatDS.
set -e
exec /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 \
  -Ddb.url="${DB_URL}" \
  -Ddb.user="${DB_USER}" \
  -Ddb.password="${DB_PASSWORD}"

#!/bin/sh
# Crea la base de datos de los tests (<DB_NAME>_test) con permisos para el usuario de la app.
# Docker solo ejecuta este script la primera vez que se crea el volumen de MySQL.
mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "
  CREATE DATABASE IF NOT EXISTS \`${MYSQL_DATABASE}_test\`;
  GRANT ALL PRIVILEGES ON \`${MYSQL_DATABASE}_test\`.* TO '${MYSQL_USER}'@'%';
"

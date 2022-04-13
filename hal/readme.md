


# Restore a backup

PGPASSWORD=password123 pg_restore -h localhost -U hal -d hal /tmp/hal.Sunday-13.pg

PGPASSWORD=password123 pg_restore -c -h 172.19.0.2 -U hal -d hal /home/ff/projects/HAL900/hal-backups/hal.Wednesday-06.pg

# psql

PGPASSWORD=password123 psql -h 172.19.0.2 -U hal -d hal

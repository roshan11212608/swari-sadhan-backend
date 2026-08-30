@echo off
mysqldump -u root -proot --no-data --skip-comments --skip-triggers swarisadhan 2>nul > schema_dump.sql
mysql -u root -proot -e "DROP DATABASE IF EXISTS swarisadhan_test;" 2>nul
mysql -u root -proot -e "CREATE DATABASE swarisadhan_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>nul
mysql -u root -proot swarisadhan_test < schema_dump.sql 2>nul
mysql -u root -proot swarisadhan_test < setup_flyway_history.sql 2>nul
echo Done

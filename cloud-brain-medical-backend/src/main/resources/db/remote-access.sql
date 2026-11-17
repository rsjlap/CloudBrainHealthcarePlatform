-- 在「放 MySQL 的电脑」上执行，允许小组其他成员远程连库
-- 用法：mysql -u root -p < remote-access.sql
-- 或 Navicat / MySQL Workbench 打开执行

CREATE DATABASE IF NOT EXISTS cloud_brain_medical
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- 小组远程账号（可按需改用户名/密码，需与组员 application-dev.yml 或 .env 一致）
CREATE USER IF NOT EXISTS 'cloud_team'@'%' IDENTIFIED BY 'cloud123456';
GRANT ALL PRIVILEGES ON cloud_brain_medical.* TO 'cloud_team'@'%';

-- 本机 root 也可远程（可选，演示环境用；生产勿开）
-- CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '你的root密码';
-- GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;

FLUSH PRIVILEGES;

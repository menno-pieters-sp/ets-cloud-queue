CREATE DATABASE etscloudqueue DEFAULT CHARACTER SET utf8;

CREATE USER 'etscloudqueue'@'localhost' IDENTIFIED BY 'etscloudqueue';
GRANT ALL ON etscloudqueue.* TO 'etscloudqueue'@'localhost';


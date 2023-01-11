/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `etscloudqueue`
--

/*!40000 DROP DATABASE IF EXISTS `etscloudqueue`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `etscloudqueue` /*!40100 DEFAULT CHARACTER SET utf8 */ /*!80016 DEFAULT ENCRYPTION='N' */;

CREATE USER 'etscloudqueue'@'localhost' IDENTIFIED BY 'etscloudqueue';
GRANT ALL ON etscloudqueue.* TO 'etscloudqueue'@'localhost';

USE `etscloudqueue`;

--
-- Table structure for table `ets_queue`
--

DROP TABLE IF EXISTS `ets_queue`;
CREATE TABLE `ets_queue` (
  `id` varchar(128) NOT NULL,
  `description` longtext,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `ets_queue_access`
--

DROP TABLE IF EXISTS `ets_queue_access`;
CREATE TABLE `ets_queue_access` (
  `queue_id` varchar(128) NOT NULL,
  `user_id` varchar(128) NOT NULL,
  `read` tinyint NOT NULL DEFAULT '0',
  `write` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`user_id`,`queue_id`),
  KEY `fk_queue_id_idx` (`queue_id`),
  CONSTRAINT `fk_xs_queue_id` FOREIGN KEY (`queue_id`) REFERENCES `ets_queue` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_xs_user_id` FOREIGN KEY (`user_id`) REFERENCES `ets_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `ets_queue_entry`
--

DROP TABLE IF EXISTS `ets_queue_entry`;
CREATE TABLE `ets_queue_entry` (
  `id` int NOT NULL AUTO_INCREMENT,
  `queue_id` varchar(128) NOT NULL,
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `data` longtext NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `fk_queue_id_idx` (`queue_id`),
  CONSTRAINT `fk_queue_id` FOREIGN KEY (`queue_id`) REFERENCES `ets_queue` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8;

--
-- Table structure for table `ets_user`
--

DROP TABLE IF EXISTS `ets_user`;
CREATE TABLE `ets_user` (
  `id` varchar(128) NOT NULL,
  `name` varchar(128) NOT NULL,
  `display_name` varchar(128) DEFAULT NULL,
  `active` tinyint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `ets_user_token`
--

DROP TABLE IF EXISTS `ets_user_token`;
CREATE TABLE `ets_user_token` (
  `id` varchar(128) NOT NULL,
  `token` varchar(768) NOT NULL,
  `user_id` varchar(128) NOT NULL,
  `description` varchar(256) DEFAULT NULL,
  `expiration` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `token_UNIQUE` (`token`),
  KEY `fk_user_id_idx` (`user_id`),
  CONSTRAINT `fk_user_id` FOREIGN KEY (`user_id`) REFERENCES `ets_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

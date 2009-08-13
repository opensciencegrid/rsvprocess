-- MySQL dump 10.11
--
-- Host: localhost    Database: rsvprocess
-- ------------------------------------------------------
-- Server version	5.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `rsvprocess`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `rsvprocess` /*!40100 DEFAULT CHARACTER SET latin1 */;

USE `rsvprocess`;

--
-- Table structure for table `Tbl_SamUploadFlagDesc`
--

DROP TABLE IF EXISTS `Tbl_SamUploadFlagDesc`;
CREATE TABLE `Tbl_SamUploadFlagDesc` (
  `flagid` int(11) NOT NULL,
  `flagDesc` varchar(255) default NULL COMMENT 'Description of what this flag means',
  PRIMARY KEY  (`flagid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `Tbl_SamUploadStatus`
--

DROP TABLE IF EXISTS `Tbl_SamUploadStatus`;
CREATE TABLE `Tbl_SamUploadStatus` (
  `dbid` int(11) NOT NULL COMMENT 'dbid corresponding to MetricRecord.dbid though not a foreign key',
  `flagid` int(11) NOT NULL COMMENT 'flagid corresponding to rsvextra.SamUploadFlagDesc.flagid',
  `comment` text COMMENT 'This field can be used to add comments about upload success or failures',
  PRIMARY KEY  (`dbid`),
  KEY `Fk_SUS_SamFlagid` (`flagid`),
  CONSTRAINT `Tbl_SamUploadStatus_ibfk_1` FOREIGN KEY (`flagid`) REFERENCES `Tbl_SamUploadFlagDesc` (`flagid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `metricdata`
--

DROP TABLE IF EXISTS `metricdata`;
CREATE TABLE `metricdata` (
  `id` int(11) NOT NULL default '0',
  `timestamp` int(11) NOT NULL default '0',
  `resource_id` smallint(5) unsigned NOT NULL default '0',
  `metric_id` smallint(5) unsigned NOT NULL default '0',
  `metric_status_id` tinyint(3) unsigned NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `for_latest_grab` (`metric_id`,`resource_id`,`timestamp`),
  KEY `timestamp` USING BTREE (`timestamp`,`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='<strong><u>Rsvextra.Rsv_result</u></strong>: This entirty is';

DROP TABLE IF EXISTS `processlog`;
CREATE TABLE  `rsvprocess`.`processlog` (
  `timestamp` timestamp NOT NULL default '0000-00-00 00:00:00' on update CURRENT_TIMESTAMP,
  `key` varchar(128) NOT NULL,
  `value` varchar(1024) NOT NULL,
  PRIMARY KEY  USING BTREE (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `resource_detail`
--

DROP TABLE IF EXISTS `resource_detail`;
CREATE TABLE `resource_detail` (
  `resource_id` varchar(45) NOT NULL,
  `metric_id` varchar(45) NOT NULL,
  `xml` text NOT NULL,
  PRIMARY KEY  (`resource_id`,`metric_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `rss_article`
--

DROP TABLE IF EXISTS `rss_article`;
CREATE TABLE `rss_article` (
  `id` int(11) NOT NULL auto_increment,
  `date` int(11) NOT NULL default '0',
  `title` varchar(255) NOT NULL default 'no title',
  `description` varchar(255) NOT NULL default 'no description',
  `body` mediumtext NOT NULL,
  `ticket` int(6) default NULL,
  PRIMARY KEY  (`id`),
  KEY `date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `service_ar`
--

DROP TABLE IF EXISTS `service_ar`;
CREATE TABLE `service_ar` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `resource_id` int(10) unsigned NOT NULL,
  `service_id` int(10) unsigned NOT NULL,
  `availability` double NOT NULL,
  `reliability` double NOT NULL,
  `timestamp` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`id`),
  KEY `resource_service` (`resource_id`,`service_id`),
  KEY `timestamp` (`timestamp`)
) ENGINE=InnoDB AUTO_INCREMENT=232 DEFAULT CHARSET=latin1;

--
-- Table structure for table `statuschange_resource`
--

DROP TABLE IF EXISTS `statuschange_resource`;
CREATE TABLE `statuschange_resource` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `resource_id` smallint(5) unsigned NOT NULL,
  `status_id` tinyint(1) unsigned NOT NULL default '0',
  `timestamp` int(10) unsigned NOT NULL,
  `detail` varchar(256) NOT NULL,
  PRIMARY KEY  (`id`),
  KEY `timestamp` (`timestamp`)
) ENGINE=InnoDB AUTO_INCREMENT=41670 DEFAULT CHARSET=latin1;

--
-- Table structure for table `statuschange_service`
--

DROP TABLE IF EXISTS `statuschange_service`;
CREATE TABLE `statuschange_service` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `resource_id` smallint(5) unsigned NOT NULL,
  `service_id` tinyint(3) unsigned NOT NULL,
  `status_id` tinyint(1) unsigned NOT NULL default '0',
  `timestamp` int(10) unsigned NOT NULL,
  `detail` varchar(256) NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=52316 DEFAULT CHARSET=latin1;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2009-08-13  0:52:12

-- MySQL dump 10.13  Distrib 8.4.5, for Linux (aarch64)
--
-- Host: knittda-db.c9o4y6280l3u.ap-northeast-2.rds.amazonaws.com    Database: knittda_db
-- ------------------------------------------------------
-- Server version	8.0.41

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
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '';

--
-- Current Database: `knittda_db`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `knittda_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `knittda_db`;

--
-- Table structure for table `design`
--

DROP TABLE IF EXISTS `design`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `design` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `designer` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `yarn_info` text,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `needle_info` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=623 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `design`
--

LOCK TABLES `design` WRITE;
/*!40000 ALTER TABLE `design` DISABLE KEYS */;
INSERT INTO `design` VALUES (1,'.','.',NULL,NULL,NULL,NULL),(2,'ㅜ','ㅠ',NULL,NULL,NULL,NULL),(6,'김대리','피그먼트울 하이넥 새들 가디건','피그먼트울(1콘/260g/600m) 2 (2) 2 (2) 2 (3) 3 콘 / 약 400 (420) 450 (490) 520 (560) 600 g',NULL,NULL,NULL),(293,'바늘이야기','베지터블 꽈배기 반팔 바텀업 스웨터','바늘이야기 베지터블 3 (3) 4 볼',NULL,NULL,NULL),(528,'김대리','울아란 레이니데이 코위찬','울 아란(색상선택) 9볼(메인색상 6볼, 배색 3볼)',NULL,NULL,NULL),(531,'야닝야닝의 야매뜨개','깅엄체크 테이블매트',NULL,NULL,NULL,NULL),(532,'','',NULL,NULL,NULL,NULL),(534,'아2','아1',NULL,NULL,NULL,NULL),(538,'몽','몽',NULL,NULL,NULL,NULL),(540,'ㅇ','ㄴ',NULL,NULL,NULL,NULL),(541,'','',NULL,NULL,NULL,NULL),(543,'기억 안나','푸딩파우치',NULL,NULL,NULL,NULL),(544,'','',NULL,NULL,NULL,NULL),(548,'행운','HBD 딸기 케이크 모자',NULL,NULL,NULL,NULL),(549,'ex2','ex1',NULL,NULL,NULL,NULL),(553,'Marjolijn Reuter','Sweet Gift of Love Scarf',NULL,NULL,NULL,NULL),(579,'','hamburger pouch',NULL,NULL,NULL,NULL),(582,'','sylvanian strawberry crochet',NULL,NULL,NULL,NULL),(583,'바늘이야기','실버호보백',NULL,NULL,NULL,NULL),(585,'바늘이야기','핑크가방',NULL,NULL,NULL,NULL),(590,'.','.',NULL,NULL,NULL,NULL),(605,'',NULL,NULL,'2025-07-16 05:53:34.036849',NULL,NULL),(606,'쁘띠니트','노프릴스웨터','에어리코튼','2025-07-16 13:12:33.644302',NULL,'진저 3.5,4,4.5'),(607,'모래니트','코지 리본 스웨터','러스크 1합','2025-07-16 13:28:40.122758',NULL,'치아오구 4mm'),(608,'김대리','블랙베리아란스웨터','프빌 도브 3합','2025-07-16 13:41:13.595656',NULL,'5mm'),(609,'쁘띠니트','모비스웨터','뉴보름 2합','2025-07-16 13:50:06.004764',NULL,'치아오구 5mm'),(610,'신디','칼리오페스웨터','풍성한 파란실 2합','2025-07-16 14:33:08.601744',NULL,'대바늘 6mm'),(611,'신똘님','더클래식스웨터','청송뜨개실 오렌지','2025-07-16 14:38:31.905159',NULL,'대바늘 4.5mm'),(612,'strikk','까멜리아스웨터','슈니No.17, No.12, No.18','2025-07-16 14:44:20.137386',NULL,'대바늘'),(613,'gam studio','건지스웨터','hedgehog tweedy','2025-07-16 14:49:45.983915',NULL,'대바늘'),(614,'우맘니팅','디어마이스웨터','카멜통사','2025-07-16 14:54:28.608249',NULL,'대바늘'),(616,'',NULL,NULL,'2025-07-16 15:28:54.522869',NULL,NULL),(617,'어게인 니트',NULL,NULL,'2025-07-16 15:34:11.827307',NULL,NULL),(618,'바늘이야기',NULL,NULL,'2025-07-16 15:42:12.230888',NULL,NULL);
/*!40000 ALTER TABLE `design` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `thumbnail_image`
--

DROP TABLE IF EXISTS `thumbnail_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `thumbnail_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `image_url` varchar(1024) NOT NULL,
  `created_at` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `thumbnail_image`
--

LOCK TABLES `thumbnail_image` WRITE;
/*!40000 ALTER TABLE `thumbnail_image` DISABLE KEYS */;
-- 기존 PROJECT 타입 이미지들을 thumbnail_image로 마이그레이션
INSERT INTO `thumbnail_image` (`id`, `image_url`, `created_at`)
SELECT `id`, `image_url`, `created_at` FROM `image` WHERE `image_type` = 'PROJECT' AND `record_id` IS NULL;
/*!40000 ALTER TABLE `thumbnail_image` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `image`
--

DROP TABLE IF EXISTS `image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `image_order` int DEFAULT NULL,
  `image_url` varchar(1024) NOT NULL,
  `record_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `image_type` enum('RECORD') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8idcip3mxih9wvwuale60ci3e` (`record_id`),
  CONSTRAINT `FK8idcip3mxih9wvwuale60ci3e` FOREIGN KEY (`record_id`) REFERENCES `record` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=321 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `image`
--

LOCK TABLES `image` WRITE;
/*!40000 ALTER TABLE `image` DISABLE KEYS */;
-- RECORD 타입 이미지만 유지
INSERT INTO `image` (`id`, `image_order`, `image_url`, `record_id`, `created_at`, `image_type`)
SELECT `id`, `image_order`, `image_url`, `record_id`, `created_at`, `image_type` 
FROM `image` WHERE `image_type` = 'RECORD';
/*!40000 ALTER TABLE `image` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project`
--

DROP TABLE IF EXISTS `project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `design_id` bigint NOT NULL,
  `end_date` date DEFAULT NULL,
  `goal_date` date DEFAULT NULL,
  `last_record_at` datetime(6) DEFAULT NULL,
  `nickname` varchar(255) NOT NULL,
  `start_date` date DEFAULT NULL,
  `status` enum('COMPLETED','IN_PROGRESS') NOT NULL DEFAULT 'IN_PROGRESS',
  `thumbnail` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_project_design` (`design_id`),
  KEY `FK_project_user` (`user_id`),
  KEY `FK_project_thumbnail` (`thumbnail`),
  CONSTRAINT `FK_project_design` FOREIGN KEY (`design_id`) REFERENCES `design` (`id`),
  CONSTRAINT `FK_project_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FK_project_thumbnail` FOREIGN KEY (`thumbnail`) REFERENCES `thumbnail_image` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=620 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project`
--

LOCK TABLES `project` WRITE;
/*!40000 ALTER TABLE `project` DISABLE KEYS */;
-- 기존 project 데이터를 유지하되, thumbnail을 thumbnail_image의 id로 매핑
INSERT INTO `project` (`id`, `created_at`, `design_id`, `end_date`, `goal_date`, `last_record_at`, `nickname`, `start_date`, `status`, `thumbnail`, `user_id`)
SELECT p.`id`, p.`created_at`, p.`design_id`, p.`end_date`, p.`goal_date`, p.`last_record_at`, p.`nickname`, p.`start_date`, p.`status`, 
       (SELECT ti.`id` FROM `thumbnail_image` ti WHERE ti.`image_url` = (SELECT i.`image_url` FROM `image` i WHERE i.`id` = p.`thumbnail` LIMIT 1) LIMIT 1),
       p.`user_id`
FROM `project` p;
/*!40000 ALTER TABLE `project` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `record`
--

DROP TABLE IF EXISTS `record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `record_status` enum('ALMOST_DONE','COMPLETED','IN_PROGRESS','NOT_STARTED','STARTED') NOT NULL,
  `project_id` bigint NOT NULL,
  `embedding_json` text,
  PRIMARY KEY (`id`),
  KEY `FK1y6vpd46d3j4rxf24q7ut2x7i` (`project_id`),
  CONSTRAINT `FK1y6vpd46d3j4rxf24q7ut2x7i` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=192 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `record`
--

LOCK TABLES `record` WRITE;
/*!40000 ALTER TABLE `record` DISABLE KEYS */;
INSERT INTO `record` VALUES (1,'냠','2025-06-07 13:08:18.561637','IN_PROGRESS',2,NULL),(3,'부끄러버','2025-06-07 17:16:31.027710','STARTED',4,NULL),(18,'잉','2025-06-09 01:06:50.022309','NOT_STARTED',16,NULL),(28,'뜰 때 완전 재미있었음✨\n오밀조밀 들어가는게 많아서 어렵고 번거로운데 그만큼 재미있고 귀엽다!','2025-06-09 14:35:51.701335','COMPLETED',28,NULL),(29,'칸이 잘 보이니 늘리는 재미가 있었다🩷\n생각보다 더 예뻐서 다양한 색으로 떠서 지인들 선물도 해주고 싶고 코스터로도 뜨고 싶고…\n기법이 계속 똑같은데 배색을 해야하니 지루하지 않아서 좋았다! 코바늘 배색 연습에 너무 좋은 도안인듯.\n근데 손목이 좀 아프다🥲','2025-06-09 14:39:15.954311','COMPLETED',9,NULL),(30,'다이소 실로 두개 가량 뜸! 립스틱이나 작은 소품 넣기 용이함','2025-06-09 14:39:38.900717','COMPLETED',21,NULL),(31,'왕창 만들고 사각으로 뜬 다음에 이어붙여서 가방 만들 예정','2025-06-09 14:40:21.192160','STARTED',19,NULL),(32,'냠','2025-06-10 04:03:49.763890','ALMOST_DONE',10,NULL),(33,'완벽.','2025-06-10 04:15:53.804258','STARTED',10,NULL),(34,'힘들엇','2025-06-10 05:14:32.366832','IN_PROGRESS',18,NULL),(35,'뿌듯','2025-06-10 05:15:39.417958','ALMOST_DONE',18,NULL),(41,'안녕 캡스톤!','2025-06-10 14:36:54.901164','IN_PROGRESS',16,NULL),(42,'처음으로 완성한 목도리🧣\n엄마가 끈기 있다면서 칭찬해줬다!\n메리야스로 무늬 넣기, 꽈배기 무늬 연습하기 너무 좋았던 도안이다. 하트 무늬가 있어서 목도리가 늘어가는 것도 잘 보이고 뿌듯했음!','2025-06-10 16:49:44.720746','COMPLETED',34,NULL),(77,'하….힘들다\n4시간 가량의 선물은 이정도\n올 해 안에 뜰 수 있을까🥺','2025-07-06 08:48:12.169816','NOT_STARTED',66,NULL),(78,'전 지쳐버렸어요..','2025-07-06 08:52:08.086116','NOT_STARTED',68,NULL),(80,'.','2025-07-09 05:46:20.807364','IN_PROGRESS',16,NULL),(103,'웅','2025-07-12 12:38:54.952165','IN_PROGRESS',16,NULL),(104,'응가','2025-07-12 14:46:48.445867','NOT_STARTED',82,NULL),(120,'코 수가 심상치 않다.\n녹색 8볼로 다 뜰 수 있겠지?','2025-07-16 05:55:18.143361','NOT_STARTED',95,NULL),(124,'노프릴 스웨터 시작! 목부터 떠내려가는 탑다운 방식이다. 한코 고무뜨기로 시작해서 저먼숏로우 방식으로 넥라인을 만들며 떠내려가야 한다. 턴할때 헷갈려서 몇번 실수했다','2025-07-16 13:20:21.489733','STARTED',96,NULL),(125,'레글런 늘림 하면서 몸통뜨는중. 20cm 더 떠야하는데 무메 너무 지루하다ㅜㅜ','2025-07-16 13:21:48.033564','IN_PROGRESS',96,NULL),(126,'소매 분리 후 계속 무메로 뜨다가 고무단까지 하면 몸통 완성!','2025-07-16 13:22:48.192285','IN_PROGRESS',96,NULL),(127,'소매 뜨는 중! 암홀 구멍 덜 생기게 하는 방법으로 떴는데, 그래도 구멍이 생겼다ㅜㅜ','2025-07-16 13:26:32.978544','ALMOST_DONE',96,NULL),(128,'양쪽 소매 다 떠주면 완성!','2025-07-16 13:26:56.275240','COMPLETED',96,NULL),(129,'코잡고 뒷판 시작!','2025-07-16 13:33:33.937759','STARTED',97,NULL),(130,'뒷판 뜨는 중. 무늬는 너무 예쁜데 3색 배색은 아직 어렵다. 천천히 뜨는중','2025-07-16 13:34:43.703576','STARTED',97,NULL),(131,'드디어 앞판 시작! 뒷판하면서 배색에 조금 익숙해진터라 속도가 조금 빨라졌다','2025-07-16 13:36:05.729474','IN_PROGRESS',97,NULL),(132,'리본 배색 구간 지나서 계속 떠주면 몸통 완성!','2025-07-16 13:37:19.475847','ALMOST_DONE',97,NULL),(133,'소매에도 무늬가 있어서 오래 걸렸다ㅜㅜ 소매 뜨고 고무단 마무리까지 해주면 진짜 완성!','2025-07-16 13:38:54.752745','COMPLETED',97,NULL),(134,'코잡고 L사이즈 고무단 뜨는중. 고무단은 언제나 지루하다','2025-07-16 13:43:01.680981','NOT_STARTED',98,NULL),(135,'열심히 뒷판 뜨는중! 베리 무늬가 너무 귀엽다','2025-07-16 13:44:04.166096','STARTED',98,NULL),(136,'뒷판 끝! 무늬뜨기도 점점 익숙해졌다','2025-07-16 13:45:19.246579','IN_PROGRESS',98,NULL),(137,'앞판도 뒷판하고 동일하게 코잡고 열심히 무늬 떠주면 된다. 뒷판하고 어깨까지 이어주면 몸통 끝!','2025-07-16 13:46:06.687541','IN_PROGRESS',98,NULL),(138,'양쪽 소매 뜨고 목 고무단까지 떠주면 완성','2025-07-16 13:47:19.689230','COMPLETED',98,NULL),(139,'뒷판 시작.. 어려서워서 몇번 푸르시오 했지만 해냈다!!','2025-07-16 13:51:26.420775','STARTED',99,NULL),(140,'다이아몬드 무늬 틀릴까바 조심스럽게 뜨는 중! 목 연결부분까지 떴다','2025-07-16 13:52:28.969801','STARTED',99,NULL),(141,'몸통 다 뜨고 목 겹단까지 마쳤다','2025-07-16 13:53:38.255636','IN_PROGRESS',99,NULL),(142,'한쪽 팔 완성.. 다른 쪽도 떠야하는거 생각하면 지친다','2025-07-16 13:54:46.052508','ALMOST_DONE',99,NULL),(143,'모비스웨터도 완성!!','2025-07-16 13:55:18.704872','COMPLETED',99,NULL),(144,'.','2025-07-16 14:30:17.614977','IN_PROGRESS',82,NULL),(145,'선물 받은 실로 목부터 뜨기 시작','2025-07-16 14:34:30.555458','NOT_STARTED',100,NULL),(146,'목부근 다 뜨고 착샷','2025-07-16 14:35:18.342770','NOT_STARTED',100,NULL),(147,'팔 시작','2025-07-16 14:35:56.911523','STARTED',100,NULL),(148,'몸통 거의 다 떴다','2025-07-16 14:36:50.717496','IN_PROGRESS',100,NULL),(149,'완성 후 착샷 굿','2025-07-16 14:37:10.955540','COMPLETED',100,NULL),(150,'목부터 시작','2025-07-16 14:39:03.471996','NOT_STARTED',101,NULL),(151,'쭉쭉 뜨기','2025-07-16 14:39:48.450808','STARTED',101,NULL),(152,'팔부분 뜨기 시작','2025-07-16 14:40:12.085610','STARTED',101,NULL),(153,'허리 고무단 뜨다가 실수','2025-07-16 14:40:56.625692','NOT_STARTED',101,NULL),(154,'다 뜨고 입어보니 따뜻하고 조으네요','2025-07-16 14:41:23.950696','COMPLETED',101,NULL),(155,'목부터 시작\n카페 가서 떴다\n가을 선물용으로','2025-07-16 14:45:28.528233','NOT_STARTED',102,NULL),(156,'강아지','2025-07-16 14:46:24.885502','STARTED',102,NULL),(157,'앞뒤 요크 다 뜨고 몸통과 소매 분리','2025-07-16 14:47:14.116364','IN_PROGRESS',102,NULL),(158,'입어봄 사이즈 굿','2025-07-16 14:47:40.561901','IN_PROGRESS',102,NULL),(159,'허리 고무단','2025-07-16 14:48:03.442327','ALMOST_DONE',102,NULL),(160,'완성','2025-07-16 14:48:24.194934','COMPLETED',102,NULL),(161,'두식이 주려고 뜬다\n두식이 목이 두꺼워서 좀 걱정이다','2025-07-16 14:50:23.775407','NOT_STARTED',103,NULL),(162,'요크부분 시작\n두식아 사랑해','2025-07-16 14:51:02.013192','STARTED',103,NULL),(163,'몸통에 들어갈 아란 파트가 완료되었다','2025-07-16 14:51:05.642657','STARTED',95,NULL),(164,'앞뒤 요크 끝나고 원통으로','2025-07-16 14:51:40.584682','STARTED',103,NULL),(165,'앞판을 다 떴다! 이제 소매 연결만 하면 끝','2025-07-16 14:52:02.015221','IN_PROGRESS',95,NULL),(166,'스토퍼로 막아서 살펴보기','2025-07-16 14:52:18.556533','IN_PROGRESS',103,NULL),(167,'착샷\n소매시작\n두식이 주기엔 작으니 내가 입어야지','2025-07-16 14:52:46.133084','ALMOST_DONE',103,NULL),(168,'게이지가 잘 안 맞아서, 다시 풀렀다','2025-07-16 14:52:55.709036','ALMOST_DONE',95,NULL),(169,'내가 원하는 핏으로 완성!','2025-07-16 14:53:30.614761','COMPLETED',95,NULL),(170,'작은 시작','2025-07-16 14:54:50.742758','NOT_STARTED',104,NULL),(171,'스와치','2025-07-16 14:55:02.250757','NOT_STARTED',104,NULL),(172,'게이지가 원작실이 딱임','2025-07-16 14:55:25.520572','STARTED',104,NULL),(173,'잔털좀봐\n신기하지','2025-07-16 14:56:12.590732','ALMOST_DONE',104,NULL),(174,'목..?','2025-07-16 14:56:31.294535','ALMOST_DONE',104,NULL),(175,'고무 뜨기 완료. 이게 언제 니트가 될랑가…','2025-07-16 15:29:31.262669','NOT_STARTED',106,NULL),(176,'레글런 늘림 완료!','2025-07-16 15:30:16.086384','STARTED',106,NULL),(177,'실 다 써서 추가로 하나 더 사서 연결 했다. 아직도 레글런 늘림 중','2025-07-16 15:30:35.544566','STARTED',106,NULL),(178,'한 단계 더 나갔다. 이제 소매 분리 단 뜨는 중','2025-07-16 15:31:02.419484','IN_PROGRESS',106,NULL),(179,'이제 시작! 목표일까지 어서 끝내보자','2025-07-16 15:36:48.202052','NOT_STARTED',107,NULL),(180,'뒷 판 뜨는 중','2025-07-16 15:37:06.608973','IN_PROGRESS',107,NULL),(181,'이어주는 중이담','2025-07-16 15:37:26.493536','ALMOST_DONE',107,NULL),(182,'드디어 입고 나왔다','2025-07-16 15:37:45.238558','COMPLETED',107,NULL),(183,'어깨 앞판 떴다','2025-07-16 15:43:05.492322','NOT_STARTED',108,NULL),(184,'몸판 완성!','2025-07-16 15:43:21.218468','STARTED',108,NULL),(185,'소매 완성했지롱','2025-07-16 15:43:40.316239','IN_PROGRESS',108,NULL),(186,'돗바늘이 제일 어려운 사람 나야','2025-07-16 15:43:59.039737','ALMOST_DONE',108,NULL),(187,'세탁까지 끝났지롱','2025-07-16 15:44:20.024684','COMPLETED',108,NULL),(191,'테스트','2025-07-16 19:06:15.682191','STARTED',99,NULL);
/*!40000 ALTER TABLE `record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `record_tags`
--

DROP TABLE IF EXISTS `record_tags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `record_tags` (
  `record_id` bigint NOT NULL,
  `tags` varchar(255) DEFAULT NULL,
  KEY `FKa90d7bh0789xrwwuhli1ag5ya` (`record_id`),
  CONSTRAINT `FKa90d7bh0789xrwwuhli1ag5ya` FOREIGN KEY (`record_id`) REFERENCES `record` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `record_tags`
--

LOCK TABLES `record_tags` WRITE;
/*!40000 ALTER TABLE `record_tags` DISABLE KEYS */;
INSERT INTO `record_tags` VALUES (1,'지쳤어요'),(3,'완벽 해요'),(18,'지쳤어요'),(28,'실수했어요'),(28,'뿌듯해요'),(29,'푸르시오'),(29,'실수했어요'),(29,'실이 부족해요'),(29,'배색 뜨기'),(29,'뿌듯해요'),(29,'성공했어요'),(30,'뿌듯해요'),(31,'뿌듯해요'),(32,'함뜨했어요'),(33,'완벽 해요'),(34,'지쳤어요'),(34,'뿌듯해요'),(35,'뿌듯해요'),(35,'성공했어요'),(41,'푸르시오'),(42,'뿌듯해요'),(42,'실수했어요'),(42,'무한 메리야스 뜨기'),(42,'성공했어요'),(77,'지쳤어요'),(78,'지쳤어요'),(78,'실수했어요'),(78,'힘들어요'),(80,'성공했어요'),(104,'성공했어요'),(112,'완벽 해요'),(113,'완벽 해요'),(114,'완벽 해요'),(115,'완벽 해요'),(116,'완벽 해요'),(117,'무늬 뜨기'),(118,'무늬 뜨기'),(119,'무늬 뜨기'),(120,'무늬 뜨기'),(124,'힘들어요'),(124,'지쳤어요'),(124,'실수했어요'),(125,'지쳤어요'),(125,'무한 메리야스 뜨기'),(126,'무한 메리야스 뜨기'),(126,'완벽 해요'),(127,'지쳤어요'),(127,'무한 메리야스 뜨기'),(127,'힘들어요'),(128,'완벽 해요'),(128,'성공했어요'),(129,'힘들어요'),(129,'배색 뜨기'),(130,'힘들어요'),(130,'뿌듯해요'),(130,'배색 뜨기'),(131,'배색 뜨기'),(131,'뿌듯해요'),(131,'완벽 해요'),(132,'무늬 뜨기'),(132,'배색 뜨기'),(132,'뿌듯해요'),(132,'성공했어요'),(133,'완벽 해요'),(133,'배색 뜨기'),(133,'무늬 뜨기'),(133,'뿌듯해요'),(133,'성공했어요'),(134,'지쳤어요'),(135,'무늬 뜨기'),(135,'힘들어요'),(135,'지쳤어요'),(136,'뿌듯해요'),(136,'무늬 뜨기'),(136,'성공했어요'),(137,'뿌듯해요'),(137,'성공했어요'),(137,'완벽 해요'),(137,'무늬 뜨기'),(138,'완벽 해요'),(138,'무늬 뜨기'),(138,'성공했어요'),(138,'뿌듯해요'),(139,'무늬 뜨기'),(139,'푸르시오'),(139,'뿌듯해요'),(140,'무늬 뜨기'),(140,'힘들어요'),(141,'완벽 해요'),(141,'무늬 뜨기'),(141,'뿌듯해요'),(142,'지쳤어요'),(142,'무늬 뜨기'),(142,'힘들어요'),(143,'성공했어요'),(143,'뿌듯해요'),(143,'완벽 해요'),(144,'성공했어요'),(145,'무한 메리야스 뜨기'),(145,'함뜨했어요'),(145,'성공했어요'),(146,'함뜨했어요'),(146,'힘들어요'),(147,'무한 메리야스 뜨기'),(147,'지쳤어요'),(147,'함뜨했어요'),(148,'성공했어요'),(149,'성공했어요'),(150,'함뜨했어요'),(151,'힘들어요'),(152,'완벽 해요'),(152,'함뜨했어요'),(153,'실수했어요'),(153,'힘들어요'),(154,'완벽 해요'),(154,'함뜨했어요'),(154,'뿌듯해요'),(154,'성공했어요'),(155,'함뜨했어요'),(156,'지쳤어요'),(156,'힘들어요'),(156,'성공했어요'),(156,'뿌듯해요'),(157,'지쳤어요'),(157,'실수했어요'),(157,'함뜨했어요'),(158,'함뜨했어요'),(158,'뿌듯해요'),(158,'힘들어요'),(158,'성공했어요'),(159,'완벽 해요'),(159,'성공했어요'),(160,'성공했어요'),(160,'뿌듯해요'),(161,'힘들어요'),(161,'지쳤어요'),(162,'함뜨했어요'),(162,'성공했어요'),(162,'힘들어요'),(163,'배색 뜨기'),(164,'성공했어요'),(164,'힘들어요'),(165,'지쳤어요'),(165,'무늬 뜨기'),(165,'뿌듯해요'),(166,'지쳤어요'),(167,'성공했어요'),(167,'뿌듯해요'),(168,'배색 뜨기'),(168,'실수했어요'),(168,'푸르시오'),(169,'완벽 해요'),(169,'성공했어요'),(170,'뿌듯해요'),(171,'무한 메리야스 뜨기'),(172,'지쳤어요'),(173,'성공했어요'),(173,'함뜨했어요'),(174,'힘들어요'),(174,'지쳤어요'),(175,'무늬 뜨기'),(176,'무늬 뜨기'),(176,'성공했어요'),(176,'뿌듯해요'),(177,'실이 부족해요'),(177,'힘들어요'),(178,'함뜨했어요'),(179,'실수했어요'),(179,'완벽 해요'),(180,'완벽 해요'),(181,'힘들어요'),(182,'완벽 해요'),(182,'성공했어요'),(183,'무늬 뜨기'),(185,'뿌듯해요'),(186,'힘들어요'),(187,'뿌듯해요'),(187,'성공했어요'),(187,'완벽 해요'),(191,'힘들어요');
/*!40000 ALTER TABLE `record_tags` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `kakao_id` bigint NOT NULL,
  `nickname` varchar(255) DEFAULT NULL,
  `profile_image_url` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_4tp32nb01jmfcirpipti37lfs` (`kakao_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'2025-06-07 10:59:30.810477',NULL,4294578011,'김광일','http://k.kakaocdn.net/dn/bKMNdk/btsM505wqO2/wWOH6lRotOrokQzhISCg81/img_640x640.jpg','2025-06-07 10:59:30.810492'),(2,'2025-06-07 10:59:30.810066',NULL,4250638508,'이지수','http://k.kakaocdn.net/dn/beLfG1/btsIbko0roq/HMpTHGKcNF39Wk3S70R0Sk/img_640x640.jpg','2025-06-07 10:59:30.810128'),(3,'2025-06-07 10:59:59.877568',NULL,4294980711,'문슬기','http://img1.kakaocdn.net/thumb/R640x640.q70/?fname=http://t1.kakaocdn.net/account_images/default_profile.jpeg','2025-06-07 10:59:59.877596'),(4,'2025-06-07 11:03:31.079884',NULL,4295012635,'한승현','http://k.kakaocdn.net/dn/jZ1E4/btsNdJbZA66/nyagNRxeEiRXAHt4g1A0UK/img_640x640.jpg','2025-06-07 11:03:31.079903'),(5,'2025-06-07 11:05:24.274844',NULL,4295024462,'박예지','http://k.kakaocdn.net/dn/bvRc22/btsL4Q5zfXM/iYfwlHJ95MUiqENPbciuhk/img_640x640.jpg','2025-06-07 11:05:24.274866'),(6,'2025-06-07 11:54:20.877696',NULL,4295101849,'이주희','http://k.kakaocdn.net/dn/QquSu/btsMt5fZ5c5/mJKMJcYrg546TtHFt4HksK/img_640x640.jpg','2025-06-07 11:54:20.877718'),(7,'2025-06-07 13:04:49.071314',NULL,4295185089,'한주은','http://img1.kakaocdn.net/thumb/R640x640.q70/?fname=http://t1.kakaocdn.net/account_images/default_profile.jpeg','2025-06-07 13:04:49.071333'),(8,'2025-06-07 13:54:15.221916',NULL,4247308343,'정혜영','http://img1.kakaocdn.net/thumb/R640x640.q70/?fname=http://t1.kakaocdn.net/account_images/default_profile.jpeg','2025-06-07 13:54:15.221946'),(9,'2025-06-07 17:06:49.595192',NULL,4295380849,'곽수민','http://k.kakaocdn.net/dn/bGCplh/btrGtghsaTZ/lK90UUiZJOR7C08j2V7Jkk/img_640x640.jpg','2025-06-07 17:06:49.595208'),(10,'2025-06-08 02:52:01.999221',NULL,4294980640,'김현지','http://img1.kakaocdn.net/thumb/R640x640.q70/?fname=http://t1.kakaocdn.net/account_images/default_profile.jpeg','2025-06-08 02:52:02.000553'),(11,'2025-06-09 00:49:41.381221',NULL,4259498654,'안정우','http://img1.kakaocdn.net/thumb/R640x640.q70/?fname=http://t1.kakaocdn.net/account_images/default_profile.jpeg','2025-06-09 00:49:41.381234'),(12,'2025-06-09 09:15:39.502471',NULL,4294996524,'선령','http://img1.kakaocdn.net/thumb/R640x640.q70/?fname=http://t1.kakaocdn.net/account_images/default_profile.jpeg','2025-06-09 09:15:39.502486');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-07-17  1:49:52

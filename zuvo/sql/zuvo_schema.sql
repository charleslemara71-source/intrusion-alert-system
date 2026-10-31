-- =====================================================================
--  ZUVO  -  MySQL database creation script
--  Run this in MySQL Workbench (File > Open SQL Script > Run) BEFORE
--  starting the Spring Boot app for the first time.
--  (Spring Boot also keeps the tables up to date with ddl-auto=update.)
-- =====================================================================
CREATE DATABASE IF NOT EXISTS zuvo_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE zuvo_db;

CREATE TABLE IF NOT EXISTS users (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  full_name        VARCHAR(120) NOT NULL,
  date_of_birth    DATE NOT NULL,
  gender           VARCHAR(20),
  email            VARCHAR(160) NOT NULL UNIQUE,
  password_hash    VARCHAR(100) NOT NULL,
  email_verified   BIT(1) NOT NULL DEFAULT b'0',
  profile_pic_url  VARCHAR(500),
  bio              VARCHAR(300),
  role             VARCHAR(20) NOT NULL DEFAULT 'USER',
  created_at       DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS email_verifications (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NOT NULL,
  code_hash   VARCHAR(100) NOT NULL,
  expires_at  DATETIME(6) NOT NULL,
  attempts    INT NOT NULL DEFAULT 0,
  used        BIT(1) NOT NULL DEFAULT b'0',
  created_at  DATETIME(6) NOT NULL,
  INDEX idx_ev_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS follows (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  follower_id   BIGINT NOT NULL,
  following_id  BIGINT NOT NULL,
  status        VARCHAR(12) NOT NULL,          -- PENDING, ACCEPTED, DECLINED
  created_at    DATETIME(6) NOT NULL,
  responded_at  DATETIME(6),
  UNIQUE KEY uq_follow (follower_id, following_id),
  INDEX idx_follow_following (following_id, status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS posts (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id          BIGINT NOT NULL,
  type             VARCHAR(10) NOT NULL,        -- PHOTO, VIDEO, REEL, QUOTE
  media_url        VARCHAR(600),
  media_public_id  VARCHAR(300),
  resource_type    VARCHAR(20),
  caption          VARCHAR(1000),
  category         VARCHAR(30),
  visibility       VARCHAR(12) NOT NULL,        -- FOLLOWERS or PUBLIC
  curated          BIT(1) NOT NULL DEFAULT b'0',
  created_at       DATETIME(6) NOT NULL,
  INDEX idx_posts_user (user_id, created_at),
  INDEX idx_posts_vis (visibility, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS comments (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id     BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  body        VARCHAR(1000) NOT NULL,
  created_at  DATETIME(6) NOT NULL,
  INDEX idx_comments_post (post_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS post_likes (
  id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id  BIGINT NOT NULL,
  user_id  BIGINT NOT NULL,
  UNIQUE KEY uq_like (post_id, user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS messages (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  sender_id    BIGINT NOT NULL,
  receiver_id  BIGINT NOT NULL,
  content      VARCHAR(2000) NOT NULL,
  created_at   DATETIME(6) NOT NULL,
  seen         BIT(1) NOT NULL DEFAULT b'0',
  INDEX idx_msg_pair (sender_id, receiver_id, created_at),
  INDEX idx_msg_receiver (receiver_id, seen)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_interests (
  id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id   BIGINT NOT NULL,
  category  VARCHAR(30) NOT NULL,
  INDEX idx_int_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS live_streams (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  host_id     BIGINT NOT NULL,
  room_name   VARCHAR(80) NOT NULL,
  title       VARCHAR(120),
  status      VARCHAR(10) NOT NULL,             -- LIVE or ENDED
  started_at  DATETIME(6) NOT NULL,
  ended_at    DATETIME(6),
  INDEX idx_live_status (status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS live_requests (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  stream_id   BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  status      VARCHAR(10) NOT NULL,             -- PENDING, ACCEPTED, DECLINED
  created_at  DATETIME(6) NOT NULL,
  UNIQUE KEY uq_live_req (stream_id, user_id)
) ENGINE=InnoDB;

-- member_profile.profile definition
DROP TABLE IF EXISTS `profile`;

CREATE TABLE `profile` (
  `username` varchar(100) NOT NULL COMMENT 'DMS member username, unique, primary key',
  `avatar_url` varchar(2083) DEFAULT NULL COMMENT 'DMS profile avatar url',
  `discourse_username` varchar(100) DEFAULT NULL COMMENT 'The discourse username associated with the member',
  `discourse_avatar_url` varchar(2083) DEFAULT NULL COMMENT 'Discourse avatar url',
  `discord_userid` varchar(100) DEFAULT NULL COMMENT 'The discord userid associated with the member',
  `attributes` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'Misc attributes' CHECK (json_valid(`attributes`)),
  PRIMARY KEY (`username`),
  UNIQUE KEY `profile_discourse_username_unique` (`discourse_username`),
  UNIQUE KEY `profile_discord_userid_unique` (`discord_userid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Table to store basic profile level data for DMS members';
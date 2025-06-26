-- member_profile.profile definition

CREATE TABLE `profile` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '_id is autoincrement id assigned to a profile, this column is used as Foreign Key in other tables. First 100 are reserved for system profiles. primary key',
  `username` varchar(100) NOT NULL COMMENT 'DMS member username, unique',
  `avatar_url` varchar(2083) DEFAULT NULL COMMENT 'DMS profile avatar url',
  `discourse_username` varchar(100) DEFAULT NULL COMMENT 'The discourse username associated with the member, unique',
  `discourse_avatar_url` varchar(2083) DEFAULT NULL COMMENT 'Discourse avatar url',
  `discord_userid` varchar(100) DEFAULT NULL COMMENT 'The discord userid associated with the member, unique',
  `attributes` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'Misc attributes' CHECK (json_valid(`attributes`)),
  `created` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE current_timestamp(),
  `is_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Represents if the member account was enabled when it was last fetched from AD',
  PRIMARY KEY (`id`),
  UNIQUE KEY `profile_username_unique` (`username`),
  UNIQUE KEY `profile_discourse_username_unique` (`discourse_username`),
  UNIQUE KEY `profile_discord_userid_unique` (`discord_userid`),
  KEY `profile_isEnabled_IDX` (`is_enabled`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=110 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Table to store basic profile level data for DMS members';

-- member_profile.activity_log definition

DROP TABLE IF EXISTS `activity_log`;
CREATE TABLE `activity_log` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id is auto increment primary key.',
  `source` tinyint(3) unsigned NOT NULL COMMENT 'the source system that generated the activity log',
  `actor_profile_row_id` int(10) unsigned DEFAULT NULL COMMENT 'the actor that tiggered the log, foreign_key to profile table',
  `subject_profile_row_id` int(10) unsigned DEFAULT NULL COMMENT 'the actor that tiggered the log, foreign_key to profile table',
  `event` tinyint(3) unsigned NOT NULL COMMENT 'the event being logged, this has a different meaning for each source',
  `attributes` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'Event attributes' CHECK (json_valid(`attributes`)),
  `created` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `activity_log_actor_profile_FK` (`actor_profile_row_id`),
  KEY `activity_log_subject_profile_FK` (`subject_profile_row_id`),
  CONSTRAINT `activity_log_actor_profile_FK` FOREIGN KEY (`actor_profile_row_id`) REFERENCES `profile` (`id`),
  CONSTRAINT `activity_log_subject_profile_FK` FOREIGN KEY (`subject_profile_row_id`) REFERENCES `profile` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Table to store activity log of various events for members, usually write ops performed on a member profile';

-- member_profile.groups definition

DROP TABLE IF EXISTS `groups`;
CREATE TABLE `groups` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id is auto increment primary key.',
  `name` varchar(255) NOT NULL COMMENT 'The name of the group',
  `dn` varchar(512) NOT NULL COMMENT 'The distinguished name (DN) of the group in Active Directory',
  `created` timestamp NOT NULL DEFAULT current_timestamp() COMMENT 'The timestamp when the record was created in our database',
  PRIMARY KEY (`id`),
  UNIQUE KEY `groups_name_UNQ` (`name`),
  UNIQUE KEY `groups_dn_UNQ` (`dn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Reference table for groups in Active Directory';

-- member_profile.group_history definition

DROP TABLE IF EXISTS `group_history`;
CREATE TABLE `group_history` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id is auto increment primary key.',
  `actor_id` int(10) unsigned NOT NULL COMMENT 'The ID of the user who performed the group action',
  `member_id` int(10) unsigned NOT NULL COMMENT 'The ID of the member affected by the group action',
  `group_id` int(10) unsigned NOT NULL COMMENT 'The ID of the group that was modified',
  `event_timestamp` timestamp NOT NULL COMMENT 'The timestamp when the event occurred in Active Directory',
  `created` timestamp NOT NULL DEFAULT current_timestamp() COMMENT 'The timestamp when the record was created in our database',
  PRIMARY KEY (`id`),
  KEY `group_history_actor_id_IDX` (`actor_id`) USING BTREE,
  KEY `group_history_member_id_IDX` (`member_id`) USING BTREE,
  KEY `group_history_group_id_IDX` (`group_id`) USING BTREE,
  KEY `group_history_event_timestamp_IDX` (`event_timestamp`) USING BTREE,
  CONSTRAINT `group_history_actor_FK` FOREIGN KEY (`actor_id`) REFERENCES `profile` (`id`),
  CONSTRAINT `group_history_member_FK` FOREIGN KEY (`member_id`) REFERENCES `profile` (`id`),
  CONSTRAINT `group_history_group_FK` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Table to store history of group membership changes from Active Directory';

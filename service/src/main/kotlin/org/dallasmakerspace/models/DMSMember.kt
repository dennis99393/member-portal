package org.dallasmakerspace.models

import org.dallasmakerspace.members.db.ProfileDAO

/** Converter function to create a DMSMember from a ProfileDAO. */
fun daoToProfileModel(dao: ProfileDAO) =
    DMSMember(
        id = dao.idColumn.value,
        username = dao.id.value,
        enabled = dao.isEnabled,
        avatarUrl = dao.avatarUrl,
        discourseUsername = dao.discourseUsername,
        discourseAvatarUrl = dao.discourseAvatarUrl,
        discordUserId = dao.discordUserId,
        discordUsername = dao.discordUsername,
        discordAvatarUrl = dao.discordAvatarUrl,
        discordWebhookId = dao.discordWebhookId)

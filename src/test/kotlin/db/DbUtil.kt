package com.neelkamath.omniChat.db

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteAll

private val tables: List<Table> = listOf(
    Contacts,
    GroupChatUsers,
    GroupChats,
    PrivateChatDeletions,
    PrivateChats,
    MessageStatuses,
    Messages,
    Chats,
    Users
)

/** Deletes every row from every table created. */
fun wipeDb(): Unit = transact {
    tables.forEach { it.deleteAll() }
}

/** Drops every table and type created. */
fun tearDownDb() {
    dropTables()
    dropTypes()
}

private fun dropTables(): Unit = transact { SchemaUtils.drop(*tables.toTypedArray()) }

private fun dropTypes(): Unit = transact { exec("DROP TYPE IF EXISTS message_status;") }
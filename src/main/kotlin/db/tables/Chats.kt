package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.tables.Chats.id
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

/** The [id]s of [PrivateChats] and [GroupChats]. */
object Chats : IntIdTable() {
    /** Creates a chat ID. */
    fun create(): Int = transaction {
        insertAndGetId {}.value
    }

    fun delete(chatId: Int): Unit = transaction {
        deleteWhere { Chats.id eq chatId }
    }

    fun isExisting(chatId: Int): Boolean = transaction { select(Chats.id eq chatId).empty().not() }
}

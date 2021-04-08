package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.graphql.routing.MessageText
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/** @see [Messages] */
object TextMessages : Table() {
    override val tableName = "text_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val text: Column<String> = varchar("text", MessageText.MAX_LENGTH)

    /** @see [Messages.createTextMessage] */
    fun create(id: Int, text: MessageText): Unit = transaction {
        insert {
            it[this.messageId] = id
            it[this.text] = text.value
        }
    }

    fun read(id: Int): MessageText = transaction { select(messageId eq id).first()[text].let(::MessageText) }

    fun delete(idList: Collection<Int>): Unit = transaction {
        deleteWhere { messageId inList idList }
    }
}

package com.neelkamath.omniChat.db.tables

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/** @see [PollOptions] */
object PollVotes : Table() {
    override val tableName = "poll_votes"
    private val userId: Column<Int> = integer("user_id").references(Users.id)
    private val optionId: Column<Int> = integer("option_id").references(PollOptions.id)

    /** Creates a vote for the [userId] on the [optionId] if they haven't already. */
    fun create(userId: Int, optionId: Int) {
        if (!exists(userId, optionId))
            transaction {
                insert {
                    it[this.userId] = userId
                    it[this.optionId] = optionId
                }
            }
    }

    private fun exists(userId: Int, optionId: Int): Boolean = transaction {
        select { (PollVotes.userId eq userId) and (PollVotes.optionId eq optionId) }.empty().not()
    }

    fun read(optionId: Int): List<Int> = transaction {
        select { PollVotes.optionId eq optionId }.map { it[userId] }
    }

    /** Deletes the [userId]'s vote on the [optionId] if it exists. */
    fun deleteVote(userId: Int, optionId: Int): Unit = transaction {
        deleteWhere { (PollVotes.userId eq userId) and (PollVotes.optionId eq optionId) }
    }

    fun deleteVotes(optionIdList: List<Int>): Unit = transaction {
        deleteWhere { optionId inList optionIdList }
    }
}
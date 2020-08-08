package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.createUser
import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.deleteUser
import com.neelkamath.omniChat.graphql.routing.AccountEdge
import com.neelkamath.omniChat.graphql.routing.AccountsConnection
import com.neelkamath.omniChat.graphql.routing.Bio
import com.neelkamath.omniChat.graphql.routing.UpdatedOnlineStatus
import com.neelkamath.omniChat.searchUsers
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*

data class User(
    val id: Int,
    val uuid: UUID,
    val isOnline: Boolean,
    /** Ignore if [isOnline]. `null` if the user's never been online. */
    val lastOnline: LocalDateTime?,
    val bio: Bio?,
    val pic: Pic?
)

/** Replacement for Keycloak's cumbersome custom user attributes. Pics cannot exceed [Pic.MAX_BYTES]. */
object Users : IntIdTable() {
    /** The ID given to the user by Keycloak. */
    private val uuid: Column<UUID> = uuid("uuid")

    private val picId: Column<Int?> = integer("pic_id").references(Pics.id).nullable()
    private val bio: Column<String?> = varchar("bio", Bio.MAX_LENGTH).nullable()
    private val isOnline: Column<Boolean> = bool("is_online").clientDefault { false }
    private val lastOnline: Column<LocalDateTime?> = datetime("last_online").nullable()

    /**
     * @see [createUser]
     * @see [setOnlineStatus]
     * @see [updatePic]
     */
    fun create(uuid: String, bio: Bio?): Unit = transaction {
        insert {
            it[this.uuid] = UUID.fromString(uuid)
            it[this.bio] = bio?.value
        }
    }

    fun exists(id: Int): Boolean = transaction {
        select { Users.id eq id }.empty().not()
    }

    fun read(id: Int): User = transaction {
        select { Users.id eq id }.first()
    }.let(::buildUser)

    fun read(uuid: String): User = transaction {
        select { Users.uuid eq UUID.fromString(uuid) }.first()
    }.let(::buildUser)

    private fun buildUser(row: ResultRow): User = User(
        row[id].value,
        row[uuid],
        row[isOnline],
        row[lastOnline],
        row[bio]?.let(::Bio),
        row[picId]?.let(Pics::read)
    )

    /**
     * Notifies subscribers of the [UpdatedOnlineStatus] only if [isOnline] differs from the [userId]'s current status.
     */
    fun setOnlineStatus(userId: Int, isOnline: Boolean): Unit = transaction {
        if (select { Users.id eq userId }.first()[Users.isOnline] == isOnline) return@transaction
        update({ Users.id eq userId }) {
            it[Users.isOnline] = isOnline
            it[lastOnline] = LocalDateTime.now()
        }
        val subscribers = Contacts.readOwners(userId).plus(readChatSharers(userId)).map(::OnlineStatusesAsset)
        onlineStatusesNotifier.publish(UpdatedOnlineStatus(userId, isOnline), subscribers)
    }

    /** Calls [negotiateUserUpdate]. */
    fun updateBio(userId: Int, bio: Bio?) {
        transaction {
            update({ Users.id eq userId }) { it[Users.bio] = bio?.value }
        }
        negotiateUserUpdate(userId)
    }

    /** Deletes the [pic] if it's `null`. Calls [negotiateUserUpdate]. */
    fun updatePic(userId: Int, pic: Pic?) {
        transaction {
            val op = Users.id eq userId
            update({ op }) { it[this.picId] = null }
            val picId = select(op).first()[picId]
            update({ op }) { it[this.picId] = Pics.update(picId, pic) }
        }
        negotiateUserUpdate(userId)
    }

    private fun readPrimaryKey(userId: Int): Int = transaction {
        select { Users.id eq userId }.first()[Users.id].value
    }

    /**
     * Case-insensitively [query]s every user's username, first name, last name, and email address.
     *
     * @see [searchUsers]
     */
    fun search(query: String, pagination: ForwardPagination? = null): AccountsConnection {
        val users = searchUsers(query).map { AccountEdge(it, readPrimaryKey(it.id)) }
        return AccountsConnection.build(users, pagination)
    }

    /**
     * Deletes the specified user from this table.
     *
     * @see [deleteUser]
     */
    fun delete(id: Int): Unit = transaction {
        deleteWhere { Users.id eq id }
    }
}
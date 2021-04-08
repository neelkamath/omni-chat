package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.Pic
import com.neelkamath.omniChatBackend.db.PostgresEnum
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Used for profile pics, and group chat pics.
 *
 * @see [PicMessages]
 */
object Pics : IntIdTable() {
    private val original: Column<ByteArray> = binary("original", Pic.ORIGINAL_MAX_BYTES)
    private val thumbnail: Column<ByteArray> = binary("thumbnail", Pic.THUMBNAIL_MAX_BYTES)
    private val type: Column<Pic.Type> = customEnumeration(
        name = "type",
        sql = "pic_type",
        fromDb = { Pic.Type.valueOf((it as String).toUpperCase()) },
        toDb = { PostgresEnum("pic_type", it) },
    )

    /** Returns the ID of the pic. */
    fun create(pic: Pic): Int = transaction {
        insertAndGetId {
            it[original] = pic.original
            it[thumbnail] = pic.thumbnail
            it[type] = pic.type
        }.value
    }

    fun read(id: Int): Pic =
        transaction { select(Pics.id eq id).first() }.let { Pic(it[type], it[original], it[thumbnail]) }

    /**
     * Returns the [pic]'s [id] after updating it.
     *
     * - If the [id] is an [Int], and the [pic] is a [Pic], the pic will be updated, and its ID will be returned.
     * - If the [id] is an [Int], and the [pic] is `null`, the pic will be deleted, and `null` will be returned.
     * - If the [id] is `null`, and the [pic] is a [Pic], the pic will be created, and its ID will be returned.
     * - If the [id] is `null`, and the [pic] is `null`, `null` will be returned.
     */
    fun update(id: Int?, pic: Pic?): Int? = when {
        id != null && pic != null -> {
            transaction {
                update({ Pics.id eq id }) {
                    it[original] = pic.original
                    it[thumbnail] = pic.thumbnail
                    it[type] = pic.type
                }
            }
            id
        }
        id != null && pic == null -> {
            delete(id)
            null
        }
        id == null && pic != null -> create(pic)
        id == null && pic == null -> null
        else -> throw NoWhenBranchMatchedException()
    }

    fun delete(id: Int): Unit = transaction {
        deleteWhere { Pics.id eq id }
    }
}

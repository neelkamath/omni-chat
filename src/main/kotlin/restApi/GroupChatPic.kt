package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.db.tables.Chats
import com.neelkamath.omniChat.db.tables.GroupChatUsers
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.userId
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

fun routeGroupChatPic(routing: Routing): Unit = with(routing) {
    route("group-chat-pic") {
        getGroupChatPic(this)
        authenticate { patchGroupChatPic(this) }
    }
}

private fun getGroupChatPic(route: Route): Unit = with(route) {
    get {
        val chatId = call.parameters["chat-id"]!!.toInt()
        val type = PicType.valueOf(call.parameters["pic-type"]!!)
        if (!Chats.exists(chatId)) call.respond(HttpStatusCode.BadRequest)
        else {
            val pic = GroupChats.readPic(chatId)
            if (pic == null) call.respond(HttpStatusCode.NoContent)
            else
                when (type) {
                    PicType.ORIGINAL -> call.respondBytes(pic.original)
                    PicType.THUMBNAIL -> call.respondBytes(pic.thumbnail)
                }
        }
    }
}

private fun patchGroupChatPic(route: Route): Unit = with(route) {
    patch {
        val chatId = call.parameters["chat-id"]!!.toInt()
        val pic = readMultipartPic()
        when {
            pic == null -> call.respond(HttpStatusCode.BadRequest)
            !GroupChatUsers.isAdmin(call.userId!!, chatId) -> call.respond(HttpStatusCode.Unauthorized)
            else -> {
                GroupChats.updatePic(chatId, pic)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

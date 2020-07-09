package com.neelkamath.omniChat

fun NewAccount.toAccount(): Account =
    Account(readUserByUsername(username).id, username, emailAddress, bio, firstName, lastName)

fun buildNewGroupChat(userIdList: List<String>): NewGroupChat =
    NewGroupChat(GroupChatTitle("T"), GroupChatDescription(""), userIdList.toList())

fun buildNewGroupChat(vararg userIdList: String): NewGroupChat = buildNewGroupChat(userIdList.toList())
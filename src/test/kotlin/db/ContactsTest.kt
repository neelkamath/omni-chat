package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.createVerifiedUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ContactsTest : FunSpec({
    context("create(String, Set<String>)") {
        test("Saving contacts should ignore existing contacts") {
            val (userId, contact1Id, contact2Id, contact3Id) = createVerifiedUsers(4).map { it.info.id }
            Contacts.create(userId, setOf(contact1Id, contact2Id))
            Contacts.create(userId, setOf(contact1Id, contact2Id, contact3Id))
            Contacts.readIdList(userId) shouldBe listOf(contact1Id, contact2Id, contact3Id)
        }
    }
})
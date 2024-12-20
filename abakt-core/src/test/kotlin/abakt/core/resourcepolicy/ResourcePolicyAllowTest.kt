package abakt.core.resourcepolicy

import abakt.core.ResourceAction
import abakt.core.resourcePolicy
import abakt.core.uuid
import abakt.test.withPrincipal
import abakt.test.withResource
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ResourcePolicyAllowTest : StringSpec({

    "allow - single action" {
        data class User(val id: String)
        data class Document(val ownerId: String)

        val actionWrite = ResourceAction<Document>("write")
        val resourcePolicy = resourcePolicy<User, Document> {
            allow(actionWrite) { principal.id == resource.ownerId }
        }

        val ownerId = uuid()
        resourcePolicy.withResource(Document(ownerId)) {
            User(ownerId) shouldBeAllowed actionWrite
            User(uuid()) shouldNotBeAllowed actionWrite

            // Non specified actions should be denied
            User(ownerId) shouldNotBeAllowed ResourceAction<Document>("delete")
        }
    }

    "allow - multiple actions" {
        data class User(val id: String)
        data class Document(val ownerId: String)

        val actionWrite = ResourceAction<Document>("write")
        val actionDelete = ResourceAction<Document>("delete")
        val resourcePolicy = resourcePolicy<User, Document> {
            allow(actionWrite, actionDelete) { principal.id == resource.ownerId }
        }

        val ownerId = uuid()
        resourcePolicy.withPrincipal(User(ownerId)) {
            actionWrite shouldBeAllowedOn Document(ownerId)
            actionDelete shouldBeAllowedOn Document(ownerId)
        }

        resourcePolicy.withPrincipal(User(uuid())) {
            actionWrite shouldBeDeniedOn Document(ownerId)
            actionDelete shouldBeDeniedOn Document(ownerId)
        }
    }

    "allowAll" {
        data class User(val id: String)
        data class Document(val ownerId: String)

        val resourcePolicy = resourcePolicy<User, Document> {
            allowAll { principal.id == resource.ownerId }
        }

        val ownerId = uuid()
        val actionWrite = ResourceAction<Document>("write")
        val actionDelete = ResourceAction<Document>("delete")
        resourcePolicy.withResource(Document(ownerId)) {
            User(ownerId) shouldBeAllowed actionWrite
            User(ownerId) shouldBeAllowed actionDelete
            User(ownerId) shouldBeAllowed ResourceAction("somethingElse")
            User(uuid()) shouldBeDenied actionWrite
            User(uuid()) shouldBeDenied actionDelete
        }
    }

    "alwaysAllow" {
        class User
        class Document

        val actionRead = ResourceAction<Document>("read")
        val actionWrite = ResourceAction<Document>("write")
        val resourcePolicy = resourcePolicy<User, Document> {
            alwaysAllow(actionRead, actionWrite)
        }

        resourcePolicy.withResource(Document()) {
            User() shouldBeAllowed actionRead
            User() shouldBeAllowed actionWrite
        }
    }

    "allow should fail when no actions are specified" {
        shouldThrowWithMessage<IllegalArgumentException>("At least one action must be specified") {
            resourcePolicy<String, String> {
                allow { true }
            }
        }
    }

    "alwaysAllow should fail when no actions are specified" {
        shouldThrowWithMessage<IllegalArgumentException>("At least one action must be specified") {
            resourcePolicy<String, String> {
                alwaysAllow()
            }
        }
    }

    "allowed returns the expected value when an action is allowed/denied" {
        data class User(val id: String)
        data class Document(val ownerId: String)

        val actionRead = ResourceAction<Document>("read")
        val actionWrite = ResourceAction<Document>("write")
        val resourcePolicy = resourcePolicy<User, Document> {
            alwaysAllow(actionRead)
            allow(actionWrite) { principal.id == resource.ownerId }
        }

        // Owner is allowed to read/write
        resourcePolicy.allowed(User("owner"), Document("owner"), actionRead) shouldBe true
        resourcePolicy.allowed(User("owner"), Document("owner"), actionWrite) shouldBe true
        resourcePolicy.allowed(User("owner"), Document("owner"), actionRead, actionWrite) shouldBe true

        // Other users can only read
        resourcePolicy.allowed(User(uuid()), Document("owner"), actionRead) shouldBe true
        resourcePolicy.allowed(User(uuid()), Document("owner"), actionWrite) shouldBe false
        resourcePolicy.allowed(User(uuid()), Document("owner"), actionRead, actionWrite) shouldBe false
    }
})

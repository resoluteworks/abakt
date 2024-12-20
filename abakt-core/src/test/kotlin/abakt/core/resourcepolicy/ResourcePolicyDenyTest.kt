package abakt.core.resourcepolicy

import abakt.core.ResourceAction
import abakt.core.resourcePolicy
import abakt.test.shouldAllow
import abakt.test.shouldDeny
import abakt.test.withResource
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.StringSpec

class ResourcePolicyDenyTest : StringSpec({

    "deny - single action" {
        class User
        data class Document(val locked: Boolean)

        val actionRead = ResourceAction<Document>("read")
        val actionWrite = ResourceAction<Document>("write")
        val resourcePolicy = resourcePolicy<User, Document> {
            alwaysAllow(actionRead, actionWrite)
            deny(actionWrite) { r.locked }
        }

        resourcePolicy.withResource(Document(locked = false)) {
            User() shouldBeAllowed actionRead
            User() shouldBeAllowed actionWrite
        }

        resourcePolicy.withResource(Document(locked = true)) {
            User() shouldBeAllowed actionRead
            User() shouldBeDenied actionWrite
        }
    }

    "deny - multiple actions" {
        class User
        data class Document(val locked: Boolean)

        val actionRead = ResourceAction<Document>("read")
        val actionWrite = ResourceAction<Document>("write")
        val actionDelete = ResourceAction<Document>("delete")

        val resourcePolicy = resourcePolicy<User, Document> {
            alwaysAllow(actionRead, actionWrite, actionDelete)
            deny(actionWrite, actionDelete) { resource.locked }
        }

        resourcePolicy.withResource(Document(locked = false)) {
            User() shouldBeAllowed actionRead
            User() shouldBeAllowed actionWrite
            User() shouldBeAllowed actionDelete
        }

        resourcePolicy.withResource(Document(locked = true)) {
            User() shouldBeAllowed actionRead
            User() shouldBeDenied actionWrite
            User() shouldBeDenied actionDelete
        }
    }

    "deny takes precedence over allow" {
        data class User(val id: String)
        data class Document(val ownerId: String, val locked: Boolean)

        val actionWrite = ResourceAction<Document>("write")

        val resourcePolicy = resourcePolicy<User, Document> {
            allow(actionWrite) { p.id == r.ownerId }
            deny(actionWrite) { resource.locked }
        }

        resourcePolicy.shouldAllow(User("owner"), Document("owner", locked = false), actionWrite)
        resourcePolicy.shouldDeny(User("owner"), Document("owner", locked = true), actionWrite)
    }

    "denyAll" {
        data class User(val id: String)
        data class Document(val ownerId: String, val locked: Boolean)

        val actionRead = ResourceAction<Document>("read")
        val actionWrite = ResourceAction<Document>("write")

        val resourcePolicy = resourcePolicy<User, Document> {
            alwaysAllow(actionRead, actionWrite)
            denyAll { resource.locked }
        }

        resourcePolicy.withResource(Document("owner", locked = false)) {
            User("owner") shouldBeAllowed actionRead
            User("owner") shouldBeAllowed actionWrite
        }

        resourcePolicy.withResource(Document("owner", locked = true)) {
            User("owner") shouldBeDenied actionRead
            User("owner") shouldBeDenied actionWrite
        }
    }

    "deny should fail when no actions are specified" {
        shouldThrowWithMessage<IllegalArgumentException>("At least one action must be specified") {
            resourcePolicy<String, String> {
                deny { true }
            }
        }
    }
})

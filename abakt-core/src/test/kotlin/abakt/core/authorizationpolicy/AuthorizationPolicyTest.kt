package abakt.core.authorizationpolicy

import abakt.core.PermissionDeniedException
import abakt.core.ResourceAction
import abakt.core.authorizationPolicy
import abakt.core.uuid
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AuthorizationPolicyTest : StringSpec({

    "allowed should return the correct value" {
        val policy = authorizationPolicy<User> {
            resource<Document> {
                derivedRole("owner") { p.id == r.ownerId }
                alwaysAllow(Document.read)
                allow(Document.write) { hasDerivedRole("owner") }
            }
        }

        // Owner can read/write
        policy.allowed(User("owner"), Document("owner"), Document.read) shouldBe true
        policy.allowed(User("owner"), Document("owner"), Document.write) shouldBe true
        policy.allowed(User("owner"), Document("owner"), Document.read, Document.write) shouldBe true

        // Other users can only read
        policy.allowed(User("someOtherUser"), Document("owner"), Document.read) shouldBe true
        policy.allowed(User("someOtherUser"), Document("owner"), Document.write) shouldBe false
        policy.allowed(User("someOtherUser"), Document("owner"), Document.read, Document.write) shouldBe false
    }

    "check should throw exception when an action is not allowed" {
        val policy = authorizationPolicy<User> {
            resource<Document> {
                derivedRole("owner") { principal.id == resource.ownerId }
                alwaysAllow(Document.read)
                allow(Document.write) { hasDerivedRole("owner") }
            }
        }

        shouldThrow<PermissionDeniedException> {
            policy.check(User("someOtherUser"), Document("owner"), Document.write)
        }
    }

    "check should not throw an exception when an action is allowed" {
        val policy = authorizationPolicy<User> {
            resource<Document> {
                derivedRole("owner") { principal.id == resource.ownerId }
                alwaysAllow(Document.read)
                allow(Document.write) { hasDerivedRole("owner") }
            }
        }

        shouldNotThrowAny {
            policy.check(User("owner"), Document("owner"), Document.write)
        }
    }

    "check block - allowed" {
        val policy = authorizationPolicy<User> {
            resource<Document> {
                derivedRole("owner") { principal.id == resource.ownerId }
                alwaysAllow(Document.read)
                allow(Document.write) { hasDerivedRole("owner") }
            }
        }

        val document = Document("owner")
        shouldNotThrowAny {
            policy.check {
                User("owner").can(Document.read, document)
                User("owner") can Document.write(document)
            }
        }
    }

    "check block - denied" {
        val policy = authorizationPolicy<User> {
            resource<Document> {
                derivedRole("owner") { principal.id == resource.ownerId }
                alwaysAllow(Document.read)
                allow(Document.write) { hasDerivedRole("owner") }
            }
        }

        val document = Document("owner")
        shouldThrow<PermissionDeniedException> {
            policy.check {
                User(uuid()) can Document.write(document)
            }
        }

        // Throws an exception even when a check passes
        shouldThrow<PermissionDeniedException> {
            policy.check {
                User(uuid()) can Document.read(document)
                User(uuid()) can Document.write(document)
            }
        }
    }

    "should throw exception when checking permissions for a resource type that isn't defined" {
        val policy = authorizationPolicy<User> {
            resource<Document> {
                derivedRole("owner") { principal.id == resource.ownerId }
                alwaysAllow(Document.read)
                allow(Document.write) { hasDerivedRole("owner") }
            }
        }

        data class SomeOtherResource(val ownerId: String)
        shouldThrowWithMessage<IllegalArgumentException>("No policies found for class ${SomeOtherResource::class}") {
            policy.check(User("owner"), SomeOtherResource("owner"), ResourceAction<SomeOtherResource>("read"))
        }
    }
}) {

    private data class User(val id: String)
    private data class Document(val ownerId: String) {
        companion object {
            val read = ResourceAction<Document>("read")
            val write = ResourceAction<Document>("write")
        }
    }
}

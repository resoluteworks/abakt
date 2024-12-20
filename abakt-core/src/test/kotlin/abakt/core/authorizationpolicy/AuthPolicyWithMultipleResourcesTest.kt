package abakt.core.authorizationpolicy

import abakt.core.ResourceAction
import abakt.core.authorizationPolicy
import abakt.core.resourcePolicy
import abakt.test.shouldAllow
import abakt.test.withPrincipal
import abakt.test.withResource
import io.kotest.core.spec.style.StringSpec

class AuthPolicyWithMultipleResourcesTest : StringSpec() {

    init {
        "policy with multiple resources" {

            val folderPolicy = resourcePolicy<User, Folder> {
                derivedRole("owner") { principal.id == resource.ownerId }
                alwaysAllow(Folder.read, Folder.write)
                allow(Folder.delete) { hasDerivedRole("owner") }
            }

            val policy = authorizationPolicy<User> {
                resource<Document> {
                    derivedRole("owner") { principal.id == resource.ownerId }
                    alwaysAllow(Document.read)
                    allow(Document.write) { hasDerivedRole("owner") }
                }
                resource(folderPolicy)
            }

            policy.shouldAllow(User("owner"), Document("owner"), Document.write)

            policy.withResource(Document("owner")) {
                User("owner") shouldBeAllowed Document.read
                User("owner") shouldBeAllowed Document.write
                User("otherUser") shouldBeAllowed Document.read
                User("otherUser") shouldBeDenied Document.write
            }

            policy.withPrincipal(User("owner")) {
                Folder.read shouldBeAllowedOn Folder("owner")
                Folder.write shouldBeAllowedOn Folder("owner")
                Folder.delete shouldBeAllowedOn Folder("owner")
            }

            policy.withResource(Folder("owner")) {
                User("otherUser") shouldBeAllowed Folder.read
                User("otherUser") shouldBeAllowed Folder.write
                User("otherUser") shouldBeDenied Folder.delete
            }
        }
    }

    private data class User(val id: String)
    private data class Document(val ownerId: String) {
        companion object {
            val read = ResourceAction<Document>("read")
            val write = ResourceAction<Document>("write")
        }
    }

    private data class Folder(val ownerId: String) {
        companion object {
            val read = ResourceAction<Folder>("read")
            val write = ResourceAction<Folder>("write")
            val delete = ResourceAction<Folder>("delete")
        }
    }
}

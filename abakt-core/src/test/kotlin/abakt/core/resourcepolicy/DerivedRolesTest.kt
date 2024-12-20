package abakt.core.resourcepolicy

import abakt.core.ResourceAction
import abakt.core.resourcePolicy
import abakt.core.uuid
import abakt.test.shouldAllow
import abakt.test.shouldNotAllow
import io.kotest.core.spec.style.StringSpec

class DerivedRolesTest : StringSpec({

    "document owner" {
        data class Principal(val id: String = uuid())
        data class Document(
            val createdBy: String,
            val collaborators: Set<String> = emptySet()
        )

        val policy = resourcePolicy<Principal, Document> {
            derivedRole("owner") { principal.id == resource.createdBy }
            derivedRole("collaborator") { principal.id in resource.collaborators }

            // Anyone can read
            allow(ResourceAction("read")) { true }

            // Only owner and collaborators can write
            allow(ResourceAction("write")) { hasDerivedRole("owner") || hasDerivedRole("collaborator") }

            // Only the owner can archive it
            allow(ResourceAction("archive")) { "owner" in derivedRoles }
        }

        val ownerId = uuid()
        val collaboratorId = uuid()
        val owner = Principal(ownerId)
        val collaborator = Principal(collaboratorId)
        val document = Document(ownerId, setOf(collaboratorId))
        val someOtherUser = Principal()

        policy.shouldAllow(owner, document, ResourceAction("read"))
        policy.shouldAllow(collaborator, document, ResourceAction("read"))
        policy.shouldAllow(someOtherUser, document, ResourceAction("read"))

        policy.shouldAllow(owner, document, ResourceAction("write"))
        policy.shouldAllow(collaborator, document, ResourceAction("write"))
        policy.shouldNotAllow(someOtherUser, document, ResourceAction("write"))

        policy.shouldAllow(owner, document, ResourceAction("archive"))
        policy.shouldNotAllow(collaborator, document, ResourceAction("archive"))
        policy.shouldNotAllow(someOtherUser, document, ResourceAction("archive"))
    }
})

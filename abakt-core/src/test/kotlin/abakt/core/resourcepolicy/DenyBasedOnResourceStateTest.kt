package abakt.core.resourcepolicy

import abakt.core.ResourceAction
import abakt.core.resourcePolicy
import abakt.core.uuid
import abakt.test.shouldAllow
import abakt.test.shouldNotAllow
import io.kotest.core.spec.style.StringSpec

class DenyBasedOnResourceStateTest : StringSpec({

    "deny based on resource state" {
        data class Principal(val id: String = uuid())
        data class Document(val createdBy: String, val status: DocumentStatus)

        val policy = resourcePolicy<Principal, Document> {
            alwaysAllow(ResourceAction("read"))

            // Only the owner can delete
            allow(ResourceAction("delete")) { resource.createdBy == principal.id }

            // No one can delete a published document
            deny(ResourceAction("delete")) { resource.status == DocumentStatus.PUBLISHED }
        }

        val creatorId = uuid()
        val creator = Principal(creatorId)
        val someOtherUser = Principal()

        policy.shouldAllow(creator, Document(creatorId, DocumentStatus.DRAFT), ResourceAction("read"))
        policy.shouldAllow(someOtherUser, Document(creatorId, DocumentStatus.DRAFT), ResourceAction("read"))

        policy.shouldAllow(creator, Document(creatorId, DocumentStatus.DRAFT), ResourceAction("delete"))
        policy.shouldNotAllow(someOtherUser, Document(creatorId, DocumentStatus.DRAFT), ResourceAction("delete"))

        // Deny takes precedence, even when the owner is the creator
        policy.shouldNotAllow(creator, Document(creatorId, DocumentStatus.PUBLISHED), ResourceAction("delete"))
        policy.shouldNotAllow(someOtherUser, Document(creatorId, DocumentStatus.PUBLISHED), ResourceAction("delete"))
    }
}) {
    private enum class DocumentStatus { DRAFT, PUBLISHED }
}

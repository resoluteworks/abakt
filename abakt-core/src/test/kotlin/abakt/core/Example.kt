package abakt.core

import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.StringSpec

@Ignored
class Example : StringSpec({

    "example" {
        // The principal and resource classes. These are defined by the
        // client application and can be any types.
        data class Principal(val id: String, val role: String)
        data class Document(val ownerId: String, val locked: Boolean)

        // Actions that can be taken against a document
        val read = ResourceAction<Document>("read")
        val write = ResourceAction<Document>("write")
        val delete = ResourceAction<Document>("delete")

        // An authorization policy starts by specifying the principal type
        val authPolicy = authorizationPolicy<Principal> {

            // Add a resource policy to this authorization policy. An authorization
            // policy can have multiple resource policies.
            resource<Document> {

                // A definition of a derived role for this resource. A principal
                // is an "owner" if their ID matches the ownerId of the resource.
                derivedRole("owner") { resource.ownerId == principal.id }

                // Allow all principals to read resources of this type
                alwaysAllow(read)

                // Allow the resource owner to "write" resources of this type
                allow(write) { hasDerivedRole("owner") }

                // Only an admin or a manager can delete resources of this type
                allow(delete) { principal.role in setOf("ADMIN", "MANAGER") }

                // Deny all actions when a document is locked.
                denyAll { resource.locked }
            }
        }

        val principal = Principal(uuid(), "USER")
        val document = Document(uuid(), false)

        // Use check() to throw an exception when the operation is not permitted
        authPolicy.check(principal, document, write)

        // Same as above but with a more expressive syntax
        authPolicy.check { principal can delete(document) }

        // Use allowed() to return a boolean instead of throwing an exception
        if (authPolicy.allowed(principal, document, write)) {
            // ...
        }
    }
})

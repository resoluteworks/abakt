package abakt.core

/**
 * Internal object used by [AuthorizationPolicy] to enable a more fluent API for checking permissions.
 *
 * For example:
 *
 *      policy.check {
 *          User("owner").can(Document.read, document)
 *          User("owner") can Document.write(document)
 *      }
 */
class PermissionsCheck<Principal : Any, Resource : Any>(private val policy: AuthorizationPolicy<Principal>) {

    fun Principal.can(action: ResourceAction<Resource>, resource: Resource) {
        policy.check(this, resource, action)
    }

    operator fun ResourceAction<Resource>.invoke(resource: Resource): ResourceAndAction<Resource> = ResourceAndAction(resource, this)

    infix fun Principal.can(resourceAndAction: ResourceAndAction<Resource>) {
        policy.check(this, resourceAndAction.resource, resourceAndAction.action)
    }

    class ResourceAndAction<Resource : Any>(val resource: Resource, val action: ResourceAction<Resource>)
}

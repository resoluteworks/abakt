package abakt.core

import kotlin.reflect.KClass

/**
 * An authorization policy that defines the permissions for a principal to perform actions on several types of resources.
 */
class AuthorizationPolicy<Principal : Any> {

    private val resourcePolicies = mutableMapOf<KClass<*>, ResourcePolicy<Principal, *>>()

    /**
     * Adds a policy for the specified resource type, initialized via the [init] block.
     */
    inline fun <reified Resource : Any> resource(init: ResourcePolicy<Principal, Resource>.() -> Unit) {
        addPolicy(Resource::class, ResourcePolicy<Principal, Resource>().apply(init))
    }

    /**
     * Adds a policy for the specified resource type.
     */
    inline fun <reified Resource : Any> resource(resourcePolicy: ResourcePolicy<Principal, Resource>) {
        addPolicy(Resource::class, resourcePolicy)
    }

    fun <Resource : Any> addPolicy(cls: KClass<Resource>, policy: ResourcePolicy<Principal, Resource>) {
        resourcePolicies[cls] = policy
    }

    /**
     * Returns a boolean indicating whether the principal is allowed all of the specified actions against the resource.
     * Use this method when you wish to perform a permission check and handle a boolean value rather than an exception.
     *
     * @param principal The principal attempting to access the resource.
     * @param resource The resource being accessed.
     * @param actions The actions being performed on the resource.
     * @return true if all actions are allowed, false otherwise.
     */
    fun <Resource : Any> allowed(principal: Principal, resource: Resource, vararg actions: ResourceAction<Resource>): Boolean {
        val resourcePolicy = resourcePolicies[resource::class]
            ?.let { it as ResourcePolicy<Principal, Resource> }
            ?: throw IllegalArgumentException("No policies found for class ${resource::class}")

        return actions.all {
            resourcePolicy.allowed(principal, resource, it)
        }
    }

    /**
     * Checks that the action performed by the principal is allowed against the resource.
     * Throws a [PermissionDeniedException] when the action is not allowed.
     * Use this method when you wish to perform a permission check that throws an exception.
     *
     * @param principal The principal attempting to access the resource.
     * @param action The action being performed on the resource.
     * @param resource The resource being accessed.
     * @throws PermissionDeniedException when the action is not allowed.
     */
    @Throws(PermissionDeniedException::class)
    fun <Resource : Any> check(principal: Principal, resource: Resource, vararg actions: ResourceAction<Resource>) {
        if (!allowed(principal, resource, *actions)) {
            throw PermissionDeniedException("Action not allowed")
        }
    }

    /**
     * Utility for defining a block of permission checks.
     *
     * For example:
     *
     *     policy.check {
     *         User("owner").can(Document.read, document)
     *         User("owner") can Document.write(document)
     *     }
     *
     */
    fun <Resource : Any> check(checkPermissions: PermissionsCheck<Principal, Resource>.() -> Unit) {
        val check = PermissionsCheck<Principal, Resource>(this)
        checkPermissions(check)
    }
}

/**
 * Convenience function for creating an [AuthorizationPolicy] and initializing it via the provided [init] block.
 */
fun <Principal : Any> authorizationPolicy(init: AuthorizationPolicy<Principal>.() -> Unit): AuthorizationPolicy<Principal> {
    val authorizer = AuthorizationPolicy<Principal>()
    init(authorizer)
    return authorizer
}

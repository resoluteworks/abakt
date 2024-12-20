package abakt.core

/**
 * Context for evaluating permissions. Contains the principal, resource, and derived roles which
 * are used at runtime to determine whether the principal has permission to perform an action on the resource.
 */
class EvaluationContext<Principal : Any, Resource : Any>(
    val principal: Principal,
    val resource: Resource,
    val derivedRoles: Set<String>
) {
    val p: Principal = principal
    val r: Resource = resource

    /**
     * Check if the principal has a derived role.
     */
    fun hasDerivedRole(derivedRole: String): Boolean = derivedRoles.contains(derivedRole)
}

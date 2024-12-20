package abakt.core

/**
 * Context for evaluating derived roles. Contains the principal and resource which are used to define
 * the conditions under which a derived role is granted.
 */
class DerivedRoleContext<Principal : Any, Resource : Any>(
    val principal: Principal,
    val resource: Resource
) {
    val p: Principal = principal
    val r: Resource = resource
}

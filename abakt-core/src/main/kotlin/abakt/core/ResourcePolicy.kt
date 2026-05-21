package abakt.core

import io.github.oshai.kotlinlogging.KotlinLogging

private typealias RuleCondition<Principal, Resource> = EvaluationContext<Principal, Resource>.() -> Boolean
private typealias DerivedRoleCondition<Principal, Resource> = DerivedRoleContext<Principal, Resource>.() -> Boolean
private typealias ListFilterProducer<Principal> = ListFilterContext<Principal>.() -> Any?

private val log = KotlinLogging.logger {}

/**
 * Represents a policy defining the rules for accessing a specific type of resource.
 */
class ResourcePolicy<Principal : Any, Resource : Any> {

    private val rules = mutableListOf<Rule<Principal, Resource>>()
    private val derivedRolesRules = mutableMapOf<String, DerivedRoleCondition<Principal, Resource>>()
    private val listFilters = mutableMapOf<ResourceAction<Resource>, ListFilterProducer<Principal>>()

    /**
     * Adds a rule to allow the specified actions if the condition is met.
     *
     * @param actions The actions to allow.
     * @param condition The condition which must be met for the actions to be allowed.
     */
    fun allow(vararg actions: ResourceAction<Resource>, condition: RuleCondition<Principal, Resource>) {
        if (actions.isEmpty()) {
            throw IllegalArgumentException("At least one action must be specified")
        }
        rules.add(Rule({ it in actions }, condition, Rule.Effect.ALLOW))
    }

    /**
     * Adds a rule to allow all actions if the condition is met.
     *
     * @param condition The condition which must be met for all actions to be allowed.
     */
    fun allowAll(condition: RuleCondition<Principal, Resource>) {
        rules.add(Rule({ true }, condition, Rule.Effect.ALLOW))
    }

    /**
     * Adds a rule to always allow the specified actions, immaterial of the principal accessing the resource.
     *
     * @param actions The actions to always allow.
     */
    fun alwaysAllow(vararg actions: ResourceAction<Resource>) {
        if (actions.isEmpty()) {
            throw IllegalArgumentException("At least one action must be specified")
        }
        rules.add(Rule({ it in actions }, { true }, Rule.Effect.ALLOW))
    }

    /**
     * Adds a rule to deny the specified actions if the condition is met.
     *
     * @param actions The actions to deny.
     * @param condition The condition which must be met for the actions to be denied.
     */
    fun deny(vararg actions: ResourceAction<Resource>, condition: RuleCondition<Principal, Resource>) {
        if (actions.isEmpty()) {
            throw IllegalArgumentException("At least one action must be specified")
        }
        rules.add(Rule({ it in actions }, condition, Rule.Effect.DENY))
    }

    /**
     * Adds a rule to deny all actions if the condition is met.
     *
     * @param condition The condition which must be met for all actions to be denied.
     */
    fun denyAll(condition: RuleCondition<Principal, Resource>) {
        rules.add(Rule({ true }, condition, Rule.Effect.DENY))
    }

    /**
     * Creates a derived role definition based on the condition provided.
     *
     * @param role The name of the derived role.
     * @param condition The condition which must be met for the derived role to be assigned to the principal when accessing the resource.
     */
    fun derivedRole(role: String, condition: DerivedRoleCondition<Principal, Resource>) {
        derivedRolesRules[role] = condition
    }

    /**
     * Declares the filter representing the set of [Resource] instances the principal is allowed
     * to list under the given [action]. The [producer] returns the filter, or `null` to deny.
     *
     * Unlike [allow]/[deny], a list filter is a function of the principal alone — it cannot be
     * evaluated against a specific [Resource] instance because list authorization happens before
     * any rows have been fetched.
     *
     * The filter type is opaque to abakt; the calling library decides what to use (for example a
     * MongoDB `Bson` filter or a SQL predicate). Only one filter may be registered per action;
     * a subsequent call for the same action replaces the previous producer.
     *
     * @param action The action the filter applies to (typically a "list" action).
     * @param producer Returns the filter, or `null` if the principal is denied.
     */
    fun <Filter : Any> listFilter(
        action: ResourceAction<Resource>,
        producer: ListFilterContext<Principal>.() -> Filter?
    ) {
        listFilters[action] = producer
    }

    /**
     * Returns the filter declared via [listFilter] for the given [principal] and [action], or
     * `null` if the principal is denied or no list filter has been declared for the action.
     */
    @Suppress("UNCHECKED_CAST")
    fun <Filter : Any> filterFor(principal: Principal, action: ResourceAction<Resource>): Filter? =
        listFilters[action]?.invoke(ListFilterContext(principal)) as Filter?

    /**
     * Returns a boolean indicating whether the [principal] is allowed to perform all the specified
     * [actions] against the [resource].
     *
     * @param principal The principal attempting to access the resource.
     * @param resource The resource being accessed.
     * @param actions The actions being performed on the resource.
     * @return true if all actions are allowed, false otherwise.
     */
    fun allowed(principal: Principal, resource: Resource, vararg actions: ResourceAction<Resource>): Boolean {
        val derivedRoles = derivedRolesRules.filter { it.value(DerivedRoleContext(principal, resource)) }.keys
        val actionContext = EvaluationContext(principal, resource, derivedRoles)
        return actions.all { actionAllowed(it, actionContext) }
    }

    private fun actionAllowed(action: ResourceAction<Resource>, evaluationContext: EvaluationContext<Principal, Resource>): Boolean {
        val applicableRules = rules.filter {
            it.matchAction(action) && it.condition(evaluationContext)
        }

        // If no rules match then this action should be denied
        if (applicableRules.isEmpty()) {
            return false
        }

        // Only return true if none of the rules are of type DENY
        val allowed = applicableRules.none { it.effect == Rule.Effect.DENY }
        log.atDebug {
            message = "Resource policy check"
            payload = mapOf(
                "principal" to evaluationContext.principal,
                "resource" to evaluationContext.resource,
                "action" to action.action,
                "allowed" to allowed
            )
        }
        return allowed
    }

    private class Rule<Principal : Any, Resource : Any>(
        val matchAction: (ResourceAction<Resource>) -> Boolean,
        val condition: RuleCondition<Principal, Resource>,
        val effect: Effect
    ) {
        enum class Effect {
            ALLOW,
            DENY
        }
    }
}

/**
 * Convenience function for creating a [ResourcePolicy] and initializing it via the provided [init] block.
 *
 * For example:
 *
 *     val policy = resourcePolicy<Principal, Resource> {
 *         derivedRole("owner") { p.id == r.ownerId }
 *         alwaysAllow(ResourceAction("read"))
 *         allow(ResourceAction("write")) { hasDerivedRole("owner") }
 *     }
 */
fun <Principal : Any, Resource : Any> resourcePolicy(
    init: ResourcePolicy<Principal, Resource>.() -> Unit
): ResourcePolicy<Principal, Resource> {
    val policy = ResourcePolicy<Principal, Resource>()
    init(policy)
    return policy
}

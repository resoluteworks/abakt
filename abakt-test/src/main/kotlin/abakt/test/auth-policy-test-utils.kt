package abakt.test

import abakt.core.AuthorizationPolicy
import abakt.core.ResourceAction
import io.kotest.matchers.shouldBe

fun <Principal : Any, Resource : Any> AuthorizationPolicy<Principal>.shouldAllow(
    principal: Principal,
    resource: Resource,
    vararg actions: ResourceAction<Resource>
) {
    this.allowed(principal, resource, *actions) shouldBe true
}

fun <Principal : Any, Resource : Any> AuthorizationPolicy<Principal>.shouldNotAllow(
    principal: Principal,
    resource: Resource,
    vararg actions: ResourceAction<Resource>
) {
    this.allowed(principal, resource, *actions) shouldBe false
}

fun <Principal : Any, Resource : Any> AuthorizationPolicy<Principal>.shouldDeny(
    principal: Principal,
    resource: Resource,
    vararg actions: ResourceAction<Resource>
) {
    this.shouldNotAllow(principal, resource, *actions)
}

fun <Principal : Any, Resource : Any> AuthorizationPolicy<Principal>.withResource(
    resource: Resource,
    block: AuthPolicyResourceTest<Principal, Resource>.() -> Unit
) {
    AuthPolicyResourceTest(this, resource).block()
}

class AuthPolicyResourceTest<Principal : Any, Resource : Any>(
    private val policy: AuthorizationPolicy<Principal>,
    private val resource: Resource
) {
    infix fun Principal.shouldBeAllowed(action: ResourceAction<Resource>) {
        policy.shouldAllow(this, resource, action)
    }

    infix fun Principal.shouldNotBeAllowed(action: ResourceAction<Resource>) {
        policy.shouldNotAllow(this, resource, action)
    }

    infix fun Principal.shouldBeDenied(action: ResourceAction<Resource>) {
        policy.shouldNotAllow(this, resource, action)
    }
}

fun <Principal : Any> AuthorizationPolicy<Principal>.withPrincipal(
    principal: Principal,
    block: AuthPolicyPrincipalTest<Principal>.() -> Unit
) {
    AuthPolicyPrincipalTest(this, principal).block()
}

class AuthPolicyPrincipalTest<Principal : Any>(
    private val policy: AuthorizationPolicy<Principal>,
    private val principal: Principal
) {

    infix fun <Resource : Any> ResourceAction<Resource>.shouldBeAllowedOn(resource: Resource) {
        policy.shouldAllow<Principal, Resource>(principal, resource, this)
    }

    infix fun <Resource : Any> ResourceAction<Resource>.shouldNotBeAllowedOn(resource: Resource) {
        policy.shouldNotAllow<Principal, Resource>(principal, resource, this)
    }

    infix fun <Resource : Any> ResourceAction<Resource>.shouldBeDeniedOn(resource: Resource) {
        policy.shouldNotAllow<Principal, Resource>(principal, resource, this)
    }
}

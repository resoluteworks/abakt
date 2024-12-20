package abakt.test

import abakt.core.ResourceAction
import abakt.core.ResourcePolicy
import io.kotest.matchers.shouldBe

fun <Principal : Any, Resource : Any> ResourcePolicy<Principal, Resource>.shouldAllow(
    principal: Principal,
    resource: Resource,
    vararg actions: ResourceAction<Resource>
) {
    this.allowed(principal, resource, *actions) shouldBe true
}

fun <Principal : Any, Resource : Any> ResourcePolicy<Principal, Resource>.shouldNotAllow(
    principal: Principal,
    resource: Resource,
    vararg actions: ResourceAction<Resource>
) {
    this.allowed(principal, resource, *actions) shouldBe false
}

fun <Principal : Any, Resource : Any> ResourcePolicy<Principal, Resource>.shouldDeny(
    principal: Principal,
    resource: Resource,
    vararg actions: ResourceAction<Resource>
) {
    this.shouldNotAllow(principal, resource, *actions)
}

fun <Principal : Any, Resource : Any> ResourcePolicy<Principal, Resource>.withResource(
    resource: Resource,
    block: ResourcePolicyResourceTest<Principal, Resource>.() -> Unit
) {
    ResourcePolicyResourceTest(this, resource).block()
}

class ResourcePolicyResourceTest<Principal : Any, Resource : Any>(
    private val policy: ResourcePolicy<Principal, Resource>,
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

fun <Principal : Any, Resource : Any> ResourcePolicy<Principal, Resource>.withPrincipal(
    principal: Principal,
    block: ResourcePolicyPrincipalTest<Principal, Resource>.() -> Unit
) {
    ResourcePolicyPrincipalTest(this, principal).block()
}

class ResourcePolicyPrincipalTest<Principal : Any, Resource : Any>(
    private val policy: ResourcePolicy<Principal, Resource>,
    private val principal: Principal
) {

    infix fun ResourceAction<Resource>.shouldBeAllowedOn(resource: Resource) {
        policy.shouldAllow<Principal, Resource>(principal, resource, this)
    }

    infix fun ResourceAction<Resource>.shouldNotBeAllowedOn(resource: Resource) {
        policy.shouldNotAllow<Principal, Resource>(principal, resource, this)
    }

    infix fun ResourceAction<Resource>.shouldBeDeniedOn(resource: Resource) {
        policy.shouldNotAllow<Principal, Resource>(principal, resource, this)
    }
}

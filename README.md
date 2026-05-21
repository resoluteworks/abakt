# Abakt

[![GitHub release (latest by date)](https://img.shields.io/github/v/release/resoluteworks/abakt)](https://github.com/resoluteworks/abakt/releases)
[![Coveralls](https://img.shields.io/coverallsCoverage/github/resoluteworks/abakt)](https://coveralls.io/github/resoluteworks/abakt)
[![License: Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

**Abakt** is a Kotlin (JVM) framework for attribute-based access control (ABAC).
Declare your policies as a type-safe DSL, check them inline, and keep your authorization logic
out of your business code.

```kotlin
authPolicy.check { user can document.write }
```

## Why Abakt

- **Type-safe DSL** &mdash; principals, resources, and actions are all typed; the compiler catches
  mistakes before they reach production.
- **Unintrusive** &mdash; no marker interfaces, no inheritance, no annotations. Your domain types
  stay your own.
- **Derived roles** &mdash; express roles that depend on the relationship between principal and
  resource (e.g. "owner", "member of same org").
- **List filters** &mdash; turn a policy into a query predicate so you can authorize listing
  endpoints at the database layer, not after fetching.
- **Library, not platform** &mdash; in-memory, no servers, no sidecars, no network hops. Drop it
  into any JVM application.
- **First-class testing** &mdash; a dedicated [Kotest](https://kotest.io/) module for writing
  expressive policy tests.

## Documentation

- [Core API](https://resoluteworks.github.io/abakt/dokka/abakt-core/abakt.core/)
- [Test utilities API](https://resoluteworks.github.io/abakt/dokka/abakt-test/abakt.test/)

## Installation

```kotlin
dependencies {
    implementation("works.resolute:abakt-core:${abaktVersion}")

    // For policy testing with Kotest
    testImplementation("works.resolute:abakt-test:${abaktVersion}")
}
```

## Quick start

```kotlin
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
```

## List filters

A list filter turns a policy into a predicate the caller can apply *before* fetching data. This
matters because per-row authorization doesn't scale: paginating, sorting, and counting only
work correctly when the database itself returns just the rows the principal is allowed to see.

The filter type is opaque to Abakt &mdash; the calling code decides what it produces (a MongoDB
`Bson`, a SQL `WHERE` clause, a JPA `Specification`, a custom AST, anything).

```kotlin
val list = ResourceAction<Document>("list")

val authPolicy = authorizationPolicy<Principal> {
    resource<Document> {
        // Existing per-instance rules still apply for check()/allowed()
        allow(read) { resource.organisationId == principal.organisationId }

        // A filter producer for the "list" action. Returns `null` to deny
        // listing entirely; otherwise returns the predicate the caller will
        // hand to its data layer.
        listFilter(list) {
            when (principal.role) {
                "ADMIN" -> Filters.empty()
                "USER"  -> Filters.eq("organisationId", principal.organisationId)
                else    -> null
            }
        }
    }
}

// Resolve the filter at the call site, then pass it to your repository.
val filter = authPolicy.filterFor<Document, Bson>(principal, list)
    ?: throw PermissionDeniedException("Listing denied")

documents.find(filter).toList()
```

## Testing policies

`abakt-test` provides Kotest-friendly utilities for asserting that your rules behave as
expected, both for an individual resource policy and for a composed authorization policy.

### Resource policy tests

```kotlin
val resourcePolicy = resourcePolicy<User, Document> { ... }

resourcePolicy.shouldAllow(owner, document, read)
resourcePolicy.shouldDeny(otherUser, document, write)

resourcePolicy.withResource(largeExpense) {
    managerInFinance shouldBeAllowed approve
    userInFinance   shouldNotBeAllowed approve
}
```

### Authorization policy tests

```kotlin
val policy = authorizationPolicy<User> {
    resource<Document> { ... }
    resource<Folder>   { ... }
}

policy.shouldAllow(User("owner"), Document("owner"), documentWrite)

policy.withPrincipal(User("owner")) {
    folderRead   shouldBeAllowedOn Folder("owner")
    folderDelete shouldBeAllowedOn Folder("owner")
}

policy.withResource(Folder("owner")) {
    User("otherUser") shouldBeAllowed folderRead
    User("otherUser") shouldBeDenied  folderDelete
}
```

## Design notes

Abakt is built around a few deliberate choices:

- **No marker interfaces.** Principals and resources are plain generics, so you can layer
  Abakt onto an existing domain model without changing it.
- **No wiring opinions.** The library covers policy *definition* and *verification*; how you
  invoke checks (web filters, interceptors, explicit calls) is up to you.
- **In-memory only.** Policies are Kotlin code, evaluated in-process. There is no server,
  no network call, no external schema.

Abakt borrows ideas from [Cerbos](https://www.cerbos.dev/) but is not a replacement for it:
Cerbos is an authorization *platform*, Abakt is a JVM-embedded *library*.

## License

[Apache 2.0](LICENSE)

# Abakt
![GitHub release (latest by date)](https://img.shields.io/github/v/release/resoluteworks/abakt)
![Coveralls](https://img.shields.io/coverallsCoverage/github/resoluteworks/abakt)

Abakt is a Kotlin (JVM) framework for implementing attribute-based access control (ABAC) policies
using a typesafe DSL and expressive constructs.

[Core API Docs](https://resoluteworks.github.io/abakt/dokka/abakt-core/abakt.core/)

[Test library API Docs](https://resoluteworks.github.io/abakt/dokka/abakt-test/abakt.test/)

## Dependency
```kotlin
implementation("io.resoluteworks:abakt-core:${abaktVersion}")
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

## Motivation and guiding principles
The main motivation for this framework is to provide the ability to define and validate ABAC
permissions as easily as possible, by using type-safe constructs and a concise API.

At the same time, we wanted something that's unintrusive and flexible. This is why we don't have
marker interfaces like `Principal` or `Resource` and we've opted for generics instead.

This allows the client code to bring its own representation for these concepts, and for the
framework to act as a drop-in, or an extension.

For the same reasons, the core library only provides the basic elements of operating an authorization
policy: definition and verification. The consumer application can then make its own decisions about
how these elements are wired (web filters, proxies, explicit calls, etc.).

## Testing policies
A testing framework based on [Kotest](https://kotest.io/) is provided to help with testing
authentication policies. These utilities allow you to write expressive tests to validate
the rules for an individual resource policy, or an entire authorization policy.

#### Dependency
```kotlin
implementation("io.resoluteworks:abakt-test:${abaktVersion}")
```

#### Testing resource policies
```kotlin
val resourcePolicy = resourcePolicy<Document> { ... }

resourcePolicy.shouldAllow(owner, document, ResourceAction("read"))
resourcePolicy.shouldDeny(otherUser, document, ResourceAction("write"))

resourcePolicy.withResource(largeExpense) {
    managerInFinance shouldBeAllowed actionApprove
    userInFinance shouldNotBeAllowed actionApprove
}
```

#### Testing authorization policies
Similar constructs can be used to test authorization policies.
```kotlin
val policy = authorizationPolicy<User> {
    resource<Document> {...}
    resource<Folder> {...}
}

policy.shouldAllow(User("owner"), Document("owner"), actionDocumentWrite)

policy.withPrincipal(User("owner")) {
    actionFolderRead shouldBeAllowedOn Folder("owner")
    actionFolderDelete shouldBeAllowedOn Folder("owner")
}

policy.withResource(Folder("owner")) {
    User("otherUser") shouldBeAllowed actionFolderRead
    User("otherUser") shouldBeDenied actionFolderDelete
}
```

## Inspiration
Abakt borrows principles and approaches from [Cerbos](https://www.cerbos.dev/), which is a great
authorization platform. However, our framework doesn't (cannot) stand as an alternative in the same space. Abakt is not an authorization platform, but an in-memory
authorization framework for Kotlin/JVM.

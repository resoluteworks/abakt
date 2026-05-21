package abakt.core.resourcepolicy

import abakt.core.PermissionDeniedException
import abakt.core.ResourceAction
import abakt.core.resourcePolicy
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ResourcePolicyListFilterTest : StringSpec({

    "listFilter returns the produced filter for an allowed principal" {
        data class User(val id: String, val organisationId: String)
        data class File(val ownerOrg: String)

        val list = ResourceAction<File>("list")
        val policy = resourcePolicy<User, File> {
            listFilter(list) {
                "ownerOrg=${principal.organisationId}"
            }
        }

        policy.filterFor<String>(User("u1", "org1"), list) shouldBe "ownerOrg=org1"
        policy.filterFor<String>(User("u2", "org2"), list) shouldBe "ownerOrg=org2"
    }

    "filterFor throws PermissionDeniedException when the producer denies (returns null)" {
        data class User(val id: String, val canList: Boolean)
        data class File(val id: String)

        val list = ResourceAction<File>("list")
        val policy = resourcePolicy<User, File> {
            listFilter(list) { if (p.canList) "all" else null }
        }

        policy.filterFor<String>(User("u1", true), list) shouldBe "all"
        shouldThrow<PermissionDeniedException> {
            policy.filterFor<String>(User("u2", false), list)
        }
    }

    "filterFor throws PermissionDeniedException when no list filter is declared for the action" {
        data class User(val id: String)
        data class File(val id: String)

        val list = ResourceAction<File>("list")
        val policy = resourcePolicy<User, File> {
            // intentionally no listFilter
        }

        shouldThrow<PermissionDeniedException> {
            policy.filterFor<String>(User("u1"), list)
        }
    }

    "later listFilter for the same action replaces the previous one" {
        data class User(val id: String)
        data class File(val id: String)

        val list = ResourceAction<File>("list")
        val policy = resourcePolicy<User, File> {
            listFilter(list) { "first" }
            listFilter(list) { "second" }
        }

        policy.filterFor<String>(User("u1"), list) shouldBe "second"
    }

    "different actions can have independent list filters" {
        data class User(val id: String, val organisationId: String)
        data class File(val id: String)

        val listOwn = ResourceAction<File>("listOwn")
        val listOrg = ResourceAction<File>("listOrg")
        val policy = resourcePolicy<User, File> {
            listFilter(listOwn) { "userId=${p.id}" }
            listFilter(listOrg) { "orgId=${p.organisationId}" }
        }

        val user = User("u1", "org1")
        policy.filterFor<String>(user, listOwn) shouldBe "userId=u1"
        policy.filterFor<String>(user, listOrg) shouldBe "orgId=org1"
    }
})

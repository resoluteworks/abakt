package abakt.core.authorizationpolicy

import abakt.core.ResourceAction
import abakt.core.authorizationPolicy
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class AuthorizationPolicyListFilterTest : StringSpec({

    "filterFor returns the produced filter for an allowed principal" {
        val policy = authorizationPolicy<User> {
            resource<File> {
                listFilter(File.list) { "orgId=${principal.organisationId}" }
            }
        }

        policy.filterFor<File, String>(User("u1", "org1"), File.list) shouldBe "orgId=org1"
    }

    "filterFor returns null when the producer denies" {
        val policy = authorizationPolicy<User> {
            resource<File> {
                listFilter(File.list) { if (p.organisationId == "org1") "ok" else null }
            }
        }

        policy.filterFor<File, String>(User("u1", "org1"), File.list) shouldBe "ok"
        policy.filterFor<File, String>(User("u2", "org2"), File.list).shouldBeNull()
    }

    "filterFor returns null when no list filter is declared for the action" {
        val policy = authorizationPolicy<User> {
            resource<File> {
                // no listFilter
            }
        }

        policy.filterFor<File, String>(User("u1", "org1"), File.list).shouldBeNull()
    }

    "filterFor throws when no policy is registered for the resource type" {
        val policy = authorizationPolicy<User> {
            // nothing registered for File
        }

        shouldThrowWithMessage<IllegalArgumentException>(
            "No policies found for class ${File::class}"
        ) {
            policy.filterFor<File, String>(User("u1", "org1"), File.list)
        }
    }

    "filterFor can be called via the KClass overload" {
        val policy = authorizationPolicy<User> {
            resource<File> {
                listFilter(File.list) { "orgId=${p.organisationId}" }
            }
        }

        policy.filterFor<File, String>(File::class, User("u1", "org1"), File.list) shouldBe "orgId=org1"
    }
}) {

    private data class User(val id: String, val organisationId: String)
    private data class File(val id: String) {
        companion object {
            val list = ResourceAction<File>("list")
        }
    }
}

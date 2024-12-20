package abakt.core.resourcepolicy

import abakt.core.ResourceAction
import abakt.core.uuid
import abakt.test.shouldAllow
import abakt.test.shouldNotAllow
import io.kotest.core.spec.style.StringSpec

class HolidaysTest : StringSpec({

    "holidays" {
        data class Principal(
            val id: String = uuid(),
            val role: UserRole,
            val directReports: Set<String> = emptySet()
        )

        data class HolidayRequest(
            val requestedBy: String,
            val days: Int
        )

        val actionRead = ResourceAction<HolidayRequest>("read")
        val actionApprove = ResourceAction<HolidayRequest>("approve")

        val policy = abakt.core.resourcePolicy<Principal, HolidayRequest> {
            // Only the owning user and their manager can read a holiday request
            allow(actionRead) {
                (resource.requestedBy == principal.id) || (resource.requestedBy in principal.directReports)
            }

            // Only their manager can approve a holiday request
            allow(actionApprove) {
                resource.requestedBy in principal.directReports
            }

            // No one can approve holidays that are longer than 15 days
            deny(actionApprove) {
                resource.days > 15
            }
        }

        val requestedBy = uuid()
        val request = HolidayRequest(requestedBy, 10)
        val requester = Principal(requestedBy, UserRole.USER)
        val managerOfRequester = Principal(
            uuid(), UserRole.MANAGER,
            setOf(requestedBy)
        )
        val someOtherManager = Principal(
            uuid(), UserRole.MANAGER,
            setOf(uuid())
        )
        val someOtherUser = Principal(uuid(), UserRole.USER)

        policy.shouldAllow(requester, request, actionRead)
        policy.shouldAllow(managerOfRequester, request, actionRead)
        policy.shouldNotAllow(someOtherUser, request, actionRead)
        policy.shouldNotAllow(someOtherManager, request, actionRead)

        policy.shouldAllow(managerOfRequester, request, actionApprove)
        policy.shouldNotAllow(requester, request, actionApprove)
        policy.shouldNotAllow(someOtherUser, request, actionApprove)
        policy.shouldNotAllow(someOtherManager, request, actionApprove)

        policy.shouldNotAllow(requester, HolidayRequest(requestedBy, 20), actionApprove)
        policy.shouldNotAllow(managerOfRequester, HolidayRequest(requestedBy, 16), actionApprove)
        policy.shouldNotAllow(someOtherUser, HolidayRequest(requestedBy, 25), actionApprove)
        policy.shouldNotAllow(someOtherManager, HolidayRequest(requestedBy, 20), actionApprove)
    }
}) {
    private enum class UserRole { USER, MANAGER }
}

package abakt.core.resourcepolicy

import abakt.core.ResourceAction
import abakt.core.resourcePolicy
import abakt.core.uuid
import abakt.test.shouldAllow
import abakt.test.shouldNotAllow
import abakt.test.withResource
import io.kotest.core.spec.style.StringSpec

class ExpensesTest : StringSpec({

    "expenses" {
        data class Principal(
            val id: String,
            val role: UserRole,
            val department: String,
            val directReports: Set<String> = emptySet()
        )

        data class Expense(
            val submittedBy: String,
            val sum: Long,
            val status: ExpenseStatus = ExpenseStatus.PENDING
        )

        val actionRead = ResourceAction<Expense>("read")
        val actionUpdate = ResourceAction<Expense>("update")
        val actionApprove = ResourceAction<Expense>("approve")

        val policy = resourcePolicy<Principal, Expense> {
            // The only ones that can read are
            // - the user that created the Expense
            // - any user in Finance department
            // - any MANAGER to which the user reports to
            allow(actionRead) {
                resource.submittedBy == principal.id ||
                    principal.department == "Finance" ||
                    (principal.role == UserRole.MANAGER && resource.submittedBy in principal.directReports)
            }

            // Only the user themselves can update an expense IF it's still PENDING
            allow(actionUpdate) {
                principal.id == resource.submittedBy && resource.status == ExpenseStatus.PENDING
            }

            // Anyone in Finance can approve if the sum is <= 100, otherwise they have to be a MANAGER in Finance
            allow(actionApprove) {
                principal.department == "Finance" && (resource.sum <= 100 || principal.role == UserRole.MANAGER)
            }

            // Admin can do anything
            allowAll { principal.role == UserRole.ADMIN }
        }

        val submittedBy = uuid()
        val expense = Expense(submittedBy, 80)
        val largeExpense = Expense(submittedBy, 120)
        val submitter = Principal(submittedBy, UserRole.USER, "IT")
        val managerOfSubmitter = Principal(uuid(), UserRole.MANAGER, "IT", setOf(submittedBy))
        val someOtherManager = Principal(uuid(), UserRole.MANAGER, "HR", setOf(uuid()))
        val someOtherUser = Principal(uuid(), UserRole.USER, "Ops", setOf(uuid()))
        val userInFinance = Principal(uuid(), UserRole.USER, "Finance")
        val managerInFinance = Principal(uuid(), UserRole.MANAGER, "Finance")

        policy.withResource(expense) {
            submitter shouldBeAllowed actionRead
            userInFinance shouldBeAllowed actionRead
            managerInFinance shouldBeAllowed actionRead
            managerOfSubmitter shouldBeAllowed actionRead
            someOtherManager shouldBeDenied actionRead
            someOtherUser shouldBeDenied actionRead

            submitter shouldBeAllowed actionUpdate
            userInFinance shouldNotBeAllowed actionUpdate
            managerInFinance shouldNotBeAllowed actionUpdate
            managerOfSubmitter shouldNotBeAllowed actionUpdate
            someOtherManager shouldNotBeAllowed actionUpdate

            // Anyone in Finance can approve if the sum is <= 100
            userInFinance shouldBeAllowed actionApprove
            managerInFinance shouldBeAllowed actionApprove

            // Users not in finance can't approve
            submitter shouldNotBeAllowed actionApprove
            managerOfSubmitter shouldNotBeAllowed actionApprove
            someOtherManager shouldNotBeAllowed actionApprove
            someOtherUser shouldNotBeAllowed actionApprove
        }

        policy.shouldNotAllow(submitter, Expense(submittedBy, 50, ExpenseStatus.APPROVED), actionUpdate)

        // They have to be a MANAGER in Finance to approve expenses larger than 100
        policy.withResource(largeExpense) {
            userInFinance shouldNotBeAllowed actionApprove
            managerInFinance shouldBeAllowed actionApprove
        }

        // Admin can do anything
        val admin = Principal(uuid(), UserRole.ADMIN, "Doesn't matter")
        policy.shouldAllow(admin, expense, ResourceAction("read"))
        policy.shouldAllow(admin, expense, ResourceAction("update"))
        policy.shouldAllow(admin, expense, actionApprove)
        policy.shouldAllow(admin, largeExpense, actionApprove)
    }
}) {
    private enum class UserRole { ADMIN, USER, MANAGER }
    private enum class ExpenseStatus { PENDING, APPROVED }
}

package abakt.core.resourcepolicy

import abakt.core.ResourceAction
import abakt.core.ResourcePolicy
import abakt.core.resourcePolicy
import abakt.core.uuid
import abakt.test.shouldAllow
import abakt.test.shouldNotAllow
import io.kotest.core.spec.style.StringSpec

class OrdersTest : StringSpec() {

    private val roleCustomer = "orderCustomer"
    private val roleHandler = "orderHandler"
    private val roleCourier = "orderCourier"

    private val customerId = uuid()
    private val customer = Principal(customerId, UserRole.CUSTOMER)
    private val handlingEmployee = Principal(uuid(), UserRole.EMPLOYEE)
    private val otherEmployee = Principal(uuid(), UserRole.EMPLOYEE)
    private val courier = Principal(uuid(), UserRole.COURIER)
    private val otherCourier = Principal(uuid(), UserRole.COURIER)

    init {
        "online order management" {

            val policy = resourcePolicy<Principal, Order> {
                derivedRole(roleCustomer) { resource.customerId == principal.id }
                derivedRole(roleHandler) { resource.handlingEmployee == principal.id }
                derivedRole(roleCourier) { resource.courier == principal.id }

                // Only a customer can check out an order and only if it's in a DRAFT state
                allow(Order.checkout) {
                    principal.role == UserRole.CUSTOMER && resource.status == OrderStatus.DRAFT
                }

                // Only the customer and the handling employee can cancel an order
                allow(Order.cancelOrder) {
                    hasDerivedRole(roleCustomer) || hasDerivedRole(roleHandler)
                }

                // Orders can't be cancelled when dispatched, out for delivery, delivered
                deny(Order.cancelOrder) {
                    resource.status in setOf(
                        OrderStatus.DISPATCHED,
                        OrderStatus.OUT_FOR_DELIVERY,
                        OrderStatus.DELIVERED
                    )
                }

                // Only the employee handling the order can dispatch it, and not if it has payment issues
                allow(Order.dispatchOrder) {
                    hasDerivedRole(roleHandler) && resource.status != OrderStatus.PAYMENT_ISSUE
                }

                // Only the courier for this order can mark order as out for delivery or delivered
                allow(Order.markOrderOutForDelivery, Order.markOrderDelivered) {
                    principal.id == resource.courier
                }

                // No one can do anything with an order once it's delivered
                denyAll {
                    resource.status == OrderStatus.DELIVERED
                }
            }

            // Only a customer can check out an order and only if it's in a DRAFT state
            policy.shouldAllow(customer, Order(OrderStatus.DRAFT, customerId), Order.checkoutOrder)
            policy.shouldNotAllow(customer, Order(OrderStatus.CHECKED_OUT, customerId), Order.checkoutOrder)
            policy.shouldNotAllow(customer, Order(OrderStatus.PROCESSING, customerId), Order.checkoutOrder)
            policy.shouldNotAllowAnyStatus(handlingEmployee, Order.checkoutOrder)
            policy.shouldNotAllowAnyStatus(otherEmployee, Order.checkoutOrder)
            policy.shouldNotAllowAnyStatus(courier, Order.checkoutOrder)
            policy.shouldNotAllowAnyStatus(otherCourier, Order.checkoutOrder)

            // Only the customer and the handling employee can cancel an order
            policy.shouldAllow(customer, Order(OrderStatus.CHECKED_OUT, customerId), Order.cancelOrder)
            policy.shouldAllow(handlingEmployee, Order(OrderStatus.CHECKED_OUT, customerId, handlingEmployee.id), Order.cancelOrder)
            policy.shouldNotAllowAnyStatus(otherEmployee, Order.cancelOrder)
            policy.shouldNotAllowAnyStatus(courier, Order.cancelOrder)
            policy.shouldNotAllowAnyStatus(otherCourier, Order.cancelOrder)

            // Orders can't be cancelled when dispatched, out for delivery, delivered
            policy.shouldNotAllowStatuses(
                customer,
                Order.cancelOrder,
                OrderStatus.DISPATCHED,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.DELIVERED
            )
            policy.shouldNotAllowStatuses(
                handlingEmployee,
                Order.cancelOrder,
                OrderStatus.DISPATCHED,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.DELIVERED
            )
            policy.shouldNotAllowStatuses(
                otherEmployee, Order.cancelOrder,
                OrderStatus.DISPATCHED,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.DELIVERED
            )
            policy.shouldNotAllowStatuses(
                courier,
                Order.cancelOrder,
                OrderStatus.DISPATCHED,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.DELIVERED
            )
            policy.shouldNotAllowStatuses(
                otherCourier,
                Order.cancelOrder,
                OrderStatus.DISPATCHED,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.DELIVERED
            )

            // Only the employee handling the order can dispatch it, and not if it has payment issues
            policy.shouldAllow(handlingEmployee, Order(OrderStatus.PROCESSING, customerId, handlingEmployee.id), Order.dispatchOrder)
            policy.shouldNotAllow(otherEmployee, Order(OrderStatus.PROCESSING, customerId, handlingEmployee.id), Order.dispatchOrder)
            policy.shouldNotAllow(handlingEmployee, Order(OrderStatus.PAYMENT_ISSUE, customerId, handlingEmployee.id), Order.dispatchOrder)

            // Only the courier for this order can mark order as out for delivery or delivered
            val fullOrder = Order(OrderStatus.DISPATCHED, customerId, handlingEmployee.id, courier.id)
            policy.shouldAllow(courier, fullOrder, Order.markOrderOutForDelivery)
            policy.shouldAllow(courier, fullOrder, Order.markOrderDelivered)
            policy.shouldNotAllow(otherCourier, fullOrder, Order.markOrderOutForDelivery)
            policy.shouldNotAllow(otherCourier, fullOrder, Order.markOrderDelivered)
            policy.shouldNotAllow(handlingEmployee, fullOrder, Order.markOrderDelivered)

            fun ResourcePolicy<Principal, Order>.shouldNotAllowAnyActionWhenDelivered(principal: Principal) {
                this.shouldNotAllow(
                    principal,
                    Order(OrderStatus.DELIVERED, customerId, handlingEmployee.id, courier.id),
                    Order.checkoutOrder
                )
                this.shouldNotAllow(principal, Order(OrderStatus.DELIVERED, customerId, handlingEmployee.id, courier.id), Order.cancelOrder)
                this.shouldNotAllow(
                    principal,
                    Order(OrderStatus.DELIVERED, customerId, handlingEmployee.id, courier.id),
                    Order.dispatchOrder
                )
                this.shouldNotAllow(
                    principal,
                    Order(OrderStatus.DELIVERED, customerId, handlingEmployee.id, courier.id),
                    Order.markOrderOutForDelivery
                )
                this.shouldNotAllow(
                    principal,
                    Order(OrderStatus.DELIVERED, customerId, handlingEmployee.id, courier.id),
                    Order.markOrderDelivered
                )
            }

            // No one can do anything with an order once it's delivered
            policy.shouldNotAllowAnyActionWhenDelivered(customer)
            policy.shouldNotAllowAnyActionWhenDelivered(handlingEmployee)
            policy.shouldNotAllowAnyActionWhenDelivered(otherEmployee)
            policy.shouldNotAllowAnyActionWhenDelivered(courier)
            policy.shouldNotAllowAnyActionWhenDelivered(otherCourier)
        }
    }

    private enum class UserRole {
        CUSTOMER,
        EMPLOYEE,
        COURIER
    }

    private enum class OrderStatus {
        DRAFT, // The customer is still choosing items
        CHECKED_OUT, // The customer sent the order to the company (finished buying online)
        CANCELLED, // The customer or the company cancelled the order (customer changed their mind, or company doesn't have it in stock)
        PAYMENT_ISSUE, // The company found problems with the order payment (card rejected, etc.)
        PROCESSING, // The company is reviewing the order, confirming stock, packaging, etc
        DISPATCHED, // The order is dispatched
        OUT_FOR_DELIVERY, // The order is out for delivery to the customer
        DELIVERED // The courier had to re-arrange delivery to customer
    }

    private data class Order(
        val status: OrderStatus,
        var customerId: String,
        var handlingEmployee: String? = null,
        var courier: String? = null
    ) {
        companion object {
            val checkout = ResourceAction<Order>("checkoutOrder")
            val checkoutOrder = ResourceAction<Order>("checkoutOrder")
            val cancelOrder = ResourceAction<Order>("cancelOrder")
            val dispatchOrder = ResourceAction<Order>("dispatchOrder")
            val markOrderOutForDelivery = ResourceAction<Order>("markOrderOutForDelivery")
            val markOrderDelivered = ResourceAction<Order>("markOrderDelivered")
        }
    }

    private data class Principal(
        val id: String,
        val role: UserRole
    )

    private fun ResourcePolicy<Principal, Order>.shouldNotAllowStatuses(
        principal: Principal,
        action: ResourceAction<Order>,
        vararg orderStatuses: OrderStatus
    ) {
        orderStatuses.forEach {
            this.shouldNotAllow(principal, Order(it, customerId, handlingEmployee.id, courier.id), action)
        }
    }

    private fun ResourcePolicy<Principal, Order>.shouldNotAllowAnyStatus(principal: Principal, action: ResourceAction<Order>) {
        OrderStatus.entries.forEach {
            this.shouldNotAllow(principal, Order(it, customerId, handlingEmployee.id, courier.id), action)
        }
    }
}

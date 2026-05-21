package abakt.core

/**
 * Context for producing a list filter. Provides the principal for whom the filter
 * is being produced. Unlike [EvaluationContext], no resource is available because list
 * filters are evaluated before any rows have been fetched.
 */
class ListFilterContext<Principal : Any>(val principal: Principal) {
    val p: Principal = principal
}

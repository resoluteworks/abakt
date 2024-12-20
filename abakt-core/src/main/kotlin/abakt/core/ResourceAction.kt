package abakt.core

/**
 * Represents an action that can be performed on a resource. The generic type [Resource] is the type
 * of resource this action can be performed on.
 */
data class ResourceAction<Resource : Any>(val action: String)

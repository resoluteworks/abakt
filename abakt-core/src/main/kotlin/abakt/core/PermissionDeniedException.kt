package abakt.core

/**
 * Exception thrown when a permission check fails for a given principal and resource.
 */
class PermissionDeniedException(message: String) : RuntimeException(message)

package explode.backend.bomb.v1

/**
 * For internal use only with no garantee on stablility.
 */
@Target(AnnotationTarget.EXPRESSION, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class InternalUse

/**
 * Will be removed before next senior version.
 */
@Target(AnnotationTarget.EXPRESSION, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class WillBeRemoved
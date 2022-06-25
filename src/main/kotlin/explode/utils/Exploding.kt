package explode.utils

/**
 * Anything annotated with this means untested, unstable or only-for-test.
 * You should not use this in any condition unless you know what will happen.
 */
annotation class Exploding(val value: String = "")

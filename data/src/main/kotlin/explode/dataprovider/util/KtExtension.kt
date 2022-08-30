package explode.dataprovider.util

inline fun <T> T.applyIf(condition: Boolean, then: T.() -> Unit) = apply {
	if(condition) { then() }
}

inline fun <T> T.alsoIf(condition: Boolean, then: (T) -> Unit) = also {
	if(condition) { then(this) }
}
package explode.dataprovider.provider

class BlowException : IllegalStateException {
	constructor() : super()
	constructor(s: String?) : super(s)
	constructor(message: String?, cause: Throwable?) : super(message, cause)
	constructor(cause: Throwable?) : super(cause)
}

fun fail(message: String?): Nothing = throw BlowException(message)
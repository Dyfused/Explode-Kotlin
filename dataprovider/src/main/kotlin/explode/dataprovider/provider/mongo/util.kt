package explode.dataprovider.provider.mongo

const val ThisDocument = "$$" + "ROOT"

fun randomId() = List(24) {
	(('a'..'z') + ('0'..'9')).random()
}.joinToString(separator = "")

fun randomIdUncrypted() = List(20) {
	(('a'..'z') + ('0'..'9')).random()
}.joinToString(separator = "") + "0000"
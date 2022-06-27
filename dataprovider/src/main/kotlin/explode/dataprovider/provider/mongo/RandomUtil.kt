package explode.dataprovider.provider.mongo

object RandomUtil {

	fun randomId() = List(24) {
		(('a'..'z') + ('0'..'9')).random()
	}.joinToString(separator = "")

}
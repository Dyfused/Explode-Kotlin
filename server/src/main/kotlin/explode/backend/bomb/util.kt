package explode.backend.bomb

fun copy(origin: Any, dest: Any) {
	dest::class.java.declaredFields.forEach { destField ->
		val name = destField.name
		runCatching {
			val originField = origin::class.java.getDeclaredField(name)
			val originValue = originField.get(origin)
			destField.set(dest, originValue)
		}.onFailure {
			if(it !is NoSuchFieldException) {
				it.printStackTrace()
			}
		}
	}
}
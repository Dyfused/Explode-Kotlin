package explode.rena.model

fun Map<String, Any>.toRenaStyledMap(): String {
	val sb = StringBuilder()
	val m = this.toMutableMap()

	sb.append("B.").append(m.remove("B") ?: error("Must contain 'B' value")).appendLine()// beginning
	m.forEach { (k, v) ->
		validateKey(k)
		sb.append(k).append("?").append(v).appendLine()
	}
	sb.append("E.") // ending

	return sb.toString()
}

fun String.toRenaStyledMap(): Map<String, Any> {
	val map = mutableMapOf<String, Any>()
	splitToSequence("\n").forEach {
		if(it.startsWith("B")) {
			map["B"] = it.substring(2)
		}
		else if(it.startsWith("E")) {
			return map
		}
		else if(it.isEmpty()) {
			// ignore
		}
		else {
			val s = it.split('?', limit = 2).takeIf { it.size == 2 } ?: error("Unexpected line: $it")
			validateKey(s[0])
			map[s[0]] = s[1]
		}
	}
	error("Not found 'E.' at the end.")
}

private fun validateKey(key: String) {
	if(key.length != 1) {
		error("Key of Rena Styled Map must be only 1 character.")
	}
	else if(key != key.uppercase()) {
		error("Key of Rena Styled Map must be uppercase character.")
	}
}
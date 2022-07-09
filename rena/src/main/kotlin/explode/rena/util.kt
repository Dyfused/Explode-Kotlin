package explode.rena

infix fun String.startsWith(prefix: String) = this.startsWith(prefix, ignoreCase = false)
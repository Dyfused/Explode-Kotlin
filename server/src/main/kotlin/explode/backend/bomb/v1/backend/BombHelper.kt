package explode.backend.bomb.v1.backend

import kotlin.reflect.KProperty1

/*

Functions in this file is used to shrink the data of the objects,
in order not to leak sensitive values like user token, user password, etc.

These functions should be named by `bombify`.

 */

// from: https://stackoverflow.com/questions/60010298/how-can-i-convert-a-camel-case-string-to-snake-case-and-back-in-idiomatic-kotlin
private val camelRegex = "(?<=[a-zA-Z])[A-Z]".toRegex()

// convert camel cases to snake case like 'chartId' to 'chart-id'
@Suppress("unused")
private fun String.camelCaseToSnakeCase() = camelRegex.replace(this) { "-${it.value}" }.lowercase()

/**
 * Replace [this] field values with [newValues] by Java reflection.
 */
internal inline fun <reified T : Any> T.reflectApplyChanges(
	newValues: Map<String, Any?>,
	vararg excludedFields: KProperty1<T, *>
) {
	val c = T::class.java
	val exclude = excludedFields.map { it.name }

	// replace the values with Java reflection
	newValues.forEach { (name, value) ->
		kotlin.runCatching {
			if(name !in exclude) {
				val field = c.getDeclaredField(name).apply { isAccessible = true }
				field.set(this, value)
			}
		}.onFailure {
			it.printStackTrace()
		}
	}
}
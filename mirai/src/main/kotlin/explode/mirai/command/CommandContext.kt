package explode.mirai.command

import explode.dataprovider.model.database.*
import explode.mirai.Explode
import net.mamoe.mirai.console.command.CommandSender

private val context = mutableMapOf<Long?, Any?>()

internal fun CommandSender.getContext() = context[user?.id]
internal fun CommandSender.putContext(value: Any?) {
	context[user?.id] = value
}

internal fun CommandSender.clearContext() {
	context[user?.id] = null
}

internal fun CommandSender.parseContext(str: String): ContextResult {

	return when {
		// match ID
		str.startsWith("#") -> ContextID(str.substring(1))
		// match last value
		str == "$" -> this.getContext().wrap()
		// match '$.<index>' like '$.1'
		str.startsWith("$.") -> {
			val index = str.substring(2).toIntOrNull()
			if(index == null) {
				ContextEmpty
			} else {
				this.getContext().ofIndex(index).wrap()
			}
		}
		// match '$::<key>' like '$::name'
		str.startsWith("$::") -> {
			val key = str.substring(3)
			if(key.isNotEmpty()) {
				this.getContext().ofKey(key).wrap()
			} else {
				ContextEmpty
			}
		}
		// otherwise
		else -> ContextName(str)
	}
}

internal fun <T> T.ofIndex(index: Int): Any? {
	return when(this) {
		is List<*> -> this.getOrNull(index)
		is MongoSet ->
			when(val chartId = this.charts.getOrNull(index)) {
				null -> null
				else -> Explode.getChart(chartId)
			}

		else -> null
	}
}

internal fun <T> T.ofKey(key: String): Any? = when(this) {
	is Map<*, *> -> this[key]
	is MongoSet ->
		when(key.lowercase()) {
			"name", "music" -> this.musicName
			"chart", "charts" -> this.charts.map(Explode::getChart)
			"chart-id", "chart-ids" -> this.charts
			"status" -> this.status
			"price", "coin" -> this.price
			"noter" -> Explode.getUser(this.noterId)
			"noter-id" -> this.noterId
			"intro", "introduction" -> this.introduction
			else -> null
		}

	is MongoChart ->
		when(key.lowercase()) {
			"d" -> this.D
			"class", "difficulty-class" -> this.difficultyClass
			"value", "difficulty-value" -> this.difficultyValue
			"in", "included", "set", "parent-set" -> with(Explode) { getParentSet() }
			else -> null
		}

	is MongoUser ->
		when(key.lowercase()) {
			"name", "username" -> this.username
			"coin" -> this.coin
			"diamond" -> this.diamond
			"r" -> this.R
			else -> null
		}

	else -> null
}

internal fun <T> T?.wrap() = if(this == null) ContextEmpty else ContextValue(this)

sealed interface ContextResult

object ContextEmpty : ContextResult
data class ContextID(val id: String) : ContextResult
data class ContextName(val name: String) : ContextResult
data class ContextValue<T : Any>(val value: T) : ContextResult
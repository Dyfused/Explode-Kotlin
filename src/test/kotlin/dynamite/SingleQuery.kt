package dynamite

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode

private val f = JsonNodeFactory.withExactBigDecimals(false)

class SingleQuery(private val queryString: String, val variables: MutableMap<String, String> = mutableMapOf()) {

	fun toPostJson(): String {
		val jsonData = f.objectNode()
		jsonData.put("query", queryString)

		if(variables.isNotEmpty()) {
			val variableJson = f.objectNode()
			variables.forEach { (key, value) -> variableJson.put(key, value) }
			jsonData.set<ObjectNode>("variables", variableJson)
		}

		return jsonData.toPrettyString().apply(::println)
	}
}
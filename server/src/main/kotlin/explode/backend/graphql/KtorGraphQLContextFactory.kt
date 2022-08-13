@file:Suppress("DEPRECATION")

package explode.backend.graphql

import com.expediagroup.graphql.generator.execution.GraphQLContext
import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import io.ktor.server.request.*

class KtorGraphQLContextFactory : GraphQLContextFactory<GraphQLContext, ApplicationRequest> {

	override suspend fun generateContextMap(request: ApplicationRequest): Map<Any, Any> = mutableMapOf(
		"soudayo" to (request.headers["x-soudayo"] ?: "trash-potato-server")
	)
}
@file:Suppress("DEPRECATION")

package explode

import com.expediagroup.graphql.generator.execution.GraphQLContext
import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import io.ktor.server.request.*

class KtorGraphQLContextFactory : GraphQLContextFactory<GraphQLContext, ApplicationRequest> {

	override suspend fun generateContextMap(request: ApplicationRequest): Map<Any, Any> = mutableMapOf(
		"user" to Unit // FIXME: Didn't firgure out what is this for
	)
}